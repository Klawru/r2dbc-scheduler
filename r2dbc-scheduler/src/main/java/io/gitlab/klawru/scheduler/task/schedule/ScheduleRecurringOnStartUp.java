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
package io.gitlab.klawru.scheduler.task.schedule;

import io.gitlab.klawru.scheduler.executor.Execution;
import io.gitlab.klawru.scheduler.task.instance.TaskInstance;
import io.gitlab.klawru.scheduler.util.Clock;
import io.gitlab.klawru.scheduler.SchedulerClient;
import io.gitlab.klawru.scheduler.task.RecurringTask;
import io.gitlab.klawru.scheduler.task.instance.TaskInstanceId;
import io.gitlab.klawru.scheduler.util.MapperUtil;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Value
public class ScheduleRecurringOnStartUp<T> {
    String instance;
    T data;

    public Mono<Void> onStartup(SchedulerClient client, Clock clock, RecurringTask<T> task) {
        Scheduler schedule = task.getSchedule();
        TaskInstance<T> taskInstance = task.instance(instance);
        if (schedule.isDisabled()) {
            log.info("Task {} marked as disabled", taskInstance.getTaskNameId());
            return client.getExecution(taskInstance)
                    .doOnNext(tExecution -> log.info("The task {} will be disabled", taskInstance.getTaskNameId()))
                    .flatMap(client::cancel);
        } else {
            return client.getExecution(taskInstance)
                    .as(MapperUtil::mapToOptional)
                    .flatMap(executionOptional -> {
                        if (executionOptional.isEmpty()) {
                            Instant newExecutionTime = schedule.nextExecutionTime(clock.now());
                            log.info("Scheduling a new recurring task {} on {}", taskInstance.getTaskNameId(), newExecutionTime);
                            return client.schedule(taskInstance, newExecutionTime, data);
                        }

                        Execution<T> execution = executionOptional.get();
                        Duration pollingInterval = client.getConfig().getPollingInterval();
                        Optional<Instant> newExecutionTime = getNextTime(pollingInterval,
                                clock,
                                schedule,
                                execution.getExecutionTime(),
                                execution.getTaskInstance()
                        );
                        if (newExecutionTime.isPresent()) {
                            log.info("Rescheduling the task {} on {}", taskInstance, newExecutionTime.get());
                            return client.reschedule(execution.getTaskInstance(), newExecutionTime.get());
                        }
                        return Mono.empty();
                    });
        }
    }

    @NotNull
    Optional<Instant> getNextTime(Duration polingInterval,
                                  Clock clock,
                                  Scheduler schedule,
                                  Instant currentExecutionTime,
                                  TaskInstanceId taskInstance) {
        Instant newExecutionTime = schedule.nextExecutionTime(clock.now());
        Duration doublePollingInterval = polingInterval.multipliedBy(2);
        if (currentExecutionTime.isBefore(clock.now().plus(doublePollingInterval))) {
            log.info("Task {} will not be rescheduled because the existing due date is too close to the current time.", taskInstance.getTaskNameId());
            return Optional.empty();
        }

        if (schedule.isDeterministic()) {
            Duration duration = Duration.between(currentExecutionTime, newExecutionTime).abs();
            if (duration.compareTo(Duration.ofSeconds(1)) <= 0)
                return Optional.empty();
            else {
                return Optional.of(newExecutionTime);
            }
        } else {
            //non-deterministic
            //Don't schedule the task further than the already existing schedule.
            //Otherwise, the task will constantly shift in time.
            if (newExecutionTime.isBefore(currentExecutionTime)) {
                log.info("Rescheduling the task {} on {}", taskInstance.getTaskNameId(), newExecutionTime);
                return Optional.of(newExecutionTime);
            } else {
                return Optional.empty();
            }
        }
    }


}
