package com.fasterxml.jackson.databind.ext;

import java.nio.file.Path;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;

/**
 * @since 2.10
 */
public class Java7HandlersImpl extends Java7Handlers
{
    @Override
    public Class<?> getClassJavaNioFilePath() {
        return Path.class;
    }

    @Override
    public JsonDeserializer<?> getDeserializerForJavaNioFilePath(Class<?> rawType) {
        if (rawType == Path.class) {
            return new NioPathDeserializer();
        }
        return null;
    }

    @Override
    public JsonSerializer<?> getSerializerForJavaNioFilePath(Class<?> rawType) {
        if (Path.class.isAssignableFrom(rawType)) {
            return new NioPathSerializer();
        }
        return null;
    }
}
