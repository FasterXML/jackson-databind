package com.fasterxml.jackson.databind.util;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.databind.JsonSerializable;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

/**
 * Helper class used to encapsulate "raw values", pre-encoded textual content
 * that can be output as opaque value with no quoting/escaping, using
 * {@link com.fasterxml.jackson.core.JsonGenerator#writeRawValue(String)}.
 * It may be stored in {@link TokenBuffer}, as well as in Tree Model
 * ({@link com.fasterxml.jackson.databind.JsonNode})
 * 
 * @since 2.6
 */
public class RawValue
    implements JsonSerializable
{
    protected Object _value;

    public RawValue(String v) {
        _value = v;
    }

    public RawValue(SerializableString v) {
        _value = v;
    }

    public RawValue(JsonSerializable v) {
        _value = v;
    }
    
    protected RawValue(Object value, boolean bogus) {
        _value = value;
    }

    /**
     * Accessor for returning enclosed raw value in whatever form it was created in
     * (usually {@link java.lang.String}, {link SerializableString}, or any {@link JsonSerializable}).
     */
    public Object rawValue() {
        return _value;
    }
    
    @Override
    public void serialize(JsonGenerator gen, SerializerProvider serializers) throws IOException
    {
        if (_value instanceof JsonSerializable) {
            ((JsonSerializable) _value).serialize(gen, serializers);
        } else {
            _serialize(gen);
        }
    }

    @Override
    public void serializeWithType(JsonGenerator gen, SerializerProvider serializers,
            TypeSerializer typeSer) throws IOException
    {
        if (_value instanceof JsonSerializable) {
            ((JsonSerializable) _value).serializeWithType(gen, serializers, typeSer);
        } else if (_value instanceof SerializableString) {
            /* Since these are not really to be deserialized (with or without type info),
             * just re-route as regular write, which will create one... hopefully it works
             */
            serialize(gen, serializers);
        }
    }

    public void serialize(JsonGenerator gen) throws IOException
    {
        if (_value instanceof JsonSerializable) {
            // No SerializerProvider passed, must go via generator, callback
            gen.writeObject(_value);
        } else {
            _serialize(gen);
        }
    }

    protected void _serialize(JsonGenerator gen) throws IOException
    {
        if (_value instanceof SerializableString) {
            gen.writeRawValue((SerializableString) _value);
        } else {
            gen.writeRawValue(String.valueOf(_value));
        }
    }
}
