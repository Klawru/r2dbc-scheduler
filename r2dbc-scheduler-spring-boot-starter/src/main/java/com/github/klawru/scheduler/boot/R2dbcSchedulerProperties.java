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

package com.github.klawru.scheduler.boot;

import com.github.klawru.scheduler.config.SchedulerConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties("r2dbc-scheduler")
public class R2dbcSchedulerProperties {
    private boolean enabled;
    private int threads = 10;
    private Duration pollingInterval = Duration.ofSeconds(10);
    private Duration unresolvedDeleteInterval = Duration.ofSeconds(10);
    private Duration shutdownMaxWait = Duration.ofMinutes(10);
    private double lowerLimitFractionOfThreads = 0.5;
    private double upperLimitFractionOfThreads = 1;
    private Duration heartbeatInterval = Duration.ofMinutes(5);
    private String schedulerName;
    private String tableName = "scheduled_job";
    private Duration deleteUnresolvedAfter = Duration.ofDays(14);

    SchedulerConfig.SchedulerConfigBuilder toConfig() {
        return com.github.klawru.scheduler.config.SchedulerConfig.builder()
                .threads(threads)
                .pollingInterval(pollingInterval)
                .unresolvedDeleteInterval(unresolvedDeleteInterval)
                .shutdownMaxWait(shutdownMaxWait)
                .lowerLimitFractionOfThreads(lowerLimitFractionOfThreads)
                .upperLimitFractionOfThreads(upperLimitFractionOfThreads)
                .heartbeatInterval(heartbeatInterval)
                .schedulerName(schedulerName)
                .tableName(tableName)
                .deleteUnresolvedAfter(deleteUnresolvedAfter);
    }
}
