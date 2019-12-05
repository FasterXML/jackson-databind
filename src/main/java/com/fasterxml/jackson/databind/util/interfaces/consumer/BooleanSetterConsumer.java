package com.fasterxml.jackson.databind.util.interfaces.consumer;

@FunctionalInterface
public interface BooleanSetterConsumer<T> {
    void accept(T t, boolean value);
}
