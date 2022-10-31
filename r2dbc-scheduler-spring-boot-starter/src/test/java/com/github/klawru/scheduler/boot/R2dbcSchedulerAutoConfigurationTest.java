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

package com.github.klawru.scheduler.boot;

import com.github.klawru.scheduler.SchedulerClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class R2dbcSchedulerAutoConfigurationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:13.3-alpine")
            .withLabel("app", "r2dbc-scheduler")
            .withReuse(true);

    private ApplicationContextRunner contextRunner;

    @BeforeEach
    void beforeEach() {
        var initializer = new ConditionEvaluationReportLoggingListener(LogLevel.INFO);
        contextRunner = new ApplicationContextRunner()
                .withInitializer(initializer)
                .withPropertyValues("spring.application.name=r2dbc-scheduler-test",
                        String.format("spring.r2dbc.url=r2dbc:postgres://%s:%d/%s", POSTGRES.getHost(), POSTGRES.getFirstMappedPort(), POSTGRES.getDatabaseName()),
                        "spring.r2dbc.username=" + POSTGRES.getUsername(),
                        "spring.r2dbc.password=" + POSTGRES.getUsername(),
                        "r2dbc-scheduler.schedulerName=${spring.application.name}"
                )
                .withConfiguration(AutoConfigurations.of(
                        R2dbcSchedulerAutoConfiguration.class,
                        R2dbcAutoConfiguration.class,
                        SqlInitializationAutoConfiguration.class
                ));
    }

    @Test
    void checkAutoConfiguration() {
        contextRunner.run(context -> {
            assertThat(context)
                    .hasNotFailed()
                    .hasSingleBean(SchedulerClient.class);
        });
    }

    @Test
    void checkAutoConfigurationWhenDisabled() {
        contextRunner
                .withPropertyValues("r2dbc-scheduler.enabled=false")
                .run(context -> {
                    assertThat(context)
                            .hasNotFailed()
                            .doesNotHaveBean(SchedulerClient.class);
                });
    }

}