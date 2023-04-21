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
package io.gitlab.klawru.scheduler;

import io.gitlab.klawru.scheduler.config.SchedulerConfiguration;
import io.gitlab.klawru.scheduler.executor.Execution;
import io.gitlab.klawru.scheduler.stats.SchedulerClientStatus;
import io.gitlab.klawru.scheduler.stats.SchedulerMetricsRegistry;
import io.gitlab.klawru.scheduler.task.instance.TaskInstance;
import io.gitlab.klawru.scheduler.task.instance.TaskInstanceId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

public interface SchedulerClient extends StartPauseService, AutoCloseable {
    /**
     * Schedule a new task
     *
     * @param taskInstance task-instance
     * @param <T>          type of task-data
     * @return Mono
     */
    <T> Mono<Void> schedule(TaskInstance<T> taskInstance);

    /**
     * Schedule a new task with a time to run
     *
     * @param taskInstance task-instance
     * @param nextTime     time when the task should be started
     * @param <T>          type of task-data
     * @return Mono
     */
    <T> Mono<Void> schedule(TaskInstance<T> taskInstance, Instant nextTime);

    /**
     * Schedule a new task with data to run
     *
     * @param taskInstance task-instance
     * @param newData      task-data
     * @param <T>          type of task-data
     * @return Mono
     */
    <T> Mono<Void> schedule(TaskInstance<T> taskInstance, T newData);

    /**
     * @param taskInstance task-instance
     * @param nextTime     time when the task should be started
     * @param newData      task-data
     * @param <T>          type of task-data
     * @return Mono
     */
    <T> Mono<Void> schedule(TaskInstance<T> taskInstance, Instant nextTime, T newData);

    /**
     * Update an existing task with the new run time from taskInstance
     *
     * @param taskInstance task-instance
     * @param <T>          type of task-data
     * @return Mono
     */
    <T> Mono<Void> reschedule(TaskInstance<T> taskInstance);

    /**
     * Update an existing task with the new run time
     *
     * @param taskInstance task-instance
     * @param nextTime     time when the task should be started
     * @param <T>          type of task-data
     * @return Mono
     */
    <T> Mono<Void> reschedule(TaskInstance<T> taskInstance, Instant nextTime);

    /**
     * Update an existing task with new time and data
     *
     * @param taskInstance task-instance
     * @param newData      new task-data
     * @param <T>          type of task-data
     * @return void
     */
    <T> Mono<Void> reschedule(TaskInstance<T> taskInstance, T newData);

    /**
     * Update an existing task with new time and data
     *
     * @param taskInstance task-instance
     * @param nextTime     time when the task should be started
     * @param newData      new task-data
     * @param <T>          type of task-data
     * @return Mono
     */
    <T> Mono<Void> reschedule(TaskInstance<T> taskInstance, Instant nextTime, T newData);


    /**
     * Remove a task from the queue.
     * If the task was running, it would not be stopped until scheduler tries to update heartbeat.
     *
     * @param taskInstanceId task-instance id
     * @return Mono
     */
    Mono<Void> cancel(TaskInstanceId taskInstanceId);

    /**
     * Try to load new tasks for execution
     */
    void fetchTask();

    /**
     * Returns the number of running tasks
     *
     * @return number of running tasks
     */
    int getCountProcessingTask();

    /**
     * Find a tasks by example
     *
     * @param taskExample task-instance
     * @param <T>         type of task-data
     * @return execution
     */
    <T> Flux<Execution<T>> findExecutions(TaskExample<T> taskExample);

    /**
     * Count a tasks by example
     *
     * @param taskExample task-instance
     * @param <T>         type of task-data
     * @return execution
     */
    <T> Mono<Long> countExecution(TaskExample<T> taskExample);

    SchedulerClientStatus getCurrentStatus();

    SchedulerConfiguration getConfig();

    SchedulerMetricsRegistry getMetricsRegistry();

    @Override
    void close();
}
