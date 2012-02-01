package com.fasterxml.jackson.databind.ser.impl;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
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
    implements ContextualSerializer
{
    protected final JsonSerializer<String> _serializer;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */
    
    public IndexedStringListSerializer() {
        this(null);
    }

    @SuppressWarnings("unchecked")
    public IndexedStringListSerializer(JsonSerializer<?> ser) {
        super(List.class, null);
        _serializer = (JsonSerializer<String>) ser;
        
    }

    @Override protected JsonNode contentSchema() {
        return createSchemaNode("string", true);
    }

    /*
    /**********************************************************
    /* Post-processing
    /**********************************************************
     */
    
    @Override
    public JsonSerializer<?> createContextual(SerializerProvider provider,
            BeanProperty property)
        throws JsonMappingException
    {
        JsonSerializer<?> ser = _serializer;
        if (ser == null) {
            ser = provider.findValueSerializer(String.class, _property);
        } else if (ser instanceof ContextualSerializer) {
            ser = ((ContextualSerializer) ser).createContextual(provider, property);
        }
        // Optimization: default serializer just writes String, so we can avoid a call:
        if (isDefaultSerializer(ser)) {
            ser = null;
        }
        // note: will never have TypeSerializer, because Strings are "natural" type
        if (ser == _serializer) {
            return this;
        }
        return new IndexedStringListSerializer(ser);
    }

    /*
    /**********************************************************
    /* Actual serialization
    /**********************************************************
     */

    @Override
    public void serialize(List<String> value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        jgen.writeStartArray();
        if (_serializer == null) {
            serializeContents(value, jgen, provider);
        } else {
            serializeUsingCustom(value, jgen, provider);
        }
        jgen.writeEndArray();
    }
    
    @Override
    public void serializeWithType(List<String> value, JsonGenerator jgen, SerializerProvider provider,
            TypeSerializer typeSer)
        throws IOException, JsonGenerationException
    {
        typeSer.writeTypePrefixForArray(value, jgen);
        if (_serializer == null) {
            serializeContents(value, jgen, provider);
        } else {
            serializeUsingCustom(value, jgen, provider);
        }
        typeSer.writeTypeSuffixForArray(value, jgen);
    }
    
    private final void serializeContents(List<String> value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        int i = 0;
        try {
            final int len = value.size();
            for (; i < len; ++i) {
                String str = value.get(i);
                if (str == null) {
                    provider.defaultSerializeNull(jgen);
                } else {
                    jgen.writeString(str);
                }
            }
        } catch (Exception e) {
            wrapAndThrow(provider, e, value, i);
        }
    }

    private final void serializeUsingCustom(List<String> value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        int i = 0;
        try {
            final int len = value.size();
            final JsonSerializer<String> ser = _serializer;
            for (i = 0; i < len; ++i) {
                String str = value.get(i);
                if (str == null) {
                    provider.defaultSerializeNull(jgen);
                } else {
                    ser.serialize(str, jgen, provider);
                }
            }
        } catch (Exception e) {
            wrapAndThrow(provider, e, value, i);
        }
    }
}
