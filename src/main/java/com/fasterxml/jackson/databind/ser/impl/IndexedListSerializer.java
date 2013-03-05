package com.fasterxml.jackson.databind.ser.impl;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContainerSerializer;
import com.fasterxml.jackson.databind.ser.std.AsArraySerializerBase;

/**
 * This is an optimized serializer for Lists that can be efficiently
 * traversed by index (as opposed to others, such as {@link LinkedList}
 * that can not}.
 */
@JacksonStdImpl
public final class IndexedListSerializer
    extends AsArraySerializerBase<List<?>>
{
    public IndexedListSerializer(JavaType elemType, boolean staticTyping, TypeSerializer vts,
            BeanProperty property, JsonSerializer<Object> valueSerializer)
    {
        super(List.class, elemType, staticTyping, vts, property, valueSerializer);
    }

    public IndexedListSerializer(IndexedListSerializer src,
            BeanProperty property, TypeSerializer vts, JsonSerializer<?> valueSerializer)
    {
        super(src, property, vts, valueSerializer);
    }

    @Override
    public IndexedListSerializer withResolved(BeanProperty property,
            TypeSerializer vts, JsonSerializer<?> elementSerializer) {
        return new IndexedListSerializer(this, property, vts, elementSerializer);
    }

    /*
    /**********************************************************
    /* Accessors
    /**********************************************************
     */
    
    @Override
    public boolean isEmpty(List<?> value) {
        return (value == null) || value.isEmpty();
    }

    @Override
    public boolean hasSingleElement(List<?> value) {
        return (value.size() == 1);
    }

    @Override
    public ContainerSerializer<?> _withValueTypeSerializer(TypeSerializer vts) {
        return new IndexedListSerializer(_elementType, _staticTyping, vts, _property, _elementSerializer);
    }
    
    @Override
    public void serializeContents(List<?> value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        if (_elementSerializer != null) {
            serializeContentsUsing(value, jgen, provider, _elementSerializer);
            return;
        }
        if (_valueTypeSerializer != null) {
            serializeTypedContents(value, jgen, provider);
            return;
        }
        final int len = value.size();
        if (len == 0) {
            return;
        }
        int i = 0;
        try {
            PropertySerializerMap serializers = _dynamicSerializers;
            for (; i < len; ++i) {
                Object elem = value.get(i);
                if (elem == null) {
                    provider.defaultSerializeNull(jgen);
                } else {
                    Class<?> cc = elem.getClass();
                    JsonSerializer<Object> serializer = serializers.serializerFor(cc);
                    if (serializer == null) {
                        // To fix [JACKSON-508]
                        if (_elementType.hasGenericTypes()) {
                            serializer = _findAndAddDynamic(serializers,
                                    provider.constructSpecializedType(_elementType, cc), provider);
                        } else {
                            serializer = _findAndAddDynamic(serializers, cc, provider);
                        }
                        serializers = _dynamicSerializers;
                    }
                    serializer.serialize(elem, jgen, provider);
                }
            }
        } catch (Exception e) {
            // [JACKSON-55] Need to add reference information
            wrapAndThrow(provider, e, value, i);
        }
    }
    
    public void serializeContentsUsing(List<?> value, JsonGenerator jgen, SerializerProvider provider,
            JsonSerializer<Object> ser)
        throws IOException, JsonGenerationException
    {
        final int len = value.size();
        if (len == 0) {
            return;
        }
        final TypeSerializer typeSer = _valueTypeSerializer;
        for (int i = 0; i < len; ++i) {
            Object elem = value.get(i);
            try {
                if (elem == null) {
                    provider.defaultSerializeNull(jgen);
                } else if (typeSer == null) {
                    ser.serialize(elem, jgen, provider);
                } else {
                    ser.serializeWithType(elem, jgen, provider, typeSer);
                }
            } catch (Exception e) {
                // [JACKSON-55] Need to add reference information
                wrapAndThrow(provider, e, value, i);
            }
        }
    }

    public void serializeTypedContents(List<?> value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        final int len = value.size();
        if (len == 0) {
            return;
        }
        int i = 0;
        try {
            final TypeSerializer typeSer = _valueTypeSerializer;
            PropertySerializerMap serializers = _dynamicSerializers;
            for (; i < len; ++i) {
                Object elem = value.get(i);
                if (elem == null) {
                    provider.defaultSerializeNull(jgen);
                } else {
                    Class<?> cc = elem.getClass();
                    JsonSerializer<Object> serializer = serializers.serializerFor(cc);
                    if (serializer == null) {
                        // To fix [JACKSON-508]
                        if (_elementType.hasGenericTypes()) {
                            serializer = _findAndAddDynamic(serializers,
                                    provider.constructSpecializedType(_elementType, cc), provider);
                        } else {
                            serializer = _findAndAddDynamic(serializers, cc, provider);
                        }
                        serializers = _dynamicSerializers;
                    }
                    serializer.serializeWithType(elem, jgen, provider, typeSer);
                }
            }
        } catch (Exception e) {
            // [JACKSON-55] Need to add reference information
            wrapAndThrow(provider, e, value, i);
        }
    }
}
