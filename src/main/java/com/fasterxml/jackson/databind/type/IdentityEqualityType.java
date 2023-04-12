package com.fasterxml.jackson.databind.type;

import com.fasterxml.jackson.databind.JavaType;

/**
 * Internal abstract type representing {@link TypeBase} implementations which use reference equality.
 */
abstract class IdentityEqualityType extends TypeBase {
    protected IdentityEqualityType(
            Class<?> raw,
            TypeBindings bindings,
            JavaType superClass,
            JavaType[] superInts,
            int hash,
            Object valueHandler,
            Object typeHandler,
            boolean asStatic) {
        super(raw, bindings, superClass, superInts, hash, valueHandler, typeHandler, asStatic);
    }

    protected IdentityEqualityType(TypeBase base) {
        super(base);
    }

    @Override
    public final boolean equals(Object o) {
        return o == this;
    }

    @Override
    public final int hashCode() {
        // The identity hashCode must be used otherwise all instances will have colliding hashCodes.
        return System.identityHashCode(this);
    }
}
