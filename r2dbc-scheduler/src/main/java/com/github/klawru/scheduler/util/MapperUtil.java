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

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.function.Supplier;

@UtilityClass
public class MapperUtil {

    public static <T> Supplier<T> memoize(Supplier<T> original) {
        return new MemoizeSupplier<>(original);
    }

    public static <T> Mono<T> get(Mono<Optional<T>> flux) {
        return flux.filter(Optional::isPresent)
                .map(Optional::get);
    }

    public static <T> Flux<T> get(Flux<Optional<T>> optionalFlux) {
        return optionalFlux.filter(Optional::isPresent).map(Optional::get);
    }

    public static <T> Mono<Optional<T>> mapToOptional(Mono<T> tMono) {
        return tMono
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty());
    }

    static class MemoizeSupplier<T extends @Nullable Object> implements Supplier<T> {
        Supplier<T> delegate;
        volatile boolean initialized;
        private T value;

        MemoizeSupplier(@NotNull Supplier<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public T get() {
            //Double-Checked Locking
            if (!initialized) {
                synchronized (this) {
                    if (!initialized) {
                        T t = delegate.get();
                        value = t;
                        initialized = true;
                        delegate = null;
                        return t;
                    }
                }
            }
            return value;
        }
    }

}
