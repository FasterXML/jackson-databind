package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.util.Date;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonschema.visitors.JsonFormatVisitor;

/**
 * Specialized serializer that can be used as the generic key
 * serializer, when serializing {@link java.util.Map}s to JSON
 * Objects.
 */
public class StdKeySerializer
    extends StdSerializer<Object>
{
    final static StdKeySerializer instace = new StdKeySerializer();

    public StdKeySerializer() { super(Object.class); }
    
    @Override
    public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        if (value instanceof Date) {
            provider.defaultSerializeDateKey((Date) value, jgen);
        } else {
            jgen.writeFieldName(value.toString());
        }
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitor visitor, JavaType typeHint)
    {
    	visitor.stringFormat(typeHint);
    }
}
