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
package com.github.klawru.scheduler.util;

import com.github.klawru.scheduler.DefaultSchedulerClient;
import com.github.klawru.scheduler.SchedulerClient;
import com.github.klawru.scheduler.TaskResolver;
import com.github.klawru.scheduler.config.SchedulerConfig;
import com.github.klawru.scheduler.executor.DefaultTaskSchedulers;
import com.github.klawru.scheduler.executor.ExecutorService;
import com.github.klawru.scheduler.executor.TaskExecutor;
import com.github.klawru.scheduler.r2dbc.R2dbcClient;
import com.github.klawru.scheduler.repository.ExecutionMapper;
import com.github.klawru.scheduler.repository.R2dbcTaskService;
import com.github.klawru.scheduler.repository.TaskRepository;
import com.github.klawru.scheduler.repository.TaskService;
import com.github.klawru.scheduler.repository.postgres.PostgresTaskRepository;
import com.github.klawru.scheduler.repository.serializer.JacksonSerializer;
import com.github.klawru.scheduler.repository.serializer.Serializer;
import com.github.klawru.scheduler.stats.SchedulerMetricsRegistry;
import com.github.klawru.scheduler.task.AbstractTask;
import io.r2dbc.spi.ConnectionFactory;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.r2dbc.core.DatabaseClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;

@Setter
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SchedulerBuilder {
    Clock clock;
    ConnectionFactory connectionFactory;

    ArrayList<AbstractTask<?>> tasks;
    TaskResolver taskResolver;

    R2dbcClient r2dbcClient;
    TaskService taskService;

    ExecutionMapper executionMapper;
    Serializer serializer;
    TaskRepository taskRepository;

    DefaultTaskSchedulers taskSchedulers;
    TaskExecutor executor;

    SchedulerConfig.SchedulerConfigBuilder schedulerConfig;

    SchedulerMetricsRegistry schedulerMetricsRegistry;

    public static SchedulerBuilder create(ConnectionFactory connectionFactory, AbstractTask<?>... tasks) {
        Clock clock = new SystemClock();
        ArrayList<AbstractTask<?>> taskList = new ArrayList<>(Arrays.asList(tasks));

        return new SchedulerBuilder()
                .clock(clock)
                .connectionFactory(connectionFactory)
                .setSchedulerConfig(SchedulerConfig.builder())
                .schedulerMetricsRegistry(new SchedulerMetricsRegistry())
                .tasks(taskList);
    }

    public SchedulerBuilder setSchedulerConfig(SchedulerConfig.SchedulerConfigBuilder schedulerConfig) {
        this.schedulerConfig = schedulerConfig;
        return this;
    }

    public SchedulerBuilder schedulerConfig(Consumer<SchedulerConfig.SchedulerConfigBuilder> customization) {
        customization.accept(this.schedulerConfig);
        return this;
    }

    public SchedulerClient build() {
        SchedulerConfig config = schedulerConfig.build();

        if (r2dbcClient == null)
            r2dbcClient = new R2dbcClient(DatabaseClient.builder()
                    .connectionFactory(connectionFactory)
                    .namedParameters(true)
                    .build());
        if (taskResolver != null)
            taskResolver.add(tasks);
        else
            taskResolver = new TaskResolver(tasks, schedulerMetricsRegistry, clock);
        if (taskRepository == null)
            taskRepository = new PostgresTaskRepository(r2dbcClient, config.getTableName());
        if (executionMapper == null)
            executionMapper = new ExecutionMapper();
        if (serializer == null)
            serializer = new JacksonSerializer();
        if (taskService == null)
            taskService = new R2dbcTaskService(taskRepository, taskResolver, executionMapper, config.getSchedulerName(), clock, serializer);
        if (taskSchedulers == null)
            taskSchedulers = new DefaultTaskSchedulers(config);
        if (executor == null)
            executor = new ExecutorService(taskSchedulers, schedulerMetricsRegistry);
        return new DefaultSchedulerClient(taskService, executor, taskSchedulers, schedulerMetricsRegistry, config, clock);
    }
}
