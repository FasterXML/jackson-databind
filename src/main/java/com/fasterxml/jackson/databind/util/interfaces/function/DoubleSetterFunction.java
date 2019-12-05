package com.fasterxml.jackson.databind.util.interfaces.function;

@FunctionalInterface
public interface DoubleSetterFunction<T> {
    double accept(T t, double value);
}
