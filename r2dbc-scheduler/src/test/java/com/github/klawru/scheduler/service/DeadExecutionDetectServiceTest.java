/*
 * Copyright 2023 Klawru
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.klawru.scheduler.service;

import com.github.klawru.scheduler.AbstractPostgresTest;
import com.github.klawru.scheduler.executor.Execution;
import com.github.klawru.scheduler.ExecutionOperations;
import com.github.klawru.scheduler.SchedulerClient;
import com.github.klawru.scheduler.r2dbc.R2dbcClient;
import com.github.klawru.scheduler.repository.ExecutionEntity;
import com.github.klawru.scheduler.repository.postgres.PostgresTaskRepository;
import com.github.klawru.scheduler.task.OneTimeTask;
import com.github.klawru.scheduler.task.callback.DeadExecutionHandler.ReviveDeadExecution;
import com.github.klawru.scheduler.task.instance.TaskInstance;
import com.github.klawru.scheduler.util.DataHolder;
import com.github.klawru.scheduler.util.SchedulerBuilder;
import com.github.klawru.scheduler.util.Tasks;
import com.github.klawru.scheduler.util.TestTasks;
import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.byLessThan;
import static org.awaitility.Awaitility.await;

class DeadExecutionDetectServiceTest extends AbstractPostgresTest {

    final Duration heartbeatInterval = Duration.ofSeconds(10);

    SchedulerClient client;
    PostgresTaskRepository repository;

    TestTasks.CountingHandler<Void> handlerA;
    DeadExecutionCounter<Void> deadExecutionCounter;
    OneTimeTask<Void> taskA;

    @BeforeEach
    void setUp() {
        handlerA = new TestTasks.CountingHandler<>();
        deadExecutionCounter = new DeadExecutionCounter<>();
        taskA = Tasks.oneTime("taskA")
                .onDeadExecution(deadExecutionCounter)
                .execute(handlerA);

        ConnectionFactory connectionFactory = createConnectionFactory();
        repository = new PostgresTaskRepository(new R2dbcClient(DatabaseClient.create(connectionFactory)),
                "scheduled_job");
        client = SchedulerBuilder.create(connectionFactory, taskA)
                .clock(testClock)
                .schedulerConfig(configBuilder -> configBuilder
                        .schedulerName("test-scheduler")
                        .heartbeatInterval(heartbeatInterval))
                .build();
    }

    @AfterEach
    void tearDown() {
        Optional.ofNullable(client).ifPresent(SchedulerClient::pause);
    }

    /**
     * @see DeadExecutionDetectService
     */
    @Test
    void detectDeadExecution() {
        TaskInstance<Void> taskInstanceA1 = taskA.instance("1");
        Instant pickedTime = testClock.now().minus(heartbeatInterval.multipliedBy(10));
        //save task to DB
        repository.createIfNotExists(taskInstanceA1, pickedTime, DataHolder.empty()).block();
        //lock a task by another scheduler
        var pickedExecutions = repository.lockAndGetDue("anotherScheduler",
                pickedTime,
                1,
                List.of()).collectList().block();
        assertThat(pickedExecutions).hasSize(1)
                .first()
                .returns(taskInstanceA1.getTaskName(), ExecutionEntity::getTaskName)
                .returns(taskInstanceA1.getId(), ExecutionEntity::getId)
                .returns(true, ExecutionEntity::isPicked);
        testClock.plusSecond(10);
        //When
        client.start();
        client.detectDeadExecution();
        //Then
        await("Until the task is rescheduled")
                .atMost(10, SECONDS)
                .until(() -> deadExecutionCounter.getCount() != 0);

        Optional<ExecutionEntity> rescheduled = repository.getExecution(taskInstanceA1).blockOptional();
        assertThat(rescheduled).get()
                .returns(taskInstanceA1.getTaskName(), ExecutionEntity::getTaskName)
                .returns(taskInstanceA1.getId(), ExecutionEntity::getId)
                .returns(1, ExecutionEntity::getConsecutiveFailures)
                .satisfies(execution -> assertThat(execution.getLastFailure())
                        .isCloseTo(testClock.now(), byLessThan(100, MILLIS)))
                .satisfies(execution -> assertThat(execution.getExecutionTime())
                        .isCloseTo(testClock.now(), byLessThan(100, MILLIS)));
    }

    static class DeadExecutionCounter<T> extends ReviveDeadExecution<T> {

        private final AtomicInteger count = new AtomicInteger();

        @Override
        public Mono<Void> deadExecution(Execution<? super T> execution, ExecutionOperations executionOperations) {
            return super.deadExecution(execution, executionOperations)
                    .doFinally(signalType -> count.incrementAndGet());
        }

        public int getCount() {
            return count.get();
        }
    }
}