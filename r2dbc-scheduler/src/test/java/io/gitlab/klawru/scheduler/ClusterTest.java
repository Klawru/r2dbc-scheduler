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
import io.gitlab.klawru.scheduler.executor.execution.state.ExecutionStateName;
import io.gitlab.klawru.scheduler.task.OneTimeTask;
import io.gitlab.klawru.scheduler.task.callback.FailureHandler;
import io.gitlab.klawru.scheduler.util.Tasks;
import io.gitlab.klawru.scheduler.util.TestTasks;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Slf4j
class ClusterTest extends AbstractPostgresTest {

    private TaskScheduler schedulerA;
    private TaskScheduler schedulerB;

    private ConcurrentHashMap<ExecutionStateName, AtomicInteger> stateCount;
    private TestTasks.CountingHandler<Void> onetimeTaskHandlerA;
    private OneTimeTask<Void> oneTimeTaskA;

    @BeforeEach
    void setUp() {
        executeScriptFile("db.sql");
        onetimeTaskHandlerA = new TestTasks.CountingHandler<>();

        stateCount = new ConcurrentHashMap<>();
        oneTimeTaskA = Tasks.oneTime("oneTimeTaskA")
                .onComplete((executionComplete, newData, executionOperations) -> {
                    var stateName = executionComplete.currentState().getName();
                    stateCount.computeIfAbsent(stateName, s -> new AtomicInteger()).incrementAndGet();
                    return executionOperations.remove(executionComplete);
                })
                .onFailure(new FailureHandler.OnFailureRemove<>() {
                    @Override
                    public Mono<Void> onFailure(Execution<? super Void> execution, ExecutionOperations executionOperations) {
                        var stateName = execution.currentState().getName();
                        stateCount.computeIfAbsent(stateName, s -> new AtomicInteger()).incrementAndGet();
                        return super.onFailure(execution, executionOperations);
                    }
                })
                .execute(onetimeTaskHandlerA);
        schedulerA = new TaskScheduler(schedulerFor("schedulerA", oneTimeTaskA));
        schedulerB = new TaskScheduler(schedulerFor("schedulerB", oneTimeTaskA));
    }

    @AfterEach
    void tearDown() {
        Optional.ofNullable(schedulerA).ifPresent(TaskScheduler::close);
        Optional.ofNullable(schedulerB).ifPresent(TaskScheduler::close);
    }

    @Test
    void testConcurrency() {
        int taskSize = 1000;
        Flux.fromStream(IntStream.rangeClosed(1, taskSize).mapToObj(String::valueOf))
                .flatMap(id -> schedulerA.getClient().schedule(oneTimeTaskA.instance(id)))
                .blockLast();
        testClock.plusSecond(10);
        //When
        schedulerA.getClient().fetchTask();
        schedulerB.getClient().fetchTask();
        //Then
        await().atMost(10, SECONDS).until(() -> metricsRegistry.getAllTask() == taskSize);
        assertThat(stateCount.get(ExecutionStateName.COMPLETE)).hasValue(taskSize);
        assertThat(stateCount.keySet()).containsExactly(ExecutionStateName.COMPLETE);
    }
}
