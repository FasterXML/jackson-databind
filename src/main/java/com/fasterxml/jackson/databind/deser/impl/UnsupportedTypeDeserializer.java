package com.fasterxml.jackson.databind.deser.impl;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * Special bogus "serializer" that will throw
 * {@link com.fasterxml.jackson.databind.exc.MismatchedInputException}
 * if an attempt is made to deserialize a value.
 * This is used for "known unknown" types: types that we can recognize
 * but can not support easily (or support known to be added via extension
 * module).
 *<p>
 * NOTE: starting with 2.13, does allow deserialization from
 * {@code JsonToken.VALUE_EMBEDDED_OBJECT} if type matches (or is {@code null}).
 *
 * @since 2.12
 */
public class UnsupportedTypeDeserializer extends StdDeserializer<Object>
{
    private static final long serialVersionUID = 1L;

    protected final JavaType _type;

    protected final String _message;

    public UnsupportedTypeDeserializer(JavaType t, String m) {
        super(t);
        _type = t;
        _message = m;
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        // 26-May-2021, tatu: For [databind#3091], do allow reads from embedded values
        if (p.currentToken() == JsonToken.VALUE_EMBEDDED_OBJECT) {
            Object value = p.getEmbeddedObject();
            if ((value == null) || _type.getRawClass().isAssignableFrom(value.getClass())) {
                return value;
            }
        }
        ctxt.reportBadDefinition(_type, _message);
        return null;
    }
}
