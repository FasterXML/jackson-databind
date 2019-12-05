package com.fasterxml.jackson.databind.util.interfaces.consumer;

@FunctionalInterface
public interface ByteSetterConsumer<T> {
    void accept(T t, byte value);
}
