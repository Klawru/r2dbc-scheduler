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
package io.gitlab.klawru.scheduler.config;

import lombok.Builder;
import lombok.Value;

import java.time.Duration;
import java.util.Objects;


@Value
@Builder
public class SchedulerConfiguration {

    public static final int THREADS_DEFAULT = 10;
    public static final Duration POLLING_INTERVAL_DEFAULT = Duration.ofSeconds(10);
    public static final Duration UNRESOLVED_DELETE_INTERVAL = POLLING_INTERVAL_DEFAULT.multipliedBy(10);
    public static final Duration UNRESOLVED_DELETE_AFTER_DEFAULT = Duration.ofDays(14);
    public static final Duration SHUTDOWN_MAX_WAIT_DEFAULT = Duration.ofMinutes(10);
    public static final double LOWER_LIMIT_FRACTION_OF_THREADS_DEFAULT = 0.5;
    public static final double UPPER_LIMIT_FRACTION_OF_THREADS_DEFAULT = 1;
    public static final Duration HEARTBEAT_INTERVAL_DEFAULT = Duration.ofMinutes(5);
    public static final String TABLE_NAME_DEFAULT = "scheduled_job";
    public static final Duration DELETE_UNRESOLVED_AFTER_DEFAULT = Duration.ofDays(14);
    /**
     * Number threads for tasks
     */
    @Builder.Default
    int threads = THREADS_DEFAULT;
    /**
     * How often tasks are checked
     */
    @Builder.Default
    Duration pollingInterval = POLLING_INTERVAL_DEFAULT;
    /**
     * How often is heartbeat updated for executable tasks
     */
    @Builder.Default
    Duration heartbeatInterval = HEARTBEAT_INTERVAL_DEFAULT;
    /**
     * The maximum time given for the scheduler to complete
     */
    @Builder.Default
    Duration shutdownMaxWait = SHUTDOWN_MAX_WAIT_DEFAULT;

    /**
     * How often is unknown tasks will be deleted.
     */
    @Builder.Default
    Duration deleteUnresolvedInterval = DELETE_UNRESOLVED_AFTER_DEFAULT;
    /**
     * Time after which unknown tasks will be deleted.
     */
    @Builder.Default
    Duration deleteUnresolvedAfter = DELETE_UNRESOLVED_AFTER_DEFAULT;
    /**
     * The lower threshold of executable tasks at which new ones will be loaded. Depends on the number of threads.
     */
    @Builder.Default
    double lowerLimitFractionOfThreads = LOWER_LIMIT_FRACTION_OF_THREADS_DEFAULT;
    /**
     * The maximum number of tasks loaded for execution. Depends on the number of threads.
     */
    @Builder.Default
    double upperLimitFractionOfThreads = UPPER_LIMIT_FRACTION_OF_THREADS_DEFAULT;
    /**
     * The name of this scheduler instance.
     */
    String schedulerName;
    /**
     * The name of this scheduler table in DB.
     */
    @Builder.Default
    String tableName = TABLE_NAME_DEFAULT;

    public void validate() {
        Objects.requireNonNull(pollingInterval, "pollingInterval must not be null");
        Objects.requireNonNull(heartbeatInterval, "heartbeatInterval must not be null");
        Objects.requireNonNull(shutdownMaxWait, "shutdownMaxWait must not be null");
        Objects.requireNonNull(deleteUnresolvedAfter, "deleteUnresolvedAfter must not be null");
        Objects.requireNonNull(schedulerName, "schedulerName must not be null");
        Objects.requireNonNull(tableName, "tableName must not be null");
    }

    /**
     * Builder class for javadoc
     */
    public static class SchedulerConfigurationBuilder {
    }
}
