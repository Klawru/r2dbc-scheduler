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
package com.github.klawru.scheduler;

import com.github.klawru.scheduler.stats.SchedulerMetricsRegistry;
import com.github.klawru.scheduler.task.AbstractTask;
import com.github.klawru.scheduler.util.Clock;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class TaskResolver {
    private final Map<String, AbstractTask<?>> taskMap;
    private final Map<String, UnresolvedTask> unresolvedTasks = new ConcurrentHashMap<>();
    private final Clock clock;
    private final SchedulerMetricsRegistry registry;

    public TaskResolver(List<AbstractTask<?>> tasks, SchedulerMetricsRegistry schedulerMetricsRegistry, Clock clock) {
        this.registry = schedulerMetricsRegistry;
        this.taskMap = new HashMap<>();
        this.clock = clock;
        add(tasks);
    }

    public void add(List<AbstractTask<?>> tasks) {
        for (AbstractTask<?> abstractTask : tasks) {
            taskMap.put(abstractTask.getName(), abstractTask);
        }
    }

    public Optional<AbstractTask<?>> findTask(String name) {
        Optional<AbstractTask<?>> taskOptional = Optional.ofNullable(taskMap.get(name));
        if (taskOptional.isEmpty()) {
            unresolvedTasks.computeIfAbsent(name, taskName -> {
                log.warn("Not found task by name:'{}'", taskName);
                return new UnresolvedTask(taskName);
            });
        }
        return taskOptional;
    }

    public Collection<AbstractTask<?>> findAll() {
        return taskMap.values();
    }

    public Collection<String> getUnresolvedName() {
        return unresolvedTasks.keySet();
    }

    @Getter
    public class UnresolvedTask {
        private final String taskName;
        private final Instant firstUnresolved;

        public UnresolvedTask(String taskName) {
            this.taskName = taskName;
            this.firstUnresolved = clock.now();
        }
    }
}
