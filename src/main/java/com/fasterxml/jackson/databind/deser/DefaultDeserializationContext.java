package com.fasterxml.jackson.databind.deser;

import java.util.LinkedHashMap;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.impl.ReadableObjectId;

/**
 * Default {@link DeserializationContext} implementation that adds
 * extended API for {@link ObjectMapper} (and {@link ObjectReader})
 * to call, as well as implements certain parts that base class
 * has left abstract.
 */
public abstract class DefaultDeserializationContext
    extends DeserializationContext
{
    protected LinkedHashMap<ObjectIdGenerator.IdKey, ReadableObjectId> _objectIds;
    
    /**
     * Constructor that will pass specified deserializer factory and
     * cache: cache may be null (in which case default implementation
     * will be used), factory can not be null
     */
    protected DefaultDeserializationContext(DeserializerFactory df, DeserializerCache cache) {
        super(df, cache);
    }
    
    protected DefaultDeserializationContext(DefaultDeserializationContext src,
            DeserializationConfig config, JsonParser jp, InjectableValues values) {
        super(src, config, jp, values);
    }

    protected DefaultDeserializationContext(DefaultDeserializationContext src,
            DeserializerFactory factory) {
        super(src, factory);
    }

    /*
    /**********************************************************
    /* Abstract methods impls
    /**********************************************************
     */

    @Override
    public ReadableObjectId findObjectId(Object id,
            ObjectIdGenerator<?> generator)
    {
        final ObjectIdGenerator.IdKey key = generator.key(id);
        if (_objectIds == null) {
            _objectIds = new LinkedHashMap<ObjectIdGenerator.IdKey, ReadableObjectId>();
        } else {
            ReadableObjectId entry = _objectIds.get(key);
            if (entry != null) {
                return entry;
            }
        }
        ReadableObjectId entry = new ReadableObjectId(id);
        _objectIds.put(key, entry);
        return entry;
    }
    
    /*
    /**********************************************************
    /* Extended API
    /**********************************************************
     */

    /**
     * Fluent factory method used for constructing a blueprint instance
     * with different factory
     */
    public abstract DefaultDeserializationContext with(DeserializerFactory factory);
    
    /**
     * Method called to create actual usable per-deserialization
     * context instance.
     */
    public abstract DefaultDeserializationContext createInstance(
            DeserializationConfig config, JsonParser jp, InjectableValues values);
    
    /*
    /**********************************************************
    /* And then the concrete implementation class
    /**********************************************************
     */

    public final static class Impl extends DefaultDeserializationContext
    {
        /**
         * Default constructor for a blueprint object, which will use the standard
         * {@link DeserializerCache}, given factory.
         */
        public Impl(DeserializerFactory df) {
            super(df, null);
        }

        protected Impl(Impl src,
                DeserializationConfig config, JsonParser jp, InjectableValues values) {
            super(src, config, jp, values);
        }

        protected Impl(Impl src, DeserializerFactory factory) {
            super(src, factory);
        }
        
        @Override
        public DefaultDeserializationContext createInstance(DeserializationConfig config,
                JsonParser jp, InjectableValues values) {
            return new Impl(this, config, jp, values);
        }

        @Override
        public DefaultDeserializationContext with(DeserializerFactory factory) {
            return new Impl(this, factory);
        }        
    }
}
