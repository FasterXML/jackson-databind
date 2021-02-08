package com.fasterxml.jackson.databind.deser.jdk;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

import com.fasterxml.jackson.core.JacksonException;
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
    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

     public ArrayBlockingQueueDeserializer(JavaType containerType,
            ValueDeserializer<Object> valueDeser, TypeDeserializer valueTypeDeser,
            ValueInstantiator valueInstantiator)
    {
        super(containerType, valueDeser, valueTypeDeser, valueInstantiator);
    }

    /**
     * Constructor used when creating contextualized instances.
     */
     protected ArrayBlockingQueueDeserializer(JavaType containerType,
            ValueDeserializer<Object> valueDeser, TypeDeserializer valueTypeDeser,
            ValueInstantiator valueInstantiator,
            ValueDeserializer<Object> delegateDeser,
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
    protected ArrayBlockingQueueDeserializer withResolved(ValueDeserializer<?> dd,
            ValueDeserializer<?> vd, TypeDeserializer vtd,
            NullValueProvider nuller, Boolean unwrapSingle)
    {
        return new ArrayBlockingQueueDeserializer(_containerType,
                (ValueDeserializer<Object>) vd, vtd,
                _valueInstantiator, (ValueDeserializer<Object>) dd,
                nuller, unwrapSingle);
    }

    /*
    /**********************************************************************
    /* ValueDeserializer API
    /**********************************************************************
     */

    @Override
    protected Collection<Object> createDefaultInstance(DeserializationContext ctxt)
        throws JacksonException
    {
        // 07-Nov-2016, tatu: Important: cannot create using default ctor (one
        //    does not exist); and also need to know exact size. Hence, return
        //    null from here
        return null;
    }

    @Override
    protected Collection<Object> _deserializeFromArray(JsonParser p, DeserializationContext ctxt,
            Collection<Object> result0)
        throws JacksonException
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
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt, TypeDeserializer typeDeserializer)
        throws JacksonException
    {
        // In future could check current token... for now this should be enough:
        return typeDeserializer.deserializeTypedFromArray(p, ctxt);
    }
}
