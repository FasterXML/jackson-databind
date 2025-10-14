package tools.jackson.databind.deser.jdk;

import java.util.concurrent.atomic.AtomicReference;

import tools.jackson.databind.*;
import tools.jackson.databind.deser.ValueInstantiator;
import tools.jackson.databind.deser.std.ReferenceTypeDeserializer;
import tools.jackson.databind.jsontype.TypeDeserializer;

public class AtomicReferenceDeserializer
    extends ReferenceTypeDeserializer<AtomicReference<Object>>
{
    public AtomicReferenceDeserializer(JavaType fullType, ValueInstantiator inst,
            TypeDeserializer typeDeser, ValueDeserializer<?> deser)
    {
        super(fullType, inst, typeDeser, deser);
    }

    /*
    /**********************************************************
    /* Abstract method implementations
    /**********************************************************
     */

    @Override
    public AtomicReferenceDeserializer withResolved(TypeDeserializer typeDeser, ValueDeserializer<?> valueDeser) {
        return new AtomicReferenceDeserializer(_fullType, _valueInstantiator,
                typeDeser, valueDeser);
    }

    @Override
    public AtomicReference<Object> getNullValue(DeserializationContext ctxt) {
        // 07-May-2019, tatu: [databind#2303], needed for nested ReferenceTypes
        return new AtomicReference<>(_valueDeserializer.getNullValue(ctxt));
    }

    @Override
    public AtomicReference<Object> referenceValue(Object contents) {
        return new AtomicReference<>(contents);
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
