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
package com.github.klawru.scheduler.r2dbc;

import io.r2dbc.spi.R2dbcException;
import io.r2dbc.spi.Row;

import java.util.function.Function;

@FunctionalInterface
public interface RowMapper<T> extends Function<Row, T> {
    T map(Row row) throws R2dbcException;

    @Override
    default T apply(Row row) {
        return map(row);
    }

}
