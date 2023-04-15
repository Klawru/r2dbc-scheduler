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

package io.gitlab.klawru.scheduler;

import io.gitlab.klawru.scheduler.stats.SchedulerMetricsRegistry;
import io.gitlab.klawru.scheduler.task.AbstractTask;
import io.gitlab.klawru.scheduler.util.*;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.proxy.ProxyConnectionFactory;
import io.r2dbc.proxy.support.QueryExecutionInfoFormatter;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.springframework.core.io.ClassPathResource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.r2dbc.spi.ConnectionFactoryOptions.*;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.testcontainers.containers.PostgreSQLContainer.POSTGRESQL_PORT;

@Slf4j
@TestInstance(PER_CLASS)
@Testcontainers
public abstract class AbstractPostgresTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13.3-alpine")
            .withLabel("app", "r2dbc-scheduler")
            .withReuse(true);
    public final TestClock testClock = new TestClock();
    @RegisterExtension
    private final AutoCloseExtension<SchedulerClient> stopSchedulerExtension = new AutoCloseExtension<>(SchedulerClient::close);
    @RegisterExtension
    private final AutoCloseExtension<ConnectionPool> stopPoolExtension = new AutoCloseExtension<>(ConnectionPool::close);
    private final TaskWaiter taskWaiter = new TaskWaiter();
    public SchedulerMetricsRegistry metricsRegistry;

    public ConnectionFactory pool;

    @BeforeEach
    void setUpPool(TestInfo testInfo) {
        metricsRegistry = new SchedulerMetricsRegistry();
        pool = createConnectionFactory();
        SqlScriptUtil.executeScriptFile("postgres_table.sql", pool);
        SqlScriptUtil.executeScript("TRUNCATE TABLE scheduled_job;", pool);
        log.info("Starting test: " + testInfo.getDisplayName());
    }

    @AfterEach
    void afterEach() {
        taskWaiter.reset();
    }


    @NotNull
    public ConnectionFactory createConnectionFactory() {
        ConnectionPool connectionFactory = (ConnectionPool) ConnectionFactories.get(builder()
                .option(DRIVER, "pool")
                .option(PROTOCOL, "postgresql")
                .option(HOST, postgres.getHost())
                .option(PORT, postgres.getMappedPort(POSTGRESQL_PORT))
                .option(USER, postgres.getUsername())
                .option(PASSWORD, postgres.getPassword())
                .option(DATABASE, postgres.getDatabaseName())
                .build());
        stopPoolExtension.add(connectionFactory);

        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter()
                .newLine()
                .showConnection().showTransaction().newLine()
                .showSuccess().showTime().newLine()
                .showType().showBatchSize().showBindingsSize().newLine()
                .showQuery().newLine()
                .showBindings();

        Logger queryLogger = org.slf4j.LoggerFactory.getLogger("io.gitlab.klawru.scheduler.query");

        return ProxyConnectionFactory.builder(connectionFactory)
                .onAfterQuery(methodExecutionInfo -> queryLogger.debug(formatter.format(methodExecutionInfo)))
                .build();
    }

    public SchedulerClient schedulerFor(AbstractTask<?>... tasks) {
        return schedulerFor("test-scheduler", tasks);
    }

    public SchedulerClient schedulerFor(String name, AbstractTask<?>... tasks) {
        return schedulerFor(name, true, tasks);
    }

    public SchedulerClient schedulerFor(String name, boolean autoClose, AbstractTask<?>... tasks) {
        var client = SchedulerBuilder.create(createConnectionFactory(), tasks)
                .schedulerConfig(schedulerConfigBuilder -> schedulerConfigBuilder
                        .schedulerName(name)
                        .threads(2)
                )
                .schedulerMetricsRegistry(metricsRegistry)
                .clock(testClock)
                .build();
        client.start();
        if (autoClose) {
            stopSchedulerExtension.add(client);
        }
        return client;
    }

    public void executeScriptFile(String path) {
        SqlScriptUtil.executeScriptResource(new ClassPathResource(path), pool);
    }

    public void executeScript(@Language("SQL") String script) {
        SqlScriptUtil.executeScript(script, pool);
    }

    public void wailAllExecutionDone(SchedulerClient schedulerClient) {
        schedulerClient.fetchTask();
        taskWaiter.waitTask(() -> metricsRegistry.getCompleteTask());
    }

}
