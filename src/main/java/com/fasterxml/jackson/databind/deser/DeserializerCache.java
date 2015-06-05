package com.fasterxml.jackson.databind.deser;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.std.StdDelegatingDeserializer;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.type.*;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.Converter;

/**
 * Class that defines caching layer between callers (like
 * {@link ObjectMapper},
 * {@link com.fasterxml.jackson.databind.DeserializationContext})
 * and classes that construct deserializers
 * ({@link com.fasterxml.jackson.databind.deser.DeserializerFactory}).
 */
public final class DeserializerCache
    implements java.io.Serializable // since 2.1 -- needs to be careful tho
{
    private static final long serialVersionUID = 1L;

    /*
    /**********************************************************
    /* Caching
    /**********************************************************
     */

    /**
     * We will also cache some dynamically constructed deserializers;
     * specifically, ones that are expensive to construct.
     * This currently means bean and Enum deserializers; starting with
     * 2.5, container deserializers will also be cached.
     *<p>
     * Given that we don't expect much concurrency for additions
     * (should very quickly converge to zero after startup), let's
     * define a relatively low concurrency setting.
     */
    final protected ConcurrentHashMap<JavaType, JsonDeserializer<Object>> _cachedDeserializers
        = new ConcurrentHashMap<JavaType, JsonDeserializer<Object>>(64, 0.75f, 4);

    /**
     * During deserializer construction process we may need to keep track of partially
     * completed deserializers, to resolve cyclic dependencies. This is the
     * map used for storing deserializers before they are fully complete.
     */
    final protected HashMap<JavaType, JsonDeserializer<Object>> _incompleteDeserializers
        = new HashMap<JavaType, JsonDeserializer<Object>>(8);

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    public DeserializerCache() { }

    /*
    /**********************************************************
    /* JDK serialization handling
    /**********************************************************
     */

    Object writeReplace() {
        // instead of making this transient, just clear it:
        _incompleteDeserializers.clear();
        // TODO: clear out "cheap" cached deserializers?
        return this;
    }
    
    /*
    /**********************************************************
    /* Access to caching aspects
    /**********************************************************
     */

    /**
     * Method that can be used to determine how many deserializers this
     * provider is caching currently 
     * (if it does caching: default implementation does)
     * Exact count depends on what kind of deserializers get cached;
     * default implementation caches only dynamically constructed deserializers,
     * but not eagerly constructed standard deserializers (which is different
     * from how serializer provider works).
     *<p>
     * The main use case for this method is to allow conditional flushing of
     * deserializer cache, if certain number of entries is reached.
     */
    public int cachedDeserializersCount() {
        return _cachedDeserializers.size();
    }

    /**
     * Method that will drop all dynamically constructed deserializers (ones that
     * are counted as result value for {@link #cachedDeserializersCount}).
     * This can be used to remove memory usage (in case some deserializers are
     * only used once or so), or to force re-construction of deserializers after
     * configuration changes for mapper than owns the provider.
     */
    public void flushCachedDeserializers() {
        _cachedDeserializers.clear();       
    }

    /*
    /**********************************************************
    /* General deserializer locating method
    /**********************************************************
     */

    /**
     * Method called to get hold of a deserializer for a value of given type;
     * or if no such deserializer can be found, a default handler (which
     * may do a best-effort generic serialization or just simply
     * throw an exception when invoked).
     *<p>
     * Note: this method is only called for value types; not for keys.
     * Key deserializers can be accessed using {@link #findKeyDeserializer}.
     *<p>
     * Note also that deserializer returned is guaranteed to be resolved
     * (if it is of type {@link ResolvableDeserializer}), but
     * not contextualized (wrt {@link ContextualDeserializer}): caller
     * has to handle latter if necessary.
     *
     * @param ctxt Deserialization context
     * @param propertyType Declared type of the value to deserializer (obtained using
     *   'setter' method signature and/or type annotations
     *
     * @throws JsonMappingException if there are fatal problems with
     *   accessing suitable deserializer; including that of not
     *   finding any serializer
     */
    public JsonDeserializer<Object> findValueDeserializer(DeserializationContext ctxt,
            DeserializerFactory factory, JavaType propertyType)
        throws JsonMappingException
    {
        JsonDeserializer<Object> deser = _findCachedDeserializer(propertyType);
        if (deser == null) {
            // If not, need to request factory to construct (or recycle)
            deser = _createAndCacheValueDeserializer(ctxt, factory, propertyType);
            if (deser == null) {
                /* Should we let caller handle it? Let's have a helper method
                 * decide it; can throw an exception, or return a valid
                 * deserializer
                 */
                deser = _handleUnknownValueDeserializer(propertyType);
            }
        }
        return deser;
    }

    /**
     * Method called to get hold of a deserializer to use for deserializing
     * keys for {@link java.util.Map}.
     *
     * @throws JsonMappingException if there are fatal problems with
     *   accessing suitable key deserializer; including that of not
     *   finding any serializer
     */
    public KeyDeserializer findKeyDeserializer(DeserializationContext ctxt,
            DeserializerFactory factory, JavaType type)
        throws JsonMappingException
    {
        KeyDeserializer kd = factory.createKeyDeserializer(ctxt, type);
        if (kd == null) { // if none found, need to use a placeholder that'll fail
            return _handleUnknownKeyDeserializer(type);
        }
        // First: need to resolve?
        if (kd instanceof ResolvableDeserializer) {
            ((ResolvableDeserializer) kd).resolve(ctxt);
        }
        return kd;
    }

    /**
     * Method called to find out whether provider would be able to find
     * a deserializer for given type, using a root reference (i.e. not
     * through fields or membership in an array or collection)
     */
    public boolean hasValueDeserializerFor(DeserializationContext ctxt,
            DeserializerFactory factory, JavaType type)
        throws JsonMappingException
    {
        /* Note: mostly copied from findValueDeserializer, except for
         * handling of unknown types
         */
        JsonDeserializer<Object> deser = _findCachedDeserializer(type);
        if (deser == null) {
            deser = _createAndCacheValueDeserializer(ctxt, factory, type);
        }
        return (deser != null);
    }

    /*
    /**********************************************************
    /* Helper methods that handle cache lookups
    /**********************************************************
     */

    protected JsonDeserializer<Object> _findCachedDeserializer(JavaType type)
    {
        if (type == null) {
            throw new IllegalArgumentException("Null JavaType passed");
        }
        if (_hasCustomValueHandler(type)) {
            return null;
        }
        return _cachedDeserializers.get(type);
    }

    /**
     * Method that will try to create a deserializer for given type,
     * and resolve and cache it if necessary
     * 
     * @param ctxt Currently active deserialization context
     * @param type Type of property to deserialize
     */
    protected JsonDeserializer<Object> _createAndCacheValueDeserializer(DeserializationContext ctxt,
            DeserializerFactory factory, JavaType type)
        throws JsonMappingException
    {
        /* Only one thread to construct deserializers at any given point in time;
         * limitations necessary to ensure that only completely initialized ones
         * are visible and used.
         */
        synchronized (_incompleteDeserializers) {
            // Ok, then: could it be that due to a race condition, deserializer can now be found?
            JsonDeserializer<Object> deser = _findCachedDeserializer(type);
            if (deser != null) {
                return deser;
            }
            int count = _incompleteDeserializers.size();
            // Or perhaps being resolved right now?
            if (count > 0) {
                deser = _incompleteDeserializers.get(type);
                if (deser != null) {
                    return deser;
                }
            }
            // Nope: need to create and possibly cache
            try {
                return _createAndCache2(ctxt, factory, type);
            } finally {
                // also: any deserializers that have been created are complete by now
                if (count == 0 && _incompleteDeserializers.size() > 0) {
                    _incompleteDeserializers.clear();
                }
            }
        }
    }

    /**
     * Method that handles actual construction (via factory) and caching (both
     * intermediate and eventual)
     */
    protected JsonDeserializer<Object> _createAndCache2(DeserializationContext ctxt,
            DeserializerFactory factory, JavaType type)
        throws JsonMappingException
    {
        JsonDeserializer<Object> deser;
        try {
            deser = _createDeserializer(ctxt, factory, type);
        } catch (IllegalArgumentException iae) {
            /* We better only expose checked exceptions, since those
             * are what caller is expected to handle
             */
            throw new JsonMappingException(iae.getMessage(), null, iae);
        }
        if (deser == null) {
            return null;
        }
        /* cache resulting deserializer? always true for "plain" BeanDeserializer
         * (but can be re-defined for sub-classes by using @JsonCachable!)
         */
        // 08-Jun-2010, tatu: Related to [JACKSON-296], need to avoid caching MapSerializers... so:
        boolean isResolvable = (deser instanceof ResolvableDeserializer);
        // 27-Mar-2015, tatu: As per [databind#735], avoid caching types with custom value desers
        boolean addToCache = !_hasCustomValueHandler(type) && deser.isCachable();

        /* we will temporarily hold on to all created deserializers (to
         * handle cyclic references, and possibly reuse non-cached
         * deserializers (list, map))
         */
        /* 07-Jun-2010, tatu: Danger: [JACKSON-296] was caused by accidental
         *   resolution of a reference -- couple of ways to prevent this;
         *   either not add Lists or Maps, or clear references eagerly.
         *   Let's actually do both; since both seem reasonable.
         */
        /* Need to resolve? Mostly done for bean deserializers; required for
         * resolving cyclic references.
         */
        if (isResolvable) {
            _incompleteDeserializers.put(type, deser);
            ((ResolvableDeserializer)deser).resolve(ctxt);
            _incompleteDeserializers.remove(type);
        }
        if (addToCache) {
            _cachedDeserializers.put(type, deser);
        }
        return deser;
    }

    /*
    /**********************************************************
    /* Helper methods for actual construction of deserializers
    /**********************************************************
     */
    
    /**
     * Method that does the heavy lifting of checking for per-type annotations,
     * find out full type, and figure out which actual factory method
     * to call.
     */
    @SuppressWarnings("unchecked")
    protected JsonDeserializer<Object> _createDeserializer(DeserializationContext ctxt,
            DeserializerFactory factory, JavaType type)
        throws JsonMappingException
    {
        final DeserializationConfig config = ctxt.getConfig();

        // First things first: do we need to use abstract type mapping?
        if (type.isAbstract() || type.isMapLikeType() || type.isCollectionLikeType()) {
            type = factory.mapAbstractType(config, type);
        }
        BeanDescription beanDesc = config.introspect(type);
        // Then: does type define explicit deserializer to use, with annotation(s)?
        JsonDeserializer<Object> deser = findDeserializerFromAnnotation(ctxt,
                beanDesc.getClassInfo());
        if (deser != null) {
            return deser;
        }

        // If not, may have further type-modification annotations to check:
        JavaType newType = modifyTypeByAnnotation(ctxt, beanDesc.getClassInfo(), type);
        if (newType != type) {
            type = newType;
            beanDesc = config.introspect(newType);
        }

        // We may also have a Builder type to consider...
        Class<?> builder = beanDesc.findPOJOBuilder();
        if (builder != null) {
            return (JsonDeserializer<Object>) factory.createBuilderBasedDeserializer(
            		ctxt, type, beanDesc, builder);
        }

        // Or perhaps a Converter?
        Converter<Object,Object> conv = beanDesc.findDeserializationConverter();
        if (conv == null) { // nope, just construct in normal way
            return (JsonDeserializer<Object>) _createDeserializer2(ctxt, factory, type, beanDesc);
        }
        // otherwise need to do bit of introspection
        JavaType delegateType = conv.getInputType(ctxt.getTypeFactory());
        // One more twist, as per [Issue#288]; probably need to get new BeanDesc
        if (!delegateType.hasRawClass(type.getRawClass())) {
            beanDesc = config.introspect(delegateType);
        }
        return new StdDelegatingDeserializer<Object>(conv, delegateType,
                _createDeserializer2(ctxt, factory, delegateType, beanDesc));
    }

    protected JsonDeserializer<?> _createDeserializer2(DeserializationContext ctxt,
            DeserializerFactory factory, JavaType type, BeanDescription beanDesc)
        throws JsonMappingException
    {
        final DeserializationConfig config = ctxt.getConfig();
        // If not, let's see which factory method to use:
        if (type.isEnumType()) {
            return factory.createEnumDeserializer(ctxt, type, beanDesc);
        }
        if (type.isContainerType()) {
            if (type.isArrayType()) {
                return factory.createArrayDeserializer(ctxt, (ArrayType) type, beanDesc);
            }
            if (type.isMapLikeType()) {
                MapLikeType mlt = (MapLikeType) type;
                if (mlt.isTrueMapType()) {
                    return factory.createMapDeserializer(ctxt,(MapType) mlt, beanDesc);
                }
                return factory.createMapLikeDeserializer(ctxt, mlt, beanDesc);
            }
            if (type.isCollectionLikeType()) {
                /* 03-Aug-2012, tatu: As per [Issue#40], one exception is if shape
                 *   is to be Shape.OBJECT. Ideally we'd determine it bit later on
                 *   (to allow custom handler checks), but that won't work for other
                 *   reasons. So do it here.
                 */
                JsonFormat.Value format = beanDesc.findExpectedFormat(null);
                if (format == null || format.getShape() != JsonFormat.Shape.OBJECT) {
                    CollectionLikeType clt = (CollectionLikeType) type;
                    if (clt.isTrueCollectionType()) {
                        return factory.createCollectionDeserializer(ctxt, (CollectionType) clt, beanDesc);
                    }
                    return factory.createCollectionLikeDeserializer(ctxt, clt, beanDesc);
                }
            }
        }
        if (JsonNode.class.isAssignableFrom(type.getRawClass())) {
            return factory.createTreeDeserializer(config, type, beanDesc);
        }
        return factory.createBeanDeserializer(ctxt, type, beanDesc);
    }

    /**
     * Helper method called to check if a class or method
     * has annotation that tells which class to use for deserialization.
     * Returns null if no such annotation found.
     */
    protected JsonDeserializer<Object> findDeserializerFromAnnotation(DeserializationContext ctxt,
            Annotated ann)
        throws JsonMappingException
    {
        Object deserDef = ctxt.getAnnotationIntrospector().findDeserializer(ann);
        if (deserDef == null) {
            return null;
        }
        JsonDeserializer<Object> deser = ctxt.deserializerInstance(ann, deserDef);
        // One more thing however: may need to also apply a converter:
        return findConvertingDeserializer(ctxt, ann, deser);
    }

    /**
     * Helper method that will check whether given annotated entity (usually class,
     * but may also be a property accessor) indicates that a {@link Converter} is to
     * be used; and if so, to construct and return suitable serializer for it.
     * If not, will simply return given serializer as is.
     */
    protected JsonDeserializer<Object> findConvertingDeserializer(DeserializationContext ctxt,
            Annotated a, JsonDeserializer<Object> deser)
        throws JsonMappingException
    {
        Converter<Object,Object> conv = findConverter(ctxt, a);
        if (conv == null) {
            return deser;
        }
        JavaType delegateType = conv.getInputType(ctxt.getTypeFactory());
        return (JsonDeserializer<Object>) new StdDelegatingDeserializer<Object>(conv, delegateType, deser);
    }

    protected Converter<Object,Object> findConverter(DeserializationContext ctxt,
            Annotated a)
        throws JsonMappingException
    {
        Object convDef = ctxt.getAnnotationIntrospector().findDeserializationConverter(a);
        if (convDef == null) {
            return null;
        }
        return ctxt.converterInstance(a, convDef);
    }    
    /**
     * Method called to see if given method has annotations that indicate
     * a more specific type than what the argument specifies.
     * If annotations are present, they must specify compatible Class;
     * instance of which can be assigned using the method. This means
     * that the Class has to be raw class of type, or its sub-class
     * (or, implementing class if original Class instance is an interface).
     *
     * @param a Method or field that the type is associated with
     * @param type Type derived from the setter argument
     *
     * @return Original type if no annotations are present; or a more
     *   specific type derived from it if type annotation(s) was found
     *
     * @throws JsonMappingException if invalid annotation is found
     */
    private JavaType modifyTypeByAnnotation(DeserializationContext ctxt,
            Annotated a, JavaType type)
        throws JsonMappingException
    {
        // first: let's check class for the instance itself:
        AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
        Class<?> subclass = intr.findDeserializationType(a, type);
        if (subclass != null) {
            try {
                type = type.narrowBy(subclass);
            } catch (IllegalArgumentException iae) {
                throw new JsonMappingException("Failed to narrow type "+type+" with concrete-type annotation (value "+subclass.getName()+"), method '"+a.getName()+"': "+iae.getMessage(), null, iae);
            }
        }

        // then key class
        if (type.isContainerType()) {
            Class<?> keyClass = intr.findDeserializationKeyType(a, type.getKeyType());
            if (keyClass != null) {
                // illegal to use on non-Maps
                if (!(type instanceof MapLikeType)) {
                    throw new JsonMappingException("Illegal key-type annotation: type "+type+" is not a Map(-like) type");
                }
                try {
                    type = ((MapLikeType) type).narrowKey(keyClass);
                } catch (IllegalArgumentException iae) {
                    throw new JsonMappingException("Failed to narrow key type "+type+" with key-type annotation ("+keyClass.getName()+"): "+iae.getMessage(), null, iae);
                }
            }
            JavaType keyType = type.getKeyType();
            /* 21-Mar-2011, tatu: ... and associated deserializer too (unless already assigned)
             *   (not 100% why or how, but this does seem to get called more than once, which
             *   is not good: for now, let's just avoid errors)
             */
            if (keyType != null && keyType.getValueHandler() == null) {
                Object kdDef = intr.findKeyDeserializer(a);
                if (kdDef != null) {
                    KeyDeserializer kd = ctxt.keyDeserializerInstance(a, kdDef);
                    if (kd != null) {
                        type = ((MapLikeType) type).withKeyValueHandler(kd);
                        keyType = type.getKeyType(); // just in case it's used below
                    }
                }
            }            
            
            // and finally content class; only applicable to structured types
            Class<?> cc = intr.findDeserializationContentType(a, type.getContentType());
            if (cc != null) {
                try {
                    type = type.narrowContentsBy(cc);
                } catch (IllegalArgumentException iae) {
                    throw new JsonMappingException("Failed to narrow content type "+type+" with content-type annotation ("+cc.getName()+"): "+iae.getMessage(), null, iae);
                }
            }
            // ... as well as deserializer for contents:
            JavaType contentType = type.getContentType();
            if (contentType.getValueHandler() == null) { // as with above, avoid resetting (which would trigger exception)
                Object cdDef = intr.findContentDeserializer(a);
                if (cdDef != null) {
                    JsonDeserializer<?> cd = null;
                    if (cdDef instanceof JsonDeserializer<?>) {
                        cdDef = (JsonDeserializer<?>) cdDef;
                    } else {
                        Class<?> cdClass = _verifyAsClass(cdDef, "findContentDeserializer", JsonDeserializer.None.class);
                        if (cdClass != null) {
                            cd = ctxt.deserializerInstance(a, cdClass);
                        }
                    }
                    if (cd != null) {
                        type = type.withContentValueHandler(cd);
                    }
                }
            }
        }
        return type;
    }

    /*
    /**********************************************************
    /* Helper methods, other
    /**********************************************************
     */

    /**
     * Helper method used to prevent both caching and cache lookups for structured
     * types that have custom value handlers
     *
     * @since 2.4.6
     */
    private boolean _hasCustomValueHandler(JavaType t) {
        if (t.isContainerType()) {
            JavaType ct = t.getContentType();
            if (ct != null) {
                return (ct.getValueHandler() != null) || (ct.getTypeHandler() != null);
            }
        }
        return false;
    }

    private Class<?> _verifyAsClass(Object src, String methodName, Class<?> noneClass)
    {
        if (src == null) {
            return null;
        }
        if (!(src instanceof Class)) {
            throw new IllegalStateException("AnnotationIntrospector."+methodName+"() returned value of type "+src.getClass().getName()+": expected type JsonSerializer or Class<JsonSerializer> instead");
        }
        Class<?> cls = (Class<?>) src;
        if (cls == noneClass || ClassUtil.isBogusClass(cls)) {
            return null;
        }
        return cls;
    }

    /*
    /**********************************************************
    /* Overridable error reporting methods
    /**********************************************************
     */

    protected JsonDeserializer<Object> _handleUnknownValueDeserializer(JavaType type)
        throws JsonMappingException
    {
        /* Let's try to figure out the reason, to give better error
         * messages
         */
        Class<?> rawClass = type.getRawClass();
        if (!ClassUtil.isConcrete(rawClass)) {
            throw new JsonMappingException("Can not find a Value deserializer for abstract type "+type);
        }
        throw new JsonMappingException("Can not find a Value deserializer for type "+type);
    }

    protected KeyDeserializer _handleUnknownKeyDeserializer(JavaType type)
        throws JsonMappingException
    {
        throw new JsonMappingException("Can not find a (Map) Key deserializer for type "+type);
    }

}
