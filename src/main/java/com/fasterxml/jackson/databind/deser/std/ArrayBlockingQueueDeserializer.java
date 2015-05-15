package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;

/**
 * We need a custom deserializer both because {@link ArrayBlockingQueue} has no
 * default constructor AND because it has size limit used for constructing
 * underlying storage automatically.
 */
public class ArrayBlockingQueueDeserializer
    extends CollectionDeserializer
{
    private static final long serialVersionUID = 1;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    /**
     * Constructor used when creating contextualized instances.
     */
    public ArrayBlockingQueueDeserializer(JavaType collectionType,
            JsonDeserializer<Object> valueDeser, TypeDeserializer valueTypeDeser,
            ValueInstantiator valueInstantiator,
            JsonDeserializer<Object> delegateDeser)
    {
        super(collectionType, valueDeser, valueTypeDeser, valueInstantiator, delegateDeser);
    }

    /**
     * Copy-constructor that can be used by sub-classes to allow
     * copy-on-write styling copying of settings of an existing instance.
     */
    protected ArrayBlockingQueueDeserializer(ArrayBlockingQueueDeserializer src) {
        super(src);
    }

    /**
     * Fluent-factory method call to construct contextual instance.
     */
    @Override
    @SuppressWarnings("unchecked")
    protected ArrayBlockingQueueDeserializer withResolved(JsonDeserializer<?> dd,
            JsonDeserializer<?> vd, TypeDeserializer vtd)
    {
        if ((dd == _delegateDeserializer) && (vd == _valueDeserializer) && (vtd == _valueTypeDeserializer)) {
            return this;
        }
        return new ArrayBlockingQueueDeserializer(_collectionType,
                (JsonDeserializer<Object>) vd, vtd,
                _valueInstantiator, (JsonDeserializer<Object>) dd);
                
    }

    /*
    /**********************************************************
    /* JsonDeserializer API
    /**********************************************************
     */
    
    @SuppressWarnings("unchecked")
    @Override
    public Collection<Object> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException
    {
        if (_delegateDeserializer != null) {
            return (Collection<Object>) _valueInstantiator.createUsingDelegate(ctxt,
                    _delegateDeserializer.deserialize(jp, ctxt));
        }
        if (jp.getCurrentToken() == JsonToken.VALUE_STRING) {
            String str = jp.getText();
            if (str.length() == 0) {
                return (Collection<Object>) _valueInstantiator.createFromString(ctxt, str);
            }
        }
        return deserialize(jp, ctxt, null);
    }

    @Override
    public Collection<Object> deserialize(JsonParser jp, DeserializationContext ctxt, Collection<Object> result0) throws IOException
    {
        // Ok: must point to START_ARRAY (or equivalent)
        if (!jp.isExpectedStartArrayToken()) {
            return handleNonArray(jp, ctxt, new ArrayBlockingQueue<Object>(1));
        }
        ArrayList<Object> tmp = new ArrayList<Object>();
        
        JsonDeserializer<Object> valueDes = _valueDeserializer;
        JsonToken t;
        final TypeDeserializer typeDeser = _valueTypeDeserializer;

        try {
            while ((t = jp.nextToken()) != JsonToken.END_ARRAY) {
                Object value;
                
                if (t == JsonToken.VALUE_NULL) {
                    value = valueDes.getNullValue(ctxt);
                } else if (typeDeser == null) {
                    value = valueDes.deserialize(jp, ctxt);
                } else {
                    value = valueDes.deserializeWithType(jp, ctxt, typeDeser);
                }
                tmp.add(value);
            }
        } catch (Exception e) {
            throw JsonMappingException.wrapWithPath(e, tmp, tmp.size());
        }
        if (result0 != null) {
            result0.addAll(tmp);
            return result0;
        }
        return new ArrayBlockingQueue<Object>(tmp.size(), false, tmp);
    }

    @Override
    public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt, TypeDeserializer typeDeserializer) throws IOException {
        // In future could check current token... for now this should be enough:
        return typeDeserializer.deserializeTypedFromArray(jp, ctxt);
    }
}
