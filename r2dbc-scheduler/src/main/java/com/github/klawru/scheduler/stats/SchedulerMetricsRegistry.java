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
package com.github.klawru.scheduler.stats;

import com.github.klawru.scheduler.executor.Execution;
import com.github.klawru.scheduler.executor.execution.state.ExecutionState;
import com.github.klawru.scheduler.executor.execution.state.ExecutionStateName;

import java.util.concurrent.atomic.AtomicLong;

import static com.github.klawru.scheduler.executor.execution.state.ExecutionStateName.COMPLETE;

public class SchedulerMetricsRegistry implements SchedulerListener {
    private final AtomicLong completeTaskCount = new AtomicLong(0);
    private final AtomicLong failedTaskCount = new AtomicLong(0);


    @Override
    public void afterExecution(Execution<?> execution) {
        ExecutionStateName state = execution.getLastState(COMPLETE).map(ExecutionState::getName).orElse(null);
        if (COMPLETE.equals(state)) {
            completeTaskCount.incrementAndGet();
        } else {
            failedTaskCount.incrementAndGet();
        }
    }

    public long getAllTask() {
        return completeTaskCount.get() + failedTaskCount.get();
    }

    public long getCompleteTask() {
        return completeTaskCount.get();
    }

    public long getFailedTask() {
        return failedTaskCount.get();
    }


}
