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

import io.gitlab.klawru.scheduler.SchedulerClient;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

public class SchedulerContextReadyStarter implements SchedulerStarter {

    SchedulerClient schedulerClient;

    public SchedulerContextReadyStarter(SchedulerClient schedulerClient) {
        this.schedulerClient = schedulerClient;
    }

    @Override
    @EventListener(ContextRefreshedEvent.class)
    public void start() {
        schedulerClient.start();
    }
}