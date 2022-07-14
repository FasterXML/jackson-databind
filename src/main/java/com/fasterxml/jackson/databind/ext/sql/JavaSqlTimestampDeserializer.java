package com.fasterxml.jackson.databind.ext.sql;

import java.text.DateFormat;
import java.util.Date;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.jdk.DateBasedDeserializer;

/**
 * Simple deserializer for handling {@link java.sql.Timestamp} values.
 *<p>
 * One way to customize Timestamp formats accepted is to override method
 * {@link DeserializationContext#parseDate} that this basic
 * deserializer calls.
 */
public class JavaSqlTimestampDeserializer extends DateBasedDeserializer<java.sql.Timestamp>
{
    public JavaSqlTimestampDeserializer() { super(java.sql.Timestamp.class); }
    public JavaSqlTimestampDeserializer(JavaSqlTimestampDeserializer src, DateFormat df, String formatString) {
        super(src, df, formatString);
    }

    @Override
    protected JavaSqlTimestampDeserializer withDateFormat(DateFormat df, String formatString) {
        return new JavaSqlTimestampDeserializer(this, df, formatString);
    }

    @Override // since 2.12
    public Object getEmptyValue(DeserializationContext ctxt) {
        return new java.sql.Timestamp(0L);
    }

    @Override
    public java.sql.Timestamp deserialize(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        Date d = _parseDate(p, ctxt);
        return (d == null) ? null : new java.sql.Timestamp(d.getTime());
    }
}
