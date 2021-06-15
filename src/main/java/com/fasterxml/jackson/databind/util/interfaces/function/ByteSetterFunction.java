package com.fasterxml.jackson.databind.util.interfaces.function;

@FunctionalInterface
public interface ByteSetterFunction<T> {
    byte accept(T t, byte value);
}
