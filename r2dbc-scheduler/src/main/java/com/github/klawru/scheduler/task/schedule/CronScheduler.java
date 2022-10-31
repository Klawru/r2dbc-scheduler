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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.support.CronExpression;

import java.time.Instant;
import java.time.ZoneId;
import java.time.chrono.ChronoZonedDateTime;
import java.util.Optional;

@Slf4j
public class CronScheduler implements Scheduler {
    private final CronExpression cronExpression;
    @Getter
    private final boolean disabled;
    private final ZoneId zoneId;

    /**
     * @param cron second (0-59), minute (0 - 59), hour (0 - 23), day of the month (1 - 31),
     *             month (1 - 12) (or JAN-DEC), day of the week (0 - 7)
     * @throws IllegalArgumentException in the expression does not conform to the cron format
     */
    public CronScheduler(String cron) throws IllegalArgumentException {
        this(cron, ZoneId.systemDefault(), false);
    }

    public CronScheduler(String cron, ZoneId zoneId) throws IllegalArgumentException {
        this(cron, zoneId, false);
    }

    public CronScheduler(String cron, ZoneId zoneId, boolean disabled) throws IllegalArgumentException {
        this.cronExpression = CronExpression.parse(cron);
        this.disabled = disabled;
        this.zoneId = zoneId;
    }

    @Override
    public Instant nextExecutionTime(Instant now) {
        return Optional.ofNullable(cronExpression.next(now.atZone(zoneId)))
                .map(ChronoZonedDateTime::toInstant)
                .orElseGet(() -> {
                    log.error("CronScheduler didn't find time matching expression '{}'", cronExpression);
                    return Instant.MAX;
                });
    }

    @Override
    public boolean isDeterministic() {
        return true;
    }
}
