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
package io.gitlab.klawru.scheduler;

import io.gitlab.klawru.scheduler.executor.Execution;
import io.gitlab.klawru.scheduler.task.instance.NextExecutionTime;
import reactor.core.publisher.Mono;

import java.time.Instant;


public interface ExecutionOperations {
    Mono<Void> remove(Execution<?> completed);

    Mono<Void> reschedule(Execution<?> execution);

    Mono<Void> reschedule(Execution<?> execution, Instant nextExecutionTime);

    <T> Mono<Void> reschedule(Execution<? super T> execution, NextExecutionTime nextExecutionTime);
}
