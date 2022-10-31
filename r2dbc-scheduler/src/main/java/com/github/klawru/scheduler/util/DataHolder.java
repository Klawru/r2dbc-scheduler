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
package com.github.klawru.scheduler.util;

import com.github.klawru.scheduler.exception.DataHolderException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DataHolder<T> {
    private static final DataHolder<?> EMPTY_DATA = new DataHolder<>(null);
    Supplier<T> data;

    public static <T> DataHolder<T> of(@Nullable T data) {
        return new DataHolder<>(() -> data);
    }

    @SuppressWarnings("unchecked")
    public static <T> DataHolder<T> empty() {
        return (DataHolder<T>) EMPTY_DATA;
    }

    public <R> DataHolder<R> map(Function<T, R> mapper) {
        if (isEmpty())
            return empty();
        return new DataHolder<>(MapperUtil.memoize(() -> mapper.apply(data.get())));
    }

    @Nullable
    public T getData() {
        if (isEmpty())
            throw new DataHolderException("No data present");
        return data.get();
    }

    public boolean isEmpty() {
        return data == null;
    }

    public boolean isPresent() {
        return !isEmpty();
    }
}
