package com.fasterxml.jackson.databind;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.io.SegmentedStringWriter;
import com.fasterxml.jackson.core.json.JsonFactory;
import com.fasterxml.jackson.core.type.ResolvedType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.*;
import com.fasterxml.jackson.databind.cfg.*;
import com.fasterxml.jackson.databind.deser.*;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.introspect.*;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.*;
import com.fasterxml.jackson.databind.jsontype.impl.StdSubtypeResolver;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.databind.ser.*;
import com.fasterxml.jackson.databind.type.*;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.RootNameLookup;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.databind.util.TokenBuffer;

/**
 * ObjectMapper provides functionality for reading and writing JSON,
 * either to and from basic POJOs (Plain Old Java Objects), or to and from
 * a general-purpose JSON Tree Model ({@link JsonNode}), as well as
 * related functionality for performing conversions.
 * It is also highly customizable to work both with different styles of JSON
 * content, and to support more advanced Object concepts such as
 * polymorphism and Object identity.
 * <code>ObjectMapper</code> also acts as a factory for more advanced {@link ObjectReader}
 * and {@link ObjectWriter} classes.
 * Mapper (and {@link ObjectReader}s, {@link ObjectWriter}s it constructs) will
 * use instances of {@link JsonParser} and {@link JsonGenerator}
 * for implementing actual reading/writing of JSON.
 * Note that although most read and write methods are exposed through this class,
 * some of the functionality is only exposed via {@link ObjectReader} and
 * {@link ObjectWriter}: specifically, reading/writing of longer sequences of
 * values is only available through {@link ObjectReader#readValues(InputStream)}
 * and {@link ObjectWriter#writeValues(OutputStream)}.
 *<p>
Simplest usage is of form:
<pre>
  final ObjectMapper mapper = new ObjectMapper(); // can use static singleton, inject: just make sure to reuse!
  MyValue value = new MyValue();
  // ... and configure
  File newState = new File("my-stuff.json");
  mapper.writeValue(newState, value); // writes JSON serialization of MyValue instance
  // or, read
  MyValue older = mapper.readValue(new File("my-older-stuff.json"), MyValue.class);

  // Or if you prefer JSON Tree representation:
  JsonNode root = mapper.readTree(newState);
  // and find values by, for example, using a {@link com.fasterxml.jackson.core.JsonPointer} expression:
  int age = root.at("/personal/age").getValueAsInt(); 
</pre>
 *<p> 
 * Mapper instances are fully thread-safe provided that ALL configuration of the
 * instance occurs before ANY read or write calls. If configuration of a mapper instance
 * is modified after first usage, changes may or may not take effect, and configuration
 * calls themselves may fail.
 * If you need to use different configuration, you have two main possibilities:
 *<ul>
 * <li>Construct and use {@link ObjectReader} for reading, {@link ObjectWriter} for writing.
 *    Both types are fully immutable and you can freely create new instances with different
 *    configuration using either factory methods of {@link ObjectMapper}, or readers/writers
 *    themselves. Construction of new {@link ObjectReader}s and {@link ObjectWriter}s is
 *    a very light-weight operation so it is usually appropriate to create these on per-call
 *    basis, as needed, for configuring things like optional indentation of JSON.
 *  </li>
 * <li>If the specific kind of configurability is not available via {@link ObjectReader} and
 *   {@link ObjectWriter}, you may need to use multiple {@link ObjectMapper} instead (for example:
 *   you cannot change mix-in annotations on-the-fly; or, set of custom (de)serializers).
 *   To help with this usage, you may want to use method {@link #copy()} which creates a clone
 *   of the mapper with specific configuration, and allows configuration of the copied instance
 *   before it gets used. Note that {@link #copy} operation is as expensive as constructing
 *   a new {@link ObjectMapper} instance: if possible, you should still pool and reuse mappers
 *   if you intend to use them for multiple operations.
 *  </li>
 * </ul>
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
 *<p>
 * Notes on security: use "default typing" feature (see {@link #enableDefaultTyping()})
 * is a potential security risk, if used with untrusted content (content generated by
 * untrusted external parties). If so, you may want to construct a custom 
 * {@link TypeResolverBuilder} implementation to limit possible types to instantiate,
 * (using {@link #setDefaultTyping}).
 */
