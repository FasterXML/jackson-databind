package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.NullValueProvider;
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

     public ArrayBlockingQueueDeserializer(JavaType containerType,
            JsonDeserializer<Object> valueDeser, TypeDeserializer valueTypeDeser,
            ValueInstantiator valueInstantiator)
    {
        super(containerType, valueDeser, valueTypeDeser, valueInstantiator);
    }

    /**
     * Constructor used when creating contextualized instances.
     */
     protected ArrayBlockingQueueDeserializer(JavaType containerType,
            JsonDeserializer<Object> valueDeser, TypeDeserializer valueTypeDeser,
            ValueInstantiator valueInstantiator,
            JsonDeserializer<Object> delegateDeser,
            NullValueProvider nuller, Boolean unwrapSingle)
    {
        super(containerType, valueDeser, valueTypeDeser, valueInstantiator, delegateDeser,
                nuller, unwrapSingle);
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
            JsonDeserializer<?> vd, TypeDeserializer vtd,
            NullValueProvider nuller, Boolean unwrapSingle)
    {
        return new ArrayBlockingQueueDeserializer(_containerType,
                (JsonDeserializer<Object>) vd, vtd,
                _valueInstantiator, (JsonDeserializer<Object>) dd,
                nuller, unwrapSingle);
    }

    /*
    /**********************************************************
    /* JsonDeserializer API
    /**********************************************************
     */

    @Override
    protected Collection<Object> createDefaultInstance(DeserializationContext ctxt)
        throws IOException
    {
        // 07-Nov-2016, tatu: Important: cannot create using default ctor (one
        //    does not exist); and also need to know exact size. Hence, return
        //    null from here
        return null;
    }

    // NOTE: implementation changed between 2.11 and 2.12
    @Override
    protected Collection<Object> _deserializeFromArray(JsonParser p, DeserializationContext ctxt,
            Collection<Object> result0)
        throws IOException
    {
        if (result0 == null) { // usual case
            result0 = new ArrayList<>();
        }
        result0 = super._deserializeFromArray(p, ctxt, result0);
        if (result0.isEmpty()) {
            return new ArrayBlockingQueue<>(1, false);
        }
        return new ArrayBlockingQueue<>(result0.size(), false, result0);
    }

    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt, TypeDeserializer typeDeserializer) throws IOException {
        // In future could check current token... for now this should be enough:
        return typeDeserializer.deserializeTypedFromArray(p, ctxt);
    }
}
