package com.fasterxml.jackson.databind.deser.std;

import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;

public class AtomicReferenceDeserializer
    extends ReferenceTypeDeserializer<AtomicReference<Object>>
{
    private static final long serialVersionUID = 1L;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    @Deprecated // since 2.8
    public AtomicReferenceDeserializer(JavaType fullType) {
        this(fullType, null, null);
    }

    public AtomicReferenceDeserializer(JavaType fullType,
            TypeDeserializer typeDeser, JsonDeserializer<?> deser)
    {
        super(fullType, typeDeser, deser);
    }

    /*
    /**********************************************************
    /* Abstract method implementations
    /**********************************************************
     */

    @Override
    public AtomicReferenceDeserializer withResolved(TypeDeserializer typeDeser, JsonDeserializer<?> valueDeser) {
        return new AtomicReferenceDeserializer(_fullType, typeDeser, valueDeser);
    }

    @Override
    public AtomicReference<Object> getNullValue(DeserializationContext ctxt) {
        return new AtomicReference<Object>();
    }

    @Override
    public AtomicReference<Object> referenceValue(Object contents) {
        return new AtomicReference<Object>(contents);
    }

    /*
    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt,
            TypeDeserializer typeDeser) throws IOException
    {
        final JsonToken t = p.getCurrentToken();
        if (t == JsonToken.VALUE_NULL) { // can this actually happen?
            return getNullValue(ctxt);
        }
        // 22-Oct-2015, tatu: This handling is probably not needed (or is wrong), but
        //   could be result of older (pre-2.7) Jackson trying to serialize natural types.
        //  Because of this, let's allow for now, unless proven problematic
        if ((t != null) && t.isScalarValue()) {
            return deserialize(p, ctxt);
        }
        // 19-Apr-2016, tatu: Alas, due to there not really being anything for AtomicReference
        //   itself, need to just ignore `typeDeser`, use TypeDeserializer we do have for contents
        //   and it might just work.

        if (_valueTypeDeserializer == null) {
            return deserialize(p, ctxt);
        }
        return new AtomicReference<Object>(_valueTypeDeserializer.deserializeTypedFromAny(p, ctxt));
    }
    */
}
