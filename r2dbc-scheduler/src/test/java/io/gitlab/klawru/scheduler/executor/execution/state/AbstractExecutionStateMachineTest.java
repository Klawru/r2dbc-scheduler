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

package io.gitlab.klawru.scheduler.executor.execution.state;

import io.gitlab.klawru.scheduler.exception.IllegalStateChangeException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AbstractExecutionStateMachineTest {

    @Test
    void changeState() {
        PickedState pickedState = new PickedState();
        var machine = new ExecutionStateMachine(pickedState);

        assertThatThrownBy(() -> machine.changeState(pickedState))
                .isInstanceOf(IllegalStateChangeException.class);
    }

    @Test
    void currentState() {
        EnqueuedState enqueuedState = new EnqueuedState(UUID.randomUUID());
        var machine = new ExecutionStateMachine(new PickedState());
        machine.changeState(enqueuedState);
        //Then
        assertThat(machine.currentState())
                .isEqualTo(enqueuedState);
    }

    @Test
    void getLastState() {
        EnqueuedState enqueuedState = new EnqueuedState(UUID.randomUUID());
        var machine = new ExecutionStateMachine(new PickedState());
        machine.changeState(enqueuedState);
        machine.changeState(new ProcessedState());
        //Then
        assertThat(machine.getLastState(ExecutionStateName.ENQUEUED))
                .get()
                .isEqualTo(enqueuedState);
    }

    @Test
    void canTransition() {
        FailedState failedState = new FailedState(new RuntimeException());
        CompleteState completeState = new CompleteState();

        assertThat(ExecutionStateMachine.canTransition(completeState, failedState)).isTrue();
        assertThat(ExecutionStateMachine.canTransition(failedState, completeState)).isFalse();
    }

}