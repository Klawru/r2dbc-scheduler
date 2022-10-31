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
package com.github.klawru.scheduler.executor;

import com.github.klawru.scheduler.config.SchedulerConfig;
import lombok.Getter;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Getter
public class DefaultTaskSchedulers implements TaskSchedulers {
    private final Scheduler housekeeperScheduler;
    private final Scheduler taskScheduler;
    private final int taskThreads;
    private final int taskLowerLimit;
    private final int taskUpperLimit;

    public DefaultTaskSchedulers(SchedulerConfig schedulerConfig) {
        this(schedulerConfig.getSchedulerName(),
                2,
                schedulerConfig.getThreads(),
                (int) schedulerConfig.getLowerLimitFractionOfThreads() * schedulerConfig.getThreads(),
                (int) schedulerConfig.getUpperLimitFractionOfThreads() * schedulerConfig.getThreads());
    }

    public DefaultTaskSchedulers(String schedulerName,
                                 int housekeeperThreads,
                                 int taskThreads,
                                 int taskLowerLimit,
                                 int taskUpperLimit) {
        this(taskThreads, taskLowerLimit, taskUpperLimit,
                Schedulers.newParallel(schedulerName + "-housekeeper", housekeeperThreads),
                Schedulers.newBoundedElastic(taskThreads, taskUpperLimit, schedulerName + "-task"));
    }

    public DefaultTaskSchedulers(int taskThreads,
                                 int taskLowerLimit,
                                 int taskUpperLimit,
                                 Scheduler housekeeperScheduler,
                                 Scheduler taskScheduler) {
        this.taskThreads = taskThreads;
        this.taskLowerLimit = taskLowerLimit;
        this.taskUpperLimit = taskUpperLimit;
        this.taskScheduler = taskScheduler;
        this.housekeeperScheduler = housekeeperScheduler;
    }

    @Override
    public void close() {
        taskScheduler.dispose();
        housekeeperScheduler.dispose();
    }
}
