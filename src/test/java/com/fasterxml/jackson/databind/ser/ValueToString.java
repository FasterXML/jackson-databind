package com.fasterxml.jackson.databind.ser;

public interface ValueToString<T> {
    String convert(T value);
}
