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

import io.gitlab.klawru.scheduler.DefaultExecutionOperations;
import io.gitlab.klawru.scheduler.TaskResolver;
import io.gitlab.klawru.scheduler.exception.ExecutionException;
import io.gitlab.klawru.scheduler.exception.TaskServiceException;
import io.gitlab.klawru.scheduler.executor.Execution;
import io.gitlab.klawru.scheduler.executor.execution.state.DeadExecutionState;
import io.gitlab.klawru.scheduler.executor.execution.state.ExecutionState;
import io.gitlab.klawru.scheduler.executor.execution.state.PickedState;
import io.gitlab.klawru.scheduler.executor.execution.state.ViewState;
import io.gitlab.klawru.scheduler.repository.serializer.Serializer;
import io.gitlab.klawru.scheduler.task.AbstractTask;
import io.gitlab.klawru.scheduler.task.callback.ScheduleOnStartup;
import io.gitlab.klawru.scheduler.task.instance.NextExecutionTime;
import io.gitlab.klawru.scheduler.task.instance.TaskInstance;
import io.gitlab.klawru.scheduler.task.instance.TaskInstanceId;
import io.gitlab.klawru.scheduler.util.Clock;
import io.gitlab.klawru.scheduler.util.DataHolder;
import io.gitlab.klawru.scheduler.util.MapperUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;


@Slf4j
public class R2dbcTaskService implements TaskService, Closeable {
    private final TaskRepository repository;

    public R2dbcTaskService(TaskRepository repository, TaskResolver taskResolver, ExecutionMapper executionMapper,
                            String schedulerName, Clock clock, Serializer serializer) {
        this.repository = repository;
        this.taskResolver = taskResolver;
        this.executionMapper = executionMapper;
        this.schedulerName = Objects.requireNonNull(schedulerName, "schedulerName must not be null");
        this.clock = clock;
        this.serializer = serializer;
    }

    @Getter
    private final TaskResolver taskResolver;
    private final ExecutionMapper executionMapper;
    private final String schedulerName;
    private final Clock clock;
    private final Serializer serializer;

    @Override
    public <T> Mono<Void> createIfNotExists(TaskInstanceId execution, NextExecutionTime nextExecutionTime, DataHolder<T> dataHolder) {
        return repository.createIfNotExists(execution,
                nextExecutionTime.nextExecutionTime(clock.now()),
                dataHolder.map(serializer::serialize));
    }

