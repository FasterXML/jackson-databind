package com.fasterxml.jackson.databind.ext;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;

/**
 * Since v2.15, {@link Java7HandlersImpl} is no longer loaded via reflection.
 * <p>
 *     Prior to v2.15, this class supported Java7-incomplete platforms, specifically
 *     platforms that do not support {@link java.nio.file.Path}.
 * </p>
 *
 * @since 2.10 (cleaved off of {@link Java7Support})
 */
public abstract class Java7Handlers
{
    private final static Java7Handlers IMPL = new Java7HandlersImpl();

    public static Java7Handlers instance() {
        return IMPL;
    }

    public abstract Class<?> getClassJavaNioFilePath();

    public abstract JsonDeserializer<?> getDeserializerForJavaNioFilePath(Class<?> rawType);

    public abstract JsonSerializer<?> getSerializerForJavaNioFilePath(Class<?> rawType);
}
