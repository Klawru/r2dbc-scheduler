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
import reactor.core.publisher.Mono;

import java.util.Optional;

@FunctionalInterface
public interface CompletionHandler<T> {

    Mono<Void> complete(Execution<? super T> executionComplete, Optional<? super T> newData, ExecutionOperations executionOperations);


    class OnCompleteRemove<T> implements CompletionHandler<T> {

        @Override
        public Mono<Void> complete(Execution<? super T> executionComplete, Optional<? super T> newData, ExecutionOperations executionOperations) {
            return executionOperations.remove(executionComplete);
        }
    }


    class OnCompleteReschedule<T> implements CompletionHandler<T> {

        @Override
        public Mono<Void> complete(Execution<? super T> executionComplete, Optional<? super T> newData, ExecutionOperations executionOperations) {
            return executionOperations.reschedule(executionComplete);
        }
    }

}
