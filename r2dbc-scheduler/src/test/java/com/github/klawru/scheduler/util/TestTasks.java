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

package com.github.klawru.scheduler.util;

import com.github.klawru.scheduler.task.ExecutionContext;
import com.github.klawru.scheduler.task.ExecutionHandler;
import com.github.klawru.scheduler.task.OneTimeTask;
import com.github.klawru.scheduler.task.instance.TaskInstance;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class TestTasks {

    public static final ExecutionHandler<Void> EMPTY_EXECUTION_HANDLER = (taskInstance, executionContext) -> Mono.empty();

    public static OneTimeTask<Void> oneTime(String name, ExecutionHandler<Void> handler) {
        return Tasks.oneTime(name, Void.class)
                .execute(handler);
    }

    public static <T> OneTimeTask<T> oneTimeWithType(String name, Class<T> dataClass, ExecutionHandler<T> handler) {
        return Tasks.oneTime(name, dataClass)
                .execute(handler);
    }

    public static class CountingHandler<T> implements ExecutionHandler<T> {
        private final Duration wait;
        private final AtomicInteger timesExecuted = new AtomicInteger(0);

        public CountingHandler() {
            wait = Duration.ofMillis(0);
        }

        public CountingHandler(Duration wait) {
            this.wait = wait;
        }

        @Override
        @SuppressWarnings({"java:S2925", "BlockingMethodInNonBlockingContext"})
        public Mono<T> execute(TaskInstance<T> taskInstance, ExecutionContext<T> context) {
            return Mono.fromRunnable(() -> {
                log.debug("Execute CountingHandler in task='{}'", taskInstance.getTaskNameId());
                try {
                    Thread.sleep(wait.toMillis());
                } catch (InterruptedException e) {
                    log.warn("Interrupted");
                }
                this.timesExecuted.incrementAndGet();
            });
        }

        public int getTimesExecuted() {
            return timesExecuted.get();
        }
    }

    public static class ScheduleAnotherTaskHandler implements ExecutionHandler<Void> {
        private final TaskInstance<Void> newTask;
        private final Instant instant;
        public int timesExecuted = 0;

        public ScheduleAnotherTaskHandler(TaskInstance<Void> newTask, Instant instant) {
            this.newTask = newTask;
            this.instant = instant;
        }

        @Override
        public Mono<Void> execute(TaskInstance<Void> taskInstance, ExecutionContext<Void> context) {
            this.timesExecuted++;
            return context.getSchedulerClient().schedule(newTask, instant);
        }
    }

    public static class SavingHandler<T> implements ExecutionHandler<T> {
        @Getter
        private T savedData;

        @Override
        public Mono<T> execute(TaskInstance<T> taskInstance, ExecutionContext<T> context) {
            return Mono.fromRunnable(() -> savedData = taskInstance.getData());
        }
    }

}
