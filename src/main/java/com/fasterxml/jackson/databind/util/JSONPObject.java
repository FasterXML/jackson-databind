package com.fasterxml.jackson.databind.util;

import java.io.IOException;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

/**
 * Container class that can be used to wrap any Object instances (including
 * nulls), and will serialize embedded in
 * <a href="http://en.wikipedia.org/wiki/JSONP">JSONP</a> wrapping.
 *
 * @see com.fasterxml.jackson.databind.util.JSONWrappedObject
 */
public class JSONPObject
    implements JsonSerializable
{
    /**
     * JSONP function name to use for serialization
     */
    protected final String _function;

    /**
     * Value to be serialized as JSONP padded; can be null.
     */
    protected final Object _value;

    /**
     * Optional static type to use for serialization; if null, runtime
     * type is used. Can be used to specify declared type which defines
     * serializer to use, as well as aspects of extra type information
     * to include (if any).
     */
    protected final JavaType _serializationType;

    public JSONPObject(String function, Object value) {
        this(function, value, (JavaType) null);
    }

    public JSONPObject(String function, Object value, JavaType asType)
    {
        _function = function;
        _value = value;
        _serializationType = asType;
    }

    /*
    /**********************************************************
    /* JsonSerializable(WithType) implementation
    /**********************************************************
     */

    @Override
    public void serializeWithType(JsonGenerator gen, SerializerProvider provider, TypeSerializer typeSer)
            throws IOException
    {
        // No type for JSONP wrapping: value serializer will handle typing for value:
        serialize(gen, provider);
    }

    @Override
    public void serialize(JsonGenerator gen, SerializerProvider provider)
            throws IOException
    {
        // First, wrapping:
        gen.writeRaw(_function);
        gen.writeRaw('(');

        if (_value == null) {
            provider.defaultSerializeNull(gen);
        } else {
            // NOTE: Escape line-separator characters that break JSONP only if no custom character escapes are set.
            // If custom escapes are in place JSONP-breaking characters will not be escaped and it is recommended to
            // add escaping for those (see JsonpCharacterEscapes class).
            boolean override = (gen.getCharacterEscapes() == null);
            if (override) {
                gen.setCharacterEscapes(JsonpCharacterEscapes.instance());
            }

            try {
                if (_serializationType != null) {
                    provider.findTypedValueSerializer(_serializationType, true, null).serialize(_value, gen, provider);
                } else {
                    provider.findTypedValueSerializer(_value.getClass(), true, null).serialize(_value, gen, provider);
                }
            } finally {
                if (override) {
                    gen.setCharacterEscapes(null);
                }
            }
        }
        gen.writeRaw(')');
    }

    /*
    /**************************************************************
    /* Accessors
    /**************************************************************
     */

    public String getFunction() { return _function; }
    public Object getValue() { return _value; }
    public JavaType getSerializationType() { return _serializationType; }
}
