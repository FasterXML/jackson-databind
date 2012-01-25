package com.fasterxml.jackson.databind.deser;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.core.JsonNode;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.NoClass;
import com.fasterxml.jackson.databind.deser.std.*;
import com.fasterxml.jackson.databind.ext.OptionalHandlerFactory;
import com.fasterxml.jackson.databind.introspect.*;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.type.*;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.EnumResolver;

/**
 * Abstract factory base class that can provide deserializers for standard
 * JDK classes, including collection classes and simple heuristics for
 * "upcasting" commmon collection interface types
 * (such as {@link java.util.Collection}).
 *<p>
 * Since all simple deserializers are eagerly instantiated, and there is
 * no additional introspection or customizability of these types,
 * this factory is stateless.
 */
public abstract class BasicDeserializerFactory
    extends DeserializerFactory
{
    /**
     * We will pre-create serializers for common non-structured
     * (that is things other than Collection, Map or array)
     * types. These need not go through factory.
     */
    final protected static HashMap<ClassKey, JsonDeserializer<Object>> _simpleDeserializers
        = new HashMap<ClassKey, JsonDeserializer<Object>>();

    /**
     * Also special array deserializers for primitive array types.
     */
    final protected static HashMap<JavaType,JsonDeserializer<Object>> _arrayDeserializers
        = PrimitiveArrayDeserializers.getAll();

    /**
     * Set of available key deserializers is currently limited
     * to standard types; and all known instances are storing in this map.
     */
    final protected static HashMap<JavaType, KeyDeserializer> _keyDeserializers = StdKeyDeserializers.constructAll();

    static {
        // First, add the fall-back "untyped" deserializer:
        _add(_simpleDeserializers, Object.class, new UntypedObjectDeserializer());
    
        // Then String and String-like converters:
        StdDeserializer<?> strDeser = new StringDeserializer();
        _add(_simpleDeserializers, String.class, strDeser);
        _add(_simpleDeserializers, CharSequence.class, strDeser);
    
        // Primitives/wrappers, other Numbers:
        _add(_simpleDeserializers, NumberDeserializers.all());
        // Date/time types
        _add(_simpleDeserializers, DateDeserializers.all());
        // other JDK types
        _add(_simpleDeserializers, JdkDeserializers.all());
        // and a few Jackson types as well:
        _add(_simpleDeserializers, JacksonDeserializers.all());
    }

    private static void _add(Map<ClassKey, JsonDeserializer<Object>> desers,
            StdDeserializer<?>[] serializers) {
        for (StdDeserializer<?> ser : serializers) {
            _add(desers, ser.getValueClass(), ser);
        }
    }

    private static void _add(Map<ClassKey, JsonDeserializer<Object>> desers,
            Class<?> valueClass, StdDeserializer<?> stdDeser)
    {
        @SuppressWarnings("unchecked")
        JsonDeserializer<Object> deser = (JsonDeserializer<Object>) stdDeser;
        desers.put(new ClassKey(valueClass), deser);
    }
    
    /* We do some defaulting for abstract Map classes and
     * interfaces, to avoid having to use exact types or annotations in
     * cases where the most common concrete Maps will do.
     */
    @SuppressWarnings("rawtypes")
    final static HashMap<String, Class<? extends Map>> _mapFallbacks =
        new HashMap<String, Class<? extends Map>>();
    static {
        _mapFallbacks.put(Map.class.getName(), LinkedHashMap.class);
        _mapFallbacks.put(ConcurrentMap.class.getName(), ConcurrentHashMap.class);
        _mapFallbacks.put(SortedMap.class.getName(), TreeMap.class);

        /* 11-Jan-2009, tatu: Let's see if we can still add support for
         *    JDK 1.6 interfaces, even if we run on 1.5. Just need to be
         *    more careful with typos, since compiler won't notice any
         *    problems...
         */
        _mapFallbacks.put("java.util.NavigableMap", TreeMap.class);
        try {
            Class<?> key = Class.forName("java.util.ConcurrentNavigableMap");
            Class<?> value = Class.forName("java.util.ConcurrentSkipListMap");
            @SuppressWarnings("unchecked")
                Class<? extends Map<?,?>> mapValue = (Class<? extends Map<?,?>>) value;
            _mapFallbacks.put(key.getName(), mapValue);
        } catch (ClassNotFoundException cnfe) { // occurs on 1.5
        }
    }

    /* We do some defaulting for abstract Collection classes and
     * interfaces, to avoid having to use exact types or annotations in
     * cases where the most common concrete Collection will do.
     */
    @SuppressWarnings("rawtypes")
    final static HashMap<String, Class<? extends Collection>> _collectionFallbacks =
        new HashMap<String, Class<? extends Collection>>();
    static {
        _collectionFallbacks.put(Collection.class.getName(), ArrayList.class);
        _collectionFallbacks.put(List.class.getName(), ArrayList.class);
        _collectionFallbacks.put(Set.class.getName(), HashSet.class);
        _collectionFallbacks.put(SortedSet.class.getName(), TreeSet.class);
        _collectionFallbacks.put(Queue.class.getName(), LinkedList.class);

        /* 11-Jan-2009, tatu: Let's see if we can still add support for
         *    JDK 1.6 interfaces, even if we run on 1.5. Just need to be
         *    more careful with typos, since compiler won't notice any
         *    problems...
         */
        _collectionFallbacks.put("java.util.Deque", LinkedList.class);
        _collectionFallbacks.put("java.util.NavigableSet", TreeSet.class);
    }

    /**
     * To support external/optional deserializers, we'll use a helper class
     */
    protected OptionalHandlerFactory optionalHandlers = OptionalHandlerFactory.instance;
    
    /*
    /**********************************************************
    /* Config
    /**********************************************************
     */
    
    /**
     * Configuration settings for this factory; immutable instance (just like this
     * factory), new version created via copy-constructor (fluent-style)
     */
    protected final Config _factoryConfig;
    
    /*
    /**********************************************************
    /* Life cycle
    /**********************************************************
     */

    protected BasicDeserializerFactory(Config config) {
        _factoryConfig = config;
    }

    @Override
    public final Config getConfig() {
        return _factoryConfig;
    }
    
    // can't be implemented quite here
    @Override
    public abstract DeserializerFactory withConfig(DeserializerFactory.Config config);
    
    /*
    /**********************************************************
    /* Methods for sub-classes to override to provide
    /* custom deserializers
    /**********************************************************
     */
    
    protected JsonDeserializer<?> _findCustomArrayDeserializer(ArrayType type,
            DeserializationConfig config,
            BeanDescription beanDesc, BeanProperty property,
            TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer)
        throws JsonMappingException
    {
        for (Deserializers d  : _factoryConfig.deserializers()) {
            JsonDeserializer<?> deser = d.findArrayDeserializer(type, config,
                    beanDesc, property, elementTypeDeserializer, elementDeserializer);
            if (deser != null) {
                return deser;
            }
        }
        return null;
    }
    
    protected JsonDeserializer<?> _findCustomCollectionDeserializer(CollectionType type,
            DeserializationConfig config,
            BeanDescription beanDesc, BeanProperty property,
            TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer)
        throws JsonMappingException
    {
        for (Deserializers d  : _factoryConfig.deserializers()) {
            JsonDeserializer<?> deser = d.findCollectionDeserializer(type, config, beanDesc, property,
                    elementTypeDeserializer, elementDeserializer);
            if (deser != null) {
                return deser;
            }
        }
        return null;
    }

    protected JsonDeserializer<?> _findCustomCollectionLikeDeserializer(CollectionLikeType type,
            DeserializationConfig config,
            BeanDescription beanDesc, BeanProperty property,
            TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer)
        throws JsonMappingException
    {
        for (Deserializers d  : _factoryConfig.deserializers()) {
            JsonDeserializer<?> deser = d.findCollectionLikeDeserializer(type, config, beanDesc, property,
                    elementTypeDeserializer, elementDeserializer);
            if (deser != null) {
                return deser;
            }
        }
        return null;
    }
    
    protected JsonDeserializer<?> _findCustomEnumDeserializer(Class<?> type,
            DeserializationConfig config, BeanDescription beanDesc, BeanProperty property)
        throws JsonMappingException
    {
        for (Deserializers d  : _factoryConfig.deserializers()) {
            JsonDeserializer<?> deser = d.findEnumDeserializer(type, config, beanDesc, property);
            if (deser != null) {
                return deser;
            }
        }
        return null;
    }

    protected JsonDeserializer<?> _findCustomMapDeserializer(MapType type,
            DeserializationConfig config, 
            BeanDescription beanDesc, BeanProperty property,
            KeyDeserializer keyDeserializer,
            TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer)
        throws JsonMappingException
    {
        for (Deserializers d  : _factoryConfig.deserializers()) {
            JsonDeserializer<?> deser = d.findMapDeserializer(type, config, beanDesc, property,
                    keyDeserializer, elementTypeDeserializer, elementDeserializer);
            if (deser != null) {
                return deser;
            }
        }
        return null;
    }

    protected JsonDeserializer<?> _findCustomMapLikeDeserializer(MapLikeType type,
            DeserializationConfig config,
            BeanDescription beanDesc, BeanProperty property,
            KeyDeserializer keyDeserializer,
            TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer)
        throws JsonMappingException
    {
        for (Deserializers d  : _factoryConfig.deserializers()) {
            JsonDeserializer<?> deser = d.findMapLikeDeserializer(type, config, beanDesc, property,
                    keyDeserializer, elementTypeDeserializer, elementDeserializer);
            if (deser != null) {
                return deser;
            }
        }
        return null;
    }
    
    protected JsonDeserializer<?> _findCustomTreeNodeDeserializer(Class<? extends JsonNode> type,
            DeserializationConfig config,
            BeanDescription beanDesc, BeanProperty property)
        throws JsonMappingException
    {
        for (Deserializers d  : _factoryConfig.deserializers()) {
            JsonDeserializer<?> deser = d.findTreeNodeDeserializer(type, config, beanDesc, property);
            if (deser != null) {
                return deser;
            }
        }
        return null;
    }
    
    /*
    /**********************************************************
    /* JsonDeserializerFactory impl (partial)
    /**********************************************************
     */

    @Override
    public abstract ValueInstantiator findValueInstantiator(DeserializationContext ctxt,
            BeanDescription beanDesc)
        throws JsonMappingException;

    @Override
    public abstract JavaType mapAbstractType(DeserializationConfig config, JavaType type)
            throws JsonMappingException;
    
    @Override
    public JsonDeserializer<?> createArrayDeserializer(DeserializationContext ctxt,
            ArrayType type, final BeanDescription beanDesc, BeanProperty property)
        throws JsonMappingException
    {
        JavaType elemType = type.getContentType();
        
        // Very first thing: is deserializer hard-coded for elements?
        JsonDeserializer<Object> contentDeser = elemType.getValueHandler();
        if (contentDeser == null) {
            // Maybe special array type, such as "primitive" arrays (int[] etc)
            JsonDeserializer<?> deser = _arrayDeserializers.get(elemType);
            if (deser != null) {
                /* 23-Nov-2010, tatu: Although not commonly needed, ability to override
                 *   deserializers for all types (including primitive arrays) is useful
                 *   so let's allow this
                 */
                JsonDeserializer<?> custom = _findCustomArrayDeserializer(type,
                        ctxt.getConfig(), beanDesc, property, null, contentDeser);
                if (custom != null) {
                    return custom;
                }
                return deser;
            }
            // If not, generic one:
            if (elemType.isPrimitive()) { // sanity check
                throw new IllegalArgumentException("Internal error: primitive type ("+type+") passed, no array deserializer found");
            }
        }
        // Then optional type info (1.5): if type has been resolved, we may already know type deserializer:
        TypeDeserializer elemTypeDeser = elemType.getTypeHandler();
        // but if not, may still be possible to find:
        if (elemTypeDeser == null) {
            elemTypeDeser = findTypeDeserializer(ctxt.getConfig(), elemType, property);
        }
        // 23-Nov-2010, tatu: Custom array deserializer?
        JsonDeserializer<?> custom = _findCustomArrayDeserializer(type,
                ctxt.getConfig(), beanDesc, property, elemTypeDeser, contentDeser);
        if (custom != null) {
            return custom;
        }
        return new ObjectArrayDeserializer(type, property, contentDeser, elemTypeDeser);
    }
    
    @Override
    public JsonDeserializer<?> createCollectionDeserializer(DeserializationContext ctxt,
            CollectionType type, BeanDescription beanDesc, BeanProperty property)
        throws JsonMappingException
    {
        JavaType contentType = type.getContentType();
        // Very first thing: is deserializer hard-coded for elements?
        JsonDeserializer<Object> contentDeser = contentType.getValueHandler();

        // Then optional type info (1.5): if type has been resolved, we may already know type deserializer:
        TypeDeserializer contentTypeDeser = contentType.getTypeHandler();
        // but if not, may still be possible to find:
        if (contentTypeDeser == null) {
            contentTypeDeser = findTypeDeserializer(ctxt.getConfig(), contentType, property);
        }

        // 23-Nov-2010, tatu: Custom deserializer?
        JsonDeserializer<?> custom = _findCustomCollectionDeserializer(type,
                ctxt.getConfig(), beanDesc, property,
                contentTypeDeser, contentDeser);
        if (custom != null) {
            return custom;
        }
        
        Class<?> collectionClass = type.getRawClass();
        if (contentDeser == null) { // not defined by annotation
            // One special type: EnumSet:
            if (EnumSet.class.isAssignableFrom(collectionClass)) {
                return new EnumSetDeserializer(contentType, property, null);
            }
        }
        
        /* One twist: if we are being asked to instantiate an interface or
         * abstract Collection, we need to either find something that implements
         * the thing, or give up.
         *
         * Note that we do NOT try to guess based on secondary interfaces
         * here; that would probably not work correctly since casts would
         * fail later on (as the primary type is not the interface we'd
         * be implementing)
         */
        if (type.isInterface() || type.isAbstract()) {
            @SuppressWarnings({ "rawtypes" })
            Class<? extends Collection> fallback = _collectionFallbacks.get(collectionClass.getName());
            if (fallback == null) {
                throw new IllegalArgumentException("Can not find a deserializer for non-concrete Collection type "+type);
            }
            collectionClass = fallback;
            type = (CollectionType) ctxt.getConfig().constructSpecializedType(type, collectionClass);
            // But if so, also need to re-check creators...
            beanDesc = ctxt.getConfig().introspectForCreation(type);
        }
        ValueInstantiator inst = findValueInstantiator(ctxt, beanDesc);
        // 13-Dec-2010, tatu: Can use more optimal deserializer if content type is String, so:
        if (contentType.getRawClass() == String.class) {
            // no value type deserializer because Strings are one of natural/native types:
            return new StringCollectionDeserializer(type, property, contentDeser, inst);
        }
        return new CollectionDeserializer(type, property, contentDeser, contentTypeDeser, inst);
    }

    // Copied almost verbatim from "createCollectionDeserializer" -- should try to share more code
    @Override
    public JsonDeserializer<?> createCollectionLikeDeserializer(DeserializationContext ctxt,
            CollectionLikeType type, final BeanDescription beanDesc, BeanProperty property)
        throws JsonMappingException
    {
        JavaType contentType = type.getContentType();
        // Very first thing: is deserializer hard-coded for elements?
        JsonDeserializer<Object> contentDeser = contentType.getValueHandler();

        // Then optional type info (1.5): if type has been resolved, we may already know type deserializer:
        TypeDeserializer contentTypeDeser = contentType.getTypeHandler();
        // but if not, may still be possible to find:
        if (contentTypeDeser == null) {
            contentTypeDeser = findTypeDeserializer(ctxt.getConfig(), contentType, property);
        }
        return _findCustomCollectionLikeDeserializer(type, ctxt.getConfig(), beanDesc, property,
                contentTypeDeser, contentDeser);
    }
    
    @Override
    public JsonDeserializer<?> createMapDeserializer(DeserializationContext ctxt,
            MapType type, BeanDescription beanDesc, BeanProperty property)
        throws JsonMappingException
    {
        final DeserializationConfig config = ctxt.getConfig();
        JavaType keyType = type.getKeyType();
        JavaType contentType = type.getContentType();
        
        // First: is there annotation-specified deserializer for values?
        @SuppressWarnings("unchecked")
        JsonDeserializer<Object> contentDeser = (JsonDeserializer<Object>) contentType.getValueHandler();
        
        // Ok: need a key deserializer (null indicates 'default' here)
        KeyDeserializer keyDes = (KeyDeserializer) keyType.getValueHandler();
        // Then optional type info (1.5); either attached to type, or resolved separately:
        TypeDeserializer contentTypeDeser = contentType.getTypeHandler();
        // but if not, may still be possible to find:
        if (contentTypeDeser == null) {
            contentTypeDeser = findTypeDeserializer(config, contentType, property);
        }

        // 23-Nov-2010, tatu: Custom deserializer?
        JsonDeserializer<?> custom = _findCustomMapDeserializer(type, config, beanDesc, property,
                keyDes, contentTypeDeser, contentDeser);

        if (custom != null) {
            return custom;
        }
        // Value handling is identical for all, but EnumMap requires special handling for keys
        Class<?> mapClass = type.getRawClass();
        if (EnumMap.class.isAssignableFrom(mapClass)) {
            Class<?> kt = keyType.getRawClass();
            if (kt == null || !kt.isEnum()) {
                throw new IllegalArgumentException("Can not construct EnumMap; generic (key) type not available");
            }
            return new EnumMapDeserializer(type, property, null, contentDeser);
        }

        // Otherwise, generic handler works ok.

        /* But there is one more twist: if we are being asked to instantiate
         * an interface or abstract Map, we need to either find something
         * that implements the thing, or give up.
         *
         * Note that we do NOT try to guess based on secondary interfaces
         * here; that would probably not work correctly since casts would
         * fail later on (as the primary type is not the interface we'd
         * be implementing)
         */
        if (type.isInterface() || type.isAbstract()) {
            @SuppressWarnings("rawtypes")
            Class<? extends Map> fallback = _mapFallbacks.get(mapClass.getName());
            if (fallback == null) {
                throw new IllegalArgumentException("Can not find a deserializer for non-concrete Map type "+type);
            }
            mapClass = fallback;
            type = (MapType) config.constructSpecializedType(type, mapClass);
            // But if so, also need to re-check creators...
            beanDesc = config.introspectForCreation(type);
        }
        ValueInstantiator inst = findValueInstantiator(ctxt, beanDesc);
        MapDeserializer md = new MapDeserializer(type, property, inst, keyDes, contentDeser, contentTypeDeser);
        md.setIgnorableProperties(config.getAnnotationIntrospector().findPropertiesToIgnore(beanDesc.getClassInfo()));
        return md;
    }

    // Copied almost verbatim from "createMapDeserializer" -- should try to share more code
    @Override
    public JsonDeserializer<?> createMapLikeDeserializer(DeserializationContext ctxt,
            MapLikeType type, final BeanDescription beanDesc, BeanProperty property)
        throws JsonMappingException
    {
        JavaType keyType = type.getKeyType();
        JavaType contentType = type.getContentType();
        
        // First: is there annotation-specified deserializer for values?
        @SuppressWarnings("unchecked")
        JsonDeserializer<Object> contentDeser = (JsonDeserializer<Object>) contentType.getValueHandler();
        
        // Ok: need a key deserializer (null indicates 'default' here)
        KeyDeserializer keyDes = (KeyDeserializer) keyType.getValueHandler();
        /* !!! 24-Jan-2012, tatu: NOTE: impls MUST use resolve() to find key deserializer!
        if (keyDes == null) {
            keyDes = p.findKeyDeserializer(config, keyType, property);
        }
        */
        // Then optional type info (1.5); either attached to type, or resolve separately:
        TypeDeserializer contentTypeDeser = contentType.getTypeHandler();
        // but if not, may still be possible to find:
        if (contentTypeDeser == null) {
            contentTypeDeser = findTypeDeserializer(ctxt.getConfig(), contentType, property);
        }
        return _findCustomMapLikeDeserializer(type, ctxt.getConfig(),
                beanDesc, property, keyDes, contentTypeDeser, contentDeser);
    }

    /**
     * Factory method for constructing serializers of {@link Enum} types.
     */
    @Override
    public JsonDeserializer<?> createEnumDeserializer(DeserializationContext ctxt,
            JavaType type, BeanDescription beanDesc, BeanProperty property)
        throws JsonMappingException
    {
        Class<?> enumClass = type.getRawClass();
        // 23-Nov-2010, tatu: Custom deserializer?
        JsonDeserializer<?> custom = _findCustomEnumDeserializer(enumClass,
                ctxt.getConfig(), beanDesc, property);
        if (custom != null) {
            return custom;
        }

        // [JACKSON-193] May have @JsonCreator for static factory method:
        for (AnnotatedMethod factory : beanDesc.getFactoryMethods()) {
            if (ctxt.getAnnotationIntrospector().hasCreatorAnnotation(factory)) {
                int argCount = factory.getParameterCount();
                if (argCount == 1) {
                    Class<?> returnType = factory.getRawType();
                    // usually should be class, but may be just plain Enum<?> (for Enum.valueOf()?)
                    if (returnType.isAssignableFrom(enumClass)) {
                        return EnumDeserializer.deserializerForCreator(ctxt.getConfig(), enumClass, factory);
                    }
                }
                throw new IllegalArgumentException("Unsuitable method ("+factory+") decorated with @JsonCreator (for Enum type "
                        +enumClass.getName()+")");
            }
        }
        // [JACKSON-749] Also, need to consider @JsonValue, if one found
        return new EnumDeserializer(constructEnumResolver(enumClass, ctxt.getConfig(), beanDesc.findJsonValueMethod()));
    }

    @Override
    public JsonDeserializer<?> createTreeDeserializer(DeserializationConfig config,
            JavaType nodeType, BeanDescription beanDesc, BeanProperty property)
        throws JsonMappingException
    {
        @SuppressWarnings("unchecked")
        Class<? extends JsonNode> nodeClass = (Class<? extends JsonNode>) nodeType.getRawClass();
        // 23-Nov-2010, tatu: Custom deserializer?
        JsonDeserializer<?> custom = _findCustomTreeNodeDeserializer(nodeClass, config,
                beanDesc, property);
        if (custom != null) {
            return custom;
        }
        return JsonNodeDeserializer.getDeserializer(nodeClass);
    }

    /**
     * Method called by {@link BeanDeserializerFactory} to see if there might be a standard
     * deserializer registered for given type.
     */
    @SuppressWarnings("unchecked")
    protected JsonDeserializer<Object> findStdBeanDeserializer(DeserializationConfig config,
            JavaType type, BeanProperty property)
        throws JsonMappingException
    {
        Class<?> cls = type.getRawClass();
        // note: we do NOT check for custom deserializers here; that's for sub-class to do
        JsonDeserializer<Object> deser = _simpleDeserializers.get(new ClassKey(cls));
        if (deser != null) {
            return deser;
        }
        
        // [JACKSON-283]: AtomicReference is a rather special type...
        if (AtomicReference.class.isAssignableFrom(cls)) {
            // Must find parameterization
            TypeFactory tf = config.getTypeFactory();
            JavaType[] params = tf.findTypeParameters(type, AtomicReference.class);
            JavaType referencedType;
            if (params == null || params.length < 1) { // untyped (raw)
                referencedType = TypeFactory.unknownType();
            } else {
                referencedType = params[0];
            }
            
            JsonDeserializer<?> d2 = new JdkDeserializers.AtomicReferenceDeserializer(referencedType, property);
            return (JsonDeserializer<Object>)d2;
        }
        // [JACKSON-386]: External/optional type handlers are handled somewhat differently
        JsonDeserializer<?> d = optionalHandlers.findDeserializer(type, config);
        if (d != null) {
            return (JsonDeserializer<Object>)d;
        }
        return null;
    }
    
    @Override
    public TypeDeserializer findTypeDeserializer(DeserializationConfig config, JavaType baseType,
            BeanProperty property)
        throws JsonMappingException
    {
        Class<?> cls = baseType.getRawClass();
        BeanDescription bean = config.introspectClassAnnotations(cls);
        AnnotatedClass ac = bean.getClassInfo();
        AnnotationIntrospector ai = config.getAnnotationIntrospector();
        TypeResolverBuilder<?> b = ai.findTypeResolver(config, ac, baseType);

        /* Ok: if there is no explicit type info handler, we may want to
         * use a default. If so, config object knows what to use.
         */
        Collection<NamedType> subtypes = null;
        if (b == null) {
            b = config.getDefaultTyper(baseType);
            if (b == null) {
                return null;
            }
        } else {
            subtypes = config.getSubtypeResolver().collectAndResolveSubtypes(ac, config, ai);
        }
        // [JACKSON-505]: May need to figure out default implementation, if none found yet
        // (note: check for abstract type is not 100% mandatory, more of an optimization)
        if ((b.getDefaultImpl() == null) && baseType.isAbstract()) {
            JavaType defaultType = mapAbstractType(config, baseType);
            if (defaultType != null && defaultType.getRawClass() != baseType.getRawClass()) {
                b = b.defaultImpl(defaultType.getRawClass());
            }
        }
        return b.buildTypeDeserializer(config, baseType, subtypes, property);
    }

    @Override
    public KeyDeserializer createKeyDeserializer(DeserializationContext ctxt,
            JavaType type, BeanProperty property)
        throws JsonMappingException
    {
        final DeserializationConfig config = ctxt.getConfig();
        if (_factoryConfig.hasKeyDeserializers()) {
            BeanDescription beanDesc = config.introspectClassAnnotations(type.getRawClass());
            for (KeyDeserializers d  : _factoryConfig.keyDeserializers()) {
                KeyDeserializer deser = d.findKeyDeserializer(type, config, beanDesc, property);
                if (deser != null) {
                    return deser;
                }
            }
        }
        // and if none found, standard ones:
        Class<?> raw = type.getRawClass();
        if (raw == String.class || raw == Object.class) {
            return StdKeyDeserializers.constructStringKeyDeserializer(config, type);
        }
        // Most other keys are for limited number of static types
        KeyDeserializer kdes = _keyDeserializers.get(type);
        if (kdes != null) {
            return kdes;
        }
        // And then other one-offs; first, Enum:
        if (type.isEnumType()) {
            return _createEnumKeyDeserializer(ctxt, type, property);
        }
        // One more thing: can we find ctor(String) or valueOf(String)?
        kdes = StdKeyDeserializers.findStringBasedKeyDeserializer(config, type);
        return kdes;
    }

    private KeyDeserializer _createEnumKeyDeserializer(DeserializationContext ctxt,
            JavaType type, BeanProperty property)
        throws JsonMappingException
    {
        final DeserializationConfig config = ctxt.getConfig();
        BeanDescription beanDesc = config.introspect(type);
        JsonDeserializer<?> des = findDeserializerFromAnnotation(ctxt, beanDesc.getClassInfo(), property);
        if (des != null) {
            return StdKeyDeserializers.constructDelegatingKeyDeserializer(config, type, des);
        }
        Class<?> enumClass = type.getRawClass();
        // 23-Nov-2010, tatu: Custom deserializer?
        JsonDeserializer<?> custom = _findCustomEnumDeserializer(enumClass, config, beanDesc, property);
        if (custom != null) {
            return StdKeyDeserializers.constructDelegatingKeyDeserializer(config, type, des);
        }

        EnumResolver<?> enumRes = constructEnumResolver(enumClass, config, beanDesc.findJsonValueMethod());
        // [JACKSON-193] May have @JsonCreator for static factory method:
        for (AnnotatedMethod factory : beanDesc.getFactoryMethods()) {
            if (config.getAnnotationIntrospector().hasCreatorAnnotation(factory)) {
                int argCount = factory.getParameterCount();
                if (argCount == 1) {
                    Class<?> returnType = factory.getRawType();
                    // usually should be class, but may be just plain Enum<?> (for Enum.valueOf()?)
                    if (returnType.isAssignableFrom(enumClass)) {
                        // note: mostly copied from 'EnumDeserializer.deserializerForCreator(...)'
                        if (factory.getParameterType(0) != String.class) {
                            throw new IllegalArgumentException("Parameter #0 type for factory method ("+factory+") not suitable, must be java.lang.String");
                        }
                        if (config.canOverrideAccessModifiers()) {
                            ClassUtil.checkAndFixAccess(factory.getMember());
                        }
                        return StdKeyDeserializers.constructEnumKeyDeserializer(enumRes, factory);
                    }
                }
                throw new IllegalArgumentException("Unsuitable method ("+factory+") decorated with @JsonCreator (for Enum type "
                        +enumClass.getName()+")");
            }
        }
        // [JACKSON-749] Also, need to consider @JsonValue, if one found
        return StdKeyDeserializers.constructEnumKeyDeserializer(enumRes);
    }
    
    /*
    /**********************************************************
    /* Extended API
    /**********************************************************
     */

    /**
     * Method called to create a type information deserializer for values of
     * given non-container property, if one is needed.
     * If not needed (no polymorphic handling configured for property), should return null.
     *<p>
     * Note that this method is only called for non-container bean properties,
     * and not for values in container types or root values (or container properties)
     *
     * @param baseType Declared base type of the value to deserializer (actual
     *    deserializer type will be this type or its subtype)
     * 
     * @return Type deserializer to use for given base type, if one is needed; null if not.
     */
    public TypeDeserializer findPropertyTypeDeserializer(DeserializationConfig config, JavaType baseType,
           AnnotatedMember annotated, BeanProperty property)
        throws JsonMappingException
    {
        AnnotationIntrospector ai = config.getAnnotationIntrospector();
        TypeResolverBuilder<?> b = ai.findPropertyTypeResolver(config, annotated, baseType);        
        // Defaulting: if no annotations on member, check value class
        if (b == null) {
            return findTypeDeserializer(config, baseType, property);
        }
        // but if annotations found, may need to resolve subtypes:
        Collection<NamedType> subtypes = config.getSubtypeResolver().collectAndResolveSubtypes(annotated, config, ai);
        return b.buildTypeDeserializer(config, baseType, subtypes, property);
    }
    
    /**
     * Method called to find and create a type information deserializer for values of
     * given container (list, array, map) property, if one is needed.
     * If not needed (no polymorphic handling configured for property), should return null.
     *<p>
     * Note that this method is only called for container bean properties,
     * and not for values in container types or root values (or non-container properties)
     * 
     * @param containerType Type of property; must be a container type
     * @param propertyEntity Field or method that contains container property
     */    
    public TypeDeserializer findPropertyContentTypeDeserializer(DeserializationConfig config, JavaType containerType,
            AnnotatedMember propertyEntity, BeanProperty property)
        throws JsonMappingException
    {
        AnnotationIntrospector ai = config.getAnnotationIntrospector();
        TypeResolverBuilder<?> b = ai.findPropertyContentTypeResolver(config, propertyEntity, containerType);        
        JavaType contentType = containerType.getContentType();
        // Defaulting: if no annotations on member, check class
        if (b == null) {
            return findTypeDeserializer(config, contentType, property);
        }
        // but if annotations found, may need to resolve subtypes:
        Collection<NamedType> subtypes = config.getSubtypeResolver().collectAndResolveSubtypes(propertyEntity, config, ai);
        return b.buildTypeDeserializer(config, contentType, subtypes, property);
    }
    
    /*
    /**********************************************************
    /* Helper methods, value/content/key type introspection
    /**********************************************************
     */
    
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
     * Helper method used to resolve method return types and field
     * types. The main trick here is that the containing bean may
     * have type variable binding information (when deserializing
     * using generic type passed as type reference), which is
     * needed in some cases.
     */
    protected JavaType resolveType(DeserializationContext ctxt,
            BeanDescription beanDesc, JavaType type, AnnotatedMember member,
            BeanProperty property)                    
        throws JsonMappingException
    {
        // [JACKSON-154]: Also need to handle keyUsing, contentUsing
        if (type.isContainerType()) {
            AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
            JavaType keyType = type.getKeyType();
            if (keyType != null) {
                Object kdDef = intr.findKeyDeserializer(member);
                KeyDeserializer kd = ctxt.keyDeserializerInstance(member, property, kdDef);
                if (kd != null) {
                    type = ((MapLikeType) type).withKeyValueHandler(kd);
                    keyType = type.getKeyType(); // just in case it's used below
                }
            }
            // and all container types have content types...
            Object cdDef = intr.findContentDeserializer(member);
            JsonDeserializer<?> cd = ctxt.deserializerInstance(member, property, cdDef);
            if (cd != null) {
                type = type.withContentValueHandler(cd);
            }
            /* 04-Feb-2010, tatu: Need to figure out JAXB annotations that indicate type
             *    information to use for polymorphic members; and specifically types for
             *    collection values (contents).
             *    ... but only applies to members (fields, methods), not classes
             */
            if (member instanceof AnnotatedMember) {
            	TypeDeserializer contentTypeDeser = findPropertyContentTypeDeserializer(
            	        ctxt.getConfig(), type, (AnnotatedMember) member, property);            	
            	if (contentTypeDeser != null) {
            	    type = type.withContentTypeHandler(contentTypeDeser);
            	}
            }
        }
    	TypeDeserializer valueTypeDeser;

        if (member instanceof AnnotatedMember) { // JAXB allows per-property annotations
            valueTypeDeser = findPropertyTypeDeserializer(ctxt.getConfig(),
                    type, (AnnotatedMember) member, property);
        } else { // classes just have Jackson annotations
            // probably only occurs if 'property' is null anyway
            valueTypeDeser = findTypeDeserializer(ctxt.getConfig(), type, null);
        }
    	if (valueTypeDeser != null) {
            type = type.withTypeHandler(valueTypeDeser);
    	}
    	return type;
    }
    
    public ValueInstantiator valueInstantiatorInstance(DeserializationConfig config,
            Annotated annotated, Object instDef)
        throws JsonMappingException
    {
        if (instDef == null) {
            return null;
        }

        ValueInstantiator inst;
        
        if (instDef instanceof ValueInstantiator) {
            inst = (ValueInstantiator) instDef;
        } else {
            if (!(instDef instanceof Class)) {
                throw new IllegalStateException("AnnotationIntrospector returned key deserializer definition of type "
                        +instDef.getClass().getName()
                        +"; expected type KeyDeserializer or Class<KeyDeserializer> instead");
            }
            Class<?> instClass = (Class<?>)instDef;
            if (instClass == NoClass.class) {
                return null;
            }
            if (!ValueInstantiator.class.isAssignableFrom(instClass)) {
                throw new IllegalStateException("AnnotationIntrospector returned Class "+instClass.getName()
                        +"; expected Class<ValueInstantiator>");
            }
            HandlerInstantiator hi = config.getHandlerInstantiator();
            if (hi != null) {
                inst = hi.valueInstantiatorInstance(config, annotated, instClass);
            } else {
                inst = (ValueInstantiator) ClassUtil.createInstance(instClass,
                        config.canOverrideAccessModifiers());
            }
        }
        // not resolvable or contextual, just return:
        return inst;
    }
    
    protected EnumResolver<?> constructEnumResolver(Class<?> enumClass, DeserializationConfig config,
            AnnotatedMethod jsonValueMethod)
    {
        if (jsonValueMethod != null) {
            Method accessor = jsonValueMethod.getAnnotated();
            if (config.canOverrideAccessModifiers()) {
                ClassUtil.checkAndFixAccess(accessor);
            }
            return EnumResolver.constructUnsafeUsingMethod(enumClass, accessor);
        }
        // [JACKSON-212]: may need to use Enum.toString()
        if (config.isEnabled(DeserializationConfig.Feature.READ_ENUMS_USING_TO_STRING)) {
            return EnumResolver.constructUnsafeUsingToString(enumClass);
        }
        return EnumResolver.constructUnsafe(enumClass, config.getAnnotationIntrospector());
    }

    protected AnnotatedMethod _findJsonValueFor(DeserializationConfig config, JavaType enumType)
    {
        if (enumType == null) {
            return null;
        }
        BeanDescription beanDesc = config.introspect(enumType);
        return beanDesc.findJsonValueMethod();
    }
}
