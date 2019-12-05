package com.fasterxml.jackson.databind.util.interfaces.function;

@FunctionalInterface
public interface IntSetterFunction<T> {
    int accept(T t, int value);
}
