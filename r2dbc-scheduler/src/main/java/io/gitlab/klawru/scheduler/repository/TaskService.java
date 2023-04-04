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

import io.gitlab.klawru.scheduler.TaskResolver;
import io.gitlab.klawru.scheduler.executor.Execution;
import io.gitlab.klawru.scheduler.task.instance.NextExecutionTime;
import io.gitlab.klawru.scheduler.task.callback.ScheduleOnStartup;
import io.gitlab.klawru.scheduler.task.instance.TaskInstance;
import io.gitlab.klawru.scheduler.task.instance.TaskInstanceId;
import io.gitlab.klawru.scheduler.util.DataHolder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.Closeable;
import java.time.Duration;
import java.util.stream.Stream;

public interface TaskService extends Closeable {

    <T> Mono<Void> createIfNotExists(TaskInstanceId execution, NextExecutionTime scheduler, DataHolder<T> dataHolder);

    <T> Mono<Void> reschedule(TaskInstanceId taskInstanceId, NextExecutionTime nextExecutionTime, DataHolder<T> newData);

    <T> Mono<Void> reschedule(Execution<T> execution, NextExecutionTime nextExecutionTime, DataHolder<T> newData);


    Flux<Execution<?>> lockAndGetDue(int limit);

    Mono<Void> remove(Execution<?> execution);

    Mono<Boolean> updateHeartbeat(Execution<?> execution);

    Mono<Integer> removeExecutions(String taskName);

    Flux<Execution<?>> getAll();

    Mono<Integer> deleteUnresolvedTask(Duration deleteUnresolvedAfter);

    Mono<Void> rescheduleDeadExecutionTask(Duration durationNotUpdate);

    <T> Mono<Execution<T>> findExecution(TaskInstance<T> taskInstanceId);

    Mono<Execution<?>> findExecution(TaskInstanceId taskInstanceId);

    Flux<Execution<?>> findExecution(boolean picked);

    <T> Flux<Execution<T>> findExecution(String name, boolean picked, Class<T> dataClass);

    TaskResolver getTaskResolver();

    Stream<ScheduleOnStartup> scheduleOnStartUp();
}
