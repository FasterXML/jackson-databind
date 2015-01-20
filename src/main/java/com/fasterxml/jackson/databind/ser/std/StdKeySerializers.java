package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;

@SuppressWarnings("serial")
public class StdKeySerializers
{
    protected final static JsonSerializer<Object> DEFAULT_KEY_SERIALIZER = new StdKeySerializer();

    @SuppressWarnings("unchecked")
    protected final static JsonSerializer<Object> DEFAULT_STRING_SERIALIZER
        = (JsonSerializer<Object>)(JsonSerializer<?>) new StringKeySerializer();

    private StdKeySerializers() { }

    /**
     * @param config Serialization configuration in use, may be needed in choosing
     *    serializer to use
     * @param rawKeyType Type of key values to serialize
     * @param useDefault If no match is found, should we return fallback deserializer
     *    (true), or null (false)?
     */
    @SuppressWarnings("unchecked")
    public static JsonSerializer<Object> getStdKeySerializer(SerializationConfig config,
            Class<?> rawKeyType, boolean useDefault)
    {
        if (rawKeyType != null) {
            if (rawKeyType == String.class) {
                return DEFAULT_STRING_SERIALIZER;
            }
            if (rawKeyType == Object.class || rawKeyType.isPrimitive()
                    || Number.class.isAssignableFrom(rawKeyType)) {
                return DEFAULT_KEY_SERIALIZER;
            }
            if (rawKeyType == Class.class) {
                return (JsonSerializer<Object>) ClassKeySerializer.instance;
            }
            if (Date.class.isAssignableFrom(rawKeyType)) {
                return (JsonSerializer<Object>) DateKeySerializer.instance;
            }
            if (Calendar.class.isAssignableFrom(rawKeyType)) {
                return (JsonSerializer<Object>) CalendarKeySerializer.instance;
            }
        }
        return useDefault ? DEFAULT_KEY_SERIALIZER : null;
    }

    /**
     * @deprecated Since 2.5
     */
    @Deprecated
    public static JsonSerializer<Object> getStdKeySerializer(JavaType keyType) {
        return getStdKeySerializer(null, keyType.getRawClass(), true);
    }

    public static JsonSerializer<Object> getDefault() {
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

    public static class ClassKeySerializer extends StdSerializer<Class<?>> {
        protected final static JsonSerializer<?> instance = new ClassKeySerializer();

        public ClassKeySerializer() { super(Class.class, false); }
        
        @Override
        public void serialize(Class<?> value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
            jgen.writeFieldName(value.getName());
        }
    }
}
