package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

@SuppressWarnings("serial")
public class EnumSetSerializer
    extends AsArraySerializerBase<EnumSet<? extends Enum<?>>>
{
    /**
     * @since 2.6
     */
    public EnumSetSerializer(JavaType elemType) {
        super(EnumSet.class, elemType, true, null, null);
    }

    public EnumSetSerializer(EnumSetSerializer src,
            BeanProperty property, TypeSerializer vts, JsonSerializer<?> valueSerializer,
            Boolean unwrapSingle) {
        super(src, property, vts, valueSerializer, unwrapSingle);
    }

    @Override
    public EnumSetSerializer _withValueTypeSerializer(TypeSerializer vts) {
        return new EnumSetSerializer(this, _property, vts, _elementSerializer, _unwrapSingle);
    }

    @Override
    public EnumSetSerializer withResolved(BeanProperty property,
            TypeSerializer vts, JsonSerializer<?> elementSerializer,
            Boolean unwrapSingle) {
        return new EnumSetSerializer(this, property, vts, elementSerializer, unwrapSingle);
    }

    @Override
    public boolean isEmpty(SerializerProvider prov, EnumSet<? extends Enum<?>> value) {
        return value.isEmpty();
    }

    @Override
    public boolean hasSingleElement(EnumSet<? extends Enum<?>> value) {
        return value.size() == 1;
    }

    @Override
    public final void serialize(EnumSet<? extends Enum<?>> value, JsonGenerator gen,
            SerializerProvider provider) throws IOException
    {
        final int len = value.size();
        if (len == 1) {
            if (((_unwrapSingle == null)
                    && provider.isEnabled(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED))
                    || (_unwrapSingle == Boolean.TRUE)) {
                serializeContents(value, gen, provider);
                return;
            }
        }
        gen.writeStartArray(value, len);
        serializeContents(value, gen, provider);
        gen.writeEndArray();
    }

    @Override
    public void serializeContents(EnumSet<? extends Enum<?>> value, JsonGenerator gen,
            SerializerProvider provider)
        throws IOException
    {
        gen.assignCurrentValue(value);
        if (_valueTypeSerializer != null) {
            // 16-Dec-2024, tatu: As per [databind#4849], need to support polymorphic
            //   types for elements
             _serializeTypedContents(value, gen, provider, _valueTypeSerializer);
             return;
        }
        JsonSerializer<Object> enumSer = _elementSerializer;
        // Need to dynamically find instance serializer; unfortunately
        // that seems to be the only way to figure out type (no accessors
        // to the enum class that set knows)
        for (Enum<?> en : value) {
            if (enumSer == null) {
                enumSer = provider.findContentValueSerializer(en.getDeclaringClass(), _property);
            }
            enumSer.serialize(en, gen, provider);
        }
    }

    private void _serializeTypedContents(EnumSet<? extends Enum<?>> value, JsonGenerator gen,
            SerializerProvider provider, TypeSerializer vts)
        throws IOException
    {
        JsonSerializer<Object> enumSer = _elementSerializer;
        for (Enum<?> en : value) {
            if (enumSer == null) {
                enumSer = provider.findContentValueSerializer(en.getDeclaringClass(), _property);
            }
            enumSer.serializeWithType(en, gen, provider, vts);
        }
    }
}
