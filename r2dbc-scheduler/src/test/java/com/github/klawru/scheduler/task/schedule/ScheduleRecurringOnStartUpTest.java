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

package com.github.klawru.scheduler.task.schedule;

import com.github.klawru.scheduler.AbstractPostgresTest;
import com.github.klawru.scheduler.task.instance.TaskInstanceId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static java.time.temporal.ChronoUnit.HOURS;
import static org.assertj.core.api.Assertions.assertThat;

class ScheduleRecurringOnStartUpTest extends AbstractPostgresTest {


    @Test
    void getNextTimeWhenCurrentExecutionTimeIsTooCloseThenEmpty() {
        Duration polingInterval = Duration.ofSeconds(10);
        Instant currentExecutionTime = testClock.now().plus(polingInterval);
        CronScheduler everyHour = new CronScheduler("0 0 * * * *");
        var dailyTask = new ScheduleRecurringOnStartUp<>("schedule", null);
        //When
        Optional<Instant> nextTime = dailyTask.getNextTime(polingInterval,
                testClock,
                everyHour,
                currentExecutionTime,
                TaskInstanceId.of("name", "1"));
        //Then
        assertThat(nextTime)
                .isEmpty();
    }

    @Test
    void getNextTimeWhenCurrentExecutionTimeIsFarThenReschedule() {
        Duration polingInterval = Duration.ofSeconds(10);
        Instant currentExecutionTime = testClock.now().plus(polingInterval.multipliedBy(1000));
        CronScheduler everyHour = new CronScheduler("0 0 * * * *");
        var dailyTask = new ScheduleRecurringOnStartUp<>("schedule", null);
        //When
        Optional<Instant> nextTime = dailyTask.getNextTime(polingInterval,
                testClock,
                everyHour,
                currentExecutionTime,
                TaskInstanceId.of("name", "1"));
        //Then
        assertThat(nextTime)
                .isPresent();
    }

    @Test
    void getNextTimeWhenCurrentTimeIsCloseToNextThenEmpty() {
        Duration polingInterval = Duration.ofSeconds(10);
        Instant currentExecutionTime = testClock.now().plus(1, HOURS).truncatedTo(HOURS);
        CronScheduler everyHour = new CronScheduler("0 0 * * * *");
        var dailyTask = new ScheduleRecurringOnStartUp<>("schedule", null);
        //When
        Optional<Instant> nextTime = dailyTask.getNextTime(polingInterval,
                testClock,
                everyHour,
                currentExecutionTime,
                TaskInstanceId.of("name", "1"));
        //Then
        assertThat(nextTime)
                .isEmpty();
    }

    @Test
    void getNextTimeWhenNonDeterministicSchedulerEmitTimeBeyondCurrentTimeThenEmpty() {
        Duration polingInterval = Duration.ofSeconds(10);
        Instant currentExecutionTime = testClock.now().plus(polingInterval.multipliedBy(3));
        Scheduler hourDelayScheduler = new FixedDelayScheduler(Duration.ofHours(1));
        var dailyTask = new ScheduleRecurringOnStartUp<>("schedule", null);
        //When
        Optional<Instant> nextTime = dailyTask.getNextTime(polingInterval,
                testClock,
                hourDelayScheduler,
                currentExecutionTime,
                TaskInstanceId.of("name", "1"));
        //Then
        assertThat(nextTime)
                .isEmpty();
    }
}