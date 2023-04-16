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
import io.gitlab.klawru.scheduler.task.ExecutionHandler;
import io.gitlab.klawru.scheduler.task.OneTimeTask;
import io.gitlab.klawru.scheduler.task.callback.FailureHandler;
import io.gitlab.klawru.scheduler.task.instance.TaskInstance;
import io.gitlab.klawru.scheduler.util.Tasks;
import io.gitlab.klawru.scheduler.util.TestTasks;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class SchedulerClientTest extends AbstractPostgresTest {

    private TaskScheduler scheduler;

    private TestTasks.CountingHandler<Void> taskHandlerA;
    private OneTimeTask<Void> taskA;

    private TestTasks.CountingHandler<Void> taskHandlerB;
    private OneTimeTask<Void> taskB;

    private TestTasks.ScheduleAnotherTaskHandler scheduleAnother;
    private OneTimeTask<Void> scheduleAnotherTask;

    private TestTasks.SavingHandler<String> savingHandler;
    private OneTimeTask<String> savingTask;

    private ExecutionHandler<String> errorHandler;
    private OneTimeTask<String> errorTask;

    private OneTimeTask<String> errorTaskRemoveAfterError;

    @BeforeEach
    void setUp() {
        taskHandlerA = new TestTasks.CountingHandler<>();
        taskA = TestTasks.oneTime("taskA", taskHandlerA);

        taskHandlerB = new TestTasks.CountingHandler<>();
        taskB = TestTasks.oneTime("taskB", taskHandlerB);

        scheduleAnother = new TestTasks.ScheduleAnotherTaskHandler(taskA.instance("scheduleAnotherTask"), testClock.now().plusSeconds(10));
        scheduleAnotherTask = TestTasks.oneTime("scheduleAnother", scheduleAnother);

        savingHandler = new TestTasks.SavingHandler<>();
        savingTask = TestTasks.oneTimeWithType("savingTask", String.class, savingHandler);

        errorHandler = (taskInstance, context) -> {
            throw new RuntimeException("Task exception");
        };
        errorTask = TestTasks.oneTimeWithType("errorTask", String.class, errorHandler);
        errorTaskRemoveAfterError = Tasks.oneTime("errorTaskThenRemove", String.class)
                .onFailure(new FailureHandler.OnFailureRemove<>())
                .execute(errorHandler);

        var client = schedulerFor(taskA, taskB, scheduleAnotherTask, savingTask, errorTask, errorTaskRemoveAfterError);
        scheduler = new TaskScheduler(client);
    }

    @AfterEach
    void tearDown() {
        Optional.ofNullable(scheduler.getClient()).ifPresent(SchedulerClient::pause);
    }

    @Test
    void schedule() {
        //When
        scheduler.schedule(taskA.instance("1"));
        //Then
        wailAllExecutionDone();
        assertThat(taskHandlerA.getTimesExecuted()).isEqualTo(1);
    }

    @Test
    void scheduleWithTime() {
        //When
        scheduler.schedule(taskB.instance("2"), testClock.now().plusSeconds(10));
        //Then
        wailAllExecutionDone();
        assertThat(taskHandlerB.getTimesExecuted()).isZero();
        testClock.plusSecond(10);
        wailAllExecutionDone();
        assertThat(taskHandlerB.getTimesExecuted()).isEqualTo(1);
    }

    @Test
    void scheduleWithTimeAndData() {
        String data = "SAVE";
        //When
        scheduler.schedule(savingTask.instance("3"), testClock.now().plusSeconds(10), data);
        //Then
        wailAllExecutionDone();
        assertThat(savingHandler.getSavedData()).isNull();
        testClock.plusSecond(10);
        wailAllExecutionDone();
        assertThat(savingHandler.getSavedData()).isEqualTo(data);
    }

    @Test
    void rescheduleWithTimeAndData() {
        String data = "SAVE";
        //When
        scheduler.schedule(savingTask.instance("3"), testClock.now(), data);
        //Then
        wailAllExecutionDone();
        assertThat(savingHandler.getSavedData()).isEqualTo(data);
    }


    @Test
    void reschedule() {
        String data1 = "data1";

        scheduler.schedule(savingTask.instance("1", data1), testClock.now().plusSeconds(10));
        scheduler.reschedule(savingTask.instance("1"), testClock.now());

        wailAllExecutionDone();
        assertThat(savingHandler.getSavedData()).isEqualTo(data1);
    }

    @Test
    void rescheduleWithDataUpdateTest() {
        String data = "data";

        scheduler.schedule(savingTask.instance("2", (String) null), testClock.now().plusSeconds(10));
        scheduler.reschedule(savingTask.instance("2", data), data);

        wailAllExecutionDone();
        assertThat(savingHandler.getSavedData()).isEqualTo(data);

        testClock.plusSecond(10);
        wailAllExecutionDone();
        assertThat(savingHandler.getSavedData()).isEqualTo(data);
    }

    @Test
    void rescheduleWithTime() {
        String data = "data";

        scheduler.schedule(savingTask.instance("2", (String) null), testClock.now().plusSeconds(10));
        scheduler.reschedule(savingTask.instance("2"), testClock.now(), data);

        wailAllExecutionDone();
        assertThat(savingHandler.getSavedData()).isEqualTo(data);
    }


    @Test
    void rescheduleWithTimeAndDataTest() {
        String data = "data";

        scheduler.schedule(savingTask.instance("2", (String) null), testClock.now().plusSeconds(10));
        scheduler.reschedule(savingTask.instance("2", data), testClock.now(), data);

        wailAllExecutionDone();
        assertThat(savingHandler.getSavedData()).isEqualTo(data);
    }

    @Test
    void rescheduleWithNullData() {
        String data = "data";

        scheduler.schedule(savingTask.instance("2", data), testClock.now().plusSeconds(10));
        scheduler.reschedule(savingTask.instance("2", (String) null), testClock.now(), data);

        wailAllExecutionDone();
        assertThat(savingHandler.getSavedData()).isEqualTo(data);

        testClock.plusSecond(10);
        wailAllExecutionDone();
        assertThat(savingHandler.getSavedData()).isEqualTo(data);
    }


    @Test
    void scheduleFromAnotherTask() {
        scheduler.schedule(scheduleAnotherTask.instance("4"), testClock.now());
        wailAllExecutionDone();

        assertThat(scheduleAnother.timesExecuted).isEqualTo(1);
        assertThat(taskHandlerA.getTimesExecuted()).isZero();

        testClock.plusSecond(10);
        wailAllExecutionDone();
        assertThat(taskHandlerA.getTimesExecuted()).isEqualTo(1);
    }


    @Test
    void rescheduleWhenError() {
        TaskInstance<String> taskInstance = errorTask.instance("5", "data");
        scheduler.schedule(errorTask.instance("5", "data"), testClock.now());
        wailAllExecutionDone();
        Assertions.assertThat(scheduler.getAllExecution())
                .hasSize(1)
                .first()
                .returns(taskInstance.getTaskName(), execution -> execution.getTaskInstance().getTaskName())
                .returns(taskInstance.getId(), execution -> execution.getTaskInstance().getId())
                .returns(false, Execution::isPicked)
                .returns(null, Execution::getPickedBy)
                .returns(1, Execution::getConsecutiveFailures)
                .returns(testClock.now(), Execution::getLastFailure);
    }

    @Test
    void removeWhenError() {
        TaskInstance<String> taskInstance = errorTaskRemoveAfterError.instance("5", "data");
        scheduler.schedule(taskInstance);
        wailAllExecutionDone();
        Assertions.assertThat(scheduler.getAllExecution()).isEmpty();
    }

    @Test
    void scheduleMultipleTaskTest() {
        scheduler.schedule(taskA.instance("5"), testClock.now().plusSeconds(10));
        scheduler.schedule(taskA.instance("6"), testClock.now().plusSeconds(10));
        scheduler.schedule(taskB.instance("17"), testClock.now().plusSeconds(10));
        scheduler.schedule(taskB.instance("18"), testClock.now().plusSeconds(10));
        scheduler.schedule(taskB.instance("19"), testClock.now().plusSeconds(10));

        Assertions.assertThat(scheduler.getAllExecution()).hasSize(5);
        Assertions.assertThat(scheduler.getScheduledExecutions()).hasSize(5);
        assertThat(countExecutionsForTask(scheduler, taskA.getName(), Void.class)).isEqualTo(2);
        assertThat(countExecutionsForTask(scheduler, taskB.getName(), Void.class)).isEqualTo(3);
        Assertions.assertThat(scheduler.getScheduledExecutionsForTask(taskB.getName(), Void.class)).hasSize(3);
    }

    @Test
    void getExecutionTest() {
        scheduler.schedule(taskA.instance("1"), testClock.now().plusSeconds(10));

        Assertions.assertThat(scheduler.getExecution(taskA.instance("1"))).isPresent();
        Assertions.assertThat(scheduler.getExecution(taskA.instance("2"))).isEmpty();
        Assertions.assertThat(scheduler.getExecution(taskB.instance("1"))).isEmpty();
    }

    private void wailAllExecutionDone() {
        wailAllExecutionDone(scheduler.getClient());
    }

    private <T> int countExecutionsForTask(TaskScheduler client, String taskName, Class<T> dataClass) {
        return client.getScheduledExecutionsForTask(taskName, dataClass).size();
    }


}
