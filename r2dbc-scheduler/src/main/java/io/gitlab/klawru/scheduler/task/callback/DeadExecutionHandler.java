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
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@FunctionalInterface
public interface DeadExecutionHandler<T> {

    Mono<Void> deadExecution(Execution<? super T> execution, ExecutionOperations executionOperations);


    @Slf4j
    class CancelDeadExecution<T> implements DeadExecutionHandler<T> {

        @Override
        public Mono<Void> deadExecution(Execution<? super T> execution, ExecutionOperations executionOperations) {
            log.debug("Cancelling dead execution: " + execution);
            return executionOperations.remove(execution);
        }
    }

    @Slf4j
    class ReviveDeadExecution<T> implements DeadExecutionHandler<T> {

        @Override
        public Mono<Void> deadExecution(Execution<? super T> execution, ExecutionOperations executionOperations) {
            return executionOperations.reschedule(execution, execution.getTaskInstance().getNextExecutionTime());
        }
    }
}
