package com.fasterxml.jackson.databind.util.interfaces.consumer;

@FunctionalInterface
public interface CharSetterConsumer<T> {
    void accept(T t, char value);
}
