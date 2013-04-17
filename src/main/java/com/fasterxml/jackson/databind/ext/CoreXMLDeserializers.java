package com.fasterxml.jackson.databind.ext;

import java.io.IOException;
import java.util.*;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;

/**
 * Container deserializers that handle "core" XML types: ones included in standard
 * JDK 1.5. Types are directly needed by JAXB, but may be unavailable on some
 * limited platforms; hence separate out from basic deserializer factory.
 */
public class CoreXMLDeserializers
    extends Deserializers.Base
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
            return QNameDeserializer.instance;
        }
        if (raw == XMLGregorianCalendar.class) {
            return GregorianCalendarDeserializer.instance;
        }
        if (raw == Duration.class) {
            return DurationDeserializer.instance;
        }
        return null;
    }

    /*
    /**********************************************************
    /* Concrete deserializers
    /**********************************************************
     */

    public static class DurationDeserializer
        extends FromStringDeserializer<Duration>
    {
        private static final long serialVersionUID = 1L;
        public final static DurationDeserializer instance = new DurationDeserializer();
        public DurationDeserializer() { super(Duration.class); }
    
        @Override
        protected Duration _deserialize(String value, DeserializationContext ctxt)
            throws IllegalArgumentException
        {
            return _dataTypeFactory.newDuration(value);
        }
    }

    public static class GregorianCalendarDeserializer
        extends StdScalarDeserializer<XMLGregorianCalendar>
    {
        private static final long serialVersionUID = 1L;
        public final static GregorianCalendarDeserializer instance = new GregorianCalendarDeserializer();
        public GregorianCalendarDeserializer() { super(XMLGregorianCalendar.class); }
        
        @Override
        public XMLGregorianCalendar deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
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
    }

    public static class QNameDeserializer
        extends FromStringDeserializer<QName>
    {
        private static final long serialVersionUID = 1L;
        public final static QNameDeserializer instance = new QNameDeserializer();
        
        
        public QNameDeserializer() { super(QName.class); }
        
        @Override
        protected QName _deserialize(String value, DeserializationContext ctxt)
            throws IllegalArgumentException
        {
            return QName.valueOf(value);
        }
    }
}
