package com.fasterxml.jackson.databind;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URL;
import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.SegmentedStringWriter;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.core.type.ResolvedType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.*;
import com.fasterxml.jackson.databind.cfg.BaseSettings;
import com.fasterxml.jackson.databind.cfg.DatabindVersion;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.deser.*;
import com.fasterxml.jackson.databind.introspect.BasicClassIntrospector;
import com.fasterxml.jackson.databind.introspect.ClassIntrospector;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.databind.jsonschema.JsonSchema;
import com.fasterxml.jackson.databind.jsontype.*;
import com.fasterxml.jackson.databind.jsontype.impl.StdSubtypeResolver;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.databind.ser.*;
import com.fasterxml.jackson.databind.type.ClassKey;
import com.fasterxml.jackson.databind.type.SimpleType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.type.TypeModifier;
import com.fasterxml.jackson.databind.util.RootNameLookup;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.databind.util.TokenBuffer;

/**
 * This mapper (or, data binder, or codec) provides functionality for
 * converting between Java objects (instances of JDK provided core classes,
 * beans), and matching JSON constructs.
 * It will use instances of {@link JsonParser} and {@link JsonGenerator}
 * for implementing actual reading/writing of JSON.
 *<p>
 * The main conversion API is defined in {@link ObjectCodec}, so that
 * implementation details of this class need not be exposed to
 * streaming parser and generator classes.
 *<p>
 * Note on caching: root-level deserializers are always cached, and accessed
 * using full (generics-aware) type information. This is different from
 * caching of referenced types, which is more limited and is done only
 * for a subset of all deserializer types. The main reason for difference
 * is that at root-level there is no incoming reference (and hence no
 * referencing property, no referral information or annotations to
 * produce differing deserializers), and that the performance impact
 * greatest at root level (since it'll essentially cache the full
 * graph of deserializers involved).
 */
