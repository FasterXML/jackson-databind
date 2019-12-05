package com.fasterxml.jackson.databind.util.interfaces.consumer;

@FunctionalInterface
public interface DoubleSetterConsumer<T> {
    void accept(T t, double value);
}
