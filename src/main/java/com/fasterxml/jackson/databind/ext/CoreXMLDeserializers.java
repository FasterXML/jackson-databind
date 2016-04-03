package com.fasterxml.jackson.databind.ext;

import java.io.IOException;
import java.util.*;

import javax.xml.datatype.*;
import javax.xml.namespace.QName;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

/**
 * Container deserializers that handle "core" XML types: ones included in standard
 * JDK 1.5. Types are directly needed by JAXB, but may be unavailable on some
 * limited platforms; hence separate out from basic deserializer factory.
 */
public class CoreXMLDeserializers extends Deserializers.Base
{
    /**
     * Data type factories are thread-safe after instantiation (and
     * configuration, if any); and since instantion (esp. implementation
     * introspection) can be expensive we better reuse the instance.
     */
    final static DatatypeFactory _dataTypeFactory;
    static {
        try {
            _dataTypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JsonDeserializer<?> findBeanDeserializer(JavaType type,
        DeserializationConfig config, BeanDescription beanDesc)
    {
        Class<?> raw = type.getRawClass();
        if (raw == QName.class) {
            return new Std(raw, TYPE_QNAME);
        }
        if (raw == XMLGregorianCalendar.class) {
            return new Std(raw, TYPE_G_CALENDAR);
        }
        if (raw == Duration.class) {
            return new Std(raw, TYPE_DURATION);
        }
        return null;
    }

    /*
    /**********************************************************
    /* Concrete deserializers
    /**********************************************************
     */

    protected final static int TYPE_DURATION = 1;
    protected final static int TYPE_G_CALENDAR = 2;
    protected final static int TYPE_QNAME = 3;

    /**
     * Combo-deserializer that supports deserialization of somewhat optional
     * javax.xml types {@link QName}, {@link Duration} and {@link XMLGregorianCalendar}.
     * Combined into a single class to eliminate bunch of one-off implementation
     * classes, to reduce resulting jar size (mostly).
     *
     * @since 2.4
     */
    public static class Std extends FromStringDeserializer<Object>
    {
        private static final long serialVersionUID = 1L;

        protected final int _kind;

        public Std(Class<?> raw, int kind) {
            super(raw);
            _kind = kind;
        }

        @Override
        public Object deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            // For most types, use super impl; but not for GregorianCalendar
            if (_kind == TYPE_G_CALENDAR) {
                return parseXMLGregorianCalendarPossiblyNested(jp, ctxt);
            }
            return super.deserialize(jp, ctxt);
        }

        protected XMLGregorianCalendar parseXMLGregorianCalendarPossiblyNested(JsonParser jp, DeserializationContext ctxt)
            throws IOException
        {
            // Must unnest nested arrays here using iteration instead of in _parseDate(...) using recursion
            // because if _parseDate(...) throws an exception, then this must try to parse using _dataTypeFactory.newXMLGregorianCalendar(String)
            // which would lose the recursive array nesting context from _parseDate(...)

            int nestedArrayCount = 0;
            for (JsonToken t = jp.getCurrentToken(); t == JsonToken.START_ARRAY && ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS); t = jp.nextToken()) {
                nestedArrayCount++;
            }

            final XMLGregorianCalendar xgCal = parseXMLGregorianCalendarNotNested(jp, ctxt);

            for (int i = 0; i < nestedArrayCount; i++) {
                if (jp.nextToken() != JsonToken.END_ARRAY) {
                    throw ctxt.wrongTokenException(
                        jp,
                        JsonToken.END_ARRAY,
                        "Attempted to unwrap single value array for single 'javax.xml.datatype.XMLGregorianCalendar' value but there was more than a single value in the array"
                    );
                }
            }

            return xgCal;
        }

        protected XMLGregorianCalendar parseXMLGregorianCalendarNotNested(JsonParser jp, DeserializationContext ctxt)
            throws IOException
        {
            try {
                return parseXMLGregorianCalendarFromJacksonFormat(jp, ctxt);
            }
            catch (InvalidFormatException e) {
                // try to parse from native XML Schema 1.0 lexical representation String,
                // which includes time-only formats not handled by parseXMLGregorianCalendarFromJacksonFormat(...)

                JsonToken t = jp.getCurrentToken();

                if (t == JsonToken.VALUE_STRING) {
                    return _dataTypeFactory.newXMLGregorianCalendar(jp.getText().trim());
                }

                throw ctxt.mappingException(_valueClass, t);
            }
        }

        protected XMLGregorianCalendar parseXMLGregorianCalendarFromJacksonFormat(JsonParser jp, DeserializationContext ctxt)
            throws IOException
        {
            Date d = _parseDate(jp, ctxt);
            if (d == null) {
                return null;
            }
            GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTime(d);
            TimeZone tz = ctxt.getTimeZone();
            if (tz != null) {
                calendar.setTimeZone(tz);
            }
            return _dataTypeFactory.newXMLGregorianCalendar(calendar);
        }

        @Override
        protected Object _deserialize(String value, DeserializationContext ctxt) throws IllegalArgumentException
        {
            switch (_kind) {
            case TYPE_DURATION:
                return _dataTypeFactory.newDuration(value);
            case TYPE_QNAME:
                return QName.valueOf(value);
            }
            throw new IllegalStateException();
        }
    }
}
