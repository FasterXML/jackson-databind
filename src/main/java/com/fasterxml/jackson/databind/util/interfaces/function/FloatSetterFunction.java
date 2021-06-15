package com.fasterxml.jackson.databind.util.interfaces.function;

@FunctionalInterface
public interface FloatSetterFunction<T> {
    float accept(T t, float value);
}
