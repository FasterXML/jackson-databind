package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ResolvableSerializer;
import com.fasterxml.jackson.databind.util.ClassUtil;

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
    implements ResolvableSerializer
{
    protected JsonSerializer<String> _serializer;
    
    public IndexedStringListSerializer(BeanProperty property) {
        super(List.class, property);
    }

    @Override protected JsonNode contentSchema() {
        return createSchemaNode("string", true);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void resolve(SerializerProvider provider) throws JsonMappingException
    {
        JsonSerializer<?> ser = provider.findValueSerializer(String.class, _property);
        if (!ClassUtil.isJacksonStdImpl(ser)) {
            _serializer = (JsonSerializer<String>) ser;
        }
    }

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
