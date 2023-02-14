package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.ser.impl.PropertySerializerMap;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.EnumValues;

@SuppressWarnings("serial")
public abstract class StdKeySerializers
{
    @SuppressWarnings("deprecation")
    protected final static JsonSerializer<Object> DEFAULT_KEY_SERIALIZER = new StdKeySerializer();

    protected final static JsonSerializer<Object> DEFAULT_STRING_SERIALIZER = new StringKeySerializer();

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
        //    cannot be used, since caller has not yet checked for that annotation
        //    This is why Enum types are not handled here quite yet

        // [databind#943: Use a dynamic key serializer if we are not given actual
        // type declaration
        if ((rawKeyType == null) || (rawKeyType == Object.class)) {
            return new Dynamic();
        }
        if (rawKeyType == String.class) {
            return DEFAULT_STRING_SERIALIZER;
        }
        if (rawKeyType.isPrimitive()) {
            rawKeyType = ClassUtil.wrapperType(rawKeyType);
        }
        if (rawKeyType == Integer.class) {
            return new Default(Default.TYPE_INTEGER, rawKeyType);
        }
        if (rawKeyType == Long.class) {
            return new Default(Default.TYPE_LONG, rawKeyType);
        }
        if (rawKeyType.isPrimitive() || Number.class.isAssignableFrom(rawKeyType)) {
            // 28-Jun-2016, tatu: Used to just return DEFAULT_KEY_SERIALIZER, but makes
            //   more sense to use simpler one directly
            return new Default(Default.TYPE_TO_STRING, rawKeyType);
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
        if (rawKeyType == byte[].class) {
            return new Default(Default.TYPE_BYTE_ARRAY, rawKeyType);
        }
        if (useDefault) {
            // 19-Oct-2016, tatu: Used to just return DEFAULT_KEY_SERIALIZER but why not:
            return new Default(Default.TYPE_TO_STRING, rawKeyType);
        }
        return null;
    }

