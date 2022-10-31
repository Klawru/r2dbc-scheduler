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
package com.github.klawru.scheduler.config;

import lombok.Builder;
import lombok.Value;

import java.time.Duration;


@Value
@Builder
public class SchedulerConfig {

    @Builder.Default
    int threads = 10;
    @Builder.Default
    Duration pollingInterval = Duration.ofSeconds(10);
    @Builder.Default
    Duration unresolvedDeleteInterval = Duration.ofSeconds(10);
    @Builder.Default
    Duration shutdownMaxWait = Duration.ofMinutes(10);

    @Builder.Default
    double lowerLimitFractionOfThreads = 0.5;
    @Builder.Default
    double upperLimitFractionOfThreads = 1;

    @Builder.Default
    Duration heartbeatInterval = Duration.ofMinutes(5);

    String schedulerName;

    @Builder.Default
    String tableName = "scheduled_job";

    @Builder.Default
    Duration deleteUnresolvedAfter = Duration.ofDays(14);


    public void validate() {
        if (schedulerName == null) {
            throw new IllegalArgumentException("schedulerName must not be null");
        }
    }
}
