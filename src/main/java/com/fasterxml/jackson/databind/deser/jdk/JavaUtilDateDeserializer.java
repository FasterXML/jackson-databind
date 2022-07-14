package com.fasterxml.jackson.databind.deser.jdk;

import java.text.DateFormat;
import java.util.Date;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;

/**
 * Simple deserializer for handling {@link java.util.Date} values.
 *<p>
 * One way to customize Date formats accepted is to override method
 * {@link DeserializationContext#parseDate} that this basic
 * deserializer calls.
 */
@JacksonStdImpl
public class JavaUtilDateDeserializer extends DateBasedDeserializer<Date>
{
    public final static JavaUtilDateDeserializer instance = new JavaUtilDateDeserializer();

    public JavaUtilDateDeserializer() { super(Date.class); }
    public JavaUtilDateDeserializer(JavaUtilDateDeserializer base, DateFormat df, String formatString) {
        super(base, df, formatString);
    }

    @Override
    protected JavaUtilDateDeserializer withDateFormat(DateFormat df, String formatString) {
        return new JavaUtilDateDeserializer(this, df, formatString);
    }

    @Override // since 2.12
    public Object getEmptyValue(DeserializationContext ctxt) {
        return new Date(0L);
    }
    
    @Override
    public java.util.Date deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        return _parseDate(p, ctxt);
    }
}
