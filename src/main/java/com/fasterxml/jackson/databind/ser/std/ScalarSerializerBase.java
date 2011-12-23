package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.lang.reflect.Type;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.TypeSerializer;

public abstract class ScalarSerializerBase<T>
    extends SerializerBase<T>
{
    protected ScalarSerializerBase(Class<T> t) {
        super(t);
    }

    /**
     * Alternate constructor that is (alas!) needed to work
     * around kinks of generic type handling
     */
    @SuppressWarnings("unchecked")
    protected ScalarSerializerBase(Class<?> t, boolean dummy) {
        super((Class<T>) t);
    }
    
    /**
     * Default implementation will write type prefix, call regular serialization
     * method (since assumption is that value itself does not need JSON
     * Array or Object start/end markers), and then write type suffix.
     * This should work for most cases; some sub-classes may want to
     * change this behavior.
     */
    @Override
    public void serializeWithType(T value, JsonGenerator jgen, SerializerProvider provider,
            TypeSerializer typeSer)
        throws IOException, JsonGenerationException
    {
        typeSer.writeTypePrefixForScalar(value, jgen);
        serialize(value, jgen, provider);
        typeSer.writeTypeSuffixForScalar(value, jgen);
    }

    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        throws JsonMappingException
    {
        return createSchemaNode("string", true);
    }
}
