package com.fasterxml.jackson.databind.ext;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.util.ClassUtil;

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
    private final static Java7Handlers IMPL;

    static {
        Java7Handlers impl = null;
        try {
            Class<?> cls = Class.forName("com.fasterxml.jackson.databind.ext.Java7HandlersImpl");
            impl = (Java7Handlers) ClassUtil.createInstance(cls, false);
        } catch (Throwable t) {
            // 09-Sep-2019, tatu: Could choose not to log this, but since this is less likely
            //    to miss (than annotations), do it
            // 02-Nov-2020, Xakep_SDK: Remove java.logging module dependency
//            java.util.logging.Logger.getLogger(Java7Handlers.class.getName())
//                .warning("Unable to load JDK7 types (java.nio.file.Path): no Java7 type support added");
        }
        IMPL = impl;
    }

    public static Java7Handlers instance() {
        return IMPL;
    }

    public abstract Class<?> getClassJavaNioFilePath();

    public abstract JsonDeserializer<?> getDeserializerForJavaNioFilePath(Class<?> rawType);

    public abstract JsonSerializer<?> getSerializerForJavaNioFilePath(Class<?> rawType);
}
