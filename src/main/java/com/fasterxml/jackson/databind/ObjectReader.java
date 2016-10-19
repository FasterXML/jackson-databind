package com.fasterxml.jackson.databind;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.filter.FilteringParserDelegate;
import com.fasterxml.jackson.core.filter.JsonPointerBasedFilter;
import com.fasterxml.jackson.core.filter.TokenFilter;
import com.fasterxml.jackson.core.type.ResolvedType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.cfg.ContextAttributes;
import com.fasterxml.jackson.databind.deser.DataFormatReaders;
import com.fasterxml.jackson.databind.deser.DefaultDeserializationContext;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;
import com.fasterxml.jackson.databind.type.SimpleType;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * Builder object that can be used for per-serialization configuration of
 * deserialization parameters, such as root type to use or object
 * to update (instead of constructing new instance).
 *<p>
 * Uses "mutant factory" pattern so that instances are immutable
 * (and thus fully thread-safe with no external synchronization);
 * new instances are constructed for different configurations.
 * Instances are initially constructed by {@link ObjectMapper} and can be
 * reused, shared, cached; both because of thread-safety and because
 * instances are relatively light-weight.
 */
public class ObjectReader
    extends ObjectCodec
    implements Versioned, java.io.Serializable // since 2.1
{
    private static final long serialVersionUID = 1L; // since 2.5

    private final static JavaType JSON_NODE_TYPE = SimpleType.constructUnsafe(JsonNode.class);

    /*
    /**********************************************************
    /* Immutable configuration from ObjectMapper
    /**********************************************************
     */

    /**
     * General serialization configuration settings; while immutable,
     * can use copy-constructor to create modified instances as necessary.
     */
    protected final DeserializationConfig _config;

    /**
     * Blueprint instance of deserialization context; used for creating
     * actual instance when needed.
     */
    protected final DefaultDeserializationContext _context;

    /**
     * Factory used for constructing {@link JsonGenerator}s
     */
    protected final JsonFactory _parserFactory;

    /**
     * Flag that indicates whether root values are expected to be unwrapped or not
     */
    protected final boolean _unwrapRoot;

    /**
     * Filter to be consider for JsonParser.  
     * Default value to be null as filter not considered.
     */
    private final TokenFilter _filter;
    
    /*
    /**********************************************************
    /* Configuration that can be changed during building
    /**********************************************************
     */

    /**
     * Declared type of value to instantiate during deserialization.
     * Defines which deserializer to use; as well as base type of instance
     * to construct if an updatable value is not configured to be used
     * (subject to changes by embedded type information, for polymorphic
     * types). If {@link #_valueToUpdate} is non-null, only used for
     * locating deserializer.
     */
    protected final JavaType _valueType;

    /**
     * We may pre-fetch deserializer as soon as {@link #_valueType}
     * is known, and if so, reuse it afterwards.
     * This allows avoiding further deserializer lookups and increases
     * performance a bit on cases where readers are reused.
     * 
     * @since 2.1
     */
    protected final JsonDeserializer<Object> _rootDeserializer;
    
    /**
     * Instance to update with data binding; if any. If null,
     * a new instance is created, if non-null, properties of
     * this value object will be updated instead.
     * Note that value can be of almost any type, except not
     * {@link com.fasterxml.jackson.databind.type.ArrayType}; array
     * types can not be modified because array size is immutable.
     */
    protected final Object _valueToUpdate;

    /**
     * When using data format that uses a schema, schema is passed
     * to parser.
     */
    protected final FormatSchema _schema;

    /**
     * Values that can be injected during deserialization, if any.
     */
    protected final InjectableValues _injectableValues;

    /**
     * Optional detector used for auto-detecting data format that byte-based
     * input uses.
     *<p>
     * NOTE: If defined non-null, <code>readValue()</code> methods that take
     * {@link Reader} or {@link String} input <b>will fail with exception</b>,
     * because format-detection only works on byte-sources. Also, if format
     * can not be detect reliably (as per detector settings),
     * a {@link JsonParseException} will be thrown).
     * 
     * @since 2.1
     */
    protected final DataFormatReaders _dataFormatReaders;

    /*
    /**********************************************************
    /* Caching
    /**********************************************************
     */

    /**
     * Root-level cached deserializers.
     * Passed by {@link ObjectMapper}, shared with it.
     */
    final protected ConcurrentHashMap<JavaType, JsonDeserializer<Object>> _rootDeserializers;

    /*
    /**********************************************************
    /* Life-cycle, construction
    /**********************************************************
     */

    /**
     * Constructor used by {@link ObjectMapper} for initial instantiation
     */
    protected ObjectReader(ObjectMapper mapper, DeserializationConfig config) {
        this(mapper, config, null, null, null, null);
    }

    /**
     * Constructor called when a root deserializer should be fetched based
     * on other configuration.
     */
    protected ObjectReader(ObjectMapper mapper, DeserializationConfig config,
            JavaType valueType, Object valueToUpdate,
            FormatSchema schema, InjectableValues injectableValues)
    {
        _config = config;
        _context = mapper._deserializationContext;
        _rootDeserializers = mapper._rootDeserializers;
        _parserFactory = mapper._jsonFactory;
        _valueType = valueType;
        _valueToUpdate = valueToUpdate;
        if (valueToUpdate != null && valueType.isArrayType()) {
            throw new IllegalArgumentException("Can not update an array value");
        }
        _schema = schema;
        _injectableValues = injectableValues;
        _unwrapRoot = config.useRootWrapping();

        _rootDeserializer = _prefetchRootDeserializer(valueType);
        _dataFormatReaders = null;        
        _filter = null;
    }
    
    /**
     * Copy constructor used for building variations.
     */
    protected ObjectReader(ObjectReader base, DeserializationConfig config,
            JavaType valueType, JsonDeserializer<Object> rootDeser, Object valueToUpdate,
            FormatSchema schema, InjectableValues injectableValues,
            DataFormatReaders dataFormatReaders)
    {
        _config = config;
        _context = base._context;

        _rootDeserializers = base._rootDeserializers;
        _parserFactory = base._parserFactory;

        _valueType = valueType;
        _rootDeserializer = rootDeser;
        _valueToUpdate = valueToUpdate;
        if (valueToUpdate != null && valueType.isArrayType()) {
            throw new IllegalArgumentException("Can not update an array value");
        }
        _schema = schema;
        _injectableValues = injectableValues;
        _unwrapRoot = config.useRootWrapping();
        _dataFormatReaders = dataFormatReaders;
        _filter = base._filter;
    }

    /**
     * Copy constructor used when modifying simple feature flags
     */
    protected ObjectReader(ObjectReader base, DeserializationConfig config)
    {
        _config = config;
        _context = base._context;

        _rootDeserializers = base._rootDeserializers;
        _parserFactory = base._parserFactory;

        _valueType = base._valueType;
        _rootDeserializer = base._rootDeserializer;
        _valueToUpdate = base._valueToUpdate;
        _schema = base._schema;
        _injectableValues = base._injectableValues;
        _unwrapRoot = config.useRootWrapping();
        _dataFormatReaders = base._dataFormatReaders;
        _filter = base._filter;
    }
    
    protected ObjectReader(ObjectReader base, JsonFactory f)
    {
        // may need to override ordering, based on data format capabilities
        _config = base._config
            .with(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, f.requiresPropertyOrdering());
        _context = base._context;

        _rootDeserializers = base._rootDeserializers;
        _parserFactory = f;

        _valueType = base._valueType;
        _rootDeserializer = base._rootDeserializer;
        _valueToUpdate = base._valueToUpdate;
        _schema = base._schema;
        _injectableValues = base._injectableValues;
        _unwrapRoot = base._unwrapRoot;
        _dataFormatReaders = base._dataFormatReaders;
        _filter = base._filter;
    }
    
    protected ObjectReader(ObjectReader base, TokenFilter filter) {
        _config = base._config;
        _context = base._context;
        _rootDeserializers = base._rootDeserializers;
        _parserFactory = base._parserFactory;
        _valueType = base._valueType;
        _rootDeserializer = base._rootDeserializer;
        _valueToUpdate = base._valueToUpdate;
        _schema = base._schema;
        _injectableValues = base._injectableValues;
        _unwrapRoot = base._unwrapRoot;
        _dataFormatReaders = base._dataFormatReaders;
        _filter = filter;
    }
    
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
    /* Methods sub-classes MUST override, used for constructing
    /* reader instances, (re)configuring parser instances
    /* Added in 2.5
    /**********************************************************
     */

    /**
     * Overridable factory method called by various "withXxx()" methods
     * 
     * @since 2.5
     */
    protected ObjectReader _new(ObjectReader base, JsonFactory f) {
        return new ObjectReader(base, f);
    }

    /**
     * Overridable factory method called by various "withXxx()" methods
     * 
     * @since 2.5
     */
    protected ObjectReader _new(ObjectReader base, DeserializationConfig config) {
        return new ObjectReader(base, config);
    }

    /**
     * Overridable factory method called by various "withXxx()" methods
     * 
     * @since 2.5
     */
    protected ObjectReader _new(ObjectReader base, DeserializationConfig config,
            JavaType valueType, JsonDeserializer<Object> rootDeser, Object valueToUpdate,
            FormatSchema schema, InjectableValues injectableValues,
            DataFormatReaders dataFormatReaders) {
        return new ObjectReader(base, config, valueType, rootDeser,  valueToUpdate,
                 schema,  injectableValues, dataFormatReaders);
    }

    /**
     * Factory method used to create {@link MappingIterator} instances;
     * either default, or custom subtype.
     * 
     * @since 2.5
     */
    protected <T> MappingIterator<T> _newIterator(JsonParser p, DeserializationContext ctxt,
            JsonDeserializer<?> deser, boolean parserManaged)
    {
        return new MappingIterator<T>(_valueType, p, ctxt,
                deser, parserManaged, _valueToUpdate);
    }

    /*
    /**********************************************************
    /* Methods sub-classes may choose to override, if customized
    /* initialization is needed.
    /**********************************************************
     */

    protected JsonToken _initForReading(DeserializationContext ctxt, JsonParser p)
        throws IOException
    {
        if (_schema != null) {
            p.setSchema(_schema);
        }
        _config.initialize(p); // since 2.5

        /* First: must point to a token; if not pointing to one, advance.
         * This occurs before first read from JsonParser, as well as
         * after clearing of current token.
         */
        JsonToken t = p.getCurrentToken();
        if (t == null) { // and then we must get something...
            t = p.nextToken();
            if (t == null) {
                // Throw mapping exception, since it's failure to map, not an actual parsing problem
                ctxt.reportMissingContent(null); // default msg is fine
            }
        }
        return t;
    }

    /**
     * Alternative to {@link #_initForReading} used in cases where reading
     * of multiple values means that we may or may not want to advance the stream,
     * but need to do other initialization.
     *<p>
     * Base implementation only sets configured {@link FormatSchema}, if any, on parser.
     * 
     * @since 2.8
     */
    protected void _initForMultiRead(DeserializationContext ctxt, JsonParser p)
        throws IOException
    {
        if (_schema != null) {
            p.setSchema(_schema);
        }
        _config.initialize(p);
    }

    /*
    /**********************************************************
    /* Life-cycle, fluent factory methods for DeserializationFeatures
    /**********************************************************
     */

    /**
     * Method for constructing a new reader instance that is configured
     * with specified feature enabled.
     */
    public ObjectReader with(DeserializationFeature feature) {
        return _with(_config.with(feature));
    }

    /**
     * Method for constructing a new reader instance that is configured
     * with specified features enabled.
     */
    public ObjectReader with(DeserializationFeature first,
            DeserializationFeature... other)
    {
        return _with(_config.with(first, other));
    }    

    /**
     * Method for constructing a new reader instance that is configured
     * with specified features enabled.
     */
    public ObjectReader withFeatures(DeserializationFeature... features) {
        return _with(_config.withFeatures(features));
    }    

    /**
     * Method for constructing a new reader instance that is configured
     * with specified feature disabled.
     */
    public ObjectReader without(DeserializationFeature feature) {
        return _with(_config.without(feature)); 
    }

    /**
     * Method for constructing a new reader instance that is configured
     * with specified features disabled.
     */
    public ObjectReader without(DeserializationFeature first,
            DeserializationFeature... other) {
        return _with(_config.without(first, other));
    }    

    /**
     * Method for constructing a new reader instance that is configured
     * with specified features disabled.
     */
    public ObjectReader withoutFeatures(DeserializationFeature... features) {
        return _with(_config.withoutFeatures(features));
    }    

    /*
    /**********************************************************
    /* Life-cycle, fluent factory methods for JsonParser.Features
    /**********************************************************
     */

    /**
     * Method for constructing a new reader instance that is configured
     * with specified feature enabled.
     */
    public ObjectReader with(JsonParser.Feature feature) {
        return _with(_config.with(feature));
    }

    /**
     * Method for constructing a new reader instance that is configured
     * with specified features enabled.
     */
    public ObjectReader withFeatures(JsonParser.Feature... features) {
        return _with(_config.withFeatures(features));
    }    

    /**
     * Method for constructing a new reader instance that is configured
     * with specified feature disabled.
     */
    public ObjectReader without(JsonParser.Feature feature) {
        return _with(_config.without(feature)); 
    }

    /**
     * Method for constructing a new reader instance that is configured
     * with specified features disabled.
     */
    public ObjectReader withoutFeatures(JsonParser.Feature... features) {
        return _with(_config.withoutFeatures(features));
    }

    /*
    /**********************************************************
    /* Life-cycle, fluent factory methods for FormatFeature (2.7)
    /**********************************************************
     */

    /**
     * Method for constructing a new reader instance that is configured
     * with specified feature enabled.
     *
     * @since 2.7
     */
    public ObjectReader with(FormatFeature feature) {
        return _with(_config.with(feature));
    }

    /**
     * Method for constructing a new reader instance that is configured
     * with specified features enabled.
     *
     * @since 2.7
     */
    public ObjectReader withFeatures(FormatFeature... features) {
        return _with(_config.withFeatures(features));
    }    

    /**
     * Method for constructing a new reader instance that is configured
     * with specified feature disabled.
     *
     * @since 2.7
     */
    public ObjectReader without(FormatFeature feature) {
        return _with(_config.without(feature)); 
    }

    /**
     * Method for constructing a new reader instance that is configured
     * with specified features disabled.
     *
     * @since 2.7
     */
    public ObjectReader withoutFeatures(FormatFeature... features) {
        return _with(_config.withoutFeatures(features));
    }
    
    /*
    /**********************************************************
    /* Life-cycle, fluent factory methods, other
    /**********************************************************
     */

    /**
     * Convenience method to bind from {@link JsonPointer}.  
     * {@link JsonPointerBasedFilter} is registered and will be used for parsing later. 
     * @since 2.6
     */
    public ObjectReader at(final String value) {
        return new ObjectReader(this, new JsonPointerBasedFilter(value));
    }

    /**
     * Convenience method to bind from {@link JsonPointer}
      * {@link JsonPointerBasedFilter} is registered and will be used for parsing later.
     * @since 2.6
     */
    public ObjectReader at(final JsonPointer pointer) {
        return new ObjectReader(this, new JsonPointerBasedFilter(pointer));
    }

    /**
     * Mutant factory method that will construct a new instance that has
     * specified underlying {@link DeserializationConfig}.
     *<p>
     * NOTE: use of this method is not recommended, as there are many other
     * re-configuration methods available.
     */
    public ObjectReader with(DeserializationConfig config) {
        return _with(config);
    }    

    /**
     * Method for constructing a new instance with configuration that uses
     * passed {@link InjectableValues} to provide injectable values.
     *<p>
     * Note that the method does NOT change state of this reader, but
     * rather construct and returns a newly configured instance.
     */
    public ObjectReader with(InjectableValues injectableValues)
    {
        if (_injectableValues == injectableValues) {
            return this;
        }
        return _new(this, _config,
                _valueType, _rootDeserializer, _valueToUpdate,
                _schema, injectableValues, _dataFormatReaders);
    }

    /**
     * Method for constructing a new reader instance with configuration that uses
     * passed {@link JsonNodeFactory} for constructing {@link JsonNode}
     * instances.
     *<p>
     * Note that the method does NOT change state of this reader, but
     * rather construct and returns a newly configured instance.
     */
    public ObjectReader with(JsonNodeFactory f) {
        return _with(_config.with(f));
    }

    /**
     * Method for constructing a new reader instance with configuration that uses
     * passed {@link JsonFactory} for constructing underlying Readers.
     *<p>
     * NOTE: only factories that <b>DO NOT REQUIRE SPECIAL MAPPERS</b>
     * (that is, ones that return <code>false</code> for
     * {@link JsonFactory#requiresCustomCodec()}) can be used: trying
     * to use one that requires custom codec will throw exception
     * 
     * @since 2.1
     */
    public ObjectReader with(JsonFactory f) {
        if (f == _parserFactory) {
            return this;
        }
        ObjectReader r = _new(this, f);
        // Also, try re-linking, if possible...
        if (f.getCodec() == null) {
            f.setCodec(r);
        }
        return r;
    }
    
    /**
     * Method for constructing a new instance with configuration that
     * specifies what root name to expect for "root name unwrapping".
     * See {@link DeserializationConfig#withRootName(String)} for
     * details.
     *<p>
     * Note that the method does NOT change state of this reader, but
     * rather construct and returns a newly configured instance.
     */
    public ObjectReader withRootName(String rootName) {
        return _with(_config.withRootName(rootName));
    }

    /**
     * @since 2.6
     */
    public ObjectReader withRootName(PropertyName rootName) {
        return _with(_config.withRootName(rootName));
    }
    
    /**
     * Convenience method that is same as calling:
     *<code>
     *   withRootName("")
     *</code>
     * which will forcibly prevent use of root name wrapping when writing
     * values with this {@link ObjectReader}.
     * 
     * @since 2.6
     */
    public ObjectReader withoutRootName() {
        return _with(_config.withRootName(PropertyName.NO_NAME));
    }
    
    /**
     * Method for constructing a new instance with configuration that
     * passes specified {@link FormatSchema} to {@link JsonParser} that
     * is constructed for parsing content.
     *<p>
     * Note that the method does NOT change state of this reader, but
     * rather construct and returns a newly configured instance.
     */
    public ObjectReader with(FormatSchema schema)
    {
        if (_schema == schema) {
            return this;
        }
        _verifySchemaType(schema);
        return _new(this, _config, _valueType, _rootDeserializer, _valueToUpdate,
                schema, _injectableValues, _dataFormatReaders);
    }

    /**
     * Method for constructing a new reader instance that is configured
     * to data bind into specified type.
     *<p>
     * Note that the method does NOT change state of this reader, but
     * rather construct and returns a newly configured instance.
     * 
     * @since 2.5
     */
    public ObjectReader forType(JavaType valueType)
    {
        if (valueType != null && valueType.equals(_valueType)) {
            return this;
        }
        JsonDeserializer<Object> rootDeser = _prefetchRootDeserializer(valueType);
        // type is stored here, no need to make a copy of config
        DataFormatReaders det = _dataFormatReaders;
        if (det != null) {
            det = det.withType(valueType);
        }
        return _new(this, _config, valueType, rootDeser,
                _valueToUpdate, _schema, _injectableValues, det);
    }    

    /**
     * Method for constructing a new reader instance that is configured
     * to data bind into specified type.
     *<p>
     * Note that the method does NOT change state of this reader, but
     * rather construct and returns a newly configured instance.
     *
     * @since 2.5
     */
    public ObjectReader forType(Class<?> valueType) {
        return forType(_config.constructType(valueType));
    }    

    /**
     * Method for constructing a new reader instance that is configured
     * to data bind into specified type.
     *<p>
     * Note that the method does NOT change state of this reader, but
     * rather construct and returns a newly configured instance.
     *
     * @since 2.5
     */
    public ObjectReader forType(TypeReference<?> valueTypeRef) {
        return forType(_config.getTypeFactory().constructType(valueTypeRef.getType()));
    }    

    /**
     * @deprecated since 2.5 Use {@link #forType(JavaType)} instead
     */
    @Deprecated
    public ObjectReader withType(JavaType valueType) {
        return forType(valueType);
    }

    /**
     * @deprecated since 2.5 Use {@link #forType(Class)} instead
     */
    @Deprecated
    public ObjectReader withType(Class<?> valueType) {
        return forType(_config.constructType(valueType));
    }    

    /**
     * @deprecated since 2.5 Use {@link #forType(Class)} instead
     */
    @Deprecated
    public ObjectReader withType(java.lang.reflect.Type valueType) {
        return forType(_config.getTypeFactory().constructType(valueType));
    }

    /**
     * @deprecated since 2.5 Use {@link #forType(TypeReference)} instead
     */
    @Deprecated
    public ObjectReader withType(TypeReference<?> valueTypeRef) {
        return forType(_config.getTypeFactory().constructType(valueTypeRef.getType()));
    }    

    /**
     * Method for constructing a new instance with configuration that
     * updates passed Object (as root value), instead of constructing 
     * a new value.
     *<p>
     * Note that the method does NOT change state of this reader, but
     * rather construct and returns a newly configured instance.
     */
    public ObjectReader withValueToUpdate(Object value)
    {
        if (value == _valueToUpdate) return this;
        if (value == null) {
            // 18-Oct-2016, tatu: Actually, should be allowed, to remove value
            //   to update, if any
            return _new(this, _config, _valueType, _rootDeserializer, null,
                    _schema, _injectableValues, _dataFormatReaders);
        }
        JavaType t;
        
        /* no real benefit from pre-fetching, as updating readers are much
         * less likely to be reused, and value type may also be forced
         * with a later chained call...
         */
        if (_valueType == null) {
            t = _config.constructType(value.getClass());
        } else {
            t = _valueType;
        }
        return _new(this, _config, t, _rootDeserializer, value,
                _schema, _injectableValues, _dataFormatReaders);
    }

    /**
     * Method for constructing a new instance with configuration that
     * uses specified View for filtering.
     *<p>
     * Note that the method does NOT change state of this reader, but
     * rather construct and returns a newly configured instance.
     */
    public ObjectReader withView(Class<?> activeView) {
        return _with(_config.withView(activeView));
    }

    public ObjectReader with(Locale l) {
        return _with(_config.with(l));
    }

    public ObjectReader with(TimeZone tz) {
        return _with(_config.with(tz));
    }

    public ObjectReader withHandler(DeserializationProblemHandler h) {
        return _with(_config.withHandler(h));
    }

    public ObjectReader with(Base64Variant defaultBase64) {
        return _with(_config.with(defaultBase64));
    }

    /**
     * Fluent factory method for constructing a reader that will try to
     * auto-detect underlying data format, using specified list of
     * {@link JsonFactory} instances, and default {@link DataFormatReaders} settings
     * (for customized {@link DataFormatReaders}, you can construct instance yourself).
     * to construct appropriate {@link JsonParser} for actual parsing.
     *<p>
     * Note: since format detection only works with byte sources, it is possible to
     * get a failure from some 'readValue()' methods. Also, if input can not be reliably
     * (enough) detected as one of specified types, an exception will be thrown.
     *<p>
     * Note: not all {@link JsonFactory} types can be passed: specifically, ones that
     * require "custom codec" (like XML factory) will not work. Instead, use
     * method that takes {@link ObjectReader} instances instead of factories.
     * 
     * @param readers Data formats accepted, in decreasing order of priority (that is,
     *   matches checked in listed order, first match wins)
     * 
     * @return Newly configured writer instance
     * 
     * @since 2.1
     */
    public ObjectReader withFormatDetection(ObjectReader... readers) {
        return withFormatDetection(new DataFormatReaders(readers));
    }

    /**
     * Fluent factory method for constructing a reader that will try to
     * auto-detect underlying data format, using specified
     * {@link DataFormatReaders}.
     *<p>
     * NOTE: since format detection only works with byte sources, it is possible to
     * get a failure from some 'readValue()' methods. Also, if input can not be reliably
     * (enough) detected as one of specified types, an exception will be thrown.
     * 
     * @param readers DataFormatReaders to use for detecting underlying format.
     * 
     * @return Newly configured writer instance
     * 
     * @since 2.1
     */
    public ObjectReader withFormatDetection(DataFormatReaders readers) {
        return _new(this, _config, _valueType, _rootDeserializer, _valueToUpdate,
                _schema, _injectableValues, readers);
    }

    /**
     * @since 2.3
     */
    public ObjectReader with(ContextAttributes attrs) {
        return _with(_config.with(attrs));
    }

    /**
     * @since 2.3
     */
    public ObjectReader withAttributes(Map<?,?> attrs) {
        return _with(_config.withAttributes(attrs));
    }

    /**
     * @since 2.3
     */
    public ObjectReader withAttribute(Object key, Object value) {
        return _with( _config.withAttribute(key, value));
    }

    /**
     * @since 2.3
     */
    public ObjectReader withoutAttribute(Object key) {
        return _with(_config.withoutAttribute(key));
    }

    /*
    /**********************************************************
    /* Overridable factory methods may override
    /**********************************************************
     */
    
    protected ObjectReader _with(DeserializationConfig newConfig) {
        if (newConfig == _config) {
            return this;
        }
        ObjectReader r = _new(this, newConfig);
        if (_dataFormatReaders != null) {
            r  = r.withFormatDetection(_dataFormatReaders.with(newConfig));
        }
        return r;
    }
    
    /*
    /**********************************************************
    /* Simple accessors
    /**********************************************************
     */
    
    public boolean isEnabled(DeserializationFeature f) {
        return _config.isEnabled(f);
    }

    public boolean isEnabled(MapperFeature f) {
        return _config.isEnabled(f);
    }

    public boolean isEnabled(JsonParser.Feature f) {
        return _parserFactory.isEnabled(f);
    }

    /**
     * @since 2.2
     */
    public DeserializationConfig getConfig() {
        return _config;
    }
    
    /**
     * @since 2.1
     */
    @Override
    public JsonFactory getFactory() {
        return _parserFactory;
    }

    public TypeFactory getTypeFactory() {
        return _config.getTypeFactory();
    }

    /**
     * @since 2.3
     */
    public ContextAttributes getAttributes() {
        return _config.getAttributes();
    }

    /**
     * @since 2.6
     */
    public InjectableValues getInjectableValues() {
        return _injectableValues;
    }

    /*
    /**********************************************************
    /* Deserialization methods; basic ones to support ObjectCodec first
    /* (ones that take JsonParser)
    /**********************************************************
     */

    /**
     * Method that binds content read using given parser, using
     * configuration of this reader, including expected result type.
     * Value return is either newly constructed, or root value that
     * was specified with {@link #withValueToUpdate(Object)}.
     *<p>
     * NOTE: this method never tries to auto-detect format, since actual
     * (data-format specific) parser is given.
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(JsonParser p) throws IOException
    {
        return (T) _bind(p, _valueToUpdate);
    }

    /**
     * Convenience method that binds content read using given parser, using
     * configuration of this reader, except that expected value type
     * is specified with the call (instead of currently configured root type).
     * Value return is either newly constructed, or root value that
     * was specified with {@link #withValueToUpdate(Object)}.
     *<p>
     * NOTE: this method never tries to auto-detect format, since actual
     * (data-format specific) parser is given.
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T readValue(JsonParser p, Class<T> valueType) throws IOException
    {
        return (T) forType(valueType).readValue(p);
    }

    /**
     * Convenience method that binds content read using given parser, using
     * configuration of this reader, except that expected value type
     * is specified with the call (instead of currently configured root type).
     * Value return is either newly constructed, or root value that
     * was specified with {@link #withValueToUpdate(Object)}.
     *<p>
     * NOTE: this method never tries to auto-detect format, since actual
     * (data-format specific) parser is given.
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T readValue(JsonParser p, TypeReference<?> valueTypeRef) throws IOException
    {
        return (T) forType(valueTypeRef).readValue(p);
    }

    /**
     * Convenience method that binds content read using given parser, using
     * configuration of this reader, except that expected value type
     * is specified with the call (instead of currently configured root type).
     * Value return is either newly constructed, or root value that
     * was specified with {@link #withValueToUpdate(Object)}.
     *<p>
     * NOTE: this method never tries to auto-detect format, since actual
     * (data-format specific) parser is given.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T readValue(JsonParser p, ResolvedType valueType) throws IOException, JsonProcessingException {
        return (T) forType((JavaType)valueType).readValue(p);
    }

    /**
     * Type-safe overloaded method, basically alias for {@link #readValue(JsonParser, ResolvedType)}.
     *<p>
     * NOTE: this method never tries to auto-detect format, since actual
     * (data-format specific) parser is given.
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(JsonParser p, JavaType valueType) throws IOException {
        return (T) forType(valueType).readValue(p);
    }

    /**
     * Convenience method that is equivalent to:
     *<pre>
     *   withType(valueType).readValues(p);
     *</pre>
     *<p>
     * Method reads a sequence of Objects from parser stream.
     * Sequence can be either root-level "unwrapped" sequence (without surrounding
     * JSON array), or a sequence contained in a JSON Array.
     * In either case {@link JsonParser} <b>MUST</b> point to the first token of
     * the first element, OR not point to any token (in which case it is advanced
     * to the next token). This means, specifically, that for wrapped sequences,
     * parser MUST NOT point to the surrounding <code>START_ARRAY</code> (one that
     * contains values to read) but rather to the token following it which is the first
     * token of the first value to read.
     *<p>
     * NOTE: this method never tries to auto-detect format, since actual
     * (data-format specific) parser is given.
     */
    @Override
    public <T> Iterator<T> readValues(JsonParser p, Class<T> valueType) throws IOException {
        return forType(valueType).readValues(p);
    }

    /**
     * Convenience method that is equivalent to:
     *<pre>
     *   withType(valueTypeRef).readValues(p);
     *</pre>
     *<p>
     * Method reads a sequence of Objects from parser stream.
     * Sequence can be either root-level "unwrapped" sequence (without surrounding
     * JSON array), or a sequence contained in a JSON Array.
     * In either case {@link JsonParser} <b>MUST</b> point to the first token of
     * the first element, OR not point to any token (in which case it is advanced
     * to the next token). This means, specifically, that for wrapped sequences,
     * parser MUST NOT point to the surrounding <code>START_ARRAY</code> (one that
     * contains values to read) but rather to the token following it which is the first
     * token of the first value to read.
     *<p>
     * NOTE: this method never tries to auto-detect format, since actual
     * (data-format specific) parser is given.
     */
    @Override
    public <T> Iterator<T> readValues(JsonParser p, TypeReference<?> valueTypeRef) throws IOException {
        return forType(valueTypeRef).readValues(p);
    }
    
    /**
     * Convenience method that is equivalent to:
     *<pre>
     *   withType(valueType).readValues(p);
     *</pre>
     *<p>
     * Method reads a sequence of Objects from parser stream.
     * Sequence can be either root-level "unwrapped" sequence (without surrounding
     * JSON array), or a sequence contained in a JSON Array.
     * In either case {@link JsonParser} <b>MUST</b> point to the first token of
     * the first element, OR not point to any token (in which case it is advanced
     * to the next token). This means, specifically, that for wrapped sequences,
     * parser MUST NOT point to the surrounding <code>START_ARRAY</code> (one that
     * contains values to read) but rather to the token following it which is the first
     * token of the first value to read.
     *<p>
     * NOTE: this method never tries to auto-detect format, since actual
     * (data-format specific) parser is given.
     */
    @Override
    public <T> Iterator<T> readValues(JsonParser p, ResolvedType valueType) throws IOException {
        return readValues(p, (JavaType) valueType);
    }

    /**
     * Convenience method that is equivalent to:
     *<pre>
     *   withType(valueType).readValues(p);
     *</pre>
     *<p>
     * Method reads a sequence of Objects from parser stream.
     * Sequence can be either root-level "unwrapped" sequence (without surrounding
     * JSON array), or a sequence contained in a JSON Array.
     * In either case {@link JsonParser} <b>MUST</b> point to the first token of
     * the first element, OR not point to any token (in which case it is advanced
     * to the next token). This means, specifically, that for wrapped sequences,
     * parser MUST NOT point to the surrounding <code>START_ARRAY</code> (one that
     * contains values to read) but rather to the token following it which is the first
     * token of the first value to read.
     *<p>
     * NOTE: this method never tries to auto-detect format, since actual
     * (data-format specific) parser is given.
     */
    public <T> Iterator<T> readValues(JsonParser p, JavaType valueType) throws IOException {
        return forType(valueType).readValues(p);
    }

    /*
    /**********************************************************
    /* TreeCodec impl
    /**********************************************************
     */

    @Override
    public JsonNode createArrayNode() {
        return _config.getNodeFactory().arrayNode();
    }

    @Override
    public JsonNode createObjectNode() {
        return _config.getNodeFactory().objectNode();
    }

    @Override
    public JsonParser treeAsTokens(TreeNode n) {
        return new TreeTraversingParser((JsonNode) n, this);
    }

    /**
     * Convenience method that binds content read using given parser, using
     * configuration of this reader, except that content is bound as
     * JSON tree instead of configured root value type.
     *<p>
     * Note: if an object was specified with {@link #withValueToUpdate}, it
     * will be ignored.
     *<p>
     * NOTE: this method never tries to auto-detect format, since actual
     * (data-format specific) parser is given.
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends TreeNode> T readTree(JsonParser p) throws IOException {
        return (T) _bindAsTree(p);
    }
     
    @Override
    public void writeTree(JsonGenerator jgen, TreeNode rootNode) {
        throw new UnsupportedOperationException();
    }
    
    /*
    /**********************************************************
    /* Deserialization methods; others similar to what ObjectMapper has
    /**********************************************************
     */
    
    /**
     * Method that binds content read from given input source,
     * using configuration of this reader.
     * Value return is either newly constructed, or root value that
     * was specified with {@link #withValueToUpdate(Object)}.
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(InputStream src)
        throws IOException, JsonProcessingException
    {
        if (_dataFormatReaders != null) {
            return (T) _detectBindAndClose(_dataFormatReaders.findFormat(src), false);
        }
        
        return (T) _bindAndClose(_considerFilter(_parserFactory.createParser(src), false));
    }

    /**
     * Method that binds content read from given input source,
     * using configuration of this reader.
     * Value return is either newly constructed, or root value that
     * was specified with {@link #withValueToUpdate(Object)}.
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(Reader src)
        throws IOException, JsonProcessingException
    {
        if (_dataFormatReaders != null) {
            _reportUndetectableSource(src);
        }

        return (T) _bindAndClose(_considerFilter(_parserFactory.createParser(src), false));
    }
    
    /**
     * Method that binds content read from given JSON string,
     * using configuration of this reader.
     * Value return is either newly constructed, or root value that
     * was specified with {@link #withValueToUpdate(Object)}.
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(String src)
        throws IOException, JsonProcessingException
    {
        if (_dataFormatReaders != null) {
            _reportUndetectableSource(src);
        }
        
        return (T) _bindAndClose(_considerFilter(_parserFactory.createParser(src), false));
    }

    /**
     * Method that binds content read from given byte array,
     * using configuration of this reader.
     * Value return is either newly constructed, or root value that
     * was specified with {@link #withValueToUpdate(Object)}.
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(byte[] src)
        throws IOException, JsonProcessingException
    {
        if (_dataFormatReaders != null) {
            return (T) _detectBindAndClose(src, 0, src.length);
        }
        
        return (T) _bindAndClose(_considerFilter(_parserFactory.createParser(src), false));
    }

    /**
     * Method that binds content read from given byte array,
     * using configuration of this reader.
     * Value return is either newly constructed, or root value that
     * was specified with {@link #withValueToUpdate(Object)}.
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(byte[] src, int offset, int length)
        throws IOException, JsonProcessingException
    {
        if (_dataFormatReaders != null) {
            return (T) _detectBindAndClose(src, offset, length);
        }

        return (T) _bindAndClose(_considerFilter(_parserFactory.createParser(src, offset, length),
                false));
    }
    
    @SuppressWarnings("unchecked")
    public <T> T readValue(File src)
        throws IOException, JsonProcessingException
    {
        if (_dataFormatReaders != null) {
            return (T) _detectBindAndClose(_dataFormatReaders.findFormat(_inputStream(src)), true);
        }

        return (T) _bindAndClose(_considerFilter(_parserFactory.createParser(src), false));
    }

    /**
     * Method that binds content read from given input source,
     * using configuration of this reader.
     * Value return is either newly constructed, or root value that
     * was specified with {@link #withValueToUpdate(Object)}.
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(URL src)
        throws IOException, JsonProcessingException
    {
        if (_dataFormatReaders != null) {
            return (T) _detectBindAndClose(_dataFormatReaders.findFormat(_inputStream(src)), true);
        }

        return (T) _bindAndClose(_considerFilter(_parserFactory.createParser(src), false));
    }

    /**
     * Convenience method for converting results from given JSON tree into given
     * value type. Basically short-cut for:
     *<pre>
     *   objectReader.readValue(src.traverse())
     *</pre>
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(JsonNode src)
        throws IOException, JsonProcessingException
    {
        if (_dataFormatReaders != null) {
            _reportUndetectableSource(src);
        }
        
        return (T) _bindAndClose(_considerFilter(treeAsTokens(src), false));
    }

    /**
     * @since 2.8
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(DataInput src) throws IOException
    {
        if (_dataFormatReaders != null) {
            _reportUndetectableSource(src);
        }
        return (T) _bindAndClose(_considerFilter(_parserFactory.createParser(src), false));
    }

    /**
     * Method that reads content from given input source,
     * using configuration of this reader, and binds it as JSON Tree.
     *<p>
     * Note that if an object was specified with a call to
     * {@link #withValueToUpdate(Object)}
     * it will just be ignored; result is always a newly constructed
     * {@link JsonNode} instance.
     */
    public JsonNode readTree(InputStream in)
        throws IOException, JsonProcessingException
    {
        if (_dataFormatReaders != null) {
            return _detectBindAndCloseAsTree(in);
        }
        return _bindAndCloseAsTree(_considerFilter(_parserFactory.createParser(in), false));
    }
    
    /**
     * Method that reads content from given input source,
     * using configuration of this reader, and binds it as JSON Tree.
     *<p>
     * Note that if an object was specified with a call to
     * {@link #withValueToUpdate(Object)}
     * it will just be ignored; result is always a newly constructed
     * {@link JsonNode} instance.
     */
    public JsonNode readTree(Reader r)
        throws IOException, JsonProcessingException
    {
        if (_dataFormatReaders != null) {
            _reportUndetectableSource(r);
        }
        return _bindAndCloseAsTree(_considerFilter(_parserFactory.createParser(r), false));
    }

    /**
     * Method that reads content from given JSON input String,
     * using configuration of this reader, and binds it as JSON Tree.
     *<p>
     * Note that if an object was specified with a call to
     * {@link #withValueToUpdate(Object)}
     * it will just be ignored; result is always a newly constructed
     * {@link JsonNode} instance.
     */
    public JsonNode readTree(String json)
        throws IOException, JsonProcessingException
    {
        if (_dataFormatReaders != null) {
            _reportUndetectableSource(json);
        }
        return _bindAndCloseAsTree(_considerFilter(_parserFactory.createParser(json), false));
    }

    public JsonNode readTree(DataInput src) throws IOException
    {
        if (_dataFormatReaders != null) {
            _reportUndetectableSource(src);
        }
        return _bindAndCloseAsTree(_considerFilter(_parserFactory.createParser(src), false));
    }

    /*
    /**********************************************************
    /* Deserialization methods; reading sequence of values
    /**********************************************************
     */
    
    /**
     * Method for reading sequence of Objects from parser stream.
     *<p>
     * Sequence can be either root-level "unwrapped" sequence (without surrounding
     * JSON array), or a sequence contained in a JSON Array.
     * In either case {@link JsonParser} must point to the first token of
     * the first element, OR not point to any token (in which case it is advanced
     * to the next token). This means, specifically, that for wrapped sequences,
     * parser MUST NOT point to the surrounding <code>START_ARRAY</code> but rather
     * to the token following it.
     */
    public <T> MappingIterator<T> readValues(JsonParser p)
        throws IOException, JsonProcessingException
    {
        DeserializationContext ctxt = createDeserializationContext(p);
        // false -> do not close as caller gave parser instance
        return _newIterator(p, ctxt, _findRootDeserializer(ctxt), false);
    }
    
    /**
     * Method for reading sequence of Objects from parser stream.
     *<p>
     * Sequence can be either wrapped or unwrapped root-level sequence:
     * wrapped means that the elements are enclosed in JSON Array;
     * and unwrapped that elements are directly accessed at main level.
     * Assumption is that iff the first token of the document is
     * <code>START_ARRAY</code>, we have a wrapped sequence; otherwise
     * unwrapped. For wrapped sequences, leading <code>START_ARRAY</code>
     * is skipped, so that for both cases, underlying {@link JsonParser}
     * will point to what is expected to be the first token of the first
     * element.
     *<p>
     * Note that the wrapped vs unwrapped logic means that it is NOT
     * possible to use this method for reading an unwrapped sequence
     * of elements written as JSON Arrays: to read such sequences, one
     * has to use {@link #readValues(JsonParser)}, making sure parser
     * points to the first token of the first element (i.e. the second
     * <code>START_ARRAY</code> which is part of the first element).
     */
    public <T> MappingIterator<T> readValues(InputStream src)
        throws IOException, JsonProcessingException
    {
        if (_dataFormatReaders != null) {
            return _detectBindAndReadValues(_dataFormatReaders.findFormat(src), false);
        }
        
        return _bindAndReadValues(_considerFilter(_parserFactory.createParser(src), true));
    }
    
    /**
     * Overloaded version of {@link #readValue(InputStream)}.
     */
    @SuppressWarnings("resource")
    public <T> MappingIterator<T> readValues(Reader src)
        throws IOException, JsonProcessingException
    {
        if (_dataFormatReaders != null) {
            _reportUndetectableSource(src);
        }
        JsonParser p = _considerFilter(_parserFactory.createParser(src), true);
        DeserializationContext ctxt = createDeserializationContext(p);
        _initForMultiRead(ctxt, p);
        p.nextToken();
        return _newIterator(p, ctxt, _findRootDeserializer(ctxt), true);
    }
    
    /**
     * Overloaded version of {@link #readValue(InputStream)}.
     * 
     * @param json String that contains JSON content to parse
     */
    @SuppressWarnings("resource")
    public <T> MappingIterator<T> readValues(String json)
        throws IOException, JsonProcessingException
    {
        if (_dataFormatReaders != null) {
            _reportUndetectableSource(json);
        }
        JsonParser p = _considerFilter(_parserFactory.createParser(json), true);
        DeserializationContext ctxt = createDeserializationContext(p);
        _initForMultiRead(ctxt, p);
        p.nextToken();
        return _newIterator(p, ctxt, _findRootDeserializer(ctxt), true);
    }

    /**
     * Overloaded version of {@link #readValue(InputStream)}.
     */
    public <T> MappingIterator<T> readValues(byte[] src, int offset, int length)
        throws IOException, JsonProcessingException
    {
        if (_dataFormatReaders != null) {
            return _detectBindAndReadValues(_dataFormatReaders.findFormat(src, offset, length), false);
        }
        return _bindAndReadValues(_considerFilter(_parserFactory.createParser(src, offset, length),
                true));
    }

    /**
     * Overloaded version of {@link #readValue(InputStream)}.
     */
    public final <T> MappingIterator<T> readValues(byte[] src)
            throws IOException, JsonProcessingException {
        return readValues(src, 0, src.length);
    }
    
    /**
     * Overloaded version of {@link #readValue(InputStream)}.
     */
    public <T> MappingIterator<T> readValues(File src)
        throws IOException, JsonProcessingException
    {
        if (_dataFormatReaders != null) {
            return _detectBindAndReadValues(
                    _dataFormatReaders.findFormat(_inputStream(src)), false);
        }
        return _bindAndReadValues(_considerFilter(_parserFactory.createParser(src), true));
    }

    /**
     * Overloaded version of {@link #readValue(InputStream)}.
     * 
     * @param src URL to read to access JSON content to parse.
     */
    public <T> MappingIterator<T> readValues(URL src)
        throws IOException, JsonProcessingException
    {
        if (_dataFormatReaders != null) {
            return _detectBindAndReadValues(
                    _dataFormatReaders.findFormat(_inputStream(src)), true);
        }
        return _bindAndReadValues(_considerFilter(_parserFactory.createParser(src), true));
    }

    /**
     * @since 2.8
     */
    public <T> MappingIterator<T> readValues(DataInput src) throws IOException
    {
        if (_dataFormatReaders != null) {
            _reportUndetectableSource(src);
        }
        return _bindAndReadValues(_considerFilter(_parserFactory.createParser(src), true));
    }

    /*
    /**********************************************************
    /* Implementation of rest of ObjectCodec methods
    /**********************************************************
     */

    @Override
    public <T> T treeToValue(TreeNode n, Class<T> valueType) throws JsonProcessingException
    {
        try {
            return readValue(treeAsTokens(n), valueType);
        } catch (JsonProcessingException e) {
            throw e;
        } catch (IOException e) { // should not occur, no real i/o...
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }    

    @Override
    public void writeValue(JsonGenerator gen, Object value) throws IOException, JsonProcessingException {
        throw new UnsupportedOperationException("Not implemented for ObjectReader");
    }

    /*
    /**********************************************************
    /* Helper methods, data-binding
    /**********************************************************
     */
    
    /**
     * Actual implementation of value reading+binding operation.
     */
    protected Object _bind(JsonParser p, Object valueToUpdate) throws IOException
    {
        /* First: may need to read the next token, to initialize state (either
         * before first read from parser, or after previous token has been cleared)
         */
        Object result;
        final DeserializationContext ctxt = createDeserializationContext(p);
        JsonToken t = _initForReading(ctxt, p);
        if (t == JsonToken.VALUE_NULL) {
            if (valueToUpdate == null) {
                result = _findRootDeserializer(ctxt).getNullValue(ctxt);
            } else {
                result = valueToUpdate;
            }
        } else if (t == JsonToken.END_ARRAY || t == JsonToken.END_OBJECT) {
            result = valueToUpdate;
        } else { // pointing to event other than null
            JsonDeserializer<Object> deser = _findRootDeserializer(ctxt);
            if (_unwrapRoot) {
                result = _unwrapAndDeserialize(p, ctxt, _valueType, deser);
            } else {
                if (valueToUpdate == null) {
                    result = deser.deserialize(p, ctxt);
                } else {
                    deser.deserialize(p, ctxt, valueToUpdate);
                    result = valueToUpdate;
                }
            }
        }
        // Need to consume the token too
        p.clearCurrentToken();
        return result;
    }
    
    /**
     * Consider filter when creating JsonParser.  
     */
    protected JsonParser _considerFilter(final JsonParser p, boolean multiValue) {
        // 26-Mar-2016, tatu: Need to allow multiple-matches at least if we have
        //    have a multiple-value read (that is, "readValues()").
        return ((_filter == null) || FilteringParserDelegate.class.isInstance(p))
                ? p : new FilteringParserDelegate(p, _filter, false, multiValue);
    }
    
    protected Object _bindAndClose(JsonParser p0) throws IOException
    {
        try (JsonParser p = p0) {
            Object result;

            DeserializationContext ctxt = createDeserializationContext(p);
            JsonToken t = _initForReading(ctxt, p);
            if (t == JsonToken.VALUE_NULL) {
                if (_valueToUpdate == null) {
                    result = _findRootDeserializer(ctxt).getNullValue(ctxt);
                } else {
                    result = _valueToUpdate;
                }
            } else if (t == JsonToken.END_ARRAY || t == JsonToken.END_OBJECT) {
                result = _valueToUpdate;
            } else {
                JsonDeserializer<Object> deser = _findRootDeserializer(ctxt);
                if (_unwrapRoot) {
                    result = _unwrapAndDeserialize(p, ctxt, _valueType, deser);
                } else {
                    if (_valueToUpdate == null) {
                        result = deser.deserialize(p, ctxt);
                    } else {
                        deser.deserialize(p, ctxt, _valueToUpdate);
                        result = _valueToUpdate;                    
                    }
                }
            }
            return result;
        }
    }

    protected JsonNode _bindAndCloseAsTree(JsonParser p0) throws IOException {
        try (JsonParser p = p0) {
            return _bindAsTree(p);
        }
    }
    
    protected JsonNode _bindAsTree(JsonParser p) throws IOException
    {
        JsonNode result;
        DeserializationContext ctxt = createDeserializationContext(p);
        JsonToken t = _initForReading(ctxt, p);
        if (t == JsonToken.VALUE_NULL || t == JsonToken.END_ARRAY || t == JsonToken.END_OBJECT) {
            result = NullNode.instance;
        } else {
            JsonDeserializer<Object> deser = _findTreeDeserializer(ctxt);
            if (_unwrapRoot) {
                result = (JsonNode) _unwrapAndDeserialize(p, ctxt, JSON_NODE_TYPE, deser);
            } else {
                result = (JsonNode) deser.deserialize(p, ctxt);
            }
        }
        // Need to consume the token too
        p.clearCurrentToken();
        return result;
    }
    
    /**
     * @since 2.1
     */
    protected <T> MappingIterator<T> _bindAndReadValues(JsonParser p) throws IOException
    {
        DeserializationContext ctxt = createDeserializationContext(p);
        _initForMultiRead(ctxt, p);
        p.nextToken();
        return _newIterator(p, ctxt, _findRootDeserializer(ctxt), true);
    }

    protected Object _unwrapAndDeserialize(JsonParser p, DeserializationContext ctxt,
            JavaType rootType, JsonDeserializer<Object> deser) throws IOException
    {
        PropertyName expRootName = _config.findRootName(rootType);
        // 12-Jun-2015, tatu: Should try to support namespaces etc but...
        String expSimpleName = expRootName.getSimpleName();

        if (p.getCurrentToken() != JsonToken.START_OBJECT) {
            ctxt.reportWrongTokenException(p, JsonToken.START_OBJECT,
                    "Current token not START_OBJECT (needed to unwrap root name '%s'), but %s",
                    expSimpleName, p.getCurrentToken());
        }
        if (p.nextToken() != JsonToken.FIELD_NAME) {
            ctxt.reportWrongTokenException(p, JsonToken.FIELD_NAME,
                    "Current token not FIELD_NAME (to contain expected root name '%s'), but %s", 
                    expSimpleName, p.getCurrentToken());
        }
        String actualName = p.getCurrentName();
        if (!expSimpleName.equals(actualName)) {
            ctxt.reportMappingException("Root name '%s' does not match expected ('%s') for type %s",
                    actualName, expSimpleName, rootType);
        }
        // ok, then move to value itself....
        p.nextToken();
        Object result;
        if (_valueToUpdate == null) {
            result = deser.deserialize(p, ctxt);
        } else {
            deser.deserialize(p, ctxt, _valueToUpdate);
            result = _valueToUpdate;                    
        }
        // and last, verify that we now get matching END_OBJECT
        if (p.nextToken() != JsonToken.END_OBJECT) {
            ctxt.reportWrongTokenException(p, JsonToken.END_OBJECT,
                    "Current token not END_OBJECT (to match wrapper object with root name '%s'), but %s",
                    expSimpleName, p.getCurrentToken());
        }
        return result;
    }

    /*
    /**********************************************************
    /* Internal methods, format auto-detection
    /**********************************************************
     */
    
    @SuppressWarnings("resource")
    protected Object _detectBindAndClose(byte[] src, int offset, int length) throws IOException
    {
        DataFormatReaders.Match match = _dataFormatReaders.findFormat(src, offset, length);
        if (!match.hasMatch()) {
            _reportUnkownFormat(_dataFormatReaders, match);
        }
        JsonParser p = match.createParserWithMatch();
        return match.getReader()._bindAndClose(p);
    }

    @SuppressWarnings("resource")
    protected Object _detectBindAndClose(DataFormatReaders.Match match, boolean forceClosing)
        throws IOException
    {
        if (!match.hasMatch()) {
            _reportUnkownFormat(_dataFormatReaders, match);
        }
        JsonParser p = match.createParserWithMatch();
        // One more thing: we Own the input stream now; and while it's 
        // not super clean way to do it, we must ensure closure so:
        if (forceClosing) {
            p.enable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
        }
        // important: use matching ObjectReader (may not be 'this')
        return match.getReader()._bindAndClose(p);
    }

    @SuppressWarnings("resource")
    protected <T> MappingIterator<T> _detectBindAndReadValues(DataFormatReaders.Match match, boolean forceClosing)
        throws IOException, JsonProcessingException
    {
        if (!match.hasMatch()) {
            _reportUnkownFormat(_dataFormatReaders, match);
        }
        JsonParser p = match.createParserWithMatch();
        // One more thing: we Own the input stream now; and while it's 
        // not super clean way to do it, we must ensure closure so:
        if (forceClosing) {
            p.enable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
        }
        // important: use matching ObjectReader (may not be 'this')
        return match.getReader()._bindAndReadValues(p);
    }
    
    @SuppressWarnings("resource")
    protected JsonNode _detectBindAndCloseAsTree(InputStream in) throws IOException
    {
        DataFormatReaders.Match match = _dataFormatReaders.findFormat(in);
        if (!match.hasMatch()) {
            _reportUnkownFormat(_dataFormatReaders, match);
        }
        JsonParser p = match.createParserWithMatch();
        p.enable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
        return match.getReader()._bindAndCloseAsTree(p);
    }
    
    /**
     * Method called to indicate that format detection failed to detect format
     * of given input
     */
    protected void _reportUnkownFormat(DataFormatReaders detector, DataFormatReaders.Match match) throws JsonProcessingException
    {
        // 17-Aug-2015, tatu: Unfortunately, no parser/generator available so:
        throw new JsonParseException(null, "Can not detect format from input, does not look like any of detectable formats "
                +detector.toString());
    }

    /*
    /**********************************************************
    /* Internal methods, other
    /**********************************************************
     */

    /**
     * @since 2.2
     */
    protected void _verifySchemaType(FormatSchema schema)
    {
        if (schema != null) {
            if (!_parserFactory.canUseSchema(schema)) {
                    throw new IllegalArgumentException("Can not use FormatSchema of type "+schema.getClass().getName()
                            +" for format "+_parserFactory.getFormatName());
            }
        }
    }

    /**
     * Internal helper method called to create an instance of {@link DeserializationContext}
     * for deserializing a single root value.
     * Can be overridden if a custom context is needed.
     */
    protected DefaultDeserializationContext createDeserializationContext(JsonParser p) {
        return _context.createInstance(_config, p, _injectableValues);
    }

    protected void _reportUndetectableSource(Object src) throws JsonProcessingException
    {
        // 17-Aug-2015, tatu: Unfortunately, no parser/generator available so:
        throw new JsonParseException(null, "Can not use source of type "
                +src.getClass().getName()+" with format auto-detection: must be byte- not char-based");
    }

    protected InputStream _inputStream(URL src) throws IOException {
        return src.openStream();
    }

    protected InputStream _inputStream(File f) throws IOException {
        return new FileInputStream(f);
    }

    /*
    /**********************************************************
    /* Helper methods, locating deserializers etc
    /**********************************************************
     */
    
    /**
     * Method called to locate deserializer for the passed root-level value.
     */
    protected JsonDeserializer<Object> _findRootDeserializer(DeserializationContext ctxt)
        throws JsonMappingException
    {
        if (_rootDeserializer != null) {
            return _rootDeserializer;
        }

        // Sanity check: must have actual type...
        JavaType t = _valueType;
        if (t == null) {
            ctxt.reportMappingException("No value type configured for ObjectReader");
        }
        
        // First: have we already seen it?
        JsonDeserializer<Object> deser = _rootDeserializers.get(t);
        if (deser != null) {
            return deser;
        }
        // Nope: need to ask provider to resolve it
        deser = ctxt.findRootValueDeserializer(t);
        if (deser == null) { // can this happen?
            ctxt.reportMappingException("Can not find a deserializer for type %s", t);
        }
        _rootDeserializers.put(t, deser);
        return deser;
    }

    /**
     * @since 2.6
     */
    protected JsonDeserializer<Object> _findTreeDeserializer(DeserializationContext ctxt)
        throws JsonMappingException
    {
        JsonDeserializer<Object> deser = _rootDeserializers.get(JSON_NODE_TYPE);
        if (deser == null) {
            // Nope: need to ask provider to resolve it
            deser = ctxt.findRootValueDeserializer(JSON_NODE_TYPE);
            if (deser == null) { // can this happen?
                ctxt.reportMappingException("Can not find a deserializer for type %s",
                        JSON_NODE_TYPE);
            }
            _rootDeserializers.put(JSON_NODE_TYPE, deser);
        }
        return deser;
    }

    /**
     * Method called to locate deserializer ahead of time, if permitted
     * by configuration. Method also is NOT to throw an exception if
     * access fails.
     */
    protected JsonDeserializer<Object> _prefetchRootDeserializer(JavaType valueType)
    {
        if (valueType == null || !_config.isEnabled(DeserializationFeature.EAGER_DESERIALIZER_FETCH)) {
            return null;
        }
        // already cached?
        JsonDeserializer<Object> deser = _rootDeserializers.get(valueType);
        if (deser == null) {
            try {
                // If not, need to resolve; for which we need a temporary context as well:
                DeserializationContext ctxt = createDeserializationContext(null);
                deser = ctxt.findRootValueDeserializer(valueType);
                if (deser != null) {
                    _rootDeserializers.put(valueType, deser);
                }
                return deser;
            } catch (JsonProcessingException e) {
                // need to swallow?
            }
        }
        return deser;
    }
}
