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

import io.gitlab.klawru.scheduler.task.instance.TaskInstance;
import lombok.*;
import org.jetbrains.annotations.Nullable;

@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class TaskExample<T> {
    @Nullable
    private final String id;
    @Nullable
    private final String name;
    @Nullable
    private final Boolean picked;
    @NonNull
    private final Class<T> dataClass;

    public static <T> TaskExample<T> of(TaskInstance<T> instance) {
        return new TaskExample<>(instance.getId(),
                instance.getTaskName(),
                null,
                instance.getTask().getDataClass());
    }

    public static TaskExample<Object> all() {
        return new TaskExample<>(null,
                null,
                null,
                Object.class);
    }

    public static TaskExample<Object> picked() {
        return new TaskExample<>(null,
                null,
                true,
                Object.class);
    }

    public static <T> TaskExample<T> picked(String taskName, Class<T> dataClass) {
        return new TaskExample<>(null,
                taskName,
                true,
                dataClass);
    }

    public static TaskExample<Object> scheduled() {
        return new TaskExample<>(null,
                null,
                false,
                Object.class);
    }

    public static TaskExample<Object> scheduled(String taskName) {
        return new TaskExample<>(null,
                taskName,
                false,
                Object.class);
    }

    public static <T> TaskExample<T> scheduled(String taskName, Class<T> dataClass) {
        return new TaskExample<>(null,
                taskName,
                false,
                dataClass);
    }
}
