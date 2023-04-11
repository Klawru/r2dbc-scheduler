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
import io.gitlab.klawru.scheduler.TaskScheduler;
import io.gitlab.klawru.scheduler.task.AbstractTask;
import io.gitlab.klawru.scheduler.util.SchedulerBuilder;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

@EnableConfigurationProperties(R2dbcSchedulerProperties.class)
@AutoConfigureAfter({R2dbcAutoConfiguration.class, SqlInitializationAutoConfiguration.class})
@ConditionalOnBean(ConnectionFactory.class)
@ConditionalOnProperty(value = "r2dbc-scheduler.enabled", matchIfMissing = true)
public class R2dbcSchedulerAutoConfiguration {
    private final ConnectionFactory connectionFactory;
    private final R2dbcSchedulerProperties schedulerConfig;

    public R2dbcSchedulerAutoConfiguration(ConnectionFactory connectionFactory, R2dbcSchedulerProperties schedulerConfig) {
        this.connectionFactory = connectionFactory;
        this.schedulerConfig = schedulerConfig;
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    SchedulerClient schedulerClient(
            ObjectProvider<SchedulerConfigCustomizer> configCustomizers,
            ObjectProvider<SchedulerBuilderCustomizer> customizers,
            List<AbstractTask<?>> tasks) {
        configCustomizers.orderedStream()
                .forEach(schedulerConfigCustomizer -> schedulerConfigCustomizer.customize(schedulerConfig));

        SchedulerBuilder builder = SchedulerBuilder.create(connectionFactory)
                .tasks(tasks)
                .setSchedulerConfig(schedulerConfig.toConfig());

        customizers.orderedStream().forEach(customizer -> customizer.customize(builder));
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    SchedulerStarter schedulerContextReadyStarter(SchedulerClient schedulerClient) {
        return new SchedulerContextReadyStarter(schedulerClient);
    }

    @Bean
    @ConditionalOnMissingBean
    TaskScheduler taskScheduler(SchedulerClient schedulerClient) {
        return new TaskScheduler(schedulerClient);
    }

}
