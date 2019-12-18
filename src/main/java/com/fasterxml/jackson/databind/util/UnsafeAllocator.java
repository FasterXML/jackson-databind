package com.fasterxml.jackson.databind.util;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class UnsafeAllocator {
    private static Unsafe unsafe;
    static{
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field f = unsafeClass.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafe = (Unsafe) f.get(null);
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
    }
    public static Object beanAllocatorByJvmUnsafe(Class<?> clazz) throws InstantiationException {
        if(unsafe==null){
            throw new InstantiationException(String.format("Jvm Unsafe could't find,Make sure load unsafe security in [%s]",clazz.getName()));
        }
        return unsafe.allocateInstance(clazz);
    }
}
