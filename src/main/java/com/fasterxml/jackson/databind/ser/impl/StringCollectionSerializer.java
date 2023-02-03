package com.fasterxml.jackson.databind.ser.impl;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.WritableTypeId;
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
            Boolean unwrapSingle)
    {
        super(src, unwrapSingle);
    }

    @Override
    public JsonSerializer<?> _withResolved(BeanProperty prop, Boolean unwrapSingle) {
        return new StringCollectionSerializer(this, unwrapSingle);
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
    public void serialize(Collection<String> value, JsonGenerator g,
            SerializerProvider provider) throws IOException
    {
        final int len = value.size();
        if (len == 1) {
            if (((_unwrapSingle == null) &&
                    provider.isEnabled(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED))
                    || (_unwrapSingle == Boolean.TRUE)) {
                serializeContents(value, g, provider);
                return;
            }
        }
        g.writeStartArray(value, len);
        serializeContents(value, g, provider);
        g.writeEndArray();
    }

    @Override
    public void serializeWithType(Collection<String> value, JsonGenerator g,
            SerializerProvider provider, TypeSerializer typeSer)
        throws IOException
    {
        WritableTypeId typeIdDef = typeSer.writeTypePrefix(g,
                typeSer.typeId(value, JsonToken.START_ARRAY));
        g.setCurrentValue(value);
        serializeContents(value, g, provider);
        typeSer.writeTypeSuffix(g, typeIdDef);
    }

    private final void serializeContents(Collection<String> value, JsonGenerator g,
            SerializerProvider provider)
        throws IOException
    {
        int i = 0;

        try {
            for (String str : value) {
                if (str == null) {
                    provider.defaultSerializeNull(g);
                } else {
                    g.writeString(str);
                }
                ++i;
            }
        } catch (Exception e) {
            wrapAndThrow(provider, e, value, i);
        }
    }
}
