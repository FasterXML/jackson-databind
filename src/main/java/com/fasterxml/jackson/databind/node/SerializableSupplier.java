package com.fasterxml.jackson.databind.node;

import java.io.Serializable;
import java.util.function.Supplier;

public interface SerializableSupplier<T> extends Supplier<T>, Serializable {
    // empty
}
