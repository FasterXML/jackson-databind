package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

/**
 * Intermediate base class for limited number of scalar types
 * that should never include type information. These are "native"
 * types that are default mappings for corresponding JSON scalar
 * types: {@link java.lang.String}, {@link java.lang.Integer},
 * {@link java.lang.Double} and {@link java.lang.Boolean}.
 */
public abstract class NonTypedScalarSerializerBase<T>
    extends StdScalarSerializer<T>
{
    protected NonTypedScalarSerializerBase(Class<T> t) {
        super(t);
    }

    @Override
    public final void serializeWithType(T value, JsonGenerator jgen, SerializerProvider provider,
            TypeSerializer typeSer)
        throws IOException, JsonGenerationException
    {
        // no type info, just regular serialization
        serialize(value, jgen, provider);            
    }
}
