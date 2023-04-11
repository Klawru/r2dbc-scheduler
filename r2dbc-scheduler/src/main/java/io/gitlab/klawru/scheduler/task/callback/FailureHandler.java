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
package io.gitlab.klawru.scheduler.task.callback;

import io.gitlab.klawru.scheduler.ExecutionOperations;
import io.gitlab.klawru.scheduler.executor.Execution;
import io.gitlab.klawru.scheduler.executor.execution.state.ExecutionState;
import io.gitlab.klawru.scheduler.executor.execution.state.FailedState;
import io.gitlab.klawru.scheduler.task.instance.TaskInstance;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

@FunctionalInterface
public interface FailureHandler<T> {
    int DEFAULT_RETRY = 3;
    Duration DEFAULT_RETRY_INTERVAL = Duration.ofMinutes(5);

    Mono<Void> onFailure(Execution<? super T> executionComplete, ExecutionOperations executionOperations);


    @Slf4j
    class OnFailureRemove<T> implements FailureHandler<T> {

        @Override
        public Mono<Void> onFailure(Execution<? super T> execution, ExecutionOperations executionOperations) {
            ExecutionState executionState = execution.currentState();
            TaskInstance<? super T> taskInstance = execution.getTaskInstance();
            if (executionState instanceof FailedState) {
                var failedState = (FailedState) executionState;
                log.warn("Job '{}' failed {}-times, and removed", taskInstance.getTaskNameId(), execution.getConsecutiveFailures() + 1, failedState.getCause());
            } else
                log.warn("Job '{}' failed {}-times, and removed", taskInstance.getTaskNameId(), execution.getConsecutiveFailures() + 1);
            return executionOperations.remove(execution);
        }
    }

    @Slf4j
    class OnFailureRetryLater<T> implements FailureHandler<T> {
        private final int retry;
        private final Duration duration;

        public OnFailureRetryLater(int retry, Duration sleepDuration) {
            if (retry < 1)
                throw new IllegalArgumentException("The number of retries cannot be less than 1");
            this.retry = retry;
            this.duration = sleepDuration;
        }


        @Override
        public Mono<Void> onFailure(Execution<? super T> execution, ExecutionOperations executionOperations) {
            ExecutionState executionState = execution.currentState();
            TaskInstance<? super T> taskInstance = execution.getTaskInstance();
            if (execution.getConsecutiveFailures() <= retry) {
                Instant nextTime = executionState.getCreateTime().plus(duration);
                log.debug("Job '{}:{}' failed {}-times. Rescheduled at {}", taskInstance.getTaskName(), taskInstance.getId(), execution.getConsecutiveFailures() + 1, nextTime);
                return executionOperations.reschedule(execution, nextTime).then();
            } else {
                log.warn("Job '{}:{}' failed {}-times, and removed", taskInstance.getTaskName(), taskInstance.getId(), execution.getConsecutiveFailures() + 1);
                return executionOperations.remove(execution);
            }
        }
    }
}