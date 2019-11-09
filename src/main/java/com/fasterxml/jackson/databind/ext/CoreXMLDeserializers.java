package com.fasterxml.jackson.databind.ext;

import java.io.IOException;
import java.util.*;

import javax.xml.datatype.*;
import javax.xml.namespace.QName;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer;

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

    @Override // since 2.11
    public boolean hasDeserializerFor(DeserializationConfig config, Class<?> valueType) {
        return (valueType == QName.class)
                || (valueType == XMLGregorianCalendar.class)
                || (valueType == Duration.class)
                ;
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
        public Object deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException
        {
            // For most types, use super impl; but GregorianCalendar also allows
            // integer value (timestamp), which needs separate handling
            if (_kind == TYPE_G_CALENDAR) {
                if (p.hasToken(JsonToken.VALUE_NUMBER_INT)) {
                    return _gregorianFromDate(ctxt, _parseDate(p, ctxt));
                }
            }
            return super.deserialize(p, ctxt);
        }

        @Override
        protected Object _deserialize(String value, DeserializationContext ctxt)
            throws IOException
        {
            switch (_kind) {
            case TYPE_DURATION:
                return _dataTypeFactory.newDuration(value);
            case TYPE_QNAME:
                return QName.valueOf(value);
            case TYPE_G_CALENDAR:
                Date d;
                try {
                    d = _parseDate(value, ctxt);
                }
                catch (JsonMappingException e) {
                    // try to parse from native XML Schema 1.0 lexical representation String,
                    // which includes time-only formats not handled by parseXMLGregorianCalendarFromJacksonFormat(...)
                    return _dataTypeFactory.newXMLGregorianCalendar(value);
                }
                return _gregorianFromDate(ctxt, d);
            }
            throw new IllegalStateException();
        }

        protected XMLGregorianCalendar _gregorianFromDate(DeserializationContext ctxt,
                Date d)
        {
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
    }
}
