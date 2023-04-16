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

package io.gitlab.klawru.scheduler.service;

import io.gitlab.klawru.scheduler.config.SchedulerConfiguration;
import io.gitlab.klawru.scheduler.executor.DefaultTaskSchedulers;
import io.gitlab.klawru.scheduler.executor.Execution;
import io.gitlab.klawru.scheduler.executor.TaskExecutor;
import io.gitlab.klawru.scheduler.executor.execution.state.ProcessedState;
import io.gitlab.klawru.scheduler.repository.TaskService;
import io.gitlab.klawru.scheduler.task.OneTimeTask;
import io.gitlab.klawru.scheduler.task.instance.TaskInstance;
import io.gitlab.klawru.scheduler.util.TestTasks;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.scheduler.VirtualTimeScheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;

@Slf4j
class UpdateHeartbeatServiceTest {
    Duration heartbeatDuration = Duration.ofSeconds(1);
    OneTimeTask<Void> id = TestTasks.oneTime("task", ((taskInstance, context) -> Mono.empty()));

    VirtualTimeScheduler virtualTimeScheduler;
    TaskService mockTaskService;
    TaskExecutor mockTaskExecutor;
    UpdateHeartbeatService heartbeatService;

    @BeforeEach
    void setUp() {
        SchedulerConfiguration configuration = SchedulerConfiguration.builder()
                .heartbeatInterval(heartbeatDuration)
                .schedulerName("UpdateHeartbeatServiceTest")
                .build();

        virtualTimeScheduler = VirtualTimeScheduler.getOrSet();
        DefaultTaskSchedulers taskSchedulers = new DefaultTaskSchedulers(2, 2,
                2,
                virtualTimeScheduler,
                virtualTimeScheduler);
        mockTaskService = Mockito.mock(TaskService.class);
        mockTaskExecutor = Mockito.mock(TaskExecutor.class);


        heartbeatService = new UpdateHeartbeatService(mockTaskService, mockTaskExecutor, taskSchedulers, configuration);
    }

    @AfterAll
    static void tearDown() {
        VirtualTimeScheduler.reset();
    }

    @Test
    void start() {
        Mockito.when(mockTaskExecutor.currentlyExecuting())
                .then((invocation) -> Stream.of(createExecution(id.instance("id"))));
        Mockito.when(mockTaskService.updateHeartbeat(any())).thenReturn(Mono.just(true));
        //When
        heartbeatService.start();
        virtualTimeScheduler.advanceTimeBy(heartbeatDuration.multipliedBy(1));
        //Then
        Mockito.verify(mockTaskService, atLeast(1)).updateHeartbeat(any());
        heartbeatService.pause();
    }

    @Test
    void pause() {
        Mockito.when(mockTaskExecutor.currentlyExecuting())
                .then((invocation) -> Stream.of(createExecution(id.instance("id"))));
        Mockito.when(mockTaskService.updateHeartbeat(any())).thenReturn(Mono.just(true));
        //When
        heartbeatService.start();
        virtualTimeScheduler.advanceTimeBy(heartbeatDuration);
        heartbeatService.pause();
        Mockito.reset(mockTaskService);
        virtualTimeScheduler.advanceTimeBy(heartbeatDuration.multipliedBy(2));
        //Then
        Mockito.verify(mockTaskService, Mockito.timeout(heartbeatDuration.toMillis()).times(0)).updateHeartbeat(any());
    }


    @Test
    void updatingHeartbeatFluxWhenOkThenOk() {
        OneTimeTask<Void> id = TestTasks.oneTime("task", ((taskInstance, context) -> Mono.empty()));
        Mockito.when(mockTaskExecutor.currentlyExecuting())
                .then((invocation) -> Stream.of(createExecution(id.instance("id"))));
        Mockito.when(mockTaskService.updateHeartbeat(any())).thenReturn(Mono.just(true));
        //Then
        StepVerifier.withVirtualTime(() -> heartbeatService.updatingHeartbeatFlux().log(log.getName()), 2)
                .expectSubscription()
                .expectNoEvent(heartbeatDuration)
                .expectNext(true)
                .expectNoEvent(heartbeatDuration)
                .expectNext(true)
                .thenCancel()
                .verify(heartbeatDuration.multipliedBy(4));
    }

    @Test
    void updatingHeartbeatFluxWhenCantUpdateThenStopTask() {
        OneTimeTask<Void> id = TestTasks.oneTime("task", ((taskInstance, context) -> Mono.empty()));
        Mockito.when(mockTaskExecutor.currentlyExecuting())
                .then((invocation) -> Stream.of(createExecution(id.instance("id"))));
        Mockito.when(mockTaskService.updateHeartbeat(any())).thenReturn(Mono.just(false));
        //Then
        StepVerifier.withVirtualTime(() -> heartbeatService.updatingHeartbeatFlux().log(log.getName()), 2)
                .expectSubscription()
                .expectNoEvent(heartbeatDuration)
                .expectNext(false)
                .thenCancel()
                .verify(heartbeatDuration.multipliedBy(4));

        Mockito.verify(mockTaskExecutor).removeFromQueue(any());
    }


    @NotNull
    private static <T> Execution<T> createExecution(TaskInstance<T> id) {
        return new Execution<>(
                id,
                new ProcessedState(),
                Instant.now(),
                true,
                "this",
                0,
                Instant.now(),
                2,
                null,
                null
        );
    }
}