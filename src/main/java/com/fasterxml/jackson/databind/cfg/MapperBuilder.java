package com.fasterxml.jackson.databind.cfg;

import java.text.DateFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.function.Consumer;
import java.util.function.Function;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.*;
import com.fasterxml.jackson.databind.introspect.BasicClassIntrospector;
import com.fasterxml.jackson.databind.introspect.ClassIntrospector;
import com.fasterxml.jackson.databind.introspect.SimpleMixInResolver;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.databind.jsontype.SubtypeResolver;
import com.fasterxml.jackson.databind.jsontype.impl.StdSubtypeResolver;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.ser.*;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.RootNameLookup;

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

    protected final static BaseSettings DEFAULT_BASE_SETTINGS = BaseSettings.std();

    /*
    /**********************************************************
    /* Basic settings
    /**********************************************************
     */

    protected BaseSettings _baseSettings;

    /**
     * Underlying stream factory
     */
    protected final TokenStreamFactory _streamFactory;

    protected final ConfigOverrides _configOverrides;
    
    /*
    /**********************************************************
    /* Handlers, introspection
    /**********************************************************
     */

    /**
     * Introspector used to figure out Bean properties needed for bean serialization
     * and deserialization. Overridable so that it is possible to change low-level
     * details of introspection, like adding new annotation types.
     */
    protected ClassIntrospector _classIntrospector;

    protected SubtypeResolver _subtypeResolver;

    /*
    /**********************************************************
    /* Factories for serialization
    /**********************************************************
     */

    protected SerializerFactory _serializerFactory;

    /**
     * Prototype {@link SerializerProvider} to use for creating per-operation providers.
     */
    protected DefaultSerializerProvider _serializerProvider;

    protected FilterProvider _filterProvider;

    protected PrettyPrinter _defaultPrettyPrinter;

    /*
    /**********************************************************
    /* Factories for deserialization
    /**********************************************************
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

    /*
    /**********************************************************
    /* Feature flags: ser, deser
    /**********************************************************
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
    /**********************************************************
    /* Feature flags: generation, parsing
    /**********************************************************
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
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    protected MapperBuilder(TokenStreamFactory streamFactory)
    {
        _streamFactory = streamFactory;
        _baseSettings = DEFAULT_BASE_SETTINGS;
        _configOverrides = new ConfigOverrides();

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

        _classIntrospector = null;
        _subtypeResolver = null;

        _serializerFactory = BeanSerializerFactory.instance;
        _serializerProvider = null;
        _filterProvider = null;

        _deserializerFactory = BeanDeserializerFactory.instance;
        _deserializationContext = null;
        _injectableValues = null;
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

        _classIntrospector = base._classIntrospector;
        _subtypeResolver = base._subtypeResolver;

        _serializerFactory = base._serializerFactory;
        _serializerProvider = base._serializerProvider;
        _filterProvider = base._filterProvider;

        _deserializerFactory = base._deserializerFactory;
        _deserializationContext = base._deserializationContext;
        _injectableValues = base._injectableValues;
    }

    /*
    /**********************************************************
    /* Build methods
    /**********************************************************
     */

    /**
     * Method to call to create an initialize actual mapper instance
     */
    public abstract M build();

    public SerializationConfig buildSerializationConfig(SimpleMixInResolver mixins,
            RootNameLookup rootNames)
    {
        return new SerializationConfig(this,
                _mapperFeatures, _serFeatures, _generatorFeatures, _formatGeneratorFeatures,
                mixins, rootNames, _configOverrides);
    }

    public DeserializationConfig buildDeserializationConfig(SimpleMixInResolver mixins,
            RootNameLookup rootNames)
    {
        return new DeserializationConfig(this,
                _mapperFeatures, _deserFeatures, _parserFeatures, _formatParserFeatures,
                mixins, rootNames, _configOverrides);
    }

    /*
    /**********************************************************
    /* Accessors, general
    /**********************************************************
     */

    public BaseSettings baseSettings() {
        return _baseSettings;
    }

    public TokenStreamFactory streamFactory() {
        return _streamFactory;
    }

    public TypeFactory typeFactory() {
        return _baseSettings.getTypeFactory();
    }

    public AnnotationIntrospector annotationIntrospector() {
        return _baseSettings.getAnnotationIntrospector();
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

    /*
    /**********************************************************
    /* Accessors, serialization
    /**********************************************************
     */

    public SerializerFactory serializerFactory() {
        return _serializerFactory;
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
    /**********************************************************
    /* Accessors, deserialization
    /**********************************************************
     */

    public DeserializerFactory deserializerFactory() {
        return _deserializerFactory;
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

    /*
    /**********************************************************
    /* Changing features: mapper, ser, deser
    /**********************************************************
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
    /**********************************************************
    /* Changing features: parser, generator
    /**********************************************************
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
    /**********************************************************
    /* Changing settings, config overrides
    /**********************************************************
     */

    /**
     * Method for changing config overrides for specific type, through
     * callback to specific handler.
     */
    public B withConfigOverrides(Class<?> forType,
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
    public B changeDefaultVisibility(Function<VisibilityChecker<?>,VisibilityChecker<?>> handler) {
        VisibilityChecker<?> oldV = _configOverrides.getDefaultVisibility();
        VisibilityChecker<?> newV = handler.apply(oldV);
        if (newV != oldV) {
            Objects.requireNonNull(newV, "Can not assign null default VisibilityChecker");
            _configOverrides.setDefaultVisibility(newV);
        }
        return _this();
    }

    /*
    /**********************************************************
    /* Changing factories/handlers, general
    /**********************************************************
     */

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

    public B typeFactory(TypeFactory f) {
        _baseSettings = _baseSettings.with(f);
        return _this();
    }

    public B nodeFactory(JsonNodeFactory f) {
        _baseSettings = _baseSettings.with(f);
        return _this();
    }

    public B classIntrospector(ClassIntrospector ci) {
        _classIntrospector = ci;
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

    public B subtypeResolver(SubtypeResolver r) {
        _subtypeResolver = r;
        return _this();
    }

    public B propertyNamingStrategy(PropertyNamingStrategy s) {
        _baseSettings = _baseSettings.with(s);
        return _this();
    }

    /*
    /**********************************************************
    /* Changing factories, serialization
    /**********************************************************
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
    /**********************************************************
    /* Changing factories, deserialization
    /**********************************************************
     */

    public B deserializerFactory(DeserializerFactory f) {
        _deserializerFactory = f;
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

    /*
    /**********************************************************
    /* Changing settings, date/time
    /**********************************************************
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
    /**********************************************************
    /* Changing settings, formatting
    /**********************************************************
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
    /**********************************************************
    /* Other helper methods
    /**********************************************************
     */

    // silly convenience cast method we need
    @SuppressWarnings("unchecked")
    protected final B _this() { return (B) this; }
}
