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
package io.gitlab.klawru.scheduler.util;

import io.gitlab.klawru.scheduler.task.ExecutionHandler;
import io.gitlab.klawru.scheduler.task.OneTimeTask;
import io.gitlab.klawru.scheduler.task.callback.CompletionHandler;
import io.gitlab.klawru.scheduler.task.callback.DeadExecutionHandler;
import io.gitlab.klawru.scheduler.task.callback.FailureHandler;

public class OneTimeTaskBuilder<T> {
    private final String name;
    private final Class<T> dataClass;
    private CompletionHandler<T> completionHandler;
    private DeadExecutionHandler<T> onDeadExecution;
    private FailureHandler<T> onFailure;


    public OneTimeTaskBuilder(String name, Class<T> dataClass) {
        this.name = name;
        this.dataClass = dataClass;
        this.completionHandler = new CompletionHandler.OnCompleteRemove<>();
        this.onDeadExecution = new DeadExecutionHandler.CancelDeadExecution<>();
        this.onFailure = new FailureHandler.OnFailureRetryLater<>(FailureHandler.DEFAULT_RETRY, FailureHandler.DEFAULT_RETRY_INTERVAL);
    }

    public OneTimeTaskBuilder<T> onComplete(CompletionHandler<T> completionHandler) {
        this.completionHandler = completionHandler;
        return this;
    }

    public OneTimeTaskBuilder<T> onFailure(FailureHandler<T> failureHandler) {
        this.onFailure = failureHandler;
        return this;
    }

    public OneTimeTaskBuilder<T> onDeadExecution(DeadExecutionHandler<T> deadExecutionHandler) {
        this.onDeadExecution = deadExecutionHandler;
        return this;
    }

    public OneTimeTask<T> execute(ExecutionHandler<T> executionHandler) {
        return new OneTimeTask<>(name, dataClass, executionHandler, completionHandler, onFailure, onDeadExecution);
    }
}
