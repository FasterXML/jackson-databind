package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;

/**
 * Base class for deserializers that handle types that are serialized
 * as JSON scalars (non-structured, i.e. non-Object, non-Array, values).
 */
public abstract class StdScalarDeserializer<T> extends StdDeserializer<T>
{
    // @since 2.8.8
    protected final static int FEATURES_ACCEPT_ARRAYS =
            DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS.getMask() |
            DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT.getMask();

    private static final long serialVersionUID = 1L;

    protected StdScalarDeserializer(Class<?> vc) { super(vc); }
    protected StdScalarDeserializer(JavaType valueType) { super(valueType); }

    // since 2.5
    protected StdScalarDeserializer(StdScalarDeserializer<?> src) { super(src); }
    
    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt, TypeDeserializer typeDeserializer) throws IOException {
        return typeDeserializer.deserializeTypedFromScalar(p, ctxt);
    }

    protected T _deserializeFromArray(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        JsonToken t;
        if (ctxt.hasSomeOfFeatures(FEATURES_ACCEPT_ARRAYS)) {
            t = p.nextToken();
            if (t == JsonToken.END_ARRAY) {
                if (ctxt.isEnabled(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT)) {
                    return getNullValue(ctxt);
                }
            }
            if (ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
                final T parsed = deserialize(p, ctxt);
                if (p.nextToken() != JsonToken.END_ARRAY) {
                    handleMissingEndArrayForSingle(p, ctxt);
                }
                return parsed;            
            }
        } else {
            t = p.getCurrentToken();
        }
        @SuppressWarnings("unchecked")
        T result = (T) ctxt.handleUnexpectedToken(_valueClass, t, p, null);
        return result;
    }
}
