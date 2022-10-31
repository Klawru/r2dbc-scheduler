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
package com.github.klawru.scheduler.service;

import com.github.klawru.scheduler.DefaultExecutionOperations;
import com.github.klawru.scheduler.DefaultSchedulerClient;
import com.github.klawru.scheduler.StartPauseService;
import com.github.klawru.scheduler.config.SchedulerConfig;
import com.github.klawru.scheduler.executor.Execution;
import com.github.klawru.scheduler.executor.TaskExecutor;
import com.github.klawru.scheduler.repository.TaskService;
import com.github.klawru.scheduler.util.Trigger;
import com.github.klawru.scheduler.executor.TaskSchedulers;
import com.github.klawru.scheduler.task.DefaultExecutionContext;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.util.Optional;

@Slf4j
public class TaskFetchService implements StartPauseService {
    private final DefaultSchedulerClient client;
    private final TaskExecutor executor;
    private final TaskService taskService;
    private final SchedulerConfig config;
    private final TaskSchedulers schedulers;
    private final Trigger triggerFetch;


    Disposable taskFetchStream;

    public TaskFetchService(DefaultSchedulerClient client, TaskService taskService, TaskExecutor executor, TaskSchedulers schedulers, SchedulerConfig config) {
        this.client = client;
        this.executor = executor;
        this.taskService = taskService;
        this.config = config;
        this.schedulers = schedulers;
        this.taskFetchStream = startTaskFetch();
        this.triggerFetch = new Trigger();
    }

    @NotNull
    protected Disposable startTaskFetch() {
        log.debug("Start TaskFetchService on '{}'", config.getSchedulerName());
        return Flux.defer(() -> {
                    int executionsToFetch = executor.taskUpperLimit() - executor.getNumberInQueueOrProcessing();
                    return taskService.lockAndGetDue(executionsToFetch);
                })
                .publishOn(schedulers.getHousekeeperScheduler())
                .doOnNext(executionEntity -> log.debug("task fetch '{}'", executionEntity.getTaskInstance().getTaskNameId()))
                .doOnError(throwable -> log.error("Exception on task fetch", throwable))
                //repeat on triggerFetch or delay
                .repeatWhen(longFlux -> longFlux.delayUntil(aLong -> Flux.firstWithSignal(
                        Mono.delay(config.getPollingInterval(), schedulers.getHousekeeperScheduler()),
                        triggerFetch.getFlux()))
                )
                .retryWhen(Retry.fixedDelay(Long.MAX_VALUE, config.getPollingInterval()).scheduler(schedulers.getHousekeeperScheduler()))
                .doOnError(throwable -> log.error("Unexpected exception on task fetch", throwable))
                .retryWhen(Retry.indefinitely())
                .subscribeOn(schedulers.getHousekeeperScheduler())
                .subscribe(this::addToExecutor);
    }

    protected <T> void addToExecutor(Execution<T> execution) {
        executor.addToQueue(execution,
                DefaultExecutionContext.of(execution, client),
                DefaultExecutionOperations.of(taskService, this::getTriggerFetchCallback));
    }

    protected void getTriggerFetchCallback() {
        if (executor.getNumberInQueueOrProcessing() <= executor.taskLowerLimit())
            fetchTask();
    }

    public void fetchTask() {
        triggerFetch.emit();
    }

    @Override
    public void pause() {
        Optional.ofNullable(taskFetchStream)
                .filter(disposable -> !disposable.isDisposed())
                .ifPresent(Disposable::dispose);
        taskFetchStream = null;
    }

    public void start() {
        if (taskFetchStream == null)
            startTaskFetch();
    }
}
