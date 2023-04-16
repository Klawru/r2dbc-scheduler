/*
 * Copyright © Klawru
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

package io.gitlab.klawru.scheduler.boot;

import io.gitlab.klawru.scheduler.config.SchedulerConfiguration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

import java.time.Duration;

import static io.gitlab.klawru.scheduler.config.SchedulerConfiguration.*;
import static java.time.temporal.ChronoUnit.*;

@Data
@ConfigurationProperties("r2dbc-scheduler")
public class R2dbcSchedulerProperties {

    private boolean enabled;
    /**
     * Number threads for tasks
     */
    private int threads = THREADS_DEFAULT;
    /**
     * How often tasks are checked
     */
    @DurationUnit(SECONDS)
    private Duration pollingInterval = POLLING_INTERVAL_DEFAULT;
    /**
     * How often is heartbeat updated for executable tasks
     */
    @DurationUnit(MINUTES)
    private Duration heartbeatInterval = HEARTBEAT_INTERVAL_DEFAULT;
    /**
     * How often is unknown tasks will be deleted.
     */
    @DurationUnit(HOURS)
    private Duration unresolvedDeleteInterval = UNRESOLVED_DELETE_AFTER_DEFAULT;
    /**
     * Time after which unknown tasks are deleted
     */
    @DurationUnit(HOURS)
    private Duration unresolvedDeleteAfterInterval = UNRESOLVED_DELETE_AFTER_DEFAULT;
    /**
     * Time after which unknown tasks will be deleted.
     */
    private Duration deleteUnresolvedAfter = DELETE_UNRESOLVED_AFTER_DEFAULT;
    /**
     * The maximum time given for the scheduler to complete
     */
    @DurationUnit(SECONDS)
    private Duration shutdownMaxWait = SHUTDOWN_MAX_WAIT_DEFAULT;
    /**
     * The lower threshold of executable tasks at which new ones will be loaded. Depends on the number of threads.
     */
    private double lowerLimitFractionOfThreads = LOWER_LIMIT_FRACTION_OF_THREADS_DEFAULT;
    /**
     * The maximum number of tasks loaded for execution. Depends on the number of threads.
     */
    private double upperLimitFractionOfThreads = UPPER_LIMIT_FRACTION_OF_THREADS_DEFAULT;
    /**
     * The name of this scheduler instance.
     */
    private String schedulerName;
    /**
     * The name of this scheduler table in DB.
     */
    private String tableName = TABLE_NAME_DEFAULT;

    SchedulerConfigurationBuilder toConfig() {
        return SchedulerConfiguration.builder()
                .threads(threads)
                .pollingInterval(pollingInterval)
                .shutdownMaxWait(shutdownMaxWait)
                .lowerLimitFractionOfThreads(lowerLimitFractionOfThreads)
                .upperLimitFractionOfThreads(upperLimitFractionOfThreads)
                .heartbeatInterval(heartbeatInterval)
                .schedulerName(schedulerName)
                .tableName(tableName)
                .deleteUnresolvedInterval(unresolvedDeleteInterval)
                .deleteUnresolvedAfter(deleteUnresolvedAfter);
    }
}
