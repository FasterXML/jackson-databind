package tools.jackson.databind.cfg;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.DateFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.core.*;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.core.util.Snapshottable;
import tools.jackson.databind.*;
import tools.jackson.databind.deser.*;
import tools.jackson.databind.introspect.*;
import tools.jackson.databind.jsontype.*;
import tools.jackson.databind.jsontype.impl.DefaultTypeResolverBuilder;
import tools.jackson.databind.jsontype.impl.StdSubtypeResolver;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.ser.*;
import tools.jackson.databind.type.LogicalType;
import tools.jackson.databind.type.TypeFactory;
import tools.jackson.databind.type.TypeModifier;
import tools.jackson.databind.util.ArrayBuilders;
import tools.jackson.databind.util.LinkedNode;
import tools.jackson.databind.util.RootNameLookup;
import tools.jackson.databind.util.StdDateFormat;

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
    protected final static long DEFAULT_MAPPER_FEATURES = MapperFeature.collectLongDefaults();
    protected final static int DEFAULT_SER_FEATURES = ConfigFeature.collectFeatureDefaults(SerializationFeature.class);
    protected final static int DEFAULT_DESER_FEATURES = ConfigFeature.collectFeatureDefaults(DeserializationFeature.class);

    protected final static PrettyPrinter DEFAULT_PRETTY_PRINTER = new DefaultPrettyPrinter();
    protected final static AnnotationIntrospector DEFAULT_ANNOTATION_INTROSPECTOR = new JacksonAnnotationIntrospector();

    protected final static PolymorphicTypeValidator DEFAULT_TYPE_VALIDATOR = new DefaultBaseTypeLimitingValidator();

    protected final static AccessorNamingStrategy.Provider DEFAULT_ACCESSOR_NAMING = new DefaultAccessorNamingStrategy.Provider();

    protected final static BaseSettings DEFAULT_BASE_SETTINGS = new BaseSettings(
            DEFAULT_ANNOTATION_INTROSPECTOR,
            null, DEFAULT_ACCESSOR_NAMING,
            null, // no default typing, by default
            DEFAULT_TYPE_VALIDATOR, // and polymorphic type by class won't pass either
            StdDateFormat.instance, null,
            Locale.getDefault(),
            null, // to indicate "use Jackson default TimeZone" (UTC)
            Base64Variants.getDefaultVariant(),
            DefaultCacheProvider.defaultInstance(),
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

    /**
     * Coercion settings (global, per-type overrides)
     */
    protected final CoercionConfigs _coercionConfigs;

    /*
    /**********************************************************************
    /* Modules
    /**********************************************************************
     */

    /**
     * Modules registered for addition, indexed by registration id.
     */
    protected Map<Object, JacksonModule> _modules;

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
     * ({@link tools.jackson.databind.jsontype.TypeSerializer}s,
     * {@link tools.jackson.databind.jsontype.TypeDeserializer}s).
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

    /**
     * {@link SerializationContexts} to use as factory for stateful {@link SerializerProvider}s
     */
    protected SerializationContexts _serializationContexts;

    protected SerializerFactory _serializerFactory;

    protected FilterProvider _filterProvider;

    protected PrettyPrinter _defaultPrettyPrinter;

    /*
    /**********************************************************************
    /* Factories etc for deserialization
    /**********************************************************************
     */

    /**
     * Factory to use for creating per-operation contexts.
     */
    protected DeserializationContexts _deserializationContexts;

    protected DeserializerFactory _deserializerFactory;

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

    protected ConstructorDetector _ctorDetector;

    protected CacheProvider _cacheProvider;

    /*
    /**********************************************************************
    /* Handlers/factories, other:
    /**********************************************************************
     */

    /**
     * Explicitly configured default {@link ContextAttributes}, if any.
     */
    protected ContextAttributes _defaultAttributes;

    /*
    /**********************************************************************
    /* Feature flags: ser, deser
    /**********************************************************************
     */

    /**
     * Set of shared mapper features enabled.
     */
    protected long _mapperFeatures;

    /**
     * Set of {@link SerializationFeature}s enabled.
     */
    protected int _serFeatures;

    /**
     * Set of {@link DeserializationFeature}s enabled.
     */
    protected int _deserFeatures;

    protected DatatypeFeatures _datatypeFeatures;

    /*
    /**********************************************************************
    /* Feature flags: generation, parsing
    /**********************************************************************
     */

    /**
     * States of {@link StreamReadFeature}s to enable/disable.
     */
    protected int _streamReadFeatures;

    /**
     * States of {@link StreamWriteFeature}s to enable/disable.
     */
    protected int _streamWriteFeatures;

    /**
     * Optional per-format parser feature flags.
     */
    protected int _formatReadFeatures;

    /**
     * Optional per-format generator feature flags.
     */
    protected int _formatWriteFeatures;

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
        _coercionConfigs = new CoercionConfigs();
        _modules = null;

        _streamReadFeatures = streamFactory.getStreamReadFeatures();
        _streamWriteFeatures = streamFactory.getStreamWriteFeatures();
        _formatReadFeatures = streamFactory.getFormatReadFeatures();
        _formatWriteFeatures = streamFactory.getFormatWriteFeatures();

        _mapperFeatures = DEFAULT_MAPPER_FEATURES;
        // Some overrides we may need based on format
        if (streamFactory.requiresPropertyOrdering()) {
            _mapperFeatures |= MapperFeature.SORT_PROPERTIES_ALPHABETICALLY.getLongMask();
        }
        _deserFeatures = DEFAULT_DESER_FEATURES;
        _serFeatures = DEFAULT_SER_FEATURES;
        _datatypeFeatures = DatatypeFeatures.defaultFeatures();

        _typeFactory = null;
        _classIntrospector = null;
        _typeResolverProvider = null;
        _subtypeResolver = null;
        _mixInHandler = null;

        _serializerFactory = null;
        _serializationContexts = null;
        _filterProvider = null;

        _deserializerFactory = null;
        _deserializationContexts = null;
        _injectableValues = null;
        _problemHandlers = null;
        _ctorDetector = null;
        _cacheProvider = null;
        _abstractTypeResolvers = NO_ABSTRACT_TYPE_RESOLVERS;

        _defaultAttributes = null;
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
        _coercionConfigs = Snapshottable.takeSnapshot(state._coercionConfigs);

        _streamReadFeatures = state._streamReadFeatures;
        _streamWriteFeatures = state._streamWriteFeatures;
        _formatReadFeatures = state._formatReadFeatures;
        _formatWriteFeatures = state._formatWriteFeatures;
        _mapperFeatures = state._mapperFeatures;
        _deserFeatures = state._deserFeatures;
        _serFeatures = state._serFeatures;
        _datatypeFeatures = state._datatypeFeatures;

        // Handlers, introspection
        _typeFactory = Snapshottable.takeSnapshot(state._typeFactory);

        _classIntrospector = state._classIntrospector;
        _typeResolverProvider = state._typeResolverProvider;
        _subtypeResolver = Snapshottable.takeSnapshot(state._subtypeResolver);
        _mixInHandler = (MixInHandler) Snapshottable.takeSnapshot(state._mixInHandler);

        // Factories for serialization
        _serializationContexts = state._serializationContexts;
        _serializerFactory = state._serializerFactory;
        _filterProvider = state._filterProvider;
        _defaultPrettyPrinter = state._defaultPrettyPrinter;

        // Factories for deserialization
        _deserializationContexts = state._deserializationContexts;
        _deserializerFactory = state._deserializerFactory;
        _injectableValues = Snapshottable.takeSnapshot(state._injectableValues);
        _problemHandlers = state._problemHandlers;
        _abstractTypeResolvers = state._abstractTypeResolvers;
        _ctorDetector = state._ctorDetector;
        _cacheProvider = state._cacheProvider;

        // Factories/handlers, other
        _defaultAttributes = Snapshottable.takeSnapshot(state._defaultAttributes);

        // Modules
        if (state._modules == null) {
            _modules = null;
        } else {
            _modules = new LinkedHashMap<>();
            for (JacksonModule mod : state._modules) {
                addModule(mod);
            }
        }
    }

    /*
    protected MapperBuilder(MapperBuilder<?,?> base)
    {
        _streamFactory = base._streamFactory;
        _baseSettings = base._baseSettings;
        _configOverrides = base._configOverrides;
        _coercionConfigs = base._coercionConfigs;

        _mapperFeatures = base._mapperFeatures;
        _serFeatures = base._serFeatures;
        _deserFeatures = base._deserFeatures;

        _streamReadFeatures = base._streamReadFeatures;
        _stremWriteFeatures = base._stremWriteFeatures;
        _formatReadFeatures = base._formatReadFeatures;
        _formatWriteFeatures = base._formatWriteFeatures;
        _datatypeFeatures = base._datatypeFeatures;

        _typeFactory = base._typeFactory;
        _classIntrospector = base._classIntrospector;
        _typeResolverProvider = base._typeResolverProvider;
        _subtypeResolver = base._subtypeResolver;
        _mixInHandler = base._mixInHandler;

        _serializerFactory = base._serializerFactory;
        _serializationContexts = base._serializationContexts;
        _filterProvider = base._filterProvider;

        _deserializerFactory = base._deserializerFactory;
        _deserializationContext = base._deserializationContext;
        _injectableValues = base._injectableValues;
        _problemHandlers = base._problemHandlers;
        _abstractTypeResolvers = base._abstractTypeResolvers;
        _ctorDetector = base._ctorDetector;
        _cacheProvider = base._cacheProvider;
    }
    */

    /*
    /**********************************************************************
    /* Methods for actual build process
    /**********************************************************************
     */

    /**
     * Method to call to create actual mapper instance.
     *<p>
     * Implementation detail: usually construction occurs by passing {@code this}
     * builder instance to constructor of specific mapper type builder builds.
     */
    public abstract M build();

    /**
     * Method called by mapper being constructed to first save state (delegated to
     * {code _saveState()} method), then apply modules (if any), and then return
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

    public SerializationConfig buildSerializationConfig(ConfigOverrides configOverrides,
            MixInHandler mixins, TypeFactory tf, ClassIntrospector classIntr, SubtypeResolver str,
            RootNameLookup rootNames,
            FilterProvider filterProvider)
    {
        return new SerializationConfig(this,
                _mapperFeatures, _serFeatures, _streamWriteFeatures, _formatWriteFeatures,
                configOverrides,
                tf, classIntr, mixins, str,
                defaultAttributes(), rootNames,
                filterProvider);
    }

    public DeserializationConfig buildDeserializationConfig(ConfigOverrides configOverrides,
            MixInHandler mixins, TypeFactory tf, ClassIntrospector classIntr, SubtypeResolver str,
            RootNameLookup rootNames,
            CoercionConfigs coercionConfigs)
    {
        return new DeserializationConfig(this,
                _mapperFeatures, _deserFeatures, _streamReadFeatures, _formatReadFeatures,
                configOverrides, coercionConfigs,
                tf, classIntr, mixins, str,
                defaultAttributes(), rootNames,
                _abstractTypeResolvers, _ctorDetector);
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
    public boolean isEnabled(DatatypeFeature f) {
        return _datatypeFeatures.isEnabled(f);
    }

    public boolean isEnabled(StreamReadFeature f) {
        return f.enabledIn(_streamReadFeatures);
    }
    public boolean isEnabled(StreamWriteFeature f) {
        return f.enabledIn(_streamWriteFeatures);
    }

    public DatatypeFeatures datatypeFeatures() {
        return _datatypeFeatures;
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

    /**
     * Overridable method for changing default {@link ContextAttributes} instance to use
     * if not explicitly specified during build process.
     */
    public ContextAttributes defaultAttributes() {
        // 01-Feb-2020, tatu: Looks different from pattern used with other
        //    defaults; seems better not to change state of builder?
        if (_defaultAttributes == null) {
            return ContextAttributes.getEmpty();
        }
        return _defaultAttributes;
    }

    /**
     * Overridable method for changing default {@link ContextAttributes} instance to use
     * if not explicitly specified during build process.
     */
    protected ContextAttributes _defaultDefaultAttributes() {
        return ContextAttributes.getEmpty();
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

    public SerializationContexts serializationContexts() {
        if (_serializationContexts == null) {
            _serializationContexts = _defaultSerializationContexts();
        }
        return _serializationContexts;
    }

    /**
     * Overridable method for changing default {@link SerializerProvider} prototype
     * to use.
     */
    protected SerializationContexts _defaultSerializationContexts() {
        return new SerializationContexts.DefaultImpl();
    }

    public SerializerFactory serializerFactory() {
        if (_serializerFactory == null) {
            _serializerFactory = _defaultSerializerFactory();
        }
        return _serializerFactory;
    }

    protected SerializerFactory _defaultSerializerFactory() {
        return BeanSerializerFactory.instance;
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

    public DeserializationContexts deserializationContexts() {
        if (_deserializationContexts == null) {
            _deserializationContexts = _defaultDeserializationContexts();
        }
        return _deserializationContexts;
    }

    /**
     * Overridable method for changing default {@link SerializerProvider} prototype
     * to use.
     */
    protected DeserializationContexts _defaultDeserializationContexts() {
        return new DeserializationContexts.DefaultImpl();
    }

    public DeserializerFactory deserializerFactory() {
        if (_deserializerFactory == null) {
            _deserializerFactory = _defaultDeserializerFactory();
        }
        return _deserializerFactory;
    }

    DeserializerFactory _defaultDeserializerFactory() {
        return BeanDeserializerFactory.instance;
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
            _mapperFeatures |= f.getLongMask();
        }
        return _this();
    }

    public B disable(MapperFeature... features) {
        for (MapperFeature f : features) {
            _mapperFeatures &= ~f.getLongMask();
        }
        return _this();
    }

    public B configure(MapperFeature feature, boolean state)
    {
        if (state) {
            _mapperFeatures |= feature.getLongMask();
        } else {
            _mapperFeatures &= ~feature.getLongMask();
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

    public B enable(DatatypeFeature... features) {
        _datatypeFeatures = _datatypeFeatures.withFeatures(features);
        return _this();
    }

    public B disable(DatatypeFeature... features) {
        _datatypeFeatures = _datatypeFeatures.withoutFeatures(features);
        return _this();
    }

    public B configure(DatatypeFeature feature, boolean state) {
        if (state) {
            _datatypeFeatures = _datatypeFeatures.with(feature);
        } else {
            _datatypeFeatures = _datatypeFeatures.without(feature);
        }
        return _this();
    }

    /*
    /**********************************************************************
    /* Changing features: parser, generator
    /**********************************************************************
     */

    public B enable(StreamReadFeature... features) {
        for (StreamReadFeature f : features) {
            _streamReadFeatures |= f.getMask();
        }
        return _this();
    }

    public B disable(StreamReadFeature... features) {
        for (StreamReadFeature f : features) {
            _streamReadFeatures &= ~f.getMask();
        }
        return _this();
    }

    public B configure(StreamReadFeature feature, boolean state) {
        if (state) {
            _streamReadFeatures |= feature.getMask();
        } else {
            _streamReadFeatures &= ~feature.getMask();
        }
        return _this();
    }

    public B enable(StreamWriteFeature... features) {
        for (StreamWriteFeature f : features) {
            _streamWriteFeatures |= f.getMask();
        }
        return _this();
    }

    public B disable(StreamWriteFeature... features) {
        for (StreamWriteFeature f : features) {
            _streamWriteFeatures &= ~f.getMask();
        }
        return _this();
    }

    public B configure(StreamWriteFeature feature, boolean state) {
        if (state) {
            _streamWriteFeatures |= feature.getMask();
        } else {
            _streamWriteFeatures &= ~feature.getMask();
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

    /**
     * Method for setting default Setter configuration, regarding things like
     * merging, null-handling; used for properties for which there are
     * no per-type or per-property overrides (via annotations or config overrides).
     */
    public B defaultLeniency(Boolean b) {
        _configOverrides.setDefaultLeniency(b);
        return _this();
    }

    /*
    /**********************************************************************
    /* Changing settings, coercion config
    /**********************************************************************
     */

    /**
     * Method for changing coercion config for specific logical types, through
     * callback to specific handler.
     */
    public B withCoercionConfig(LogicalType forType,
            Consumer<MutableCoercionConfig> handler) {
        handler.accept(_coercionConfigs.findOrCreateCoercion(forType));
        return _this();
    }

    /**
     * Method for changing coercion config for specific physical type, through
     * callback to specific handler.
     */
    public B withCoercionConfig(Class<?> forType,
            Consumer<MutableCoercionConfig> handler) {
        handler.accept(_coercionConfigs.findOrCreateCoercion(forType));
        return _this();
    }

    /**
     * Method for changing target-type-independent coercion configuration defaults.
     */
    public B withCoercionConfigDefaults(Consumer<MutableCoercionConfig> handler) {
        handler.accept(_coercionConfigs.defaultCoercions());
        return _this();
    }

    // 03-Jun-2020, tatu: Needed at least for snapshotting, if not for other usage...
    /**
     * Method for changing various aspects of configuration overrides.
     */
    public B withAllCoercionConfigs(Consumer<CoercionConfigs> handler) {
        handler.accept(_coercionConfigs);
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
     * {@link JacksonModule#getRegistrationId()}).
     * Actual registration occurs in addition order (considering last add to count,
     * in case of re-registration for same id) when {@link #build()} is called.
     */
    public B addModule(JacksonModule module)
    {
        _verifyModuleMetadata(module);
        final Object moduleId = module.getRegistrationId();
        if (_modules == null) {
            _modules = new LinkedHashMap<>();
        } else {
            // Important: since order matters, we won't try to simply replace existing one.
            // Could do in different order (put, and only re-order if there was old value),
            // but simple does it for now.
            _modules.remove(moduleId);
        }

        // 10-Sep-2019, tatu: [databind#2432] Module dependencies; need to add first
        //   but unlike main module, do NOT replace module if already added
        for (JacksonModule dep : module.getDependencies()) {
            _verifyModuleMetadata(dep);
            _modules.putIfAbsent(dep.getRegistrationId(), dep);
        }
        _modules.put(moduleId, module);
        return _this();
    }

    private void _verifyModuleMetadata(JacksonModule module)
    {
        if (module.getModuleName() == null) {
            throw new IllegalArgumentException("Module ("+module.getClass().getName()+") without defined name");
        }
        if (module.version() == null) {
            throw new IllegalArgumentException("Module ("+module.getClass().getName()+") without defined version");
        }
    }

    public B addModules(JacksonModule... modules)
    {
        for (JacksonModule module : modules) {
            addModule(module);
        }
        return _this();
    }

    public B addModules(Iterable<? extends JacksonModule> modules)
    {
        for (JacksonModule module : modules) {
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
    public static List<JacksonModule> findModules() {
        return findModules(null);
    }

    /**
     * Method for locating available methods, using JDK {@link ServiceLoader}
     * facility, along with module-provided SPI.
     *<p>
     * Note that method does not do any caching, so calls should be considered
     * potentially expensive.
     */
    public static List<JacksonModule> findModules(ClassLoader classLoader)
    {
        ArrayList<JacksonModule> modules = new ArrayList<>();
        ServiceLoader<JacksonModule> loader = secureGetServiceLoader(JacksonModule.class, classLoader);
        for (JacksonModule module : loader) {
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
    public B withModules(Consumer<JacksonModule> handler) {
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
     * have a look at {@link tools.jackson.databind.introspect.AnnotationIntrospectorPair}.
     *
     * @see tools.jackson.databind.introspect.AnnotationIntrospectorPair
     */
    public B annotationIntrospector(AnnotationIntrospector intr) {
        _baseSettings = _baseSettings.withAnnotationIntrospector(intr);
        return _this();
    }

    /**
     * Method for replacing default {@link ContextAttributes} that the mapper
     * uses: usually one initialized with a set of default shared attributes, but
     * potentially also with a custom implementation.
     *<p>
     * NOTE: instance specified will need to be thread-safe for usage, similar to the
     * default ({@link ContextAttributes.Impl}).
     *
     * @param attrs Default instance to use, if not {@code null}, or {@code null} for "use empty default set".
     *
     * @return This Builder instance to allow call chaining
     */
    public B defaultAttributes(ContextAttributes attrs) {
        _defaultAttributes = attrs;
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

    public B polymorphicTypeValidator(PolymorphicTypeValidator ptv) {
        _baseSettings = _baseSettings.with(ptv);
        return _this();
    }

    /**
     * Method for configuring {@link HandlerInstantiator} to use for creating
     * instances of handlers (such as serializers, deserializers, type and type
     * id resolvers), given a class.
     *
     * @param hi Instantiator to use; if null, use the default implementation
     *
     * @return Builder instance itself to allow chaining
     */
    public B handlerInstantiator(HandlerInstantiator hi) {
        _baseSettings = _baseSettings.with(hi);
        return _this();
    }

    /**
     * Method for configuring {@link PropertyNamingStrategy} to use for adapting
     * POJO property names (internal) into content property names (external)
     *
     * @param s Strategy instance to use; if null, use the default implementation
     *
     * @return Builder instance itself to allow chaining
     */
    public B propertyNamingStrategy(PropertyNamingStrategy s) {
        _baseSettings = _baseSettings.with(s);
        return _this();
    }

    /**
     * Method for configuring {@link AccessorNamingStrategy} to use for auto-detecting
     * accessor ("getter") and mutator ("setter") methods based on naming of methods.
     *
     * @param s Strategy instance to use; if null, use the default implementation
     *
     * @return Builder instance itself to allow chaining
     */
    public B accessorNaming(AccessorNamingStrategy.Provider s) {
        if (s == null) {
            s = new DefaultAccessorNamingStrategy.Provider();
        }
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

    public B serializationContexts(SerializationContexts ctxt) {
        _serializationContexts = ctxt;
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
        return _this();
    }

    public B deserializationContexts(DeserializationContexts ctxt) {
        _deserializationContexts = ctxt;
        return _this();
    }

    public B injectableValues(InjectableValues v) {
        _injectableValues = v;
        return _this();
    }

    public B nodeFactory(JsonNodeFactory f) {
        _baseSettings = _baseSettings.with(f);
        return _this();
    }

    /**
     * Method for specifying {@link ConstructorDetector} to use for
     * determining some aspects of creator auto-detection (specifically
     * auto-detection of constructor, and in particular behavior with
     * single-argument constructors).
     */
    public B constructorDetector(ConstructorDetector cd) {
        _ctorDetector = cd;
        return _this();
    }

    public B cacheProvider(CacheProvider cacheProvider) {
        _cacheProvider = cacheProvider;
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
    /* Configuring Mix-ins
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
     * (although preferred mechanism is {@link #removeMixIn})
     *
     * @param target Target class on which to add annotations
     * @param mixinSource Class that has annotations to add
     *
     * @return This builder instance to allow call chaining
     */
    public B addMixIn(Class<?> target, Class<?> mixinSource)
    {
        mixInHandler().addLocalDefinition(target, mixinSource);
        return _this();
    }

    /**
     * Method that allows making sure that specified {@code target} class
     * does not have associated mix-in annotations: basically can be used
     * to undo an earlier call to {@link #addMixIn}.
     *<p>
     * NOTE: removing mix-ins for given class does not try to remove possible
     * mix-ins for any of its super classes and super interfaces; only direct
     * mix-in addition, if any, is removed.
     *
     * @param target Target class for which no mix-ins should remain after call
     *
     * @return This builder instance to allow call chaining
     */
    public B removeMixIn(Class<?> target)
    {
        mixInHandler().addLocalDefinition(target, null);
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
     *  activateDefaultTyping(DefaultTyping.OBJECT_AND_NON_CONCRETE);
     *</pre>
     *<p>
     * NOTE: choice of {@link PolymorphicTypeValidator} to configure is of
     * crucial importance to security when deserializing untrusted content:
     * this because allowing deserializing of any type can lead to malicious
     * attacks using "deserialization gadgets". Implementations should use
     * allow-listing to specify acceptable types unless source of content
     * is fully trusted to only send safe types.
     */
    public B activateDefaultTyping(PolymorphicTypeValidator subtypeValidator) {
        return activateDefaultTyping(subtypeValidator, DefaultTyping.OBJECT_AND_NON_CONCRETE);
    }

    /**
     * Convenience method that is equivalent to calling
     *<pre>
     *  activateDefaultTyping(dti, JsonTypeInfo.As.WRAPPER_ARRAY);
     *</pre>
     *<p>
     * NOTE: choice of {@link PolymorphicTypeValidator} to configure is of
     * crucial importance to security when deserializing untrusted content:
     * this because allowing deserializing of any type can lead to malicious
     * attacks using "deserialization gadgets". Implementations should use
     * allow-listing to specify acceptable types unless source of content
     * is fully trusted to only send safe types.
     */
    public B activateDefaultTyping(PolymorphicTypeValidator subtypeValidator,
            DefaultTyping dti) {
        return activateDefaultTyping(subtypeValidator,
                dti, JsonTypeInfo.As.WRAPPER_ARRAY);
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
     * NOTE: choice of {@link PolymorphicTypeValidator} to configure is of
     * crucial importance to security when deserializing untrusted content:
     * this because allowing deserializing of any type can lead to malicious
     * attacks using "deserialization gadgets". Implementations should use
     * allow-listing to specify acceptable types unless source of content
     * is fully trusted to only send safe types.
     *
     * @param applicability Defines kinds of types for which additional type information
     *    is added; see {@link DefaultTyping} for more information.
     */
    public B activateDefaultTyping(PolymorphicTypeValidator subtypeValidator,
            DefaultTyping applicability, JsonTypeInfo.As includeAs)
    {
        // Use if "As.EXTERNAL_PROPERTY" will not work, check to ensure no attempts made
        if (includeAs == JsonTypeInfo.As.EXTERNAL_PROPERTY) {
            throw new IllegalArgumentException("Cannot use includeAs of "+includeAs+" for Default Typing");
        }
        return setDefaultTyping(_defaultDefaultTypingResolver(subtypeValidator,
                applicability, includeAs));
    }

    /**
     * Method for enabling automatic inclusion of type information -- needed
     * for proper deserialization of polymorphic types (unless types
     * have been annotated with {@link com.fasterxml.jackson.annotation.JsonTypeInfo}) --
     * using "As.PROPERTY" inclusion mechanism and specified property name
     * to use for inclusion (default being "@class" since default type information
     * always uses class name as type identifier)
     *<p>
     * NOTE: choice of {@link PolymorphicTypeValidator} to configure is of
     * crucial importance to security when deserializing untrusted content:
     * this because allowing deserializing of any type can lead to malicious
     * attacks using "deserialization gadgets". Implementations should use
     * allow-listing to specify acceptable types unless source of content
     * is fully trusted to only send safe types.
     */
    public B activateDefaultTypingAsProperty(PolymorphicTypeValidator subtypeValidator,
            DefaultTyping applicability, String propertyName)
    {
        return setDefaultTyping(_defaultDefaultTypingResolver(subtypeValidator,
                applicability, propertyName));
    }

    /**
     * Method for disabling automatic inclusion of type information; if so, only
     * explicitly annotated types (ones with
     * {@link com.fasterxml.jackson.annotation.JsonTypeInfo}) will have
     * additional embedded type information.
     */
    public B deactivateDefaultTyping() {
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

    /**
     * Overridable method for changing default {@link TypeResolverBuilder} to construct
     * for "default typing".
     */
    protected TypeResolverBuilder<?> _defaultDefaultTypingResolver(PolymorphicTypeValidator subtypeValidator,
            DefaultTyping applicability, JsonTypeInfo.As includeAs) {
        return new DefaultTypeResolverBuilder(subtypeValidator, applicability, includeAs);
    }

    /**
     * Overridable method for changing default {@link TypeResolverBuilder} to construct
     * for "default typing".
     */
    protected TypeResolverBuilder<?> _defaultDefaultTypingResolver(PolymorphicTypeValidator subtypeValidator,
            DefaultTyping applicability, String propertyName) {
        return new DefaultTypeResolverBuilder(subtypeValidator, applicability, propertyName);
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