public class ObjectMapper
    implements Versioned,
        java.io.Serializable
{
    private static final long serialVersionUID = 3L;

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
     *<p>
     * Since 2.4 there are special exceptions for JSON Tree model
     * types (sub-types of {@link TreeNode}: default typing is never
     * applied to them.
     * Since 2.8 additional checks are made to avoid attempts at default
     * typing primitive-valued properties.
     *<p>
     * NOTE: use of Default Typing can be a potential security risk if incoming
     * content comes from untrusted sources, and it is recommended that this
     * is either not done, or, if enabled, use {@link #setDefaultTyping}
     * passing a custom {@link TypeResolverBuilder} implementation that white-lists
     * legal types to use.
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
         * This does NOT apply to {@link TreeNode} and its subtypes.
         */
        OBJECT_AND_NON_CONCRETE,

        /**
         * Value that means that default typing will be used for
         * all types covered by {@link #OBJECT_AND_NON_CONCRETE}
         * plus all array types for them.
         * This does NOT apply to {@link TreeNode} and its subtypes.
         */
        NON_CONCRETE_AND_ARRAYS,
        
        /**
         * Value that means that default typing will be used for
         * all non-final types, with exception of small number of
         * "natural" types (String, Boolean, Integer, Double), which
         * can be correctly inferred from JSON; as well as for
         * all arrays of non-final types.
         * This does NOT apply to {@link TreeNode} and its subtypes.
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
        implements java.io.Serializable
    {
        private static final long serialVersionUID = 1L;

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
            // 03-Oct-2016, tatu: As per [databind#1395], need to skip
            //  primitive types too, regardless
            if (t.isPrimitive()) {
                return false;
            }

            switch (_appliesFor) {
            case NON_CONCRETE_AND_ARRAYS:
                while (t.isArrayType()) {
                    t = t.getContentType();
                }
                // fall through
            case OBJECT_AND_NON_CONCRETE:
                // 19-Apr-2016, tatu: ReferenceType like Optional also requires similar handling:
                while (t.isReferenceType()) {
                    t = t.getReferencedType();
                }
                return t.isJavaLangObject()
                        || (!t.isConcrete()
                                // [databind#88] Should not apply to JSON tree models:
                                && !TreeNode.class.isAssignableFrom(t.getRawClass()));

            case NON_FINAL:
                while (t.isArrayType()) {
                    t = t.getContentType();
                }
                // 19-Apr-2016, tatu: ReferenceType like Optional also requires similar handling:
                while (t.isReferenceType()) {
                    t = t.getReferencedType();
                }
                // [databind#88] Should not apply to JSON tree models:
                return !t.isFinal() && !TreeNode.class.isAssignableFrom(t.getRawClass());
            default:
            //case JAVA_LANG_OBJECT:
                return t.isJavaLangObject();
            }
        }
    }

    /*
    /**********************************************************
    /* Internal constants, singletons
    /**********************************************************
     */
    
    // Quick little shortcut, to avoid having to use global TypeFactory instance...
    // 19-Oct-2015, tatu: Not sure if this is really safe to do; let's at least allow
    //   some amount of introspection
    private final static JavaType JSON_NODE_TYPE =
            SimpleType.constructUnsafe(JsonNode.class);
//            TypeFactory.defaultInstance().constructType(JsonNode.class);

    // 16-May-2009, tatu: Ditto ^^^
    protected final static AnnotationIntrospector DEFAULT_ANNOTATION_INTROSPECTOR = new JacksonAnnotationIntrospector();

    /**
     * Base settings contain defaults used for all {@link ObjectMapper}
     * instances.
     */
    protected final static BaseSettings DEFAULT_BASE = new BaseSettings(
            null, // cannot share global ClassIntrospector any more (2.5+)
            DEFAULT_ANNOTATION_INTROSPECTOR,
             null, TypeFactory.defaultInstance(),
            null, StdDateFormat.instance, null,
            Locale.getDefault(),
            null, // to indicate "use Jackson default TimeZone" (UTC since Jackson 2.7)
            Base64Variants.getDefaultVariant(),
            JsonNodeFactory.instance
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
    protected final TokenStreamFactory _jsonFactory;

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
     * Currently active per-type configuration overrides, accessed by
     * declared type of property.
     */
    protected final ConfigOverrides _configOverrides;

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
    protected SimpleMixInResolver _mixIns;

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
    /* Module-related
    /**********************************************************
     */

    /**
     * Set of module types (as per {@link Module#getTypeId()} that have been
     * registered; kept track of iff {@link MapperFeature#IGNORE_DUPLICATE_MODULE_REGISTRATIONS}
     * is enabled, so that duplicate registration calls can be ignored
     * (to avoid adding same handlers multiple times, mostly).
     */
    protected Set<Object> _registeredModuleTypes;
    
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
     * of root-level deserializers. This is where most successful
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
     * {@link TokenStreamFactory} as necessary, use
     * {@link SerializerProvider} as its
     * {@link SerializerProvider}, and
     * {@link BeanSerializerFactory} as its
     * {@link SerializerFactory}.
     * This means that it
     * can serialize all standard JDK types, as well as regular
     * Java Beans (based on method names and Jackson-specific annotations),
     * but does not support JAXB annotations.
     */
    public ObjectMapper() {
        this(null, null, null);
    }

    /**
     * Constructs instance that uses specified {@link TokenStreamFactory}
     * for constructing necessary {@link JsonParser}s and/or
     * {@link JsonGenerator}s.
     */
    public ObjectMapper(TokenStreamFactory jf) {
        this(jf, null, null);
    }

    /**
     * Copy-constructor, mostly used to support {@link #copy}.
     */
    protected ObjectMapper(ObjectMapper src)
    {
        _jsonFactory = src._jsonFactory.copy();
        _subtypeResolver = src._subtypeResolver;
        _typeFactory = src._typeFactory;
        _injectableValues = src._injectableValues;
        _configOverrides = src._configOverrides.copy();
        _mixIns = src._mixIns.copy();

        RootNameLookup rootNames = new RootNameLookup();
        _serializationConfig = new SerializationConfig(src._serializationConfig,
                _mixIns, rootNames, _configOverrides);
        _deserializationConfig = new DeserializationConfig(src._deserializationConfig,
                _mixIns, rootNames,  _configOverrides);
        _serializerProvider = src._serializerProvider.copy();
        _deserializationContext = src._deserializationContext.copy();

        // Default serializer factory is stateless, can just assign
        _serializerFactory = src._serializerFactory;

        // as per [databind#922], [databind#1078] make sure to copy registered modules as appropriate
        Set<Object> reg = src._registeredModuleTypes;
        if (reg == null) {
            _registeredModuleTypes = null;
        } else {
            _registeredModuleTypes = new LinkedHashSet<Object>(reg);
        }
    }

    /**
     * Constructs instance that uses specified {@link TokenStreamFactory}
     * for constructing necessary {@link JsonParser}s and/or
     * {@link JsonGenerator}s, and uses given providers for accessing
     * serializers and deserializers.
     * 
     * @param jf TokenStreamFactory to use: if null, a new {@link JsonFactory} will be constructed
     * @param sp SerializerProvider to use: if null, a {@link SerializerProvider} will be constructed
     * @param dc Blueprint deserialization context instance to use for creating
     *    actual context objects; if null, will construct standard
     *    {@link DeserializationContext}
     */
    public ObjectMapper(TokenStreamFactory jf,
            DefaultSerializerProvider sp, DefaultDeserializationContext dc)
    {
        // 06-OCt-2017, tatu: Should probably change dependency one of these days...
        //   but not today.
        if (jf == null) {
            jf = new JsonFactory();
        }
        _jsonFactory = jf;
        _subtypeResolver = new StdSubtypeResolver();
        RootNameLookup rootNames = new RootNameLookup();
        // and default type factory is shared one
        _typeFactory = TypeFactory.defaultInstance();

        SimpleMixInResolver mixins = new SimpleMixInResolver(null);
        _mixIns = mixins;
        BaseSettings base = DEFAULT_BASE.withClassIntrospector(defaultClassIntrospector());
        _configOverrides = new ConfigOverrides();
        _serializationConfig = new SerializationConfig(base,
                    _subtypeResolver, mixins, rootNames, _configOverrides);
        _deserializationConfig = new DeserializationConfig(base,
                    _subtypeResolver, mixins, rootNames, _configOverrides);

        // Some overrides we may need
        final boolean needOrder = _jsonFactory.requiresPropertyOrdering();
        if (needOrder ^ _serializationConfig.isEnabled(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)) {
            configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, needOrder);
        }
        
        _serializerProvider = (sp == null) ? new DefaultSerializerProvider.Impl(_jsonFactory) : sp;
        _deserializationContext = (dc == null) ?
                new DefaultDeserializationContext.Impl(BeanDeserializerFactory.instance, _jsonFactory) : dc;
        // Default serializer factory is stateless, can just assign
        _serializerFactory = BeanSerializerFactory.instance;
    }

    /**
     * Overridable helper method used to construct default {@link ClassIntrospector}
     * to use.
     */
    protected ClassIntrospector defaultClassIntrospector() {
        return new BasicClassIntrospector();
    }

    /*
    /**********************************************************
    /* Methods sub-classes MUST override
    /**********************************************************
     */
    
    /**
     * Method for creating a new {@link ObjectMapper} instance that
     * has same initial configuration as this instance. Note that this
     * also requires making a copy of the underlying {@link TokenStreamFactory}
     * instance.
     *<p>
     * Method is typically
     * used when multiple, differently configured mappers are needed.
     * Although configuration is shared, cached serializers and deserializers
     * are NOT shared, which means that the new instance may be re-configured
     * before use; meaning that it behaves the same way as if an instance
     * was constructed from scratch.
     */
    public ObjectMapper copy() {
        _checkInvalidCopy(ObjectMapper.class);
        return new ObjectMapper(this);
    }

    protected void _checkInvalidCopy(Class<?> exp)
    {
        if (getClass() != exp) {
            // 10-Nov-2016, tatu: could almost use `ClassUtil.verifyMustOverride()` but not quite
            throw new IllegalStateException("Failed copy(): "+getClass().getName()
                    +" (version: "+version()+") does not override copy(); it has to");
        }
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
        return com.fasterxml.jackson.databind.cfg.PackageVersion.VERSION;
    }

    /*
    /**********************************************************
    /* Module registration, discovery
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
        if (isEnabled(MapperFeature.IGNORE_DUPLICATE_MODULE_REGISTRATIONS)) {
            Object typeId = module.getTypeId();
            if (typeId != null) {
                if (_registeredModuleTypes == null) {
                    // plus let's keep them in order too, easier to debug or expose
                    // in registration order if that matter
                    _registeredModuleTypes = new LinkedHashSet<Object>();
                }
                // try adding; if already had it, should skip
                if (!_registeredModuleTypes.add(typeId)) {
                    return this;
                }
            }
        }
        
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

        // And then call registration
        module.setupModule(new Module.SetupContext()
        {
            // // // Accessors

            @Override
            public Version getMapperVersion() {
                return version();
            }

            @Override
            public Object getOwner() {
                // why do we need the cast here?!?
                return ObjectMapper.this;
            }

            @Override
            public TypeFactory getTypeFactory() {
                return _typeFactory;
            }
            
            @Override
            public boolean isEnabled(MapperFeature f) {
                return ObjectMapper.this.isEnabled(f);
            }

            @Override
            public boolean isEnabled(DeserializationFeature f) {
                return ObjectMapper.this.isEnabled(f);
            }
            
            @Override
            public boolean isEnabled(SerializationFeature f) {
                return ObjectMapper.this.isEnabled(f);
            }

            @Override
            public boolean isEnabled(JsonFactory.Feature f) {
                return ObjectMapper.this.isEnabled(f);
            }

            @Override
            public boolean isEnabled(JsonParser.Feature f) {
                return ObjectMapper.this.isEnabled(f);
            }

            @Override
            public boolean isEnabled(JsonGenerator.Feature f) {
                return ObjectMapper.this.isEnabled(f);
            }

            // // // Mutant accessors

            @Override
            public MutableConfigOverride configOverride(Class<?> type) {
                return ObjectMapper.this.configOverride(type);
            }

            // // // Methods for registering handlers: deserializers

            @Override
            public void addDeserializers(Deserializers d) {
                DeserializerFactory df = _deserializationContext._factory.withAdditionalDeserializers(d);
                _deserializationContext = _deserializationContext.with(df);
            }

            @Override
            public void addKeyDeserializers(KeyDeserializers d) {
                DeserializerFactory df = _deserializationContext._factory.withAdditionalKeyDeserializers(d);
                _deserializationContext = _deserializationContext.with(df);
            }

            @Override
            public void addBeanDeserializerModifier(BeanDeserializerModifier modifier) {
                DeserializerFactory df = _deserializationContext._factory.withDeserializerModifier(modifier);
                _deserializationContext = _deserializationContext.with(df);
            }
            
            // // // Methods for registering handlers: serializers
            
            @Override
            public void addSerializers(Serializers s) {
                _serializerFactory = _serializerFactory.withAdditionalSerializers(s);
            }

            @Override
            public void addKeySerializers(Serializers s) {
                _serializerFactory = _serializerFactory.withAdditionalKeySerializers(s);
            }
            
            @Override
            public void addBeanSerializerModifier(BeanSerializerModifier modifier) {
                _serializerFactory = _serializerFactory.withSerializerModifier(modifier);
            }

            // // // Methods for registering handlers: other
            
            @Override
            public void addAbstractTypeResolver(AbstractTypeResolver resolver) {
                DeserializerFactory df = _deserializationContext._factory.withAbstractTypeResolver(resolver);
                _deserializationContext = _deserializationContext.with(df);
            }

            @Override
            public void addTypeModifier(TypeModifier modifier) {
                TypeFactory f = _typeFactory;
                f = f.withModifier(modifier);
                setTypeFactory(f);
            }

            @Override
            public void addValueInstantiators(ValueInstantiators instantiators) {
                DeserializerFactory df = _deserializationContext._factory.withValueInstantiators(instantiators);
                _deserializationContext = _deserializationContext.with(df);
            }

            @Override
            public void setClassIntrospector(ClassIntrospector ci) {
                _deserializationConfig = _deserializationConfig.with(ci);
                _serializationConfig = _serializationConfig.with(ci);
            }

            @Override
            public void insertAnnotationIntrospector(AnnotationIntrospector ai) {
                _deserializationConfig = _deserializationConfig.withInsertedAnnotationIntrospector(ai);
                _serializationConfig = _serializationConfig.withInsertedAnnotationIntrospector(ai);
            }
            
            @Override
            public void appendAnnotationIntrospector(AnnotationIntrospector ai) {
                _deserializationConfig = _deserializationConfig.withAppendedAnnotationIntrospector(ai);
                _serializationConfig = _serializationConfig.withAppendedAnnotationIntrospector(ai);
            }

            @Override
            public void registerSubtypes(Class<?>... subtypes) {
                ObjectMapper.this.registerSubtypes(subtypes);
            }

            @Override
            public void registerSubtypes(NamedType... subtypes) {
                ObjectMapper.this.registerSubtypes(subtypes);
            }

            @Override
            public void registerSubtypes(Collection<Class<?>> subtypes) {
                ObjectMapper.this.registerSubtypes(subtypes);
            }

            @Override
            public void setMixInAnnotations(Class<?> target, Class<?> mixinSource) {
                addMixIn(target, mixinSource);
            }
            
            @Override
            public void addDeserializationProblemHandler(DeserializationProblemHandler handler) {
                addHandler(handler);
            }

            @Override
            public void setNamingStrategy(PropertyNamingStrategy naming) {
                setPropertyNamingStrategy(naming);
            }
        });
        return this;
    }

    /**
     * Convenience method for registering specified modules in order;
     * functionally equivalent to:
     *<pre>
     *   for (Module module : modules) {
     *      registerModule(module);
     *   }
     *</pre>
     */
    public ObjectMapper registerModules(Module... modules)
    {
        for (Module module : modules) {
            registerModule(module);
        }
        return this;
    }

    /**
     * Convenience method for registering specified modules in order;
     * functionally equivalent to:
     *<pre>
     *   for (Module module : modules) {
     *      registerModule(module);
     *   }
     *</pre>
     */
    public ObjectMapper registerModules(Iterable<? extends Module> modules)
    {
        for (Module module : modules) {
            registerModule(module);
        }
        return this;
    }
    
    /**
     * Method for locating available methods, using JDK {@link ServiceLoader}
     * facility, along with module-provided SPI.
     *<p>
     * Note that method does not do any caching, so calls should be considered
     * potentially expensive.
     */
    public static List<Module> findModules() {
        return findModules(null);
    }

    /**
     * Method for locating available methods, using JDK {@link ServiceLoader}
     * facility, along with module-provided SPI.
     *<p>
     * Note that method does not do any caching, so calls should be considered
     * potentially expensive.
     */
    public static List<Module> findModules(ClassLoader classLoader)
    {
        ArrayList<Module> modules = new ArrayList<Module>();
        ServiceLoader<Module> loader = secureGetServiceLoader(Module.class, classLoader);
        for (Module module : loader) {
            modules.add(module);
        }
        return modules;
    }

    private static <T> ServiceLoader<T> secureGetServiceLoader(final Class<T> clazz, final ClassLoader classLoader) {
        final SecurityManager sm = System.getSecurityManager();
        if (sm == null) {
            return (classLoader == null) ?
                    ServiceLoader.load(clazz) : ServiceLoader.load(clazz, classLoader);
        }
        return AccessController.doPrivileged(new PrivilegedAction<ServiceLoader<T>>() {
            @Override
            public ServiceLoader<T> run() {
                return (classLoader == null) ?
                        ServiceLoader.load(clazz) : ServiceLoader.load(clazz, classLoader);
            }
        });
    }

    /**
     * Convenience method that is functionally equivalent to:
     *<code>
     *   mapper.registerModules(mapper.findModules());
     *</code>
     *<p>
     * As with {@link #findModules()}, no caching is done for modules, so care
     * needs to be taken to either create and share a single mapper instance;
     * or to cache introspected set of modules.
     */
    public ObjectMapper findAndRegisterModules() {
        return registerModules(findModules());
    }

    /*
    /**********************************************************
    /* Configuration: main config object access
    /**********************************************************
     */

    /**
     * Method that returns the shared default {@link SerializationConfig}
     * object that defines configuration settings for serialization.
     *<p>
     * Note that since instances are immutable, you can NOT change settings
     * by accessing an instance and calling methods: this will simply create
     * new instance of config object.
     */
    public SerializationConfig getSerializationConfig() {
        return _serializationConfig;
    }

    /**
     * Method that returns
     * the shared default {@link DeserializationConfig} object
     * that defines configuration settings for deserialization.
     *<p>
     * Note that since instances are immutable, you can NOT change settings
     * by accessing an instance and calling methods: this will simply create
     * new instance of config object.
     */
    public DeserializationConfig getDeserializationConfig() {
        return _deserializationConfig;
    }

    /**
     * Method that can be used to get hold of {@link TokenStreamFactory} that this
     * mapper uses if it needs to construct {@link JsonParser}s
     * and/or {@link JsonGenerator}s.
     *<p>
     * WARNING: note that all {@link ObjectReader} and {@link ObjectWriter}
     * instances created by this mapper usually share the same configured
     * {@link TokenStreamFactory}, so changes to its configuration will "leak".
     * To avoid such observed changes you should always use "with()" and
     * "without()" method of {@link ObjectReader} and {@link ObjectWriter}
     * for changing {@link com.fasterxml.jackson.core.JsonParser.Feature}
     * and {@link com.fasterxml.jackson.core.JsonGenerator.Feature}
     * settings to use on per-call basis.
     *
     * @return {@link TokenStreamFactory} that this mapper uses when it needs to
     *   construct Json parser and generators
     *
     * @since 3.0
     */
    public TokenStreamFactory tokenStreamFactory() { return _jsonFactory; }

    /**
     * @deprecated Since 3.0 use {@link #tokenStreamFactory()} instead.
     */
    @Deprecated // since 3.0
    public TokenStreamFactory getFactory() { return tokenStreamFactory(); }

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
     * Method for getting current {@link SerializerFactory}.
      *<p>
     * Note that since instances are immutable, you can NOT change settings
     * by accessing an instance and calling methods: this will simply create
     * new instance of factory object.
     */
    public SerializerFactory getSerializerFactory() {
        return _serializerFactory;
    }

    /**
     * Method for setting "blueprint" {@link SerializerProvider} instance
     * to use as the base for actual provider instances to use for handling
     * caching of {@link JsonSerializer} instances.
     */
    public ObjectMapper setSerializerProvider(DefaultSerializerProvider p) {
        _serializerProvider = p;
        return this;
    }

    /**
     * Accessor for the "blueprint" (or, factory) instance, from which instances
     * are created by calling {@link DefaultSerializerProvider#createInstance}.
     * Note that returned instance cannot be directly used as it is not properly
     * configured: to get a properly configured instance to call, use
     * {@link #getSerializerProviderInstance()} instead.
     */
    public SerializerProvider getSerializerProvider() {
        return _serializerProvider;
    }

    /**
     * Accessor for constructing and returning a {@link SerializerProvider}
     * instance that may be used for accessing serializers. This is same as
     * calling {@link #getSerializerProvider}, and calling <code>createInstance</code> on it.
     */
    public SerializerProvider getSerializerProviderInstance() {
        return _serializerProvider();
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
     *<p>
     * Note that this method will CLEAR any previously defined mix-ins
     * for this mapper.
     */
    public ObjectMapper setMixIns(Map<Class<?>, Class<?>> sourceMixins)
    {
        // NOTE: does NOT change possible externally configured resolver, just local defs
        _mixIns.setLocalDefinitions(sourceMixins);
        return this;
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
    public ObjectMapper addMixIn(Class<?> target, Class<?> mixinSource)
    {
        _mixIns.addLocalDefinition(target, mixinSource);
        return this;
    }

    /**
     * Method that can be called to specify given resolver for locating
     * mix-in classes to use, overriding directly added mappings.
     * Note that direct mappings are not cleared, but they are only applied
     * if resolver does not provide mix-in matches.
     */
    public ObjectMapper setMixInResolver(ClassIntrospector.MixInResolver resolver)
    {
        SimpleMixInResolver r = _mixIns.withOverrides(resolver);
        if (r != _mixIns) {
            _mixIns = r;
            _deserializationConfig = new DeserializationConfig(_deserializationConfig, r);
            _serializationConfig = new SerializationConfig(_serializationConfig, r);
        }
        return this;
    }
    
    public Class<?> findMixInClassFor(Class<?> cls) {
        return _mixIns.findMixInClassFor(cls);
    }

    // For testing only:
    public int mixInCount() {
        return _mixIns.localSize();
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
     * Method for setting currently configured default {@link VisibilityChecker},
     * object used for determining whether given property element
     * (method, field, constructor) can be auto-detected or not.
     * This default checker is used as the base visibility:
     * per-class overrides (both via annotations and per-type config overrides)
     * can further change these settings.
     */
    public ObjectMapper setVisibility(VisibilityChecker<?> vc) {
        _configOverrides.setDefaultVisibility(vc);
        return this;
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
        VisibilityChecker<?> vc = _configOverrides.getDefaultVisibility();
        vc = vc.withVisibility(forMethod, visibility);
        _configOverrides.setDefaultVisibility(vc);
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
     * Method for setting {@link AnnotationIntrospector} used by this
     * mapper instance for both serialization and deserialization.
     * Note that doing this will replace the current introspector, which
     * may lead to unavailability of core Jackson annotations.
     * If you want to combine handling of multiple introspectors,
     * have a look at {@link com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair}.
     * 
     * @see com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair
     */
    public ObjectMapper setAnnotationIntrospector(AnnotationIntrospector ai) {
        _serializationConfig = _serializationConfig.with(ai);
        _deserializationConfig = _deserializationConfig.with(ai);
        return this;
    }

    /**
     * Method for changing {@link AnnotationIntrospector} instances used
     * by this mapper instance for serialization and deserialization,
     * specifying them separately so that different introspection can be
     * used for different aspects
     * 
     * @param serializerAI {@link AnnotationIntrospector} to use for configuring
     *    serialization
     * @param deserializerAI {@link AnnotationIntrospector} to use for configuring
     *    deserialization
     * 
     * @see com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair
     */
    public ObjectMapper setAnnotationIntrospectors(AnnotationIntrospector serializerAI,
            AnnotationIntrospector deserializerAI) {
        _serializationConfig = _serializationConfig.with(serializerAI);
        _deserializationConfig = _deserializationConfig.with(deserializerAI);
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

    public PropertyNamingStrategy getPropertyNamingStrategy() {
        // arbitrary choice but let's do:
        return _serializationConfig.getPropertyNamingStrategy();
    }

    /**
     * Method for specifying {@link PrettyPrinter} to use when "default pretty-printing"
     * is enabled (by enabling {@link SerializationFeature#INDENT_OUTPUT})
     * 
     * @param pp Pretty printer to use by default.
     * 
     * @return This mapper, useful for call-chaining
     */
    public ObjectMapper setDefaultPrettyPrinter(PrettyPrinter pp) {
        _serializationConfig = _serializationConfig.withDefaultPrettyPrinter(pp);
        return this;
    }

    /*
    /**********************************************************
    /* Configuration: global-default/per-type override settings
    /**********************************************************
     */
    
    /**
     * Convenience method, equivalent to calling:
     *<pre>
     *  setPropertyInclusion(JsonInclude.Value.construct(incl, incl));
     *</pre>
     */
    public ObjectMapper setSerializationInclusion(JsonInclude.Include incl) {
        return setDefaultPropertyInclusion(JsonInclude.Value.construct(incl, incl));
    }

    /**
     * Method for setting default POJO property inclusion strategy for serialization,
     * applied for all properties for which there are no per-type or per-property
     * overrides (via annotations or config overrides).
     */
    public ObjectMapper setDefaultPropertyInclusion(JsonInclude.Value incl) {
        _configOverrides.setDefaultInclusion(incl);
        return this;
    }

    /**
     * Short-cut for:
     *<pre>
     *  setDefaultPropertyInclusion(JsonInclude.Value.construct(incl, incl));
     *</pre>
     */
    public ObjectMapper setDefaultPropertyInclusion(JsonInclude.Include incl) {
        _configOverrides.setDefaultInclusion(JsonInclude.Value.construct(incl, incl));
        return this;
    }

    /**
     * Method for setting default Setter configuration, regarding things like
     * merging, null-handling; used for properties for which there are
     * no per-type or per-property overrides (via annotations or config overrides).
     */
    public ObjectMapper setDefaultSetterInfo(JsonSetter.Value v) {
        _configOverrides.setDefaultSetterInfo(v);
        return this;
    }

    /**
     * Method for setting auto-detection visibility definition
     * defaults, which are in effect unless overridden by
     * annotations (like <code>JsonAutoDetect</code>) or per-type
     * visibility overrides.
     */
    public ObjectMapper setDefaultVisibility(JsonAutoDetect.Value vis) {
        _configOverrides.setDefaultVisibility(VisibilityChecker.Std.construct(vis));
        return this;
    }

    /**
     * Method for setting default Setter configuration, regarding things like
     * merging, null-handling; used for properties for which there are
     * no per-type or per-property overrides (via annotations or config overrides).
     */
    public ObjectMapper setDefaultMergeable(Boolean b) {
        _configOverrides.setDefaultMergeable(b);
        return this;
    }

    /*
    /**********************************************************
    /* Type information configuration
    /**********************************************************
     */

    /**
     * Convenience method that is equivalent to calling
     *<pre>
     *  enableDefaultTyping(DefaultTyping.OBJECT_AND_NON_CONCRETE);
     *</pre>
     *<p>
     * NOTE: use of Default Typing can be a potential security risk if incoming
     * content comes from untrusted sources, and it is recommended that this
     * is either not done, or, if enabled, use {@link #setDefaultTyping}
     * passing a custom {@link TypeResolverBuilder} implementation that white-lists
     * legal types to use.
     */
    public ObjectMapper enableDefaultTyping() {
        return enableDefaultTyping(DefaultTyping.OBJECT_AND_NON_CONCRETE);
    }

    /**
     * Convenience method that is equivalent to calling
     *<pre>
     *  enableDefaultTyping(dti, JsonTypeInfo.As.WRAPPER_ARRAY);
     *</pre>
     *<p>
     * NOTE: use of Default Typing can be a potential security risk if incoming
     * content comes from untrusted sources, and it is recommended that this
     * is either not done, or, if enabled, use {@link #setDefaultTyping}
     * passing a custom {@link TypeResolverBuilder} implementation that white-lists
     * legal types to use.
     */
    public ObjectMapper enableDefaultTyping(DefaultTyping dti) {
        return enableDefaultTyping(dti, JsonTypeInfo.As.WRAPPER_ARRAY);
    }

    /**
     * Method for enabling automatic inclusion of type information, needed
     * for proper deserialization of polymorphic types (unless types
     * have been annotated with {@link com.fasterxml.jackson.annotation.JsonTypeInfo}).
     *<P>
     * NOTE: use of <code>JsonTypeInfo.As#EXTERNAL_PROPERTY</code> <b>NOT SUPPORTED</b>;
     * and attempts of do so will throw an {@link IllegalArgumentException} to make
     * this limitation explicit.
     *<p>
     * NOTE: use of Default Typing can be a potential security risk if incoming
     * content comes from untrusted sources, and it is recommended that this
     * is either not done, or, if enabled, use {@link #setDefaultTyping}
     * passing a custom {@link TypeResolverBuilder} implementation that white-lists
     * legal types to use.
     * 
     * @param applicability Defines kinds of types for which additional type information
     *    is added; see {@link DefaultTyping} for more information.
     */
    public ObjectMapper enableDefaultTyping(DefaultTyping applicability, JsonTypeInfo.As includeAs)
    {
        /* 18-Sep-2014, tatu: Let's add explicit check to ensure no one tries to
         *   use "As.EXTERNAL_PROPERTY", since that will not work (with 2.5+)
         */
        if (includeAs == JsonTypeInfo.As.EXTERNAL_PROPERTY) {
            throw new IllegalArgumentException("Cannot use includeAs of "+includeAs);
        }
        
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
     *<p>
     * NOTE: use of Default Typing can be a potential security risk if incoming
     * content comes from untrusted sources, and it is recommended that this
     * is either not done, or, if enabled, use {@link #setDefaultTyping}
     * passing a custom {@link TypeResolverBuilder} implementation that white-lists
     * legal types to use.
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
     *<p>
     * NOTE: use of Default Typing can be a potential security risk if incoming
     * content comes from untrusted sources, so care should be taken to use
     * a {@link TypeResolverBuilder} that can limit allowed classes to
     * deserialize.
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

    public void registerSubtypes(Collection<Class<?>> subtypes) {
        getSubtypeResolver().registerSubtypes(subtypes);
    }

    /*
    /**********************************************************
    /* Configuration, basic type handling
    /**********************************************************
     */

    /**
     * Accessor for getting a mutable configuration override object for
     * given type, needed to add or change per-type overrides applied
     * to properties of given type.
     * Usage is through returned object by colling "setter" methods, which
     * directly modify override object and take effect directly.
     * For example you can do
     *<pre>
     *   mapper.configOverride(java.util.Date.class)
     *       .setFormat(JsonFormat.Value.forPattern("yyyy-MM-dd"));
     *<pre>
     * to change the default format to use for properties of type
     * {@link java.util.Date} (possibly further overridden by per-property
     * annotations)
     */
    public MutableConfigOverride configOverride(Class<?> type) {
        return _configOverrides.findOrCreateOverride(type);
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

    /**
     * Method that allows overriding of the underlying {@link DeserializationConfig}
     * object.
     * It is added as a fallback method that may be used if no other configuration
     * modifier method works: it should not be used if there are alternatives,
     * and its use is generally discouraged.
     *<p>
     * <b>NOTE</b>: only use this method if you know what you are doing -- it allows
     * by-passing some of checks applied to other configuration methods.
     * Also keep in mind that as with all configuration of {@link ObjectMapper},
     * this is only thread-safe if done before calling any deserialization methods.
     */
    public ObjectMapper setConfig(DeserializationConfig config) {
        _deserializationConfig = config;
        return this;
    }

    /*
    /**********************************************************
    /* Configuration, serialization
    /**********************************************************
     */

    /**
     * Method for configuring this mapper to use specified {@link FilterProvider} for
     * mapping Filter Ids to actual filter instances.
     *<p>
     * Note that usually it is better to use method {@link #writer(FilterProvider)};
     * however, sometimes
     * this method is more convenient. For example, some frameworks only allow configuring
     * of ObjectMapper instances and not {@link ObjectWriter}s.
     */
    public ObjectMapper setFilterProvider(FilterProvider filterProvider) {
        _serializationConfig = _serializationConfig.withFilters(filterProvider);
        return this;
    }

    /**
     * Method that will configure default {@link Base64Variant} that
     * <code>byte[]</code> serializers and deserializers will use.
     * 
     * @param v Base64 variant to use
     * 
     * @return This mapper, for convenience to allow chaining
     */
    public ObjectMapper setBase64Variant(Base64Variant v) {
        _serializationConfig = _serializationConfig.with(v);
        _deserializationConfig = _deserializationConfig.with(v);
        return this;
    }

    /**
     * Method that allows overriding of the underlying {@link SerializationConfig}
     * object, which contains serialization-specific configuration settings.
     * It is added as a fallback method that may be used if no other configuration
     * modifier method works: it should not be used if there are alternatives,
     * and its use is generally discouraged.
     *<p>
     * <b>NOTE</b>: only use this method if you know what you are doing -- it allows
     * by-passing some of checks applied to other configuration methods.
     * Also keep in mind that as with all configuration of {@link ObjectMapper},
     * this is only thread-safe if done before calling any serialization methods.
     */
    public ObjectMapper setConfig(SerializationConfig config) {
        _serializationConfig = config;
        return this;
    }
    
    /*
    /**********************************************************
    /* Configuration, other
    /**********************************************************
     */

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

    public DateFormat getDateFormat() {
        // arbitrary choice but let's do:
        return _serializationConfig.getDateFormat();
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
     * @since 2.6
     */
    public InjectableValues getInjectableValues() {
        return _injectableValues;
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
     * Default value used is UTC (NOT default TimeZone of JVM).
     */
    public ObjectMapper setTimeZone(TimeZone tz) {
        _deserializationConfig = _deserializationConfig.with(tz);
        _serializationConfig = _serializationConfig.with(tz);
        return this;
    }

    /*
    /**********************************************************
    /* Configuration, simple features: MapperFeature
    /**********************************************************
     */

    /**
     * Method for checking whether given {@link MapperFeature} is enabled.
     */
    public boolean isEnabled(MapperFeature f) {
        // ok to use either one, should be kept in sync
        return _serializationConfig.isEnabled(f);
    }
    
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
    
    /*
    /**********************************************************
    /* Configuration, simple features: SerializationFeature
    /**********************************************************
     */

    /**
     * Method for checking whether given serialization-specific
     * feature is enabled.
     */
    public boolean isEnabled(SerializationFeature f) {
        return _serializationConfig.isEnabled(f);
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
    
    /*
    /**********************************************************
    /* Configuration, simple features: DeserializationFeature
    /**********************************************************
     */

    /**
     * Method for checking whether given deserialization-specific
     * feature is enabled.
     */
    public boolean isEnabled(DeserializationFeature f) {
        return _deserializationConfig.isEnabled(f);
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
    
    /*
    /**********************************************************
    /* Configuration, accessing features
    /**********************************************************
     */

    /**
     * Convenience method, equivalent to:
     *<pre>
     *  tokenStreamFactory().isEnabled(f);
     *</pre>
     */
    public boolean isEnabled(JsonFactory.Feature f) {
        return _jsonFactory.isEnabled(f);
    }

    public boolean isEnabled(JsonParser.Feature f) {
        return _deserializationConfig.isEnabled(f, _jsonFactory);
    }

    public boolean isEnabled(JsonGenerator.Feature f) {
        return _serializationConfig.isEnabled(f, _jsonFactory);
    }

    /*
    /**********************************************************
    /* Public API: constructing Parsers that are properly linked
    /* to `ObjectReadContext`
    /**********************************************************
     */

    /**
     * Factory method for constructing {@link JsonParser} that is properly
     * wired to allow callbacks for deserialization: basically
     * constructs a {@link ObjectReadContext} and then calls
     * {@link TokenStreamFactory#createParser(ObjectReadContext,java.io.File)}.
     *
     * @since 3.0
     */
    public JsonParser createParser(File src) throws IOException {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return ctxt.assignAndReturnParser(_jsonFactory.createParser(ctxt, src));
    }

    /**
     * Factory method for constructing {@link JsonParser} that is properly
     * wired to allow callbacks for deserialization: basically
     * constructs a {@link ObjectReadContext} and then calls
     * {@link TokenStreamFactory#createParser(ObjectReadContext,java.net.URL)}.
     *
     * @since 3.0
     */
    public JsonParser createParser(URL src) throws IOException {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return ctxt.assignAndReturnParser(_jsonFactory.createParser(ctxt, src));
    }

    /**
     * Factory method for constructing {@link JsonParser} that is properly
     * wired to allow callbacks for deserialization: basically
     * constructs a {@link ObjectReadContext} and then calls
     * {@link TokenStreamFactory#createParser(ObjectReadContext,InputStream)}.
     *
     * @since 3.0
     */
    public JsonParser createParser(InputStream in) throws IOException {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return ctxt.assignAndReturnParser(_jsonFactory.createParser(ctxt, in));
    }

    /**
     * Factory method for constructing {@link JsonParser} that is properly
     * wired to allow callbacks for deserialization: basically
     * constructs a {@link ObjectReadContext} and then calls
     * {@link TokenStreamFactory#createParser(ObjectReadContext,Reader)}.
     *
     * @since 3.0
     */
    public JsonParser createParser(Reader r) throws IOException {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return ctxt.assignAndReturnParser(_jsonFactory.createParser(ctxt, r));
    }

    /**
     * Factory method for constructing {@link JsonParser} that is properly
     * wired to allow callbacks for deserialization: basically
     * constructs a {@link ObjectReadContext} and then calls
     * {@link TokenStreamFactory#createParser(ObjectReadContext,byte[])}.
     *
     * @since 3.0
     */
    public JsonParser createParser(byte[] data) throws IOException {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return ctxt.assignAndReturnParser(_jsonFactory.createParser(ctxt, data));
    }

    /**
     * Factory method for constructing {@link JsonParser} that is properly
     * wired to allow callbacks for deserialization: basically
     * constructs a {@link ObjectReadContext} and then calls
     * {@link TokenStreamFactory#createParser(ObjectReadContext,byte[],int,int)}.
     *
     * @since 3.0
     */
    public JsonParser createParser(byte[] data, int offset, int len) throws IOException {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return ctxt.assignAndReturnParser(_jsonFactory.createParser(ctxt, data, offset, len));
    }

    /**
     * Factory method for constructing {@link JsonParser} that is properly
     * wired to allow callbacks for deserialization: basically
     * constructs a {@link ObjectReadContext} and then calls
     * {@link TokenStreamFactory#createParser(ObjectReadContext,String)}.
     *
     * @since 3.0
     */
    public JsonParser createParser(String content) throws IOException {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return ctxt.assignAndReturnParser(_jsonFactory.createParser(ctxt, content));
    }

    /**
     * Factory method for constructing {@link JsonParser} that is properly
     * wired to allow callbacks for deserialization: basically
     * constructs a {@link ObjectReadContext} and then calls
     * {@link TokenStreamFactory#createParser(ObjectReadContext,char[])}.
     *
     * @since 3.0
     */
    public JsonParser createParser(char[] content) throws IOException {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return ctxt.assignAndReturnParser(_jsonFactory.createParser(ctxt, content));
    }

    /**
     * Factory method for constructing {@link JsonParser} that is properly
     * wired to allow callbacks for deserialization: basically
     * constructs a {@link ObjectReadContext} and then calls
     * {@link TokenStreamFactory#createParser(ObjectReadContext,char[],int,int)}.
     *
     * @since 3.0
     */
    public JsonParser createParser(char[] content, int offset, int len) throws IOException {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return ctxt.assignAndReturnParser(_jsonFactory.createParser(ctxt, content, offset, len));
    }

    /**
     * Factory method for constructing {@link JsonParser} that is properly
     * wired to allow callbacks for deserialization: basically
     * constructs a {@link ObjectReadContext} and then calls
     * {@link TokenStreamFactory#createParser(ObjectReadContext,DataInput)}.
     *
     * @since 3.0
     */
    public JsonParser createParser(DataInput content) throws IOException {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return ctxt.assignAndReturnParser(_jsonFactory.createParser(ctxt, content));
    }

    /**
     * Factory method for constructing non-blocking {@link JsonParser} that is properly
     * wired to allow configuration access (and, if relevant for parser, callbacks):
     * essentially constructs a {@link ObjectReadContext} and then calls
     * {@link TokenStreamFactory#createParser(ObjectReadContext,DataInput)}.
     *
     * @since 3.0
     */
    public JsonParser createNonBlockingByteArrayParser() throws IOException {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return ctxt.assignAndReturnParser(_jsonFactory.createNonBlockingByteArrayParser(ctxt));
    }

    /*
    /**********************************************************
    /* Public API: constructing Generator that are properly linked
    /* to `ObjectWriteContext`
    /**********************************************************
     */

    /**
     * Factory method for constructing {@link JsonGenerator} that is properly
     * wired to allow callbacks for serialization: basically
     * constructs a {@link ObjectWriteContext} and then calls
     * {@link TokenStreamFactory#createGenerator(ObjectWriteContext,OutputStream)}.
     *
     * @since 3.0
     */
    public JsonGenerator createGenerator(OutputStream out) throws IOException {
        return _jsonFactory.createGenerator(_serializerProvider(), out);
    }

    /**
     * Factory method for constructing {@link JsonGenerator} that is properly
     * wired to allow callbacks for serialization: basically
     * constructs a {@link ObjectWriteContext} and then calls
     * {@link TokenStreamFactory#createGenerator(ObjectWriteContext,OutputStream,JsonEncoding)}.
     *
     * @since 3.0
     */
    public JsonGenerator createGenerator(OutputStream out, JsonEncoding enc) throws IOException {
        return _jsonFactory.createGenerator(_serializerProvider(), out, enc);
    }

    /**
     * Factory method for constructing {@link JsonGenerator} that is properly
     * wired to allow callbacks for serialization: basically
     * constructs a {@link ObjectWriteContext} and then calls
     * {@link TokenStreamFactory#createGenerator(ObjectWriteContext,Writer)}.
     *
     * @since 3.0
     */
    public JsonGenerator createGenerator(Writer w) throws IOException {
        return _jsonFactory.createGenerator(_serializerProvider(), w);
    }

    /**
     * Factory method for constructing {@link JsonGenerator} that is properly
     * wired to allow callbacks for serialization: basically
     * constructs a {@link ObjectWriteContext} and then calls
     * {@link TokenStreamFactory#createGenerator(ObjectWriteContext,File,JsonEncoding)}.
     *
     * @since 3.0
     */
    public JsonGenerator createGenerator(File f, JsonEncoding enc)
        throws IOException {
        return _jsonFactory.createGenerator(_serializerProvider(), f, enc);
    }

    /**
     * Factory method for constructing {@link JsonGenerator} that is properly
     * wired to allow callbacks for serialization: basically
     * constructs a {@link ObjectWriteContext} and then calls
     * {@link TokenStreamFactory#createGenerator(ObjectWriteContext,DataOutput)}.
     *
     * @since 3.0
     */
    public JsonGenerator createGenerator(DataOutput out) throws IOException {
        return _jsonFactory.createGenerator(_serializerProvider(), out);
    }

    /*
    /**********************************************************
    /* Public API deserialization, main methods
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
     * cannot be introspected when using this method.
     * 
     * @throws IOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws JsonParseException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws JsonMappingException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(JsonParser p, Class<T> valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        DeserializationContext ctxt = createDeserializationContext(p);
        return (T) _readValue(ctxt, p, _typeFactory.constructType(valueType));
    } 

    /**
     * Method to deserialize JSON content into a Java type, reference
     * to which is passed as argument. Type is passed using so-called
     * "super type token" (see )
     * and specifically needs to be used if the root type is a 
     * parameterized (generic) container type.
     * 
     * @throws IOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws JsonParseException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws JsonMappingException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(JsonParser p, TypeReference<T> valueTypeRef)
        throws IOException, JsonParseException, JsonMappingException
    {
        DeserializationContext ctxt = createDeserializationContext(p);
        return (T) _readValue(ctxt, p, _typeFactory.constructType(valueTypeRef));
    }

    /**
     * Method to deserialize JSON content into a Java type, reference
     * to which is passed as argument. Type is passed using 
     * Jackson specific type; instance of which can be constructed using
     * {@link TypeFactory}.
     * 
     * @throws IOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws JsonParseException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws JsonMappingException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     */
    @SuppressWarnings("unchecked")
    public final <T> T readValue(JsonParser p, ResolvedType valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        DeserializationContext ctxt = createDeserializationContext(p);
        return (T) _readValue(ctxt, p, (JavaType) valueType);
    }

    /**
     * Type-safe overloaded method, basically alias for {@link #readValue(JsonParser, Class)}.
     * 
     * @throws IOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws JsonParseException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws JsonMappingException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(JsonParser p, JavaType valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        DeserializationContext ctxt = createDeserializationContext(p);
        return (T) _readValue(ctxt, p, valueType);
    }

    /**
     * Method to deserialize JSON content as tree expressed
     * using set of {@link JsonNode} instances. Returns
     * root of the resulting tree (where root can consist
     * of just a single node if the current event is a
     * value event, not container).
     * 
     * @return a {@link JsonNode}, if valid JSON content found; null
     *   if input has no content to bind -- note, however, that if
     *   JSON <code>null</code> token is found, it will be represented
     *   as a non-null {@link JsonNode} (one that returns <code>true</code>
     *   for {@link JsonNode#isNull()}
     * 
     * @throws IOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws JsonParseException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     */
    public <T extends TreeNode> T readTree(JsonParser p)
        throws IOException, JsonProcessingException
    {
        /* 02-Mar-2009, tatu: One twist; deserialization provider
         *   will map JSON null straight into Java null. But what
         *   we want to return is the "null node" instead.
         */
        /* 05-Aug-2011, tatu: Also, must check for EOF here before
         *   calling readValue(), since that'll choke on it otherwise
         */
        JsonToken t = p.currentToken();
        if (t == null) {
            t = p.nextToken();
            if (t == null) {
                return null;
            }
        }
        DeserializationContext ctxt = createDeserializationContext(p);
        JsonNode n = (JsonNode) _readValue(ctxt, p, JSON_NODE_TYPE);
        if (n == null) {
            n = getNodeFactory().nullNode();
        }
        @SuppressWarnings("unchecked")
        T result = (T) n;
        return result;
    }

    /**
     * Convenience method, equivalent in function to:
     *<pre>
     *   readerFor(valueType).readValues(p);
     *</pre>
     *<p>
     * Method for reading sequence of Objects from parser stream.
     * Sequence can be either root-level "unwrapped" sequence (without surrounding
     * JSON array), or a sequence contained in a JSON Array.
     * In either case {@link JsonParser} <b>MUST</b> point to the first token of
     * the first element, OR not point to any token (in which case it is advanced
     * to the next token). This means, specifically, that for wrapped sequences,
     * parser MUST NOT point to the surrounding <code>START_ARRAY</code> (one that
     * contains values to read) but rather to the token following it which is the first
     * token of the first value to read.
     *<p>
     * Note that {@link ObjectReader} has more complete set of variants.
     */
    public <T> MappingIterator<T> readValues(JsonParser p, JavaType valueType)
        throws IOException, JsonProcessingException
    {
        DeserializationContext ctxt = createDeserializationContext(p);
        JsonDeserializer<?> deser = _findRootDeserializer(ctxt, valueType);
        // false -> do NOT close JsonParser (since caller passed it)
        return new MappingIterator<T>(valueType, p, ctxt, deser,
                false, null);
    }

    /**
     * Convenience method, equivalent in function to:
     *<pre>
     *   readerFor(valueType).readValues(p);
     *</pre>
     *<p>
     * Type-safe overload of {@link #readValues(JsonParser, JavaType)}.
     */
    public <T> MappingIterator<T> readValues(JsonParser p, Class<T> valueType)
        throws IOException, JsonProcessingException
    {
        return readValues(p, _typeFactory.constructType(valueType));
    }

    /*
    /**********************************************************
    /* Public API: deserialization
    /* (mapping from token stream to Java types)
    /**********************************************************
     */

    /**
     * Method to deserialize JSON content as tree expressed
     * using set of {@link JsonNode} instances.
     * Returns root of the resulting tree (where root can consist
     * of just a single node if the current event is a
     * value event, not container).
     *<p>
     * If a low-level I/O problem (missing input, network error) occurs,
     * a {@link IOException} will be thrown.
     * If a parsing problem occurs (invalid JSON),
     * {@link JsonParseException} will be thrown.
     * If no content is found from input (end-of-input), Java
     * <code>null</code> will be returned.
     * 
     * @param in Input stream used to read JSON content
     *   for building the JSON tree.
     * 
     * @return a {@link JsonNode}, if valid JSON content found; null
     *   if input has no content to bind -- note, however, that if
     *   JSON <code>null</code> token is found, it will be represented
     *   as a non-null {@link JsonNode} (one that returns <code>true</code>
     *   for {@link JsonNode#isNull()}
     *   
     * @throws JsonParseException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     */
    public JsonNode readTree(InputStream in) throws IOException
    {
        DeserializationContext ctxt = createDeserializationContext();
        return _readTreeAndClose(ctxt, _jsonFactory.createParser(ctxt, in));
    }

    /**
     * Method to deserialize JSON content as tree expressed
     * using set of {@link JsonNode} instances.
     * Returns root of the resulting tree (where root can consist
     * of just a single node if the current event is a
     * value event, not container).
     *<p>
     * If a low-level I/O problem (missing input, network error) occurs,
     * a {@link IOException} will be thrown.
     * If a parsing problem occurs (invalid JSON),
     * {@link JsonParseException} will be thrown.
     * If no content is found from input (end-of-input), Java
     * <code>null</code> will be returned.
     *
     * @param r Reader used to read JSON content
     *   for building the JSON tree.
     * 
     * @return a {@link JsonNode}, if valid JSON content found; null
     *   if input has no content to bind -- note, however, that if
     *   JSON <code>null</code> token is found, it will be represented
     *   as a non-null {@link JsonNode} (one that returns <code>true</code>
     *   for {@link JsonNode#isNull()}
     */
    public JsonNode readTree(Reader r) throws IOException {
        DeserializationContext ctxt = createDeserializationContext();
        return _readTreeAndClose(ctxt, _jsonFactory.createParser(ctxt, r));
    }

    /**
     * Method to deserialize JSON content as tree expressed using set of {@link JsonNode} instances.
     * Returns root of the resulting tree (where root can consist of just a single node if the current
     * event is a value event, not container).
     *<p>
     * If a low-level I/O problem (missing input, network error) occurs,
     * a {@link IOException} will be thrown.
     * If a parsing problem occurs (invalid JSON),
     * {@link JsonParseException} will be thrown.
     * If no content is found from input (end-of-input), Java
     * <code>null</code> will be returned.
     *
     * @param content JSON content to parse to build the JSON tree.
     * 
     * @return a {@link JsonNode}, if valid JSON content found; null
     *   if input has no content to bind -- note, however, that if
     *   JSON <code>null</code> token is found, it will be represented
     *   as a non-null {@link JsonNode} (one that returns <code>true</code>
     *   for {@link JsonNode#isNull()}
     *
     * @throws JsonParseException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     */
    public JsonNode readTree(String content) throws IOException {
        DeserializationContext ctxt = createDeserializationContext();
        return _readTreeAndClose(ctxt, _jsonFactory.createParser(ctxt, content));
    }

    /**
     * Method to deserialize JSON content as tree expressed using set of {@link JsonNode} instances.
     * Returns root of the resulting tree (where root can consist of just a single node if the current
     * event is a value event, not container).
     *
     * @param content JSON content to parse to build the JSON tree.
     * 
     * @return a {@link JsonNode}, if valid JSON content found; null
     *   if input has no content to bind -- note, however, that if
     *   JSON <code>null</code> token is found, it will be represented
     *   as a non-null {@link JsonNode} (one that returns <code>true</code>
     *   for {@link JsonNode#isNull()}
     *
     * @throws JsonParseException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     */
    public JsonNode readTree(byte[] content) throws IOException {
        DeserializationContext ctxt = createDeserializationContext();
        return _readTreeAndClose(ctxt, _jsonFactory.createParser(ctxt, content));
    }
    
    /**
     * Method to deserialize JSON content as tree expressed using set of {@link JsonNode} instances.
     * Returns root of the resulting tree (where root can consist of just a single node if the current
     * event is a value event, not container).
     *
     * @param file File of which contents to parse as JSON for building a tree instance
     * 
     * @return a {@link JsonNode}, if valid JSON content found; null
     *   if input has no content to bind -- note, however, that if
     *   JSON <code>null</code> token is found, it will be represented
     *   as a non-null {@link JsonNode} (one that returns <code>true</code>
     *   for {@link JsonNode#isNull()}
     * 
     * @throws IOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws JsonParseException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     */
    public JsonNode readTree(File file)
        throws IOException, JsonProcessingException
    {
        DeserializationContext ctxt = createDeserializationContext();
        return _readTreeAndClose(ctxt, _jsonFactory.createParser(ctxt, file));
    }

    /**
     * Method to deserialize JSON content as tree expressed using set of {@link JsonNode} instances.
     * Returns root of the resulting tree (where root can consist of just a single node if the current
     * event is a value event, not container).
     *
     * @param source URL to use for fetching contents to parse as JSON for building a tree instance
     * 
     * @return a {@link JsonNode}, if valid JSON content found; null
     *   if input has no content to bind -- note, however, that if
     *   JSON <code>null</code> token is found, it will be represented
     *   as a non-null {@link JsonNode} (one that returns <code>true</code>
     *   for {@link JsonNode#isNull()}
     * 
     * @throws IOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws JsonParseException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     */
    public JsonNode readTree(URL source) throws IOException {
        DeserializationContext ctxt = createDeserializationContext();
        return _readTreeAndClose(ctxt, _jsonFactory.createParser(ctxt, source));
    }

    /*
    /**********************************************************
    /* Public API serialization
    /* (mapping from Java types to token streams)
    /**********************************************************
     */

    /**
     * Method that can be used to serialize any Java value as
     * JSON output, using provided {@link JsonGenerator}.
     */
    public void writeValue(JsonGenerator g, Object value)
        throws IOException, JsonGenerationException, JsonMappingException
    {
        SerializationConfig config = getSerializationConfig();
        // 04-Oct-2017, tatu: Generator should come properly configured and we should not
        //   change its state in any way, I think (at least with Jackson 3.0)
        /*
        if (config.isEnabled(SerializationFeature.INDENT_OUTPUT)) {
            if (g.getPrettyPrinter() == null) {
                g.setPrettyPrinter(config.constructDefaultPrettyPrinter());
            }
        }
        */
        if (config.isEnabled(SerializationFeature.CLOSE_CLOSEABLE) && (value instanceof Closeable)) {
            _writeCloseableValue(g, value, config);
        } else {
            _serializerProvider(config).serializeValue(g, value);
            if (config.isEnabled(SerializationFeature.FLUSH_AFTER_WRITE_VALUE)) {
                g.flush();
            }
        }
    }

    /*
    /**********************************************************
    /* Public API: Tree Model support
    /**********************************************************
     */

    public void writeTree(JsonGenerator g, TreeNode rootNode)
        throws IOException, JsonProcessingException
    {
        SerializationConfig config = getSerializationConfig();
        _serializerProvider(config).serializeValue(g, rootNode);
        if (config.isEnabled(SerializationFeature.FLUSH_AFTER_WRITE_VALUE)) {
            g.flush();
        }
    }
    
    /**
     * Method to serialize given JSON Tree, using generator
     * provided.
     */
    public void writeTree(JsonGenerator g, JsonNode rootNode)
        throws IOException, JsonProcessingException
    {
        SerializationConfig config = getSerializationConfig();
        _serializerProvider(config).serializeValue(g, rootNode);
        if (config.isEnabled(SerializationFeature.FLUSH_AFTER_WRITE_VALUE)) {
            g.flush();
        }
    }
    
    /**
     *<p>
     * Note: return type is co-variant, as basic ObjectCodec
     * abstraction cannot refer to concrete node types (as it's
     * part of core package, whereas impls are part of mapper
     * package)
     */
    public ObjectNode createObjectNode() {
        return _deserializationConfig.getNodeFactory().objectNode();
    }

    /**
     *<p>
     * Note: return type is co-variant, as basic ObjectCodec
     * abstraction cannot refer to concrete node types (as it's
     * part of core package, whereas impls are part of mapper
     * package)
     */
    public ArrayNode createArrayNode() {
        return _deserializationConfig.getNodeFactory().arrayNode();
    }

    /**
     * Method for constructing a {@link JsonParser} out of JSON tree
     * representation.
     * 
     * @param n Root node of the tree that resulting parser will read from
     */
    public JsonParser treeAsTokens(TreeNode n) {
        DeserializationContext ctxt = createDeserializationContext();
        return new TreeTraversingParser((JsonNode) n, ctxt);
    }

    /**
     * Convenience conversion method that will bind data given JSON tree
     * contains into specific value (usually bean) type.
     *<p>
     * Functionally equivalent to:
     *<pre>
     *   objectMapper.convertValue(n, valueClass);
     *</pre>
     */
    @SuppressWarnings("unchecked")
    public <T> T treeToValue(TreeNode n, Class<T> valueType)
        throws JsonProcessingException
    {
        try {
            // Simple cast when we just want to cast to, say, ObjectNode
            // ... one caveat; while everything is Object.class, let's not take shortcut
            if (valueType != Object.class && valueType.isAssignableFrom(n.getClass())) {
                return (T) n;
            }
            // 20-Apr-2016, tatu: Another thing: for VALUE_EMBEDDED_OBJECT, assume similar
            //    short-cut coercion
            if (n.asToken() == JsonToken.VALUE_EMBEDDED_OBJECT) {
                if (n instanceof POJONode) {
                    Object ob = ((POJONode) n).getPojo();
                    if ((ob == null) || valueType.isInstance(ob)) {
                        return (T) ob;
                    }
                }
            }
            return readValue(treeAsTokens(n), valueType);
        } catch (JsonProcessingException e) {
            throw e;
        } catch (IOException e) { // should not occur, no real i/o...
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /**
     * Reverse of {@link #treeToValue}; given a value (usually bean), will
     * construct equivalent JSON Tree representation. Functionally similar
     * to serializing value into JSON and parsing JSON as tree, but
     * more efficient.
     *<p>
     * NOTE: while results are usually identical to that of serialization followed
     * by deserialization, this is not always the case. In some cases serialization
     * into intermediate representation will retain encapsulation of things like
     * raw value ({@link com.fasterxml.jackson.databind.util.RawValue}) or basic
     * node identity ({@link JsonNode}). If so, result is a valid tree, but values
     * are not re-constructed through actual JSON representation. So if transformation
     * requires actual materialization of JSON (or other data format that this mapper
     * produces), it will be necessary to do actual serialization.
     * 
     * @param <T> Actual node type; usually either basic {@link JsonNode} or
     *  {@link com.fasterxml.jackson.databind.node.ObjectNode}
     * @param fromValue Bean value to convert
     * @return Root node of the resulting JSON tree
     */
    @SuppressWarnings({ "unchecked", "resource" })
    public <T extends JsonNode> T valueToTree(Object fromValue)
        throws IllegalArgumentException
    {
        if (fromValue == null) {
            return null;
        }
        // 06-Oct-2017, tatu: `convertValue()` disables root value wrapping so
        //   do it here too
        SerializationConfig config = getSerializationConfig()
            .without(SerializationFeature.WRAP_ROOT_VALUE);
        DefaultSerializerProvider prov = _serializerProvider(config);
        TokenBuffer buf = TokenBuffer.forValueConversion(prov);
        // Would like to let buffer decide, but it won't have deser config to check so...
        if (isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)) {
            buf = buf.forceUseOfBigDecimal(true);
        }
        JsonNode result;
        try {
            // Equivalent to `writeValue()`, basically:
            prov.serializeValue(buf, fromValue);
            JsonParser p = buf.asParser();
            result = readTree(p);
            p.close();
        } catch (IOException e) { // should not occur, no real i/o...
            throw new IllegalArgumentException(e.getMessage(), e);
        }
        return (T) result;
    } 

    /*
    /**********************************************************
    /* Public API, accessors
    /**********************************************************
     */

    /**
     * Method that can be called to check whether mapper thinks
     * it could serialize an instance of given Class.
     * Check is done
     * by checking whether a serializer can be found for the type.
     *<p>
     * NOTE: since this method does NOT throw exceptions, but internal
     * processing may, caller usually has little information as to why
     * serialization would fail. If you want access to internal {@link Exception},
     * call {@link #canSerialize(Class, AtomicReference)} instead.
     *
     * @return True if mapper can find a serializer for instances of
     *  given class (potentially serializable), false otherwise (not
     *  serializable)
     */
    public boolean canSerialize(Class<?> type) {
        return _serializerProvider().hasSerializerFor(type, null);
    }

    /**
     * Method similar to {@link #canSerialize(Class)} but that can return
     * actual {@link Throwable} that was thrown when trying to construct
     * serializer: this may be useful in figuring out what the actual problem is.
     */
    public boolean canSerialize(Class<?> type, AtomicReference<Throwable> cause) {
        return _serializerProvider().hasSerializerFor(type, cause);
    }
    
    /**
     * Method that can be called to check whether mapper thinks
     * it could deserialize an Object of given type.
     * Check is done by checking whether a registered deserializer can
     * be found or built for the type; if not (either by no mapping being
     * found, or through an <code>Exception</code> being thrown, false
     * is returned.
     *<p>
     * <b>NOTE</b>: in case an exception is thrown during course of trying
     * co construct matching deserializer, it will be effectively swallowed.
     * If you want access to that exception, call
     * {@link #canDeserialize(JavaType, AtomicReference)} instead.
     *
     * @return True if mapper can find a serializer for instances of
     *  given class (potentially serializable), false otherwise (not
     *  serializable)
     */
    public boolean canDeserialize(JavaType type)
    {
        return createDeserializationContext().hasValueDeserializerFor(type, null);
    }

    /**
     * Method similar to {@link #canDeserialize(JavaType)} but that can return
     * actual {@link Throwable} that was thrown when trying to construct
     * serializer: this may be useful in figuring out what the actual problem is.
     */
    public boolean canDeserialize(JavaType type, AtomicReference<Throwable> cause)
    {
        return createDeserializationContext().hasValueDeserializerFor(type, cause);
    }
    
    /*
    /**********************************************************
    /* Public API, deserialization,
    /**********************************************************
     */

    /**
     * Method to deserialize JSON content from given file into given Java type.
     * 
     * @throws IOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws JsonParseException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws JsonMappingException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(File src, Class<T> valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _readMapAndClose(ctxt, _jsonFactory.createParser(ctxt, src),
                _typeFactory.constructType(valueType));
    } 

    /**
     * Method to deserialize JSON content from given file into given Java type.
     * 
     * @throws IOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws JsonParseException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws JsonMappingException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> T readValue(File src, TypeReference valueTypeRef)
        throws IOException, JsonParseException, JsonMappingException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _readMapAndClose(ctxt, _jsonFactory.createParser(ctxt, src),
                _typeFactory.constructType(valueTypeRef));
    } 

    /**
     * Method to deserialize JSON content from given file into given Java type.
     * 
     * @throws IOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws JsonParseException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws JsonMappingException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(File src, JavaType valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _readMapAndClose(ctxt, _jsonFactory.createParser(ctxt, src), valueType);
    }

    /**
     * Method to deserialize JSON content from given resource into given Java type.
     * 
     * @throws IOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws JsonParseException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws JsonMappingException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(URL src, Class<T> valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _readMapAndClose(ctxt,
                _jsonFactory.createParser(ctxt, src), _typeFactory.constructType(valueType));
    } 

    /**
     * Method to deserialize JSON content from given resource into given Java type.
     * 
     * @throws IOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws JsonParseException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws JsonMappingException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> T readValue(URL src, TypeReference valueTypeRef)
        throws IOException, JsonParseException, JsonMappingException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _readMapAndClose(ctxt,
                _jsonFactory.createParser(ctxt, src), _typeFactory.constructType(valueTypeRef));
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(URL src, JavaType valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _readMapAndClose(ctxt,
                _jsonFactory.createParser(ctxt, src), valueType);
    } 

    /**
     * Method to deserialize JSON content from given JSON content String.
     * 
     * @throws IOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws JsonParseException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws JsonMappingException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(String content, Class<T> valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _readMapAndClose(ctxt,
                _jsonFactory.createParser(ctxt, content), _typeFactory.constructType(valueType));
    } 

    /**
     * Method to deserialize JSON content from given JSON content String.
     * 
     * @throws IOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws JsonParseException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws JsonMappingException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> T readValue(String content, TypeReference valueTypeRef)
        throws IOException, JsonParseException, JsonMappingException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _readMapAndClose(ctxt,
                _jsonFactory.createParser(ctxt, content), _typeFactory.constructType(valueTypeRef));
    } 

    /**
     * Method to deserialize JSON content from given JSON content String.
     * 
     * @throws IOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws JsonParseException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws JsonMappingException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(String content, JavaType valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _readMapAndClose(ctxt,
                _jsonFactory.createParser(ctxt, content), valueType);
    }

    @SuppressWarnings("unchecked")
    public <T> T readValue(Reader src, Class<T> valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _readMapAndClose(ctxt,
                _jsonFactory.createParser(ctxt, src), _typeFactory.constructType(valueType));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> T readValue(Reader src, TypeReference valueTypeRef)
        throws IOException, JsonParseException, JsonMappingException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _readMapAndClose(ctxt,
                _jsonFactory.createParser(ctxt, src), _typeFactory.constructType(valueTypeRef));
    }

    @SuppressWarnings("unchecked")
    public <T> T readValue(Reader src, JavaType valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _readMapAndClose(ctxt,
                _jsonFactory.createParser(ctxt, src), valueType);
    }

    @SuppressWarnings("unchecked")
    public <T> T readValue(InputStream src, Class<T> valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _readMapAndClose(ctxt,
                _jsonFactory.createParser(ctxt, src), _typeFactory.constructType(valueType));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> T readValue(InputStream src, TypeReference valueTypeRef)
        throws IOException, JsonParseException, JsonMappingException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _readMapAndClose(ctxt,
                _jsonFactory.createParser(ctxt, src), _typeFactory.constructType(valueTypeRef));
    }

    @SuppressWarnings("unchecked")
    public <T> T readValue(InputStream src, JavaType valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _readMapAndClose(ctxt,
                _jsonFactory.createParser(ctxt, src), valueType);
    }

    @SuppressWarnings("unchecked")
    public <T> T readValue(byte[] src, Class<T> valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _readMapAndClose(ctxt,
                _jsonFactory.createParser(ctxt, src), _typeFactory.constructType(valueType));
    }

    @SuppressWarnings("unchecked")
    public <T> T readValue(byte[] src, int offset, int len, 
                               Class<T> valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _readMapAndClose(ctxt,
                _jsonFactory.createParser(ctxt, src, offset, len), _typeFactory.constructType(valueType));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> T readValue(byte[] src, TypeReference valueTypeRef)
        throws IOException, JsonParseException, JsonMappingException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _readMapAndClose(ctxt,
                _jsonFactory.createParser(ctxt, src), _typeFactory.constructType(valueTypeRef));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> T readValue(byte[] src, int offset, int len,
                           TypeReference valueTypeRef)
        throws IOException, JsonParseException, JsonMappingException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _readMapAndClose(ctxt,
                _jsonFactory.createParser(ctxt, src, offset, len), _typeFactory.constructType(valueTypeRef));
    }

    @SuppressWarnings("unchecked")
    public <T> T readValue(byte[] src, JavaType valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _readMapAndClose(ctxt,
                _jsonFactory.createParser(ctxt, src), valueType);
    }

    @SuppressWarnings("unchecked")
    public <T> T readValue(byte[] src, int offset, int len,
                           JavaType valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _readMapAndClose(ctxt,
                _jsonFactory.createParser(ctxt, src, offset, len), valueType);
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(DataInput src, Class<T> valueType) throws IOException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _readMapAndClose(ctxt,
                _jsonFactory.createParser(ctxt, src), _typeFactory.constructType(valueType));
    }

    @SuppressWarnings("unchecked")
    public <T> T readValue(DataInput src, JavaType valueType) throws IOException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _readMapAndClose(ctxt,
                _jsonFactory.createParser(ctxt, src), valueType);
    }

    /*
    /**********************************************************
    /* Public API: serialization
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
        DefaultSerializerProvider prov = _serializerProvider();
        _configAndWriteValue(prov,
                _jsonFactory.createGenerator(prov, resultFile, JsonEncoding.UTF8), value);
    }

    /**
     * Method that can be used to serialize any Java value as
     * JSON output, using output stream provided (using encoding
     * {@link JsonEncoding#UTF8}).
     *<p>
     * Note: method does not close the underlying stream explicitly
     * here; however, {@link TokenStreamFactory} this mapper uses may choose
     * to close the stream depending on its settings (by default,
     * it will try to close it when {@link JsonGenerator} we construct
     * is closed).
     */
    public void writeValue(OutputStream out, Object value)
        throws IOException, JsonGenerationException, JsonMappingException
    {
        DefaultSerializerProvider prov = _serializerProvider();
        _configAndWriteValue(prov,
                _jsonFactory.createGenerator(prov, out, JsonEncoding.UTF8), value);
    }

    public void writeValue(DataOutput out, Object value)
        throws IOException
    {
        DefaultSerializerProvider prov = _serializerProvider();
        _configAndWriteValue(prov,
                _jsonFactory.createGenerator(prov, out), value);
    }

    /**
     * Method that can be used to serialize any Java value as
     * JSON output, using Writer provided.
     *<p>
     * Note: method does not close the underlying stream explicitly
     * here; however, {@link TokenStreamFactory} this mapper uses may choose
     * to close the stream depending on its settings (by default,
     * it will try to close it when {@link JsonGenerator} we construct
     * is closed).
     */
    public void writeValue(Writer w, Object value)
        throws IOException, JsonGenerationException, JsonMappingException
    {
        DefaultSerializerProvider prov = _serializerProvider();
        _configAndWriteValue(prov, _jsonFactory.createGenerator(prov, w), value);
    }

    /**
     * Method that can be used to serialize any Java value as
     * a String. Functionally equivalent to calling
     * {@link #writeValue(Writer,Object)} with {@link java.io.StringWriter}
     * and constructing String, but more efficient.
     */
    @SuppressWarnings("resource")
    public String writeValueAsString(Object value)
        throws JsonProcessingException
    {
        // alas, we have to pull the recycler directly here...
        SegmentedStringWriter sw = new SegmentedStringWriter(_jsonFactory._getBufferRecycler());
        DefaultSerializerProvider prov = _serializerProvider();
        try {
            _configAndWriteValue(prov, _jsonFactory.createGenerator(prov, sw), value);
        } catch (JsonProcessingException e) {
            throw e;
        } catch (IOException e) { // shouldn't really happen, but is declared as possibility so:
            throw JsonMappingException.fromUnexpectedIOE(e);
        }
        return sw.getAndClear();
    }

    /**
     * Method that can be used to serialize any Java value as
     * a byte array. Functionally equivalent to calling
     * {@link #writeValue(Writer,Object)} with {@link java.io.ByteArrayOutputStream}
     * and getting bytes, but more efficient.
     * Encoding used will be UTF-8.
     */
    @SuppressWarnings("resource")
    public byte[] writeValueAsBytes(Object value)
        throws JsonProcessingException
    {
        DefaultSerializerProvider prov = _serializerProvider();
        ByteArrayBuilder bb = new ByteArrayBuilder(_jsonFactory._getBufferRecycler());
        try {
            _configAndWriteValue(prov,
                    _jsonFactory.createGenerator(prov, bb, JsonEncoding.UTF8), value);
        } catch (JsonProcessingException e) {
            throw e;
        } catch (IOException e) { // shouldn't really happen, but is declared as possibility so:
            throw JsonMappingException.fromUnexpectedIOE(e);
        }
        byte[] result = bb.toByteArray();
        bb.release();
        return result;
    }

    /**
     * Method called to configure the generator as necessary and then
     * call write functionality
     */
    protected final void _configAndWriteValue(DefaultSerializerProvider prov,
            JsonGenerator g, Object value)
        throws IOException
    {
        if (prov.isEnabled(SerializationFeature.CLOSE_CLOSEABLE) && (value instanceof Closeable)) {
            _configAndWriteCloseable(prov, g, value);
            return;
        }
        try {
            prov.serializeValue(g, value);
        } catch (Exception e) {
            ClassUtil.closeOnFailAndThrowAsIOE(g, e);
            return;
        }
        g.close();
    }

    /**
     * Helper method used when value to serialize is {@link Closeable} and its <code>close()</code>
     * method is to be called right after serialization has been called
     */
    private final void _configAndWriteCloseable(DefaultSerializerProvider prov,
            JsonGenerator g, Object value)
        throws IOException
    {
        Closeable toClose = (Closeable) value;
        try {
            prov.serializeValue(g, value);
            Closeable tmpToClose = toClose;
            toClose = null;
            tmpToClose.close();
        } catch (Exception e) {
            ClassUtil.closeOnFailAndThrowAsIOE(g, toClose, e);
            return;
        }
        g.close();
    }

    /**
     * Helper method used when value to serialize is {@link Closeable} and its <code>close()</code>
     * method is to be called right after serialization has been called
     */
    protected final void _writeCloseableValue(JsonGenerator g, Object value, SerializationConfig cfg)
        throws IOException
    {
        Closeable toClose = (Closeable) value;
        try {
            _serializerProvider(cfg).serializeValue(g, value);
            if (cfg.isEnabled(SerializationFeature.FLUSH_AFTER_WRITE_VALUE)) {
                g.flush();
            }
        } catch (Exception e) {
            ClassUtil.closeOnFailAndThrowAsIOE(null, toClose, e);
            return;
        }
        toClose.close();
    }

    /*
    /**********************************************************
    /* Public API: constructing ObjectWriters
    /* for more advanced configuration
    /**********************************************************
     */

    /**
     * Convenience method for constructing {@link ObjectWriter}
     * with default settings.
     */
    public ObjectWriter writer() {
        return _newWriter(getSerializationConfig());
    }

    /**
     * Factory method for constructing {@link ObjectWriter} with
     * specified feature enabled (compared to settings that this
     * mapper instance has).
     */
    public ObjectWriter writer(SerializationFeature feature) {
        return _newWriter(getSerializationConfig().with(feature));
    }

    /**
     * Factory method for constructing {@link ObjectWriter} with
     * specified features enabled (compared to settings that this
     * mapper instance has).
     */
    public ObjectWriter writer(SerializationFeature first,
            SerializationFeature... other) {
        return _newWriter(getSerializationConfig().with(first, other));
    }
    
    /**
     * Factory method for constructing {@link ObjectWriter} that will
     * serialize objects using specified {@link DateFormat}; or, if
     * null passed, using timestamp (64-bit number.
     */
    public ObjectWriter writer(DateFormat df) {
        return _newWriter(getSerializationConfig().with(df));
    }
    
    /**
     * Factory method for constructing {@link ObjectWriter} that will
     * serialize objects using specified JSON View (filter).
     */
    public ObjectWriter writerWithView(Class<?> serializationView) {
        return _newWriter(getSerializationConfig().withView(serializationView));
    }
    
    /**
     * Factory method for constructing {@link ObjectWriter} that will
     * serialize objects using specified root type, instead of actual
     * runtime type of value. Type must be a super-type of runtime type.
     *<p>
     * Main reason for using this method is performance, as writer is able
     * to pre-fetch serializer to use before write, and if writer is used
     * more than once this avoids addition per-value serializer lookups.
     */
    public ObjectWriter writerFor(Class<?> rootType) {
        return _newWriter(getSerializationConfig(),
                ((rootType == null) ? null :_typeFactory.constructType(rootType)),
                /*PrettyPrinter*/null);
    }

    /**
     * Factory method for constructing {@link ObjectWriter} that will
     * serialize objects using specified root type, instead of actual
     * runtime type of value. Type must be a super-type of runtime type.
     *<p>
     * Main reason for using this method is performance, as writer is able
     * to pre-fetch serializer to use before write, and if writer is used
     * more than once this avoids addition per-value serializer lookups.
     */
    public ObjectWriter writerFor(TypeReference<?> rootType) {
        return _newWriter(getSerializationConfig(),
                ((rootType == null) ? null : _typeFactory.constructType(rootType)),
                /*PrettyPrinter*/null);
    }

    /**
     * Factory method for constructing {@link ObjectWriter} that will
     * serialize objects using specified root type, instead of actual
     * runtime type of value. Type must be a super-type of runtime type.
     *<p>
     * Main reason for using this method is performance, as writer is able
     * to pre-fetch serializer to use before write, and if writer is used
     * more than once this avoids addition per-value serializer lookups.
     */
    public ObjectWriter writerFor(JavaType rootType) {
        return _newWriter(getSerializationConfig(), rootType, /*PrettyPrinter*/null);
    }

    /**
     * Factory method for constructing {@link ObjectWriter} that will
     * serialize objects using the default pretty printer for indentation
     */
    public ObjectWriter writerWithDefaultPrettyPrinter() {
        SerializationConfig config = getSerializationConfig();
        return _newWriter(config,
                /*root type*/ null, config.getDefaultPrettyPrinter());
    }
    
    /**
     * Factory method for constructing {@link ObjectWriter} that will
     * serialize objects using specified filter provider.
     */
    public ObjectWriter writer(FilterProvider filterProvider) {
        return _newWriter(getSerializationConfig().withFilters(filterProvider));
    }
    
    /**
     * Factory method for constructing {@link ObjectWriter} that will
     * pass specific schema object to {@link JsonGenerator} used for
     * writing content.
     * 
     * @param schema Schema to pass to generator
     */
    public ObjectWriter writer(FormatSchema schema) {
        _verifySchemaType(schema);
        return _newWriter(getSerializationConfig(), schema);
    }

    /**
     * Factory method for constructing {@link ObjectWriter} that will
     * use specified Base64 encoding variant for Base64-encoded binary data.
     */
    public ObjectWriter writer(Base64Variant defaultBase64) {
        return _newWriter(getSerializationConfig().with(defaultBase64));
    }

    /**
     * Factory method for constructing {@link ObjectReader} that will
     * use specified character escaping details for output.
     */
    public ObjectWriter writer(CharacterEscapes escapes) {
        return _newWriter(getSerializationConfig()).with(escapes);
    }

    /**
     * Factory method for constructing {@link ObjectWriter} that will
     * use specified default attributes.
     */
    public ObjectWriter writer(ContextAttributes attrs) {
        return _newWriter(getSerializationConfig().with(attrs));
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
        return _newReader(getDeserializationConfig()).with(_injectableValues);
    }

    /**
     * Factory method for constructing {@link ObjectReader} with
     * specified feature enabled (compared to settings that this
     * mapper instance has).
     * Note that the resulting instance is NOT usable as is,
     * without defining expected value type.
     */
    public ObjectReader reader(DeserializationFeature feature) {
        return _newReader(getDeserializationConfig().with(feature));
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
        return _newReader(getDeserializationConfig().with(first, other));
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
    public ObjectReader readerForUpdating(Object valueToUpdate) {
        JavaType t = _typeFactory.constructType(valueToUpdate.getClass());
        return _newReader(getDeserializationConfig(), t, valueToUpdate,
                null, _injectableValues);
    }

    /**
     * Factory method for constructing {@link ObjectReader} that will
     * read or update instances of specified type
     */
    public ObjectReader readerFor(JavaType type) {
        return _newReader(getDeserializationConfig(), type, null,
                null, _injectableValues);
    }

    /**
     * Factory method for constructing {@link ObjectReader} that will
     * read or update instances of specified type
     */
    public ObjectReader readerFor(Class<?> type) {
        return _newReader(getDeserializationConfig(), _typeFactory.constructType(type), null,
                null, _injectableValues);
    }

    /**
     * Factory method for constructing {@link ObjectReader} that will
     * read or update instances of specified type
     */
    public ObjectReader readerFor(TypeReference<?> type) {
        return _newReader(getDeserializationConfig(), _typeFactory.constructType(type), null,
                null, _injectableValues);
    }

    /**
     * Factory method for constructing {@link ObjectReader} that will
     * use specified {@link JsonNodeFactory} for constructing JSON trees.
     */
    public ObjectReader reader(JsonNodeFactory f) {
        return _newReader(getDeserializationConfig()).with(f);
    }

    /**
     * Factory method for constructing {@link ObjectReader} that will
     * pass specific schema object to {@link JsonParser} used for
     * reading content.
     * 
     * @param schema Schema to pass to parser
     */
    public ObjectReader reader(FormatSchema schema) {
        _verifySchemaType(schema);
        return _newReader(getDeserializationConfig(), null, null,
                schema, _injectableValues);
    }

    /**
     * Factory method for constructing {@link ObjectReader} that will
     * use specified injectable values.
     * 
     * @param injectableValues Injectable values to use
     */
    public ObjectReader reader(InjectableValues injectableValues) {
        return _newReader(getDeserializationConfig(), null, null,
                null, injectableValues);
    }

    /**
     * Factory method for constructing {@link ObjectReader} that will
     * deserialize objects using specified JSON View (filter).
     */
    public ObjectReader readerWithView(Class<?> view) {
        return _newReader(getDeserializationConfig().withView(view));
    }

    /**
     * Factory method for constructing {@link ObjectReader} that will
     * use specified Base64 encoding variant for Base64-encoded binary data.
     */
    public ObjectReader reader(Base64Variant defaultBase64) {
        return _newReader(getDeserializationConfig().with(defaultBase64));
    }

    /**
     * Factory method for constructing {@link ObjectReader} that will
     * use specified default attributes.
     */
    public ObjectReader reader(ContextAttributes attrs) {
        return _newReader(getDeserializationConfig().with(attrs));
    }

    /*
    /**********************************************************
    /* Extended Public API: convenience type conversion
    /**********************************************************
     */

    /**
     * Convenience method for doing two-step conversion from given value, into
     * instance of given value type, if (but only if!) conversion is needed.
     * If given value is already of requested type, value is returned as is.
     *<p>
     * This method is functionally similar to first
     * serializing given value into JSON, and then binding JSON data into value
     * of given type, but should be more efficient since full serialization does
     * not (need to) occur.
     * However, same converters (serializers, deserializers) will be used as for
     * data binding, meaning same object mapper configuration works.
     *<p>
     * Note that it is possible that in some cases behavior does differ from
     * full serialize-then-deserialize cycle: in most case differences are
     * unintentional (that is, flaws to fix) and should be reported.
     * It is not guaranteed, however, that the behavior is 100% the same:
     * the goal is just to allow efficient value conversions for structurally
     * compatible Objects, according to standard Jackson configuration.
     *<p>
     * Further note that this functionality is not designed to support "advanced" use
     * cases, such as conversion of polymorphic values, or cases where Object Identity
     * is used.
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

    /**
     * See {@link #convertValue(Object, Class)}
     */
    @SuppressWarnings("unchecked")
    public <T> T convertValue(Object fromValue, TypeReference<T> toValueTypeRef)
        throws IllegalArgumentException
    {
        return (T) _convert(fromValue, _typeFactory.constructType(toValueTypeRef));
    } 

    /**
     * See {@link #convertValue(Object, Class)}
     */
    @SuppressWarnings("unchecked")
    public <T> T convertValue(Object fromValue, JavaType toValueType)
        throws IllegalArgumentException
    {
        return (T) _convert(fromValue, toValueType);
    } 

    /**
     * Actual conversion implementation: instead of using existing read
     * and write methods, much of code is inlined. Reason for this is
     * that we must avoid root value wrapping/unwrapping both for efficiency and
     * for correctness. If root value wrapping/unwrapping is actually desired,
     * caller must use explicit <code>writeValue</code> and
     * <code>readValue</code> methods.
     */
    @SuppressWarnings("resource")
    protected Object _convert(Object fromValue, JavaType toValueType)
        throws IllegalArgumentException
    {
        // [databind#1433] Do not shortcut null values.
        // This defaults primitives and fires deserializer getNullValue hooks.
        if (fromValue != null) {
            // also, as per [databind#11], consider case for simple cast
            // But with caveats: one is that while everything is Object.class, we don't
            // want to "optimize" that out; and the other is that we also do not want
            // to lose conversions of generic types.
            Class<?> targetType = toValueType.getRawClass();
            if (targetType != Object.class
                    && !toValueType.hasGenericTypes()
                    && targetType.isAssignableFrom(fromValue.getClass())) {
                return fromValue;
            }
        }

        // inlined 'writeValue' with minor changes:
        // first: disable wrapping when writing
        SerializationConfig config = getSerializationConfig()
                .without(SerializationFeature.WRAP_ROOT_VALUE);
        DefaultSerializerProvider prov = _serializerProvider(config);
        TokenBuffer buf = TokenBuffer.forValueConversion(prov);
        // Would like to let buffer decide, but it won't have deser config to check so...
        if (isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)) {
            buf = buf.forceUseOfBigDecimal(true);
        }
        try {
            // no need to check for closing of TokenBuffer
            prov.serializeValue(buf, fromValue);

            // then matching read, inlined 'readValue' with minor mods:
            DefaultDeserializationContext readCtxt = createDeserializationContext();
            final JsonParser p = buf.asParser(readCtxt);
            readCtxt.assignParser(p);
            Object result;
            // ok to pass in existing feature flags; unwrapping handled by mapper
            JsonToken t = _initForReading(p, toValueType);
            if (t == JsonToken.VALUE_NULL) {
                result = _findRootDeserializer(readCtxt, toValueType).getNullValue(readCtxt);
            } else if (t == JsonToken.END_ARRAY || t == JsonToken.END_OBJECT) {
                result = null;
            } else { // pointing to event other than null
                JsonDeserializer<Object> deser = _findRootDeserializer(readCtxt, toValueType);
                // note: no handling of unwrapping
                result = deser.deserialize(p, readCtxt);
            }
            p.close();
            return result;
        } catch (IOException e) { // should not occur, no real i/o...
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /**
     * Convenience method similar to {@link #convertValue(Object, JavaType)} but one
     * in which 
     *<p>
     * Implementation is approximately as follows:
     *<ol>
     * <li>Serialize `updateWithValue` into {@link TokenBuffer}</li>
     * <li>Construct {@link ObjectReader} with `valueToUpdate` (using {@link #readerForUpdating(Object)})
     *   </li>
     * <li>Construct {@link JsonParser} (using {@link TokenBuffer#asParser()})
     *   </li>
     * <li>Update using {@link ObjectReader#readValue(JsonParser)}.
     *   </li>
     * <li>Return `valueToUpdate`
     *   </li>
     *</ol>
     *<p>
     * Note that update is "shallow" in that only first level of properties (or, immediate contents
     * of container to update) are modified, unless properties themselves indicate that
     * merging should be applied for contents. Such merging can be specified using
     * annotations (see <code>JsonMerge</code>) as well as using "config overrides" (see
     * {@link #configOverride(Class)} and {@link #setDefaultMergeable(Boolean)}).
     *
     * @param valueToUpdate Object to update
     * @param overrides Object to conceptually serialize and merge into value to
     *     update; can be thought of as a provider for overrides to apply.
     * 
     * @return Either the first argument (`valueToUpdate`), if it is mutable; or a result of
     *     creating new instance that is result of "merging" values (for example, "updating" a
     *     Java array will create a new array)
     *
     * @throws JsonMappingException if there are structural incompatibilities that prevent update.
     */
    @SuppressWarnings("resource")
    public <T> T updateValue(T valueToUpdate, Object overrides)
        throws JsonMappingException
    {
        if ((valueToUpdate == null) || (overrides == null)) {
            return valueToUpdate;
        }
        SerializationConfig config = getSerializationConfig()
                .without(SerializationFeature.WRAP_ROOT_VALUE);
        DefaultSerializerProvider prov = _serializerProvider(config);
        TokenBuffer buf = TokenBuffer.forValueConversion(prov);
        // Would like to let buffer decide, but it won't have deser config to check so...
        if (isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)) {
            buf = buf.forceUseOfBigDecimal(true);
        }
        T result;
        try {
            prov.serializeValue(buf, overrides);
            JsonParser p = buf.asParser();
            result = readerForUpdating(valueToUpdate).readValue(p);
            p.close();
        } catch (IOException e) { // should not occur, no real i/o...
            if (e instanceof JsonMappingException) {
                throw (JsonMappingException) e;
            }
            // 17-Mar-2017, tatu: Really ought not happen...
            throw JsonMappingException.fromUnexpectedIOE(e);
        }
        return result;
    }

    /*
    /**********************************************************
    /* Extended Public API: JSON Schema generation
    /**********************************************************
     */

    /**
     * Method for visiting type hierarchy for given type, using specified visitor.
     *<p>
     * This method can be used for things like
     * generating <a href="http://json-schema.org/">JSON Schema</a>
     * instance for specified type.
     *
     * @param type Type to generate schema for (possibly with generic signature)
     */
    public void acceptJsonFormatVisitor(Class<?> type, JsonFormatVisitorWrapper visitor)
        throws JsonMappingException
    {
        acceptJsonFormatVisitor(_typeFactory.constructType(type), visitor);
    }

    /**
     * Method for visiting type hierarchy for given type, using specified visitor.
     * Visitation uses <code>Serializer</code> hierarchy and related properties
     *<p>
     * This method can be used for things like
     * generating <a href="http://json-schema.org/">JSON Schema</a>
     * instance for specified type.
     *
     * @param type Type to generate schema for (possibly with generic signature)
     */
    public void acceptJsonFormatVisitor(JavaType type, JsonFormatVisitorWrapper visitor)
        throws JsonMappingException
    {
        if (type == null) {
            throw new IllegalArgumentException("type must be provided");
        }
        _serializerProvider().acceptJsonFormatVisitor(type, visitor);
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
        // 03-Oct-2017, tatu: Should be ok to pass "empty" generator settings...
        return _serializerProvider.createInstance(config,
                GeneratorSettings.empty(), _serializerFactory);
    }

    protected DefaultSerializerProvider _serializerProvider() {
        // 03-Oct-2017, tatu: Should be ok to pass "empty" generator settings...
        return _serializerProvider.createInstance(getSerializationConfig(),
                GeneratorSettings.empty(), _serializerFactory);
    }

    /*
    /**********************************************************
    /* Internal methods for deserialization, overridable
    /**********************************************************
     */

    /**
     * Actual implementation of value reading+binding operation.
     */
    protected Object _readValue(DeserializationContext ctxt, JsonParser p,
            JavaType valueType)
        throws IOException
    {
        /* First: may need to read the next token, to initialize
         * state (either before first read from parser, or after
         * previous token has been cleared)
         */
        Object result;
        JsonToken t = _initForReading(p, valueType);
        final DeserializationConfig config = ctxt.getConfig();
        if (t == JsonToken.VALUE_NULL) {
            // Ask JsonDeserializer what 'null value' to use:
            result = _findRootDeserializer(ctxt, valueType).getNullValue(ctxt);
        } else if (t == JsonToken.END_ARRAY || t == JsonToken.END_OBJECT) {
            result = null;
        } else { // pointing to event other than null
            JsonDeserializer<Object> deser = _findRootDeserializer(ctxt, valueType);
            // ok, let's get the value
            if (config.useRootWrapping()) {
                result = _unwrapAndDeserialize(p, ctxt, config, valueType, deser);
            } else {
                result = deser.deserialize(p, ctxt);
            }
        }
        // Need to consume the token too
        p.clearCurrentToken();
        if (config.isEnabled(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)) {
            _verifyNoTrailingTokens(p, ctxt, valueType);
        }
        return result;
    }

    protected Object _readMapAndClose(DefaultDeserializationContext ctxt,
            JsonParser p0, JavaType valueType)
        throws IOException
    {
        ctxt.assignParser(p0);
        try (JsonParser p = p0) {
            Object result;
            JsonToken t = _initForReading(p, valueType);
            if (t == JsonToken.VALUE_NULL) {
                // Ask JsonDeserializer what 'null value' to use:
                result = _findRootDeserializer(ctxt, valueType).getNullValue(ctxt);
            } else if (t == JsonToken.END_ARRAY || t == JsonToken.END_OBJECT) {
                result = null;
            } else {
                final DeserializationConfig config = ctxt.getConfig();
                JsonDeserializer<Object> deser = _findRootDeserializer(ctxt, valueType);
                if (config.useRootWrapping()) {
                    result = _unwrapAndDeserialize(p, ctxt, config, valueType, deser);
                } else {
                    result = deser.deserialize(p, ctxt);
                }
                ctxt.checkUnresolvedObjectId();
            }
            if (ctxt.isEnabled(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)) {
                _verifyNoTrailingTokens(p, ctxt, valueType);
            }
            return result;
        }
    }

    /**
     * Similar to {@link #_readMapAndClose} but specialized for <code>JsonNode</code> reading.
     */
    protected JsonNode _readTreeAndClose(DeserializationContext ctxt,
            JsonParser p0) throws IOException
    {
        try (JsonParser p = p0) {
            final JavaType valueType = JSON_NODE_TYPE;

            DeserializationConfig cfg = getDeserializationConfig();

            // 27-Oct-2016, tatu: Need to inline `_initForReading()` due to
            //   special requirements by tree reading (no fail on eof)
            JsonToken t = p.currentToken();
            if (t == null) {
                t = p.nextToken();
                if (t == null) { // [databind#1406]: expose end-of-input as `null`
                    return null;
                }
            }
            if (t == JsonToken.VALUE_NULL) {
                return cfg.getNodeFactory().nullNode();
            }
            JsonDeserializer<Object> deser = _findRootDeserializer(ctxt, valueType);
            Object result;
            if (cfg.useRootWrapping()) {
                result = _unwrapAndDeserialize(p, ctxt, cfg, valueType, deser);
            } else {
                result = deser.deserialize(p, ctxt);
                if (cfg.isEnabled(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)) {
                    _verifyNoTrailingTokens(p, ctxt, valueType);
                }
            }
            // No ObjectIds so can ignore
//            ctxt.checkUnresolvedObjectId();
            return (JsonNode) result;
        }
    }

    protected Object _unwrapAndDeserialize(JsonParser p, DeserializationContext ctxt, 
            DeserializationConfig config,
            JavaType rootType, JsonDeserializer<Object> deser)
        throws IOException
    {
        PropertyName expRootName = config.findRootName(rootType);
        // 12-Jun-2015, tatu: Should try to support namespaces etc but...
        String expSimpleName = expRootName.getSimpleName();
        if (p.currentToken() != JsonToken.START_OBJECT) {
            ctxt.reportWrongTokenException(rootType, JsonToken.START_OBJECT,
                    "Current token not START_OBJECT (needed to unwrap root name '%s'), but %s",
                    expSimpleName, p.currentToken());
            
        }
        if (p.nextToken() != JsonToken.FIELD_NAME) {
            ctxt.reportWrongTokenException(rootType, JsonToken.FIELD_NAME,
                    "Current token not FIELD_NAME (to contain expected root name '%s'), but %s",
                    expSimpleName, p.currentToken());
        }
        String actualName = p.currentName();
        if (!expSimpleName.equals(actualName)) {
            ctxt.reportInputMismatch(rootType,
                    "Root name '%s' does not match expected ('%s') for type %s",
                    actualName, expSimpleName, rootType);
        }
        // ok, then move to value itself....
        p.nextToken();
        Object result = deser.deserialize(p, ctxt);
        // and last, verify that we now get matching END_OBJECT
        if (p.nextToken() != JsonToken.END_OBJECT) {
            ctxt.reportWrongTokenException(rootType, JsonToken.END_OBJECT,
                    "Current token not END_OBJECT (to match wrapper object with root name '%s'), but %s",
                    expSimpleName, p.currentToken());
        }
        if (config.isEnabled(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)) {
            _verifyNoTrailingTokens(p, ctxt, rootType);
        }
        return result;
    }

    /**
     * Internal helper method called to create an instance of {@link DeserializationContext}
     * for deserializing a single root value.
     * Can be overridden if a custom context is needed.
     */
    protected DefaultDeserializationContext createDeserializationContext(JsonParser p) {
        return _deserializationContext.createInstance(getDeserializationConfig(),
                /* FormatSchema */ null, _injectableValues)
                .assignParser(p);
    }

    protected DefaultDeserializationContext createDeserializationContext() {
        return _deserializationContext.createInstance(getDeserializationConfig(),
                /* FormatSchema */ null, _injectableValues);
    }

    protected DefaultDeserializationContext createDeserializationContext(DeserializationConfig config,
            JsonParser p) {
        return _deserializationContext.createInstance(config,
                /* FormatSchema */ null, _injectableValues)
                .assignParser(p);
    }
    
    /**
     * Method called to ensure that given parser is ready for reading
     * content for data binding.
     *
     * @return First token to be used for data binding after this call:
     *  can never be null as exception will be thrown if parser cannot
     *  provide more tokens.
     *
     * @throws IOException if the underlying input source has problems during
     *   parsing
     * @throws JsonParseException if parser has problems parsing content
     * @throws JsonMappingException if the parser does not have any more
     *   content to map (note: Json "null" value is considered content;
     *   enf-of-stream not)
     */
    protected JsonToken _initForReading(JsonParser p, JavaType targetType) throws IOException
    {
        // First: must point to a token; if not pointing to one, advance.
        // This occurs before first read from JsonParser, as well as
        // after clearing of current token.
        JsonToken t = p.currentToken();
        if (t == null) {
            // and then we must get something...
            t = p.nextToken();
            if (t == null) {
                // Throw mapping exception, since it's failure to map,
                //   not an actual parsing problem
                throw MismatchedInputException.from(p, targetType,
                        "No content to map due to end-of-input");
            }
        }
        return t;
    }

    protected final void _verifyNoTrailingTokens(JsonParser p, DeserializationContext ctxt,
            JavaType bindType)
        throws IOException
    {
        JsonToken t = p.nextToken();
        if (t != null) {
            Class<?> bt = ClassUtil.rawClass(bindType);
            ctxt.reportTrailingTokens(bt, p, t);
        }
    }

    /*
    /**********************************************************
    /* Internal factory methods for ObjectReaders/-Writers
    /**********************************************************
     */
    
    /**
     * Factory method sub-classes must override, to produce {@link ObjectReader}
     * instances of proper sub-type
     */
    protected ObjectReader _newReader(DeserializationConfig config) {
        return new ObjectReader(this, config);
    }

    /**
     * Factory method sub-classes must override, to produce {@link ObjectReader}
     * instances of proper sub-type
     */
    protected ObjectReader _newReader(DeserializationConfig config,
            JavaType valueType, Object valueToUpdate,
            FormatSchema schema, InjectableValues injectableValues) {
        return new ObjectReader(this, config, valueType, valueToUpdate, schema, injectableValues);
    }

    /**
     * Factory method sub-classes must override, to produce {@link ObjectWriter}
     * instances of proper sub-type
     */
    protected ObjectWriter _newWriter(SerializationConfig config) {
        return new ObjectWriter(this, config);
    }

    /**
     * Factory method sub-classes must override, to produce {@link ObjectWriter}
     * instances of proper sub-type
     */
    protected ObjectWriter _newWriter(SerializationConfig config, FormatSchema schema) {
        return new ObjectWriter(this, config, schema);
    }
    
    /**
     * Factory method sub-classes must override, to produce {@link ObjectWriter}
     * instances of proper sub-type
     */
    protected ObjectWriter _newWriter(SerializationConfig config,
            JavaType rootType, PrettyPrinter pp) {
        return new ObjectWriter(this, config, rootType, pp);
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
            return ctxt.reportBadDefinition(valueType,
                    "Cannot find a deserializer for type "+valueType);
        }
        _rootDeserializers.put(valueType, deser);
        return deser;
    }

    protected void _verifySchemaType(FormatSchema schema)
    {
        if (schema != null) {
            if (!_jsonFactory.canUseSchema(schema)) {
                    throw new IllegalArgumentException("Cannot use FormatSchema of type "+schema.getClass().getName()
                            +" for format "+_jsonFactory.getFormatName());
            }
        }
    }
}
