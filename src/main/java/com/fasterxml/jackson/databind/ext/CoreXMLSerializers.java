package com.fasterxml.jackson.databind.ext;

import java.io.IOException;
import java.lang.reflect.Type;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.fasterxml.jackson.databind.ser.std.CalendarSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/**
 * Provider for serializers of XML types that are part of full JDK 1.5, but
 * that some alleged 1.5 platforms are missing (Android, GAE).
 * And for this reason these are added using more dynamic mechanism.
 *<p>
 * Note: since many of classes defined are abstract, caller must take
 * care not to just use straight equivalency check but rather consider
 * subclassing as well.
 */
public class CoreXMLSerializers extends Serializers.Base
{
    @Override
    public JsonSerializer<?> findSerializer(SerializationConfig config,
            JavaType type, BeanDescription beanDesc)
    {
        Class<?> raw = type.getRawClass();
        if (Duration.class.isAssignableFrom(raw) || QName.class.isAssignableFrom(raw)) {
            return ToStringSerializer.instance;
        }
        if (XMLGregorianCalendar.class.isAssignableFrom(raw)) {
            return XMLGregorianCalendarSerializer.instance;
        }
        return null;
    }

    public static class XMLGregorianCalendarSerializer extends StdSerializer<XMLGregorianCalendar>
    {
        public final static XMLGregorianCalendarSerializer instance = new XMLGregorianCalendarSerializer();
        
        public XMLGregorianCalendarSerializer() {
            super(XMLGregorianCalendar.class);
        }

        @Override
        public void serialize(XMLGregorianCalendar value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException, JsonGenerationException {
            CalendarSerializer.instance.serialize(value.toGregorianCalendar(), jgen, provider);
        }
        
        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint) throws JsonMappingException {
            return CalendarSerializer.instance.getSchema(provider, typeHint);
        }

        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
                throws JsonMappingException
        {
            CalendarSerializer.instance.acceptJsonFormatVisitor(visitor, null);
        }
    }
}
