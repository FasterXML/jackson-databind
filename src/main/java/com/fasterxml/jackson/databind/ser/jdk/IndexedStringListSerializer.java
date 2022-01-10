package com.fasterxml.jackson.databind.ser.jdk;

import java.util.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonArrayFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatTypes;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

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
    public final static IndexedStringListSerializer instance = new IndexedStringListSerializer();

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */
    
    protected IndexedStringListSerializer() {
        super(List.class);
    }

    public IndexedStringListSerializer(IndexedStringListSerializer src,
            Boolean unwrapSingle) {
        super(src, unwrapSingle);
    }

    @Override
    public ValueSerializer<?> _withResolved(BeanProperty prop, Boolean unwrapSingle) {
        return new IndexedStringListSerializer(this, unwrapSingle);
    }
    
    @Override protected JsonNode contentSchema() { return createSchemaNode("string", true); }

    @Override
    protected void acceptContentVisitor(JsonArrayFormatVisitor visitor) {
        visitor.itemsFormat(JsonFormatTypes.STRING);
    }

    /*
    /**********************************************************************
    /* Actual serialization
    /**********************************************************************
     */

    @Override
    public void serialize(List<String> value, JsonGenerator g,
            SerializerProvider provider) throws JacksonException
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
    public void serializeWithType(List<String> value, JsonGenerator g, SerializerProvider ctxt,
            TypeSerializer typeSer)
        throws JacksonException
    {
        WritableTypeId typeIdDef = typeSer.writeTypePrefix(g, ctxt,
                typeSer.typeId(value, JsonToken.START_ARRAY));
        g.assignCurrentValue(value);
        serializeContents(value, g, ctxt, value.size());
        typeSer.writeTypeSuffix(g, ctxt, typeIdDef);
    }

    private final void serializeContents(List<String> value, JsonGenerator g,
            SerializerProvider provider, int len) throws JacksonException
    {
        int i = 0;
        try {
            for (; i < len; ++i) {
                String str = value.get(i);
                if (str == null) {
                    provider.defaultSerializeNullValue(g);
                } else {
                    g.writeString(str);
                }
            }
        } catch (Exception e) {
            wrapAndThrow(provider, e, value, i);
        }
    }
}