    @Override
    public Flux<Execution<?>> lockAndGetDue(int limit) {
        if (limit <= 0)
            return Flux.empty();
        return repository.lockAndGetDue(schedulerName, clock.now(), limit, taskResolver.getUnresolvedName())
                .map(this::findTaskForPick)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    @NotNull
    private <T> Execution<T> mapForPick(ExecutionEntity executionEntity, AbstractTask<T> task) {
        Supplier<T> dataSupplier = MapperUtil.memoize(() -> serializer.deserialize(task.getDataClass(), executionEntity.getData()));
        if (executionEntity.isPicked() && !schedulerName.equals(executionEntity.getPickedBy()))
            throw new TaskServiceException("The task is not accepted by the current scheduler");
        return executionMapper.mapToExecution(executionEntity, new PickedState(), task, dataSupplier);
    }

    @Override
    public Mono<Void> remove(Execution<?> execution) {
        return repository.remove(execution.getTaskInstance(), execution.getVersion());
    }

    @Override
    public Mono<Boolean> updateHeartbeat(Execution<?> execution) {
        return repository.updateHeartbeat(execution.getTaskInstance(), execution.getVersion(), clock.now())
                .flatMap(updated -> {
                    if (updated != 1)
                        log.warn("Did not update heartbeat or multiple row updated on '{}' updatedRow={}", execution.getTaskInstance().getTaskNameId(), updated);
                    return Mono.just(updated > 0);
                });
    }

    @Override
    public Mono<Integer> removeExecutions(String taskName) {
        return repository.removeAllExecutions(taskName);
    }

    @SuppressWarnings("unchecked")
    public <T> Mono<Void> reschedule(TaskInstanceId taskInstanceId, NextExecutionTime nextExecutionTime, DataHolder<T> newData) {
        return repository.getExecution(taskInstanceId)
                .map(this::findTaskForView)
                .as(MapperUtil::get)
                .flatMap(execution -> {
                    Class<?> dataClass = execution.getTaskInstance().getTask().getDataClass();
                    if (newData.isEmpty() || newData.getData() == null || dataClass.isAssignableFrom(newData.getData().getClass()))
                        return reschedule((Execution<T>) execution, nextExecutionTime, newData);
                    else {
                        log.warn("Error on reschedule task '{}': type mismatch", taskInstanceId.getTaskNameId());
                        return Mono.error(new TaskServiceException("Error on reschedule task '" + taskInstanceId.getTaskName() + "': type mismatch"));
                    }
                });
    }

    public <T> Mono<Void> reschedule(Execution<T> execution, NextExecutionTime nextExecutionTime, DataHolder<T> dataSupplier) {
        ExecutionState executionState = execution.currentState();
        Instant executionTime = nextExecutionTime.nextExecutionTime(clock.now());
        DataHolder<byte[]> serializedData = dataSupplier.map(serializer::serialize);
        switch (executionState.getName()) {
            case VIEW:
                return repository.reschedule(execution.getTaskInstance(),
                        execution.getVersion(),
                        executionTime,
                        serializedData,
                        execution.getLastSuccess(),
                        execution.getLastFailure(),
                        execution.getConsecutiveFailures());
            case COMPLETE:
                return repository.reschedule(execution.getTaskInstance(),
                        execution.getVersion(),
                        executionTime,
                        serializedData,
                        executionState.getCreateTime(),
                        execution.getLastFailure(),
                        execution.getConsecutiveFailures());
            case DEAD_EXECUTION:
            case FAILED:
                return repository.reschedule(execution.getTaskInstance(),
                        execution.getVersion(),
                        executionTime,
                        serializedData,
                        execution.getLastSuccess(),
                        clock.now(),
                        execution.getConsecutiveFailures() + 1);
            default:
                return Mono.error(new ExecutionException("Can't reschedule in execution status " + executionState.getName(), execution));
        }
    }

    @Override
    public Flux<Execution<?>> getAll() {
        return repository.getAll()
                .map(this::findTaskForView)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    @Override
    public Mono<Integer> deleteUnresolvedTask(Duration deleteUnresolvedAfter) {
        Collection<String> unresolvedName = taskResolver.getUnresolvedName();
        if (unresolvedName.isEmpty())
            return Mono.empty();
        return repository.removeOldUnresolvedTask(unresolvedName, clock.now().minus(deleteUnresolvedAfter));
    }

    @Override
    public Mono<Void> rescheduleDeadExecutionTask(Duration duration) {
        return repository.getDeadExecution(clock.now().minus(duration))
                .map(this::findTaskForDeadExecution)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .flatMap(execution -> execution.onDeadExecution(DefaultExecutionOperations.of(this)))
                .then();
    }

    @Override
    public <T> Mono<Execution<T>> findExecution(TaskInstance<T> taskInstanceId) {
        return repository.getExecution(taskInstanceId)
                .map(executionEntity -> mapWithStatus(executionEntity, taskInstanceId.getTask(), new ViewState()));
    }

    @Override
    public Mono<Execution<?>> findExecution(TaskInstanceId taskInstanceId) {
        return repository.getExecution(taskInstanceId)
                .map(this::findTaskForView)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    @Override
    public Flux<Execution<?>> findExecution(boolean picked) {
        return repository.getExecutionsForView(null, picked)
                .map(this::findTaskForView)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    @Override
    public <T> Flux<Execution<T>> findExecution(String taskName, boolean picked, Class<T> dataClass) {
        return repository.getExecutions(taskName, picked)
                .map((ExecutionEntity executionEntity) -> findTaskForView(executionEntity, dataClass))
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    @Override
    public Stream<ScheduleOnStartup> scheduleOnStartUp() {
        return taskResolver.findAll().stream()
                .filter(ScheduleOnStartup.class::isInstance)
                .map(ScheduleOnStartup.class::cast);
    }

    @NotNull
    private Optional<Execution<?>> findTaskForPick(ExecutionEntity executionEntity) {
        return taskResolver.findTask(executionEntity.getTaskName())
                .map((AbstractTask<?> task) -> mapForPick(executionEntity, task));
    }

    @SuppressWarnings("unchecked")
    @NotNull
    private <T> Optional<Execution<T>> findTaskForView(ExecutionEntity executionEntity, Class<T> dataClass) {
        return taskResolver.findTask(executionEntity.getTaskName())
                .map(task -> {
                    if (task.getDataClass().isAssignableFrom(dataClass)) {
                        AbstractTask<T> taskT = (AbstractTask<T>) task;
                        return mapWithStatus(executionEntity, taskT, new ViewState());
                    }
                    return null;
                });
    }

    @NotNull
    private Optional<Execution<?>> findTaskForView(ExecutionEntity executionEntity) {
        return taskResolver.findTask(executionEntity.getTaskName())
                .map(task -> mapWithStatus(executionEntity, task, new ViewState()));
    }

    @NotNull
    private Optional<Execution<?>> findTaskForDeadExecution(ExecutionEntity executionEntity) {
        return taskResolver.findTask(executionEntity.getTaskName())
                .map(task -> mapWithStatus(executionEntity, task, new DeadExecutionState()));
    }

    @NotNull
    private <T> Execution<T> mapWithStatus(ExecutionEntity executionEntity, AbstractTask<T> task, ExecutionState state) {
        Supplier<T> dataSupplier = MapperUtil.memoize(() -> serializer.deserialize(task.getDataClass(), executionEntity.getData()));
        return executionMapper.mapToExecution(executionEntity, state, task, dataSupplier);
    }

    @Override
    public void close() throws IOException {
        if (repository instanceof Closeable) {
            ((Closeable) repository).close();
        }
    }
}
