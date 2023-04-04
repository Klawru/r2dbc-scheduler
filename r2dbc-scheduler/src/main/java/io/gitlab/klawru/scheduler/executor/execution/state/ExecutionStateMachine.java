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
package io.gitlab.klawru.scheduler.executor.execution.state;

import io.gitlab.klawru.scheduler.exception.IllegalStateChangeException;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

public class ExecutionStateMachine {
    private final ArrayList<ExecutionState> states;

    public ExecutionStateMachine(ExecutionState first) {
        states = new ArrayList<>(5);
        states.add(Objects.requireNonNull(first, "First state can not null"));
    }

    public <T extends ExecutionState> T changeState(T next) {
        ExecutionState current = currentState();
        if (!canTransition(current, next))
            throw new IllegalStateChangeException(current, next);
        states.add(next);
        return next;
    }

    @Override
    public String toString() {
        return "{" +
                "state=" + getLastState() + "," +
                "historySize=" + states.size() +
                '}';
    }

    public ExecutionState currentState() {
        return states.get(states.size() - 1);
    }

    public Optional<ExecutionState> getLastState(ExecutionStateName nameState) {
        for (int i = states.size() - 1; i > 0; i--) {
            ExecutionState state = states.get(i);
            if (state.getName() == nameState)
                return Optional.of(state);
        }
        return Optional.empty();
    }

    public boolean canTransition(ExecutionState current, ExecutionState next) {
        return canTransition(current.getName(), next.getName());
    }

    public boolean canTransition(ExecutionStateName current, ExecutionStateName next) {
        switch (current) {
            case PICKED:
                return next == ExecutionStateName.ENQUEUED;
            case ENQUEUED:
                return ExecutionStateName.PROCESSING == next || next == ExecutionStateName.FAILED;
            case PROCESSING:
                return ExecutionStateName.COMPLETE == next || ExecutionStateName.FAILED == next;
            case COMPLETE:
                return ExecutionStateName.FAILED == next;
            case DEAD_EXECUTION:
            case FAILED:
            case VIEW:
                return false;
            default:
                throw new IllegalStateException("Unexpected value: " + current);
        }
    }

    public ExecutionState getLastState() {
        return states.get(states.size() - 1);
    }
}
