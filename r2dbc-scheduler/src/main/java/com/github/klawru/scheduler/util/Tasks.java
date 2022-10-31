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
package com.github.klawru.scheduler.util;

import com.github.klawru.scheduler.task.schedule.Scheduler;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Tasks {

    public static OneTimeTaskBuilder<Void> oneTime(String name) {
        return new OneTimeTaskBuilder<>(name, Void.class);
    }

    public static <T> OneTimeTaskBuilder<T> oneTime(String name, Class<T> dataClass) {
        return new OneTimeTaskBuilder<>(name, dataClass);
    }

    public static RecurringTaskBuilder<Void> recurring(String name, Scheduler scheduler) {
        return new RecurringTaskBuilder<>(name, Void.class, scheduler);
    }

    public static <T> RecurringTaskBuilder<T> recurring(String name, Class<T> dataClass, Scheduler scheduler) {
        return new RecurringTaskBuilder<>(name, dataClass, scheduler);
    }

}
