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

package io.gitlab.klawru.scheduler.task;

import io.gitlab.klawru.scheduler.AbstractPostgresTest;
import io.gitlab.klawru.scheduler.SchedulerClient;
import io.gitlab.klawru.scheduler.task.schedule.CronScheduler;
import io.gitlab.klawru.scheduler.util.Tasks;
import io.gitlab.klawru.scheduler.util.TestTasks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CronRecurringTaskTest extends AbstractPostgresTest {
    RecurringTask<Void> recurringTask;
    TestTasks.CountingHandler<Void> countingHandler;
    private SchedulerClient scheduler;

    @BeforeEach
    void setUp() {
        countingHandler = new TestTasks.CountingHandler<>();
        CronScheduler everyMinute = new CronScheduler("0 * * * * *");
        recurringTask = Tasks.recurring("recurringTask", Void.class, everyMinute)
                .execute(countingHandler);
        scheduler = schedulerFor(recurringTask);
    }

    @Test
    void checkRecurringTaskRunning() {
        testClock.plusSecond(61);
        wailAllExecutionDone(scheduler);
        assertThat(countingHandler.getTimesExecuted()).isEqualTo(1);

        testClock.plusSecond(61);
        wailAllExecutionDone(scheduler);
        assertThat(countingHandler.getTimesExecuted()).isEqualTo(2);
    }
}