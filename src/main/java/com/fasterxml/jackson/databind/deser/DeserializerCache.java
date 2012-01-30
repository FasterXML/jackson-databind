package com.fasterxml.jackson.databind.deser;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.NoClass;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.type.*;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * Class that defines caching layer between callers (like
 * {@link ObjectMapper},
 * {@link com.fasterxml.jackson.databind.DeserializationContext})
 * and classes that construct deserializers
 * ({@link com.fasterxml.jackson.databind.deser.DeserializerFactory}).
 */
public final class DeserializerCache
{
    /*
    /**********************************************************
    /* Caching
    /**********************************************************
     */

    /**
     * We will also cache some dynamically constructed deserializers;
     * specifically, ones that are expensive to construct.
     * This currently means bean and Enum deserializers; array, List and Map
     * deserializers will not be cached.
     *<p>
     * Given that we don't expect much concurrency for additions
     * (should very quickly converge to zero after startup), let's
     * explicitly define a low concurrency setting.
     */
    final protected ConcurrentHashMap<JavaType, JsonDeserializer<Object>> _cachedDeserializers
        = new ConcurrentHashMap<JavaType, JsonDeserializer<Object>>(64, 0.75f, 2);

    /**
     * During deserializer construction process we may need to keep track of partially
     * completed deserializers, to resolve cyclic dependencies. This is the
     * map used for storing deserializers before they are fully complete.
     */
    final protected HashMap<JavaType, JsonDeserializer<Object>> _incompleteDeserializers
        = new HashMap<JavaType, JsonDeserializer<Object>>(8);

    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    /**
     * Factory responsible for constructing actual deserializers, if not
     * one of pre-configured types.
     */
    protected final DeserializerFactory _factory;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    public DeserializerCache(DeserializerFactory f) {
        _factory = f;
    }

    /*
    /**********************************************************
    /* Fluent factory methods
    /**********************************************************
     */
    
    /**
     * Method that sub-classes need to override, to ensure that fluent-factory
     * methods will produce proper sub-type.
     */
    public DeserializerCache withFactory(DeserializerFactory factory) {
        return new DeserializerCache(factory);
    }
    
    /**
     * Method that is to configure {@link DeserializerFactory} that provider has
     * to use specified deserializer provider, with highest precedence (that is,
     * additional providers have higher precedence than default one or previously
     * added ones)
     */
    public DeserializerCache withAdditionalDeserializers(Deserializers d) {
        return withFactory(_factory.withAdditionalDeserializers(d));
    }

    public DeserializerCache withAdditionalKeyDeserializers(KeyDeserializers d) {
        return withFactory(_factory.withAdditionalKeyDeserializers(d));
    }
    
    public DeserializerCache withDeserializerModifier(BeanDeserializerModifier modifier) {
        return withFactory(_factory.withDeserializerModifier(modifier));
    }

    public DeserializerCache withAbstractTypeResolver(AbstractTypeResolver resolver) {
        return withFactory(_factory.withAbstractTypeResolver(resolver));
    }

