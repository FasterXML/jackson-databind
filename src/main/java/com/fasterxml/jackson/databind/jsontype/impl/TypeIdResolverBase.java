package com.fasterxml.jackson.databind.jsontype.impl;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.type.TypeFactory;

public abstract class TypeIdResolverBase
    implements TypeIdResolver
{
    protected final TypeFactory _typeFactory;

    /**
     * Common base type for all polymorphic instances handled.
     */
    protected final JavaType _baseType;

    protected TypeIdResolverBase(JavaType baseType, TypeFactory typeFactory)
    {
        _baseType = baseType;
        _typeFactory = typeFactory;
    }

    @Override
    public void init(JavaType bt) {
        // Standard type id resolvers do not need this: only useful for custom ones.
    }

    @Override
    public String idFromBaseType()
    {
        /* By default we will just defer to regular handling, handing out the
         * base type; and since there is no value, must just pass null here
         * assuming that implementations can deal with it.
         * Alternative would be to pass a bogus Object, but that does not seem right.
         */
        return idFromValueAndType(null, _baseType.getRawClass());
    }
}
