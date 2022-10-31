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
package com.github.klawru.scheduler.util;

import com.github.klawru.scheduler.task.callback.CompletionHandler;
import com.github.klawru.scheduler.task.schedule.Scheduler;
import com.github.klawru.scheduler.task.ExecutionHandler;
import com.github.klawru.scheduler.task.RecurringTask;
import com.github.klawru.scheduler.task.callback.DeadExecutionHandler;
import com.github.klawru.scheduler.task.callback.FailureHandler;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;

import static com.github.klawru.scheduler.task.callback.FailureHandler.DEFAULT_RETRY;
import static com.github.klawru.scheduler.task.callback.FailureHandler.DEFAULT_RETRY_INTERVAL;

@Accessors(fluent = true)
public class RecurringTaskBuilder<T> {
    private final String name;
    private final Class<T> dataClass;
    private final Scheduler schedule;
    @Setter
    private CompletionHandler<T> completionHandler;
    @Setter
    private DeadExecutionHandler<T> onDeadExecution;
    @Setter
    private FailureHandler<T> onFailure;
    @Nullable
    @Setter
    private T initData;

    public RecurringTaskBuilder(String name, Class<T> dataClass, Scheduler schedule) {
        this.name = name;
        this.dataClass = dataClass;
        this.schedule = schedule;
        this.completionHandler = new CompletionHandler.OnCompleteReschedule<>();
        this.onDeadExecution = new DeadExecutionHandler.CancelDeadExecution<>();
        this.onFailure = new FailureHandler.OnFailureRetryLater<>(DEFAULT_RETRY, DEFAULT_RETRY_INTERVAL);
    }


    public RecurringTask<T> execute(ExecutionHandler<T> executionHandler) {
        return new RecurringTask<>(name, dataClass, initData, schedule, executionHandler, completionHandler, onFailure, onDeadExecution);
    }
}
