package com.fasterxml.jackson.databind.ser.impl;

import java.io.IOException;
import java.util.Iterator;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContainerSerializer;
import com.fasterxml.jackson.databind.ser.std.AsArraySerializerBase;

@SuppressWarnings("serial")
@JacksonStdImpl
public class IteratorSerializer
    extends AsArraySerializerBase<Iterator<?>>
{
    public IteratorSerializer(JavaType elemType, boolean staticTyping, TypeSerializer vts,
            BeanProperty property)
    {
        super(Iterator.class, elemType, staticTyping, vts, property, null);
    }

    public IteratorSerializer(IteratorSerializer src,
            BeanProperty property, TypeSerializer vts, JsonSerializer<?> valueSerializer)
    {
        super(src, property, vts, valueSerializer);
    }

    @Override
    public boolean isEmpty(SerializerProvider prov, Iterator<?> value) {
        return (value == null) || !value.hasNext();
    }

    @Override
    public boolean hasSingleElement(Iterator<?> value) {
        // no really good way to determine (without consuming iterator), so:
        return false;
    }
    
    @Override
    public ContainerSerializer<?> _withValueTypeSerializer(TypeSerializer vts) {
        return new IteratorSerializer(_elementType, _staticTyping, vts, _property);
    }

    @Override
    public IteratorSerializer withResolved(BeanProperty property,
            TypeSerializer vts, JsonSerializer<?> elementSerializer) {
        return new IteratorSerializer(this, property, vts, elementSerializer);
    }

    @Override
    public final void serialize(Iterator<?> value, JsonGenerator jgen, SerializerProvider provider) throws IOException
    {
        if (provider.isEnabled(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED) && hasSingleElement(value)) {
            serializeContents(value, jgen, provider);
            return;
        }
        jgen.writeStartArray();
        serializeContents(value, jgen, provider);
        jgen.writeEndArray();
    }
    
    @Override
    public void serializeContents(Iterator<?> value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException
    {
        if (value.hasNext()) {
            final TypeSerializer typeSer = _valueTypeSerializer;
            JsonSerializer<Object> prevSerializer = null;
            Class<?> prevClass = null;
            do {
                Object elem = value.next();
                if (elem == null) {
                    provider.defaultSerializeNull(jgen);
                    continue;
                }
                JsonSerializer<Object> currSerializer = _elementSerializer;
                if (currSerializer == null) {
                    // Minor optimization to avoid most lookups:
                    Class<?> cc = elem.getClass();
                    if (cc == prevClass) {
                        currSerializer = prevSerializer;
                    } else {
                        currSerializer = provider.findValueSerializer(cc, _property);
                        prevSerializer = currSerializer;
                        prevClass = cc;
                    }
                }
                if (typeSer == null) {
                    currSerializer.serialize(elem, jgen, provider);
                } else {
                    currSerializer.serializeWithType(elem, jgen, provider, typeSer);
                }
            } while (value.hasNext());
        }
    }
}