package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.fasterxml.jackson.databind.util.AccessPattern;

/**
 * Base class for deserializers that handle types that are serialized
 * as JSON scalars (non-structured, i.e. non-Object, non-Array, values).
 */
public abstract class StdScalarDeserializer<T> extends StdDeserializer<T>
{
    private static final long serialVersionUID = 1L;

    protected StdScalarDeserializer(Class<?> vc) { super(vc); }
    protected StdScalarDeserializer(JavaType valueType) { super(valueType); }

    // since 2.5
    protected StdScalarDeserializer(StdScalarDeserializer<?> src) { super(src); }

    /*
    /**********************************************************************
    /* Overridden accessors
    /**********************************************************************
     */

    @Override // since 2.12
    public LogicalType logicalType() {
        return LogicalType.OtherScalar;
    }

    /**
     * By default assumption is that scalar types cannot be updated: many are immutable
     * values (such as primitives and wrappers)
     */
    @Override // since 2.9
    public Boolean supportsUpdate(DeserializationConfig config) {
        return Boolean.FALSE;
    }

    // Typically Scalar values have default setting of "nulls as nulls"
    @Override
    public AccessPattern getNullAccessPattern() {
        return AccessPattern.ALWAYS_NULL;
    }

    // While some scalar types have non-null empty values (hence can't say "ALWAYS_NULL")
    // they are mostly immutable, shareable and so constant.
    @Override // since 2.9
    public AccessPattern getEmptyAccessPattern() {
        return AccessPattern.CONSTANT;
    }

    /*
    /**********************************************************************
    /* Default deserialization method impls
    /**********************************************************************
     */

    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt, TypeDeserializer typeDeserializer) throws IOException {
        return typeDeserializer.deserializeTypedFromScalar(p, ctxt);
    }

    /**
     * Overridden to simply call <code>deserialize()</code> method that does not take value
     * to update, since scalar values are usually non-mergeable.
     */
    @Override // since 2.9
    public T deserialize(JsonParser p, DeserializationContext ctxt, T intoValue) throws IOException {
        // 25-Oct-2016, tatu: And if attempt is made, see if we are to complain...
        ctxt.handleBadMerge(this);
        // if that does not report an exception we can just delegate
        return deserialize(p, ctxt);
    }
}
