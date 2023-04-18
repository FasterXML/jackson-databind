package com.fasterxml.jackson.databind.type;

import com.fasterxml.jackson.databind.JavaType;

/**
 * Internal abstract type representing {@link TypeBase} implementations which use reference equality.
 *
 * @since 2.15
 */
abstract class IdentityEqualityType extends TypeBase
{
    private static final long serialVersionUID = 1L;

    protected IdentityEqualityType(Class<?> raw,
            TypeBindings bindings, JavaType superClass, JavaType[] superInts,
            int hash,
            Object valueHandler, Object typeHandler, boolean asStatic) {
        super(raw, bindings, superClass, superInts, hash, valueHandler, typeHandler, asStatic);
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
