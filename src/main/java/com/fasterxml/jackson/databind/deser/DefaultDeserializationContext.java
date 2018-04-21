package com.fasterxml.jackson.databind.deser;

import java.util.*;
import java.util.Map.Entry;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.annotation.ObjectIdResolver;
import com.fasterxml.jackson.annotation.ObjectIdGenerator.IdKey;

import com.fasterxml.jackson.core.FormatSchema;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TokenStreamFactory;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.deser.impl.ReadableObjectId;
import com.fasterxml.jackson.databind.deser.impl.ReadableObjectId.Referring;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * Complete {@link DeserializationContext} implementation that adds
 * extended API for {@link ObjectMapper} (and {@link ObjectReader})
 * to call, as well as implements certain parts that base class
 * has left abstract.
 */
public abstract class DefaultDeserializationContext
    extends DeserializationContext
{
    protected transient LinkedHashMap<ObjectIdGenerator.IdKey, ReadableObjectId> _objectIds;

    private List<ObjectIdResolver> _objectIdResolvers;

    /**
     * Constructor that will pass specified deserializer factory and
     * cache: cache may be null (in which case default implementation
     * will be used), factory cannot be null
     */
    protected DefaultDeserializationContext(TokenStreamFactory tsf,
            DeserializerFactory deserializerFactory, DeserializerCache cache,
            DeserializationConfig config, FormatSchema schema,
            InjectableValues values) {
        super(tsf, deserializerFactory, cache,
                config, schema, values);
    }

    public DefaultDeserializationContext assignParser(JsonParser p) {
        _parser = p;
        return this;
    }

    public JsonParser assignAndReturnParser(JsonParser p) {
        _parser = p;
        return p;
    }

    /*
    /**********************************************************************
    /* Abstract methods impls, Object Id
    /**********************************************************************
     */

    @Override
    public ReadableObjectId findObjectId(Object id, ObjectIdGenerator<?> gen, ObjectIdResolver resolverType)
    {
        // 02-Apr-2015, tatu: As per [databind#742] should allow 'null', similar to how
        //   missing id already works.
        if (id == null) {
            return null;
        }

        final ObjectIdGenerator.IdKey key = gen.key(id);

        if (_objectIds == null) {
            _objectIds = new LinkedHashMap<ObjectIdGenerator.IdKey,ReadableObjectId>();
        } else {
            ReadableObjectId entry = _objectIds.get(key);
            if (entry != null) {
                return entry;
            }
        }

        // Not seen yet, must create entry and configure resolver.
        ObjectIdResolver resolver = null;

        if (_objectIdResolvers == null) {
            _objectIdResolvers = new ArrayList<ObjectIdResolver>(8);
        } else {
            for (ObjectIdResolver res : _objectIdResolvers) {
                if (res.canUseFor(resolverType)) {
                    resolver = res;
                    break;
                }
            }
        }

        if (resolver == null) {
            resolver = resolverType.newForDeserialization(this);
            _objectIdResolvers.add(resolver);
        }

        ReadableObjectId entry = createReadableObjectId(key);
        entry.setResolver(resolver);
        _objectIds.put(key, entry);
        return entry;
    }

    /**
     * Overridable factory method to create a new instance of ReadableObjectId or its
     * subclass. It is meant to be overridden when custom ReadableObjectId is
     * needed for {@link #tryToResolveUnresolvedObjectId}.
     * Default implementation simply constructs default {@link ReadableObjectId} with
     * given <code>key</code>.
     * 
     * @param key The key to associate with the new ReadableObjectId
     * @return New ReadableObjectId instance
     */
    protected ReadableObjectId createReadableObjectId(IdKey key) {
        return new ReadableObjectId(key);
    }

    @Override
    public void checkUnresolvedObjectId() throws UnresolvedForwardReference
    {
        if (_objectIds == null) {
            return;
        }
        // 29-Dec-2014, tatu: As per [databind#299], may also just let unresolved refs be...
        if (!isEnabled(DeserializationFeature.FAIL_ON_UNRESOLVED_OBJECT_IDS)) {
            return;
        }
        UnresolvedForwardReference exception = null;
        for (Entry<IdKey,ReadableObjectId> entry : _objectIds.entrySet()) {
            ReadableObjectId roid = entry.getValue();
            if (!roid.hasReferringProperties()) {
                continue;
            }
            // as per [databind#675], allow resolution at this point
            if (tryToResolveUnresolvedObjectId(roid)) {
                continue;
            }
            if (exception == null) {
                exception = new UnresolvedForwardReference(getParser(), "Unresolved forward references for: ");
            }
            Object key = roid.getKey().key;
            for (Iterator<Referring> iterator = roid.referringProperties(); iterator.hasNext(); ) {
                Referring referring = iterator.next();
                exception.addUnresolvedId(key, referring.getBeanType(), referring.getLocation());
            }
        }
        if (exception != null) {
            throw exception;
        }
    }

    /**
     * Overridable helper method called to try to resolve otherwise unresolvable {@link ReadableObjectId};
     * and if this succeeds, return <code>true</code> to indicate problem has been resolved in
     * some way, so that caller can avoid reporting it as an error.
     *<p>
     * Default implementation simply calls {@link ReadableObjectId#tryToResolveUnresolved} and
     * returns whatever it returns.
     */
    protected boolean tryToResolveUnresolvedObjectId(ReadableObjectId roid)
    {
        return roid.tryToResolveUnresolved(this);
    }
    
    /*
    /**********************************************************************
    /* Abstract methods impls, other factory methods
    /**********************************************************************
     */
    
    @SuppressWarnings("unchecked")
    @Override
    public JsonDeserializer<Object> deserializerInstance(Annotated ann, Object deserDef)
        throws JsonMappingException
    {
        if (deserDef == null) {
            return null;
        }
        JsonDeserializer<?> deser;
        
        if (deserDef instanceof JsonDeserializer) {
            deser = (JsonDeserializer<?>) deserDef;
        } else {
            // Alas, there's no way to force return type of "either class
            // X or Y" -- need to throw an exception after the fact
            if (!(deserDef instanceof Class)) {
                throw new IllegalStateException("AnnotationIntrospector returned deserializer definition of type "+deserDef.getClass().getName()+"; expected type JsonDeserializer or Class<JsonDeserializer> instead");
            }
            Class<?> deserClass = (Class<?>)deserDef;
            // there are some known "no class" markers to consider too:
            if (deserClass == JsonDeserializer.None.class || ClassUtil.isBogusClass(deserClass)) {
                return null;
            }
            if (!JsonDeserializer.class.isAssignableFrom(deserClass)) {
                throw new IllegalStateException("AnnotationIntrospector returned Class "+deserClass.getName()+"; expected Class<JsonDeserializer>");
            }
            HandlerInstantiator hi = _config.getHandlerInstantiator();
            deser = (hi == null) ? null : hi.deserializerInstance(_config, ann, deserClass);
            if (deser == null) {
                deser = (JsonDeserializer<?>) ClassUtil.createInstance(deserClass,
                        _config.canOverrideAccessModifiers());
            }
        }
        // First: need to resolve
        deser.resolve(this);
        return (JsonDeserializer<Object>) deser;
    }

    @Override
    public final KeyDeserializer keyDeserializerInstance(Annotated ann, Object deserDef)
        throws JsonMappingException
    {
        if (deserDef == null) {
            return null;
        }

        KeyDeserializer deser;
        
        if (deserDef instanceof KeyDeserializer) {
            deser = (KeyDeserializer) deserDef;
        } else {
            if (!(deserDef instanceof Class)) {
                throw new IllegalStateException("AnnotationIntrospector returned key deserializer definition of type "
                        +deserDef.getClass().getName()
                        +"; expected type KeyDeserializer or Class<KeyDeserializer> instead");
            }
            Class<?> deserClass = (Class<?>)deserDef;
            // there are some known "no class" markers to consider too:
            if (deserClass == KeyDeserializer.None.class || ClassUtil.isBogusClass(deserClass)) {
                return null;
            }
            if (!KeyDeserializer.class.isAssignableFrom(deserClass)) {
                throw new IllegalStateException("AnnotationIntrospector returned Class "+deserClass.getName()
                        +"; expected Class<KeyDeserializer>");
            }
            HandlerInstantiator hi = _config.getHandlerInstantiator();
            deser = (hi == null) ? null : hi.keyDeserializerInstance(_config, ann, deserClass);
            if (deser == null) {
                deser = (KeyDeserializer) ClassUtil.createInstance(deserClass,
                        _config.canOverrideAccessModifiers());
            }
        }
        // First: need to resolve
        deser.resolve(this);
        return deser;
    }

    /*
    /**********************************************************************
    /* And then the concrete implementation class
    /**********************************************************************
     */

    /**
     * Actual full concrete implementation
     */
    public final static class Impl extends DefaultDeserializationContext
    {
        public Impl(TokenStreamFactory tsf,
                DeserializerFactory deserializerFactory, DeserializerCache cache,
                DeserializationConfig config, FormatSchema schema,
                InjectableValues values) {
            super(tsf, deserializerFactory, cache,
                    config, schema, values);
        }
    }
}
