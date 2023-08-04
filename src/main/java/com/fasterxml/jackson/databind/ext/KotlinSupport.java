package com.fasterxml.jackson.databind.ext;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class KotlinSupport {
    public static boolean isJvmInlineClassSyntheticConstructor(Constructor<?> ctor) {
        Class<?>[] params = ctor.getParameterTypes();
        if (params.length == 0) {
            return false;
        }

        Class<?> lastParam = params[params.length - 1];
        return ctor.isSynthetic() && lastParam.getName().equals("kotlin.jvm.internal.DefaultConstructorMarker");
    }

    public static boolean isJvmInlineClassSyntheticBoxingFunction(Method method) {
        return Modifier.isStatic(method.getModifiers())
                && method.isSynthetic()
                && method.getName().equals("box-impl");
    }
}
