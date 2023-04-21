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

package io.gitlab.klawru.scheduler.boot.actuator;

import io.gitlab.klawru.scheduler.SchedulerClient;
import io.gitlab.klawru.scheduler.stats.SchedulerClientStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.actuate.health.Health;

import static org.assertj.core.api.Assertions.assertThat;

class SchedulerClientHealthIndicatorTest {

    SchedulerClient mockClient;
    SchedulerClientHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        mockClient = Mockito.mock(SchedulerClient.class);
        healthIndicator = new SchedulerClientHealthIndicator(mockClient);
    }

    @Test
    void healthWhenOkThenOk() {
        Mockito.when(mockClient.getCurrentStatus()).thenReturn(SchedulerClientStatus.RUNNING);
        //When
        Health actual = healthIndicator.health();
        //Then
        assertThat(actual)
                .isEqualTo(new Health.Builder()
                        .up()
                        .withDetail("status", SchedulerClientStatus.RUNNING)
                        .build());
    }

    @Test
    void healthWhenStopThenDown() {
        Mockito.when(mockClient.getCurrentStatus()).thenReturn(SchedulerClientStatus.STOPPED);
        //When
        Health actual = healthIndicator.health();
        //Then
        assertThat(actual)
                .isEqualTo(new Health.Builder()
                        .down()
                        .withDetail("status", SchedulerClientStatus.STOPPED)
                        .build());
    }
}