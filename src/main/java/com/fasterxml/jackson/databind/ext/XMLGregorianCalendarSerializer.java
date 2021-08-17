package com.fasterxml.jackson.databind.ext;

import java.util.Calendar;

import javax.xml.datatype.XMLGregorianCalendar;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ValueSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.jdk.JavaUtilCalendarSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class XMLGregorianCalendarSerializer
    extends StdSerializer<XMLGregorianCalendar>
{
    final static XMLGregorianCalendarSerializer instance = new XMLGregorianCalendarSerializer();

    final ValueSerializer<Object> _delegate;
    
    public XMLGregorianCalendarSerializer() {
        this(JavaUtilCalendarSerializer.instance);
    }

    @SuppressWarnings("unchecked")
    protected XMLGregorianCalendarSerializer(ValueSerializer<?> del) {
        super(XMLGregorianCalendar.class);
        _delegate = (ValueSerializer<Object>) del;
    }

    @Override
    public ValueSerializer<?> getDelegatee() {
        return _delegate;
    }

    @Override
    public boolean isEmpty(SerializerProvider provider, XMLGregorianCalendar value) {
        return _delegate.isEmpty(provider, _convert(value));
    }

    @Override
    public void serialize(XMLGregorianCalendar value, JsonGenerator gen, SerializerProvider provider)
        throws JacksonException
    {
        _delegate.serialize(_convert(value), gen, provider);
    }

    @Override
    public void serializeWithType(XMLGregorianCalendar value, JsonGenerator g, SerializerProvider ctxt,
            TypeSerializer typeSer) throws JacksonException
    {
        // 16-Aug-2021, tatu: as per [databind#3217] we cannot simply delegate
        //    as that would produce wrong Type Id altogether. So redefine
        //    implementation from `StdScalarSerializer`

        // Need not really be string; just indicates "scalar of some kind"
        // (and so numeric timestamp is fine as well):
        WritableTypeId typeIdDef = typeSer.writeTypePrefix(g, ctxt,
                // important! Pass value AND type to use
                typeSer.typeId(value, XMLGregorianCalendar.class, JsonToken.VALUE_STRING));
        // note: serialize() will convert to delegate value
        serialize(value, g, ctxt);
        typeSer.writeTypeSuffix(g, ctxt, typeIdDef);
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint) {
        _delegate.acceptJsonFormatVisitor(visitor, null);
    }

    @Override
    public ValueSerializer<?> createContextual(SerializerProvider ctxt, BeanProperty property)
    {
        ValueSerializer<?> ser = ctxt.handlePrimaryContextualization(_delegate, property);
        if (ser != _delegate) {
            return new XMLGregorianCalendarSerializer(ser);
        }
        return this;
    }

    protected Calendar _convert(XMLGregorianCalendar input) {
        return (input == null) ? null : input.toGregorianCalendar();
    }
}