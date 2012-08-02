package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsonschema.types.JsonValueFormat;
import com.fasterxml.jackson.databind.jsonschema.visitors.JsonFormatVisitor;

/**
 * Compared to regular {@link java.util.Date} serialization, we do use String
 * representation here. Why? Basically to truncate of time part, since
 * that should not be used by plain SQL date.
 */
@JacksonStdImpl
public class SqlDateSerializer
    extends StdScalarSerializer<java.sql.Date>
{
    public SqlDateSerializer() { super(java.sql.Date.class); }

    @Override
    public void serialize(java.sql.Date value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        jgen.writeString(value.toString());
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitor visitor, JavaType typeHint)
    {
    	visitor.stringFormat().format(JsonValueFormat.DATE_TIME);
    }
}