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
import com.github.klawru.scheduler.exception.AbstractSchedulerException;
import com.github.klawru.scheduler.executor.execution.state.EnqueuedState;
import com.github.klawru.scheduler.executor.execution.state.ExecutionState;
import com.github.klawru.scheduler.stats.SchedulerMetricsRegistry;
import com.github.klawru.scheduler.task.AbstractTask;
import com.github.klawru.scheduler.task.ExecutionContext;
import com.github.klawru.scheduler.task.instance.TaskInstance;
import com.github.klawru.scheduler.util.MapperUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.github.klawru.scheduler.executor.execution.state.ExecutionStateName.ENQUEUED;


@Slf4j
@RequiredArgsConstructor
public class ExecutorService implements TaskExecutor {
    private final TaskSchedulers schedulers;
    private final SchedulerMetricsRegistry registry;

    private final AtomicInteger currentlyInQueueOrProcessing = new AtomicInteger(0);
    private final ConcurrentHashMap<UUID, Pair<Execution<?>, ExecutionSubscriber<?>>> currentlyProcessing = new ConcurrentHashMap<>();


    @Override
    public <T> void addToQueue(Execution<T> execution, ExecutionContext<T> context, ExecutionOperations executionOperations) {
        TaskInstance<T> taskInstance = execution.getTaskInstance();
        log.debug("Add queue task {}", taskInstance);
        ExecutionSubscriber<Void> executionSubscriber = addCurrentlyProcessing(execution);
        Mono.defer(() -> {
                    execution.processed();
                    AbstractTask<T> task = taskInstance.getTask();
                    //Job execute
                    return task.execute(taskInstance, context)
                            .as(MapperUtil::mapToOptional)
                            .flatMap(newData -> catchComplete(execution, newData, executionOperations));
                })
                .onErrorResume(throwable -> catchFailure(execution, executionOperations, throwable))
                //Finally
                .subscribeOn(schedulers.getTaskScheduler())
                .subscribe(executionSubscriber);
    }

    @Override
    public void removeFromQueue(Execution<?> execution) {
        stopExecution(execution);
    }

    private void stopExecution(Execution<?> execution) {
        Optional<ExecutionState> lastState = execution.getLastState(ENQUEUED);
        if (lastState.isPresent()) {
            ExecutionState executionState = lastState.get();
            UUID enqueuedId = ((EnqueuedState) executionState).getEnqueuedId();
            Pair<Execution<?>, ExecutionSubscriber<?>> executionSubscriberPair = currentlyProcessing.get(enqueuedId);
            ExecutionSubscriber<?> subscriber = executionSubscriberPair.getSecond();
            subscriber.cancel();
        } else {
            log.info("Cannot stop execution={} not found state ENQUEUED", execution.getTaskInstance());
        }
    }

    private <T> Mono<Void> catchFailure(Execution<T> execution, ExecutionOperations executionOperations, Throwable throwable) {
        return Mono.defer(() -> {
            TaskInstance<T> taskInstance = execution.getTaskInstance();
            log.error("Task '{}' failure", taskInstance.getTaskNameId(), throwable);
            if (throwable instanceof AbstractSchedulerException) {
                //Ignoring internal errors
                return Mono.empty();
            }
            execution.failed(throwable);
            return execution.onFailure(executionOperations);
        });
    }

    private <T> Mono<Void> catchComplete(Execution<T> execution, Optional<T> newData, ExecutionOperations executionOperations) {
        return Mono.defer(() -> {
            TaskInstance<T> taskInstance = execution.getTaskInstance();
            log.debug("Task '{}' complete", taskInstance.getTaskNameId());

            execution.complete();
            return execution.onComplete(newData, executionOperations);
        });
    }

    private <T> ExecutionSubscriber<Void> addCurrentlyProcessing(Execution<T> execution) {
        UUID executionId = UUID.randomUUID();
        var executionSubscriber = new ExecutionSubscriber<Void>(executionId, this::removeCurrentlyProcessing);
        currentlyInQueueOrProcessing.incrementAndGet();
        currentlyProcessing.put(executionId, Pair.of(execution, executionSubscriber));
        execution.enqueued(executionId);
        return executionSubscriber;
    }

    private void removeCurrentlyProcessing(UUID executionId) {
        var execution = currentlyProcessing.remove(executionId);
        if (execution == null) {
            log.warn("Released execution was not found in collection of executions currently being processed. Should never happen. Execution-id: " + executionId);
        } else {
            registry.afterExecution(execution.getFirst());
        }
        currentlyInQueueOrProcessing.decrementAndGet();
    }

    @Override
    public void stop(Duration wait) {
        currentlyProcessing.values().stream()
                .map(Pair::getFirst)
                .forEach(this::stopExecution);
    }

    @Override
    public int getNumberInQueueOrProcessing() {
        return currentlyInQueueOrProcessing.get();
    }

    @Override
    public int taskUpperLimit() {
        return schedulers.getTaskUpperLimit();
    }

    @Override
    public int taskLowerLimit() {
        return schedulers.getTaskLowerLimit();
    }

    @Override
    public Stream<Execution<?>> currentlyExecuting() {
        return currentlyProcessing.values().stream()
                .map(Pair::getFirst);
    }

    private static class ExecutionSubscriber<T> extends BaseSubscriber<T> {
        private final UUID id;
        private final Consumer<UUID> hookFinally;

        public ExecutionSubscriber(UUID id, Consumer<UUID> hookFinally) {
            this.id = id;
            this.hookFinally = hookFinally;
        }

        @Override
        protected void hookFinally(SignalType type) {
            super.hookFinally(type);
            hookFinally.accept(id);
        }
    }
}
