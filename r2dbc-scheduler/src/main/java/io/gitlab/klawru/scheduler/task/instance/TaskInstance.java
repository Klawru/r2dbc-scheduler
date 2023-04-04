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
package io.gitlab.klawru.scheduler.task.instance;

import io.gitlab.klawru.scheduler.task.AbstractTask;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.With;
import org.springframework.lang.Nullable;

import java.time.Instant;
import java.util.function.Supplier;

@Getter
@AllArgsConstructor
public class TaskInstance<T> implements TaskInstanceId {
    protected final String id;
    @With
    protected final Supplier<T> dataSupplier;
    protected final AbstractTask<T> task;
    @With
    protected final NextExecutionTime nextExecutionTime;

    public TaskInstance(@NonNull String id, @NonNull AbstractTask<T> task, NextExecutionTime nextExecutionTime) {
        this(id, () -> null, task, nextExecutionTime);
    }

    public TaskInstance(String id, T data, AbstractTask<T> task, Instant nextExecutionTime) {
        this(id, () -> data, task, ignored -> nextExecutionTime);
    }

    @Nullable
    public T getData() {
        return dataSupplier.get();
    }


    public Instant nextExecutionTime(Instant currentTime) {
        return nextExecutionTime.nextExecutionTime(currentTime);
    }

    @Override
    public String getTaskName() {
        return task.getName();
    }

    @Override
    public String toString() {
        return getTaskNameId();
    }
}
