package tools.jackson.databind.ser.impl;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.*;
import tools.jackson.databind.jsontype.TypeSerializer;

/**
 * Simple serializer that will call configured type serializer, passing
 * in configured data serializer, and exposing it all as a simple
 * serializer.
 */
public final class TypeWrappedSerializer
    extends ValueSerializer<Object>
{
    final protected TypeSerializer _typeSerializer;
    final protected ValueSerializer<Object> _serializer;

    @SuppressWarnings("unchecked")
    public TypeWrappedSerializer(TypeSerializer typeSer, ValueSerializer<?> ser)
    {
        super();
        _typeSerializer = typeSer;
        _serializer = (ValueSerializer<Object>) ser;
    }

    @Override
    public void serialize(Object value, JsonGenerator g, SerializerProvider provider) throws JacksonException {
        _serializer.serializeWithType(value, g, provider, _typeSerializer);
    }

    @Override
    public void serializeWithType(Object value, JsonGenerator g, SerializerProvider provider,
            TypeSerializer typeSer) throws JacksonException
    {
        // Is this an erroneous call? For now, let's assume it is not, and
        // that type serializer is just overridden if so
        _serializer.serializeWithType(value, g, provider, typeSer);
    }

    @Override
    public Class<Object> handledType() { return Object.class; }

    /*
    /**********************************************************************
    /* ContextualDeserializer
    /**********************************************************************
     */

    @Override
    public ValueSerializer<?> createContextual(SerializerProvider provider, BeanProperty property)
    {
        // 13-Mar-2017, tatu: Should we call `TypeSerializer.forProperty()`?
        ValueSerializer<?> ser = _serializer;
        if (ser != null) {
            ser = provider.handleSecondaryContextualization(ser, property);
        }
        if (ser == _serializer) {
            return this;
        }
        return new TypeWrappedSerializer(_typeSerializer, ser);
    }

    /*
    /**********************************************************************
    /* Extended API for other core classes
    /**********************************************************************
     */

    public ValueSerializer<Object> valueSerializer() {
        return _serializer;
    }

    public TypeSerializer typeSerializer() {
        return _typeSerializer;
    }
}
