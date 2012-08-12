package com.fasterxml.jackson.databind.ser.impl;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonArrayFormatVisitor;
import com.fasterxml.jackson.databind.jsonschema.types.SchemaType;
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
    
	@Override
	protected void acceptContentVisitor(JsonArrayFormatVisitor visitor) {
		visitor.itemsFormat(SchemaType.STRING);
		
	}

    /*
    /**********************************************************
    /* Post-processing
    /**********************************************************
     */
    
//  @Override
    public JsonSerializer<?> createContextual(SerializerProvider provider,
            BeanProperty property)
        throws JsonMappingException
    {
        JsonSerializer<?> ser = _serializer;
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
        jgen.writeStartArray();
        if (_serializer == null) {
            serializeContents(value, jgen, provider);
        } else {
            serializeUsingCustom(value, jgen, provider);
        }
        jgen.writeEndArray();
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
