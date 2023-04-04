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
package io.gitlab.klawru.scheduler;

import io.gitlab.klawru.scheduler.executor.Execution;
import io.gitlab.klawru.scheduler.stats.SchedulerMetricsRegistry;
import io.gitlab.klawru.scheduler.task.instance.TaskInstance;
import io.gitlab.klawru.scheduler.task.instance.TaskInstanceId;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class TaskScheduler {
    @Getter
    private final SchedulerClient client;


    public void schedule(TaskInstance<?> taskInstance) {
        client.schedule(taskInstance).block();
    }

    public void schedule(TaskInstance<?> taskInstance, Instant instant) {
        client.schedule(taskInstance, instant).block();
    }

    public <T> void schedule(TaskInstance<T> taskInstance, T newData) {
        client.schedule(taskInstance, newData).block();
    }


    public <T> void schedule(TaskInstance<T> taskInstance, Instant nextTime, T newData) {
        client.schedule(taskInstance, nextTime, newData).block();
    }


    public void reschedule(TaskInstance<?> taskInstance) {
        client.reschedule(taskInstance).block();
    }


    public void reschedule(TaskInstance<?> taskInstance, Instant instant) {
        client.reschedule(taskInstance, instant).block();
    }

    public <T> void reschedule(TaskInstance<T> schedule, T newData) {
        client.reschedule(schedule, newData).block();
    }

    public <T> void reschedule(TaskInstance<T> schedule, Instant instant, T newData) {
        client.reschedule(schedule, instant, newData).block();
    }

    public void runAnyDueExecutions() {
        client.fetchTask();
    }

    public int getCountProcessingTask() {
        return client.getCountProcessingTask();
    }

    public long countScheduledExecutions() {
        Long count = client.getScheduledExecutions().count().block();
        return Optional.ofNullable(count).orElse(0L);
    }

    public Optional<Execution<?>> getExecution(TaskInstanceId instanceId) {
        return client.getExecution(instanceId).blockOptional();
    }

    public List<Execution<?>> getAllExecution() {
        return client.getAllExecution()
                .collectList()
                .blockOptional()
                .orElse(List.of());
    }

    public List<Execution<?>> getScheduledExecutions() {
        return client.getScheduledExecutions()
                .collectList()
                .blockOptional()
                .orElse(List.of());
    }

    public <T> List<Execution<T>> getScheduledExecutionsForTask(String name, Class<T> dataClass) {
        return client.getScheduledExecutionsForTask(name, dataClass)
                .collectList()
                .blockOptional()
                .orElse(List.of());
    }

    public SchedulerMetricsRegistry getSchedulerMetricsRegistry() {
        return client.getSchedulerMetricsRegistry();
    }

    public void close() {
        client.pause();
    }
}
