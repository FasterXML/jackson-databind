package com.fasterxml.jackson.databind.deser.impl;

import java.io.IOException;
import java.util.HashSet;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.*;
import com.fasterxml.jackson.databind.util.NameTransformer;

/**
 * Variant of {@link BeanDeserializer} used for handling deserialization
 * of POJOs when serialized as JSON Arrays, instead of JSON Objects.
 * 
 * @since 2.1
 */
public class BeanAsArrayDeserializer
    extends BeanDeserializerBase
{
    /**
     * Deserializer we delegate operations that we can not handle.
     */
    protected final BeanDeserializerBase _delegate;

    /**
     * Properties in order expected to be found in JSON array.
     */
    protected final SettableBeanProperty[] _orderedProperties;
    
    /*
    /**********************************************************
    /* Life-cycle, construction, initialization
    /**********************************************************
     */

    /**
     * Main constructor used both for creating new instances (by
     * {@link BeanDeserializer#asArrayDeserializer}) and for
     * creating copies with different delegate.
     */
    public BeanAsArrayDeserializer(BeanDeserializerBase delegate,
            SettableBeanProperty[] ordered)
    {
        super(delegate);
        _delegate = delegate;
        _orderedProperties = ordered;
    }
    
    @Override
    public JsonDeserializer<Object> unwrappingDeserializer(NameTransformer unwrapper)
    {
        /* We can't do much about this; could either replace _delegate
         * with unwrapping instance, or just replace this one. Latter seems
         * more sensible.
         */
        return _delegate.unwrappingDeserializer(unwrapper);
    }

    @Override
    public BeanAsArrayDeserializer withObjectIdReader(ObjectIdReader oir) {
        return new BeanAsArrayDeserializer(_delegate.withObjectIdReader(oir),
                _orderedProperties);
    }

    @Override
    public BeanAsArrayDeserializer withIgnorableProperties(HashSet<String> ignorableProps) {
        return new BeanAsArrayDeserializer(_delegate.withIgnorableProperties(ignorableProps),
                _orderedProperties);
    }

    @Override
    protected BeanDeserializerBase asArrayDeserializer() {
        return this;
    }
    
    /*
    /**********************************************************
    /* JsonDeserializer implementation
    /**********************************************************
     */

    @Override
    public Object deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
    {
        // common case first:
        if (jp.getCurrentToken() != JsonToken.START_ARRAY) {
            return _delegate.deserialize(jp, ctxt);
        }
        final Object bean = _valueInstantiator.createUsingDefault(ctxt);
        final SettableBeanProperty[] props = _orderedProperties;
        int i = 0;
        final int propCount = props.length;
        while (true) {
            if (jp.nextToken() == JsonToken.END_ARRAY) {
                return bean;
            }
            if (i == propCount) {
                break;
            }
            SettableBeanProperty prop = props[i];
            if (prop != null) { // normal case
                try {
                    prop.deserializeAndSet(jp, ctxt, bean);
                } catch (Exception e) {
                    wrapAndThrow(e, bean, prop.getName(), ctxt);
                }
            } else { // just skip?
                jp.skipChildren();
            }
            ++i;
        }
        
        // Ok; extra fields? Let's fail, unless ignoring extra props is fine
        if (!_ignoreAllUnknown) {
            throw ctxt.mappingException("Unexpected JSON values; expected at most "+propCount+" properties (in JSON Array)");
        }
        // otherwise, skip until end
        while (jp.nextToken() != JsonToken.END_ARRAY) {
            jp.skipChildren();
        }
        return bean;
    }
}
