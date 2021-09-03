package com.fasterxml.jackson.databind.deser.std;

import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
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

    /**
     * @since 2.9
     */
    public AtomicReferenceDeserializer(JavaType fullType, ValueInstantiator inst,
            TypeDeserializer typeDeser, JsonDeserializer<?> deser)
    {
        super(fullType, inst, typeDeser, deser);
    }

    /*
    /**********************************************************
    /* Abstract method implementations
    /**********************************************************
     */

    @Override
    public AtomicReferenceDeserializer withResolved(TypeDeserializer typeDeser, JsonDeserializer<?> valueDeser) {
        return new AtomicReferenceDeserializer(_fullType, _valueInstantiator,
                typeDeser, valueDeser);
    }

    @Override
    public AtomicReference<Object> getNullValue(DeserializationContext ctxt) throws JsonMappingException {
        // 07-May-2019, tatu: [databind#2303], needed for nested ReferenceTypes
        return new AtomicReference<Object>(_valueDeserializer.getNullValue(ctxt));
    }

    @Override
    public Object getEmptyValue(DeserializationContext ctxt) throws JsonMappingException {
        // 07-May-2019, tatu: I _think_ this needs to align with "null value" and
        //    not necessarily with empty value of contents? (used to just do "absent"
        //    so either way this seems to me like an improvement)
        return getNullValue(ctxt);
    }

    /**
     * Let's actually NOT coerce missing Creator parameters into empty value.
     */
    @Override // @since 2.13
    public Object getAbsentValue(DeserializationContext ctxt) throws JsonMappingException {
        return null;
    }

    @Override
    public AtomicReference<Object> referenceValue(Object contents) {
        return new AtomicReference<Object>(contents);
    }

    @Override
    public Object getReferenced(AtomicReference<Object> reference) {
        return reference.get();
    }

    @Override // since 2.9
    public AtomicReference<Object> updateReference(AtomicReference<Object> reference, Object contents) {
        reference.set(contents);
        return reference;
    }

    @Override // since 2.9
    public Boolean supportsUpdate(DeserializationConfig config) {
        // yes; regardless of value deserializer reference itself may be updated
        return Boolean.TRUE;
    }
}
