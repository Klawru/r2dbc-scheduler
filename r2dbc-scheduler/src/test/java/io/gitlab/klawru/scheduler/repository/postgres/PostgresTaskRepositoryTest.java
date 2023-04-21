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

package io.gitlab.klawru.scheduler.repository.postgres;

import io.gitlab.klawru.scheduler.AbstractPostgresTest;
import io.gitlab.klawru.scheduler.TaskExample;
import io.gitlab.klawru.scheduler.exception.RepositoryException;
import io.gitlab.klawru.scheduler.r2dbc.R2dbcClient;
import io.gitlab.klawru.scheduler.repository.ExecutionEntity;
import io.gitlab.klawru.scheduler.task.OneTimeTask;
import io.gitlab.klawru.scheduler.task.instance.TaskInstance;
import io.gitlab.klawru.scheduler.task.instance.TaskInstanceId;
import io.gitlab.klawru.scheduler.util.DataHolder;
import io.gitlab.klawru.scheduler.util.TestTasks;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostgresTaskRepositoryTest extends AbstractPostgresTest {

    private final RecursiveComparisonConfiguration recursiveConfiguration = RecursiveComparisonConfiguration.builder()
            .withComparatorForType(Comparator.comparing(instant -> instant.truncatedTo(ChronoUnit.MICROS)), Instant.class)
            .build();
    R2dbcClient r2dbcClient;
    private PostgresTaskRepository taskRepository;
    private OneTimeTask<Void> taskA;
    private OneTimeTask<String> taskB;

    @BeforeEach
    void setUp() {
        taskA = TestTasks.oneTime("taskA", new TestTasks.CountingHandler<>());
        taskB = TestTasks.oneTimeWithType("taskB", String.class, new TestTasks.SavingHandler<>());

        r2dbcClient = new R2dbcClient(DatabaseClient.create(createConnectionFactory()));
        taskRepository = new PostgresTaskRepository(r2dbcClient, "scheduled_job");

    }

    @Test
    void createIfNotExists() {
        //When
        saveTask(taskA.instance("1"), testClock.now(), DataHolder.empty());
        saveTask(taskB.instance("1"), testClock.now(), DataHolder.empty());
        //
        List<ExecutionEntity> allTask = getAllTask();
        assertThat(allTask)
                .hasSize(2)
                .extracting(ExecutionEntity::getTaskName)
                .containsOnly("taskA", "taskB");
    }

    @Test
    void createIfNotExistsWhenSecondSameIdThenError() {
        TaskInstance<Void> instanceA = taskA.instance("1");
        //First create
        saveTask(instanceA, testClock.now(), DataHolder.empty());
        //When
        //Second create
        Mono<Void> createTaskA = taskRepository.createIfNotExists(instanceA, testClock.now(), DataHolder.empty());
        assertThatThrownBy(createTaskA::block)
                .isInstanceOf(RepositoryException.class)
                .hasMessageContaining("Failed to save the task to the database. Perhaps such a task has already been created");
        //Then
        List<ExecutionEntity> allTask = getAllTask();
        assertThat(allTask)
                .hasSize(1)
                .first()
                .returns(instanceA.getTaskName(), ExecutionEntity::getTaskName)
                .returns(instanceA.getId(), ExecutionEntity::getId);
    }

    @Test
    void lockAndGetDue() {
        String schedulerName = "schedulerName";
        saveTask(taskA.instance("1"), testClock.now(), DataHolder.empty());
        saveTask(taskA.instance("2"), testClock.now(), DataHolder.empty());
        //When
        List<ExecutionEntity> locked = taskRepository.lockAndGetDue(schedulerName, testClock.now(), 10, Collections.emptyList())
                .collectList().block();
        //Then
        assertThat(locked)
                .hasSize(2)
                .extracting(ExecutionEntity::getId)
                .containsOnly("1", "2");
        List<ExecutionEntity> executionEntities = getAllTask();
        assertThat(executionEntities)
                .hasSize(2)
                .allSatisfy(executionEntity -> assertThat(executionEntity)
                        .returns(schedulerName, ExecutionEntity::getPickedBy)
                        .returns(2L, ExecutionEntity::getVersion)
                );
    }

    @Test
    void remove() {
        TaskInstance<Void> instance1 = taskA.instance("1");
        TaskInstance<Void> instance2 = taskA.instance("2");
        saveTask(instance1, testClock.now(), DataHolder.empty());
        saveTask(instance2, testClock.now(), DataHolder.empty());
        //When
        taskRepository.remove(instance1).block();
        //Then
        List<ExecutionEntity> executionEntityList = getAllTask();
        assertThat(executionEntityList)
                .hasSize(1)
                .extracting(ExecutionEntity::getId)
                .containsOnly("2");
    }

    @Test
    void removeExecutions() {
        TaskInstance<Void> instanceA = taskA.instance("1");
        TaskInstance<String> instanceB = taskB.instance("2");
        saveTask(instanceA, testClock.now(), DataHolder.empty());
        saveTask(instanceB, testClock.now(), DataHolder.empty());
        //When
        taskRepository.removeAllExecutions(taskA.getName()).block();
        //Then
        List<ExecutionEntity> executionEntityList = getAllTask();
        assertThat(executionEntityList)
                .hasSize(1)
                .extracting(ExecutionEntity::getTaskName)
                .containsOnly(taskB.getName());
    }

    @Test
    void updateHeartbeat() {
        TaskInstance<Void> instanceA = taskA.instance("1");
        saveTask(instanceA, testClock.now(), DataHolder.empty());
        //When
        List<Integer> updated = taskRepository.findExecutions(TaskExample.all())
                .flatMap(executionEntity -> {
                    TaskInstanceId taskInstanceId = TaskInstanceId.of(executionEntity.getTaskName(), executionEntity.getId());
                    return taskRepository.updateHeartbeat(taskInstanceId, executionEntity.getVersion(), testClock.now());
                })
                .collectList()
                .block();
        //Then
        assertThat(updated)
                .hasSize(1)
                .containsOnly(1);
    }

    @Test
    void getExecution() {
        TaskInstance<String> instanceB = taskB.instance("1");
        byte[] data = "\"TestData\"".getBytes(StandardCharsets.UTF_8);
        saveTask(instanceB, testClock.now(), DataHolder.of(data));
        //When
        var execution = taskRepository.findExecution(instanceB).block();
        //Then
        Assertions.assertThat(execution)
                .usingRecursiveComparison(recursiveConfiguration)
                .isEqualTo(new ExecutionEntity(instanceB.getTaskName(),
                        instanceB.getId(),
                        data,
                        testClock.now(),
                        false,
                        null,
                        null,
                        null,
                        0,
                        null,
                        1
                ));
    }

    @Test
    void findExecutions() {
        TaskInstance<String> instanceB = taskB.instance("1");
        byte[] data = "\"TestData\"".getBytes(StandardCharsets.UTF_8);
        saveTask(instanceB, testClock.now(), DataHolder.of(data));
        //When
        var executions = taskRepository.findExecutions(TaskExample.scheduled(instanceB.getTaskName()))
                .collectList().block();
        //Then
        Assertions.assertThat(executions)
                .hasSize(1)
                .first()
                .usingRecursiveComparison(recursiveConfiguration)
                .isEqualTo(new ExecutionEntity(instanceB.getTaskName(),
                        instanceB.getId(),
                        data,
                        testClock.now(),
                        false,
                        null,
                        null,
                        null,
                        0,
                        null,
                        1
                ));
    }

    @Test
    void getAll() {
        saveTask(taskA.instance("1"), testClock.now(), DataHolder.empty());
        saveTask(taskA.instance("2"), testClock.now(), DataHolder.empty());
        saveTask(taskB.instance("2"), testClock.now(), DataHolder.empty());
        //When
        List<ExecutionEntity> all = taskRepository.findExecutions(TaskExample.all()).collectList().block();
        //Then
        assertThat(all)
                .hasSize(3)
                .extracting(ExecutionEntity::getTaskName)
                .containsOnly(taskA.getName(), taskB.getName());
    }

    @Test
    void removeOldUnresolvedTask() {
        saveTask(taskA.instance("1"), testClock.now().plusSeconds(10), DataHolder.empty());
        saveTask(taskB.instance("2"), testClock.now().plusSeconds(10), DataHolder.empty());
        //When
        Integer delete = taskRepository.removeOldUnresolvedTask(List.of(taskA.getName()), testClock.now()).block();
        assertThat(delete).isZero();
        testClock.plusSecond(10);
        delete = taskRepository.removeOldUnresolvedTask(List.of(taskA.getName()), testClock.now()).block();
        assertThat(delete).isEqualTo(1);
        //Then
        List<ExecutionEntity> all = getAllTask();
        assertThat(all)
                .hasSize(1)
                .extracting(ExecutionEntity::getTaskName)
                .containsOnly(taskB.getName());
    }

    @Test
    void getDeadExecution() {
        Instant past = testClock.now();
        saveTask(taskA.instance("1"), past, DataHolder.empty());
        saveTask(taskB.instance("2"), past, DataHolder.empty());
        List<ExecutionEntity> takenExecution = taskRepository.lockAndGetDue("getDeadExecution",
                past,
                1,
                List.of(taskB.getName())).collectList().block();
        assertThat(takenExecution).hasSize(1)
                .first()
                .returns(taskA.getName(), ExecutionEntity::getTaskName);

        //When
        testClock.plusSecond(60);
        var deadExecution = taskRepository.getDeadExecution(testClock.now()).collectList().block();
        //Then
        Assertions.assertThat(deadExecution)
                .hasSize(1)
                .first()
                .returns(taskA.getName(), ExecutionEntity::getTaskName);
    }

    @Test
    void reschedule() {
        TaskInstance<String> instanceB = taskB.instance("1");
        byte[] data = "\"data\"".getBytes(StandardCharsets.UTF_8);
        byte[] newData = "\"newData\"".getBytes(StandardCharsets.UTF_8);
        saveTask(instanceB, testClock.now(), DataHolder.of(data));
        //When
        taskRepository.reschedule(instanceB,
                        1,
                        testClock.now().plusSeconds(1),
                        DataHolder.of(newData),
                        testClock.now().plusSeconds(2),
                        testClock.now().plusSeconds(3),
                        4)
                .block();
        //Then
        List<ExecutionEntity> allTask = getAllTask();
        assertThat(allTask)
                .hasSize(1)
                .first()
                .usingRecursiveComparison(recursiveConfiguration)
                .isEqualTo(new ExecutionEntity(
                        instanceB.getTaskName(),
                        instanceB.getId(),
                        newData,
                        testClock.now().plusSeconds(1),
                        false,
                        null,
                        testClock.now().plusSeconds(2),
                        testClock.now().plusSeconds(3),
                        4,
                        null,
                        2
                ));
    }

    private List<ExecutionEntity> getAllTask() {
        return taskRepository.findExecutions(TaskExample.all()).collectList().blockOptional().orElse(List.of());
    }

    private void saveTask(TaskInstanceId instance, Instant executionTime, DataHolder<byte[]> data) {
        taskRepository.createIfNotExists(instance, executionTime, data).block();
    }

}