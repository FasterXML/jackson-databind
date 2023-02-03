package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.text.DateFormat;

import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;

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
    public void serialize(java.sql.Date value, JsonGenerator g, SerializerProvider provider)
        throws IOException
    {
        if (_asTimestamp(provider)) {
            g.writeNumber(_timestamp(value));
            return;
        }
        // Alas, can't just call `_serializeAsString()`....
        if (_customFormat == null) {
            // 11-Oct-2016, tatu: For backwards-compatibility purposes, we shall just use
            //    the awful standard JDK serialization via `sqlDate.toString()`... this
            //    is problematic in multiple ways (including using arbitrary timezone...)
            g.writeString(value.toString());
            return;
        }
        _serializeAsString(value, g, provider);
    }
}
