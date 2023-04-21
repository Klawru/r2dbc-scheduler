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
package io.gitlab.klawru.scheduler.repository;

import io.gitlab.klawru.scheduler.executor.Execution;
import io.gitlab.klawru.scheduler.executor.execution.state.AbstractExecutionState;
import io.gitlab.klawru.scheduler.task.AbstractTask;
import io.gitlab.klawru.scheduler.task.instance.TaskInstance;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;


public class ExecutionMapper {

    @NotNull
    public <T> Execution<T> mapToExecution(ExecutionEntity executionEntity,
                                           AbstractExecutionState state,
                                           AbstractTask<T> abstractTask,
                                           Supplier<T> dataSupplier) {
        TaskInstance<T> taskInstance = abstractTask.instance(executionEntity.getInstanceId(), dataSupplier);
        return new Execution<>(taskInstance,
                state,
                executionEntity.getExecutionTime(),
                executionEntity.isPicked(),
                executionEntity.getPickedBy(),
                executionEntity.getConsecutiveFailures(),
                executionEntity.getLastHeartbeat(),
                executionEntity.getVersion(),
                executionEntity.getLastFailure(),
                executionEntity.getLastSuccess());
    }
}
