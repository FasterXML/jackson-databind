package com.fasterxml.jackson.databind.ext.jdk8;

import java.util.Optional;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.deser.std.ReferenceTypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;

public class Jdk8OptionalDeserializer
    extends ReferenceTypeDeserializer<Optional<?>>
{
    public Jdk8OptionalDeserializer(JavaType fullType, ValueInstantiator inst,
            TypeDeserializer typeDeser, JsonDeserializer<?> deser)
    {
        super(fullType, inst, typeDeser, deser);
    }

    /*
    /**********************************************************************
    /* Abstract method implementations
    /**********************************************************************
     */

    @Override
    public Jdk8OptionalDeserializer withResolved(TypeDeserializer typeDeser, JsonDeserializer<?> valueDeser) {
        return new Jdk8OptionalDeserializer(_fullType, _valueInstantiator,
                typeDeser, valueDeser);
    }

    @Override
    public Optional<?> getNullValue(DeserializationContext ctxt) throws JsonMappingException {
        // 07-May-2019, tatu: changed for [databind#2303]
        return Optional.ofNullable(_valueDeserializer.getNullValue(ctxt));
    }

    @Override
    public Object getEmptyValue(DeserializationContext ctxt) throws JsonMappingException {
        // 07-May-2019, tatu: I _think_ this needs to align with "null value" and
        //    not necessarily with empty value of contents? (used to just do "absent"
        //    so either way this seems to me like an improvement)
        return getNullValue(ctxt);
    }

    @Override
    public Optional<?> referenceValue(Object contents) {
        return Optional.ofNullable(contents);
    }

    @Override
    public Object getReferenced(Optional<?> reference) {
        return reference.get();
    }

    @Override
    public Optional<?> updateReference(Optional<?> reference, Object contents) {
        return Optional.ofNullable(contents);
    }

    // Default ought to be fine:
//    public Boolean supportsUpdate(DeserializationConfig config) { }

}
