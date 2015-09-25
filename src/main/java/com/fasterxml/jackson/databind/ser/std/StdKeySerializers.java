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
        // 24-Sep-2015, tatu: Important -- should ONLY consider types for which `@JsonValue`
        //    can not be used, since caller has not yet checked for that annotation
        //    This is why Enum types are not handled here quite yet

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
            // other JDK types we know convert properly with 'toString()'?
            if (rawKeyType == java.util.UUID.class) {
                return new Default(Default.TYPE_TO_STRING, rawKeyType);
            }

        }
        return useDefault ? DEFAULT_KEY_SERIALIZER : null;
    }

    /**
     * Method called if no specified key serializer was located; will return a
     * "default" key serializer.
     *
     * @since 2.7
     */
    public static JsonSerializer<Object> getFallbackKeySerializer(SerializationConfig config,
            Class<?> rawKeyType) {
        if (rawKeyType != null) {
            if (rawKeyType.isEnum()) {
                return new Default(Default.TYPE_ENUM, rawKeyType);
            }
        }
        return DEFAULT_KEY_SERIALIZER;
    }

    /**
     * @deprecated since 2.7
     */
    @Deprecated
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
        final static int TYPE_ENUM = 4;
        final static int TYPE_TO_STRING = 5;

        protected final int _typeId;
        
        public Default(int typeId, Class<?> type) {
            super(type, false);
            _typeId = typeId;
        }

        @Override
        public void serialize(Object value, JsonGenerator g, SerializerProvider provider) throws IOException {
            switch (_typeId) {
            case TYPE_DATE:
                provider.defaultSerializeDateKey((Date)value, g);
                break;
            case TYPE_CALENDAR:
                provider.defaultSerializeDateKey(((Calendar) value).getTimeInMillis(), g);
                break;
            case TYPE_CLASS:
                g.writeFieldName(((Class<?>)value).getName());
                break;
            case TYPE_ENUM:
                {
                    String str = provider.isEnabled(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
                            ? value.toString() : ((Enum<?>) value).name();
                    g.writeFieldName(str);
                }
                break;
            case TYPE_TO_STRING:
            default:
                g.writeFieldName(value.toString());
            }
        }
    }

    public static class StringKeySerializer extends StdSerializer<Object>
    {
        public StringKeySerializer() { super(String.class, false); }

        @Override
        public void serialize(Object value, JsonGenerator g, SerializerProvider provider) throws IOException {
            g.writeFieldName((String) value);
        }
    }
}
