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
 * Efficient implement for serializing {@link Collection}s that contain Strings.
 * The only complexity is due to possibility that serializer for {@link String}
 * may be overridde; because of this, logic is needed to ensure that the default
 * serializer is in use to use fastest mode, or if not, to defer to custom
 * String serializer.
 */
@JacksonStdImpl
@SuppressWarnings("serial")
public class StringCollectionSerializer
    extends StaticListSerializerBase<Collection<String>>
{
    public final static StringCollectionSerializer instance = new StringCollectionSerializer();

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */
    
    protected StringCollectionSerializer() {
        super(Collection.class);
    }

    protected StringCollectionSerializer(StringCollectionSerializer src,
            JsonSerializer<?> ser, Boolean unwrapSingle)
    {
        super(src, ser, unwrapSingle);
    }        

    @Override
    public JsonSerializer<?> _withResolved(BeanProperty prop,
            JsonSerializer<?> ser, Boolean unwrapSingle) {
        return new StringCollectionSerializer(this, ser, unwrapSingle);
    }
    
    @Override protected JsonNode contentSchema() {
        return createSchemaNode("string", true);
    }
    
    @Override
    protected void acceptContentVisitor(JsonArrayFormatVisitor visitor) throws JsonMappingException
    {
        visitor.itemsFormat(JsonFormatTypes.STRING);
    }

    /*
    /**********************************************************
    /* Actual serialization
    /**********************************************************
     */
    
    @Override
    public void serialize(Collection<String> value, JsonGenerator gen,
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
            serializeContents(value, gen, provider);
        } else {
            serializeUsingCustom(value, gen, provider);
        }
        gen.writeEndArray();
    }

    private final void _serializeUnwrapped(Collection<String> value, JsonGenerator gen,
            SerializerProvider provider) throws IOException
    {
        if (_serializer == null) {
            serializeContents(value, gen, provider);
        } else {
            serializeUsingCustom(value, gen, provider);
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
