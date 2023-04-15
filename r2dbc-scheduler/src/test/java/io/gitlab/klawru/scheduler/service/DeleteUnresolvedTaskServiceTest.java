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

package io.gitlab.klawru.scheduler.service;

import io.gitlab.klawru.scheduler.AbstractPostgresTest;
import io.gitlab.klawru.scheduler.SchedulerClient;
import io.gitlab.klawru.scheduler.TaskResolver;
import io.gitlab.klawru.scheduler.r2dbc.R2dbcClient;
import io.gitlab.klawru.scheduler.repository.ExecutionEntity;
import io.gitlab.klawru.scheduler.repository.postgres.PostgresTaskRepository;
import io.gitlab.klawru.scheduler.stats.SchedulerMetricsRegistry;
import io.gitlab.klawru.scheduler.task.OneTimeTask;
import io.gitlab.klawru.scheduler.task.instance.TaskInstance;
import io.gitlab.klawru.scheduler.util.DataHolder;
import io.gitlab.klawru.scheduler.util.SchedulerBuilder;
import io.gitlab.klawru.scheduler.util.Tasks;
import io.gitlab.klawru.scheduler.util.TestTasks;
import io.r2dbc.spi.ConnectionFactory;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.mockito.Mockito;
import org.springframework.r2dbc.core.DatabaseClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
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
                        .deleteUnresolvedAfter(deleteUnresolvedAfter)
                        .pollingInterval(Duration.ofSeconds(1)))
                .build();

    }

    @AfterEach
    void tearDown() {
        Optional.ofNullable(client).ifPresent(SchedulerClient::close);
    }

    @RepeatedTest(4)
    void removeOldUnresolvedTask() {
        TaskInstance<Void> taskUnresolvedA1 = taskUnresolved.instance("unresolved");
        Instant deleteUnresolvedTime = testClock.now().minus(deleteUnresolvedAfter.plusSeconds(10));
        //save task to DB
        repository.createIfNotExists(taskUnresolvedA1, deleteUnresolvedTime, DataHolder.empty()).block();
        //check a task in table
        var all = repository.getAll().collectList().block();
        assertThat(all)
                .hasSize(1)
                .first()
                .returns(taskUnresolvedA1.getTaskName(), ExecutionEntity::getTaskName)
                .returns(taskUnresolvedA1.getId(), ExecutionEntity::getId);
        //Start the client for found 'taskUnresolved'
        client.start();
        Mockito.verify(taskResolver, timeout(Duration.ofSeconds(10).toMillis()))
                .findTask("taskUnresolved");
        client.pause();
        //When
        //Start DeleteUnresolvedTaskService
        client.start();
        //Then
        Mockito.verify(repository, timeout(Duration.ofSeconds(10).toMillis()).atLeast(1))
                .removeOldUnresolvedTask(anyCollection(), any(Instant.class));
        Awaitility.await("Delete from table").atMost(10, TimeUnit.SECONDS)
                .until(() -> repository.getAll().count()
                        .map(executionEntities -> executionEntities == 0)
                        .blockOptional().orElse(false));
        all = repository.getAll().collectList().block();
        assertThat(all).isEmpty();
    }
}