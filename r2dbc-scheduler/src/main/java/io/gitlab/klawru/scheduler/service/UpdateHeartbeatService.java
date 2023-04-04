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
package io.gitlab.klawru.scheduler.service;

import io.gitlab.klawru.scheduler.StartPauseService;
import io.gitlab.klawru.scheduler.config.SchedulerConfiguration;
import io.gitlab.klawru.scheduler.executor.Execution;
import io.gitlab.klawru.scheduler.executor.TaskExecutor;
import io.gitlab.klawru.scheduler.executor.TaskSchedulers;
import io.gitlab.klawru.scheduler.repository.TaskService;
import io.gitlab.klawru.scheduler.util.AlwaysDisposed;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.util.Optional;

@Slf4j
public class UpdateHeartbeatService implements StartPauseService {

    private final TaskExecutor executor;
    private final SchedulerConfiguration config;
    private final TaskSchedulers schedulers;
    private final TaskService taskService;

    private Disposable startUpdateHeartbeatsStream = AlwaysDisposed.of();

    public UpdateHeartbeatService(TaskService taskService,
                                  TaskExecutor executor,
                                  TaskSchedulers schedulers,
                                  SchedulerConfiguration config) {
        this.taskService = taskService;
        this.executor = executor;
        this.schedulers = schedulers;
        this.config = config;
    }

    @Override
    public void start() {
        if (this.startUpdateHeartbeatsStream.isDisposed()) {
            log.debug("Start update heartbeats");
            this.startUpdateHeartbeatsStream = Flux.defer(() -> {
                        var currentlyProcessing = executor.currentlyExecuting();
                        return Flux.fromStream(currentlyProcessing)
                                .concatMap(this::updateHeartBeat)
                                .doOnError(throwable -> log.error("Failed while updating heartbeat for execution. Will try again later.", throwable));
                    })
                    .doOnError(throwable -> log.error("Failed while updating heartbeat. Will try again later", throwable))
                    .repeatWhen(longFlux -> longFlux.delayElements(config.getHeartbeatInterval(), schedulers.getHousekeeperScheduler()))
                    .doOnError(throwable -> log.error("Unexpected exception while updating heartbeat", throwable))
                    .retryWhen(Retry.fixedDelay(Long.MAX_VALUE, config.getHeartbeatInterval()))
                    .subscribeOn(schedulers.getHousekeeperScheduler())
                    .subscribe();
        }
    }

    @NotNull
    private Mono<Void> updateHeartBeat(Execution<?> execution) {
        return taskService.updateHeartbeat(execution)
                .filter(updated -> !updated)
                .doOnNext(aFalse -> {
                    log.info("Task '{}:{}' was stopped because the heartBeate could not be updated. " +
                                    "Task has been removed from the repository",
                            execution.getTaskInstance().getTaskName(), execution.getTaskInstance().getId());
                    executor.removeFromQueue(execution);
                })
                .then();
    }


    @Override
    public void pause() {
        Optional.ofNullable(startUpdateHeartbeatsStream)
                .filter(disposable -> !disposable.isDisposed())
                .ifPresent(Disposable::dispose);
    }
}
