package com.fasterxml.jackson.databind.util.interfaces.consumer;

@FunctionalInterface
public interface LongSetterConsumer<T> {
    void accept(T t, long value);
}
