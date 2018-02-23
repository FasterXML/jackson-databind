package com.fasterxml.jackson.databind.util;

/**
 * @since 3.0
 */
public interface Copyable<T> {
    public T copy();

    public static <T> T makeCopy(Copyable<T> src) {
        if (src == null) {
            return null;
        }
        return src.copy();
    }
}
