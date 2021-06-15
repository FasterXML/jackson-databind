package com.fasterxml.jackson.databind.util.interfaces.consumer;

@FunctionalInterface
public interface FloatSetterConsumer<T> {
    void accept(T t, float value);
}
