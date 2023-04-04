/*
 * Copyright © Klawru
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

package io.gitlab.klawru.scheduler.boot;

import io.gitlab.klawru.scheduler.SchedulerClient;
import io.gitlab.klawru.scheduler.TaskResolver;
import io.gitlab.klawru.scheduler.executor.DefaultTaskSchedulers;
import io.gitlab.klawru.scheduler.executor.ExecutorService;
import io.gitlab.klawru.scheduler.executor.TaskSchedulers;
import io.gitlab.klawru.scheduler.r2dbc.R2dbcClient;
import io.gitlab.klawru.scheduler.repository.ExecutionMapper;
import io.gitlab.klawru.scheduler.repository.R2dbcTaskService;
import io.gitlab.klawru.scheduler.repository.TaskRepository;
import io.gitlab.klawru.scheduler.repository.TaskService;
import io.gitlab.klawru.scheduler.repository.postgres.PostgresTaskRepository;
import io.gitlab.klawru.scheduler.repository.serializer.JacksonSerializer;
import io.gitlab.klawru.scheduler.repository.serializer.Serializer;
import io.gitlab.klawru.scheduler.stats.SchedulerMetricsRegistry;
import io.gitlab.klawru.scheduler.task.AbstractTask;
import io.gitlab.klawru.scheduler.util.Clock;
import io.gitlab.klawru.scheduler.util.SchedulerBuilder;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.r2dbc.core.DatabaseClient;

import java.util.List;

@EnableConfigurationProperties(R2dbcSchedulerProperties.class)
@AutoConfigureAfter({R2dbcAutoConfiguration.class, SqlInitializationAutoConfiguration.class})
@ConditionalOnBean(ConnectionFactory.class)
@ConditionalOnProperty(value = "r2dbc-scheduler.enabled", matchIfMissing = true)
public class R2dbcSchedulerAutoConfiguration {
    private final ConnectionFactory connectionFactory;
    private final R2dbcSchedulerProperties schedulerConfig;

    public R2dbcSchedulerAutoConfiguration(ConnectionFactory connectionFactory, R2dbcSchedulerProperties schedulerConfig, ObjectProvider<SchedulerConfigCustomizer> configCustomizers) {
        this.connectionFactory = connectionFactory;
        this.schedulerConfig = schedulerConfig;
        configCustomizers.ifAvailable(schedulerConfigCustomizer -> schedulerConfigCustomizer.customize(schedulerConfig));
    }

    @Bean
    @ConditionalOnMissingBean
    Clock clockScheduler() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnMissingBean
    SchedulerMetricsRegistry schedulerMetricsRegistry() {
        return new SchedulerMetricsRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    TaskResolver taskResolver(List<AbstractTask<?>> abstractTasks, SchedulerMetricsRegistry schedulerMetricsRegistry, Clock clock) {
        return new TaskResolver(abstractTasks, schedulerMetricsRegistry, clock);
    }

    @Bean
    @ConditionalOnClass(name = "io.r2dbc.postgresql.PostgresqlConnectionFactory")
    @ConditionalOnMissingBean
    TaskRepository postgresTaskRepository(DatabaseClient databaseClient) {
        R2dbcClient client = new R2dbcClient(databaseClient);
        return new PostgresTaskRepository(client, schedulerConfig.getTableName());
    }

    @Bean
    @ConditionalOnMissingBean
    Serializer serializer() {
        return new JacksonSerializer();
    }

    @Bean
    @ConditionalOnMissingBean
    ExecutionMapper executionMapper() {
        return new ExecutionMapper();
    }

    @Bean
    @ConditionalOnMissingBean(TaskService.class)
    TaskService r2dbcTaskService(TaskRepository taskRepository,
                                 TaskResolver taskResolver,
                                 ExecutionMapper executionMapper,
                                 Clock clock,
                                 Serializer serializer) {
        return new R2dbcTaskService(taskRepository,
                taskResolver,
                executionMapper,
                schedulerConfig.getSchedulerName(),
                clock,
                serializer);
    }

    @Bean
    @ConditionalOnMissingBean
    TaskSchedulers taskSchedulers() {
        return new DefaultTaskSchedulers(schedulerConfig);
    }


    @Bean
    @ConditionalOnMissingBean
    ExecutorService executorService(TaskSchedulers taskSchedulers, SchedulerMetricsRegistry schedulerMetricsRegistry) {
        return new ExecutorService(taskSchedulers, schedulerMetricsRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    SchedulerClient schedulerClient(
            TaskResolver taskResolver,
            TaskService r2dbcTaskService,
            ExecutorService executorService,
            SchedulerMetricsRegistry schedulerMetricsRegistry,
            Clock clock) {
        return SchedulerBuilder.create(connectionFactory)
                .setSchedulerConfig(schedulerConfig.toConfig())
                .taskResolver(taskResolver)
                .taskService(r2dbcTaskService)
                .executor(executorService)
                .schedulerMetricsRegistry(schedulerMetricsRegistry)
                .clock(clock)
                .build();
    }

}
