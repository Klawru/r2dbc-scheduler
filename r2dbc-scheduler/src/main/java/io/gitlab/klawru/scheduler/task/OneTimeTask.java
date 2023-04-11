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
package io.gitlab.klawru.scheduler.task;

import io.gitlab.klawru.scheduler.task.callback.CompletionHandler;
import io.gitlab.klawru.scheduler.task.callback.DeadExecutionHandler;
import io.gitlab.klawru.scheduler.task.callback.FailureHandler;
import io.gitlab.klawru.scheduler.task.instance.NextExecutionTime;
import io.gitlab.klawru.scheduler.task.instance.TaskInstance;

import java.util.function.Supplier;


public class OneTimeTask<T> extends AbstractTask<T> {
    public OneTimeTask(String name,
                       Class<T> dataClass,
                       ExecutionHandler<T> executionHandler,
                       CompletionHandler<T> completionHandler,
                       FailureHandler<T> failureHandler,
                       DeadExecutionHandler<T> deadExecutionHandler) {
        super(name, dataClass, executionHandler, completionHandler, failureHandler, deadExecutionHandler);
    }

    @Override
    public TaskInstance<T> instance(String id) {
        return new TaskInstance<>(id, this, NextExecutionTime.now());
    }

    @Override
    public TaskInstance<T> instance(String id, Supplier<T> data) {
        return new TaskInstance<>(id, data, this, NextExecutionTime.now());
    }

}
