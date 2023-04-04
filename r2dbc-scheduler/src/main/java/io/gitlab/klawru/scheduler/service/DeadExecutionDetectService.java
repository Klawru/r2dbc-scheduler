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
import io.gitlab.klawru.scheduler.util.Trigger;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.util.Optional;

@Slf4j
public class DeadExecutionDetectService implements StartPauseService {

    private final TaskService taskService;
    private final TaskSchedulers schedulers;
    private final SchedulerConfiguration config;
    private final Trigger trigger;
    private Disposable deadExecutionDetectDisposable;

    public DeadExecutionDetectService(TaskService taskService,
                                      TaskSchedulers schedulers,
                                      SchedulerConfiguration config) {
        this.taskService = taskService;
        this.schedulers = schedulers;
        this.config = config;
        this.deadExecutionDetectDisposable = AlwaysDisposed.of();
        this.trigger = new Trigger();
    }

    @Override
    public void start() {
        if (deadExecutionDetectDisposable.isDisposed()) {
            log.debug("Start dead execution detect");
            this.deadExecutionDetectDisposable = taskService.deleteUnresolvedTask(config.getDeleteUnresolvedAfter())
                    .doOnNext(deleted -> log.trace("removed by removeOldUnresolvedTask count={}", deleted))
                    .doOnError(throwable -> log.error("Exception on delete unresolved task", throwable))
                    .then(taskService.rescheduleDeadExecutionTask(config.getHeartbeatInterval().multipliedBy(4))
                            .doOnError(throwable -> log.error("Exception on reschedule dead execution", throwable))
                    )
                    .repeatWhen(longFlux -> longFlux.flatMap(aLong -> Flux.firstWithSignal(
                            Mono.delay(config.getPollingInterval().multipliedBy(2),
                                    schedulers.getHousekeeperScheduler())))
                    )
                    .retryWhen(Retry.fixedDelay(Long.MAX_VALUE, config.getPollingInterval())
                            .scheduler(schedulers.getHousekeeperScheduler())
                    )
                    .doOnError(throwable -> log.error("Unexpected exception in 'deadExecutionDetect'. Restart subscription", throwable))
                    .subscribeOn(schedulers.getHousekeeperScheduler())
                    .subscribe();
        }
    }

    @Override
    public void pause() {
        Optional.ofNullable(deadExecutionDetectDisposable)
                .filter(disposable -> !disposable.isDisposed())
                .ifPresent(disposable -> {
                    log.debug("Stop dead execution detect");
                    disposable.dispose();
                });
    }

    public void detect() {
        trigger.emit();
    }
}