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

package io.gitlab.klawru.scheduler.executor;

import io.gitlab.klawru.scheduler.ExecutionOperations;
import io.gitlab.klawru.scheduler.SchedulerClient;
import io.gitlab.klawru.scheduler.config.SchedulerConfig;
import io.gitlab.klawru.scheduler.executor.execution.state.PickedState;
import io.gitlab.klawru.scheduler.repository.ExecutionEntity;
import io.gitlab.klawru.scheduler.repository.ExecutionMapper;
import io.gitlab.klawru.scheduler.stats.SchedulerMetricsRegistry;
import io.gitlab.klawru.scheduler.task.DefaultExecutionContext;
import io.gitlab.klawru.scheduler.task.ExecutionContext;
import io.gitlab.klawru.scheduler.task.ExecutionHandler;
import io.gitlab.klawru.scheduler.task.OneTimeTask;
import io.gitlab.klawru.scheduler.task.instance.TaskInstance;
import io.gitlab.klawru.scheduler.util.TestTasks;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;

class ExecutorServiceTest {

    ExecutorService executorService;
    TaskSchedulers taskSchedulers;
    SchedulerMetricsRegistry metricsRegistry;
    SchedulerClient mockSchedulerClient = Mockito.mock(SchedulerClient.class);
    ExecutionOperations executionOperations = Mockito.mock(ExecutionOperations.class);

    WaitingHandler waitingHandler;
    OneTimeTask<Void> waitingTask;

    @BeforeEach
    void setUp() {
        waitingHandler = new WaitingHandler();
        waitingTask = TestTasks.oneTime("waitingTask", waitingHandler);
        taskSchedulers = new DefaultTaskSchedulers(SchedulerConfig.builder()
                .threads(1)
                .build());
        metricsRegistry = new SchedulerMetricsRegistry();
        executorService = new ExecutorService(taskSchedulers, metricsRegistry);
    }

    @AfterEach
    void name() {
        taskSchedulers.close();
        executorService.stop(Duration.ZERO);
    }

    @Test
    void addToQueue() {
        Execution<Void> pickedExecution = getPickedExecution("addToQueue");
        SchedulerClient mockSchedulerClient = Mockito.mock(SchedulerClient.class);
        DefaultExecutionContext<Void> executionContext = DefaultExecutionContext.of(pickedExecution, mockSchedulerClient);
        ExecutionOperations executionOperations = Mockito.mock(ExecutionOperations.class);
        Mockito.when(executionOperations.remove(any())).thenReturn(Mono.empty());
        //When
        executorService.addToQueue(pickedExecution, executionContext, executionOperations);
        //Then
        assertThat(executorService.getNumberInQueueOrProcessing()).isEqualTo(1);
        assertThat(executorService.currentlyExecuting())
                .hasSize(1)
                .first()
                .returns(waitingTask.getName(), execution -> execution.getTaskInstance().getTaskName());

        waitingHandler.release();
        Mockito.verify(executionOperations, timeout(SECONDS.toMillis(1))).remove(any());
        assertThat(executorService.getNumberInQueueOrProcessing()).isZero();
        assertThat(executorService.currentlyExecuting()).isEmpty();
        assertThat(metricsRegistry.getCompleteTask()).isEqualTo(1);
    }

    @Test
    void removeFromQueue() {
        Execution<Void> pickedExecution = getPickedExecution("removeFromQueue");
        DefaultExecutionContext<Void> executionContext = DefaultExecutionContext.of(pickedExecution, mockSchedulerClient);
        //When
        executorService.addToQueue(pickedExecution, executionContext, executionOperations);
        executorService.removeFromQueue(pickedExecution);
        //Then
        await("Until remove task from queue")
                .atMost(10, SECONDS)
                .until(() -> executorService.getNumberInQueueOrProcessing(), equalTo(0));
        assertThat(executorService.currentlyExecuting()).isEmpty();
        assertThat(metricsRegistry.getFailedTask()).isEqualTo(1);
    }

    @NotNull
    private Execution<Void> getPickedExecution(String id) {
        ExecutionEntity executionEntity = new ExecutionEntity("waitingTask",
                id,
                "\"data\"".getBytes(StandardCharsets.UTF_8),
                Instant.now(),
                true,
                null,
                null,
                null,
                0,
                Instant.now(),
                2);

        return new ExecutionMapper().mapToExecution(executionEntity, new PickedState(), waitingTask, () -> null);
    }

    @RequiredArgsConstructor
    static class WaitingHandler implements ExecutionHandler<Void> {
        private final CountDownLatch barrier = new CountDownLatch(1);

        @Override
        public Mono<Void> execute(TaskInstance<Void> taskInstance, ExecutionContext<Void> context) {
            return Mono.fromRunnable(() -> {
                try {
                    barrier.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        public void release() {
            barrier.countDown();
        }
    }

}