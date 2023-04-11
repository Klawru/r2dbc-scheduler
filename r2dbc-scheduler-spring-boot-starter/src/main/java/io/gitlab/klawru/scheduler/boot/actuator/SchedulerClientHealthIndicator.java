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
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;

@RequiredArgsConstructor
public class SchedulerClientHealthIndicator extends AbstractHealthIndicator {
    private final SchedulerClient scheduler;

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        SchedulerClientStatus status = scheduler.getCurrentStatus();
        builder.withDetail("status", status);
        if (status == SchedulerClientStatus.RUNNING)
            builder.up();
        else {
            builder.down();
        }
    }
}
