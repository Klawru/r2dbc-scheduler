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
package io.gitlab.klawru.scheduler.exception;

import io.gitlab.klawru.scheduler.task.instance.TaskInstanceId;
import lombok.Getter;

@Getter
public class RepositoryException extends AbstractSchedulerException {
    private final String taskName;
    private final String id;

    public RepositoryException(String message, String taskName, String taskInstanceId) {
        super(message);
        this.taskName = taskName;
        this.id = taskInstanceId;
    }

    public RepositoryException(String message, TaskInstanceId taskInstance) {
        super(message);
        this.taskName = taskInstance.getId();
        this.id = taskInstance.getTaskName();
    }
}
