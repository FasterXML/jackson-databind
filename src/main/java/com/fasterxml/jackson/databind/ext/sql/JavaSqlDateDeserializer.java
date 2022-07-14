package com.fasterxml.jackson.databind.ext.sql;

import java.text.DateFormat;
import java.util.Date;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.jdk.DateBasedDeserializer;

/**
 * Compared to plain old {@link java.util.Date}, SQL version is easier
 * to deal with: mostly because it is more limited.
 */
public class JavaSqlDateDeserializer
    extends DateBasedDeserializer<java.sql.Date>
{
    public JavaSqlDateDeserializer() { super(java.sql.Date.class); }
    public JavaSqlDateDeserializer(JavaSqlDateDeserializer src, DateFormat df, String formatString) {
        super(src, df, formatString);
    }

    @Override
    protected JavaSqlDateDeserializer withDateFormat(DateFormat df, String formatString) {
        return new JavaSqlDateDeserializer(this, df, formatString);
    }

    @Override // since 2.12
    public Object getEmptyValue(DeserializationContext ctxt) {
        return new java.sql.Date(0L);
    }

    @Override
    public java.sql.Date deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        Date d = _parseDate(p, ctxt);
        return (d == null) ? null : new java.sql.Date(d.getTime());
    }
}
