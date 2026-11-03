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
        super(List.class, String.class);
    }

    @Deprecated // since 3.1
    public IndexedStringListSerializer(IndexedStringListSerializer src,
            Boolean unwrapSingle) {
        this(src, unwrapSingle, src._suppressableValue, src._suppressNulls);
    }

    /**
     * @since 3.1
     */
    public IndexedStringListSerializer(IndexedStringListSerializer src,
           Boolean unwrapSingle, Object suppressableValue, boolean suppressNulls) {
        super(src, unwrapSingle, suppressableValue, suppressNulls);
    }

    @Deprecated // @since 3.1
    @Override
    public ValueSerializer<?> _withResolved(BeanProperty prop, Boolean unwrapSingle) {
        return new IndexedStringListSerializer(this, unwrapSingle, null, false);
    }

    @Override
    public ValueSerializer<?> _withResolved(BeanProperty prop, Boolean unwrapSingle,
           Object suppressableValue, boolean suppressNulls) {
        return new IndexedStringListSerializer(this, unwrapSingle, suppressableValue, suppressNulls);
    }

    @Override
    protected JsonNode contentSchema() { return createSchemaNode("string", true); }

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
            SerializationContext ctxt)
        throws JacksonException
    {
        final int len = value.size();
        if (len == 1) {
            if (((_unwrapSingle == null) &&
                    ctxt.isEnabled(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED))
                    || (_unwrapSingle == Boolean.TRUE)) {
                if (_needToCheckFiltering(ctxt)) {
                    serializeContentsFiltered(value, g, ctxt, 1);

                } else {
                    serializeContentsNonFiltered(value, g, ctxt, 1);
                }
                return;
            }
        }
        g.writeStartArray(value, len);
        if (_needToCheckFiltering(ctxt)) {
            serializeContentsFiltered(value, g, ctxt, len);
        } else {
            serializeContentsNonFiltered(value, g, ctxt, len);
        }
        g.writeEndArray();
    }

    @Override
    public void serializeWithType(List<String> value, JsonGenerator g,
            SerializationContext ctxt, TypeSerializer typeSer)
        throws JacksonException
    {
        WritableTypeId typeIdDef = typeSer.writeTypePrefix(g, ctxt,
                typeSer.typeId(value, JsonToken.START_ARRAY));
        g.assignCurrentValue(value);
        if (_needToCheckFiltering(ctxt)) {
            serializeContentsFiltered(value, g, ctxt, value.size());
        } else {
            serializeContentsNonFiltered(value, g, ctxt, value.size());
        }
        typeSer.writeTypeSuffix(g, ctxt, typeIdDef);
    }

    private final void serializeContentsNonFiltered(List<String> value, JsonGenerator g,
             SerializationContext ctxt, int len)
         throws JacksonException
    {
        int i = 0;
        try {
            for (; i < len; ++i) {
                String str = value.get(i);
                if (str == null) {
                    ctxt.defaultSerializeNullValue(g);
                } else {
                    g.writeString(str);
                }
            }
        } catch (Exception e) {
            wrapAndThrow(ctxt, e, value, i);
        }
    }

    private final void serializeContentsFiltered(List<String> value, JsonGenerator g,
            SerializationContext ctxt, int len)
        throws JacksonException
   {
       int i = 0;
       try {
           for (; i < len; ++i) {
               String str = value.get(i);
               if (str == null) {
                   if (_suppressNulls) {
                       continue;
                   }
                   ctxt.defaultSerializeNullValue(g);
               } else {
                   // Check if this element should be suppressed (only in filtered mode)
                   if (!_shouldSerializeElement(str, null, ctxt)) {
                       continue;
                   }
                   g.writeString(str);
               }
           }
       } catch (Exception e) {
           wrapAndThrow(ctxt, e, value, i);
       }
   }

    protected boolean _needToCheckFiltering(SerializationContext ctxt) {
        return ((_suppressableValue != null) || _suppressNulls)
                && ctxt.isEnabled(SerializationFeature.APPLY_JSON_INCLUDE_FOR_CONTAINERS);
    }
}
