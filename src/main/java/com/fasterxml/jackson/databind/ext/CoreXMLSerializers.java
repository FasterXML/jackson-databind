package com.fasterxml.jackson.databind.ext;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.ser.std.CalendarSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.databind.util.Provider;

/**
 * Provider for serializers of XML types that are part of full JDK 1.5, but
 * that some alleged 1.5 platforms are missing (Android, GAE).
 * And for this reason these are added using more dynamic mechanism.
 *<p>
 * Note: since many of classes defined are abstract, caller must take
 * care not to just use straight equivalency check but rather consider
 * subclassing as well.
 */
public class CoreXMLSerializers
    implements Provider<Map.Entry<Class<?>,JsonSerializer<?>>>
{
    final static HashMap<Class<?>,JsonSerializer<?>> _serializers = new HashMap<Class<?>,JsonSerializer<?>>();
    /**
     * We will construct instances statically, during class loading, to try to
     * make things fail-fast, i.e. to catch problems as soon as possible.
     */
    static {
        ToStringSerializer tss = ToStringSerializer.instance;
        _serializers.put(Duration.class, tss);
        _serializers.put(XMLGregorianCalendar.class, new XMLGregorianCalendarSerializer());
        _serializers.put(QName.class, tss);
    }
    
//    @Override
    public Collection<Map.Entry<Class<?>,JsonSerializer<?>>> provide() {
        return _serializers.entrySet();
    }

    public static class XMLGregorianCalendarSerializer extends StdSerializer<XMLGregorianCalendar>
    {
        public XMLGregorianCalendarSerializer() {
            super(XMLGregorianCalendar.class);
        }

        @Override
        public void serialize(XMLGregorianCalendar value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException, JsonGenerationException {
            CalendarSerializer.instance.serialize(value.toGregorianCalendar(), jgen, provider);
        }

        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint) {
            CalendarSerializer.instance.acceptJsonFormatVisitor(visitor, null);
        }
    }
}
