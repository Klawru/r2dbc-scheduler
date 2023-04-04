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
package io.gitlab.klawru.scheduler.r2dbc;

import io.r2dbc.spi.Statement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.springframework.r2dbc.core.DatabaseClient;

@FunctionalInterface
public interface PreparedStatementSetter {

    PreparedStatementSetter NO_BIND = preparedStatement -> preparedStatement;

    BindTarget setParameters(BindTarget target);

    default Statement apply(Statement statement) {
        setParameters(new StatementBind(statement));
        return statement;
    }

    default DatabaseClient.GenericExecuteSpec apply(DatabaseClient.GenericExecuteSpec statement) {
        return ((DatabaseClientBind) setParameters(new DatabaseClientBind(statement))).getSpec();
    }

    @Getter
    @AllArgsConstructor
    class StatementBind implements BindTarget {

        private final Statement statement;

        @Override
        public BindTarget bind(String identifier, @NotNull Object value) {
            this.statement.bind(identifier, value);
            return this;
        }

        @Override
        public <T> BindTarget bind(String identifier, T value, Class<T> type) {
            if (value != null)
                bind(identifier, value);
            else
                bindNull(identifier, type);
            return this;
        }

        @Override
        public BindTarget bind(int index, @NotNull Object value) {
            this.statement.bind(index, value);
            return this;
        }

        @Override
        public <T> BindTarget bind(int index, T value, Class<T> type) {
            if (value != null)
                bind(index, value);
            else
                bind(index, type);
            return this;
        }

        @Override
        public BindTarget bindNull(String identifier, Class<?> type) {
            this.statement.bindNull(identifier, type);
            return this;
        }

        @Override
        public BindTarget bindNull(int index, Class<?> type) {
            this.statement.bindNull(index, type);
            return this;
        }

        @Override
        public void fetchSize(int rows) {
            this.statement.fetchSize(rows);
        }
    }

    @Getter
    @AllArgsConstructor
    class DatabaseClientBind implements BindTarget {

        private DatabaseClient.GenericExecuteSpec spec;

        @Override
        public BindTarget bind(String identifier, @NotNull Object value) {
            spec = this.spec.bind(identifier, value);
            return this;
        }

        @Override
        public <T> BindTarget bind(String identifier, T value, Class<T> type) {
            if (value != null)
                bind(identifier, value);
            else
                bindNull(identifier, type);
            return this;
        }

        @Override
        public BindTarget bind(int index, @NotNull Object value) {
            spec = this.spec.bind(index, value);
            return this;
        }

        @Override
        public <T> BindTarget bind(int index, T value, Class<T> type) {
            if (value != null)
                bind(index, value);
            else
                bindNull(index, type);
            return this;
        }

        @Override
        public BindTarget bindNull(String identifier, Class<?> type) {
            spec = this.spec.bindNull(identifier, type);
            return this;
        }

        @Override
        public BindTarget bindNull(int index, Class<?> type) {
            spec = this.spec.bindNull(index, type);
            return this;
        }

        @Override
        public void fetchSize(int rows) {
            spec = this.spec.filter(statement -> statement.fetchSize(rows));
        }
    }
}
