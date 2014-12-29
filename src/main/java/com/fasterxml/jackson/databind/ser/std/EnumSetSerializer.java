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
    public EnumSetSerializer(JavaType elemType, BeanProperty property)
    {
        super(EnumSet.class, elemType, true, null, property, null);
    }

    public EnumSetSerializer(EnumSetSerializer src,
            BeanProperty property, TypeSerializer vts, JsonSerializer<?> valueSerializer)
    {
        super(src, property, vts, valueSerializer);
    }
    
    @Override
    public EnumSetSerializer _withValueTypeSerializer(TypeSerializer vts) {
        // no typing for enums (always "hard" type)
        return this;
    }

    @Override
    public EnumSetSerializer withResolved(BeanProperty property,
            TypeSerializer vts, JsonSerializer<?> elementSerializer) {
        return new EnumSetSerializer(this, property, vts, elementSerializer);
    }
    
    @Override
    public boolean isEmpty(SerializerProvider prov, EnumSet<? extends Enum<?>> value) {
        return (value == null) || value.isEmpty();
    }

    @Override
    public boolean hasSingleElement(EnumSet<? extends Enum<?>> value) {
        return value.size() == 1;
    }

    @Override
    public final void serialize(EnumSet<? extends Enum<?>> value, JsonGenerator jgen, SerializerProvider provider) throws IOException
    {
    	final int len = value.size();
        if ((len == 1) && provider.isEnabled(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)) {
            serializeContents(value, jgen, provider);
            return;
        }
        jgen.writeStartArray(len);
        serializeContents(value, jgen, provider);
        jgen.writeEndArray();
    }
    
    @Override
    public void serializeContents(EnumSet<? extends Enum<?>> value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        JsonSerializer<Object> enumSer = _elementSerializer;
        /* Need to dynamically find instance serializer; unfortunately
         * that seems to be the only way to figure out type (no accessors
         * to the enum class that set knows)
         */
        for (Enum<?> en : value) {
            if (enumSer == null) {
                /* 12-Jan-2010, tatu: Since enums can not be polymorphic, let's
                 *   not bother with typed serializer variant here
                 */
                enumSer = provider.findValueSerializer(en.getDeclaringClass(), _property);
            }
            enumSer.serialize(en, jgen, provider);
        }
    }
}