    /**
     * Method that will construct a new instance with specified additional value instantiators
     * (i.e. does NOT replace existing ones)
     */
    public DeserializerCache withValueInstantiators(ValueInstantiators instantiators) {
        return withFactory(_factory.withValueInstantiators(instantiators));
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
     *
     * @param ctxt Deserialization context
     * @param propertyType Declared type of the value to deserializer (obtained using
     *   'setter' method signature and/or type annotations
     * @param property Object that represents accessor for property value; field,
     *    setter method or constructor parameter.
     *
     * @throws JsonMappingException if there are fatal problems with
     *   accessing suitable deserializer; including that of not
     *   finding any serializer
     */
    @SuppressWarnings("unchecked")
    public JsonDeserializer<Object> findValueDeserializer(DeserializationContext ctxt,
            JavaType propertyType, BeanProperty property)
        throws JsonMappingException
    {
        JsonDeserializer<Object> deser = _findCachedDeserializer(propertyType);
        if (deser != null) {
            // [JACKSON-385]: need to support contextualization:
            if (deser instanceof ContextualDeserializer<?>) {
                JsonDeserializer<?> d = ((ContextualDeserializer<?>) deser).createContextual(ctxt, property);
                deser = (JsonDeserializer<Object>) d;
            }
            return deser;
        }
        // If not, need to request factory to construct (or recycle)
        deser = _createAndCacheValueDeserializer(ctxt, propertyType, property);
        if (deser == null) {
            /* Should we let caller handle it? Let's have a helper method
             * decide it; can throw an exception, or return a valid
             * deserializer
             */
            deser = _handleUnknownValueDeserializer(propertyType);
        }
        // [JACKSON-385]: need to support contextualization:
        if (deser instanceof ContextualDeserializer<?>) {
            JsonDeserializer<?> d = ((ContextualDeserializer<?>) deser).createContextual(ctxt, property);
            deser = (JsonDeserializer<Object>) d;
        }
        return deser;
    }
    
    /**
     * Method called to locate deserializer for given type, as well as matching
     * type deserializer (if one is needed); and if type deserializer is needed,
     * construct a "wrapped" deserializer that can extract and use type information
     * for calling actual deserializer.
     *<p>
     * Since this method is only called for root elements, no referral information
     * is taken.
     */
    public JsonDeserializer<Object> findTypedValueDeserializer(DeserializationContext ctxt,
            JavaType type, BeanProperty property)
        throws JsonMappingException
    {
        JsonDeserializer<Object> deser = findValueDeserializer(ctxt, type, property);
        TypeDeserializer typeDeser = _factory.findTypeDeserializer(ctxt.getConfig(), type, property);
        if (typeDeser != null) {
            return new WrappedDeserializer(typeDeser, deser);
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
            JavaType type, BeanProperty property)
        throws JsonMappingException
    {
        KeyDeserializer kd = _factory.createKeyDeserializer(ctxt, type, property);
        if (kd == null) { // if none found, need to use a placeholder that'll fail
            return _handleUnknownKeyDeserializer(type);
        }
        // First: need to resolve?
        if (kd instanceof ResolvableDeserializer) {
            ((ResolvableDeserializer) kd).resolve(ctxt);
        }
        // Second: contextualize?
        if (kd instanceof ContextualKeyDeserializer) {
            kd = ((ContextualKeyDeserializer) kd).createContextual(ctxt, property);
        }
        return kd;
    }

    /**
     * Method called to find out whether provider would be able to find
     * a deserializer for given type, using a root reference (i.e. not
     * through fields or membership in an array or collection)
     */
    public boolean hasValueDeserializerFor(DeserializationContext ctxt, JavaType type)
    {
        /* Note: mostly copied from findValueDeserializer, except for
         * handling of unknown types
         */
        JsonDeserializer<Object> deser = _findCachedDeserializer(type);
        if (deser == null) {
            try {
                deser = _createAndCacheValueDeserializer(ctxt, type, null);
            } catch (Exception e) {
                return false;
            }
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
            throw new IllegalArgumentException();
        }
        return _cachedDeserializers.get(type);
    }

    /**
     * Method that will try to create a deserializer for given type,
     * and resolve and cache it if necessary
     * 
     * @param config Configuration
     * @param type Type of property to deserializer
     * @param property Property (field, setter, ctor arg) to use deserializer for
     */
    protected JsonDeserializer<Object>_createAndCacheValueDeserializer(DeserializationContext ctxt,
            JavaType type, BeanProperty property)
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
                return _createAndCache2(ctxt, type, property);
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
            JavaType type, BeanProperty property)
        throws JsonMappingException
    {
        JsonDeserializer<Object> deser;
        try {
            deser = _createDeserializer(ctxt, type, property);
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
        boolean addToCache = deser.isCachable();

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
            JavaType type, BeanProperty property)
        throws JsonMappingException
    {
        final DeserializationConfig config = ctxt.getConfig();

        // First things first: do we need to use abstract type mapping?
        if (type.isAbstract() || type.isMapLikeType() || type.isCollectionLikeType()) {
            type = _factory.mapAbstractType(config, type);
        }
        BeanDescription beanDesc = config.introspect(type);
        // Then: does type define explicit deserializer to use, with annotation(s)?
        JsonDeserializer<Object> deser = findDeserializerFromAnnotation(ctxt,
                beanDesc.getClassInfo(), property);
        if (deser != null) {
            return deser;
        }

        // If not, may have further type-modification annotations to check:
        JavaType newType = modifyTypeByAnnotation(ctxt, beanDesc.getClassInfo(), type, property);
        if (newType != type) {
            type = newType;
            beanDesc = config.introspect(newType);
        }

        // If not, let's see which factory method to use:
        if (type.isEnumType()) {
            return (JsonDeserializer<Object>) _factory.createEnumDeserializer(ctxt, type,
                    beanDesc, property);
        }
        if (type.isContainerType()) {
            if (type.isArrayType()) {
                return (JsonDeserializer<Object>)_factory.createArrayDeserializer(ctxt,
                        (ArrayType) type, beanDesc, property);
            }
            if (type.isMapLikeType()) {
                MapLikeType mlt = (MapLikeType) type;
                if (mlt.isTrueMapType()) {
                    return (JsonDeserializer<Object>)_factory.createMapDeserializer(ctxt,
                            (MapType) mlt, beanDesc, property);
                }
                return (JsonDeserializer<Object>)_factory.createMapLikeDeserializer(ctxt,
                        mlt, beanDesc, property);
            }
            if (type.isCollectionLikeType()) {
                CollectionLikeType clt = (CollectionLikeType) type;
                if (clt.isTrueCollectionType()) {
                    return (JsonDeserializer<Object>)_factory.createCollectionDeserializer(ctxt,
                            (CollectionType) clt, beanDesc, property);
                }
                return (JsonDeserializer<Object>)_factory.createCollectionLikeDeserializer(ctxt,
                        clt, beanDesc, property);
            }
        }

        // 02-Mar-2009, tatu: Let's consider JsonNode to be a type of its own
        if (JsonNode.class.isAssignableFrom(type.getRawClass())) {
            return (JsonDeserializer<Object>)_factory.createTreeDeserializer(config, type, beanDesc, property);
        }
        return (JsonDeserializer<Object>)_factory.createBeanDeserializer(ctxt, type, beanDesc, property);
    }

    /**
     * Helper method called to check if a class or method
     * has annotation that tells which class to use for deserialization.
     * Returns null if no such annotation found.
     */
    protected JsonDeserializer<Object> findDeserializerFromAnnotation(DeserializationContext ctxt,
            Annotated ann, BeanProperty property)
        throws JsonMappingException
    {
        Object deserDef = ctxt.getAnnotationIntrospector().findDeserializer(ann);
        if (deserDef == null) {
            return null;
        }
        return ctxt.deserializerInstance(ann, property, deserDef);
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
     * @param prop Property that lead to this annotation, if any.
     *
     * @return Original type if no annotations are present; or a more
     *   specific type derived from it if type annotation(s) was found
     *
     * @throws JsonMappingException if invalid annotation is found
     */
    private JavaType modifyTypeByAnnotation(DeserializationContext ctxt,
            Annotated a, JavaType type, BeanProperty prop)
        throws JsonMappingException
    {
        // first: let's check class for the instance itself:
        AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
        Class<?> subclass = intr.findDeserializationType(a, type,
                (prop == null)? null : prop.getName());
        if (subclass != null) {
            try {
                type = type.narrowBy(subclass);
            } catch (IllegalArgumentException iae) {
                throw new JsonMappingException("Failed to narrow type "+type+" with concrete-type annotation (value "+subclass.getName()+"), method '"+a.getName()+"': "+iae.getMessage(), null, iae);
            }
        }

        // then key class
        if (type.isContainerType()) {
            Class<?> keyClass = intr.findDeserializationKeyType(a, type.getKeyType(),
                    (prop == null)? null : prop.getName());
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
                    KeyDeserializer kd = ctxt.keyDeserializerInstance(a, prop, kdDef);
                    if (kd != null) {
                        type = ((MapLikeType) type).withKeyValueHandler(kd);
                        keyType = type.getKeyType(); // just in case it's used below
                    }
                }
            }            
            
            // and finally content class; only applicable to structured types
            Class<?> cc = intr.findDeserializationContentType(a, type.getContentType(),
                    (prop == null) ? null : prop.getName());
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
                            cd = ctxt.deserializerInstance(a, prop, cdClass);
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

    private Class<?> _verifyAsClass(Object src, String methodName, Class<?> noneClass)
    {
        if (src == null) {
            return null;
        }
        if (!(src instanceof Class)) {
            throw new IllegalStateException("AnnotationIntrospector."+methodName+"() returned value of type "+src.getClass().getName()+": expected type JsonSerializer or Class<JsonSerializer> instead");
        }
        Class<?> cls = (Class<?>) src;
        if (cls == noneClass || cls == NoClass.class) {
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

    /*
    /**********************************************************
    /*  Helper classes
    /**********************************************************
     */

    /**
     * Simple deserializer that will call configured type deserializer, passing
     * in configured data deserializer, and exposing it all as a simple
     * deserializer.
     */
    protected final static class WrappedDeserializer
        extends JsonDeserializer<Object>
    {
        final TypeDeserializer _typeDeserializer;
        final JsonDeserializer<Object> _deserializer;

        public WrappedDeserializer(TypeDeserializer typeDeser, JsonDeserializer<Object> deser)
        {
            super();
            _typeDeserializer = typeDeser;
            _deserializer = deser;
        }

        @Override
        public Object deserialize(JsonParser jp, DeserializationContext ctxt)
                throws IOException, JsonProcessingException
        {
            return _deserializer.deserializeWithType(jp, ctxt, _typeDeserializer);
        }

        @Override
        public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer)
                throws IOException, JsonProcessingException
        {
            // should never happen? (if it can, could call on that object)
            throw new IllegalStateException("Type-wrapped deserializer's deserializeWithType should never get called");
        }
    }
}
