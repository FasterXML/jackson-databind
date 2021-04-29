package com.fasterxml.jackson.databind.util;

import sun.reflect.ReflectionFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ReflectionUtil {

    private static final Map<Class<?>, Constructor<?>> constructorCache =
            new ConcurrentHashMap<>();

    public static Object newConstructorAndCreateInstance(Class<?> classToInstantiate) {
        if (classToInstantiate.isInterface() || Modifier.isAbstract(classToInstantiate.getModifiers())) {
            return null;
        }
        Constructor<?> constructor = constructorCache.get(classToInstantiate);

        try {
            if (constructor == null) {
                constructor = ReflectionFactory.getReflectionFactory()
                        .newConstructorForSerialization(classToInstantiate, Object.class.getDeclaredConstructor());
                constructor.setAccessible(true);
                constructorCache.put(classToInstantiate, constructor);
            }
            return constructor.newInstance();
        } catch (Exception e) {
            return null;
        }
    }
}