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
 * Efficient implement for serializing {@link Collection}s that contain Strings.
 * The only complexity is due to possibility that serializer for {@link String}
 * may be override; because of this, logic is needed to ensure that the default
 * serializer is in use to use fastest mode, or if not, to defer to custom
 * String serializer.
 */
@JacksonStdImpl
public class StringCollectionSerializer
    extends StaticListSerializerBase<Collection<String>>
{
    public final static StringCollectionSerializer instance = new StringCollectionSerializer();

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    protected StringCollectionSerializer() {
        super(Collection.class, String.class);
    }

    @Deprecated // since 3.1
    protected StringCollectionSerializer(StringCollectionSerializer src,
            Boolean unwrapSingle)
    {
        this(src, unwrapSingle, null, false);
    }

    /**
     * @since 3.1
     */
    protected StringCollectionSerializer(StringCollectionSerializer src,
             Boolean unwrapSingle, Object suppressableValue, boolean suppressNulls)
    {
        super(src, unwrapSingle, suppressableValue, suppressNulls);
    }

    @Deprecated // @since 3.1
    @Override
    public ValueSerializer<?> _withResolved(BeanProperty prop, Boolean unwrapSingle) {
        return new StringCollectionSerializer(this, unwrapSingle, null, false);
    }

    @Override
    public ValueSerializer<?> _withResolved(BeanProperty prop, Boolean unwrapSingle, Object suppressableValue, boolean suppressNulls) {
        return new StringCollectionSerializer(this, unwrapSingle, suppressableValue, suppressNulls);
    }

    @Override
    protected JsonNode contentSchema() {
        return createSchemaNode("string", true);
    }

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
    public void serialize(Collection<String> value, JsonGenerator g,
            SerializationContext ctxt) throws JacksonException
    {
        final int len = value.size();
        if (len == 1) {
            if (((_unwrapSingle == null) &&
                    ctxt.isEnabled(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED))
                    || (_unwrapSingle == Boolean.TRUE)) {
                if (ctxt.isEnabled(SerializationFeature.APPLY_JSON_INCLUDE_FOR_CONTAINERS)
                    && ((_suppressableValue != null) || _suppressNulls)
                ) {
                    serializeFilteredContents(value, g, ctxt);
                } else {
                    serializeContents(value, g, ctxt);
                }
                return;
            }
        }
        g.writeStartArray(value, len);
        if (ctxt.isEnabled(SerializationFeature.APPLY_JSON_INCLUDE_FOR_CONTAINERS)
            && ((_suppressableValue != null) || _suppressNulls)
        ) {
            serializeFilteredContents(value, g, ctxt);
        } else {
            serializeContents(value, g, ctxt);
        }
        g.writeEndArray();
    }

    @Override
    public void serializeWithType(Collection<String> value, JsonGenerator g,
            SerializationContext ctxt, TypeSerializer typeSer)
        throws JacksonException
    {
        WritableTypeId typeIdDef = typeSer.writeTypePrefix(g, ctxt,
                typeSer.typeId(value, JsonToken.START_ARRAY));
        g.assignCurrentValue(value);
        serializeContents(value, g, ctxt);
        typeSer.writeTypeSuffix(g, ctxt, typeIdDef);
    }

    private final void serializeContents(Collection<String> value, JsonGenerator g,
            SerializationContext ctxt)
        throws JacksonException
    {
        serializeContentsImpl(value, g, ctxt,
            false);
    }

    private final void serializeFilteredContents(Collection<String> value, JsonGenerator g,
             SerializationContext ctxt)
            throws JacksonException
    {
        serializeContentsImpl(value, g, ctxt,
                ctxt.isEnabled(SerializationFeature.APPLY_JSON_INCLUDE_FOR_CONTAINERS));
    }

    private final void serializeContentsImpl(Collection<String> value, JsonGenerator g,
             SerializationContext ctxt, boolean filtered)
            throws JacksonException
    {
        int i = 0;

        try {
            for (String str : value) {
                if (str == null) {
                    if (filtered && _suppressNulls) {
                        ++i;
                        continue;
                    }
                    ctxt.defaultSerializeNullValue(g);
                } else {
                    // Check if this element should be suppressed (only in filtered mode)
                    if (filtered && !_shouldSerializeElement(str, null, ctxt)) {
                        ++i;
                        continue;
                    }
                    g.writeString(str);
                }
                ++i;
            }
        } catch (Exception e) {
            wrapAndThrow(ctxt, e, value, i);
        }
    }
}
