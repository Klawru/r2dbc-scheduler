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
package io.gitlab.klawru.scheduler.task;

import io.gitlab.klawru.scheduler.SchedulerClient;
import io.gitlab.klawru.scheduler.task.callback.CompletionHandler;
import io.gitlab.klawru.scheduler.task.callback.DeadExecutionHandler;
import io.gitlab.klawru.scheduler.task.callback.FailureHandler;
import io.gitlab.klawru.scheduler.task.instance.TaskInstance;
import io.gitlab.klawru.scheduler.task.schedule.ScheduleRecurringOnStartUp;
import io.gitlab.klawru.scheduler.task.schedule.Scheduler;
import io.gitlab.klawru.scheduler.util.Clock;
import io.gitlab.klawru.scheduler.task.callback.ScheduleOnStartup;
import lombok.Getter;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

/**
 * Recurring tasks are tasks that happen over and over again, on a regular basis.
 *
 * @param <T> data class
 */
public class RecurringTask<T> extends AbstractTask<T> implements ScheduleOnStartup {
    @Getter
    private final Scheduler schedule;
    private final ScheduleRecurringOnStartUp<T> scheduleOnStartup;


    public RecurringTask(String name,
                         Class<T> dataClass,
                         T initData,
                         Scheduler schedule,
                         ExecutionHandler<T> executionHandler,
                         CompletionHandler<T> completionHandler,
                         FailureHandler<T> failureHandler,
                         DeadExecutionHandler<T> deadExecutionHandler) {
        super(name, dataClass, executionHandler, completionHandler, failureHandler, deadExecutionHandler);
        this.schedule = schedule;
        this.scheduleOnStartup = new ScheduleRecurringOnStartUp<>("recurring", initData);
    }

    @Override
    public TaskInstance<T> instance(String id) {
        return new TaskInstance<>(id, this, schedule::nextExecutionTime);
    }

    @Override
    public TaskInstance<T> instance(String id, Supplier<T> data) {
        return new TaskInstance<>(id, data, this, schedule::nextExecutionTime);
    }

    @Override
    public Mono<Void> onStartup(SchedulerClient scheduler, Clock clock) {
        if (scheduleOnStartup != null)
            return scheduleOnStartup.onStartup(scheduler, clock, this);
        else
            return Mono.empty();
    }


}
