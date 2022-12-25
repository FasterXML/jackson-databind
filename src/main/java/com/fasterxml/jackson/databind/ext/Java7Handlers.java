package com.fasterxml.jackson.databind.ext;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;

/**
 * To support Java7-incomplete platforms, we will offer support for JDK 7
 * datatype(s) (that is, {@link java.nio.file.Path} through this class, loaded
 * dynamically; if loading fails, support will be missing.
 * This class is the non-JDK-7-dependent API, and {@link Java7HandlersImpl} is
 * JDK7-dependent implementation of functionality.
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
