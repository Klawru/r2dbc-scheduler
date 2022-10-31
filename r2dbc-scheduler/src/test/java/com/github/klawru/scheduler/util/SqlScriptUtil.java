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

package com.github.klawru.scheduler.util;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import lombok.experimental.UtilityClass;
import org.intellij.lang.annotations.Language;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.r2dbc.connection.init.ScriptUtils;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@UtilityClass
public class SqlScriptUtil {

    public static void executeScriptFile(String path, ConnectionFactory pool) {
        executeScriptResource(new ClassPathResource(path), pool);
    }

    public static void executeScript(@Language("SQL") String script, ConnectionFactory pool) {
        executeScriptResource(new ByteArrayResource(script.getBytes(StandardCharsets.UTF_8)), pool);
    }

    public static void executeScriptResource(Resource resource, ConnectionFactory pool) {
        Mono.usingWhen(
                pool.create(),
                connection -> ScriptUtils.executeSqlScript(connection, resource),
                Connection::close
        ).block();
    }
}
