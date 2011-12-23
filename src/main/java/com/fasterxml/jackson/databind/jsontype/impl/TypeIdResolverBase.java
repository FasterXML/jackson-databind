package com.fasterxml.jackson.databind.jsontype.impl;

import org.codehaus.jackson.type.JavaType;

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
        /* Standard type id resolvers do not need this;
         * only useful for custom ones.
         */
    }
}
