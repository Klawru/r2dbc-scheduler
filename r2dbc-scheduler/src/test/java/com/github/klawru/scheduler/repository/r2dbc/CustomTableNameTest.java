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

package com.github.klawru.scheduler.repository.r2dbc;

import com.github.klawru.scheduler.AbstractPostgresTest;
import com.github.klawru.scheduler.r2dbc.PreparedStatementSetter;
import com.github.klawru.scheduler.r2dbc.R2dbcClient;
import com.github.klawru.scheduler.repository.postgres.PostgresTaskRepository;
import com.github.klawru.scheduler.task.OneTimeTask;
import com.github.klawru.scheduler.task.instance.TaskInstance;
import com.github.klawru.scheduler.util.DataHolder;
import com.github.klawru.scheduler.util.TestTasks;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.r2dbc.core.DatabaseClient;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class CustomTableNameTest extends AbstractPostgresTest {
    static final String TABLE_NAME = "custom";
    PostgresTaskRepository repository;
    R2dbcClient r2dbcClient;
    OneTimeTask<Void> simpleJob;

    @BeforeEach
    public void setUp() {
        executeScriptFile("db_with_custom_name.sql");

        r2dbcClient = new R2dbcClient(DatabaseClient.create(this.pool));
        repository = new PostgresTaskRepository(r2dbcClient, "custom");

        simpleJob = TestTasks.oneTime("CustomTableNameTest", TestTasks.EMPTY_EXECUTION_HANDLER);
    }

    @AfterEach
    void tearDown() {
        executeScript("DROP TABLE " + TABLE_NAME);
    }

    @Test
    void customTableName() {
        TaskInstance<Void> taskInstance = simpleJob.instance("customTableName");
        //When
        repository.createIfNotExists(taskInstance, Instant.now(), DataHolder.empty())
                .block();
        //Then
        Integer count = r2dbcClient.query("SELECT COUNT(*) FROM " + TABLE_NAME, PreparedStatementSetter.NO_BIND, row -> row.get(0, Integer.class))
                .block();
        assertThat(count).isEqualTo(1);
    }
}