public class ObjectMapper
    extends ObjectCodec
    implements Versioned
{
    /*
    /**********************************************************
    /* Helper classes, enums
    /**********************************************************
     */

    /**
     * Enumeration used with {@link ObjectMapper#enableDefaultTyping()}
     * to specify what kind of types (classes) default typing should
     * be used for. It will only be used if no explicit type information
     * is found, but this enumeration further limits subset of those types.
     */
    public enum DefaultTyping {
        /**
         * This value means that only properties that have
         * {@link java.lang.Object} as declared type (including
         * generic types without explicit type) will use default
         * typing.
         */
        JAVA_LANG_OBJECT,
        
        /**
         * Value that means that default typing will be used for
         * properties with declared type of {@link java.lang.Object}
         * or an abstract type (abstract class or interface).
         * Note that this does <b>not</b> include array types.
         */
        OBJECT_AND_NON_CONCRETE,

        /**
         * Value that means that default typing will be used for
         * all types covered by {@link #OBJECT_AND_NON_CONCRETE}
         * plus all array types for them.
         */
        NON_CONCRETE_AND_ARRAYS,
        
        /**
         * Value that means that default typing will be used for
         * all non-final types, with exception of small number of
         * "natural" types (String, Boolean, Integer, Double), which
         * can be correctly inferred from JSON; as well as for
         * all arrays of non-final types.
         */
        NON_FINAL
    }

    /**
     * Customized {@link TypeResolverBuilder} that provides type resolver builders
     * used with so-called "default typing"
     * (see {@link ObjectMapper#enableDefaultTyping()} for details).
     *<p>
     * Type resolver construction is based on configuration: implementation takes care
     * of only providing builders in cases where type information should be applied.
     * This is important since build calls may be sent for any and all types, and
     * type information should NOT be applied to all of them.
     */
    public static class DefaultTypeResolverBuilder
        extends StdTypeResolverBuilder
    {
        /**
         * Definition of what types is this default typer valid for.
         */
        protected final DefaultTyping _appliesFor;

        public DefaultTypeResolverBuilder(DefaultTyping t) {
            _appliesFor = t;
        }

        @Override
        public TypeDeserializer buildTypeDeserializer(DeserializationConfig config,
                JavaType baseType, Collection<NamedType> subtypes)
        {
            return useForType(baseType) ? super.buildTypeDeserializer(config, baseType, subtypes) : null;
        }

        @Override
        public TypeSerializer buildTypeSerializer(SerializationConfig config,
                JavaType baseType, Collection<NamedType> subtypes)
        {
            return useForType(baseType) ? super.buildTypeSerializer(config, baseType, subtypes) : null;            
        }

        /**
         * Method called to check if the default type handler should be
         * used for given type.
         * Note: "natural types" (String, Boolean, Integer, Double) will never
         * use typing; that is both due to them being concrete and final,
         * and since actual serializers and deserializers will also ignore any
         * attempts to enforce typing.
         */
        public boolean useForType(JavaType t)
        {
            switch (_appliesFor) {
            case NON_CONCRETE_AND_ARRAYS:
                if (t.isArrayType()) {
                    t = t.getContentType();
                }
                // fall through
            case OBJECT_AND_NON_CONCRETE:
                return (t.getRawClass() == Object.class) || !t.isConcrete();
            case NON_FINAL:
                if (t.isArrayType()) {
                    t = t.getContentType();
                }
                return !t.isFinal(); // includes Object.class
            default:
            //case JAVA_LANG_OBJECT:
                return (t.getRawClass() == Object.class);
            }
        }
    }

    /*
    /**********************************************************
    /* Internal constants, singletons
    /**********************************************************
     */
    
    // Quick little shortcut, to avoid having to use global TypeFactory instance...
    private final static JavaType JSON_NODE_TYPE = SimpleType.constructUnsafe(JsonNode.class);

    /* !!! 03-Apr-2009, tatu: Should try to avoid direct reference... but not
     *   sure what'd be simple and elegant way. So until then:
     */
    protected final static ClassIntrospector DEFAULT_INTROSPECTOR = BasicClassIntrospector.instance;

    // 16-May-2009, tatu: Ditto ^^^
    protected final static AnnotationIntrospector DEFAULT_ANNOTATION_INTROSPECTOR = new JacksonAnnotationIntrospector();

    protected final static VisibilityChecker<?> STD_VISIBILITY_CHECKER = VisibilityChecker.Std.defaultInstance();

    /**
     * Base settings contain defaults used for all {@link ObjectMapper}
     * instances.
     */
    protected final static BaseSettings DEFAULT_BASE = new BaseSettings(DEFAULT_INTROSPECTOR,
            DEFAULT_ANNOTATION_INTROSPECTOR, STD_VISIBILITY_CHECKER, null, TypeFactory.defaultInstance(),
            null, StdDateFormat.instance, null,
            Locale.getDefault(),
//            TimeZone.getDefault()
            TimeZone.getTimeZone("GMT")
            );
    
    /*
    /**********************************************************
    /* Configuration settings, shared
    /**********************************************************
     */

    /**
     * Factory used to create {@link JsonParser} and {@link JsonGenerator}
     * instances as necessary.
     */
    protected final JsonFactory _jsonFactory;

    /**
     * Specific factory used for creating {@link JavaType} instances;
     * needed to allow modules to add more custom type handling
     * (mostly to support types of non-Java JVM languages)
     */
    protected TypeFactory _typeFactory;

    /**
     * Provider for values to inject in deserialized POJOs.
     */
    protected InjectableValues _injectableValues;

    /**
     * Thing used for registering sub-types, resolving them to
     * super/sub-types as needed.
     */
    protected SubtypeResolver _subtypeResolver;

    /**
     * Cache for root names used when root-wrapping is enabled.
     */
    protected final RootNameLookup _rootNames;
    
    /*
    /**********************************************************
    /* Configuration settings: mix-in annotations
    /**********************************************************
     */
    
    /**
     * Mapping that defines how to apply mix-in annotations: key is
     * the type to received additional annotations, and value is the
     * type that has annotations to "mix in".
     *<p>
     * Annotations associated with the value classes will be used to
     * override annotations of the key class, associated with the
     * same field or method. They can be further masked by sub-classes:
     * you can think of it as injecting annotations between the target
     * class and its sub-classes (or interfaces)
     */
    protected final HashMap<ClassKey,Class<?>> _mixInAnnotations
        = new HashMap<ClassKey,Class<?>>();
    
    /*
    /**********************************************************
    /* Configuration settings, serialization
    /**********************************************************
     */

    /**
     * Configuration object that defines basic global
     * settings for the serialization process
     */
    protected SerializationConfig _serializationConfig;

    /**
     * Object that manages access to serializers used for serialization,
     * including caching.
     * It is configured with {@link #_serializerFactory} to allow
     * for constructing custom serializers.
     *<p>
     * Note: while serializers are only exposed {@link SerializerProvider},
     * mappers and readers need to access additional API defined by
     * {@link DefaultSerializerProvider}
     */
    protected DefaultSerializerProvider _serializerProvider;

    /**
     * Serializer factory used for constructing serializers.
     */
    protected SerializerFactory _serializerFactory;

    /*
    /**********************************************************
    /* Configuration settings, deserialization
    /**********************************************************
     */

    /**
     * Configuration object that defines basic global
     * settings for the serialization process
     */
    protected DeserializationConfig _deserializationConfig;

    /**
     * Blueprint context object; stored here to allow custom
     * sub-classes. Contains references to objects needed for
     * deserialization construction (cache, factory).
     */
    protected DefaultDeserializationContext _deserializationContext;

    /*
    /**********************************************************
    /* Caching
    /**********************************************************
     */

    /* Note: handling of serializers and deserializers is not symmetric;
     * and as a result, only root-level deserializers can be cached here.
     * This is mostly because typing and resolution for deserializers is
     * fully static; whereas it is quite dynamic for serialization.
     */

    /**
     * We will use a separate main-level Map for keeping track
     * of root-level deserializers. This is where most succesful
     * cache lookups get resolved.
     * Map will contain resolvers for all kinds of types, including
     * container types: this is different from the component cache
     * which will only cache bean deserializers.
     *<p>
     * Given that we don't expect much concurrency for additions
     * (should very quickly converge to zero after startup), let's
     * explicitly define a low concurrency setting.
     *<p>
     * Since version 1.5, these may are either "raw" deserializers (when
     * no type information is needed for base type), or type-wrapped
     * deserializers (if it is needed)
     */
    final protected ConcurrentHashMap<JavaType, JsonDeserializer<Object>> _rootDeserializers
        = new ConcurrentHashMap<JavaType, JsonDeserializer<Object>>(64, 0.6f, 2);

    /*
    /**********************************************************
    /* Life-cycle: constructing instance
    /**********************************************************
     */

    /**
     * Default constructor, which will construct the default
     * {@link JsonFactory} as necessary, use
     * {@link SerializerProvider} as its
     * {@link SerializerProvider}, and
     * {@link BeanSerializerFactory} as its
     * {@link SerializerFactory}.
     * This means that it
     * can serialize all standard JDK types, as well as regular
     * Java Beans (based on method names and Jackson-specific annotations),
     * but does not support JAXB annotations.
     */
    public ObjectMapper()
    {
        this(null, null, null);
    }

    /**
     * Constructs instance that uses specified {@link JsonFactory}
     * for constructing necessary {@link JsonParser}s and/or
     * {@link JsonGenerator}s.
     */
    public ObjectMapper(JsonFactory jf)
    {
        this(jf, null, null);
    }

    /**
     * Constructs instance that uses specified {@link JsonFactory}
     * for constructing necessary {@link JsonParser}s and/or
     * {@link JsonGenerator}s, and uses given providers for accessing
     * serializers and deserializers.
     * 
     * @param jf JsonFactory to use: if null, a new {@link MappingJsonFactory} will be constructed
     * @param sp SerializerProvider to use: if null, a {@link SerializerProvider} will be constructed
     * @param dc Blueprint deserialization context instance to use for creating
     *    actual context objects; if null, will construct standard
     *    {@link DeserializationContext}
     */
    public ObjectMapper(JsonFactory jf,
            DefaultSerializerProvider sp, DefaultDeserializationContext dc)
    {
        /* 02-Mar-2009, tatu: Important: we MUST default to using
         *   the mapping factory, otherwise tree serialization will
         *   have problems with POJONodes.
         * 03-Jan-2010, tatu: and obviously we also must pass 'this',
         *    to create actual linking.
         */
        if (jf == null) {
            _jsonFactory = new MappingJsonFactory(this);
        } else {
            _jsonFactory = jf;
            if (jf.getCodec() == null) { // as per [JACKSON-741]
                _jsonFactory.setCodec(this);
            }
        }
        _subtypeResolver = new StdSubtypeResolver();
        _rootNames = new RootNameLookup();
        // and default type factory is shared one
        _typeFactory = TypeFactory.defaultInstance();
        _serializationConfig = new SerializationConfig(DEFAULT_BASE,
                    _subtypeResolver, _mixInAnnotations);
        _deserializationConfig = new DeserializationConfig(DEFAULT_BASE,
                    _subtypeResolver, _mixInAnnotations);
        _serializerProvider = (sp == null) ? new DefaultSerializerProvider.Impl() : sp;
        _deserializationContext = (dc == null) ?
                new DefaultDeserializationContext.Impl(BeanDeserializerFactory.instance) : dc;

        // Default serializer factory is stateless, can just assign
        _serializerFactory = BeanSerializerFactory.instance;
    }
    
    /*
    /**********************************************************
    /* Versioned impl
    /**********************************************************
     */
    
    /**
     * Method that will return version information stored in and read from jar
     * that contains this class.
     */
    @Override
    public Version version() {
        return DatabindVersion.instance.version();
    }
    
    /*
    /**********************************************************
    /* Module registration
    /**********************************************************
     */

    /**
     * Method for registering a module that can extend functionality
     * provided by this mapper; for example, by adding providers for
     * custom serializers and deserializers.
     * 
     * @param module Module to register
     */
    public ObjectMapper registerModule(Module module)
    {
        /* Let's ensure we have access to name and version information, 
         * even if we do not have immediate use for either. This way we know
         * that they will be available from beginning
         */
        String name = module.getModuleName();
        if (name == null) {
            throw new IllegalArgumentException("Module without defined name");
        }
        Version version = module.version();
        if (version == null) {
            throw new IllegalArgumentException("Module without defined version");
        }

        final ObjectMapper mapper = this;
        
        // And then call registration
        module.setupModule(new Module.SetupContext()
        {
            // // // Accessors

            @Override
            public Version getMapperVersion() {
                return version();
            }

            @SuppressWarnings("unchecked")
            @Override
            public <C extends ObjectCodec> C getOwner() {
                // why do we need the cast here?!?
                return (C) mapper;
            }

            @Override
            public TypeFactory getTypeFactory() {
                return _typeFactory;
            }
            
            @Override
            public boolean isEnabled(MapperFeature f) {
                return mapper.isEnabled(f);
            }

            @Override
            public boolean isEnabled(DeserializationFeature f) {
                return mapper.isEnabled(f);
            }
            
            @Override
            public boolean isEnabled(SerializationFeature f) {
                return mapper.isEnabled(f);
            }

            @Override
            public boolean isEnabled(JsonFactory.Feature f) {
                return mapper.isEnabled(f);
            }

            @Override
            public boolean isEnabled(JsonParser.Feature f) {
                return mapper.isEnabled(f);
            }
            
            @Override
            public boolean isEnabled(JsonGenerator.Feature f) {
                return mapper.isEnabled(f);
            }
            
            // // // Methods for registering handlers: deserializers
            
            @Override
            public void addDeserializers(Deserializers d) {
                DeserializerFactory df = mapper._deserializationContext._factory.withAdditionalDeserializers(d);
                mapper._deserializationContext = mapper._deserializationContext.with(df);
            }

            @Override
            public void addKeyDeserializers(KeyDeserializers d) {
                DeserializerFactory df = mapper._deserializationContext._factory.withAdditionalKeyDeserializers(d);
                mapper._deserializationContext = mapper._deserializationContext.with(df);
            }

            @Override
            public void addBeanDeserializerModifier(BeanDeserializerModifier modifier) {
                DeserializerFactory df = mapper._deserializationContext._factory.withDeserializerModifier(modifier);
                mapper._deserializationContext = mapper._deserializationContext.with(df);
            }
            
            // // // Methods for registering handlers: serializers
            
            @Override
            public void addSerializers(Serializers s) {
                mapper._serializerFactory = mapper._serializerFactory.withAdditionalSerializers(s);
            }

            @Override
            public void addKeySerializers(Serializers s) {
                mapper._serializerFactory = mapper._serializerFactory.withAdditionalKeySerializers(s);
            }
            
            @Override
            public void addBeanSerializerModifier(BeanSerializerModifier modifier) {
                mapper._serializerFactory = mapper._serializerFactory.withSerializerModifier(modifier);
            }

            // // // Methods for registering handlers: other
            
            @Override
            public void addAbstractTypeResolver(AbstractTypeResolver resolver) {
                DeserializerFactory df = mapper._deserializationContext._factory.withAbstractTypeResolver(resolver);
                mapper._deserializationContext = mapper._deserializationContext.with(df);
            }

            @Override
            public void addTypeModifier(TypeModifier modifier) {
                TypeFactory f = mapper._typeFactory;
                f = f.withModifier(modifier);
                mapper.setTypeFactory(f);
            }

            @Override
            public void addValueInstantiators(ValueInstantiators instantiators) {
                DeserializerFactory df = mapper._deserializationContext._factory.withValueInstantiators(instantiators);
                mapper._deserializationContext = mapper._deserializationContext.with(df);
            }
            
            @Override
            public void insertAnnotationIntrospector(AnnotationIntrospector ai) {
                mapper._deserializationConfig = mapper._deserializationConfig.withInsertedAnnotationIntrospector(ai);
                mapper._serializationConfig = mapper._serializationConfig.withInsertedAnnotationIntrospector(ai);
            }
            
            @Override
            public void appendAnnotationIntrospector(AnnotationIntrospector ai) {
                mapper._deserializationConfig = mapper._deserializationConfig.withAppendedAnnotationIntrospector(ai);
                mapper._serializationConfig = mapper._serializationConfig.withAppendedAnnotationIntrospector(ai);
            }

            @Override
            public void registerSubtypes(Class<?>... subtypes) {
                mapper.registerSubtypes(subtypes);
            }

            @Override
            public void registerSubtypes(NamedType... subtypes) {
                mapper.registerSubtypes(subtypes);
            }
            
            @Override
            public void setMixInAnnotations(Class<?> target, Class<?> mixinSource) {
                mapper.addMixInAnnotations(target, mixinSource);
            }
            
            @Override
            public void addDeserializationProblemHandler(DeserializationProblemHandler handler) {
                mapper.addHandler(handler);
            }
        });
        return this;
    }

    /*
    /**********************************************************
    /* Configuration: main config object access
    /**********************************************************
     */

    /**
     * Method that returns the shared default {@link SerializationConfig}
     * object that defines configuration settings for serialization.
     * Returned object is "live" meaning that changes will be used
     * for future serialization operations for this mapper when using
     * mapper's default configuration
     */
    public SerializationConfig getSerializationConfig() {
        return _serializationConfig;
    }

    /**
     * Method that returns
     * the shared default {@link DeserializationConfig} object
     * that defines configuration settings for deserialization.
     * Returned object is "live" meaning that changes will be used
     * for future deserialization operations for this mapper when using
     * mapper's default configuration
     */
    public DeserializationConfig getDeserializationConfig() {
        return _deserializationConfig;
    }
    
    /*
    /**********************************************************
    /* Configuration: ser/deser factory, provider access
    /**********************************************************
     */
    
    /**
     * Method for setting specific {@link SerializerFactory} to use
     * for constructing (bean) serializers.
     */
    public ObjectMapper setSerializerFactory(SerializerFactory f) {
        _serializerFactory = f;
        return this;
    }

    /**
     * Method for setting specific {@link SerializerProvider} to use
     * for handling caching of {@link JsonSerializer} instances.
     */
    public ObjectMapper setSerializerProvider(DefaultSerializerProvider p) {
        _serializerProvider = p;
        return this;
    }

    public SerializerProvider getSerializerProvider() {
        return _serializerProvider;
    }
    
    /*
    /**********************************************************
    /* Configuration: mix-in annotations
    /**********************************************************
     */
    
    /**
     * Method to use for defining mix-in annotations to use for augmenting
     * annotations that processable (serializable / deserializable)
     * classes have.
     * Mixing in is done when introspecting class annotations and properties.
     * Map passed contains keys that are target classes (ones to augment
     * with new annotation overrides), and values that are source classes
     * (have annotations to use for augmentation).
     * Annotations from source classes (and their supertypes)
     * will <b>override</b>
     * annotations that target classes (and their super-types) have.
     */
    public final void setMixInAnnotations(Map<Class<?>, Class<?>> sourceMixins)
    {
        _mixInAnnotations.clear();
        if (sourceMixins != null && sourceMixins.size() > 0) {
            for (Map.Entry<Class<?>,Class<?>> en : sourceMixins.entrySet()) {
                _mixInAnnotations.put(new ClassKey(en.getKey()), en.getValue());
            }
        }
    }

    /**
     * Method to use for adding mix-in annotations to use for augmenting
     * specified class or interface. All annotations from
     * <code>mixinSource</code> are taken to override annotations
     * that <code>target</code> (or its supertypes) has.
     *
     * @param target Class (or interface) whose annotations to effectively override
     * @param mixinSource Class (or interface) whose annotations are to
     *   be "added" to target's annotations, overriding as necessary
     */
    public final void addMixInAnnotations(Class<?> target, Class<?> mixinSource)
    {
        _mixInAnnotations.put(new ClassKey(target), mixinSource);
    }

    public final Class<?> findMixInClassFor(Class<?> cls) {
        return (_mixInAnnotations == null) ? null : _mixInAnnotations.get(new ClassKey(cls));
    }

    public final int mixInCount() {
        return (_mixInAnnotations == null) ? 0 : _mixInAnnotations.size();
    }
    
    /*
    /**********************************************************
    /* Configuration, introspection
    /**********************************************************
     */

    /**
     * Method for accessing currently configured visibility checker;
     * object used for determining whether given property element
     * (method, field, constructor) can be auto-detected or not.
     */
    public VisibilityChecker<?> getVisibilityChecker() {
        return _serializationConfig.getDefaultVisibilityChecker();
    }

    /**
     * Method for setting currently configured visibility checker;
     * object used for determining whether given property element
     * (method, field, constructor) can be auto-detected or not.
     * This default checker is used if no per-class overrides
     * are defined.
     */    
    public void setVisibilityChecker(VisibilityChecker<?> vc) {
        _deserializationConfig = _deserializationConfig.with(vc);
        _serializationConfig = _serializationConfig.with(vc);
    }

    /**
     * Convenience method that allows changing configuration for
     * underlying {@link VisibilityChecker}s, to change details of what kinds of
     * properties are auto-detected.
     * Basically short cut for doing:
     *<pre>
     *  mapper.setVisibilityChecker(
     *     mapper.getVisibilityChecker().withVisibility(forMethod, visibility)
     *  );
     *</pre>
     * one common use case would be to do:
     *<pre>
     *  mapper.setVisibility(JsonMethod.FIELD, Visibility.ANY);
     *</pre>
     * which would make all member fields serializable without further annotations,
     * instead of just public fields (default setting).
     * 
     * @param forMethod Type of property descriptor affected (field, getter/isGetter,
     *     setter, creator)
     * @param visibility Minimum visibility to require for the property descriptors of type
     * 
     * @return Modified mapper instance (that is, "this"), to allow chaining
     *    of configuration calls
     */
    public ObjectMapper setVisibility(PropertyAccessor forMethod, JsonAutoDetect.Visibility visibility)
    {
        _deserializationConfig = _deserializationConfig.withVisibility(forMethod, visibility);
        _serializationConfig = _serializationConfig.withVisibility(forMethod, visibility);
        return this;
    }
    
    /**
     * Method for accessing subtype resolver in use.
     */
    public SubtypeResolver getSubtypeResolver() {
        return _subtypeResolver;
    }

    /**
     * Method for setting custom subtype resolver to use.
     */
    public ObjectMapper setSubtypeResolver(SubtypeResolver str) {
        _subtypeResolver = str;
        _deserializationConfig = _deserializationConfig.with(str);
        _serializationConfig = _serializationConfig.with(str);
        return this;
    }

    /**
     * Method for changing {@link AnnotationIntrospector} used by this
     * mapper instance for both serialization and deserialization
     */
    public ObjectMapper setAnnotationIntrospector(AnnotationIntrospector ai) {
        _serializationConfig = _serializationConfig.with(ai);
        _deserializationConfig = _deserializationConfig.with(ai);
        return this;
    }
    
    /**
     * Method for setting custom property naming strategy to use.
     */
    public ObjectMapper setPropertyNamingStrategy(PropertyNamingStrategy s) {
        _serializationConfig = _serializationConfig.with(s);
        _deserializationConfig = _deserializationConfig.with(s);
        return this;
    }

    /**
     * Method for setting defalt POJO property inclusion strategy for serialization.
     * Equivalent to:
     *<pre>
     *  mapper.setSerializationConfig(mapper.getSerializationConfig().withSerializationInclusion(incl));
     *</pre>
     */
    public ObjectMapper setSerializationInclusion(JsonInclude.Include incl) {
        _serializationConfig = _serializationConfig.withSerializationInclusion(incl);
        return this;
    }
   
    /*
    /**********************************************************
    /* Type information configuration (1.5+)
    /**********************************************************
     */

    /**
     * Convenience method that is equivalent to calling
     *<pre>
     *  enableObjectTyping(DefaultTyping.OBJECT_AND_NON_CONCRETE);
     *</pre>
     */
    public ObjectMapper enableDefaultTyping() {
        return enableDefaultTyping(DefaultTyping.OBJECT_AND_NON_CONCRETE);
    }

    /**
     * Convenience method that is equivalent to calling
     *<pre>
     *  enableObjectTyping(dti, JsonTypeInfo.As.WRAPPER_ARRAY);
     *</pre>
     */
    public ObjectMapper enableDefaultTyping(DefaultTyping dti) {
        return enableDefaultTyping(dti, JsonTypeInfo.As.WRAPPER_ARRAY);
    }

    /**
     * Method for enabling automatic inclusion of type information, needed
     * for proper deserialization of polymorphic types (unless types
     * have been annotated with {@link com.fasterxml.jackson.annotation.JsonTypeInfo}).
     * 
     * @param applicability Defines kinds of types for which additional type information
     *    is added; see {@link DefaultTyping} for more information.
     */
    public ObjectMapper enableDefaultTyping(DefaultTyping applicability, JsonTypeInfo.As includeAs)
    {
        TypeResolverBuilder<?> typer = new DefaultTypeResolverBuilder(applicability);
        // we'll always use full class name, when using defaulting
        typer = typer.init(JsonTypeInfo.Id.CLASS, null);
        typer = typer.inclusion(includeAs);
        return setDefaultTyping(typer);
    }

    /**
     * Method for enabling automatic inclusion of type information -- needed
     * for proper deserialization of polymorphic types (unless types
     * have been annotated with {@link com.fasterxml.jackson.annotation.JsonTypeInfo}) --
     * using "As.PROPERTY" inclusion mechanism and specified property name
     * to use for inclusion (default being "@class" since default type information
     * always uses class name as type identifier)
     */
    public ObjectMapper enableDefaultTypingAsProperty(DefaultTyping applicability, String propertyName)
    {
        TypeResolverBuilder<?> typer = new DefaultTypeResolverBuilder(applicability);
        // we'll always use full class name, when using defaulting
        typer = typer.init(JsonTypeInfo.Id.CLASS, null);
        typer = typer.inclusion(JsonTypeInfo.As.PROPERTY);
        typer = typer.typeProperty(propertyName);
        return setDefaultTyping(typer);
    }
    
    /**
     * Method for disabling automatic inclusion of type information; if so, only
     * explicitly annotated types (ones with
     * {@link com.fasterxml.jackson.annotation.JsonTypeInfo}) will have
     * additional embedded type information.
     */
    public ObjectMapper disableDefaultTyping() {
        return setDefaultTyping(null);
    }

    /**
     * Method for enabling automatic inclusion of type information, using
     * specified handler object for determining which types this affects,
     * as well as details of how information is embedded.
     * 
     * @param typer Type information inclusion handler
     */
    public ObjectMapper setDefaultTyping(TypeResolverBuilder<?> typer) {
        _deserializationConfig = _deserializationConfig.with(typer);
        _serializationConfig = _serializationConfig.with(typer);
        return this;
    }

    /**
     * Method for registering specified class as a subtype, so that
     * typename-based resolution can link supertypes to subtypes
     * (as an alternative to using annotations).
     * Type for given class is determined from appropriate annotation;
     * or if missing, default name (unqualified class name)
     */
    public void registerSubtypes(Class<?>... classes) {
        getSubtypeResolver().registerSubtypes(classes);
    }

    /**
     * Method for registering specified class as a subtype, so that
     * typename-based resolution can link supertypes to subtypes
     * (as an alternative to using annotations).
     * Name may be provided as part of argument, but if not will
     * be based on annotations or use default name (unqualified
     * class name).
     */
    public void registerSubtypes(NamedType... types) {
        getSubtypeResolver().registerSubtypes(types);
    }

    /*
    /**********************************************************
    /* Configuration, basic type handling
    /**********************************************************
     */

    /**
     * Accessor for getting currently configured {@link TypeFactory} instance.
     */
    public TypeFactory getTypeFactory() {
        return _typeFactory;
    }

    /**
     * Method that can be used to override {@link TypeFactory} instance
     * used by this mapper.
     *<p>
     * Note: will also set {@link TypeFactory} that deserialization and
     * serialization config objects use.
     */
    public ObjectMapper setTypeFactory(TypeFactory f)
    {
        _typeFactory = f;
        _deserializationConfig = _deserializationConfig.with(f);
        _serializationConfig = _serializationConfig.with(f);
        return this;
    }
    
    /**
     * Convenience method for constructing {@link JavaType} out of given
     * type (typically <code>java.lang.Class</code>), but without explicit
     * context.
     */
    public JavaType constructType(Type t) {
        return _typeFactory.constructType(t);
    }
    
    /*
    /**********************************************************
    /* Configuration, deserialization
    /**********************************************************
     */
    
    /**
     * Method for specifying {@link JsonNodeFactory} to use for
     * constructing root level tree nodes (via method
     * {@link #createObjectNode}
     */
    public ObjectMapper setNodeFactory(JsonNodeFactory f) {
        _deserializationConfig = _deserializationConfig.with(f);
        return this;
    }

    /**
     * Method for adding specified {@link DeserializationProblemHandler}
     * to be used for handling specific problems during deserialization.
     */
    public ObjectMapper addHandler(DeserializationProblemHandler h) {
        _deserializationConfig = _deserializationConfig.withHandler(h);
        return this;
    }

    /**
     * Method for removing all registered {@link DeserializationProblemHandler}s
     * instances from this mapper.
     */
    public ObjectMapper clearProblemHandlers() {
        _deserializationConfig = _deserializationConfig.withNoProblemHandlers();
        return this;
    }
    
    
    /*
    /**********************************************************
    /* Configuration, serialization
    /**********************************************************
     */

    /**
     * Convenience method that is equivalent to:
     *<pre>
     *  mapper.setFilters(mapper.getSerializationConfig().withFilters(filterProvider));
     *</pre>
     *<p>
     * Note that usually it is better to use method {@link #writer(FilterProvider)};
     * however, sometimes
     * this method is more convenient. For example, some frameworks only allow configuring
     * of ObjectMapper instances and not ObjectWriters.
     */
    public void setFilters(FilterProvider filterProvider) {
        _serializationConfig = _serializationConfig.withFilters(filterProvider);
    }

    /*
    /**********************************************************
    /* Configuration, other
    /**********************************************************
     */

    /**
     * Method that can be used to get hold of {@link JsonFactory} that this
     * mapper uses if it needs to construct {@link JsonParser}s
     * and/or {@link JsonGenerator}s.
     *
     * @return {@link JsonFactory} that this mapper uses when it needs to
     *   construct Json parser and generators
     */
    public JsonFactory getJsonFactory() { return _jsonFactory; }
    
    /**
     * Method for configuring the default {@link DateFormat} to use when serializing time
     * values as Strings, and deserializing from JSON Strings.
     * This is preferably to directly modifying {@link SerializationConfig} and
     * {@link DeserializationConfig} instances.
     * If you need per-request configuration, use {@link #writer(DateFormat)} to
     * create properly configured {@link ObjectWriter} and use that; this because
     * {@link ObjectWriter}s are thread-safe whereas ObjectMapper itself is only
     * thread-safe when configuring methods (such as this one) are NOT called.
     */
    public ObjectMapper setDateFormat(DateFormat dateFormat)
    {
        _deserializationConfig = _deserializationConfig.with(dateFormat);
        _serializationConfig = _serializationConfig.with(dateFormat);
        return this;
    }

    /**
     * Method for configuring {@link HandlerInstantiator} to use for creating
     * instances of handlers (such as serializers, deserializers, type and type
     * id resolvers), given a class.
     *
     * @param hi Instantiator to use; if null, use the default implementation
     */
    public Object setHandlerInstantiator(HandlerInstantiator hi)
    {
        _deserializationConfig = _deserializationConfig.with(hi);
        _serializationConfig = _serializationConfig.with(hi);
        return this;
    }
    
    /**
     * Method for configuring {@link InjectableValues} which used to find
     * values to inject.
     */
    public ObjectMapper setInjectableValues(InjectableValues injectableValues) {
        _injectableValues = injectableValues;
        return this;
    }
    
    /**
     * Method for overriding default locale to use for formatting.
     * Default value used is {@link Locale#getDefault()}.
     */
    public ObjectMapper setLocale(Locale l) {
        _deserializationConfig = _deserializationConfig.with(l);
        _serializationConfig = _serializationConfig.with(l);
        return this;
    }

    /**
     * Method for overriding default TimeZone to use for formatting.
     * Default value used is {@link TimeZone#getDefault()}.
     */
    public ObjectMapper setTimeZone(TimeZone tz) {
        _deserializationConfig = _deserializationConfig.with(tz);
        _serializationConfig = _serializationConfig.with(tz);
        return this;
    }
    
    /*
    /**********************************************************
    /* Configuration, simple features
    /**********************************************************
     */

    /**
     * Method for changing state of an on/off mapper feature for
     * this mapper instance.
     */
    public ObjectMapper configure(MapperFeature f, boolean state) {
        _serializationConfig = state ?
                _serializationConfig.with(f) : _serializationConfig.without(f);
        _deserializationConfig = state ?
                _deserializationConfig.with(f) : _deserializationConfig.without(f);
        return this;
    }
    
    /**
     * Method for changing state of an on/off serialization feature for
     * this object mapper.
     */
    public ObjectMapper configure(SerializationFeature f, boolean state) {
        _serializationConfig = state ?
                _serializationConfig.with(f) : _serializationConfig.without(f);
        return this;
    }

    /**
     * Method for changing state of an on/off deserialization feature for
     * this object mapper.
     */
    public ObjectMapper configure(DeserializationFeature f, boolean state) {
        _deserializationConfig = state ?
                _deserializationConfig.with(f) : _deserializationConfig.without(f);
        return this;
    }

    /**
     * Method for changing state of an on/off {@link JsonParser} feature for
     * {@link JsonFactory} instance this object mapper uses.
     *<p>
     * This is method is basically a shortcut method for calling
     * {@link JsonFactory#enable} on the shared
     * {@link JsonFactory} this mapper uses (which is accessible
     * using {@link #getJsonFactory}).
     */
    public ObjectMapper configure(JsonParser.Feature f, boolean state) {
        _jsonFactory.configure(f, state);
        return this;
    }

    /**
     * Method for changing state of an on/off {@link JsonGenerator} feature for
     * {@link JsonFactory} instance this object mapper uses.
     *<p>
     * This is method is basically a shortcut method for calling
     * {@link JsonFactory#enable} on the shared
     * {@link JsonFactory} this mapper uses (which is accessible
     * using {@link #getJsonFactory}).
     */
    public ObjectMapper configure(JsonGenerator.Feature f, boolean state) {
        _jsonFactory.configure(f, state);
        return this;
    }

    /**
     * Method for enabling specified {@link MapperConfig} features.
     * Modifies and returns this instance; no new object is created.
     */
    public ObjectMapper enable(MapperFeature... f) {
        _deserializationConfig = _deserializationConfig.with(f);
        _serializationConfig = _serializationConfig.with(f);
        return this;
    }

    /**
     * Method for enabling specified {@link DeserializationConfig} features.
     * Modifies and returns this instance; no new object is created.
     */
    public ObjectMapper disable(MapperFeature... f) {
        _deserializationConfig = _deserializationConfig.without(f);
        _serializationConfig = _serializationConfig.without(f);
        return this;
    }
    
    /**
     * Method for enabling specified {@link DeserializationConfig} features.
     * Modifies and returns this instance; no new object is created.
     */
    public ObjectMapper enable(DeserializationFeature feature) {
        _deserializationConfig = _deserializationConfig.with(feature);
        return this;
    }

    /**
     * Method for enabling specified {@link DeserializationConfig} features.
     * Modifies and returns this instance; no new object is created.
     */
    public ObjectMapper enable(DeserializationFeature first,
            DeserializationFeature... f) {
        _deserializationConfig = _deserializationConfig.with(first, f);
        return this;
    }
    
    /**
     * Method for enabling specified {@link DeserializationConfig} features.
     * Modifies and returns this instance; no new object is created.
     */
    public ObjectMapper disable(DeserializationFeature feature) {
        _deserializationConfig = _deserializationConfig.without(feature);
        return this;
    }

    /**
     * Method for enabling specified {@link DeserializationConfig} features.
     * Modifies and returns this instance; no new object is created.
     */
    public ObjectMapper disable(DeserializationFeature first,
            DeserializationFeature... f) {
        _deserializationConfig = _deserializationConfig.without(first, f);
        return this;
    }
    
    /**
     * Method for enabling specified {@link DeserializationConfig} feature.
     * Modifies and returns this instance; no new object is created.
     */
    public ObjectMapper enable(SerializationFeature f) {
        _serializationConfig = _serializationConfig.with(f);
        return this;
    }

    /**
     * Method for enabling specified {@link DeserializationConfig} features.
     * Modifies and returns this instance; no new object is created.
     */
    public ObjectMapper enable(SerializationFeature first,
            SerializationFeature... f) {
        _serializationConfig = _serializationConfig.with(first, f);
        return this;
    }
    
    /**
     * Method for enabling specified {@link DeserializationConfig} features.
     * Modifies and returns this instance; no new object is created.
     */
    public ObjectMapper disable(SerializationFeature f) {
        _serializationConfig = _serializationConfig.without(f);
        return this;
    }

    /**
     * Method for enabling specified {@link DeserializationConfig} features.
     * Modifies and returns this instance; no new object is created.
     */
    public ObjectMapper disable(SerializationFeature first,
            SerializationFeature... f) {
        _serializationConfig = _serializationConfig.without(first, f);
        return this;
    }

    /**
     * Method for checking whether given Mapper
     * feature is enabled.
     */
    public boolean isEnabled(MapperFeature f) {
        // ok to use either one, should be kept in sync
        return _serializationConfig.isEnabled(f);
    }

    /**
     * Method for checking whether given serialization-specific
     * feature is enabled.
     */
    public boolean isEnabled(SerializationFeature f) {
        return _serializationConfig.isEnabled(f);
    }
    
    /**
     * Method for checking whether given deserialization-specific
     * feature is enabled.
     */
    public boolean isEnabled(DeserializationFeature f) {
        return _deserializationConfig.isEnabled(f);
    }

    /**
     * Convenience method, equivalent to:
     *<pre>
     *  getJsonFactory().isEnabled(f);
     *</pre>
     */
    public boolean isEnabled(JsonFactory.Feature f) {
        return _jsonFactory.isEnabled(f);
    }

    /**
     * Convenience method, equivalent to:
     *<pre>
     *  getJsonFactory().isEnabled(f);
     *</pre>
     */
    public boolean isEnabled(JsonParser.Feature f) {
        return _jsonFactory.isEnabled(f);
    }
    
    /**
     * Convenience method, equivalent to:
     *<pre>
     *  getJsonFactory().isEnabled(f);
     *</pre>
     */
    public boolean isEnabled(JsonGenerator.Feature f) {
        return _jsonFactory.isEnabled(f);
    }
    
    /**
     * Method that can be used to get hold of {@link JsonNodeFactory}
     * that this mapper will use when directly constructing
     * root {@link JsonNode} instances for Trees.
     *<p>
     * Note: this is just a shortcut for calling
     *<pre>
     *   getDeserializationConfig().getNodeFactory()
     *</pre>
     */
    public JsonNodeFactory getNodeFactory() {
        return _deserializationConfig.getNodeFactory();
    }

    /*
    /**********************************************************
    /* Public API (from ObjectCodec): deserialization
    /* (mapping from JSON to Java types);
    /* main methods
    /**********************************************************
     */

    /**
     * Method to deserialize JSON content into a non-container
     * type (it can be an array type, however): typically a bean, array
     * or a wrapper type (like {@link java.lang.Boolean}).
     *<p>
     * Note: this method should NOT be used if the result type is a
     * container ({@link java.util.Collection} or {@link java.util.Map}.
     * The reason is that due to type erasure, key and value types
     * can not be introspected when using this method.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T readValue(JsonParser jp, Class<T> valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readValue(getDeserializationConfig(), jp, _typeFactory.constructType(valueType));
    } 

    /**
     * Method to deserialize JSON content into a Java type, reference
     * to which is passed as argument. Type is passed using so-called
     * "super type token" (see )
     * and specifically needs to be used if the root type is a 
     * parameterized (generic) container type.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T readValue(JsonParser jp, TypeReference<?> valueTypeRef)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readValue(getDeserializationConfig(), jp, _typeFactory.constructType(valueTypeRef));
    }

    /**
     * Method to deserialize JSON content into a Java type, reference
     * to which is passed as argument. Type is passed using 
     * Jackson specific type; instance of which can be constructed using
     * {@link TypeFactory}.
     */
    @Override
    @SuppressWarnings("unchecked")
    public final <T> T readValue(JsonParser jp, ResolvedType valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readValue(getDeserializationConfig(), jp, (JavaType) valueType);
    }

    /**
     * Type-safe overloaded method, basically alias for {@link #readValues(JsonParser, ResolvedType)}.
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(JsonParser jp, JavaType valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readValue(getDeserializationConfig(), jp, valueType);
    }
    
    /**
     * Method to deserialize JSON content as tree expressed
     * using set of {@link JsonNode} instances. Returns
     * root of the resulting tree (where root can consist
     * of just a single node if the current event is a
     * value event, not container).
     */
    @Override
    public <T extends TreeNode> T readTree(JsonParser jp)
        throws IOException, JsonProcessingException
    {
        /* 02-Mar-2009, tatu: One twist; deserialization provider
         *   will map JSON null straight into Java null. But what
         *   we want to return is the "null node" instead.
         */
        /* 05-Aug-2011, tatu: Also, must check for EOF here before
         *   calling readValue(), since that'll choke on it otherwise
         */
        DeserializationConfig cfg = getDeserializationConfig();
        JsonToken t = jp.getCurrentToken();
        if (t == null) {
            t = jp.nextToken();
            if (t == null) {
                return null;
            }
        }
        JsonNode n = (JsonNode) _readValue(cfg, jp, JSON_NODE_TYPE);
        if (n == null) {
            n = getNodeFactory().nullNode();
        }
        @SuppressWarnings("unchecked")
        T result = (T) n;
        return result;
    }

    /**
     * Method for reading sequence of Objects from parser stream.
     *<p>
     * Note that {@link ObjectReader} has more complete set of variants.
     */
    @Override
    public <T> MappingIterator<T> readValues(JsonParser jp, ResolvedType valueType)
        throws IOException, JsonProcessingException
    {
        return readValues(jp, (JavaType) valueType);
    }

    /**
     * Type-safe overloaded method, basically alias for {@link #readValues(JsonParser, ResolvedType)}.
     */
    public <T> MappingIterator<T> readValues(JsonParser jp, JavaType valueType)
            throws IOException, JsonProcessingException
    {
        DeserializationConfig config = getDeserializationConfig();
        DeserializationContext ctxt = createDeserializationContext(jp, config);
        JsonDeserializer<?> deser = _findRootDeserializer(ctxt, valueType);
        // false -> do NOT close JsonParser (since caller passed it)
        return new MappingIterator<T>(valueType, jp, ctxt, deser,
                false, null);
    }

    /**
     * Method for reading sequence of Objects from parser stream.
     */
    @Override
    public <T> MappingIterator<T> readValues(JsonParser jp, Class<T> valueType)
        throws IOException, JsonProcessingException
    {
        return readValues(jp, _typeFactory.constructType(valueType));
    }

    /**
     * Method for reading sequence of Objects from parser stream.
     */
    @Override
    public <T> MappingIterator<T> readValues(JsonParser jp, TypeReference<?> valueTypeRef)
        throws IOException, JsonProcessingException
    {
        return readValues(jp, _typeFactory.constructType(valueTypeRef));
    }
    
    /*
    /**********************************************************
    /* Public API not included in ObjectCodec: deserialization
    /* (mapping from JSON to Java types)
    /**********************************************************
     */

    /**
     * Method to deserialize JSON content as tree expressed
     * using set of {@link JsonNode} instances.
     * Returns root of the resulting tree (where root can consist
     * of just a single node if the current event is a
     * value event, not container).
     *
     * @param in Input stream used to read JSON content
     *   for building the JSON tree.
     */
    public JsonNode readTree(InputStream in)
        throws IOException, JsonProcessingException
    {
        JsonNode n = (JsonNode) _readMapAndClose(_jsonFactory.createJsonParser(in), JSON_NODE_TYPE);
        return (n == null) ? NullNode.instance : n;
    }

    /**
     * Method to deserialize JSON content as tree expressed
     * using set of {@link JsonNode} instances.
     * Returns root of the resulting tree (where root can consist
     * of just a single node if the current event is a
     * value event, not container).
     *
     * @param r Reader used to read JSON content
     *   for building the JSON tree.
     */
    public JsonNode readTree(Reader r)
        throws IOException, JsonProcessingException
    {
        JsonNode n = (JsonNode) _readMapAndClose(_jsonFactory.createJsonParser(r), JSON_NODE_TYPE);
        return (n == null) ? NullNode.instance : n;
    }

    /**
     * Method to deserialize JSON content as tree expressed using set of {@link JsonNode} instances.
     * Returns root of the resulting tree (where root can consist of just a single node if the current
     * event is a value event, not container).
     *
     * @param content JSON content to parse to build the JSON tree.
     */
    public JsonNode readTree(String content)
        throws IOException, JsonProcessingException
    {
        JsonNode n = (JsonNode) _readMapAndClose(_jsonFactory.createJsonParser(content), JSON_NODE_TYPE);
        return (n == null) ? NullNode.instance : n;
    }

    /**
     * Method to deserialize JSON content as tree expressed using set of {@link JsonNode} instances.
     * Returns root of the resulting tree (where root can consist of just a single node if the current
     * event is a value event, not container).
     *
     * @param content JSON content to parse to build the JSON tree.
     */
    public JsonNode readTree(byte[] content)
        throws IOException, JsonProcessingException
    {
        JsonNode n = (JsonNode) _readMapAndClose(_jsonFactory.createJsonParser(content), JSON_NODE_TYPE);
        return (n == null) ? NullNode.instance : n;
    }
    
    /**
     * Method to deserialize JSON content as tree expressed using set of {@link JsonNode} instances.
     * Returns root of the resulting tree (where root can consist of just a single node if the current
     * event is a value event, not container).
     *
     * @param file File of which contents to parse as JSON for building a tree instance
     */
    public JsonNode readTree(File file)
        throws IOException, JsonProcessingException
    {
        JsonNode n = (JsonNode) _readMapAndClose(_jsonFactory.createJsonParser(file), JSON_NODE_TYPE);
        return (n == null) ? NullNode.instance : n;
    }

    /**
     * Method to deserialize JSON content as tree expressed using set of {@link JsonNode} instances.
     * Returns root of the resulting tree (where root can consist of just a single node if the current
     * event is a value event, not container).
     *
     * @param source URL to use for fetching contents to parse as JSON for building a tree instance
     */
    public JsonNode readTree(URL source)
        throws IOException, JsonProcessingException
    {
        JsonNode n = (JsonNode) _readMapAndClose(_jsonFactory.createJsonParser(source), JSON_NODE_TYPE);
        return (n == null) ? NullNode.instance : n;
    }

    /*
    /**********************************************************
    /* Public API (from ObjectCodec): serialization
    /* (mapping from Java types to Json)
    /**********************************************************
     */

    /**
     * Method that can be used to serialize any Java value as
     * JSON output, using provided {@link JsonGenerator}.
     */
    @Override
    public void writeValue(JsonGenerator jgen, Object value)
        throws IOException, JsonGenerationException, JsonMappingException
    {
        SerializationConfig config = getSerializationConfig();
        if (config.isEnabled(SerializationFeature.CLOSE_CLOSEABLE) && (value instanceof Closeable)) {
            _writeCloseableValue(jgen, value, config);
        } else {
            _serializerProvider(config).serializeValue(jgen, value);
            if (config.isEnabled(SerializationFeature.FLUSH_AFTER_WRITE_VALUE)) {
                jgen.flush();
            }
        }
    }

    /**
     * Method to serialize given JSON Tree, using generator
     * provided.
     */
    public void writeTree(JsonGenerator jgen, JsonNode rootNode)
        throws IOException, JsonProcessingException
    {
        SerializationConfig config = getSerializationConfig();
        _serializerProvider(config).serializeValue(jgen, rootNode);
        if (config.isEnabled(SerializationFeature.FLUSH_AFTER_WRITE_VALUE)) {
            jgen.flush();
        }
    }

    /*
    /**********************************************************
    /* Public API (from ObjectCodec): Tree Model support
    /**********************************************************
     */

    /**
     *<p>
     * Note: return type is co-variant, as basic ObjectCodec
     * abstraction can not refer to concrete node types (as it's
     * part of core package, whereas impls are part of mapper
     * package)
     */
    @Override    
    public ObjectNode createObjectNode() {
        return _deserializationConfig.getNodeFactory().objectNode();
    }

    /**
     *<p>
     * Note: return type is co-variant, as basic ObjectCodec
     * abstraction can not refer to concrete node types (as it's
     * part of core package, whereas impls are part of mapper
     * package)
     */
    @Override
    public ArrayNode createArrayNode() {
        return _deserializationConfig.getNodeFactory().arrayNode();
    }

    /**
     * Method for constructing a {@link JsonParser} out of JSON tree
     * representation.
     * 
     * @param n Root node of the tree that resulting parser will read from
     */
    @Override
    public JsonParser treeAsTokens(TreeNode n)
    {
        return new TreeTraversingParser((JsonNode) n, this);
    }

    /**
     * Convenience conversion method that will bind data given JSON tree
     * contains into specific value (usually bean) type.
     *<p>
     * Equivalent to:
     *<pre>
     *   objectMapper.convertValue(n, valueClass);
     *</pre>
     */
    @Override
    public <T> T treeToValue(TreeNode n, Class<T> valueType)
        throws JsonProcessingException
    {
        try {
            return readValue(treeAsTokens(n), valueType);
        } catch (JsonProcessingException e) {
            throw e;
        } catch (IOException e) { // should not occur, no real i/o...
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /**
     * Reverse of {@link #treeToValue}; given a value (usually bean), will
     * construct equivalent JSON Tree representation. Functionally same
     * as if serializing value into JSON and parsing JSON as tree, but
     * more efficient.
     * 
     * @param <T> Actual node type; usually either basic {@link JsonNode} or
     *  {@link com.fasterxml.jackson.databind.node.ObjectNode}
     * @param fromValue Bean value to convert
     * @return Root node of the resulting JSON tree
     */
    @SuppressWarnings("unchecked")
    public <T extends JsonNode> T valueToTree(Object fromValue)
        throws IllegalArgumentException
    {
        if (fromValue == null) return null;
        TokenBuffer buf = new TokenBuffer(this);
        JsonNode result;
        try {
            writeValue(buf, fromValue);
            JsonParser jp = buf.asParser();
            result = readTree(jp);
            jp.close();
        } catch (IOException e) { // should not occur, no real i/o...
            throw new IllegalArgumentException(e.getMessage(), e);
        }
        return (T) result;
    } 
    
    /*
    /**********************************************************
    /* Extended Public API, accessors
    /**********************************************************
     */

    /**
     * Method that can be called to check whether mapper thinks
     * it could serialize an instance of given Class.
     * Check is done
     * by checking whether a serializer can be found for the type.
     *
     * @return True if mapper can find a serializer for instances of
     *  given class (potentially serializable), false otherwise (not
     *  serializable)
     */
    public boolean canSerialize(Class<?> type) {
        return _serializerProvider(getSerializationConfig()).hasSerializerFor(type);
    }

    /**
     * Method that can be called to check whether mapper thinks
     * it could deserialize an Object of given type.
     * Check is done
     * by checking whether a deserializer can be found for the type.
     *
     * @return True if mapper can find a serializer for instances of
     *  given class (potentially serializable), false otherwise (not
     *  serializable)
     */
    public boolean canDeserialize(JavaType type)
    {
        return createDeserializationContext(null,
                getDeserializationConfig()).hasValueDeserializerFor(type);
    }

    /*
    /**********************************************************
    /* Extended Public API, deserialization,
    /* convenience methods
    /**********************************************************
     */

    @SuppressWarnings("unchecked")
    public <T> T readValue(File src, Class<T> valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
     // !!! TODO
//    	_setupClassLoaderForDeserialization(valueType);
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(src), _typeFactory.constructType(valueType));
    } 

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> T readValue(File src, TypeReference valueTypeRef)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(src), _typeFactory.constructType(valueTypeRef));
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(File src, JavaType valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(src), valueType);
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(URL src, Class<T> valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
     // !!! TODO
//    	_setupClassLoaderForDeserialization(valueType);
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(src), _typeFactory.constructType(valueType));
    } 

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> T readValue(URL src, TypeReference valueTypeRef)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(src), _typeFactory.constructType(valueTypeRef));
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(URL src, JavaType valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(src), valueType);
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(String content, Class<T> valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
     // !!! TODO
//    	_setupClassLoaderForDeserialization(valueType);
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(content), _typeFactory.constructType(valueType));
    } 

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> T readValue(String content, TypeReference valueTypeRef)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(content), _typeFactory.constructType(valueTypeRef));
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(String content, JavaType valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(content), valueType);
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(Reader src, Class<T> valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
     // !!! TODO
//    	_setupClassLoaderForDeserialization(valueType);
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(src), _typeFactory.constructType(valueType));
    } 

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> T readValue(Reader src, TypeReference valueTypeRef)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(src), _typeFactory.constructType(valueTypeRef));
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(Reader src, JavaType valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(src), valueType);
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(InputStream src, Class<T> valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
     // !!! TODO
//    	_setupClassLoaderForDeserialization(valueType);
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(src), _typeFactory.constructType(valueType));
    } 

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> T readValue(InputStream src, TypeReference valueTypeRef)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(src), _typeFactory.constructType(valueTypeRef));
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(InputStream src, JavaType valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(src), valueType);
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(byte[] src, Class<T> valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
     // !!! TODO
//      _setupClassLoaderForDeserialization(valueType);
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(src), _typeFactory.constructType(valueType));
    } 
    
    @SuppressWarnings("unchecked")
    public <T> T readValue(byte[] src, int offset, int len, 
                               Class<T> valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
     // !!! TODO
//    	_setupClassLoaderForDeserialization(valueType);
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(src, offset, len), _typeFactory.constructType(valueType));
    } 

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> T readValue(byte[] src, TypeReference valueTypeRef)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(src), _typeFactory.constructType(valueTypeRef));
    } 
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> T readValue(byte[] src, int offset, int len,
                           TypeReference valueTypeRef)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(src, offset, len), _typeFactory.constructType(valueTypeRef));
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(byte[] src, JavaType valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(src), valueType);
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(byte[] src, int offset, int len,
                           JavaType valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(src, offset, len), valueType);
    } 
    
    /*
    /**********************************************************
    /* Extended Public API: serialization
    /* (mapping from Java types to JSON)
    /**********************************************************
     */

    /**
     * Method that can be used to serialize any Java value as
     * JSON output, written to File provided.
     */
    public void writeValue(File resultFile, Object value)
        throws IOException, JsonGenerationException, JsonMappingException
    {
        _configAndWriteValue(_jsonFactory.createJsonGenerator(resultFile, JsonEncoding.UTF8), value);
    }

    /**
     * Method that can be used to serialize any Java value as
     * JSON output, using output stream provided (using encoding
     * {@link JsonEncoding#UTF8}).
     *<p>
     * Note: method does not close the underlying stream explicitly
     * here; however, {@link JsonFactory} this mapper uses may choose
     * to close the stream depending on its settings (by default,
     * it will try to close it when {@link JsonGenerator} we construct
     * is closed).
     */
    public void writeValue(OutputStream out, Object value)
        throws IOException, JsonGenerationException, JsonMappingException
    {
        _configAndWriteValue(_jsonFactory.createJsonGenerator(out, JsonEncoding.UTF8), value);
    }

    /**
     * Method that can be used to serialize any Java value as
     * JSON output, using Writer provided.
     *<p>
     * Note: method does not close the underlying stream explicitly
     * here; however, {@link JsonFactory} this mapper uses may choose
     * to close the stream depending on its settings (by default,
     * it will try to close it when {@link JsonGenerator} we construct
     * is closed).
     */
    public void writeValue(Writer w, Object value)
        throws IOException, JsonGenerationException, JsonMappingException
    {
        _configAndWriteValue(_jsonFactory.createJsonGenerator(w), value);
    }

    /**
     * Method that can be used to serialize any Java value as
     * a String. Functionally equivalent to calling
     * {@link #writeValue(Writer,Object)} with {@link java.io.StringWriter}
     * and constructing String, but more efficient.
     */
    public String writeValueAsString(Object value)
        throws IOException, JsonGenerationException, JsonMappingException
    {        
        // alas, we have to pull the recycler directly here...
        SegmentedStringWriter sw = new SegmentedStringWriter(_jsonFactory._getBufferRecycler());
        _configAndWriteValue(_jsonFactory.createJsonGenerator(sw), value);
        return sw.getAndClear();
    }
    
    /**
     * Method that can be used to serialize any Java value as
     * a byte array. Functionally equivalent to calling
     * {@link #writeValue(Writer,Object)} with {@link java.io.ByteArrayOutputStream}
     * and getting bytes, but more efficient.
     * Encoding used will be UTF-8.
     */
    public byte[] writeValueAsBytes(Object value)
        throws IOException, JsonGenerationException, JsonMappingException
    {        
        ByteArrayBuilder bb = new ByteArrayBuilder(_jsonFactory._getBufferRecycler());
        _configAndWriteValue(_jsonFactory.createJsonGenerator(bb, JsonEncoding.UTF8), value);
        byte[] result = bb.toByteArray();
        bb.release();
        return result;
    }

    /*
    /**********************************************************
    /* Extended Public API: constructing ObjectWriters
    /* for more advanced configuration
    /**********************************************************
     */

    /**
     * Convenience method for constructing {@link ObjectWriter}
     * with default settings.
     */
    public ObjectWriter writer() {
        return new ObjectWriter(this, getSerializationConfig());
    }

    /**
     * Factory method for constructing {@link ObjectWriter} with
     * specified feature enabled (compared to settings that this
     * mapper instance has).
     */
    public ObjectWriter writer(SerializationFeature feature) {
        return new ObjectWriter(this, getSerializationConfig().with(feature));
    }

    /**
     * Factory method for constructing {@link ObjectWriter} with
     * specified features enabled (compared to settings that this
     * mapper instance has).
     */
    public ObjectWriter writer(SerializationFeature first,
            SerializationFeature... other) {
        return new ObjectWriter(this, getSerializationConfig().with(first, other));
    }
    
    /**
     * Factory method for constructing {@link ObjectWriter} that will
     * serialize objects using specified {@link DateFormat}; or, if
     * null passed, using timestamp (64-bit number.
     */
    public ObjectWriter writer(DateFormat df) {
        return new ObjectWriter(this,
                getSerializationConfig().with(df));
    }
    
    /**
     * Factory method for constructing {@link ObjectWriter} that will
     * serialize objects using specified JSON View (filter).
     */
    public ObjectWriter writerWithView(Class<?> serializationView) {
        return new ObjectWriter(this, getSerializationConfig().withView(serializationView));
    }
    
    /**
     * Factory method for constructing {@link ObjectWriter} that will
     * serialize objects using specified root type, instead of actual
     * runtime type of value. Type must be a super-type of runtime
     * type.
     */
    public ObjectWriter writerWithType(Class<?> rootType) {
        JavaType t = (rootType == null) ? null : _typeFactory.constructType(rootType);
        return new ObjectWriter(this, getSerializationConfig(), t, /*PrettyPrinter*/null);
    }

    /**
     * Factory method for constructing {@link ObjectWriter} that will
     * serialize objects using specified root type, instead of actual
     * runtime type of value. Type must be a super-type of runtime type.
     */
    public ObjectWriter writerWithType(JavaType rootType) {
        return new ObjectWriter(this, getSerializationConfig(), rootType, /*PrettyPrinter*/null);
    }

    /**
     * Factory method for constructing {@link ObjectWriter} that will
     * serialize objects using specified root type, instead of actual
     * runtime type of value. Type must be a super-type of runtime type.
     */
    public ObjectWriter writerWithType(TypeReference<?> rootType) {
        JavaType t = (rootType == null) ? null : _typeFactory.constructType(rootType);
        return new ObjectWriter(this, getSerializationConfig(), t, /*PrettyPrinter*/null);
    }
    
    /**
     * Factory method for constructing {@link ObjectWriter} that will
     * serialize objects using specified pretty printer for indentation
     * (or if null, no pretty printer)
     */
    public ObjectWriter writer(PrettyPrinter pp) {
        if (pp == null) { // need to use a marker to indicate explicit disabling of pp
            pp = ObjectWriter.NULL_PRETTY_PRINTER;
        }
        return new ObjectWriter(this, getSerializationConfig(), /*root type*/ null, pp);
    }
    
    /**
     * Factory method for constructing {@link ObjectWriter} that will
     * serialize objects using the default pretty printer for indentation
     */
    public ObjectWriter writerWithDefaultPrettyPrinter() {
        return new ObjectWriter(this, getSerializationConfig(),
                /*root type*/ null, _defaultPrettyPrinter());
    }
    
    /**
     * Factory method for constructing {@link ObjectWriter} that will
     * serialize objects using specified filter provider.
     */
    public ObjectWriter writer(FilterProvider filterProvider) {
        return new ObjectWriter(this,
                getSerializationConfig().withFilters(filterProvider));
    }
    
    /**
     * Factory method for constructing {@link ObjectWriter} that will
     * pass specific schema object to {@link JsonGenerator} used for
     * writing content.
     * 
     * @param schema Schema to pass to generator
     */
    public ObjectWriter writer(FormatSchema schema) {
        return new ObjectWriter(this, getSerializationConfig(), schema);
    }
    
    /*
    /**********************************************************
    /* Extended Public API: constructing ObjectReaders
    /* for more advanced configuration
    /**********************************************************
     */

    /**
     * Factory method for constructing {@link ObjectReader} with
     * default settings. Note that the resulting instance is NOT usable as is,
     * without defining expected value type.
     */
    public ObjectReader reader() {
        return new ObjectReader(this, getDeserializationConfig())
            .with(_injectableValues);
    }

    /**
     * Factory method for constructing {@link ObjectReader} with
     * specified feature enabled (compared to settings that this
     * mapper instance has).
     * Note that the resulting instance is NOT usable as is,
     * without defining expected value type.
     */
    public ObjectReader reader(DeserializationFeature feature) {
        return new ObjectReader(this, getDeserializationConfig().with(feature));
    }

    /**
     * Factory method for constructing {@link ObjectReader} with
     * specified features enabled (compared to settings that this
     * mapper instance has).
     * Note that the resulting instance is NOT usable as is,
     * without defining expected value type.
     */
    public ObjectReader reader(DeserializationFeature first,
            DeserializationFeature... other) {
        return new ObjectReader(this, getDeserializationConfig().with(first, other));
    }
    
    /**
     * Factory method for constructing {@link ObjectReader} that will
     * update given Object (usually Bean, but can be a Collection or Map
     * as well, but NOT an array) with JSON data. Deserialization occurs
     * normally except that the root-level value in JSON is not used for
     * instantiating a new object; instead give updateable object is used
     * as root.
     * Runtime type of value object is used for locating deserializer,
     * unless overridden by other factory methods of {@link ObjectReader}
     */
    public ObjectReader readerForUpdating(Object valueToUpdate)
    {
        JavaType t = _typeFactory.constructType(valueToUpdate.getClass());
        return new ObjectReader(this, getDeserializationConfig(), t, valueToUpdate,
                null, _injectableValues);
    }

    /**
     * Factory method for constructing {@link ObjectReader} that will
     * read or update instances of specified type
     */
    public ObjectReader reader(JavaType type)
    {
        return new ObjectReader(this, getDeserializationConfig(), type, null,
                null, _injectableValues);
    }

    /**
     * Factory method for constructing {@link ObjectReader} that will
     * read or update instances of specified type
     */
    public ObjectReader reader(Class<?> type)
    {
        return reader(_typeFactory.constructType(type));
    }

    /**
     * Factory method for constructing {@link ObjectReader} that will
     * read or update instances of specified type
     */
    public ObjectReader reader(TypeReference<?> type)
    {
        return reader(_typeFactory.constructType(type));
    }

    /**
     * Factory method for constructing {@link ObjectReader} that will
     * use specified {@link JsonNodeFactory} for constructing JSON trees.
     */
    public ObjectReader reader(JsonNodeFactory f)
    {
        return new ObjectReader(this, getDeserializationConfig()).with(f);
    }

    /**
     * Factory method for constructing {@link ObjectReader} that will
     * pass specific schema object to {@link JsonParser} used for
     * reading content.
     * 
     * @param schema Schema to pass to parser
     */
    public ObjectReader reader(FormatSchema schema) {
        return new ObjectReader(this, getDeserializationConfig(), null, null,
                schema, _injectableValues);
    }

    /**
     * Factory method for constructing {@link ObjectReader} that will
     * use specified injectable values.
     * 
     * @param injectableValues Injectable values to use
     */
    public ObjectReader reader(InjectableValues injectableValues) {
        return new ObjectReader(this, getDeserializationConfig(), null, null,
                null, injectableValues);
    }

    /**
     * Factory method for constructing {@link ObjectReader} that will
     * deserialize objects using specified JSON View (filter).
     */
    public ObjectReader readerWithView(Class<?> view) {
        return new ObjectReader(this, getDeserializationConfig().withView(view));
    }
    
    /*
    /**********************************************************
    /* Extended Public API: convenience type conversion
    /**********************************************************
     */
   
    /**
     * Convenience method for doing two-step conversion from given value, into
     * instance of given value type. This is functionality equivalent to first
     * serializing given value into JSON, then binding JSON data into value
     * of given type, but may be executed without fully serializing into
     * JSON. Same converters (serializers, deserializers) will be used as for
     * data binding, meaning same object mapper configuration works.
     *      
     * @throws IllegalArgumentException If conversion fails due to incompatible type;
     *    if so, root cause will contain underlying checked exception data binding
     *    functionality threw
     */
    @SuppressWarnings("unchecked")
    public <T> T convertValue(Object fromValue, Class<T> toValueType)
        throws IllegalArgumentException
    {
        return (T) _convert(fromValue, _typeFactory.constructType(toValueType));
    } 

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> T convertValue(Object fromValue, TypeReference toValueTypeRef)
        throws IllegalArgumentException
    {
        return (T) _convert(fromValue, _typeFactory.constructType(toValueTypeRef));
    } 

    @SuppressWarnings("unchecked")
    public <T> T convertValue(Object fromValue, JavaType toValueType)
        throws IllegalArgumentException
    {
        return (T) _convert(fromValue, toValueType);
    } 

    /**
     * Actual conversion implementation: instead of using existing read
     * and write methods, much of code is inlined. Reason for this is
     * that we must avoid wrapping/unwrapping both for efficiency and
     * for correctness. If wrapping/unwrapping is actually desired,
     * caller must use explicit <code>writeValue</code> and
     * <code>readValue</code> methods.
     */
    protected Object _convert(Object fromValue, JavaType toValueType)
        throws IllegalArgumentException
    {
        // sanity check for null first:
        if (fromValue == null) return null;
        /* Then use TokenBuffer, which is a JsonGenerator:
         * (see [JACKSON-175])
         */
        TokenBuffer buf = new TokenBuffer(this);
        try {
            // inlined 'writeValue' with minor changes:
            // first: disable wrapping when writing
            SerializationConfig config = getSerializationConfig().without(SerializationFeature.WRAP_ROOT_VALUE);
            // no need to check for closing of TokenBuffer
            _serializerProvider(config).serializeValue(buf, fromValue);

            // then matching read, inlined 'readValue' with minor mods:
            final JsonParser jp = buf.asParser();
            Object result;
            // ok to pass in existing feature flags; unwrapping handled by mapper
            final DeserializationConfig deserConfig = getDeserializationConfig();
            JsonToken t = _initForReading(jp);
            if (t == JsonToken.VALUE_NULL) {
                DeserializationContext ctxt = createDeserializationContext(jp, deserConfig);
                result = _findRootDeserializer(ctxt, toValueType).getNullValue();
            } else if (t == JsonToken.END_ARRAY || t == JsonToken.END_OBJECT) {
                result = null;
            } else { // pointing to event other than null
                DeserializationContext ctxt = createDeserializationContext(jp, deserConfig);
                JsonDeserializer<Object> deser = _findRootDeserializer(ctxt, toValueType);
                // note: no handling of unwarpping
                result = deser.deserialize(jp, ctxt);
            }
            jp.close();
            return result;
        } catch (IOException e) { // should not occur, no real i/o...
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /*
    /**********************************************************
    /* Extended Public API: JSON Schema generation
    /**********************************************************
     */

    /**
     * Generate <a href="http://json-schema.org/">Json-schema</a>
     * instance for specified class.
     *
     * @param t The class to generate schema for
     * @return Constructed JSON schema.
     */
    public JsonSchema generateJsonSchema(Class<?> t) throws JsonMappingException {
        return _serializerProvider(getSerializationConfig()).generateJsonSchema(t);
    }

    /*
    /**********************************************************
    /* Internal methods for serialization, overridable
    /**********************************************************
     */

    /**
     * Overridable helper method used for constructing
     * {@link SerializerProvider} to use for serialization.
     */
    protected DefaultSerializerProvider _serializerProvider(SerializationConfig config) {
        return _serializerProvider.createInstance(config, _serializerFactory);
    }
    
    /**
     * Helper method that should return default pretty-printer to
     * use for generators constructed by this mapper, when instructed
     * to use default pretty printer.
     */
    protected PrettyPrinter _defaultPrettyPrinter() {
        return new DefaultPrettyPrinter();
    }
    
    /**
     * Method called to configure the generator as necessary and then
     * call write functionality
     */
    protected final void _configAndWriteValue(JsonGenerator jgen, Object value)
        throws IOException, JsonGenerationException, JsonMappingException
    {
        SerializationConfig cfg = getSerializationConfig();
        // [JACKSON-96]: allow enabling pretty printing for ObjectMapper directly
        if (cfg.isEnabled(SerializationFeature.INDENT_OUTPUT)) {
            jgen.useDefaultPrettyPrinter();
        }
        // [JACKSON-282]: consider Closeable
        if (cfg.isEnabled(SerializationFeature.CLOSE_CLOSEABLE) && (value instanceof Closeable)) {
            _configAndWriteCloseable(jgen, value, cfg);
            return;
        }
        boolean closed = false;
        try {
            _serializerProvider(cfg).serializeValue(jgen, value);
            closed = true;
            jgen.close();
        } finally {
            /* won't try to close twice; also, must catch exception (so it 
             * will not mask exception that is pending)
             */
            if (!closed) {
                try {
                    jgen.close();
                } catch (IOException ioe) { }
            }
        }
    }

    protected final void _configAndWriteValue(JsonGenerator jgen, Object value, Class<?> viewClass)
        throws IOException, JsonGenerationException, JsonMappingException
    {
        SerializationConfig cfg = getSerializationConfig().withView(viewClass);
        if (cfg.isEnabled(SerializationFeature.INDENT_OUTPUT)) {
            jgen.useDefaultPrettyPrinter();
        }
        // [JACKSON-282]: consider Closeable
        if (cfg.isEnabled(SerializationFeature.CLOSE_CLOSEABLE) && (value instanceof Closeable)) {
            _configAndWriteCloseable(jgen, value, cfg);
            return;
        }
        boolean closed = false;
        try {
            _serializerProvider(cfg).serializeValue(jgen, value);
            closed = true;
            jgen.close();
        } finally {
            if (!closed) {
                try {
                    jgen.close();
                } catch (IOException ioe) { }
            }
        }
    }

    /**
     * Helper method used when value to serialize is {@link Closeable} and its <code>close()</code>
     * method is to be called right after serialization has been called
     */
    private final void _configAndWriteCloseable(JsonGenerator jgen, Object value, SerializationConfig cfg)
        throws IOException, JsonGenerationException, JsonMappingException
    {
        Closeable toClose = (Closeable) value;
        try {
            _serializerProvider(cfg).serializeValue(jgen, value);
            JsonGenerator tmpJgen = jgen;
            jgen = null;
            tmpJgen.close();
            Closeable tmpToClose = toClose;
            toClose = null;
            tmpToClose.close();
        } finally {
            /* Need to close both generator and value, as long as they haven't yet
             * been closed
             */
            if (jgen != null) {
                try {
                    jgen.close();
                } catch (IOException ioe) { }
            }
            if (toClose != null) {
                try {
                    toClose.close();
                } catch (IOException ioe) { }
            }
        }
    }
    
    /**
     * Helper method used when value to serialize is {@link Closeable} and its <code>close()</code>
     * method is to be called right after serialization has been called
     */
    private final void _writeCloseableValue(JsonGenerator jgen, Object value, SerializationConfig cfg)
        throws IOException, JsonGenerationException, JsonMappingException
    {
        Closeable toClose = (Closeable) value;
        try {
            _serializerProvider(cfg).serializeValue(jgen, value);
            if (cfg.isEnabled(SerializationFeature.FLUSH_AFTER_WRITE_VALUE)) {
                jgen.flush();
            }
            Closeable tmpToClose = toClose;
            toClose = null;
            tmpToClose.close();
        } finally {
            if (toClose != null) {
                try {
                    toClose.close();
                } catch (IOException ioe) { }
            }
        }
    }

    /*
    /**********************************************************
    /* Internal methods for deserialization, overridable
    /**********************************************************
     */

    /**
     * Internal helper method called to create an instance of {@link DeserializationContext}
     * for deserializing a single root value.
     * Can be overridden if a custom context is needed.
     */
    protected final DefaultDeserializationContext createDeserializationContext(JsonParser jp,
            DeserializationConfig cfg)
    {
        return _deserializationContext.createInstance(cfg,
                jp, _injectableValues);
    }
    
    /**
     * Actual implementation of value reading+binding operation.
     */
    protected Object _readValue(DeserializationConfig cfg, JsonParser jp, JavaType valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        /* First: may need to read the next token, to initialize
         * state (either before first read from parser, or after
         * previous token has been cleared)
         */
        Object result;
        JsonToken t = _initForReading(jp);
        if (t == JsonToken.VALUE_NULL) {
            // [JACKSON-643]: Ask JsonDeserializer what 'null value' to use:
            DeserializationContext ctxt = createDeserializationContext(jp, cfg);
            result = _findRootDeserializer(ctxt, valueType).getNullValue();
        } else if (t == JsonToken.END_ARRAY || t == JsonToken.END_OBJECT) {
            result = null;
        } else { // pointing to event other than null
            DeserializationContext ctxt = createDeserializationContext(jp, cfg);
            JsonDeserializer<Object> deser = _findRootDeserializer(ctxt, valueType);
            // ok, let's get the value
            if (cfg.useRootWrapping()) {
                result = _unwrapAndDeserialize(jp, ctxt, cfg, valueType, deser);
            } else {
                result = deser.deserialize(jp, ctxt);
            }
        }
        // Need to consume the token too
        jp.clearCurrentToken();
        return result;
    }
    
    protected Object _readMapAndClose(JsonParser jp, JavaType valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        try {
            Object result;
            JsonToken t = _initForReading(jp);
            if (t == JsonToken.VALUE_NULL) {
                // [JACKSON-643]: Ask JsonDeserializer what 'null value' to use:
                DeserializationContext ctxt = createDeserializationContext(jp,
                        getDeserializationConfig());
                result = _findRootDeserializer(ctxt, valueType).getNullValue();
            } else if (t == JsonToken.END_ARRAY || t == JsonToken.END_OBJECT) {
                result = null;
            } else {
                DeserializationConfig cfg = getDeserializationConfig();
                DeserializationContext ctxt = createDeserializationContext(jp, cfg);
                JsonDeserializer<Object> deser = _findRootDeserializer(ctxt, valueType);
                if (cfg.useRootWrapping()) {
                    result = _unwrapAndDeserialize(jp, ctxt, cfg, valueType, deser);
                } else {
                    result = deser.deserialize(jp, ctxt);
                }
            }
            // Need to consume the token too
            jp.clearCurrentToken();
            return result;
        } finally {
            try {
                jp.close();
            } catch (IOException ioe) { }
        }
    }
    
    /**
     * Method called to ensure that given parser is ready for reading
     * content for data binding.
     *
     * @return First token to be used for data binding after this call:
     *  can never be null as exception will be thrown if parser can not
     *  provide more tokens.
     *
     * @throws IOException if the underlying input source has problems during
     *   parsing
     * @throws JsonParseException if parser has problems parsing content
     * @throws JsonMappingException if the parser does not have any more
     *   content to map (note: Json "null" value is considered content;
     *   enf-of-stream not)
     */
    protected JsonToken _initForReading(JsonParser jp)
        throws IOException, JsonParseException, JsonMappingException
    {
        /* First: must point to a token; if not pointing to one, advance.
         * This occurs before first read from JsonParser, as well as
         * after clearing of current token.
         */
        JsonToken t = jp.getCurrentToken();
        if (t == null) {
            // and then we must get something...
            t = jp.nextToken();
            if (t == null) {
                /* [JACKSON-546] Throw mapping exception, since it's failure to map,
                 *   not an actual parsing problem
                 */
                throw JsonMappingException.from(jp, "No content to map due to end-of-input");
            }
        }
        return t;
    }

    protected Object _unwrapAndDeserialize(JsonParser jp, DeserializationContext ctxt, 
            DeserializationConfig config,
            JavaType rootType, JsonDeserializer<Object> deser)
        throws IOException, JsonParseException, JsonMappingException
    {
        String expName = config.getRootName();
        if (expName == null) {
            SerializedString sstr = _rootNames.findRootName(rootType, config);
            expName = sstr.getValue();
        }
        if (jp.getCurrentToken() != JsonToken.START_OBJECT) {
            throw JsonMappingException.from(jp, "Current token not START_OBJECT (needed to unwrap root name '"
                    +expName+"'), but "+jp.getCurrentToken());
        }
        if (jp.nextToken() != JsonToken.FIELD_NAME) {
            throw JsonMappingException.from(jp, "Current token not FIELD_NAME (to contain expected root name '"
                    +expName+"'), but "+jp.getCurrentToken());
        }
        String actualName = jp.getCurrentName();
        if (!expName.equals(actualName)) {
            throw JsonMappingException.from(jp, "Root name '"+actualName+"' does not match expected ('"
                    +expName+"') for type "+rootType);
        }
        // ok, then move to value itself....
        jp.nextToken();
        Object result = deser.deserialize(jp, ctxt);
        // and last, verify that we now get matching END_OBJECT
        if (jp.nextToken() != JsonToken.END_OBJECT) {
            throw JsonMappingException.from(jp, "Current token not END_OBJECT (to match wrapper object with root name '"
                    +expName+"'), but "+jp.getCurrentToken());
        }
        return result;
    }
    
    /*
    /**********************************************************
    /* Internal methods, other
    /**********************************************************
     */

    /**
     * Method called to locate deserializer for the passed root-level value.
     */
    protected JsonDeserializer<Object> _findRootDeserializer(DeserializationContext ctxt,
            JavaType valueType)
        throws JsonMappingException
    {
        // First: have we already seen it?
        JsonDeserializer<Object> deser = _rootDeserializers.get(valueType);
        if (deser != null) {
            return deser;
        }
        // Nope: need to ask provider to resolve it
        deser = ctxt.findRootValueDeserializer(valueType);
        if (deser == null) { // can this happen?
            throw new JsonMappingException("Can not find a deserializer for type "+valueType);
        }
        _rootDeserializers.put(valueType, deser);
        return deser;
    }
}
