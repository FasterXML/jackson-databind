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
    public IteratorSerializer(JavaType elemType, boolean staticTyping, TypeSerializer vts) {
        super(Iterator.class, elemType, staticTyping, vts, null);
    }

    public IteratorSerializer(IteratorSerializer src,
            BeanProperty property, TypeSerializer vts, JsonSerializer<?> valueSerializer,
            Boolean unwrapSingle) {
        super(src, property, vts, valueSerializer, unwrapSingle);
    }

    @Override
    public boolean isEmpty(SerializerProvider prov, Iterator<?> value) {
        return !value.hasNext();
    }

    @Override
    public boolean hasSingleElement(Iterator<?> value) {
        // no really good way to determine (without consuming iterator), so:
        return false;
    }

    @Override
    public ContainerSerializer<?> _withValueTypeSerializer(TypeSerializer vts) {
        return new IteratorSerializer(this, _property, vts, _elementSerializer, _unwrapSingle);
    }

    @Override
    public IteratorSerializer withResolved(BeanProperty property,
            TypeSerializer vts, JsonSerializer<?> elementSerializer,
            Boolean unwrapSingle) {
        return new IteratorSerializer(this, property, vts, elementSerializer, unwrapSingle);
    }

    @Override
    public final void serialize(Iterator<?> value, JsonGenerator gen,
            SerializerProvider provider) throws IOException
    {
        // 02-Dec-2016, tatu: As per comments above, can't determine single element so...
        /*
        if (((_unwrapSingle == null) &&
                provider.isEnabled(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED))
                || (_unwrapSingle == Boolean.TRUE)) {
            if (hasSingleElement(value)) {
                serializeContents(value, gen, provider);
                return;
            }
        }
        */
        gen.writeStartArray(value);
        serializeContents(value, gen, provider);
        gen.writeEndArray();
    }

    @Override
    public void serializeContents(Iterator<?> value, JsonGenerator g,
            SerializerProvider provider) throws IOException
    {
        if (!value.hasNext()) {
            return;
        }
        JsonSerializer<Object> serializer = _elementSerializer;
        if (serializer == null) {
            _serializeDynamicContents(value, g, provider);
            return;
        }
        final TypeSerializer typeSer = _valueTypeSerializer;
        do {
            Object elem = value.next();
            if (elem == null) {
                provider.defaultSerializeNull(g);
            } else if (typeSer == null) {
                serializer.serialize(elem, g, provider);
            } else {
                serializer.serializeWithType(elem, g, provider, typeSer);
            }
        } while (value.hasNext());
    }

    protected void _serializeDynamicContents(Iterator<?> value, JsonGenerator g,
            SerializerProvider provider) throws IOException
    {
        final TypeSerializer typeSer = _valueTypeSerializer;
        PropertySerializerMap serializers = _dynamicSerializers;
        do {
            Object elem = value.next();
            if (elem == null) {
                provider.defaultSerializeNull(g);
                continue;
            }
            Class<?> cc = elem.getClass();
            JsonSerializer<Object> serializer = serializers.serializerFor(cc);
            if (serializer == null) {
                if (_elementType.hasGenericTypes()) {
                    serializer = _findAndAddDynamic(serializers,
                            provider.constructSpecializedType(_elementType, cc), provider);
                } else {
                    serializer = _findAndAddDynamic(serializers, cc, provider);
                }
                serializers = _dynamicSerializers;
            }
            if (typeSer == null) {
                serializer.serialize(elem, g, provider);
            } else {
                serializer.serializeWithType(elem, g, provider, typeSer);
            }
        } while (value.hasNext());
    }
}