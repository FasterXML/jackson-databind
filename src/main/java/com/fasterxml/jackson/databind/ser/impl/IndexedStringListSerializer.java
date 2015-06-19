package com.fasterxml.jackson.databind.ser.impl;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonArrayFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatTypes;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StaticListSerializerBase;

/**
 * Efficient implement for serializing {@link List}s that contains Strings and are random-accessible.
 * The only complexity is due to possibility that serializer for {@link String}
 * may be overridde; because of this, logic is needed to ensure that the default
 * serializer is in use to use fastest mode, or if not, to defer to custom
 * String serializer.
 */
@JacksonStdImpl
public final class IndexedStringListSerializer
    extends StaticListSerializerBase<List<String>>
{
    private static final long serialVersionUID = 1L;

    public final static IndexedStringListSerializer instance = new IndexedStringListSerializer();

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */
    
    protected IndexedStringListSerializer() {
        super(List.class);
    }

    public IndexedStringListSerializer(IndexedStringListSerializer src,
            JsonSerializer<?> ser, Boolean unwrapSingle) {
        super(src, ser, unwrapSingle);
    }

    @Override
    public JsonSerializer<?> _withResolved(BeanProperty prop,
            JsonSerializer<?> ser, Boolean unwrapSingle) {
        return new IndexedStringListSerializer(this, ser, unwrapSingle);
    }
    
    @Override protected JsonNode contentSchema() { return createSchemaNode("string", true); }

    @Override
    protected void acceptContentVisitor(JsonArrayFormatVisitor visitor) throws JsonMappingException {
        visitor.itemsFormat(JsonFormatTypes.STRING);
    }

    /*
    /**********************************************************
    /* Actual serialization
    /**********************************************************
     */

    @Override
    public void serialize(List<String> value, JsonGenerator gen,
            SerializerProvider provider) throws IOException
    {
        final int len = value.size();
        if (len == 1) {
            if (((_unwrapSingle == null) &&
                    provider.isEnabled(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED))
                    || (_unwrapSingle == Boolean.TRUE)) {
                _serializeUnwrapped(value, gen, provider);
                return;
            }
        }
        
        gen.writeStartArray(len);
        if (_serializer == null) {
            serializeContents(value, gen, provider, len);
        } else {
            serializeUsingCustom(value, gen, provider, len);
        }
        gen.writeEndArray();
    }

    private final void _serializeUnwrapped(List<String> value, JsonGenerator gen,
            SerializerProvider provider) throws IOException
    {
        if (_serializer == null) {
            serializeContents(value, gen, provider, 1);
        } else {
            serializeUsingCustom(value, gen, provider, 1);
        }
    }
    
    @Override
    public void serializeWithType(List<String> value, JsonGenerator gen,
            SerializerProvider provider,
            TypeSerializer typeSer) throws IOException
    {
        final int len = value.size();
        typeSer.writeTypePrefixForArray(value, gen);
        if (_serializer == null) {
            serializeContents(value, gen, provider, len);
        } else {
            serializeUsingCustom(value, gen, provider, len);
        }
        typeSer.writeTypeSuffixForArray(value, gen);
    }

    private final void serializeContents(List<String> value, JsonGenerator gen,
            SerializerProvider provider, int len) throws IOException
    {
        int i = 0;
        try {
            for (; i < len; ++i) {
                String str = value.get(i);
                if (str == null) {
                    provider.defaultSerializeNull(gen);
                } else {
                    gen.writeString(str);
                }
            }
        } catch (Exception e) {
            wrapAndThrow(provider, e, value, i);
        }
    }

    private final void serializeUsingCustom(List<String> value, JsonGenerator gen,
            SerializerProvider provider, int len) throws IOException
    {
        int i = 0;
        try {
            final JsonSerializer<String> ser = _serializer;
            for (i = 0; i < len; ++i) {
                String str = value.get(i);
                if (str == null) {
                    provider.defaultSerializeNull(gen);
                } else {
                    ser.serialize(str, gen, provider);
                }
            }
        } catch (Exception e) {
            wrapAndThrow(provider, e, value, i);
        }
    }
}
