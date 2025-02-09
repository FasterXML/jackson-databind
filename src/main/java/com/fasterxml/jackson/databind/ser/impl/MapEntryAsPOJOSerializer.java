package com.fasterxml.jackson.databind.ser.impl;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Serializer used to serialize Map.Entry as POJOs: that is, as if
 * introspected as POJOs so that there's intermediate "key" and "value"
 * properties.
 *<p>
 * TODO: does not fully handle contextualization, type resolution and so on.
 *
 * @since 2.19
 */
public class MapEntryAsPOJOSerializer extends StdSerializer<Map.Entry<?,?>>
{
    private static final long serialVersionUID = 1L;

    protected MapEntryAsPOJOSerializer(JavaType type) {
        super(type);
    }

    public static MapEntryAsPOJOSerializer create(SerializerProvider ctxt,
            JavaType type)
    {
        return new MapEntryAsPOJOSerializer(type);
    }

    @Override
    public void serialize(Entry<?, ?> value, JsonGenerator gen, SerializerProvider ctxt)
            throws IOException
    {
        gen.writeStartObject(value);
        ctxt.defaultSerializeField("key",value.getKey(), gen);
        ctxt.defaultSerializeField("value", value.getValue(), gen);
        gen.writeEndObject();
    }
}
