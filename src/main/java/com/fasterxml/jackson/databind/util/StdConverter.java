package com.fasterxml.jackson.databind.util;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * Standard implementation of {@link Converter} that supports explicit
 * type access, instead of relying type detection of generic type
 * parameters. 
 * 
 * @since 2.2
 */
public abstract class StdConverter<IN,OUT>
    implements Converter<IN,OUT>
{
    /*
    /**********************************************************
    /* Partial Converter API implementation
    /**********************************************************
     */

    @Override
    public abstract OUT convert(IN value);

    @Override
    public JavaType getInputType(TypeFactory typeFactory)
    {
        JavaType[] types = typeFactory.findTypeParameters(getClass(), Converter.class);
        if (types == null || types.length < 2) {
            throw new IllegalStateException("Can not find OUT type parameter for Converter of type "+getClass().getName());
        }
        return types[0];
    }

    @Override
    public JavaType getOutputType(TypeFactory typeFactory)
    {
        JavaType[] types = typeFactory.findTypeParameters(getClass(), Converter.class);
        if (types == null || types.length < 2) {
            throw new IllegalStateException("Can not find OUT type parameter for Converter of type "+getClass().getName());
        }
        return types[1];
    }
}
