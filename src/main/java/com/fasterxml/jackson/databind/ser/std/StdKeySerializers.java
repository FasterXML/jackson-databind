package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class StdKeySerializers
{
    protected final static JsonSerializer<Object> DEFAULT_KEY_SERIALIZER = new StdKeySerializer();

    @SuppressWarnings("unchecked")
    protected final static JsonSerializer<Object> DEFAULT_STRING_SERIALIZER
        = (JsonSerializer<Object>)(JsonSerializer<?>) new StringKeySerializer();

    private StdKeySerializers() { }

    @SuppressWarnings("unchecked")
    public static JsonSerializer<Object> getStdKeySerializer(JavaType keyType)
    {
        if (keyType == null) {
            return DEFAULT_KEY_SERIALIZER;
        }
        Class<?> cls = keyType.getRawClass();
        if (cls == String.class) {
            return DEFAULT_STRING_SERIALIZER;
        }
        if (cls == Object.class || cls.isPrimitive() || Number.class.isAssignableFrom(cls)) {
            return DEFAULT_KEY_SERIALIZER;
        }
        if (Date.class.isAssignableFrom(cls)) {
            return (JsonSerializer<Object>) DateKeySerializer.instance;
        }
        if (Calendar.class.isAssignableFrom(cls)) {
            return (JsonSerializer<Object>) CalendarKeySerializer.instance;
        }
        /* 14-Mar-2014, tatu: Should support @JsonValue, as per #47; but that
         *   requires extensive introspection, and passing in more information
         *   to this method.
         */
        // If no match, just use default one:
        return DEFAULT_KEY_SERIALIZER;
    }

    /*
    /**********************************************************
    /* Standard implementations
    /**********************************************************
     */

    public static class StringKeySerializer extends StdSerializer<String>
    {
        public StringKeySerializer() { super(String.class); }
        
        @Override
        public void serialize(String value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
            jgen.writeFieldName(value);
        }
    }

    public static class DateKeySerializer extends StdSerializer<Date> {
        protected final static JsonSerializer<?> instance = new DateKeySerializer();

        public DateKeySerializer() { super(Date.class); }
        
        @Override
        public void serialize(Date value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
            provider.defaultSerializeDateKey(value, jgen);
        }
    }

    public static class CalendarKeySerializer extends StdSerializer<Calendar> {
        protected final static JsonSerializer<?> instance = new CalendarKeySerializer();

        public CalendarKeySerializer() { super(Calendar.class); }
        
        @Override
        public void serialize(Calendar value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
            provider.defaultSerializeDateKey(value.getTimeInMillis(), jgen);
        }
    }
}
