package com.fasterxml.jackson.databind.cfg;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.DateFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Snapshottable;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.*;
import com.fasterxml.jackson.databind.introspect.BasicClassIntrospector;
import com.fasterxml.jackson.databind.introspect.ClassIntrospector;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.introspect.MixInResolver;
import com.fasterxml.jackson.databind.introspect.MixInHandler;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.SubtypeResolver;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.jsontype.TypeResolverProvider;
import com.fasterxml.jackson.databind.jsontype.impl.DefaultTypeResolverBuilder;
import com.fasterxml.jackson.databind.jsontype.impl.StdSubtypeResolver;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.ser.*;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.type.TypeModifier;
import com.fasterxml.jackson.databind.util.ArrayBuilders;
import com.fasterxml.jackson.databind.util.LinkedNode;
import com.fasterxml.jackson.databind.util.RootNameLookup;
import com.fasterxml.jackson.databind.util.StdDateFormat;

/**
 * Since {@link ObjectMapper} instances are immutable in  Jackson 3.x for full thread-safety,
 * we need means to construct configured instances. This is the shared base API for
 * builders for all types of mappers.
 *
 * @since 3.0
 */
public abstract class MapperBuilder<M extends ObjectMapper,
    B extends MapperBuilder<M,B>>
{
    protected final static int DEFAULT_MAPPER_FEATURES = MapperConfig.collectFeatureDefaults(MapperFeature.class);
    protected final static int DEFAULT_SER_FEATURES = MapperConfig.collectFeatureDefaults(SerializationFeature.class);
    protected final static int DEFAULT_DESER_FEATURES = MapperConfig.collectFeatureDefaults(DeserializationFeature.class);

    protected final static PrettyPrinter DEFAULT_PRETTY_PRINTER = new DefaultPrettyPrinter();

    // 16-May-2009, tatu: Ditto ^^^
    protected final static AnnotationIntrospector DEFAULT_ANNOTATION_INTROSPECTOR = new JacksonAnnotationIntrospector();
    
    protected final static BaseSettings DEFAULT_BASE_SETTINGS = new BaseSettings(
            DEFAULT_ANNOTATION_INTROSPECTOR,
             null,
            null, // no default typing, by default
            StdDateFormat.instance, null,
            Locale.getDefault(),
            null, // to indicate "use Jackson default TimeZone" (UTC since Jackson 2.7)
            Base64Variants.getDefaultVariant(),
            JsonNodeFactory.instance
    );

    protected final static TypeResolverProvider DEFAULT_TYPE_RESOLVER_PROVIDER = new TypeResolverProvider();

    protected final static AbstractTypeResolver[] NO_ABSTRACT_TYPE_RESOLVERS = new AbstractTypeResolver[0];

    /*
    /**********************************************************************
    /* Basic settings
    /**********************************************************************
     */

    protected BaseSettings _baseSettings;

    /**
     * Underlying stream factory
     */
    protected TokenStreamFactory _streamFactory;

    /**
     * Various configuration setting overrides, both global base settings
     * and per-class overrides.
     */
    protected final ConfigOverrides _configOverrides;

    /*
    /**********************************************************************
    /* Modules
    /**********************************************************************
     */

    /**
     * Modules registered for addition, indexed by registration id.
     */
    protected Map<Object, com.fasterxml.jackson.databind.Module> _modules;

    /*
    /**********************************************************************
    /* Handlers, introspection
    /**********************************************************************
     */

    /**
     * Specific factory used for creating {@link JavaType} instances;
     * needed to allow modules to add more custom type handling
     * (mostly to support types of non-Java JVM languages)
     */
    protected TypeFactory _typeFactory;

    /**
     * Introspector used to figure out Bean properties needed for bean serialization
     * and deserialization. Overridable so that it is possible to change low-level
     * details of introspection, like adding new annotation types.
     */
    protected ClassIntrospector _classIntrospector;

    /**
     * Entity responsible for construction actual type resolvers
     * ({@link com.fasterxml.jackson.databind.jsontype.TypeSerializer}s,
     * {@link com.fasterxml.jackson.databind.jsontype.TypeDeserializer}s).
     */
    protected TypeResolverProvider _typeResolverProvider;

    protected SubtypeResolver _subtypeResolver;

    /**
     * Handler responsible for resolving mix-in classes registered, if any.
     */
    protected MixInHandler _mixInHandler;

    /*
    /**********************************************************************
    /* Factories for serialization
    /**********************************************************************
     */

    protected SerializerFactory _serializerFactory;

    /**
     * Prototype {@link SerializerProvider} to use for creating per-operation providers.
     */
    protected DefaultSerializerProvider _serializerProvider;

    protected FilterProvider _filterProvider;

    protected PrettyPrinter _defaultPrettyPrinter;

    /*
    /**********************************************************************
    /* Factories etc for deserialization
    /**********************************************************************
     */

    protected DeserializerFactory _deserializerFactory;

    /**
     * Prototype (about same as factory) to use for creating per-operation contexts.
     */
    protected DefaultDeserializationContext _deserializationContext;

    /**
     * Provider for values to inject in deserialized POJOs.
     */
    protected InjectableValues _injectableValues;

    /**
     * Optional handlers that application may register to try to work-around
     * various problem situations during deserialization
     */
    protected LinkedNode<DeserializationProblemHandler> _problemHandlers;

    protected AbstractTypeResolver[] _abstractTypeResolvers;

    /*
    /**********************************************************************
    /* Feature flags: ser, deser
    /**********************************************************************
     */

    /**
     * Set of shared mapper features enabled.
     */
    protected int _mapperFeatures;

    /**
     * Set of {@link SerializationFeature}s enabled.
     */
    protected int _serFeatures;

    /**
     * Set of {@link DeserializationFeature}s enabled.
     */
    protected int _deserFeatures;

    /*
    /**********************************************************************
    /* Feature flags: generation, parsing
    /**********************************************************************
     */

    /**
     * States of {@link com.fasterxml.jackson.core.JsonParser.Feature}s to enable/disable.
     */
    protected int _parserFeatures;

    /**
     * States of {@link com.fasterxml.jackson.core.JsonGenerator.Feature}s to enable/disable.
     */
    protected int _generatorFeatures;

    /**
     * Optional per-format parser feature flags.
     */
    protected int _formatParserFeatures;

    /**
     * Optional per-format generator feature flags.
     */
    protected int _formatGeneratorFeatures;

    /*
    /**********************************************************************
    /* Transient state
    /**********************************************************************
     */

    /**
     * Configuration state after direct access, immediately before registration
     * of modules (if any) and construction of actual mapper. Retained after
     * first access, and returned from {@link #saveStateApplyModules()}, to
     * allow future "rebuild".
     */
    protected transient MapperBuilderState _savedState;
    
    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    protected MapperBuilder(TokenStreamFactory streamFactory)
    {
        _streamFactory = streamFactory;
        _baseSettings = DEFAULT_BASE_SETTINGS;
        _configOverrides = new ConfigOverrides();
        _modules = null;

        _parserFeatures = streamFactory.getParserFeatures();
        _generatorFeatures = streamFactory.getGeneratorFeatures();
        _formatParserFeatures = streamFactory.getFormatParserFeatures();
        _formatGeneratorFeatures = streamFactory.getFormatGeneratorFeatures();

        _mapperFeatures = DEFAULT_MAPPER_FEATURES;
        // Some overrides we may need based on format
        if (streamFactory.requiresPropertyOrdering()) {
            _mapperFeatures |= MapperFeature.SORT_PROPERTIES_ALPHABETICALLY.getMask();
        }
        _deserFeatures = DEFAULT_DESER_FEATURES;
        _serFeatures = DEFAULT_SER_FEATURES;

        _typeFactory = null;
        _classIntrospector = null;
        _typeResolverProvider = null;
        _subtypeResolver = null;
        _mixInHandler = null;

        _serializerFactory = null;
        _serializerProvider = null;
        _filterProvider = null;

        _deserializerFactory = null;
        _deserializationContext = null;
        _injectableValues = null;

        _problemHandlers = null;
        _abstractTypeResolvers = NO_ABSTRACT_TYPE_RESOLVERS;
    }

    /**
     * Constructor used to support "rebuild", starting with a previously taken
     * snapshot, in order to create mappers that start with a known state of
     * configuration, including a set of modules to register.
     */
    protected MapperBuilder(MapperBuilderState state)
    {
        _streamFactory = state._streamFactory;
        _baseSettings = state._baseSettings;
        _configOverrides = Snapshottable.takeSnapshot(state._configOverrides);

        _parserFeatures = state._parserFeatures;
        _generatorFeatures = state._generatorFeatures;
        _formatParserFeatures = state._formatParserFeatures;
        _formatGeneratorFeatures = state._formatGeneratorFeatures;
        _mapperFeatures = state._mapperFeatures;
        _deserFeatures = state._deserFeatures;
        _serFeatures = state._serFeatures;

        // Handlers, introspection
        _typeFactory = state._typeFactory;
        _classIntrospector = Snapshottable.takeSnapshot(state._classIntrospector);
        _typeResolverProvider = state._typeResolverProvider;
        _subtypeResolver = Snapshottable.takeSnapshot(state._subtypeResolver);
        _mixInHandler = (MixInHandler) Snapshottable.takeSnapshot(state._mixInHandler);

        // Factories for serialization
        _serializerFactory = state._serializerFactory;
        _serializerProvider = state._serializerProvider;
        _filterProvider = state._filterProvider;
        _defaultPrettyPrinter = state._defaultPrettyPrinter;

        // Factories for deserialization
        _deserializerFactory = state._deserializerFactory;
        _deserializationContext = state._deserializationContext;
        _injectableValues = Snapshottable.takeSnapshot(state._injectableValues);
        _problemHandlers = state._problemHandlers;
        _abstractTypeResolvers = state._abstractTypeResolvers;

        // Modules
        if (state._modules == null) {
            _modules = null;
        } else {
            _modules = new LinkedHashMap<>();
            for (Object mod : state._modules) {
                addModule((com.fasterxml.jackson.databind.Module) mod);
            }
        }
    }

    protected MapperBuilder(MapperBuilder<?,?> base)
    {
        _streamFactory = base._streamFactory;
        _baseSettings = base._baseSettings;
        _configOverrides = base._configOverrides;

        _mapperFeatures = base._mapperFeatures;
        _serFeatures = base._serFeatures;
        _deserFeatures = base._deserFeatures;

        _parserFeatures = base._parserFeatures;
        _generatorFeatures = base._deserFeatures;
        _formatParserFeatures = base._formatParserFeatures;
        _formatGeneratorFeatures = base._formatGeneratorFeatures;

        _typeFactory = base._typeFactory;
        _classIntrospector = base._classIntrospector;
        _typeResolverProvider = base._typeResolverProvider;
        _subtypeResolver = base._subtypeResolver;
        _mixInHandler = base._mixInHandler;

        _serializerFactory = base._serializerFactory;
        _serializerProvider = base._serializerProvider;
        _filterProvider = base._filterProvider;

        _deserializerFactory = base._deserializerFactory;
        _deserializationContext = base._deserializationContext;
        _injectableValues = base._injectableValues;

        _problemHandlers = base._problemHandlers;
    }

    /*
    /**********************************************************************
    /* Methods for actual build process
    /**********************************************************************
     */

    /**
     * Method to call to create actual mapper instance, usually passing {@code this}
     * builder to its constructor.
     */
    public abstract M build();

    /**
     * Method called by mapper being constructed to first save state (delegated to
     * {@link #_saveState()}), then apply modules (if any), and then return
     * the saved state (but retain reference to it). If method has been called previously,
     * it will simply return retained state.
     */
    public MapperBuilderState saveStateApplyModules()
    {
        if (_savedState == null) {
            _savedState = _saveState();
            if (_modules != null) {
                ModuleContextBase ctxt = _constructModuleContext();
                _modules.values().forEach(m -> m.setupModule(ctxt));
                // and since context may buffer some changes, ensure those are flushed:
                ctxt.applyChanges(this);
            }
        }
        return _savedState;
    }

    protected ModuleContextBase _constructModuleContext() {
        return new ModuleContextBase(this, _configOverrides);
    }

    protected abstract MapperBuilderState _saveState();

    /*
    /**********************************************************************
    /* Secondary factory methods
    /**********************************************************************
     */
    
    public SerializationConfig buildSerializationConfig(MixInHandler mixins,
            RootNameLookup rootNames)
    {
        return new SerializationConfig(this,
                _mapperFeatures, _serFeatures, _generatorFeatures, _formatGeneratorFeatures,
                mixins, rootNames, _configOverrides);
    }

    public DeserializationConfig buildDeserializationConfig(MixInHandler mixins,
            RootNameLookup rootNames)
    {
        return new DeserializationConfig(this,
                _mapperFeatures, _deserFeatures, _parserFeatures, _formatParserFeatures,
                mixins, rootNames, _configOverrides,
                _abstractTypeResolvers);
    }

    /*
    /**********************************************************************
    /* Accessors, features
    /**********************************************************************
     */

    public boolean isEnabled(MapperFeature f) {
        return f.enabledIn(_mapperFeatures);
    }
    public boolean isEnabled(DeserializationFeature f) {
        return f.enabledIn(_deserFeatures);
    }
    public boolean isEnabled(SerializationFeature f) {
        return f.enabledIn(_serFeatures);
    }

    public boolean isEnabled(JsonParser.Feature f) {
        return f.enabledIn(_parserFeatures);
    }
    public boolean isEnabled(JsonGenerator.Feature f) {
        return f.enabledIn(_generatorFeatures);
    }

    /*
    /**********************************************************************
    /* Accessors, base settings
    /**********************************************************************
     */

    public BaseSettings baseSettings() {
        return _baseSettings;
    }

    public TokenStreamFactory streamFactory() {
        return _streamFactory;
    }

    public AnnotationIntrospector annotationIntrospector() {
        return _baseSettings.getAnnotationIntrospector();
    }

    /*
    /**********************************************************************
    /* Accessors, introspection
    /**********************************************************************
     */

    public TypeFactory typeFactory() {
        if (_typeFactory == null) {
            _typeFactory = _defaultTypeFactory();
        }
        return _typeFactory;
    }

    /**
     * Overridable method for changing default {@link SubtypeResolver} instance to use
     */
    protected TypeFactory _defaultTypeFactory() {
        return TypeFactory.defaultInstance();
    }

    public ClassIntrospector classIntrospector() {
        if (_classIntrospector == null) {
            _classIntrospector = _defaultClassIntrospector();
        }
        return _classIntrospector;
    }

    /**
     * Overridable method for changing default {@link SubtypeResolver} instance to use
     */
    protected ClassIntrospector _defaultClassIntrospector() {
        return new BasicClassIntrospector();
    }

    public TypeResolverProvider typeResolverProvider() {
        if (_typeResolverProvider == null) {
            _typeResolverProvider = _defaultTypeResolverProvider();
        }
        return _typeResolverProvider;
    }

    /**
     * Overridable method for changing default {@link TypeResolverProvider} instance to use
     */
    protected TypeResolverProvider _defaultTypeResolverProvider() {
        return new TypeResolverProvider();
    }

    public SubtypeResolver subtypeResolver() {
        if (_subtypeResolver == null) {
            _subtypeResolver = _defaultSubtypeResolver();
        }
        return _subtypeResolver;
    }

    /**
     * Overridable method for changing default {@link SubtypeResolver} prototype
     * to use.
     */
    protected SubtypeResolver _defaultSubtypeResolver() {
        return new StdSubtypeResolver();
    }

    public MixInHandler mixInHandler() {
        if (_mixInHandler == null) {
            _mixInHandler = _defaultMixInHandler();
        }
        return _mixInHandler;
    }

    /**
     * Overridable method for changing default {@link MixInHandler} prototype
     * to use.
     */
    protected MixInHandler _defaultMixInHandler() {
        return new MixInHandler(null);
    }

    /*
    /**********************************************************************
    /* Accessors, serialization factories, related
    /**********************************************************************
     */

    public SerializerFactory serializerFactory() {
        if (_serializerFactory == null) {
            _serializerFactory = _defaultSerializerFactory();
        }
        return _serializerFactory;
    }

    protected SerializerFactory _defaultSerializerFactory() {
        return BeanSerializerFactory.instance;
    }

    public DefaultSerializerProvider serializerProvider() {
        if (_serializerProvider == null) {
            _serializerProvider = _defaultSerializerProvider();
        }
        return _serializerProvider;
    }

    /**
     * Overridable method for changing default {@link SerializerProvider} prototype
     * to use.
     */
    protected DefaultSerializerProvider _defaultSerializerProvider() {
        return new DefaultSerializerProvider.Impl(_streamFactory);
    }

    public FilterProvider filterProvider() {
        return _filterProvider;
    }
    
    public PrettyPrinter defaultPrettyPrinter() {
        if (_defaultPrettyPrinter == null) {
            _defaultPrettyPrinter = _defaultPrettyPrinter();
        }
        return _defaultPrettyPrinter;
    }

    protected PrettyPrinter _defaultPrettyPrinter() {
        return DEFAULT_PRETTY_PRINTER;
    }

    /*
    /**********************************************************************
    /* Accessors, deserialization factories, related
    /**********************************************************************
     */

    public DeserializerFactory deserializerFactory() {
        if (_deserializerFactory == null) {
            _deserializerFactory = _defaultDeserializerFactory();
        }
        return _deserializerFactory;
    }

    DeserializerFactory _defaultDeserializerFactory() {
        return BeanDeserializerFactory.instance;
    }

    public DefaultDeserializationContext deserializationContext() {
        if (_deserializationContext == null) {
            _deserializationContext = _defaultDeserializationContext();
        }
        return _deserializationContext;
    }

    /**
     * Overridable method for changing default {@link SerializerProvider} prototype
     * to use.
     */
    protected DefaultDeserializationContext _defaultDeserializationContext() {
        return new DefaultDeserializationContext.Impl(deserializerFactory(),
                _streamFactory);
    }

    public InjectableValues injectableValues() {
        return _injectableValues;
    }

    public LinkedNode<DeserializationProblemHandler> deserializationProblemHandlers() {
        return _problemHandlers;
    }
    
    /*
    /**********************************************************************
    /* Changing features: mapper, ser, deser
    /**********************************************************************
     */

    public B enable(MapperFeature... features) {
        for (MapperFeature f : features) {
            _mapperFeatures |= f.getMask();
        }
        return _this();
    }

    public B disable(MapperFeature... features) {
        for (MapperFeature f : features) {
            _mapperFeatures &= ~f.getMask();
        }
        return _this();
    }

    public B configure(MapperFeature feature, boolean state)
    {
        if (state) {
            _mapperFeatures |= feature.getMask();
        } else {
            _mapperFeatures &= ~feature.getMask();
        }
        return _this();
    }

    public B enable(SerializationFeature... features) {
        for (SerializationFeature f : features) {
            _serFeatures |= f.getMask();
        }
        return _this();
    }

    public B disable(SerializationFeature... features) {
        for (SerializationFeature f : features) {
            _serFeatures &= ~f.getMask();
        }
        return _this();
    }

    public B configure(SerializationFeature feature, boolean state)
    {
        if (state) {
            _serFeatures |= feature.getMask();
        } else {
            _serFeatures &= ~feature.getMask();
        }
        return _this();
    }

    public B enable(DeserializationFeature... features) {
        for (DeserializationFeature f : features) {
            _deserFeatures |= f.getMask();
        }
        return _this();
    }

    public B disable(DeserializationFeature... features) {
        for (DeserializationFeature f : features) {
            _deserFeatures &= ~f.getMask();
        }
        return _this();
    }

    public B configure(DeserializationFeature feature, boolean state)
    {
        if (state) {
            _deserFeatures |= feature.getMask();
        } else {
            _deserFeatures &= ~feature.getMask();
        }
        return _this();
    }

    /*
    /**********************************************************************
    /* Changing features: parser, generator
    /**********************************************************************
     */

    public B enable(JsonParser.Feature... features) {
        for (JsonParser.Feature f : features) {
            _parserFeatures |= f.getMask();
        }
        return _this();
    }

    public B disable(JsonParser.Feature... features) {
        for (JsonParser.Feature f : features) {
            _parserFeatures &= ~f.getMask();
        }
        return _this();
    }

    public B configure(JsonParser.Feature feature, boolean state) {
        if (state) {
            _parserFeatures |= feature.getMask();
        } else {
            _parserFeatures &= ~feature.getMask();
        }
        return _this();
    }

    public B enable(JsonGenerator.Feature... features) {
        for (JsonGenerator.Feature f : features) {
            _generatorFeatures |= f.getMask();
        }
        return _this();
    }

    public B disable(JsonGenerator.Feature... features) {
        for (JsonGenerator.Feature f : features) {
            _generatorFeatures &= ~f.getMask();
        }
        return _this();
    }

    public B configure(JsonGenerator.Feature feature, boolean state) {
        if (state) {
            _generatorFeatures |= feature.getMask();
        } else {
            _generatorFeatures &= ~feature.getMask();
        }
        return _this();
    }

    /*
    /**********************************************************************
    /* Changing settings, config overrides
    /**********************************************************************
     */

    /**
     * Method for changing config overrides for specific type, through
     * callback to specific handler.
     */
    public B withConfigOverride(Class<?> forType,
            Consumer<MutableConfigOverride> handler) {
        handler.accept(_configOverrides.findOrCreateOverride(forType));
        return _this();
    }

    /**
     * Method for changing various aspects of configuration overrides.
     */
    public B withAllConfigOverrides(Consumer<ConfigOverrides> handler) {
        handler.accept(_configOverrides);
        return _this();
    }

    /**
     * Method for changing currently configured default {@link VisibilityChecker},
     * object used for determining whether given property element
     * (method, field, constructor) can be auto-detected or not.
     * Checker to modify is used for all POJO types for which there is no specific
     * per-type checker.
     *
     * @param handler Function that is given current default visibility checker and that
     *    needs to return either checker as is, or a new instance created using one or more of
     *    {@code withVisibility} (and similar) calls.
     */
    public B changeDefaultVisibility(UnaryOperator<VisibilityChecker> handler) {
        VisibilityChecker oldV = _configOverrides.getDefaultVisibility();
        VisibilityChecker newV = handler.apply(oldV);
        if (newV != oldV) {
            Objects.requireNonNull(newV, "Can not assign null default VisibilityChecker");
            _configOverrides.setDefaultVisibility(newV);
        }
        return _this();
    }

    /**
     * Method for changing currently default settings for property inclusion, used for determining
     * whether POJO properties with certain value should be excluded or not: most common case being
     * exclusion of `null` values.
     */
    public B changeDefaultPropertyInclusion(UnaryOperator<JsonInclude.Value> handler) {
        JsonInclude.Value oldIncl = _configOverrides.getDefaultInclusion();
        JsonInclude.Value newIncl = handler.apply(oldIncl);
        if (newIncl != oldIncl) {
            Objects.requireNonNull(newIncl, "Can not assign null default Property Inclusion");
            _configOverrides.setDefaultInclusion(newIncl);
        }
        //public ObjectMapper setDefaultPropertyInclusion() {
        return _this();
    }

    /**
     * Method for changing currently default settings for handling of `null` values during
     * deserialization, regarding whether they are set as-is, ignored completely, or possible
     * transformed into "empty" value of the target type (if any).
     */
    public B changeDefaultNullHandling(UnaryOperator<JsonSetter.Value> handler) {
        JsonSetter.Value oldIncl = _configOverrides.getDefaultNullHandling();
        JsonSetter.Value newIncl = handler.apply(oldIncl);
        if (newIncl != oldIncl) {
            Objects.requireNonNull(newIncl, "Can not assign null default Null Handling");
            _configOverrides.setDefaultNullHandling(newIncl);
        }
        return _this();
    }

    /**
     * Method for setting default Setter configuration, regarding things like
     * merging, null-handling; used for properties for which there are
     * no per-type or per-property overrides (via annotations or config overrides).
     */
    public B defaultMergeable(Boolean b) {
        _configOverrides.setDefaultMergeable(b);
        return _this();
    }

    /*
    /**********************************************************************
    /* Module registration, discovery, access
    /**********************************************************************
     */

    /**
     * Method that will drop all modules added (via {@link #addModule} and similar
     * calls) to this builder.
     */
    public B removeAllModules() {
        _modules = null;
        return _this();
    }

    /**
     * Method will add given module to be registered when mapper is built, possibly
     * replacing an earlier instance of the module (as specified by its
     * {@link Module#getRegistrationId()}).
     * Actual registration occurs in addition order (considering last add to count,
     * in case of re-registration for same id) when {@link #build()} is called.
     */
    public B addModule(com.fasterxml.jackson.databind.Module module)
    {
        if (module.getModuleName() == null) {
            throw new IllegalArgumentException("Module without defined name");
        }
        if (module.version() == null) {
            throw new IllegalArgumentException("Module without defined version");
        }
        // If dups are ok we still need a key, but just need to ensure it is unique so:
        final Object moduleId = module.getRegistrationId();
        if (_modules == null) {
            _modules = new LinkedHashMap<>();
        } else {
            // Important: since order matters, we won't try to simply replace existing one.
            // Could do in different order (put, and only re-order if there was old value),
            // but simple does it for now.
            _modules.remove(moduleId);
        }
        _modules.put(moduleId, module);
        return _this();
    }

    public B addModules(com.fasterxml.jackson.databind.Module... modules)
    {
        for (com.fasterxml.jackson.databind.Module module : modules) {
            addModule(module);
        }
        return _this();
    }

    public B addModules(Iterable<? extends com.fasterxml.jackson.databind.Module> modules)
    {
        for (com.fasterxml.jackson.databind.Module module : modules) {
            addModule(module);
        }
        return _this();
    }

    /**
     * Method for locating available methods, using JDK {@link ServiceLoader}
     * facility, along with module-provided SPI.
     *<p>
     * Note that method does not do any caching, so calls should be considered
     * potentially expensive.
     */
    public static List<com.fasterxml.jackson.databind.Module> findModules() {
        return findModules(null);
    }

    /**
     * Method for locating available methods, using JDK {@link ServiceLoader}
     * facility, along with module-provided SPI.
     *<p>
     * Note that method does not do any caching, so calls should be considered
     * potentially expensive.
     */
    public static List<com.fasterxml.jackson.databind.Module> findModules(ClassLoader classLoader)
    {
        ArrayList<com.fasterxml.jackson.databind.Module> modules = new ArrayList<>();
        ServiceLoader<com.fasterxml.jackson.databind.Module> loader = secureGetServiceLoader(com.fasterxml.jackson.databind.Module.class, classLoader);
        for (com.fasterxml.jackson.databind.Module module : loader) {
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
     *   addModules(builder.findModules());
     *</code>
     *<p>
     * As with {@link #findModules()}, no caching is done for modules, so care
     * needs to be taken to either create and share a single mapper instance;
     * or to cache introspected set of modules.
     */
    public B findAndAddModules() {
        return addModules(findModules());
    }

    /**
     * "Accessor" method that will expose set of registered modules, in addition
     * order, to given handler.
     */
    public B withModules(Consumer<com.fasterxml.jackson.databind.Module> handler) {
        if (_modules != null) {
            _modules.values().forEach(handler);
        }
        return _this();
    }

    /*
    /**********************************************************************
    /* Changing base settings
    /**********************************************************************
     */

    public B baseSettings(BaseSettings b) {
        _baseSettings = b;
        return _this();
    }

    /**
     * Method for replacing {@link AnnotationIntrospector} used by the
     * mapper instance to be built.
     * Note that doing this will replace the current introspector, which
     * may lead to unavailability of core Jackson annotations.
     * If you want to combine handling of multiple introspectors,
     * have a look at {@link com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair}.
     *
     * @see com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair
     */
    public B annotationIntrospector(AnnotationIntrospector intr) {
        _baseSettings = _baseSettings.withAnnotationIntrospector(intr);
        return _this();
    }

    public B nodeFactory(JsonNodeFactory f) {
        _baseSettings = _baseSettings.with(f);
        return _this();
    }

    /*
    /**********************************************************************
    /* Changing introspection helpers
    /**********************************************************************
     */

    public B typeFactory(TypeFactory f) {
        _typeFactory = f;
        return _this();
    }

    public B addTypeModifier(TypeModifier modifier) {
        // important! Need to use getter, to force lazy construction if need be
        _typeFactory = typeFactory()
                .withModifier(modifier);
        return _this();
    }

    protected B typeResolverProvider(TypeResolverProvider p) {
        _typeResolverProvider = p;
        return _this();
    }

    public B classIntrospector(ClassIntrospector ci) {
        _classIntrospector = ci;
        return _this();
    }

    public B subtypeResolver(SubtypeResolver r) {
        _subtypeResolver = r;
        return _this();
    }

    /**
     * Method for configuring {@link HandlerInstantiator} to use for creating
     * instances of handlers (such as serializers, deserializers, type and type
     * id resolvers), given a class.
     *
     * @param hi Instantiator to use; if null, use the default implementation
     */
    public B handlerInstantiator(HandlerInstantiator hi) {
        _baseSettings = _baseSettings.with(hi);
        return _this();
    }

    public B propertyNamingStrategy(PropertyNamingStrategy s) {
        _baseSettings = _baseSettings.with(s);
        return _this();
    }

    /*
    /**********************************************************************
    /* Changing factories, serialization
    /**********************************************************************
     */

    public B serializerFactory(SerializerFactory f) {
        _serializerFactory = f;
        return _this();
    }

    public B serializerProvider(DefaultSerializerProvider prov) {
        _serializerProvider = prov;
        return _this();
    }

    /**
     * Method for configuring this mapper to use specified {@link FilterProvider} for
     * mapping Filter Ids to actual filter instances.
     *<p>
     * Note that usually it is better to use method in {@link ObjectWriter}, but sometimes
     * this method is more convenient. For example, some frameworks only allow configuring
     * of ObjectMapper instances and not {@link ObjectWriter}s.
     */
    public B filterProvider(FilterProvider prov) {
        _filterProvider = prov;
        return _this();
    }

    public B defaultPrettyPrinter(PrettyPrinter pp) {
        _defaultPrettyPrinter = pp;
        return _this();
    }

    /*
    /**********************************************************************
    /* Changing factories, related, deserialization
    /**********************************************************************
     */

    public B deserializerFactory(DeserializerFactory f) {
        _deserializerFactory = f;
        // 19-Feb-2018, tatu: Hopefully not needed in future but is needed for now
        if (_deserializationContext != null) {
            _deserializationContext = _deserializationContext.with(f);
        }
        return _this();
    }

    public B deserializationContext(DefaultDeserializationContext ctxt) {
        _deserializationContext = ctxt;
        return _this();
    }

    public B injectableValues(InjectableValues v) {
        _injectableValues = v;
        return _this();
    }

    /**
     * Method used for adding a {@link DeserializationProblemHandler} for this
     * builder, at the head of the list (meaning it has priority over handler
     * registered earlier).
     */
    public B addHandler(DeserializationProblemHandler h) {
        if (!LinkedNode.contains(_problemHandlers, h)) {
            _problemHandlers = new LinkedNode<>(h, _problemHandlers);
        }
        return _this();
    }

    /**
     * Method that may be used to remove all {@link DeserializationProblemHandler}s added
     * to this builder (if any).
     */
    public B clearProblemHandlers() {
        _problemHandlers = null;
        return _this();
    }

    /**
     * Method for inserting specified {@link AbstractTypeResolver} as the first resolver
     * in chain of possibly multiple resolvers.
     */
    public B addAbstractTypeResolver(AbstractTypeResolver resolver) {
        if (resolver == null) {
            throw new IllegalArgumentException("Cannot pass null resolver");
        }
        _abstractTypeResolvers = ArrayBuilders.insertInListNoDup(_abstractTypeResolvers, resolver);
        return _this();
    }

    /*
    /**********************************************************************
    /* Changing settings, date/time
    /**********************************************************************
     */

    /**
     * Method for configuring the default {@link DateFormat} to use when serializing time
     * values as Strings, and deserializing from JSON Strings.
     * If you need per-request configuration, factory methods in
     * {@link ObjectReader} and {@link ObjectWriter} instead.
     */
    public B defaultDateFormat(DateFormat f) {
        _baseSettings = _baseSettings.with(f);
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, (f == null));
        return _this();
    }

    /**
     * Method for overriding default TimeZone to use for formatting.
     * Default value used is UTC (NOT default TimeZone of JVM).
     */
    public B defaultTimeZone(TimeZone tz) {
        _baseSettings = _baseSettings.with(tz);
        return _this();
    }

    /**
     * Method for overriding default locale to use for formatting.
     * Default value used is {@link Locale#getDefault()}.
     */
    public B defaultLocale(Locale locale) {
        _baseSettings = _baseSettings.with(locale);
        return _this();
    }

    /*
    /**********************************************************************
    /* Changing settings, formatting
    /**********************************************************************
     */

    /**
     * Method that will configure default {@link Base64Variant} that
     * <code>byte[]</code> serializers and deserializers will use.
     * 
     * @param v Base64 variant to use
     * 
     * @return This mapper, for convenience to allow chaining
     */
    public B defaultBase64Variant(Base64Variant v) {
        _baseSettings = _baseSettings.with(v);
        return _this();
    }

    /*
    /**********************************************************************
    /* Adding Mix-ins
    /**********************************************************************
     */

    /**
     * Method that may be used to completely change mix-in handling by providing
     * alternate {@link MixInHandler} implementation.
     * Most of the time this is NOT the method you want to call, and rather are looking
     * for {@link #mixInOverrides}.
     */
    public B mixInHandler(MixInHandler h) {
        _mixInHandler = h;
        return _this();
    }

    /**
     * Method that allows defining "override" mix-in resolver: something that is checked first,
     * before simple mix-in definitions.
     */
    public B mixInOverrides(MixInResolver r) {
        _mixInHandler = mixInHandler().withOverrides(r);
        return _this();
    }
    
    /**
     * Method to use for defining mix-in annotations to use for augmenting
     * annotations that processable (serializable / deserializable)
     * classes have.
     * This convenience method is equivalent to iterating over all entries
     * and calling {@link #addMixIn} with `key` and `value` of each entry.
     */
    public B addMixIns(Map<Class<?>, Class<?>> sourceMixins)
    {
        mixInHandler().addLocalDefinitions(sourceMixins);
        return _this();
    }

    /**
     * Method to use for defining mix-in annotations to use for augmenting
     * annotations that classes have, for purpose of configuration serialization
     * and/or deserialization processing.
     * Mixing in is done when introspecting class annotations and properties.
     * Annotations from "mixin" class (and its supertypes)
     * will <b>override</b>
     * annotations that target classes (and their super-types) have.
     *<p>
     * Note that standard mixin handler implementations will only allow a single mix-in
     * source class per target, so if there was a previous mix-in defined target it will
     * be cleared. This also means that you can remove mix-in definition by specifying
     * {@code mixinSource} of {@code null}
     */
    public B addMixIn(Class<?> target, Class<?> mixinSource)
    {
        mixInHandler().addLocalDefinition(target, mixinSource);
        return _this();
    }

    /*
    /**********************************************************************
    /* Subtype registration
    /**********************************************************************
     */

    public B registerSubtypes(Class<?>... subtypes) {
        subtypeResolver().registerSubtypes(subtypes);
        return _this();
    }

    public B registerSubtypes(NamedType... subtypes) {
        subtypeResolver().registerSubtypes(subtypes);
        return _this();
    }

    public B registerSubtypes(Collection<Class<?>> subtypes) {
        subtypeResolver().registerSubtypes(subtypes);
        return _this();
    }

    /*
    /**********************************************************************
    /* Default typing (temporarily)
    /**********************************************************************
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
    public B enableDefaultTyping() {
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
    public B enableDefaultTyping(DefaultTyping dti) {
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
    public B enableDefaultTyping(DefaultTyping applicability, JsonTypeInfo.As includeAs)
    {
        // 18-Sep-2014, tatu: Let's add explicit check to ensure no one tries to
        //   use "As.EXTERNAL_PROPERTY", since that will not work (with 2.5+)
        if (includeAs == JsonTypeInfo.As.EXTERNAL_PROPERTY) {
            throw new IllegalArgumentException("Cannot use includeAs of "+includeAs+" for Default Typing");
        }
        return setDefaultTyping(new DefaultTypeResolverBuilder(applicability, includeAs));
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
    public B enableDefaultTypingAsProperty(DefaultTyping applicability, String propertyName)
    {
        return setDefaultTyping(new DefaultTypeResolverBuilder(applicability, propertyName));
    }

    /**
     * Method for disabling automatic inclusion of type information; if so, only
     * explicitly annotated types (ones with
     * {@link com.fasterxml.jackson.annotation.JsonTypeInfo}) will have
     * additional embedded type information.
     */
    public B disableDefaultTyping() {
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
    public B setDefaultTyping(TypeResolverBuilder<?> typer) {
        _baseSettings = _baseSettings.with(typer);
        return _this();
    }

    /*
    /**********************************************************************
    /* Other helper methods
    /**********************************************************************
     */

    // silly convenience cast method we need
    @SuppressWarnings("unchecked")
    protected final B _this() { return (B) this; }
}
