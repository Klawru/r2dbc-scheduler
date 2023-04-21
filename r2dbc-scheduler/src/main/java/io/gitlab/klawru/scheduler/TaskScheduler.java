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
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.List;

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

    public void fetchTask() {
        client.fetchTask();
    }

    public int getCountProcessingTask() {
        return client.getCountProcessingTask();
    }

    public <T> List<Execution<T>> findExecution(TaskExample<T> taskExample) {
        return client.findExecutions(taskExample)
                .collectList()
                .blockOptional()
                .orElse(List.of());
    }

    public <T> Long countExecution(TaskExample<T> taskExample) {
        return client.countExecution(taskExample)
                .blockOptional()
                .orElse(0L);
    }

    public SchedulerMetricsRegistry getMetricsRegistry() {
        return client.getMetricsRegistry();
    }

    public void close() {
        client.pause();
    }
}
