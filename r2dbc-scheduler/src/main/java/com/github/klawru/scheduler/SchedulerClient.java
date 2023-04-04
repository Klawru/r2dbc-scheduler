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
package com.github.klawru.scheduler;

import com.github.klawru.scheduler.config.SchedulerConfiguration;
import com.github.klawru.scheduler.executor.Execution;
import com.github.klawru.scheduler.stats.SchedulerMetricsRegistry;
import com.github.klawru.scheduler.task.instance.TaskInstance;
import com.github.klawru.scheduler.task.instance.TaskInstanceId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

public interface SchedulerClient extends StartPauseService, AutoCloseable {
    SchedulerMetricsRegistry getSchedulerMetricsRegistry();

    <T> Mono<Void> schedule(TaskInstance<T> taskInstance);

    <T> Mono<Void> schedule(TaskInstance<T> taskInstance, Instant nextTime);

    <T> Mono<Void> schedule(TaskInstance<T> taskInstance, T newData);

    <T> Mono<Void> schedule(TaskInstance<T> taskInstance, Instant nextTime, T newData);

    <T> Mono<Void> reschedule(TaskInstance<T> taskInstance);

    <T> Mono<Void> reschedule(TaskInstance<T> taskInstance, Instant nextTime);

    <T> Mono<Void> reschedule(TaskInstance<T> taskInstance, T newData);

    <T> Mono<Void> reschedule(TaskInstance<T> taskInstance, Instant nextTime, T newData);

    <T> Mono<Void> cancel(Execution<T> e);

    void fetchTask();

    void detectDeadExecution();

    SchedulerConfiguration getConfig();

    Flux<Execution<?>> getAllExecution();

    <T> Mono<Execution<T>> getExecution(TaskInstance<T> taskInstanceId);

    Mono<Execution<?>> getExecution(TaskInstanceId taskInstanceId);

    Flux<Execution<?>> getScheduledExecutions();

    <T> Flux<Execution<T>> getScheduledExecutionsForTask(String name, Class<T> dataClass);

    int getCountProcessingTask();

    @Override
    void close();
}
