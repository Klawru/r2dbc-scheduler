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
package com.github.klawru.scheduler;

import com.github.klawru.scheduler.executor.Execution;
import com.github.klawru.scheduler.exception.ExecutionException;
import com.github.klawru.scheduler.executor.execution.state.ExecutionState;
import com.github.klawru.scheduler.repository.R2dbcTaskService;
import com.github.klawru.scheduler.repository.TaskService;
import com.github.klawru.scheduler.task.instance.NextExecutionTime;
import com.github.klawru.scheduler.util.DataHolder;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.Instant;


@RequiredArgsConstructor(staticName = "of")
public class DefaultExecutionOperations implements ExecutionOperations {

    private final TaskService taskRepository;
    private final Runnable callback;

    public static DefaultExecutionOperations of(R2dbcTaskService taskService) {
        return of(taskService, () -> {
        });
    }

    @Override
    public Mono<Void> remove(Execution<?> completed) {
        return taskRepository.remove(completed)
                .doFinally(signalType -> callback.run());
    }

    @Override
    public Mono<Void> reschedule(Execution<?> execution) {
        return reschedule(execution, execution.getTaskInstance().getNextExecutionTime());
    }

    @Override
    public Mono<Void> reschedule(Execution<?> execution, Instant nextExecutionTime) {
        return reschedule(execution, now -> nextExecutionTime);
    }

    @Override
    public <T> Mono<Void> reschedule(Execution<? super T> execution, NextExecutionTime nextExecutionTime) {
        ExecutionState executionState = execution.currentState();
        switch (executionState.getName()) {
            case VIEW:
            case FAILED:
            case COMPLETE:
            case DEAD_EXECUTION:
                return taskRepository.reschedule(execution, nextExecutionTime, DataHolder.empty())
                        .doFinally(signalType -> callback.run());
            default:
                return Mono.error(() ->
                        new ExecutionException("Try reschedule execution in wrong state " + executionState.getName(), execution)
                );
        }
    }
}