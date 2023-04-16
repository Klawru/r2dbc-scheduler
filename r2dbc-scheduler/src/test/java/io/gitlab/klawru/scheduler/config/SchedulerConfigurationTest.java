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

package io.gitlab.klawru.scheduler.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class SchedulerConfigurationTest {

    @Test
    void name() {
        var configuration = SchedulerConfiguration.builder()
                .threads(1)
                .pollingInterval(Duration.ofSeconds(1))
                .heartbeatInterval(Duration.ofSeconds(2))
                .shutdownMaxWait(Duration.ofSeconds(3))
                .deleteUnresolvedInterval(Duration.ofSeconds(4))
                .deleteUnresolvedAfter(Duration.ofSeconds(5))
                .lowerLimitFractionOfThreads(6)
                .upperLimitFractionOfThreads(7)
                .schedulerName("schedulerName")
                .tableName("tableName")
                .build();
        configuration.validate();
        //When
        assertThat(configuration)
                .isEqualTo(new SchedulerConfiguration(
                        1,
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(3),
                        Duration.ofSeconds(4),
                        Duration.ofSeconds(5),
                        6,
                        7,
                        "schedulerName",
                        "tableName"
                ));
    }
}