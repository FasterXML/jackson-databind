package com.fasterxml.jackson.databind.testutil;

import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;

/**
 * Test-only {@link PolymorphicTypeValidator} used by tests that should not block
 * use of any subtypes.
 */
public final class NoCheckSubTypeValidator
    extends PolymorphicTypeValidator
{
    private static final long serialVersionUID = 1L;

    public final static NoCheckSubTypeValidator instance = new NoCheckSubTypeValidator(); 

    @Override
    public Validity validateBaseType(DatabindContext ctxt, JavaType baseType) {
        return Validity.INDETERMINATE;
    }

    @Override
    public Validity validateSubClassName(DatabindContext ctxt,
            JavaType baseType, String subClassName) {
        return Validity.ALLOWED;
    }

    @Override
    public Validity validateSubType(DatabindContext ctxt, JavaType baseType,
            JavaType subType) {
        return Validity.ALLOWED;
    }
}