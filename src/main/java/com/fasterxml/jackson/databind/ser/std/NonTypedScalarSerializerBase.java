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
@SuppressWarnings("serial")
@Deprecated // since 2.9
public abstract class NonTypedScalarSerializerBase<T>
    extends StdScalarSerializer<T>
{
    protected NonTypedScalarSerializerBase(Class<T> t) {
        super(t);
    }

    protected NonTypedScalarSerializerBase(Class<?> t, boolean bogus) {
        super(t, bogus);
    }

    @Override
    public final void serializeWithType(T value, JsonGenerator gen, SerializerProvider provider,
            TypeSerializer typeSer) throws IOException
    {
        // no type info, just regular serialization
        serialize(value, gen, provider);
    }
}
