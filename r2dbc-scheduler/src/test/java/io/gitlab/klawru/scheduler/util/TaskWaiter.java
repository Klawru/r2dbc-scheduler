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

package io.gitlab.klawru.scheduler.util;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

public class TaskWaiter {
    private final AtomicLong lastTaskCount = new AtomicLong();


    public void waitTask(Supplier<Long> countExecutedTask) {
        await().atMost(10, SECONDS)
                .until(() -> checkExecutedTask(countExecutedTask));
    }

    private boolean checkExecutedTask(Supplier<Long> countExecutedTask) {
        Long count = countExecutedTask.get();
        long lastCount = lastTaskCount.incrementAndGet();
        if (count != lastCount) {
            lastTaskCount.set(count);
            return true;
        } else
            return false;
    }

    public void reset() {
        lastTaskCount.set(0);
    }

    ;
}
