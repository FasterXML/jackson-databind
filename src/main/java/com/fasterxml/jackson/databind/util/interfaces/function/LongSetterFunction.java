package com.fasterxml.jackson.databind.util.interfaces.function;

@FunctionalInterface
public interface LongSetterFunction<T> {
    long accept(T t, long value);
}
