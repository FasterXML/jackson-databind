package com.fasterxml.jackson.databind.util;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class UnsafeUtil {
    private static Unsafe unsafe;
    public static Unsafe getUnsafe() {
        if(unsafe==null){
            try {
                Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
                Field f = unsafeClass.getDeclaredField("theUnsafe");
                f.setAccessible(true);
                unsafe = (Unsafe) f.get(null);
                return unsafe;
            } catch (Exception ignored) {
                return null;
            }
        }
        return unsafe;
    }
}
