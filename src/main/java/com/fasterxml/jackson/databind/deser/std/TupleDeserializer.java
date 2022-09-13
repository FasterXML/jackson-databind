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
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.type.TupleType;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * TupleDeserializer
 */
@JacksonStdImpl
public class TupleDeserializer extends CollectionDeserializer {
    private static final long serialVersionUID = 1L;

    private TupleType tupleType;

    private List<JsonDeserializer<Object>> valueDeserList;

    private List<NullValueProvider> nullerList;

    public TupleDeserializer(CollectionDeserializer deser, TupleType tupleType)
    {
        // disable unwrapSingle.
        super(deser._valueType,
                deser._valueDeserializer, deser._valueTypeDeserializer,
                deser._valueInstantiator, deser._delegateDeserializer,
                deser._nullProvider, false);
        this.tupleType = tupleType;
        this.valueDeserList = new ArrayList<>();
        this.nullerList = new ArrayList<>();
    }

    protected TupleDeserializer (JavaType containerType,
            JsonDeserializer<Object> valueDeser, TypeDeserializer valueTypeDeser,
            ValueInstantiator valueInstantiator,
            JsonDeserializer<Object> delegateDeser,
            NullValueProvider nuller,
            TupleType tupleType,
            List<JsonDeserializer<Object>> valueDeserList, List<NullValueProvider> nullerList)
    {
        // disable unwrapSingle.
        super(containerType, valueDeser, valueTypeDeser, valueInstantiator, delegateDeser,
                nuller, false);
        this.tupleType = tupleType;
        this.valueDeserList = valueDeserList;
        this.nullerList = nullerList;
    }

    @Override
    protected Collection<Object> _deserializeFromArray(JsonParser p, DeserializationContext ctxt,
            Collection<Object> result)
        throws IOException
    {
        p.setCurrentValue(result);

        JsonToken t;
        int idx = 0;
        while ((t = p.nextToken()) != JsonToken.END_ARRAY) {
            try {
                Object value;
                if (t == JsonToken.VALUE_NULL) {
                    value = nullerList.get(idx++).getNullValue(ctxt);
                } else {
                    value = valueDeserList.get(idx++).deserialize(p, ctxt);
                }
                result.add(value);
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
        for (JavaType type : tupleType.getBindings().getTypeParameters()) {
            JsonDeserializer<Object> valueDeser = ctxt.findContextualValueDeserializer(type, null);
            NullValueProvider nuller = findContentNullProvider(ctxt, null, valueDeser);
            this.valueDeserList.add(valueDeser);
            this.nullerList.add(nuller);
        }
        return super.createContextual(ctxt, property);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected TupleDeserializer withResolved(JsonDeserializer<?> dd,
            JsonDeserializer<?> vd, TypeDeserializer vtd,
            NullValueProvider nuller, Boolean unwrapSingle)
    {
        return new TupleDeserializer(_containerType,
                (JsonDeserializer<Object>) vd, vtd,
                _valueInstantiator, (JsonDeserializer<Object>) dd,
                nuller,
                this.tupleType,
                this.valueDeserList, this.nullerList);
    }
}
