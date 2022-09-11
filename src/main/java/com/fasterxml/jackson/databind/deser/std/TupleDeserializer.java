package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.util.ArrayList;
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
import com.fasterxml.jackson.databind.type.TupleType;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * TupleDeserializer
 */
@JacksonStdImpl
public class TupleDeserializer extends CollectionDeserializer {
    private static final long serialVersionUID = 1L;

    private CollectionDeserializer deser;

    private TupleType tupleType;
    
    private List<JsonDeserializer<Object>> valueDesList = new ArrayList<>();
    
    private List<NullValueProvider> nullerList = new ArrayList<>();

    public TupleDeserializer(CollectionDeserializer deser, TupleType tupleType) {
        // disable unwrapSingle.
        super(deser._valueType,
                deser._valueDeserializer, deser._valueTypeDeserializer,
                deser._valueInstantiator, deser._delegateDeserializer,
                deser._nullProvider, false);
        this.deser = deser;
        this.tupleType = tupleType;
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

        JsonToken t;
        int idx = -1;
        while ((t = p.nextToken()) != JsonToken.END_ARRAY) {
            try {
                idx++;
                Object value;
                if (t == JsonToken.VALUE_NULL) {
                    value = nullerList.get(idx).getNullValue(ctxt);
                } else {
                    value = valueDesList.get(idx).deserialize(p, ctxt);
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
        for (JavaType type : tupleType.getBindings().getTypeParameters()) {
            JsonDeserializer<Object> valueDes = ctxt.findContextualValueDeserializer(type, property);
            NullValueProvider nuller = findContentNullProvider(ctxt, property, valueDes);
            valueDesList.add(valueDes);
            nullerList.add(nuller);
        }
        return this;
    }
}
