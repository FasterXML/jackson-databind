package com.fasterxml.jackson.databind.deser;

import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.std.StdDelegatingDeserializer;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.type.*;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.Converter;
import com.fasterxml.jackson.databind.util.LRUMap;

/**
 * Class that defines caching layer between callers (like
 * {@link ObjectMapper},
 * {@link com.fasterxml.jackson.databind.DeserializationContext})
 * and classes that construct deserializers
 * ({@link com.fasterxml.jackson.databind.deser.DeserializerFactory}).
 */
public final class DeserializerCache
    implements java.io.Serializable // since 2.1
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
     * This currently means bean, Enum and container deserializers.
     */
    final protected LRUMap<JavaType, JsonDeserializer<Object>> _cachedDeserializers;

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

    public DeserializerCache() {
        this(2000); // see [databind#1995]
    }

    public DeserializerCache(int maxSize) {
        int initial = Math.min(64, maxSize>>2);
        _cachedDeserializers = new LRUMap<>(initial, maxSize);
    }

    /*
    /**********************************************************
    /* JDK serialization handling
    /**********************************************************
     */

    Object writeReplace() {
        // instead of making this transient, just clear it:
        _incompleteDeserializers.clear();
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
                deser = _handleUnknownValueDeserializer(ctxt, propertyType);
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
            return _handleUnknownKeyDeserializer(ctxt, type);
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
        if (_hasCustomHandlers(type)) {
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
            // We better only expose checked exceptions, since those
            // are what caller is expected to handle
            ctxt.reportBadDefinition(type, ClassUtil.exceptionMessage(iae));
            deser = null; // never gets here
        }
        if (deser == null) {
            return null;
        }
        /* cache resulting deserializer? always true for "plain" BeanDeserializer
         * (but can be re-defined for sub-classes by using @JsonCachable!)
         */
        // 27-Mar-2015, tatu: As per [databind#735], avoid caching types with custom value desers
        boolean addToCache = !_hasCustomHandlers(type) && deser.isCachable();

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
        if (deser instanceof ResolvableDeserializer) {
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
        // One more twist, as per [databind#288]; probably need to get new BeanDesc
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
        // If not, let's see which factory method to use

        // 12-Feb-20202, tatu: Need to ensure that not only all Enum implementations get
        //    there, but also `Enum` -- latter wrt [databind#2605], polymorphic usage
        if (type.isEnumType()) {
            return factory.createEnumDeserializer(ctxt, type, beanDesc);
        }
        if (type.isContainerType()) {
            if (type.isArrayType()) {
                return factory.createArrayDeserializer(ctxt, (ArrayType) type, beanDesc);
            }
            if (type.isMapLikeType()) {
                // 11-Mar-2017, tatu: As per [databind#1554], also need to block
                //    handling as Map if overriden with "as POJO" option.
                // Ideally we'd determine it bit later on (to allow custom handler checks)
                // but that won't work for other reasons. So do it here.
                // (read: rewrite for 3.0)
                JsonFormat.Value format = beanDesc.findExpectedFormat(null);
                if (format.getShape() != JsonFormat.Shape.OBJECT) {
                    MapLikeType mlt = (MapLikeType) type;
                    if (mlt instanceof MapType) {
                        return factory.createMapDeserializer(ctxt,(MapType) mlt, beanDesc);
                    }
                    return factory.createMapLikeDeserializer(ctxt, mlt, beanDesc);
                }
            }
            if (type.isCollectionLikeType()) {
                /* 03-Aug-2012, tatu: As per [databind#40], one exception is if shape
                 *   is to be Shape.OBJECT. Ideally we'd determine it bit later on
                 *   (to allow custom handler checks), but that won't work for other
                 *   reasons. So do it here.
                 */
                JsonFormat.Value format = beanDesc.findExpectedFormat(null);
                if (format.getShape() != JsonFormat.Shape.OBJECT) {
                    CollectionLikeType clt = (CollectionLikeType) type;
                    if (clt instanceof CollectionType) {
                        return factory.createCollectionDeserializer(ctxt, (CollectionType) clt, beanDesc);
                    }
                    return factory.createCollectionLikeDeserializer(ctxt, clt, beanDesc);
                }
            }
        }
        if (type.isReferenceType()) {
            return factory.createReferenceDeserializer(ctxt, (ReferenceType) type, beanDesc);
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
        AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
        if (intr == null) {
            return type;
        }

        // First things first: find explicitly annotated deserializer(s)

        // then key/value handlers  (annotated deserializers)?
        if (type.isMapLikeType()) {
            JavaType keyType = type.getKeyType();
            // 21-Mar-2011, tatu: ... and associated deserializer too (unless already assigned)
            //   (not 100% why or how, but this does seem to get called more than once, which
            //   is not good: for now, let's just avoid errors)
            if (keyType != null && keyType.getValueHandler() == null) {
                Object kdDef = intr.findKeyDeserializer(a);
                if (kdDef != null) {
                    KeyDeserializer kd = ctxt.keyDeserializerInstance(a, kdDef);
                    if (kd != null) {
                        type = ((MapLikeType) type).withKeyValueHandler(kd);
                        // keyType = type.getKeyType(); // just in case it's used below
                    }
                }
            }
        }
        JavaType contentType = type.getContentType();
        if (contentType != null) {
            if (contentType.getValueHandler() == null) { // as with above, avoid resetting (which would trigger exception)
                Object cdDef = intr.findContentDeserializer(a);
                if (cdDef != null) {
                    JsonDeserializer<?> cd = null;
                    if (cdDef instanceof JsonDeserializer<?>) {
                        cd = (JsonDeserializer<?>) cdDef;
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

        // And after handlers, possible type refinements
        // (note: could possibly avoid this if explicit deserializer was invoked?)
        type = intr.refineDeserializationType(ctxt.getConfig(), a, type);

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
     * @since 2.8.11
     */
    private boolean _hasCustomHandlers(JavaType t) {
        if (t.isContainerType()) {
            // First: value types may have both value and type handlers
            JavaType ct = t.getContentType();
            if (ct != null) {
                if ((ct.getValueHandler() != null) || (ct.getTypeHandler() != null)) {
                    return true;
                }
            }
            // Second: map(-like) types may have value handler for key (but not type; keys are untyped)
            if (t.isMapLikeType()) {
                JavaType kt = t.getKeyType();
                if (kt.getValueHandler() != null) {
                    return true;
                }
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

    protected JsonDeserializer<Object> _handleUnknownValueDeserializer(DeserializationContext ctxt, JavaType type)
        throws JsonMappingException
    {
        // Let's try to figure out the reason, to give better error messages
        Class<?> rawClass = type.getRawClass();
        if (!ClassUtil.isConcrete(rawClass)) {
            return ctxt.reportBadDefinition(type, "Cannot find a Value deserializer for abstract type "+type);
        }
        return ctxt.reportBadDefinition(type, "Cannot find a Value deserializer for type "+type);
    }

    protected KeyDeserializer _handleUnknownKeyDeserializer(DeserializationContext ctxt, JavaType type)
        throws JsonMappingException
    {
        return ctxt.reportBadDefinition(type, "Cannot find a (Map) Key deserializer for type "+type);
    }
}
