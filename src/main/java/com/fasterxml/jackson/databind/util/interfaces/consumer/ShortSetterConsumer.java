package com.fasterxml.jackson.databind.util.interfaces.consumer;

@FunctionalInterface
public interface ShortSetterConsumer<T> {
    void accept(T t, short value);
}
