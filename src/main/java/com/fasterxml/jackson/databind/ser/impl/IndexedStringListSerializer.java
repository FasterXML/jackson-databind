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
            Boolean unwrapSingle) {
        super(src, unwrapSingle);
    }

    @Override
    public JsonSerializer<?> _withResolved(BeanProperty prop, Boolean unwrapSingle) {
        return new IndexedStringListSerializer(this, unwrapSingle);
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
    public void serialize(List<String> value, JsonGenerator g,
            SerializerProvider provider) throws IOException
    {
        final int len = value.size();
        if (len == 1) {
            if (((_unwrapSingle == null) &&
                    provider.isEnabled(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED))
                    || (_unwrapSingle == Boolean.TRUE)) {
                serializeContents(value, g, provider, 1);
                return;
            }
        }
        g.writeStartArray(value, len);
        serializeContents(value, g, provider, len);
        g.writeEndArray();
    }

    @Override
    public void serializeWithType(List<String> value, JsonGenerator g, SerializerProvider provider,
            TypeSerializer typeSer)
        throws IOException
    {
        WritableTypeId typeIdDef = typeSer.writeTypePrefix(g,
                typeSer.typeId(value, JsonToken.START_ARRAY));
        g.setCurrentValue(value);
        serializeContents(value, g, provider, value.size());
        typeSer.writeTypeSuffix(g, typeIdDef);
    }

    private final void serializeContents(List<String> value, JsonGenerator g,
            SerializerProvider provider, int len) throws IOException
    {
        int i = 0;
        try {
            for (; i < len; ++i) {
                String str = value.get(i);
                if (str == null) {
                    provider.defaultSerializeNull(g);
                } else {
                    g.writeString(str);
                }
            }
        } catch (Exception e) {
            wrapAndThrow(provider, e, value, i);
        }
    }
}
