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


import io.gitlab.klawru.scheduler.executor.Execution;
import io.gitlab.klawru.scheduler.task.callback.CompletionHandler;
import io.gitlab.klawru.scheduler.task.callback.FailureHandler;
import io.gitlab.klawru.scheduler.ExecutionOperations;
import io.gitlab.klawru.scheduler.task.callback.DeadExecutionHandler;
import io.gitlab.klawru.scheduler.task.instance.TaskInstance;
import lombok.Getter;
import lombok.NonNull;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.function.Supplier;


public abstract class AbstractTask<T> implements ExecutionHandler<T> {
    @Getter
    final String name;
    @Getter
    final Class<T> dataClass;
    final ExecutionHandler<T> executionHandler;
    final CompletionHandler<T> completionHandler;
    final FailureHandler<T> failureHandler;
    final DeadExecutionHandler<T> deadExecutionHandler;

    protected AbstractTask(
            @NonNull String name,
            @NonNull Class<T> dataClass,
            @NonNull ExecutionHandler<T> executionHandler,
            @NonNull CompletionHandler<T> completionHandler,
            @NonNull FailureHandler<T> failureHandler,
            @NonNull DeadExecutionHandler<T> deadExecutionHandler) {
        this.name = name;
        this.dataClass = dataClass;
        this.executionHandler = executionHandler;
        this.completionHandler = completionHandler;
        this.failureHandler = failureHandler;
        this.deadExecutionHandler = deadExecutionHandler;
    }

    public abstract TaskInstance<T> instance(String id);

    public abstract TaskInstance<T> instance(String id, Supplier<T> data);

    public TaskInstance<T> instance(String id, T data) {
        return instance(id, () -> data);
    }


    public Mono<Void> onComplete(Execution<? super T> executionComplete, Optional<? super T> newData, ExecutionOperations executionOperations) {
        return completionHandler.complete(executionComplete, newData, executionOperations);
    }

    public Mono<Void> onDeadExecution(Execution<? super T> execution, ExecutionOperations executionOperations) {
        return deadExecutionHandler.deadExecution(execution, executionOperations);
    }

    public Mono<Void> onFailure(Execution<? super T> executionComplete, ExecutionOperations executionOperations) {
        return failureHandler.onFailure(executionComplete, executionOperations);
    }

    @Override
    public Mono<T> execute(TaskInstance<T> taskInstance, ExecutionContext<T> context) {
        return executionHandler.execute(taskInstance, context);
    }
}
