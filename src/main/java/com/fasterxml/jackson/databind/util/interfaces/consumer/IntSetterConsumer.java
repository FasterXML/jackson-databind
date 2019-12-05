package com.fasterxml.jackson.databind.util.interfaces.consumer;

@FunctionalInterface
public interface IntSetterConsumer<T> {
    void accept(T t, int value);
}
