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

    protected final static JsonSerializer<Object> DEFAULT_STRING_SERIALIZER = new StringKeySerializer();

    private StdKeySerializers() { }

    /**
     * @param config Serialization configuration in use, may be needed in choosing
     *    serializer to use
     * @param rawKeyType Type of key values to serialize
     * @param useDefault If no match is found, should we return fallback deserializer
     *    (true), or null (false)?
     */
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
                return new Default(Default.TYPE_CLASS, rawKeyType);
            }
            if (Date.class.isAssignableFrom(rawKeyType)) {
                return new Default(Default.TYPE_DATE, rawKeyType);
            }
            if (Calendar.class.isAssignableFrom(rawKeyType)) {
                return new Default(Default.TYPE_CALENDAR, rawKeyType);
            }
            // other types we know convert properly with 'toString()'?
            if (rawKeyType == java.util.UUID.class) {
                return new Default(Default.TYPE_TO_STRING, rawKeyType);
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
    /* Standard implementations used
    /**********************************************************
     */

    public static class Default extends StdSerializer<Object> {
        final static int TYPE_DATE = 1;
        final static int TYPE_CALENDAR = 2;
        final static int TYPE_CLASS = 3;
        final static int TYPE_TO_STRING = 4;

        protected final int _typeId;
        
        public Default(int typeId, Class<?> type) {
            super(type, false);
            _typeId = typeId;
        }

        @Override
        public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
            switch (_typeId) {
            case TYPE_DATE:
                provider.defaultSerializeDateKey((Date)value, jgen);
                break;
            case TYPE_CALENDAR:
                provider.defaultSerializeDateKey(((Calendar) value).getTimeInMillis(), jgen);
                break;
            case TYPE_CLASS:
                jgen.writeFieldName(((Class<?>)value).getName());
                break;
            case TYPE_TO_STRING:
            default:
                jgen.writeFieldName(value.toString());
            }
        }
    }

    public static class StringKeySerializer extends StdSerializer<Object>
    {
        public StringKeySerializer() { super(String.class, false); }

        @Override
        public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
            jgen.writeFieldName((String) value);
        }
    }
    
    /*
    /**********************************************************
    /* Deprecated implementations: to be removed in future
    /**********************************************************
     */

    @Deprecated // since 2.6; remove from 2.7 or later
    public static class DateKeySerializer extends StdSerializer<Date> {
        protected final static JsonSerializer<?> instance = new DateKeySerializer();

        public DateKeySerializer() { super(Date.class); }
        
        @Override
        public void serialize(Date value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
            provider.defaultSerializeDateKey(value, jgen);
        }
    }

    @Deprecated // since 2.6; remove from 2.7 or later
    public static class CalendarKeySerializer extends StdSerializer<Calendar> {
        protected final static JsonSerializer<?> instance = new CalendarKeySerializer();

        public CalendarKeySerializer() { super(Calendar.class); }
        
        @Override
        public void serialize(Calendar value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
            provider.defaultSerializeDateKey(value.getTimeInMillis(), jgen);
        }
    }
}
