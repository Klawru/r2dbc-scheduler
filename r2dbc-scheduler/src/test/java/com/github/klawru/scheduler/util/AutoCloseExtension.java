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

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.ArrayList;
import java.util.function.Consumer;

public class AutoCloseExtension<T> implements AfterEachCallback {

    private final Consumer<T> clientConsumer;
    private final ArrayList<T> clients = new ArrayList<>();

    public AutoCloseExtension(Consumer<T> clientConsumer) {
        this.clientConsumer = clientConsumer;
    }

    public void add(T client) {
        clients.add(client);
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception {
        clients.forEach(clientConsumer);
        clients.clear();
    }
}
