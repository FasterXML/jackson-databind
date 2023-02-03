package com.fasterxml.jackson.databind.ext;

import java.io.IOException;
import java.util.Calendar;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
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

    @SuppressWarnings("serial")
    public static class XMLGregorianCalendarSerializer
        extends StdSerializer<XMLGregorianCalendar>
        implements ContextualSerializer
    {
        final static XMLGregorianCalendarSerializer instance = new XMLGregorianCalendarSerializer();

        final JsonSerializer<Object> _delegate;

        public XMLGregorianCalendarSerializer() {
            this(CalendarSerializer.instance);
        }

        @SuppressWarnings("unchecked")
        protected XMLGregorianCalendarSerializer(JsonSerializer<?> del) {
            super(XMLGregorianCalendar.class);
            _delegate = (JsonSerializer<Object>) del;
        }

        @Override
        public JsonSerializer<?> getDelegatee() {
            return _delegate;
        }

        @Override
        public boolean isEmpty(SerializerProvider provider, XMLGregorianCalendar value) {
            return _delegate.isEmpty(provider, _convert(value));
        }

        @Override
        public void serialize(XMLGregorianCalendar value, JsonGenerator gen, SerializerProvider provider)
                throws IOException {
            _delegate.serialize(_convert(value), gen, provider);
        }

        @Override
        public void serializeWithType(XMLGregorianCalendar value, JsonGenerator g, SerializerProvider provider,
                TypeSerializer typeSer) throws IOException
        {
            // 16-Aug-2021, tatu: as per [databind#3217] we cannot simply delegate
            //    as that would produce wrong Type Id altogether. So redefine
            //    implementation from `StdScalarSerializer`
//            _delegate.serializeWithType(_convert(value), gen, provider, typeSer);

            // Need not really be string; just indicates "scalar of some kind"
            // (and so numeric timestamp is fine as well):
            WritableTypeId typeIdDef = typeSer.writeTypePrefix(g,
                    // important! Pass value AND type to use
                    typeSer.typeId(value, XMLGregorianCalendar.class, JsonToken.VALUE_STRING));
            // note: serialize() will convert to delegate value
            serialize(value, g, provider);
            typeSer.writeTypeSuffix(g, typeIdDef);
        }

        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint) throws JsonMappingException {
            _delegate.acceptJsonFormatVisitor(visitor, null);
        }

        @Override
        public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
                throws JsonMappingException {
            JsonSerializer<?> ser = prov.handlePrimaryContextualization(_delegate, property);
            if (ser != _delegate) {
                return new XMLGregorianCalendarSerializer(ser);
            }
            return this;
        }

        protected Calendar _convert(XMLGregorianCalendar input) {
            return (input == null) ? null : input.toGregorianCalendar();
        }
    }
}
