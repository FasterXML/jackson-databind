package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.DateFormat;

import com.fasterxml.jackson.core.JsonGenerationException;
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
        /* 12-Apr-2014, tatu: for now, pass explicit 'false' to mean 'not using timestamp',
         *     for backwards compatibility; this differs from other Date/Calendar types.
         */
        this(Boolean.FALSE);
    }

    protected SqlDateSerializer(Boolean useTimestamp) {
        super(java.sql.Date.class, useTimestamp, null);
    }

    @Override
    public SqlDateSerializer withFormat(Boolean timestamp, DateFormat customFormat) {
    	return new SqlDateSerializer(timestamp);
    }

    @Override
    protected long _timestamp(java.sql.Date value) {
        return (value == null) ? 0L : value.getTime();
    }
    
    @Override
    public void serialize(java.sql.Date value, JsonGenerator gen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        if (_asTimestamp(provider)) {
            gen.writeNumber(_timestamp(value));
        } else {
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
