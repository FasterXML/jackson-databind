package com.fasterxml.jackson.databind;

import java.text.DateFormat;
import java.util.Locale;
import java.util.TimeZone;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.cfg.BaseSettings;
import com.fasterxml.jackson.databind.cfg.ConfigOverrides;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.deser.*;
import com.fasterxml.jackson.databind.introspect.BasicClassIntrospector;
import com.fasterxml.jackson.databind.introspect.ClassIntrospector;
import com.fasterxml.jackson.databind.introspect.SimpleMixInResolver;
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

    /*
    /**********************************************************
    /* Basic settings
    /**********************************************************
     */

    protected BaseSettings _baseSettings;

    /*
    /**********************************************************
    /* Factories for framework itself, general
    /**********************************************************
     */

    /**
     * Underlying stream factory
     */
    protected final TokenStreamFactory _streamFactory;

    
    /**
     * Introspector used to figure out Bean properties needed for bean serialization
     * and deserialization. Overridable so that it is possible to change low-level
     * details of introspection, like adding new annotation types.
     */
    protected ClassIntrospector _classIntrospector;

    protected SubtypeResolver _subtypeResolver;

    /*
    /**********************************************************
    /* Factories for framework itself, serialization
    /**********************************************************
     */
    
    protected SerializerFactory _serializerFactory;

    /**
     * Prototype {@link SerializerProvider} to use for creating per-operation providers.
     */
    protected DefaultSerializerProvider _serializerProvider;

    /*
    /**********************************************************
    /* Factories for framework itself, deserialization
    /**********************************************************
     */

    protected DeserializerFactory _deserializerFactory;
    
    /**
     * Prototype (about same as factory) to use for creating per-operation contexts.
     */
    protected DefaultDeserializationContext _deserializationContext;

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
     * States of {@link com.fasterxml.jackson.core.JsonGenerator.Feature}s to enable/disable.
     */
    protected int _generatorFeatures;

    /**
     * Bitflag of {@link com.fasterxml.jackson.core.JsonGenerator.Feature}s to enable/disable
     */
    protected int _generatorFeaturesToChange;

    /**
     * States of {@link com.fasterxml.jackson.core.FormatFeature}s to enable/disable.
     */
    protected int _formatWriteFeatures;

    /**
     * Bitflag of {@link com.fasterxml.jackson.core.FormatFeature}s to enable/disable
     */
    protected int _formatWriteFeaturesToChange;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    protected MapperBuilder(TokenStreamFactory streamFactory)
    {
        _baseSettings = BaseSettings.std();

        _mapperFeatures = DEFAULT_MAPPER_FEATURES;
        _serFeatures = DEFAULT_SER_FEATURES;
        _deserFeatures = DEFAULT_DESER_FEATURES;

        _streamFactory = streamFactory;

        _classIntrospector = null;
        _subtypeResolver = null;

        _serializerFactory = BeanSerializerFactory.instance;
        _serializerProvider = null;

        _deserializerFactory = BeanDeserializerFactory.instance;
        _deserializationContext = null;
        //        _mapperFeatures = MapperFeature;
    }

    protected MapperBuilder(MapperBuilder<?,?> base)
    {
        _baseSettings = base._baseSettings;

        _mapperFeatures = base._mapperFeatures;
        _serFeatures = base._serFeatures;
        _deserFeatures = base._deserFeatures;

        _streamFactory = base._streamFactory;

        _classIntrospector = base._classIntrospector;
        _subtypeResolver = base._subtypeResolver;

        _serializerFactory = base._serializerFactory;
        _serializerProvider = base._serializerProvider;

        _deserializerFactory = base._deserializerFactory;
        _deserializationContext = base._deserializationContext;
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
            RootNameLookup rootNames, ConfigOverrides configOverrides)
    {
        return new SerializationConfig(this, _mapperFeatures, _serFeatures,
                mixins, rootNames, configOverrides);
    }

    public DeserializationConfig buildDeserializationConfig(SimpleMixInResolver mixins,
            RootNameLookup rootNames, ConfigOverrides configOverrides)
    {
        return new DeserializationConfig(this, _mapperFeatures, _deserFeatures,
                mixins, rootNames, configOverrides);
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

    /*
    /**********************************************************
    /* Accessors, deserialization
    /**********************************************************
     */

    public DeserializerFactory deserializerFactory() {
        return _deserializerFactory;
    }

    protected DefaultDeserializationContext deserializationContext() {
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
            _serFeatures |= feature.getMask();
        } else {
            _serFeatures &= ~feature.getMask();
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
    /* Changing factories, general
    /**********************************************************
     */

    public B typeFactory(TypeFactory f) {
        _baseSettings = _baseSettings.with(f);
        return _this();
    }

    protected B nodeFactory(JsonNodeFactory f) {
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

    /*
    /**********************************************************
    /* Changing factories, deserialization
    /**********************************************************
     */

    public B deserializerFactory(DeserializerFactory f) {
        _deserializerFactory = f;
        return _this();
    }

    protected B deserializationContext(DefaultDeserializationContext ctxt) {
        _deserializationContext = ctxt;
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
