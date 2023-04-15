/*
 * Copyright 2023 Klawru
 * Copyright (C) Gustav Karlsson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gitlab.klawru.scheduler.repository.postgres;

import io.gitlab.klawru.scheduler.exception.RepositoryException;
import io.gitlab.klawru.scheduler.r2dbc.PreparedStatementSetter;
import io.gitlab.klawru.scheduler.r2dbc.R2dbcClient;
import io.gitlab.klawru.scheduler.repository.ExecutionEntity;
import io.gitlab.klawru.scheduler.repository.TaskRepository;
import io.gitlab.klawru.scheduler.task.instance.TaskInstanceId;
import io.gitlab.klawru.scheduler.util.DataHolder;
import io.r2dbc.postgresql.codec.Json;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

import static java.lang.Boolean.TRUE;

@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("java:S1192")
public class PostgresTaskRepository implements TaskRepository {
    private final R2dbcClient r2dbcClient;
    private final String tableName;

    @Override
    public Mono<Void> createIfNotExists(TaskInstanceId instance, Instant executionTime, DataHolder<byte[]> data) {
        return getExecution(instance)
                .map(existingExecution -> false)
                .switchIfEmpty(create(instance, executionTime, data))
                .flatMap(created -> {
                    if (TRUE.equals(created)) {
                        return Mono.empty();
                    } else {
                        return Mono.error(new RepositoryException("Failed to save the task to the database. Perhaps such a task has already been created", instance));
                    }
                });
    }

    protected Mono<Boolean> create(TaskInstanceId instance, Instant executionTime, DataHolder<byte[]> data) {
        return r2dbcClient.execute(
                        "insert into " + tableName + " (task_name, task_instance, " +
                                (data.isPresent() ? "task_data, " : " ") +
                                "execution_time, picked, version) " +
                                "values(:taskName, :taskInstance, " +
                                (data.isPresent() ? ":taskData, " : " ") +
                                ":executionTime, :picked, :version)",
                        bindTarget -> {
                            bindTarget
                                    .bind("taskName", instance.getTaskName())
                                    .bind("taskInstance", instance.getId())
                                    .bind("executionTime", executionTime)
                                    .bind("picked", false)
                                    .bind("version", 1L);
                            if (data.isPresent()) {
                                bindTarget.bind("taskData", Optional.ofNullable(data.getData())
                                        .map(Json::of)
                                        .orElse(null), Json.class);
                            }
                            return bindTarget;
                        })
                .map(created -> {
                    if (created != 1) {
                        throw new RepositoryException("Expected one task to be created, but create " + created + ". Indicates a bug.", instance);
                    }
                    return true;
                });
    }

    @Override
    public Flux<ExecutionEntity> lockAndGetDue(String schedulerName, Instant now, int limit, Collection<String> unresolvedTaskNames) {
        return r2dbcClient.queryMany(
                "UPDATE " + tableName + " ut SET picked = true, picked_by = :pickedBy, last_heartbeat = :lastHeartbeat, version = version + 1 " +
                        " WHERE (ut.task_name, ut.task_instance) IN (" +
                        "   SELECT st.task_name, st.task_instance FROM " + tableName + " st " +
                        "   WHERE picked = false and execution_time <= :execution_time AND task_name <> all ( :task_name )" +
                        "   ORDER BY execution_time FOR UPDATE SKIP LOCKED LIMIT :limit" +
                        " ) " +
                        " RETURNING ut.* ",
                bindTarget -> {
                    // Update
                    bindTarget
                            .bind("pickedBy", schedulerName)
                            .bind("lastHeartbeat", now);
                    // Inner select
                    bindTarget
                            .bind("execution_time", now)
                            .bind("task_name", unresolvedTaskNames.toArray(String[]::new))
                            .bind("limit", limit);
                    return bindTarget;
                },
                ExecutionResultSetMapper.EXECUTION_MAPPER
        );
    }

    public Mono<Void> remove(TaskInstanceId taskInstanceId) {
        return r2dbcClient.execute("DELETE FROM " + tableName + " " +
                        "WHERE task_name = :taskName " +
                        "AND task_instance = :taskInstance ",
                bindTarget -> bindTarget
                        .bind("taskName", taskInstanceId.getTaskName())
                        .bind("taskInstance", taskInstanceId.getId())
        ).map(removed -> {
            if (removed != 1) {
                throw new RepositoryException("Expected one execution to be removed, but removed " + removed + ". Indicates a bug.", taskInstanceId);
            }
            return true;
        }).then();
    }

    @Override
    public Mono<Integer> removeAllExecutions(String taskName) {
        return r2dbcClient.execute(
                        "DELETE FROM " + tableName + " WHERE task_name = :taskName",
                        ps -> ps.bind("taskName", taskName))
                .doOnNext(deleted -> log.trace("removed by removeAllByName count={}", deleted));
    }

    @Override
    public Mono<Integer> updateHeartbeat(TaskInstanceId taskInstanceId, long version, Instant now) {
        return r2dbcClient.execute(
                "UPDATE " + tableName + " SET last_heartbeat = :lastHeartbeat " +
                        "WHERE  task_name = :taskName " +
                        "AND task_instance = :taskInstance " +
                        "AND version = :version ",
                bindTarget -> bindTarget
                        .bind("lastHeartbeat", now)
                        .bind("taskName", taskInstanceId.getTaskName())
                        .bind("taskInstance", taskInstanceId.getId())
                        .bind("version", version)
        );
    }


    public Mono<ExecutionEntity> getExecution(TaskInstanceId taskInstance) {
        return getExecution(taskInstance.getTaskName(), taskInstance.getId())
                .doOnNext(executionEntity -> log.debug("getExecution:{}", executionEntity.getTaskNameId()));
    }

    @Override
    public Flux<ExecutionEntity> getExecutions(@NotNull String taskName, boolean picked) {
        return r2dbcClient.queryMany(
                "select * from " + tableName + " " +
                        "WHERE task_name = :taskName " +
                        "AND picked = :picked ",
                target -> target
                        .bind("taskName", taskName)
                        .bind("picked", picked),
                ExecutionResultSetMapper.EXECUTION_MAPPER
        );
    }

    @Override
    public Flux<ExecutionEntity> getExecutionsForView(String name, boolean picked) {
        return r2dbcClient.queryMany(
                "select * from " + tableName + " " +
                        "WHERE ( :taskName IS NULL OR task_name = :taskName) " +
                        "AND ( :picked IS NULL OR picked = :picked) ",
                target -> target
                        .bind("taskName", name, String.class)
                        .bind("picked", picked, Boolean.class),
                ExecutionResultSetMapper.EXECUTION_MAPPER
        );
    }


    public Mono<ExecutionEntity> getExecution(String taskName, String taskInstanceId) {
        return r2dbcClient.query(
                        "select * from " + tableName + " where task_name = :taskName and task_instance = :taskInstance",
                        bindTarget -> bindTarget
                                .bind("taskName", taskName)
                                .bind("taskInstance", taskInstanceId),
                        ExecutionResultSetMapper.EXECUTION_MAPPER
                )
                .onErrorMap(IncorrectResultSizeDataAccessException.class,
                        e -> new RepositoryException("More than one task found that matches " +
                                "the combination of taskName='" + taskName + "' and taskId='" + taskInstanceId + "'",
                                taskName, taskInstanceId)
                );
    }

    @Override
    public Flux<ExecutionEntity> getAll() {
        return r2dbcClient.queryMany(
                "select * from " + tableName + "",
                PreparedStatementSetter.NO_BIND,
                ExecutionResultSetMapper.EXECUTION_MAPPER
        );
    }

    @Override
    public Mono<Integer> removeOldUnresolvedTask(Collection<String> unresolvedName, Instant before) {
        return r2dbcClient.execute(
                "DELETE FROM " + tableName + " WHERE execution_time <= :before AND task_name = any( :taskName )",
                bindTarget -> bindTarget
                        .bind("before", before)
                        .bind("taskName", unresolvedName.toArray(String[]::new)));
    }

    @Override
    public Flux<ExecutionEntity> getDeadExecution(Instant from) {
        return r2dbcClient.queryMany("SELECT * FROM " + tableName + " WHERE last_heartbeat < :lastHeartbeat",
                bindTarget -> bindTarget.bind("lastHeartbeat", from),
                ExecutionResultSetMapper.EXECUTION_MAPPER);
    }

    @Override
    public Mono<Void> reschedule(TaskInstanceId taskInstanceId,
                                 long version,
                                 Instant nextExecutionTime,
                                 DataHolder<byte[]> newData,
                                 Instant lastSuccess,
                                 Instant lastFailure,
                                 int consecutiveFailures) {
        return r2dbcClient.execute(
                        "update " + tableName + " set " +
                                "picked = :picked, " +
                                "picked_by = :picked_by, " +
                                "last_heartbeat = :lastHeartbeat, " +
                                "last_success = :lastSuccess, " +
                                "last_failure = :lastFailure, " +
                                "consecutive_failures = :consecutiveFailures, " +
                                "execution_time = :executionTime, " +
                                (newData.isPresent() ? "task_data = :taskData, " : "") +
                                "version = version + 1 " +
                                "where task_name = :taskName " +
                                "and task_instance = :taskInstance " +
                                "and version = :version",
                        bindTarget -> {
                            bindTarget
                                    .bind("picked", false)
                                    .bind("picked_by", null, String.class)
                                    .bind("lastHeartbeat", null, Instant.class)
                                    .bind("lastSuccess", lastSuccess, Instant.class)
                                    .bind("lastFailure", lastFailure, Instant.class)
                                    .bind("consecutiveFailures", consecutiveFailures)
                                    .bind("executionTime", nextExecutionTime)
                                    .bind("taskName", taskInstanceId.getTaskName())
                                    .bind("taskInstance", taskInstanceId.getId())
                                    .bind("version", version);
                            if (newData.isPresent()) {
                                bindTarget.bind("taskData",
                                        Optional.ofNullable(newData.getData())
                                                .map(Json::of)
                                                .orElse(null),
                                        Json.class);
                            }
                            return bindTarget;
                        })
                .flatMap(updated -> {
                    if (updated != 1) {
                        return Mono.error(() ->
                                new RepositoryException("Expected one execution to be updated, but updated " + updated + ". Indicates a bug.", taskInstanceId)
                        );
                    }
                    return Mono.empty();
                });
    }
}
