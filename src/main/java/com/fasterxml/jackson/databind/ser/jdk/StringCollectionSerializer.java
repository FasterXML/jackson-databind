package com.fasterxml.jackson.databind.ser.jdk;

import java.util.*;

import tools.jackson.core.*;
import tools.jackson.core.type.WritableTypeId;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonArrayFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatTypes;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

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
        super(Collection.class);
    }

    protected StringCollectionSerializer(StringCollectionSerializer src,
            Boolean unwrapSingle)
    {
        super(src, unwrapSingle);
    }        

    @Override
    public ValueSerializer<?> _withResolved(BeanProperty prop, Boolean unwrapSingle) {
        return new StringCollectionSerializer(this, unwrapSingle);
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
            SerializerProvider provider) throws JacksonException
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
            SerializerProvider ctxt, TypeSerializer typeSer)
        throws JacksonException
    {
        WritableTypeId typeIdDef = typeSer.writeTypePrefix(g, ctxt,
                typeSer.typeId(value, JsonToken.START_ARRAY));
        g.assignCurrentValue(value);
        serializeContents(value, g, ctxt);
        typeSer.writeTypeSuffix(g, ctxt, typeIdDef);
    }

    private final void serializeContents(Collection<String> value, JsonGenerator g,
            SerializerProvider provider)
        throws JacksonException
    {
        int i = 0;

        try {
            for (String str : value) {
                if (str == null) {
                    provider.defaultSerializeNullValue(g);
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
