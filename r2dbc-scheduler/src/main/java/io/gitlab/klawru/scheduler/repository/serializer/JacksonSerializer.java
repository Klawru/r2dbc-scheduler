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
package io.gitlab.klawru.scheduler.repository.serializer;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gitlab.klawru.scheduler.exception.SerializationException;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.function.Consumer;

public class JacksonSerializer implements Serializer {
    private final ObjectMapper objectMapper;

    public JacksonSerializer() {
        this(getDefaultObjectMapper());
    }

    public JacksonSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JacksonSerializer(Consumer<ObjectMapper> objectMapperCustomizer) {
        ObjectMapper defaultObjectMapper = getDefaultObjectMapper();
        objectMapperCustomizer.accept(defaultObjectMapper);
        this.objectMapper = defaultObjectMapper;
    }

    public static ObjectMapper getDefaultObjectMapper() {
        return new ObjectMapper()
                .findAndRegisterModules()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }

    @Override
    public byte @Nullable [] serialize(Object data) {
        if (data == null)
            return null;
        try {
            return objectMapper.writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            throw new SerializationException("Failed to serialize object.", e);
        }
    }

    @Override
    public <T> T deserialize(Class<T> clazz, byte[] serializedData) {
        if (serializedData == null)
            return null;
        try {
            return objectMapper.readValue(serializedData, clazz);
        } catch (IOException e) {
            throw new SerializationException("Failed to deserialize object.", e);
        }
    }

}
