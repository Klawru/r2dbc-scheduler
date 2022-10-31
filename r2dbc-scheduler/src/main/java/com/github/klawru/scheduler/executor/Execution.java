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
package com.github.klawru.scheduler.executor;

import com.github.klawru.scheduler.ExecutionOperations;
import com.github.klawru.scheduler.exception.DeadExecutioneException;
import com.github.klawru.scheduler.executor.execution.state.*;
import com.github.klawru.scheduler.task.instance.TaskInstance;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;


@Getter
@RequiredArgsConstructor
public class Execution<T> {
    protected final TaskInstance<T> taskInstance;
    protected final ExecutionStateMachine states;
    protected final Instant executionTime;

    protected final boolean picked;
    protected final String pickedBy;
    protected final int consecutiveFailures;
    protected final Instant lastHeartbeat;

    protected final long version;
    protected final Instant lastFailure;
    protected final Instant lastSuccess;


    @SuppressWarnings("java:S107")
    public Execution(TaskInstance<T> taskInstance, ExecutionState state, Instant executionTime, boolean picked, String pickedBy, int consecutiveFailures, Instant lastHeartbeat, long version, Instant lastFailure, Instant lastSuccess) {
        this.taskInstance = taskInstance;
        this.executionTime = executionTime;
        this.picked = picked;
        this.pickedBy = pickedBy;
        this.consecutiveFailures = consecutiveFailures;
        this.lastHeartbeat = lastHeartbeat;
        this.version = version;
        this.lastFailure = lastFailure;
        this.lastSuccess = lastSuccess;
        this.states = new ExecutionStateMachine(state);
    }

    public EnqueuedState enqueued(UUID enqueuedId) {
        return states.changeState(new EnqueuedState(enqueuedId));
    }

    public ProcessedState processed() {
        return states.changeState(new ProcessedState());
    }

    public CompleteState complete() {
        return states.changeState(new CompleteState());
    }

    public FailedState failed(Throwable cause) {
        return states.changeState(new FailedState(cause));
    }


    public ExecutionState currentState() {
        return states.currentState();
    }

    public ExecutionState getLastState() {
        return states.getLastState();
    }

    public Optional<ExecutionState> getLastState(ExecutionStateName nameState) {
        return states.getLastState(nameState);
    }

    public Mono<Void> onComplete(Optional<T> newData, ExecutionOperations executionOperations) {
        return taskInstance.getTask().onComplete(this, newData, executionOperations);
    }

    public Mono<Void> onDeadExecution(ExecutionOperations executionOperations) {
        return taskInstance.getTask().onDeadExecution(this, executionOperations);
    }

    public Mono<Void> onFailure(ExecutionOperations executionOperations) {
        return taskInstance.getTask().onFailure(this, executionOperations);
    }


    @Override
    public String toString() {
        return "Execution{" +
                "taskInstance=" + taskInstance +
                ", states=" + states +
                ", executionTime=" + executionTime +
                ", picked=" + picked +
                ", pickedBy='" + pickedBy + '\'' +
                ", consecutiveFailures=" + consecutiveFailures +
                ", lastHeartbeat=" + lastHeartbeat +
                ", version=" + version +
                ", lastFailure=" + lastFailure +
                ", lastSuccess=" + lastSuccess +
                '}';
    }

}
