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
import com.github.klawru.scheduler.executor.TaskExecutor;
import com.github.klawru.scheduler.stats.SchedulerMetricsRegistry;
import com.github.klawru.scheduler.config.SchedulerConfig;
import com.github.klawru.scheduler.executor.TaskSchedulers;
import com.github.klawru.scheduler.repository.TaskService;
import com.github.klawru.scheduler.service.DeadExecutionDetectService;
import com.github.klawru.scheduler.service.DeleteUnresolvedTaskService;
import com.github.klawru.scheduler.service.TaskFetchService;
import com.github.klawru.scheduler.service.UpdateHeartbeatService;
import com.github.klawru.scheduler.task.instance.TaskInstance;
import com.github.klawru.scheduler.task.instance.TaskInstanceId;
import com.github.klawru.scheduler.util.Clock;
import com.github.klawru.scheduler.util.DataHolder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Instant;


@Slf4j
public class DefaultSchedulerClient implements SchedulerClient, StartPauseService {
    private final TaskService taskService;
    private final TaskExecutor executor;
    private final TaskSchedulers schedulers;
    private final SchedulerConfig config;
    private final Clock clock;

    @Getter
    private final SchedulerMetricsRegistry schedulerMetricsRegistry;

    private final TaskFetchService taskFetchService;
    private final UpdateHeartbeatService updateHeartbeatService;
    private final DeadExecutionDetectService deadExecutionDetectService;
    private final DeleteUnresolvedTaskService deleteUnresolvedTaskService;

    public DefaultSchedulerClient(TaskService taskService, TaskExecutor executor, TaskSchedulers schedulers, SchedulerMetricsRegistry schedulerMetricsRegistry, SchedulerConfig config, Clock clock) {
        this.taskService = taskService;
        this.executor = executor;
        this.schedulers = schedulers;
        this.schedulerMetricsRegistry = schedulerMetricsRegistry;
        this.config = config;
        this.clock = clock;
        this.taskFetchService = new TaskFetchService(this, this.taskService, this.executor, this.schedulers, this.config);
        this.updateHeartbeatService = new UpdateHeartbeatService(this.taskService, this.executor, this.schedulers, this.config);
        this.deadExecutionDetectService = new DeadExecutionDetectService(this.taskService, this.schedulers, this.config);
        this.deleteUnresolvedTaskService = new DeleteUnresolvedTaskService(this.taskService, this.schedulers, this.config);
        config.validate();
    }

    @Override
    public <T> Mono<Void> schedule(TaskInstance<T> taskInstance) {
        return taskService.createIfNotExists(taskInstance, taskInstance::nextExecutionTime, DataHolder.of(taskInstance.getData()));
    }

    @Override
    public <T> Mono<Void> schedule(TaskInstance<T> taskInstance, Instant nextTime) {
        return taskService.createIfNotExists(taskInstance, now -> nextTime, DataHolder.of(taskInstance.getData()));
    }

    @Override
    public <T> Mono<Void> schedule(TaskInstance<T> taskInstance, T newData) {
        return taskService.createIfNotExists(taskInstance, taskInstance.getNextExecutionTime(), DataHolder.of(newData));
    }


    @Override
    public <T> Mono<Void> schedule(TaskInstance<T> taskInstance, Instant nextTime, T newData) {
        return taskService.createIfNotExists(taskInstance, now -> nextTime, DataHolder.of(newData));
    }

    @Override
    public <T> Mono<Void> reschedule(TaskInstance<T> taskInstance) {
        return taskService.reschedule(taskInstance, taskInstance.getNextExecutionTime(), DataHolder.empty());
    }

    @Override
    public <T> Mono<Void> reschedule(TaskInstance<T> taskInstance, Instant nextTime) {
        return taskService.reschedule(taskInstance, now -> nextTime, DataHolder.empty());
    }

    @Override
    public <T> Mono<Void> reschedule(TaskInstance<T> taskInstance, Instant nextTime, T newData) {
        return taskService.reschedule(taskInstance, now -> nextTime, DataHolder.of(newData));
    }

    @Override
    public <T> Mono<Void> reschedule(TaskInstance<T> taskInstance, T newData) {
        return taskService.reschedule(taskInstance, taskInstance.getNextExecutionTime(), DataHolder.of(newData));
    }

    @Override
    public <T> Mono<Void> cancel(Execution<T> execution) {
        return taskService.remove(execution);
    }

    public void start() {
        log.info("Starting scheduler '{}'", config.getSchedulerName());
        startTask();
        taskFetchService.start();
        updateHeartbeatService.start();
        deadExecutionDetectService.start();
        deleteUnresolvedTaskService.start();
    }

    private void startTask() {
        Flux.fromStream(taskService::scheduleOnStartUp)
                .concatMapDelayError(scheduleOnStartup -> scheduleOnStartup.onStartup(this, clock), 1)
                .subscribeOn(schedulers.getTaskScheduler())
                .then()
                .block();
    }

    @Override
    public void pause() {
        log.info("Stop scheduler '{}'", config.getSchedulerName());
        taskFetchService.pause();
        updateHeartbeatService.pause();
        deadExecutionDetectService.pause();
        deleteUnresolvedTaskService.pause();
    }


    @Override
    public void fetchTask() {
        taskFetchService.fetchTask();
    }

    @Override
    public void detectDeadExecution() {
        deadExecutionDetectService.detect();
    }

    @Override
    public Flux<Execution<?>> getAllExecution() {
        return taskService.getAll();
    }

    @Override
    public <T> Mono<Execution<T>> getExecution(TaskInstance<T> taskInstanceId) {
        return taskService.findExecution(taskInstanceId);
    }

    @Override
    public Mono<Execution<?>> getExecution(TaskInstanceId taskInstanceId) {
        return taskService.findExecution(taskInstanceId);
    }

    @Override
    public Flux<Execution<?>> getScheduledExecutions() {
        return taskService.findExecution(false);
    }

    @Override
    public <T> Flux<Execution<T>> getScheduledExecutionsForTask(String name, Class<T> dataClass) {
        return taskService.findExecution(name, false, dataClass);
    }

    @Override
    public int getCountProcessingTask() {
        return executor.getNumberInQueueOrProcessing();
    }

    @Override
    public SchedulerConfig getConfig() {
        return config;
    }

    @Override
    public void close() {
        pause();
        executor.stop(config.getShutdownMaxWait());
        try {
            taskService.close();
        } catch (IOException e) {
            log.error("Error on close task service", e);
        }
        schedulers.close();
    }
}
