package tools.jackson.databind.ser.jdk;

import java.util.*;

import tools.jackson.core.*;
import tools.jackson.core.type.WritableTypeId;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JacksonStdImpl;
import tools.jackson.databind.jsonFormatVisitors.JsonArrayFormatVisitor;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatTypes;
import tools.jackson.databind.jsontype.TypeSerializer;

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

    @Deprecated // since 3.1.0
    public IndexedStringListSerializer(IndexedStringListSerializer src,
            Boolean unwrapSingle) {
        this(src, unwrapSingle, src._suppressableValue, src._suppressNulls);
    }

    /**
     * @since 3.1.0
     */
    public IndexedStringListSerializer(IndexedStringListSerializer src,
           Boolean unwrapSingle, Object suppressableValue, boolean suppressNulls) {
        super(src, unwrapSingle, suppressableValue, suppressNulls);
    }

    @Override
    public ValueSerializer<?> _withResolved(BeanProperty prop, Boolean unwrapSingle) {
        return new IndexedStringListSerializer(this, unwrapSingle, null, false);
    }

    @Override
    public ValueSerializer<?> _withResolved(BeanProperty prop, Boolean unwrapSingle,
           Object suppressableValue, boolean suppressNulls) {
        return new IndexedStringListSerializer(this, unwrapSingle, suppressableValue, suppressNulls);
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
            SerializationContext provider) throws JacksonException
    {
        final int len = value.size();
        if (len == 1) {
            if (((_unwrapSingle == null) &&
                    provider.isEnabled(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED))
                    || (_unwrapSingle == Boolean.TRUE)) {
                if ((_suppressableValue != null) || _suppressNulls) {
                    serializeFilteredContents(value, g, provider, 1);
                } else {
                    serializeContents(value, g, provider, 1);
                }
                return;
            }
        }
        g.writeStartArray(value, len);
        if ((_suppressableValue != null) || _suppressNulls) {
            serializeFilteredContents(value, g, provider, len);
        } else {
            serializeContents(value, g, provider, len);
        }
        g.writeEndArray();
    }

    @Override
    public void serializeWithType(List<String> value, JsonGenerator g, SerializationContext ctxt,
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
            SerializationContext provider, int len) throws JacksonException
    {
        serializeContentsImpl(value, g, provider, len, false);
    }

    private final void serializeFilteredContents(List<String> value, JsonGenerator g,
            SerializationContext provider, int len) throws JacksonException
    {
        serializeContentsImpl(value, g, provider, len, true);
    }

    private final void serializeContentsImpl(List<String> value, JsonGenerator g,
             SerializationContext provider, int len, boolean filtered) throws JacksonException
    {
        int i = 0;
        try {
            for (; i < len; ++i) {
                String str = value.get(i);
                if (str == null) {
                    if (filtered && _suppressNulls) {
                        continue;
                    }
                    provider.defaultSerializeNullValue(g);
                } else {
                    // Check if this element should be suppressed (only in filtered mode)
                    if (filtered && !_shouldSerializeElement(str, null, provider)) {
                        continue;
                    }
                    g.writeString(str);
                }
            }
        } catch (Exception e) {
            wrapAndThrow(provider, e, value, i);
        }
    }
}