    /**
     * Method called if no specified key serializer was located; will return a
     * "default" key serializer.
     *
     * @since 2.7
     */
    @SuppressWarnings("unchecked")
    public static JsonSerializer<Object> getFallbackKeySerializer(SerializationConfig config,
            Class<?> rawKeyType)
    {
        if (rawKeyType != null) {
            // 29-Sep-2015, tatu: Odd case here, of `Enum`, which we may get for `EnumMap`; not sure
            //   if that is a bug or feature. Regardless, it seems to require dynamic handling
            //   (compared to getting actual fully typed Enum).
            //  Note that this might even work from the earlier point, but let's play it safe for now
            // 11-Aug-2016, tatu: Turns out we get this if `EnumMap` is the root value because
            //    then there is no static type
            if (rawKeyType == Enum.class) {
                return new Dynamic();
            }
            // 29-Sep-2019, tatu: [databind#2457] can not use 'rawKeyType.isEnum()`, won't work
            //    for subtypes.
            if (ClassUtil.isEnumType(rawKeyType)) {
                return EnumKeySerializer.construct(rawKeyType,
                        EnumValues.constructFromName(config, (Class<Enum<?>>) rawKeyType));
            }
        }
        // 19-Oct-2016, tatu: Used to just return DEFAULT_KEY_SERIALIZER but why not:
        return new Default(Default.TYPE_TO_STRING, rawKeyType);
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

    /**
     * This is a "chameleon" style multi-type key serializer for simple
     * standard JDK types.
     *<p>
     * TODO: Should (but does not yet) support re-configuring format used for
     * {@link java.util.Date} and {@link java.util.Calendar} key serializers,
     * as well as alternative configuration of Enum key serializers.
     */
    public static class Default extends StdSerializer<Object> {
        final static int TYPE_DATE = 1;
        final static int TYPE_CALENDAR = 2;
        final static int TYPE_CLASS = 3;
        final static int TYPE_ENUM = 4;
        final static int TYPE_INTEGER = 5; // since 2.9
        final static int TYPE_LONG = 6; // since 2.9
        final static int TYPE_BYTE_ARRAY = 7; // since 2.9
        final static int TYPE_TO_STRING = 8;

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
                    String key;

                    if (provider.isEnabled(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)) {
                        key = value.toString();
                    } else {
                        Enum<?> e = (Enum<?>) value;
                        // 14-Sep-2019, tatu: [databind#2129] Use this specific feature
                        if (provider.isEnabled(SerializationFeature.WRITE_ENUM_KEYS_USING_INDEX)) {
                            key = String.valueOf(e.ordinal());
                        } else {
                            key = e.name();
                        }
                    }
                    g.writeFieldName(key);
                }
                break;
            case TYPE_INTEGER:
            case TYPE_LONG:
                g.writeFieldId(((Number) value).longValue());
                break;
            case TYPE_BYTE_ARRAY:
                {
                    String encoded = provider.getConfig().getBase64Variant().encode((byte[]) value);
                    g.writeFieldName(encoded);
                }
                break;
            case TYPE_TO_STRING:
            default:
                g.writeFieldName(value.toString());
            }
        }
    }

    /**
     * Key serializer used when key type is not known statically, and actual key
     * serializer needs to be dynamically located.
     */
    public static class Dynamic extends StdSerializer<Object>
    {
        // Important: MUST be transient, to allow serialization of key serializer itself
        protected transient PropertySerializerMap _dynamicSerializers;

        public Dynamic() {
            super(String.class, false);
            _dynamicSerializers = PropertySerializerMap.emptyForProperties();
        }

        Object readResolve() {
            // Since it's transient, and since JDK serialization by-passes ctor, need this:
            _dynamicSerializers = PropertySerializerMap.emptyForProperties();
            return this;
        }

        @Override
        public void serialize(Object value, JsonGenerator g, SerializerProvider provider)
                throws IOException
        {
            Class<?> cls = value.getClass();
            PropertySerializerMap m = _dynamicSerializers;
            JsonSerializer<Object> ser = m.serializerFor(cls);
            if (ser == null) {
                ser = _findAndAddDynamic(m, cls, provider);
            }
            ser.serialize(value, g, provider);
        }

        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint) throws JsonMappingException {
            visitStringFormat(visitor, typeHint);
        }

        protected JsonSerializer<Object> _findAndAddDynamic(PropertySerializerMap map,
                Class<?> type, SerializerProvider provider) throws JsonMappingException
        {
            // 27-Jun-2017, tatu: [databind#1679] Need to avoid StackOverflowError...
            if (type == Object.class) {
                // basically just need to call `toString()`, easiest way:
                JsonSerializer<Object> ser = new Default(Default.TYPE_TO_STRING, type);
                _dynamicSerializers = map.newWith(type, ser);
                return ser;
            }
            PropertySerializerMap.SerializerAndMapResult result =
                    // null -> for now we won't keep ref or pass BeanProperty; could change
                    map.findAndAddKeySerializer(type, provider, null);
            // did we get a new map of serializers? If so, start using it
            if (map != result.map) {
                _dynamicSerializers = result.map;
            }
            return result.serializer;
        }
    }

    /**
     * Simple and fast key serializer when keys are Strings.
     */
    public static class StringKeySerializer extends StdSerializer<Object>
    {
        public StringKeySerializer() { super(String.class, false); }

        @Override
        public void serialize(Object value, JsonGenerator g, SerializerProvider provider) throws IOException {
            g.writeFieldName((String) value);
        }
    }

    /**
     * Specialized instance to use for Enum keys, as per [databind#1322]
     *
     * @since 2.8
     */
    public static class EnumKeySerializer extends StdSerializer<Object>
    {
        protected final EnumValues _values;

        protected EnumKeySerializer(Class<?> enumType, EnumValues values) {
            super(enumType, false);
            _values = values;
        }

        public static EnumKeySerializer construct(Class<?> enumType,
                EnumValues enumValues)
        {
            return new EnumKeySerializer(enumType, enumValues);
        }

        @Override
        public void serialize(Object value, JsonGenerator g, SerializerProvider serializers)
                throws IOException
        {
            if (serializers.isEnabled(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)) {
                g.writeFieldName(value.toString());
                return;
            }
            Enum<?> en = (Enum<?>) value;
            // 14-Sep-2019, tatu: [databind#2129] Use this specific feature
            if (serializers.isEnabled(SerializationFeature.WRITE_ENUM_KEYS_USING_INDEX)) {
                g.writeFieldName(String.valueOf(en.ordinal()));
                return;
            }
            g.writeFieldName(_values.serializedValueFor(en));
        }
    }
}
