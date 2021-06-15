package com.fasterxml.jackson.databind.util.interfaces.function;

@FunctionalInterface
public interface CharSetterFunction<T> {
    char accept(T t, char value);
}
