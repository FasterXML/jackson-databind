package com.fasterxml.jackson.databind.util.interfaces.function;

@FunctionalInterface
public interface BooleanSetterFunction<T> {
    boolean accept(T t, boolean value);
}
