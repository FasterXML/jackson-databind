package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.*;

public abstract class ArraySerializerBase<T>
    extends ContainerSerializer<T>
{
    protected final TypeSerializer _valueTypeSerializer;

    protected final BeanProperty _property;
    
    protected ArraySerializerBase(Class<T> cls, TypeSerializer vts, BeanProperty property)
    {
        super(cls);
        _valueTypeSerializer = vts;
        _property = property;
    }
    
    protected ArraySerializerBase(ArraySerializerBase<?> src, TypeSerializer vts)
    {
        super(src._handledType, false);
        _valueTypeSerializer = vts;
        _property = src._property;
    }
    
    protected ArraySerializerBase(ArraySerializerBase<?> src, BeanProperty property)
    {
        super(src._handledType, false);
        _valueTypeSerializer = src._valueTypeSerializer;
        _property = property;
    }

    
    @Override
    public final void serialize(T value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        jgen.writeStartArray();
        serializeContents(value, jgen, provider);
        jgen.writeEndArray();
    }
    
    @Override
    public final void serializeWithType(T value, JsonGenerator jgen, SerializerProvider provider,
            TypeSerializer typeSer)
        throws IOException, JsonGenerationException
    {
        typeSer.writeTypePrefixForArray(value, jgen);
        serializeContents(value, jgen, provider);
        typeSer.writeTypeSuffixForArray(value, jgen);
    }

    protected abstract void serializeContents(T value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException;
}
