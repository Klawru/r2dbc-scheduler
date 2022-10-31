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

import com.github.klawru.scheduler.StartPauseService;
import com.github.klawru.scheduler.config.SchedulerConfig;
import com.github.klawru.scheduler.executor.TaskSchedulers;
import com.github.klawru.scheduler.repository.TaskService;
import com.github.klawru.scheduler.util.AlwaysDisposed;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Optional;

@Slf4j
public class DeleteUnresolvedTaskService implements StartPauseService {

    private final TaskService taskService;
    private final TaskSchedulers schedulers;
    private final SchedulerConfig config;
    private Disposable subscription;

    public DeleteUnresolvedTaskService(TaskService taskService,
                                       TaskSchedulers schedulers,
                                       SchedulerConfig config) {
        this.taskService = taskService;
        this.schedulers = schedulers;
        this.config = config;
        this.subscription = AlwaysDisposed.of();
    }

    @Override
    public void start() {
        if (subscription.isDisposed()) {
            log.debug("Start delete unresolved task");
            Duration pollingInterval = config.getPollingInterval().multipliedBy(10);
            this.subscription = taskService.deleteUnresolvedTask(config.getDeleteUnresolvedAfter())
                    .doOnSubscribe(s -> log.debug("DeleteUnresolvedTaskService"))
                    .doOnNext(deleted -> log.debug("removed by delete unresolved task count={}", deleted))
                    .doOnError(throwable -> log.error("Exception on delete unresolved task", throwable))
                    .repeatWhen(longFlux -> Mono.delay(pollingInterval, schedulers.getHousekeeperScheduler()))
                    .retryWhen(Retry.fixedDelay(Long.MAX_VALUE, pollingInterval).scheduler(schedulers.getHousekeeperScheduler()))
                    .doOnError(throwable -> log.warn("Unexpected exception on delete unresolved task.", throwable))
                    .subscribeOn(schedulers.getHousekeeperScheduler())
                    .subscribe();
        }
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
