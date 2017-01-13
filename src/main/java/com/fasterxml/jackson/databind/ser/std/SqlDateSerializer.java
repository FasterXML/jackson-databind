package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.DateFormat;

import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;

/**
 * Compared to regular {@link java.util.Date} serialization, we do use String
 * representation here. Why? Basically to truncate of time part, since
 * that should not be used by plain SQL date.
 */
@JacksonStdImpl
@SuppressWarnings("serial")
public class SqlDateSerializer
    extends DateTimeSerializerBase<java.sql.Date>
{
    public SqlDateSerializer() {
        // 11-Oct-2016, tatu: As per [databind#219] fixed for 2.9; was passing `false` prior
        this(null, null);
    }

    protected SqlDateSerializer(Boolean useTimestamp, DateFormat customFormat) {
        super(java.sql.Date.class, useTimestamp, customFormat);
    }

    @Override
    public SqlDateSerializer withFormat(Boolean timestamp, DateFormat customFormat) {
    	return new SqlDateSerializer(timestamp, customFormat);
    }

    @Override
    protected long _timestamp(java.sql.Date value) {
        return (value == null) ? 0L : value.getTime();
    }
    
    @Override
    public void serialize(java.sql.Date value, JsonGenerator gen, SerializerProvider provider)
        throws IOException
    {
        if (_asTimestamp(provider)) {
            gen.writeNumber(_timestamp(value));
        } else if (_customFormat != null) {
            // 11-Oct-2016, tatu: As per [databind#219], same as with `java.util.Date`
            synchronized (_customFormat) {
                gen.writeString(_customFormat.format(value));
            }
        } else {
            // 11-Oct-2016, tatu: For backwards-compatibility purposes, we shall just use
            //    the awful standard JDK serialization via `sqlDate.toString()`... this
            //    is problematic in multiple ways (including using arbitrary timezone...)
            gen.writeString(value.toString());
        }
    }

    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint)
    {
        //todo: (ryan) add a format for the date in the schema?
        return createSchemaNode("string", true);
    }
    
    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint) throws JsonMappingException
    {
        _acceptJsonFormatVisitor(visitor, typeHint, _useTimestamp);
    }
}
