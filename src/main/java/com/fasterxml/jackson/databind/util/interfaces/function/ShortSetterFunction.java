package com.fasterxml.jackson.databind.util.interfaces.function;

@FunctionalInterface
public interface ShortSetterFunction<T> {
    short accept(T t, short value);
}
