package com.fasterxml.jackson.databind.ser.impl;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonArrayFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatTypes;
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
    public final static IndexedStringListSerializer instance = new IndexedStringListSerializer();
    
    protected final JsonSerializer<String> _serializer;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */
    
    protected IndexedStringListSerializer() {
        this(null);
    }

    @SuppressWarnings("unchecked")
    public IndexedStringListSerializer(JsonSerializer<?> ser) {
        super(List.class);
        _serializer = (JsonSerializer<String>) ser;
        
    }

    @Override protected JsonNode contentSchema() {
        return createSchemaNode("string", true);
    }

    @Override
    protected void acceptContentVisitor(JsonArrayFormatVisitor visitor)
        throws JsonMappingException
    {
		visitor.itemsFormat(JsonFormatTypes.STRING);
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
        /* 29-Sep-2012, tatu: Actually, we need to do much more contextual
         *    checking here since we finally know for sure the property,
         *    and it may have overrides
         */
        JsonSerializer<?> ser = null;
        // First: if we have a property, may have property-annotation overrides
        if (property != null) {
            AnnotatedMember m = property.getMember();
            if (m != null) {
                Object serDef = provider.getAnnotationIntrospector().findContentSerializer(m);
                if (serDef != null) {
                    ser = provider.serializerInstance(m, serDef);
                }
            }
        }
        if (ser == null) {
            ser = _serializer;
        }
        // #124: May have a content converter
        ser = findConvertingContentSerializer(provider, property, ser);
        if (ser == null) {
            ser = provider.findValueSerializer(String.class, property);
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
        final int len = value.size();
        // [JACKSON-805]
        if ((len == 1) && provider.isEnabled(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)) {
            _serializeUnwrapped(value, jgen, provider);
            return;
        }
        
        jgen.writeStartArray();
        if (_serializer == null) {
            serializeContents(value, jgen, provider, len);
        } else {
            serializeUsingCustom(value, jgen, provider, len);
        }
        jgen.writeEndArray();
    }

    private final void _serializeUnwrapped(List<String> value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        if (_serializer == null) {
            serializeContents(value, jgen, provider, 1);
        } else {
            serializeUsingCustom(value, jgen, provider, 1);
        }
    }
    
    @Override
    public void serializeWithType(List<String> value, JsonGenerator jgen, SerializerProvider provider,
            TypeSerializer typeSer)
        throws IOException, JsonGenerationException
    {
        final int len = value.size();
        typeSer.writeTypePrefixForArray(value, jgen);
        if (_serializer == null) {
            serializeContents(value, jgen, provider, len);
        } else {
            serializeUsingCustom(value, jgen, provider, len);
        }
        typeSer.writeTypeSuffixForArray(value, jgen);
    }
    
    private final void serializeContents(List<String> value, JsonGenerator jgen, SerializerProvider provider,
            int len)
        throws IOException, JsonGenerationException
    {
        int i = 0;
        try {
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

    private final void serializeUsingCustom(List<String> value, JsonGenerator jgen, SerializerProvider provider,
            int len)
        throws IOException, JsonGenerationException
    {
        int i = 0;
        try {
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
