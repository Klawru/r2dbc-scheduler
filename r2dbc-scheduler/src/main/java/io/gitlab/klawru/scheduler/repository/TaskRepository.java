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
package io.gitlab.klawru.scheduler.repository;

import io.gitlab.klawru.scheduler.task.instance.TaskInstanceId;
import io.gitlab.klawru.scheduler.util.DataHolder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Collection;

public interface TaskRepository {
    Mono<Void> createIfNotExists(TaskInstanceId instance, Instant nextExecutionTime, DataHolder<byte[]> data);

    Flux<ExecutionEntity> lockAndGetDue(String schedulerName, Instant now, int limit, Collection<String> unresolvedTaskNames);

    Mono<Void> remove(TaskInstanceId taskInstanceId, long version);

    Mono<Integer> updateHeartbeat(TaskInstanceId taskInstanceId, long version, Instant now);

    Mono<Integer> removeAllExecutions(String taskName);

    Mono<Void> reschedule(TaskInstanceId taskInstanceId,
                          long version,
                          Instant nextExecutionTime,
                          DataHolder<byte[]> newData,
                          Instant lastSuccess,
                          Instant lastFailure,
                          int consecutiveFailures);

    Mono<Integer> removeOldUnresolvedTask(Collection<String> unresolvedName, Instant from);

    Flux<ExecutionEntity> getAll();

    Flux<ExecutionEntity> getDeadExecution(Instant from);

    Mono<ExecutionEntity> getExecution(TaskInstanceId id);

    Flux<ExecutionEntity> getExecutions(String name, boolean picked);

    Flux<ExecutionEntity> getExecutionsForView(String name, boolean picked);
}
