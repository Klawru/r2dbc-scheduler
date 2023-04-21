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

package io.gitlab.klawru.scheduler;

import io.gitlab.klawru.scheduler.exception.TaskServiceException;
import io.gitlab.klawru.scheduler.task.AbstractTask;
import io.gitlab.klawru.scheduler.task.OneTimeTask;
import io.gitlab.klawru.scheduler.task.RecurringTask;
import io.gitlab.klawru.scheduler.task.schedule.FixedDelayScheduler;
import io.gitlab.klawru.scheduler.util.Tasks;
import io.gitlab.klawru.scheduler.util.TestClock;
import io.gitlab.klawru.scheduler.util.TestTasks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskResolverTest {

    TaskResolver taskResolver;

    OneTimeTask<Void> oneTimeTask;
    RecurringTask<Void> recurringTask;

    @BeforeEach
    void setUp() {
        TestClock testClock = new TestClock();
        oneTimeTask = TestTasks.oneTime("oneTime", TestTasks.EMPTY_EXECUTION_HANDLER);
        recurringTask = Tasks.recurring("recurring", new FixedDelayScheduler(Duration.ofHours(1)))
                .execute(TestTasks.EMPTY_EXECUTION_HANDLER);
        Collection<AbstractTask<?>> tasks = List.of(
                oneTimeTask,
                recurringTask
        );
        taskResolver = new TaskResolver(tasks, testClock);
    }

    @Test
    void addTask() {
        OneTimeTask<Void> newTask = TestTasks.oneTime("newTask", TestTasks.EMPTY_EXECUTION_HANDLER);
        List<OneTimeTask<Void>> sameTask = List.of(newTask);
        //When
        taskResolver.add(sameTask);
        //Then
        assertThat(taskResolver.findTask("newTask"))
                .get()
                .isEqualTo(newTask);
    }

    @Test
    void addTaskWithSameNameThenError() {
        List<OneTimeTask<Void>> sameTask = List.of(TestTasks.oneTime("oneTime", TestTasks.EMPTY_EXECUTION_HANDLER));
        //Then
        assertThatThrownBy(() -> taskResolver.add(sameTask))
                .isInstanceOf(TaskServiceException.class)
                .hasMessage("Task with same name already exist");
    }

    @Test
    void findTask() {
        String name = oneTimeTask.getName();
        //When
        Optional<AbstractTask<?>> actual = taskResolver.findTask(name);
        //Then
        assertThat(actual)
                .isPresent()
                .get()
                .returns(name, AbstractTask::getName);
    }

    @Test
    void findTaskWhenNotFoundThenEmpty() {
        String unknownTask = "unknownTask";
        //When
        Optional<AbstractTask<?>> actual = taskResolver.findTask(unknownTask);
        //Then
        assertThat(actual).isEmpty();
        assertThat(taskResolver.getUnresolvedName())
                .hasSize(1)
                .contains(unknownTask);
    }
}