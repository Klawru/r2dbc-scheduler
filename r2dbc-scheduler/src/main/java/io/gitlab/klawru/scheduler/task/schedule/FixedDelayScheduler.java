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
package io.gitlab.klawru.scheduler.task.schedule;

import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public class FixedDelayScheduler implements Scheduler {

    @Getter
    private final Duration delay;
    @Getter
    private final boolean disabled;

    public FixedDelayScheduler(Duration delay) {
        this(delay, false);
    }

    public FixedDelayScheduler(Duration delay, boolean disabled) {
        this.delay = Objects.requireNonNull(delay, "Delay should not null");
        this.disabled = disabled;
    }

    @Override
    public Instant nextExecutionTime(Instant now) {
        return now.plus(delay);
    }

    @Override
    public boolean isDeterministic() {
        return false;
    }
}
