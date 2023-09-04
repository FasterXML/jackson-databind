package tools.jackson.databind.deser;

import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonFormat;

import tools.jackson.databind.*;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.deser.std.StdConvertingDeserializer;
import tools.jackson.databind.introspect.Annotated;
import tools.jackson.databind.type.*;
import tools.jackson.databind.util.ClassUtil;
import tools.jackson.databind.util.Converter;
import tools.jackson.databind.util.LookupCache;
import tools.jackson.databind.util.SimpleLookupCache;

/**
 * Class that defines caching layer between callers (like {@link ObjectMapper},
 * {@link tools.jackson.databind.DeserializationContext})
 * and classes that construct deserializers
 * ({@link tools.jackson.databind.deser.DeserializerFactory}).
 */
public final class DeserializerCache
    implements java.io.Serializable
{
    private static final long serialVersionUID = 3L;

    /**
     * Default size of the underlying cache to use.
     *<p>
     * NOTE: reduced from 2.x default.
     */
    public final static int DEFAULT_MAX_CACHE_SIZE = 1000;

    /*
    /**********************************************************************
    /* Caching
    /**********************************************************************
     */

    /**
     * We will also cache some dynamically constructed deserializers;
     * specifically, ones that are expensive to construct.
     * This currently (3.0) means POJO, Enum and Container (collection,
     * map) deserializers.
     */
    private final LookupCache<JavaType, ValueDeserializer<Object>> _cachedDeserializers;

    /**
     * During deserializer construction process we may need to keep track of partially
     * completed deserializers, to resolve cyclic dependencies. This is the
     * map used for storing deserializers before they are fully complete.
     */
    private final transient HashMap<JavaType, ValueDeserializer<Object>> _incompleteDeserializers
        = new HashMap<>(8);

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    public DeserializerCache() {
        this(DEFAULT_MAX_CACHE_SIZE);
    }

    public DeserializerCache(int maxSize) {
        this(new SimpleLookupCache<>(Math.min(64, maxSize>>2), maxSize));
    }

    public DeserializerCache(LookupCache<JavaType, ValueDeserializer<Object>> cache) {
        _cachedDeserializers = cache;
    }

    /*
    /**********************************************************************
    /* JDK serialization handling
    /**********************************************************************
     */

    //  Need to re-create just to initialize `transient` fields
    protected Object readResolve() {
        return new DeserializerCache(_cachedDeserializers);
    }

    /*
    /**********************************************************************
    /* Access to caching aspects
    /**********************************************************************
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
    /**********************************************************************
    /* General deserializer locating method
    /**********************************************************************
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
     * (see {@link ValueDeserializer#resolve}), but
     * not contextualized (wrt {@link ValueDeserializer#createContextual}): caller
     * has to handle latter if necessary.
     *
     * @param ctxt Deserialization context
     * @param propertyType Declared type of the value to deserializer (obtained using
     *   'setter' method signature and/or type annotations
     */
    public ValueDeserializer<Object> findValueDeserializer(DeserializationContext ctxt,
            DeserializerFactory factory, JavaType propertyType)
    {
        ValueDeserializer<Object> deser = _findCachedDeserializer(propertyType);
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
     * @throws DatabindException if there are fatal problems with
     *   accessing suitable key deserializer; including that of not
     *   finding any serializer
     */
    public KeyDeserializer findKeyDeserializer(DeserializationContext ctxt,
            DeserializerFactory factory, JavaType type)
    {
        KeyDeserializer kd = factory.createKeyDeserializer(ctxt, type);
        if (kd == null) { // if none found, need to use a placeholder that'll fail
            return _handleUnknownKeyDeserializer(ctxt, type);
        }
        // First: need to resolve?
        kd.resolve(ctxt);
        return kd;
    }

    /*
    /**********************************************************************
    /* Helper methods that handle cache lookups
    /**********************************************************************
     */

    protected ValueDeserializer<Object> _findCachedDeserializer(JavaType type)
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
    protected ValueDeserializer<Object> _createAndCacheValueDeserializer(DeserializationContext ctxt,
            DeserializerFactory factory, JavaType type)
    {
        /* Only one thread to construct deserializers at any given point in time;
         * limitations necessary to ensure that only completely initialized ones
         * are visible and used.
         */
        synchronized (_incompleteDeserializers) {
            // Ok, then: could it be that due to a race condition, deserializer can now be found?
            ValueDeserializer<Object> deser = _findCachedDeserializer(type);
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
    protected ValueDeserializer<Object> _createAndCache2(DeserializationContext ctxt,
            DeserializerFactory factory, JavaType type)
    {
        ValueDeserializer<Object> deser;
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
        _incompleteDeserializers.put(type, deser);
        try {
            deser.resolve(ctxt);
        } finally {
            _incompleteDeserializers.remove(type);
        }
        if (addToCache) {
            _cachedDeserializers.put(type, deser);
        }
        return deser;
    }

    /*
    /**********************************************************************
    /* Helper methods for actual construction of deserializers
    /**********************************************************************
     */

    /**
     * Method that does the heavy lifting of checking for per-type annotations,
     * find out full type, and figure out which actual factory method
     * to call.
     */
    @SuppressWarnings("unchecked")
    protected ValueDeserializer<Object> _createDeserializer(DeserializationContext ctxt,
            DeserializerFactory factory, JavaType type)
    {
        final DeserializationConfig config = ctxt.getConfig();

        // First things first: do we need to use abstract type mapping?
        if (type.isAbstract() || type.isMapLikeType() || type.isCollectionLikeType()) {
            type = config.mapAbstractType(type);
        }
        BeanDescription beanDesc = ctxt.introspectBeanDescription(type);
        // Then: does type define explicit deserializer to use, with annotation(s)?
        ValueDeserializer<Object> deser = findDeserializerFromAnnotation(ctxt,
                beanDesc.getClassInfo());
        if (deser != null) {
            return deser;
        }

        // If not, may have further type-modification annotations to check:
        JavaType newType = modifyTypeByAnnotation(ctxt, beanDesc.getClassInfo(), type);
        if (newType != type) {
            type = newType;
            beanDesc = ctxt.introspectBeanDescription(newType);
        }

        // We may also have a Builder type to consider...
        Class<?> builder = beanDesc.findPOJOBuilder();
        if (builder != null) {
            return (ValueDeserializer<Object>) factory.createBuilderBasedDeserializer(
            		ctxt, type, beanDesc, builder);
        }

        // Or perhaps a Converter?
        Converter<Object,Object> conv = beanDesc.findDeserializationConverter();
        if (conv == null) { // nope, just construct in normal way
            return (ValueDeserializer<Object>) _createDeserializer2(ctxt, factory, type, beanDesc);
        }
        // otherwise need to do bit of introspection
        JavaType delegateType = conv.getInputType(ctxt.getTypeFactory());
        // One more twist, as per [databind#288]; probably need to get new BeanDesc
        if (!delegateType.hasRawClass(type.getRawClass())) {
            beanDesc = ctxt.introspectBeanDescription(delegateType);
        }
        return new StdConvertingDeserializer<Object>(conv, delegateType,
                _createDeserializer2(ctxt, factory, delegateType, beanDesc));
    }

    protected ValueDeserializer<?> _createDeserializer2(DeserializationContext ctxt,
            DeserializerFactory factory, JavaType type, BeanDescription beanDesc)
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
                JsonFormat.Value format = beanDesc.findExpectedFormat(type.getRawClass());
                if (format.getShape() != JsonFormat.Shape.POJO) {
                    MapLikeType mlt = (MapLikeType) type;
                    if (mlt instanceof MapType) {
                        return factory.createMapDeserializer(ctxt,(MapType) mlt, beanDesc);
                    }
                    return factory.createMapLikeDeserializer(ctxt, mlt, beanDesc);
                }
            }
            if (type.isCollectionLikeType()) {
                /* One exception is if shape is to be Shape.POJO (or, as alias, OBJECT).
                 * Ideally we'd determine it bit later on (to allow custom handler checks),
                 * but that won't work for other reasons. So do it here.
                 */
                JsonFormat.Value format = beanDesc.findExpectedFormat(type.getRawClass());
                if (format.getShape() != JsonFormat.Shape.POJO) {
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
    protected ValueDeserializer<Object> findDeserializerFromAnnotation(DeserializationContext ctxt,
            Annotated ann)
    {
        Object deserDef = ctxt.getAnnotationIntrospector().findDeserializer(ctxt.getConfig(), ann);
        if (deserDef == null) {
            return null;
        }
        ValueDeserializer<Object> deser = ctxt.deserializerInstance(ann, deserDef);
        // One more thing however: may need to also apply a converter:
        return findConvertingDeserializer(ctxt, ann, deser);
    }

    /**
     * Helper method that will check whether given annotated entity (usually class,
     * but may also be a property accessor) indicates that a {@link Converter} is to
     * be used; and if so, to construct and return suitable serializer for it.
     * If not, will simply return given serializer as is.
     */
    protected ValueDeserializer<Object> findConvertingDeserializer(DeserializationContext ctxt,
            Annotated a, ValueDeserializer<Object> deser)
    {
        Converter<Object,Object> conv = findConverter(ctxt, a);
        if (conv == null) {
            return deser;
        }
        JavaType delegateType = conv.getInputType(ctxt.getTypeFactory());
        return (ValueDeserializer<Object>) new StdConvertingDeserializer<Object>(conv, delegateType, deser);
    }

    protected Converter<Object,Object> findConverter(DeserializationContext ctxt,
            Annotated a)
    {
        Object convDef = ctxt.getAnnotationIntrospector().findDeserializationConverter(ctxt.getConfig(), a);
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
     */
    private JavaType modifyTypeByAnnotation(DeserializationContext ctxt,
            Annotated a, JavaType type)
    {
        AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
        if (intr == null) {
            return type;
        }
        final MapperConfig<?> config = ctxt.getConfig();

        // First things first: find explicitly annotated deserializer(s)

        // then key/value handlers  (annotated deserializers)?
        if (type.isMapLikeType()) {
            JavaType keyType = type.getKeyType();
            // 21-Mar-2011, tatu: ... and associated deserializer too (unless already assigned)
            //   (not 100% why or how, but this does seem to get called more than once, which
            //   is not good: for now, let's just avoid errors)
            if (keyType != null && keyType.getValueHandler() == null) {
                Object kdDef = intr.findKeyDeserializer(config, a);
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
                Object cdDef = intr.findContentDeserializer(config, a);
                if (cdDef != null) {
                    ValueDeserializer<?> cd = null;
                    if (cdDef instanceof ValueDeserializer<?>) {
                        cd = (ValueDeserializer<?>) cdDef;
                    } else {
                        Class<?> cdClass = _verifyAsClass(cdDef, "findContentDeserializer", ValueDeserializer.None.class);
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
        type = intr.refineDeserializationType(config, a, type);

        return type;
    }

    /*
    /**********************************************************************
    /* Helper methods, other
    /**********************************************************************
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
            throw new IllegalStateException("AnnotationIntrospector."+methodName+"() returned value of type "
+src.getClass().getName()+": expected type `ValueSerializer` or `Class<ValueSerializer>` instead");
        }
        Class<?> cls = (Class<?>) src;
        if (cls == noneClass || ClassUtil.isBogusClass(cls)) {
            return null;
        }
        return cls;
    }

    /*
    /**********************************************************************
    /* Error reporting methods
    /**********************************************************************
     */

    protected ValueDeserializer<Object> _handleUnknownValueDeserializer(DeserializationContext ctxt, JavaType type)
    {
        // Let's try to figure out the reason, to give better error messages
        Class<?> rawClass = type.getRawClass();
        if (!ClassUtil.isConcrete(rawClass)) {
            return ctxt.reportBadDefinition(type, "Cannot find a Value deserializer for abstract type "+type);
        }
        return ctxt.reportBadDefinition(type, "Cannot find a Value deserializer for type "+type);
    }

    protected KeyDeserializer _handleUnknownKeyDeserializer(DeserializationContext ctxt, JavaType type)
    {
        return ctxt.reportBadDefinition(type, "Cannot find a (Map) Key deserializer for type "+type);
    }
}
