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

package io.gitlab.klawru.scheduler.service;

import io.gitlab.klawru.scheduler.config.SchedulerConfiguration;
import io.gitlab.klawru.scheduler.executor.DefaultTaskSchedulers;
import io.gitlab.klawru.scheduler.repository.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.scheduler.VirtualTimeScheduler;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;

@Slf4j
class DeadExecutionDetectServiceTest {
    Duration pollingInterval = Duration.ofSeconds(1);
    Duration deadExecutionDetectInterval = pollingInterval.multipliedBy(2);

    VirtualTimeScheduler virtualTimeScheduler;
    TaskService mockTaskService;
    DeadExecutionDetectService deleteUnresolvedTaskService;

    @BeforeEach
    void setUp() {
        SchedulerConfiguration configuration = SchedulerConfiguration.builder()
                .heartbeatInterval(pollingInterval)
                .pollingInterval(pollingInterval)
                .schedulerName("UpdateHeartbeatServiceTest")
                .build();

        virtualTimeScheduler = VirtualTimeScheduler.getOrSet();
        DefaultTaskSchedulers taskSchedulers = new DefaultTaskSchedulers(2, 2,
                2,
                virtualTimeScheduler,
                virtualTimeScheduler);
        mockTaskService = Mockito.mock(TaskService.class);
        deleteUnresolvedTaskService = new DeadExecutionDetectService(mockTaskService, taskSchedulers, configuration);
    }

    @AfterAll
    static void tearDown() {
        VirtualTimeScheduler.reset();
    }

    /**
     * @see DeadExecutionDetectService
     */
    @Test
    void detectDeadExecution() {
        Mockito.when(mockTaskService.rescheduleDeadExecutionTask(any())).thenReturn(Mono.just(1L));
        //Then
        StepVerifier.withVirtualTime(() -> deleteUnresolvedTaskService.getRescheduleDeadExecutionFlux().log(log.getName()), 4)
                .expectSubscription()
                .expectNoEvent(deadExecutionDetectInterval)
                .expectNext(1L)
                .expectNoEvent(deadExecutionDetectInterval)
                .expectNext(1L)
                .thenCancel()
                .verify(deadExecutionDetectInterval.multipliedBy(10));
    }

    @Test
    void start() {
        Mockito.when(mockTaskService.rescheduleDeadExecutionTask(any())).thenReturn(Mono.just(1L));
        //When
        deleteUnresolvedTaskService.start();
        virtualTimeScheduler.advanceTimeBy(deadExecutionDetectInterval.multipliedBy(2));
        //Then
        Mockito.verify(mockTaskService, atLeast(1)).rescheduleDeadExecutionTask(any());
        deleteUnresolvedTaskService.pause();
    }

    @Test
    void pause() {
        Mockito.when(mockTaskService.rescheduleDeadExecutionTask(any())).thenReturn(Mono.just(1L));
        //When
        deleteUnresolvedTaskService.start();
        virtualTimeScheduler.advanceTimeBy(deadExecutionDetectInterval);
        deleteUnresolvedTaskService.pause();
        Mockito.reset(mockTaskService);
        virtualTimeScheduler.advanceTimeBy(deadExecutionDetectInterval.multipliedBy(2));
        //Then
        Mockito.verify(mockTaskService, Mockito.timeout(pollingInterval.toMillis()).times(0)).rescheduleDeadExecutionTask(any());
    }
}