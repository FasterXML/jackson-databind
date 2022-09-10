package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.deser.NullValueProvider;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.type.TupleType;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * TupleDeserializer
 */
@JacksonStdImpl
public class TupleDeserializer extends CollectionDeserializer {
    private static final long serialVersionUID = 1L;

    private CollectionDeserializer deser;

    private List<JavaType> elementTypeList;

    public TupleDeserializer(CollectionDeserializer deser, TupleType tupleType) {
        super(deser);
        this.deser = deser;
        this.elementTypeList = tupleType.getBindings().getTypeParameters();
    }

    public CollectionDeserializer getCollectionDeserializer() {
        return deser;
    }

    @Override
    protected Collection<Object> _deserializeFromArray(JsonParser p, DeserializationContext ctxt,
            Collection<Object> result)
        throws IOException
    {
        // [databind#631]: Assign current value, to be accessible by custom serializers
        p.setCurrentValue(result);

        JsonDeserializer<Object> valueDes = null;
        final TypeDeserializer typeDeser = _valueTypeDeserializer;
        JsonToken t;
        int idx = -1;
        while ((t = p.nextToken()) != JsonToken.END_ARRAY) {
            try {
                idx++;
                valueDes = ctxt.findContextualValueDeserializer(elementTypeList.get(idx), null);
                Object value;
                if (t == JsonToken.VALUE_NULL) {
                    NullValueProvider nuller = findContentNullProvider(ctxt, null, valueDes);
                    value = nuller.getNullValue(ctxt);
                } else {
                    if (typeDeser == null) {
                        value = valueDes.deserialize(p, ctxt);
                    } else {
                        value = valueDes.deserializeWithType(p, ctxt, typeDeser);
                    }
                }
                result.add(value);

                /* 17-Dec-2017, tatu: should not occur at this level...
            } catch (UnresolvedForwardReference reference) {
                throw JsonMappingException
                    .from(p, "Unresolved forward reference but no identity info", reference);
                */
            } catch (Exception e) {
                boolean wrap = (ctxt == null) || ctxt.isEnabled(DeserializationFeature.WRAP_EXCEPTIONS);
                if (!wrap) {
                    ClassUtil.throwIfRTE(e);
                }
                throw JsonMappingException.wrapWithPath(e, result, result.size());
            }
        }
        return result;
    }

    @Override
    public CollectionDeserializer createContextual(DeserializationContext ctxt,
            BeanProperty property) throws JsonMappingException
    {
        this.deser = deser.createContextual(ctxt, property);
        return this;
    }
}
