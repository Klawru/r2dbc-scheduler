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
package io.gitlab.klawru.scheduler.r2dbc;

import lombok.extern.slf4j.Slf4j;
import org.intellij.lang.annotations.Language;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
public class R2dbcClient {
    private final DatabaseClient databaseClient;

    public R2dbcClient(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    public <T> Mono<T> query(@Language("sql") String query, PreparedStatementSetter setParameters, RowMapper<T> rowMapper) {
        DatabaseClient.GenericExecuteSpec sql = databaseClient.sql(query);
        sql = setParameters.apply(sql);
        return sql
                .map(rowMapper::map)
                .one();
    }

    public <T> Flux<T> queryMany(@Language("sql") String query, PreparedStatementSetter setParameters, RowMapper<T> rowMapper) {
        DatabaseClient.GenericExecuteSpec sql = databaseClient.sql(query);
        sql = setParameters.apply(sql);
        return sql
                .map(rowMapper::map)
                .all();
    }

    public Mono<Integer> execute(@Language("sql") String query, PreparedStatementSetter setParameters) {
        DatabaseClient.GenericExecuteSpec sql = databaseClient.sql(query);
        sql = setParameters.apply(sql);
        return sql
                .fetch().rowsUpdated();
    }

}
