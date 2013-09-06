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
 * Efficient implement for serializing {@link Collection}s that contain Strings.
 * The only complexity is due to possibility that serializer for {@link String}
 * may be overridde; because of this, logic is needed to ensure that the default
 * serializer is in use to use fastest mode, or if not, to defer to custom
 * String serializer.
 */
@JacksonStdImpl
public class StringCollectionSerializer
    extends StaticListSerializerBase<Collection<String>>
    implements ContextualSerializer
{
    public final static StringCollectionSerializer instance = new StringCollectionSerializer();
    
    protected final JsonSerializer<String> _serializer;
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */
    
    protected StringCollectionSerializer() {
        this(null);
    }

    @SuppressWarnings("unchecked")
    protected StringCollectionSerializer(JsonSerializer<?> ser)
    {
        super(Collection.class);
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
        } else {
            ser = provider.handleSecondaryContextualization(ser, property);
        }
        // Optimization: default serializer just writes String, so we can avoid a call:
        if (isDefaultSerializer(ser)) {
            ser = null;
        }
        // note: will never have TypeSerializer, because Strings are "natural" type
        if (ser == _serializer) {
            return this;
        }
        return new StringCollectionSerializer(ser);
    }

    /*
    /**********************************************************
    /* Actual serialization
    /**********************************************************
     */
    
    @Override
    public void serialize(Collection<String> value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        // [JACKSON-805]
        if ((value.size() == 1) && provider.isEnabled(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)) {
            _serializeUnwrapped(value, jgen, provider);
            return;
        }      
        jgen.writeStartArray();
        if (_serializer == null) {
            serializeContents(value, jgen, provider);
        } else {
            serializeUsingCustom(value, jgen, provider);
        }
        jgen.writeEndArray();
    }

    private final void _serializeUnwrapped(Collection<String> value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        if (_serializer == null) {
            serializeContents(value, jgen, provider);
        } else {
            serializeUsingCustom(value, jgen, provider);
        }
    }

    @Override
    public void serializeWithType(Collection<String> value, JsonGenerator jgen, SerializerProvider provider,
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

    private final void serializeContents(Collection<String> value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        if (_serializer != null) {
            serializeUsingCustom(value, jgen, provider);
            return;
        }
        int i = 0;
        for (String str : value) {
            try {
                if (str == null) {
                    provider.defaultSerializeNull(jgen);
                } else {
                    jgen.writeString(str);
                }
                ++i;
            } catch (Exception e) {
                wrapAndThrow(provider, e, value, i);
            }
        }
    }

    private void serializeUsingCustom(Collection<String> value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        final JsonSerializer<String> ser = _serializer;
        int i = 0;
        for (String str : value) {
            try {
                if (str == null) {
                    provider.defaultSerializeNull(jgen);
                } else {
                    ser.serialize(str, jgen, provider);
                }
            } catch (Exception e) {
                wrapAndThrow(provider, e, value, i);
            }
       }
    }
}
