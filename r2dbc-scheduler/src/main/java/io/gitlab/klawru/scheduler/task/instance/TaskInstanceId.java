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
package io.gitlab.klawru.scheduler.task.instance;

import lombok.Value;

public interface TaskInstanceId {
    static TaskInstanceId of(String taskName, String id) {
        return new DefaultTaskInstanceId(taskName, id);
    }

    String getTaskName();

    String getId();

    default String getTaskNameId() {
        return "{" + getTaskName() + ":" + getId() + "}";
    }

    @Value
    class DefaultTaskInstanceId implements TaskInstanceId {
        String taskName;
        String id;

        @Override
        public String toString() {
            return getTaskNameId();
        }
    }
}
