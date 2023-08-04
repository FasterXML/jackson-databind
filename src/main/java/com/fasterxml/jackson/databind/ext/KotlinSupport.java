package com.fasterxml.jackson.databind.ext;

import java.lang.reflect.Constructor;

public class KotlinSupport {
    public static boolean isJvmInlineClassSyntheticConstructor(Constructor<?> ctor) {
        Class<?>[] params = ctor.getParameterTypes();
        if (params.length == 0) {
            return false;
        }

        Class<?> lastParam = params[params.length - 1];
        return ctor.isSynthetic() && lastParam.getName().equals("kotlin.jvm.internal.DefaultConstructorMarker");
    }
}
