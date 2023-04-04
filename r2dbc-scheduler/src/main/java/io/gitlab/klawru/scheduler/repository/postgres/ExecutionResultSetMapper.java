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
package io.gitlab.klawru.scheduler.repository.postgres;

import io.gitlab.klawru.scheduler.r2dbc.RowMapper;
import io.gitlab.klawru.scheduler.repository.ExecutionEntity;
import io.r2dbc.postgresql.codec.Json;
import io.r2dbc.spi.R2dbcException;
import io.r2dbc.spi.Row;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Optional;

@Slf4j
class ExecutionResultSetMapper implements RowMapper<ExecutionEntity> {

    public static final ExecutionResultSetMapper EXECUTION_MAPPER = new ExecutionResultSetMapper();

    @Override
    public ExecutionEntity map(Row rs) throws R2dbcException {
        String taskName = rs.get("task_name", String.class);
        String instanceId = rs.get("task_instance", String.class);
        byte[] data = Optional.ofNullable(rs.get("task_data", Json.class)).map(Json::asArray).orElse(null);
        Instant executionTime = rs.get("execution_time", Instant.class);
        boolean picked = Boolean.TRUE.equals(rs.get("picked", Boolean.class));
        final String pickedBy = rs.get("picked_by", String.class);
        Instant lastSuccess = rs.get("last_success", Instant.class);
        Instant lastFailure = rs.get("last_failure", Instant.class);
        int consecutiveFailures = Optional.ofNullable(rs.get("consecutive_failures", Integer.class)).orElse(0);
        Instant lastHeartbeat = rs.get("last_heartbeat", Instant.class);
        long version = Optional.ofNullable(rs.get("version", Long.class)).orElse(0L);

        return new ExecutionEntity(taskName,
                instanceId,
                data,
                executionTime,
                picked,
                pickedBy,
                lastSuccess,
                lastFailure,
                consecutiveFailures,
                lastHeartbeat,
                version);
    }
}