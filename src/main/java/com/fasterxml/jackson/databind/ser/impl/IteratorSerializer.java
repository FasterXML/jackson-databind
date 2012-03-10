package com.fasterxml.jackson.databind.ser.impl;

import java.io.IOException;
import java.util.Iterator;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContainerSerializer;
import com.fasterxml.jackson.databind.ser.std.AsArraySerializerBase;

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
    public boolean isEmpty(Iterator<?> value) {
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
    public void serializeContents(Iterator<?> value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        if (value.hasNext()) {
            final TypeSerializer typeSer = _valueTypeSerializer;
            JsonSerializer<Object> prevSerializer = null;
            Class<?> prevClass = null;
            do {
                Object elem = value.next();
                if (elem == null) {
                    provider.defaultSerializeNull(jgen);
                } else {
                    // Minor optimization to avoid most lookups:
                    Class<?> cc = elem.getClass();
                    JsonSerializer<Object> currSerializer;
                    if (cc == prevClass) {
                        currSerializer = prevSerializer;
                    } else {
                        currSerializer = provider.findValueSerializer(cc, _property);
                        prevSerializer = currSerializer;
                        prevClass = cc;
                    }
                    if (typeSer == null) {
                        currSerializer.serialize(elem, jgen, provider);
                    } else {
                        currSerializer.serializeWithType(elem, jgen, provider, typeSer);
                    }
                }
            } while (value.hasNext());
        }
    }
}