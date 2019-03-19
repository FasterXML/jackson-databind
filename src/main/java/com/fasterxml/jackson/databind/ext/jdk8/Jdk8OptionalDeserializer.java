package com.fasterxml.jackson.databind.ext.jdk8;

import java.util.Optional;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.deser.std.ReferenceTypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;

public class Jdk8OptionalDeserializer
    extends ReferenceTypeDeserializer<Optional<?>>
{
    private static final long serialVersionUID = 1L;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    public Jdk8OptionalDeserializer(JavaType fullType, ValueInstantiator inst,
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
    public Jdk8OptionalDeserializer withResolved(TypeDeserializer typeDeser, JsonDeserializer<?> valueDeser) {
        return new Jdk8OptionalDeserializer(_fullType, _valueInstantiator,
                typeDeser, valueDeser);
    }

    @Override
    public Optional<?> getNullValue(DeserializationContext ctxt) {
        return Optional.empty();
    }

    @Override
    public Optional<?> referenceValue(Object contents) {
        return Optional.ofNullable(contents);
    }

    @Override
    public Object getReferenced(Optional<?> reference) {
        return reference.get();
    }

    @Override // since 2.9
    public Optional<?> updateReference(Optional<?> reference, Object contents) {
        return Optional.ofNullable(contents);
    }

    // Default ought to be fine:
//    public Boolean supportsUpdate(DeserializationConfig config) { }

}
