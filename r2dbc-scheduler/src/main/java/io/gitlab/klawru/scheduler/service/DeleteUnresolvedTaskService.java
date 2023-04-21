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
import io.gitlab.klawru.scheduler.executor.TaskSchedulers;
import io.gitlab.klawru.scheduler.repository.TaskService;
import io.gitlab.klawru.scheduler.util.AlwaysDisposed;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Optional;

@Slf4j
public class DeleteUnresolvedTaskService implements StartPauseService {

    private final TaskService taskService;
    private final TaskSchedulers schedulers;
    private final SchedulerConfiguration config;
    private Disposable subscription;

    public DeleteUnresolvedTaskService(TaskService taskService,
                                       TaskSchedulers schedulers,
                                       SchedulerConfiguration config) {
        this.taskService = taskService;
        this.schedulers = schedulers;
        this.config = config;
        this.subscription = AlwaysDisposed.get();
    }

    @Override
    public void start() {
        if (subscription.isDisposed()) {
            log.debug("Start delete unresolved task");
            this.subscription = getDeleteUnresolvedTaskFlux()
                    .subscribe();
        }
    }

    @NotNull
    protected Flux<Integer> getDeleteUnresolvedTaskFlux() {
        Duration pollingInterval = config.getDeleteUnresolvedInterval();
        Duration deleteUnresolvedInterval = config.getDeleteUnresolvedAfter();
        return taskService.deleteUnresolvedTask(deleteUnresolvedInterval)
                .doOnNext(deleted -> log.debug("removed by delete unresolved task count={}", deleted))
                .doOnError(throwable -> log.error("Exception on delete unresolved task", throwable))
                .repeatWhen(countFlux ->  countFlux.delayElements(pollingInterval, schedulers.getHousekeeperScheduler()))
                .retryWhen(Retry.fixedDelay(Long.MAX_VALUE, pollingInterval).scheduler(schedulers.getHousekeeperScheduler()))
                .doOnError(throwable -> log.warn("Unexpected exception on delete unresolved task.", throwable))
                .subscribeOn(schedulers.getHousekeeperScheduler());
    }

    @Override
    public void pause() {
        Optional.ofNullable(subscription)
                .filter(disposable -> !disposable.isDisposed())
                .ifPresent(disposable -> {
                    log.debug("Stop delete unresolved task");
                    disposable.dispose();
                });
    }
}
