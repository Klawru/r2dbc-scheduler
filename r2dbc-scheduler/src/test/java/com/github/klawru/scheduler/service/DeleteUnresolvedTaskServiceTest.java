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
import com.github.klawru.scheduler.stats.SchedulerMetricsRegistry;
import com.github.klawru.scheduler.SchedulerClient;
import com.github.klawru.scheduler.TaskResolver;
import com.github.klawru.scheduler.r2dbc.R2dbcClient;
import com.github.klawru.scheduler.repository.ExecutionEntity;
import com.github.klawru.scheduler.repository.postgres.PostgresTaskRepository;
import com.github.klawru.scheduler.task.OneTimeTask;
import com.github.klawru.scheduler.task.instance.TaskInstance;
import com.github.klawru.scheduler.util.DataHolder;
import com.github.klawru.scheduler.util.SchedulerBuilder;
import com.github.klawru.scheduler.util.Tasks;
import com.github.klawru.scheduler.util.TestTasks;
import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.r2dbc.core.DatabaseClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.timeout;

class DeleteUnresolvedTaskServiceTest extends AbstractPostgresTest {
    final Duration deleteUnresolvedAfter = Duration.ofDays(1);

    SchedulerClient client;
    PostgresTaskRepository repository;
    TaskResolver taskResolver;

    OneTimeTask<Void> taskA;
    OneTimeTask<Void> taskUnresolved;

    @BeforeEach
    void setUp() {
        taskA = Tasks.oneTime("taskA")
                .execute(new TestTasks.CountingHandler<>());
        taskUnresolved = Tasks.oneTime("taskUnresolved")
                .execute(new TestTasks.CountingHandler<>());

        ConnectionFactory connectionFactory = createConnectionFactory();
        SchedulerMetricsRegistry statRegistry = new SchedulerMetricsRegistry();
        repository = Mockito.spy(new PostgresTaskRepository(new R2dbcClient(DatabaseClient.create(connectionFactory)),
                "scheduled_job"));
        taskResolver = Mockito.spy(new TaskResolver(List.of(), statRegistry, testClock));
        client = SchedulerBuilder.create(connectionFactory, taskA)
                .clock(testClock)
                .schedulerMetricsRegistry(statRegistry)
                .taskResolver(taskResolver)
                .taskRepository(repository)
                .schedulerConfig(configBuilder -> configBuilder
                        .schedulerName("test-scheduler")
                        .deleteUnresolvedAfter(deleteUnresolvedAfter))
                .build();
    }

    @AfterEach
    void tearDown() {
        Optional.ofNullable(client).ifPresent(SchedulerClient::close);
    }

    @Test
    void name() {
        TaskInstance<Void> taskUnresolvedA1 = taskUnresolved.instance("unresolved");
        Instant deleteUnresolvedTime = testClock.now().minus(deleteUnresolvedAfter.plusSeconds(10));
        //save task to DB
        repository.createIfNotExists(taskUnresolvedA1, deleteUnresolvedTime, DataHolder.empty()).block();
        //lock a task by another scheduler
        var all = repository.getAll().collectList().blockOptional();
        assertThat(all).get(list(ExecutionEntity.class))
                .hasSize(1)
                .first()
                .returns(taskUnresolvedA1.getTaskName(), ExecutionEntity::getTaskName)
                .returns(taskUnresolvedA1.getId(), ExecutionEntity::getId);
        //When
        client.start();
        Mockito.verify(taskResolver, timeout(Duration.ofSeconds(10).toMillis()))
                .findTask(anyString());
        client.pause();
        client.start();
        //Then
        Mockito.verify(repository, timeout(Duration.ofSeconds(10).toMillis()).atLeast(1))
                .removeOldUnresolvedTask(anyCollection(), any(Instant.class));
        all = repository.getAll().collectList().blockOptional();
        assertThat(all)
                .get(list(ExecutionEntity.class))
                .isEmpty();
    }
}