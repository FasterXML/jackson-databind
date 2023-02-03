package com.fasterxml.jackson.databind;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.filter.FilteringParserDelegate;
import com.fasterxml.jackson.core.filter.JsonPointerBasedFilter;
import com.fasterxml.jackson.core.filter.TokenFilter;
import com.fasterxml.jackson.core.filter.TokenFilter.Inclusion;
import com.fasterxml.jackson.core.type.ResolvedType;
import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.cfg.ContextAttributes;
import com.fasterxml.jackson.databind.cfg.DatatypeFeature;
import com.fasterxml.jackson.databind.deser.DataFormatReaders;
import com.fasterxml.jackson.databind.deser.DefaultDeserializationContext;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.ClassUtil;

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
 *<p>
 * NOTE: this class is NOT meant as sub-classable (with Jackson 2.8 and
 * above) by users. It is left as non-final mostly to allow frameworks
 * that require bytecode generation for proxying and similar use cases,
 * but there is no expecation that functionality should be extended
 * by sub-classing.
 */
public class ObjectReader
    extends ObjectCodec
    implements Versioned, java.io.Serializable // since 2.1
{
    private static final long serialVersionUID = 2L; // since 2.9

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
     * types cannot be modified because array size is immutable.
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
     * cannot be detect reliably (as per detector settings),
     * a {@link StreamReadException} will be thrown).
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

    /**
     * Lazily resolved {@link JavaType} for {@link JsonNode}
     */
    protected transient JavaType _jsonNodeType;

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
    /* Helper methods used internally for invoking constructors
    /* Need to be overridden if sub-classing (not recommended)
    /* is used.
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
    /* Methods for initializing parser instance to use
    /**********************************************************
     */

    protected JsonToken _initForReading(DeserializationContext ctxt, JsonParser p)
        throws IOException
    {
        _config.initialize(p, _schema);

        /* First: must point to a token; if not pointing to one, advance.
         * This occurs before first read from JsonParser, as well as
         * after clearing of current token.
         */
        JsonToken t = p.currentToken();
        if (t == null) { // and then we must get something...
            t = p.nextToken();
            if (t == null) {
                // Throw mapping exception, since it's failure to map, not an actual parsing problem
                ctxt.reportInputMismatch(_valueType,
                        "No content to map due to end-of-input");
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
        _config.initialize(p, _schema);
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
    /**********************************************************************
    /* Life-cycle, fluent factory methods for DatatypeFeatures (2.14+)
    /**********************************************************************
     */

    /**
     * Method for constructing a new reader instance that is configured
     * with specified feature enabled.
     *
     * @since 2.14
     */
    public ObjectReader with(DatatypeFeature feature) {
        return _with(_config.with(feature));
    }

    /**
     * Method for constructing a new reader instance that is configured
     * with specified features enabled.
     *
     * @since 2.14
     */
    public ObjectReader withFeatures(DatatypeFeature... features) {
        return _with(_config.withFeatures(features));
    }

    /**
     * Method for constructing a new reader instance that is configured
     * with specified feature disabled.
     *
     * @since 2.14
     */
    public ObjectReader without(DatatypeFeature feature) {
        return _with(_config.without(feature));
    }

    /**
     * Method for constructing a new reader instance that is configured
     * with specified features disabled.
     *
     * @since 2.14
     */
    public ObjectReader withoutFeatures(DatatypeFeature... features) {
        return _with(_config.withoutFeatures(features));
    }

    /*
    /**********************************************************
    /* Life-cycle, fluent factory methods for JsonParser.Features
    /* (to be deprecated in 2.12?)
    /**********************************************************
     */

    /**
     * Method for constructing a new reader instance that is configured
     * with specified feature enabled.
     *
     * @param feature Feature to enable
     *
     * @return Reader instance with specified feature enabled
     */
    public ObjectReader with(JsonParser.Feature feature) {
        return _with(_config.with(feature));
    }

    /**
     * Method for constructing a new reader instance that is configured
     * with specified features enabled.
     *
     * @param features Features to enable
     *
     * @return Reader instance with specified features enabled
     */
    public ObjectReader withFeatures(JsonParser.Feature... features) {
        return _with(_config.withFeatures(features));
    }

    /**
     * Method for constructing a new reader instance that is configured
     * with specified feature disabled.
     *
     * @param feature Feature to disable
     *
     * @return Reader instance with specified feature disabled
     */
    public ObjectReader without(JsonParser.Feature feature) {
        return _with(_config.without(feature));
    }

    /**
     * Method for constructing a new reader instance that is configured
     * with specified features disabled.
     *
     * @param features Features to disable
     *
     * @return Reader instance with specified features disabled
     */
    public ObjectReader withoutFeatures(JsonParser.Feature... features) {
        return _with(_config.withoutFeatures(features));
    }

    /*
    /**********************************************************************
    /* Life-cycle, fluent factory methods for StreamReadFeatures (added in 2.11)
    /**********************************************************************
     */

    /**
     * Method for constructing a new reader instance that is configured
     * with specified feature enabled.
     *
     * @return Reader instance with specified feature enabled
     *
     * @since 2.11
     */
    public ObjectReader with(StreamReadFeature feature) {
        return _with(_config.with(feature.mappedFeature()));
    }

    /**
     * Method for constructing a new reader instance that is configured
     * with specified feature disabled.
     *
     * @return Reader instance with specified feature enabled
     *
     * @since 2.11
     */
    public ObjectReader without(StreamReadFeature feature) {
        return _with(_config.without(feature.mappedFeature()));
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
    public ObjectReader at(final String pointerExpr) {
        _assertNotNull("pointerExpr", pointerExpr);
        return new ObjectReader(this, new JsonPointerBasedFilter(pointerExpr));
    }

    /**
     * Convenience method to bind from {@link JsonPointer}
      * {@link JsonPointerBasedFilter} is registered and will be used for parsing later.
     * @since 2.6
     */
    public ObjectReader at(final JsonPointer pointer) {
        _assertNotNull("pointer", pointer);
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
     * get a failure from some 'readValue()' methods. Also, if input cannot be reliably
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
     * get a failure from some 'readValue()' methods. Also, if input cannot be reliably
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

    /**
     * @since 2.14
     */
    public boolean isEnabled(DatatypeFeature f) {
        return _config.isEnabled(f);
    }

    public boolean isEnabled(JsonParser.Feature f) {
        return _config.isEnabled(f, _parserFactory);
    }

    /**
     * @since 2.11
     */
    public boolean isEnabled(StreamReadFeature f) {
        return _config.isEnabled(f.mappedFeature(), _parserFactory);
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

    /**
     * @since 2.10
     */
    public JavaType getValueType() {
        return _valueType;
    }

    /*
    /**********************************************************
    /* Factory methods for creating JsonParsers (added in 2.11)
    /**********************************************************
     */

    /**
     * Factory method for constructing properly initialized {@link JsonParser}
     * to read content from specified {@link File}.
     * Parser is not managed (or "owned") by ObjectReader: caller is responsible
     * for properly closing it once content reading is complete.
     *
     * @since 2.11
     */
    public JsonParser createParser(File src) throws IOException {
        _assertNotNull("src", src);
        return _config.initialize(_parserFactory.createParser(src), _schema);
    }

    /**
     * Factory method for constructing properly initialized {@link JsonParser}
     * to read content from specified {@link File}.
     * Parser is not managed (or "owned") by ObjectReader: caller is responsible
     * for properly closing it once content reading is complete.
     *
     * @since 2.11
     */
    public JsonParser createParser(URL src) throws IOException {
        _assertNotNull("src", src);
        return _config.initialize(_parserFactory.createParser(src), _schema);
    }

    /**
     * Factory method for constructing properly initialized {@link JsonParser}
     * to read content using specified {@link InputStream}.
     * Parser is not managed (or "owned") by ObjectReader: caller is responsible
     * for properly closing it once content reading is complete.
     *
     * @since 2.11
     */
    public JsonParser createParser(InputStream in) throws IOException {
        _assertNotNull("in", in);
        return _config.initialize(_parserFactory.createParser(in), _schema);
    }

    /**
     * Factory method for constructing properly initialized {@link JsonParser}
     * to read content using specified {@link Reader}.
     * Parser is not managed (or "owned") by ObjectReader: caller is responsible
     * for properly closing it once content reading is complete.
     *
     * @since 2.11
     */
    public JsonParser createParser(Reader r) throws IOException {
        _assertNotNull("r", r);
        return _config.initialize(_parserFactory.createParser(r), _schema);
    }

    /**
     * Factory method for constructing properly initialized {@link JsonParser}
     * to read content from specified byte array.
     * Parser is not managed (or "owned") by ObjectReader: caller is responsible
     * for properly closing it once content reading is complete.
     *
     * @since 2.11
     */
    public JsonParser createParser(byte[] content) throws IOException {
        _assertNotNull("content", content);
        return _config.initialize(_parserFactory.createParser(content), _schema);
    }

    /**
     * Factory method for constructing properly initialized {@link JsonParser}
     * to read content from specified byte array.
     * Parser is not managed (or "owned") by ObjectReader: caller is responsible
     * for properly closing it once content reading is complete.
     *
     * @since 2.11
     */
    public JsonParser createParser(byte[] content, int offset, int len) throws IOException {
        _assertNotNull("content", content);
        return _config.initialize(_parserFactory.createParser(content, offset, len), _schema);
    }

    /**
     * Factory method for constructing properly initialized {@link JsonParser}
     * to read content from specified String.
     * Parser is not managed (or "owned") by ObjectReader: caller is responsible
     * for properly closing it once content reading is complete.
     *
     * @since 2.11
     */
    public JsonParser createParser(String content) throws IOException {
        _assertNotNull("content", content);
        return _config.initialize(_parserFactory.createParser(content), _schema);
    }

    /**
     * Factory method for constructing properly initialized {@link JsonParser}
     * to read content from specified character array
     * Parser is not managed (or "owned") by ObjectReader: caller is responsible
     * for properly closing it once content reading is complete.
     *
     * @since 2.11
     */
    public JsonParser createParser(char[] content) throws IOException {
        _assertNotNull("content", content);
        return _config.initialize(_parserFactory.createParser(content), _schema);
    }

    /**
     * Factory method for constructing properly initialized {@link JsonParser}
     * to read content from specified character array.
     * Parser is not managed (or "owned") by ObjectReader: caller is responsible
     * for properly closing it once content reading is complete.
     *
     * @since 2.11
     */
    public JsonParser createParser(char[] content, int offset, int len) throws IOException {
        _assertNotNull("content", content);
        return _config.initialize(_parserFactory.createParser(content, offset, len), _schema);
    }

    /**
     * Factory method for constructing properly initialized {@link JsonParser}
     * to read content using specified {@link DataInput}.
     * Parser is not managed (or "owned") by ObjectReader: caller is responsible
     * for properly closing it once content reading is complete.
     *
     * @since 2.11
     */
    public JsonParser createParser(DataInput content) throws IOException {
        _assertNotNull("content", content);
        return _config.initialize(_parserFactory.createParser(content), _schema);
    }

    /**
     * Factory method for constructing properly initialized {@link JsonParser}
     * to read content using non-blocking (asynchronous) mode.
     * Parser is not managed (or "owned") by ObjectReader: caller is responsible
     * for properly closing it once content reading is complete.
     *
     * @since 2.11
     */
    public JsonParser createNonBlockingByteArrayParser() throws IOException {
        return _config.initialize(_parserFactory.createNonBlockingByteArrayParser(), _schema);
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
        _assertNotNull("p", p);
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
        _assertNotNull("p", p);
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
    public <T> T readValue(JsonParser p, TypeReference<T> valueTypeRef) throws IOException
    {
        _assertNotNull("p", p);
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
    public <T> T readValue(JsonParser p, ResolvedType valueType) throws IOException {
        _assertNotNull("p", p);
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
        _assertNotNull("p", p);
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
        _assertNotNull("p", p);
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
    public <T> Iterator<T> readValues(JsonParser p, TypeReference<T> valueTypeRef) throws IOException {
        _assertNotNull("p", p);
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
        _assertNotNull("p", p);
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
        _assertNotNull("p", p);
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

    @Override // since 2.10
    public JsonNode missingNode() {
        return _config.getNodeFactory().missingNode();
    }

    @Override // since 2.10
    public JsonNode nullNode() {
        return _config.getNodeFactory().nullNode();
    }

    @Override
    public JsonParser treeAsTokens(TreeNode n) {
        _assertNotNull("n", n);
        // 05-Dec-2017, tatu: Important! Must clear "valueToUpdate" since we do not
        //    want update to be applied here, as a side effect
        ObjectReader codec = withValueToUpdate(null);
        return new TreeTraversingParser((JsonNode) n, codec);
    }

    /**
     * Convenience method that binds content read using given parser, using
     * configuration of this reader, except that content is bound as
     * JSON tree instead of configured root value type.
     * Returns {@link JsonNode} that represents the root of the resulting tree, if there
     * was content to read, or {@code null} if no more content is accessible
     * via passed {@link JsonParser}.
     *<p>
     * NOTE! Behavior with end-of-input (no more content) differs between this
     * {@code readTree} method, and all other methods that take input source: latter
     * will return "missing node", NOT {@code null}
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
        _assertNotNull("p", p);
        return (T) _bindAsTreeOrNull(p);
    }

    @Override
    public void writeTree(JsonGenerator g, TreeNode rootNode) {
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
     *
     * @param src Source to read content from
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(InputStream src) throws IOException
    {
        if (_dataFormatReaders != null) {
            return (T) _detectBindAndClose(_dataFormatReaders.findFormat(src), false);
        }
        return (T) _bindAndClose(_considerFilter(createParser(src), false));
    }

    /**
     * Same as {@link #readValue(InputStream)} except that target value type
     * overridden as {@code valueType}
     *
     * @param src Source to read content from
     * @param valueType Target type to bind content to
     *
     * @since 2.11
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(InputStream src, Class<T> valueType) throws IOException
    {
        return (T) forType(valueType).readValue(src);
    }

    /**
     * Method that binds content read from given input source,
     * using configuration of this reader.
     * Value return is either newly constructed, or root value that
     * was specified with {@link #withValueToUpdate(Object)}.
     *
     * @param src Source to read content from
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(Reader src) throws IOException
    {
        if (_dataFormatReaders != null) {
            _reportUndetectableSource(src);
        }
        return (T) _bindAndClose(_considerFilter(createParser(src), false));
    }

    /**
     * Same as {@link #readValue(Reader)} except that target value type
     * overridden as {@code valueType}
     *
     * @param src Source to read content from
     * @param valueType Target type to bind content to
     *
     * @since 2.11
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(Reader src, Class<T> valueType) throws IOException
    {
        return (T) forType(valueType).readValue(src);
    }

    /**
     * Method that binds content read from given JSON string,
     * using configuration of this reader.
     * Value return is either newly constructed, or root value that
     * was specified with {@link #withValueToUpdate(Object)}.
     *
     * @param src String that contains content to read
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(String src) throws JsonProcessingException, JsonMappingException
    {
        if (_dataFormatReaders != null) {
            _reportUndetectableSource(src);
        }
        try { // since 2.10 remove "impossible" IOException as per [databind#1675]
            return (T) _bindAndClose(_considerFilter(createParser(src), false));
        } catch (JsonProcessingException e) {
            throw e;
        } catch (IOException e) { // shouldn't really happen but being declared need to
            throw JsonMappingException.fromUnexpectedIOE(e);
        }
    }

    /**
     * Same as {@link #readValue(String)} except that target value type
     * overridden as {@code valueType}
     *
     * @param src String that contains content to read
     * @param valueType Target type to bind content to
     *
     * @since 2.11
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(String src, Class<T> valueType) throws IOException
    {
        return (T) forType(valueType).readValue(src);
    }

    /**
     * Method that binds content read from given byte array,
     * using configuration of this reader.
     * Value return is either newly constructed, or root value that
     * was specified with {@link #withValueToUpdate(Object)}.
     *
     * @param content Byte array that contains encoded content to read
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(byte[] content) throws IOException
    {
        if (_dataFormatReaders != null) {
            return (T) _detectBindAndClose(content, 0, content.length);
        }
        return (T) _bindAndClose(_considerFilter(createParser(content), false));
    }

    /**
     * Same as {@link #readValue(byte[])} except that target value type
     * overridden as {@code valueType}
     *
     * @param content Byte array that contains encoded content to read
     * @param valueType Target type to bind content to
     *
     * @since 2.11
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(byte[] content, Class<T> valueType) throws IOException
    {
        return (T) forType(valueType).readValue(content);
    }

    /**
     * Method that binds content read from given byte array,
     * using configuration of this reader.
     * Value return is either newly constructed, or root value that
     * was specified with {@link #withValueToUpdate(Object)}.
     *
     * @param buffer Byte array that contains encoded content to read
     * @param offset Offset of the first content byte in {@code buffer}
     * @param length Length of content in {@code buffer}, in bytes
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(byte[] buffer, int offset, int length) throws IOException
    {
        if (_dataFormatReaders != null) {
            return (T) _detectBindAndClose(buffer, offset, length);
        }
        return (T) _bindAndClose(_considerFilter(createParser(buffer, offset, length),
                false));
    }

    /**
     * Same as {@link #readValue(byte[],int,int)} except that target value type
     * overridden as {@code valueType}
     *
     * @param buffer Byte array that contains encoded content to read
     * @param offset Offset of the first content byte in {@code buffer}
     * @param length Length of content in {@code buffer}, in bytes
     * @param valueType Target type to bind content to
     *
     * @since 2.11
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(byte[] buffer, int offset, int length, Class<T> valueType) throws IOException
    {
        return (T) forType(valueType).readValue(buffer, offset, length);
    }

    /**
     * Method that binds content read from given {@link File}
     * using configuration of this reader.
     * Value return is either newly constructed, or root value that
     * was specified with {@link #withValueToUpdate(Object)}.
     *
     * @param src File that contains content to read
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(File src) throws IOException
    {
        if (_dataFormatReaders != null) {
            return (T) _detectBindAndClose(_dataFormatReaders.findFormat(_inputStream(src)), true);
        }

        return (T) _bindAndClose(_considerFilter(createParser(src), false));
    }

    /**
     * Same as {@link #readValue(File)} except that target value type
     * overridden as {@code valueType}
     *
     * @param src File that contains content to read
     * @param valueType Target type to bind content to
     *
     * @since 2.11
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(File src, Class<T> valueType) throws IOException
    {
        return (T) forType(valueType).readValue(src);
    }

    /**
     * Method that binds content read from given input source,
     * using configuration of this reader.
     * Value return is either newly constructed, or root value that
     * was specified with {@link #withValueToUpdate(Object)}.
     *<p>
     *<p>
     * NOTE: handling of {@link java.net.URL} is delegated to
     * {@link JsonFactory#createParser(java.net.URL)} and usually simply
     * calls {@link java.net.URL#openStream()}, meaning no special handling
     * is done. If different HTTP connection options are needed you will need
     * to create {@link java.io.InputStream} separately.
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(URL src) throws IOException
    {
        if (_dataFormatReaders != null) {
            return (T) _detectBindAndClose(_dataFormatReaders.findFormat(_inputStream(src)), true);
        }
        return (T) _bindAndClose(_considerFilter(createParser(src), false));
    }

    /**
     * Same as {@link #readValue(URL)} except that target value type
     * overridden as {@code valueType}
     *
     * @param src URL pointing to resource that contains content to read
     * @param valueType Target type to bind content to
     *
     * @since 2.11
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(URL src, Class<T> valueType) throws IOException
    {
        return (T) forType(valueType).readValue(src);
    }

    /**
     * Convenience method for converting results from given JSON tree into given
     * value type. Basically short-cut for:
     *<pre>
     *   objectReader.readValue(src.traverse())
     *</pre>
     *
     * @param content Tree that contains content to convert
     */
    @SuppressWarnings({ "unchecked" })
    public <T> T readValue(JsonNode content) throws IOException
    {
        _assertNotNull("content", content);
        if (_dataFormatReaders != null) {
            _reportUndetectableSource(content);
        }
        return (T) _bindAndClose(_considerFilter(treeAsTokens(content), false));
    }

    /**
     * Same as {@link #readValue(JsonNode)} except that target value type
     * overridden as {@code valueType}
     *
     * @param content Tree that contains content to convert
     * @param valueType Target type to convert content to
     *
     * @since 2.11
     */
    @SuppressWarnings({ "unchecked" })
    public <T> T readValue(JsonNode content, Class<T> valueType) throws IOException
    {
        return (T) forType(valueType).readValue(content);
    }

    @SuppressWarnings("unchecked")
    public <T> T readValue(DataInput src) throws IOException
    {
        if (_dataFormatReaders != null) {
            _reportUndetectableSource(src);
        }
        return (T) _bindAndClose(_considerFilter(createParser(src), false));
    }

    /**
     * Same as {@link #readValue(DataInput)} except that target value type
     * overridden as {@code valueType}
     *
     * @param content DataInput that contains content to read
     * @param valueType Target type to bind content to
     *
     * @since 2.11
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(DataInput content, Class<T> valueType) throws IOException
    {
        return (T) forType(valueType).readValue(content);
    }

    /*
    /**********************************************************
    /* Deserialization methods; JsonNode ("tree")
    /**********************************************************
     */

    /**
     * Method that reads content from given input source,
     * using configuration of this reader, and binds it as JSON Tree.
     * Returns {@link JsonNode} that represents the root of the resulting tree, if there
     * was content to read, or "missing node" (instance of {@link JsonNode} for which
     * {@link JsonNode#isMissingNode()} returns true, and behaves otherwise similar to
     * "null node") if no more content is accessible through passed-in input source.
     *<p>
     * NOTE! Behavior with end-of-input (no more content) differs between this
     * {@code readTree} method, and {@link #readTree(JsonParser)} -- latter returns
     * {@code null} for "no content" case.
     *<p>
     * Note that if an object was specified with a call to
     * {@link #withValueToUpdate(Object)}
     * it will just be ignored; result is always a newly constructed
     * {@link JsonNode} instance.
     */
    public JsonNode readTree(InputStream src) throws IOException
    {
        if (_dataFormatReaders != null) {
            return _detectBindAndCloseAsTree(src);
        }
        return _bindAndCloseAsTree(_considerFilter(createParser(src), false));
    }

    /**
     * Same as {@link #readTree(InputStream)} except content accessed through
     * passed-in {@link Reader}
     */
    public JsonNode readTree(Reader src) throws IOException
    {
        if (_dataFormatReaders != null) {
            _reportUndetectableSource(src);
        }
        return _bindAndCloseAsTree(_considerFilter(createParser(src), false));
    }

    /**
     * Same as {@link #readTree(InputStream)} except content read from
     * passed-in {@link String}
     */
    public JsonNode readTree(String json) throws JsonProcessingException, JsonMappingException
    {
        if (_dataFormatReaders != null) {
            _reportUndetectableSource(json);
        }
        try { // since 2.10 remove "impossible" IOException as per [databind#1675]
            return _bindAndCloseAsTree(_considerFilter(createParser(json), false));
        } catch (JsonProcessingException e) {
            throw e;
        } catch (IOException e) { // shouldn't really happen but being declared need to
            throw JsonMappingException.fromUnexpectedIOE(e);
        }
    }

    /**
     * Same as {@link #readTree(InputStream)} except content read from
     * passed-in byte array.
     */
    public JsonNode readTree(byte[] json) throws IOException
    {
        _assertNotNull("json", json);
        if (_dataFormatReaders != null) {
            _reportUndetectableSource(json);
        }
        return _bindAndCloseAsTree(_considerFilter(createParser(json), false));
    }

    /**
     * Same as {@link #readTree(InputStream)} except content read from
     * passed-in byte array.
     */
    public JsonNode readTree(byte[] json, int offset, int len) throws IOException
    {
        if (_dataFormatReaders != null) {
            _reportUndetectableSource(json);
        }
        return _bindAndCloseAsTree(_considerFilter(createParser(json, offset, len), false));
    }

    /**
     * Same as {@link #readTree(InputStream)} except content read using
     * passed-in {@link DataInput}.
     */
    public JsonNode readTree(DataInput src) throws IOException
    {
        if (_dataFormatReaders != null) {
            _reportUndetectableSource(src);
        }
        return _bindAndCloseAsTree(_considerFilter(createParser(src), false));
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
    public <T> MappingIterator<T> readValues(JsonParser p) throws IOException
    {
        _assertNotNull("p", p);
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
    public <T> MappingIterator<T> readValues(InputStream src) throws IOException
    {
        if (_dataFormatReaders != null) {
            return _detectBindAndReadValues(_dataFormatReaders.findFormat(src), false);
        }

        return _bindAndReadValues(_considerFilter(createParser(src), true));
    }

    /**
     * Overloaded version of {@link #readValue(InputStream)}.
     */
    public <T> MappingIterator<T> readValues(Reader src) throws IOException
    {
        if (_dataFormatReaders != null) {
            _reportUndetectableSource(src);
        }
        JsonParser p = _considerFilter(createParser(src), true);
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
    public <T> MappingIterator<T> readValues(String json) throws IOException
    {
        if (_dataFormatReaders != null) {
            _reportUndetectableSource(json);
        }
        JsonParser p = _considerFilter(createParser(json), true);
        DeserializationContext ctxt = createDeserializationContext(p);
        _initForMultiRead(ctxt, p);
        p.nextToken();
        return _newIterator(p, ctxt, _findRootDeserializer(ctxt), true);
    }

    /**
     * Overloaded version of {@link #readValue(InputStream)}.
     */
    public <T> MappingIterator<T> readValues(byte[] src, int offset, int length) throws IOException
    {
        if (_dataFormatReaders != null) {
            return _detectBindAndReadValues(_dataFormatReaders.findFormat(src, offset, length), false);
        }
        return _bindAndReadValues(_considerFilter(createParser(src, offset, length),
                true));
    }

    /**
     * Overloaded version of {@link #readValue(InputStream)}.
     */
    public final <T> MappingIterator<T> readValues(byte[] src) throws IOException {
        _assertNotNull("src", src);
        return readValues(src, 0, src.length);
    }

    /**
     * Overloaded version of {@link #readValue(InputStream)}.
     */
    public <T> MappingIterator<T> readValues(File src) throws IOException
    {
        if (_dataFormatReaders != null) {
            return _detectBindAndReadValues(
                    _dataFormatReaders.findFormat(_inputStream(src)), false);
        }
        return _bindAndReadValues(_considerFilter(createParser(src), true));
    }

    /**
     * Overloaded version of {@link #readValue(InputStream)}.
     *<p>
     * NOTE: handling of {@link java.net.URL} is delegated to
     * {@link JsonFactory#createParser(java.net.URL)} and usually simply
     * calls {@link java.net.URL#openStream()}, meaning no special handling
     * is done. If different HTTP connection options are needed you will need
     * to create {@link java.io.InputStream} separately.
     *
     * @param src URL to read to access JSON content to parse.
     */
    public <T> MappingIterator<T> readValues(URL src) throws IOException
    {
        if (_dataFormatReaders != null) {
            return _detectBindAndReadValues(
                    _dataFormatReaders.findFormat(_inputStream(src)), true);
        }
        return _bindAndReadValues(_considerFilter(createParser(src), true));
    }

    /**
     * @since 2.8
     */
    public <T> MappingIterator<T> readValues(DataInput src) throws IOException
    {
        if (_dataFormatReaders != null) {
            _reportUndetectableSource(src);
        }
        return _bindAndReadValues(_considerFilter(createParser(src), true));
    }

    /*
    /**********************************************************
    /* Implementation of rest of ObjectCodec methods
    /**********************************************************
     */

    @Override
    public <T> T treeToValue(TreeNode n, Class<T> valueType) throws JsonProcessingException
    {
        _assertNotNull("n", n);
        try {
            return readValue(treeAsTokens(n), valueType);
        } catch (JsonProcessingException e) {
            throw e;
        } catch (IOException e) { // should not occur, no real i/o...
            throw JsonMappingException.fromUnexpectedIOE(e);
        }
    }

    /**
     * Same as {@link #treeToValue(TreeNode, Class)} but with type-resolved target value type.
     *
     * @since 2.13
     */
    public <T> T treeToValue(TreeNode n, JavaType valueType) throws JsonProcessingException
    {
        _assertNotNull("n", n);
        try {
            return readValue(treeAsTokens(n), valueType);
        } catch (JsonProcessingException e) {
            throw e;
        } catch (IOException e) { // should not occur, no real i/o...
            throw JsonMappingException.fromUnexpectedIOE(e);
        }
    }

    @Override
    public void writeValue(JsonGenerator gen, Object value) throws IOException {
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
        // First: may need to read the next token, to initialize state (either
        // before first read from parser, or after previous token has been cleared)
        Object result;
        final DefaultDeserializationContext ctxt = createDeserializationContext(p);
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
            result = ctxt.readRootValue(p, _valueType, _findRootDeserializer(ctxt), _valueToUpdate);
        }
        // Need to consume the token too
        p.clearCurrentToken();
        if (_config.isEnabled(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)) {
            _verifyNoTrailingTokens(p, ctxt, _valueType);
        }
        return result;
    }

    protected Object _bindAndClose(JsonParser p0) throws IOException
    {
        try (JsonParser p = p0) {
            Object result;

            final DefaultDeserializationContext ctxt = createDeserializationContext(p);
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
                result = ctxt.readRootValue(p, _valueType, _findRootDeserializer(ctxt), _valueToUpdate);
            }
            // No need to consume the token as parser gets closed anyway
            if (_config.isEnabled(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)) {
                _verifyNoTrailingTokens(p, ctxt, _valueType);
            }
            return result;
        }
    }

    protected final JsonNode _bindAndCloseAsTree(JsonParser p0) throws IOException {
        try (JsonParser p = p0) {
            return _bindAsTree(p);
        }
    }

    protected final JsonNode _bindAsTree(JsonParser p) throws IOException
    {
        // 16-Apr-2021, tatu: Should usually NOT be called this way but
        //    as per [databind#3122] should still work
        if (_valueToUpdate != null) {
            return (JsonNode) _bind(p, _valueToUpdate);
        }

        // Need to inline `_initForReading()` due to tree reading handling end-of-input specially
        _config.initialize(p);
        if (_schema != null) {
            p.setSchema(_schema);
        }

        JsonToken t = p.currentToken();
        if (t == null) {
            t = p.nextToken();
            if (t == null) {
                return _config.getNodeFactory().missingNode();
            }
        }
        final DefaultDeserializationContext ctxt = createDeserializationContext(p);
        final JsonNode resultNode;

        if (t == JsonToken.VALUE_NULL) {
            resultNode = _config.getNodeFactory().nullNode();
        } else {
            // Will not be called for merge (need not pass _valueToUpdate)
            resultNode = (JsonNode) ctxt.readRootValue(p, _jsonNodeType(), _findTreeDeserializer(ctxt), null);
        }
        // Need to consume the token too
        p.clearCurrentToken();
        if (_config.isEnabled(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)) {
            _verifyNoTrailingTokens(p, ctxt, _jsonNodeType());
        }
        return resultNode;
    }

    /**
     * Same as {@link #_bindAsTree} except end-of-input is reported by returning
     * {@code null}, not "missing node"
     */
    protected final JsonNode _bindAsTreeOrNull(JsonParser p) throws IOException
    {
        // 16-Apr-2021, tatu: Should usually NOT be called this way but
        //    as per [databind#3122] should still work
        if (_valueToUpdate != null) {
            return (JsonNode) _bind(p, _valueToUpdate);
        }

        // Need to inline `_initForReading()` (as per above)
        _config.initialize(p);
        if (_schema != null) {
            p.setSchema(_schema);
        }
        JsonToken t = p.currentToken();
        if (t == null) {
            t = p.nextToken();
            if (t == null) {
                return null;
            }
        }
        final DefaultDeserializationContext ctxt = createDeserializationContext(p);
        final JsonNode resultNode;
        if (t == JsonToken.VALUE_NULL) {
            resultNode = _config.getNodeFactory().nullNode();
        } else {
            // Will not be called for merge (need not pass _valueToUpdate)
            resultNode = (JsonNode) ctxt.readRootValue(p, _jsonNodeType(), _findTreeDeserializer(ctxt), null);
        }
        // Need to consume the token too
        p.clearCurrentToken();
        if (_config.isEnabled(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)) {
            _verifyNoTrailingTokens(p, ctxt, _jsonNodeType());
        }
        return resultNode;
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

    /**
     * Consider filter when creating JsonParser.
     */
    protected JsonParser _considerFilter(final JsonParser p, boolean multiValue) {
        // 26-Mar-2016, tatu: Need to allow multiple-matches at least if we have
        //    have a multiple-value read (that is, "readValues()").
        return ((_filter == null) || FilteringParserDelegate.class.isInstance(p))
                ? p : new FilteringParserDelegate(p, _filter, Inclusion.ONLY_INCLUDE_ALL, multiValue);
    }

    /**
     * @since 2.9
     */
    protected final void _verifyNoTrailingTokens(JsonParser p, DeserializationContext ctxt,
            JavaType bindType)
        throws IOException
    {
        JsonToken t = p.nextToken();
        if (t != null) {
            Class<?> bt = ClassUtil.rawClass(bindType);
            if (bt == null) {
                if (_valueToUpdate != null) {
                    bt = _valueToUpdate.getClass();
                }
            }
            ctxt.reportTrailingTokens(bt, p, t);
        }
    }

    /*
    /**********************************************************
    /* Internal methods, format auto-detection
    /**********************************************************
     */

    protected Object _detectBindAndClose(byte[] src, int offset, int length) throws IOException
    {
        DataFormatReaders.Match match = _dataFormatReaders.findFormat(src, offset, length);
        if (!match.hasMatch()) {
            _reportUnkownFormat(_dataFormatReaders, match);
        }
        JsonParser p = match.createParserWithMatch();
        return match.getReader()._bindAndClose(p);
    }

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

    protected <T> MappingIterator<T> _detectBindAndReadValues(DataFormatReaders.Match match, boolean forceClosing)
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
        return match.getReader()._bindAndReadValues(p);
    }

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
    protected void _reportUnkownFormat(DataFormatReaders detector, DataFormatReaders.Match match)
        throws IOException
    {
        // 17-Aug-2015, tatu: Unfortunately, no parser/generator available so:
        throw new JsonParseException(null, "Cannot detect format from input, does not look like any of detectable formats "
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
                    throw new IllegalArgumentException("Cannot use FormatSchema of type "+schema.getClass().getName()
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

    // @since 2.12 -- needed for Deserializer pre-fetch
    protected DefaultDeserializationContext createDummyDeserializationContext() {
        return _context.createDummyInstance(_config);
    }

    protected InputStream _inputStream(URL src) throws IOException {
        return src.openStream();
    }

    protected InputStream _inputStream(File f) throws IOException {
        return new FileInputStream(f);
    }

    protected void _reportUndetectableSource(Object src) throws StreamReadException
    {
        // 17-Aug-2015, tatu: Unfortunately, no parser/generator available so:
        throw new JsonParseException(null, "Cannot use source of type "
                +src.getClass().getName()+" with format auto-detection: must be byte- not char-based");
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
        throws DatabindException
    {
        if (_rootDeserializer != null) {
            return _rootDeserializer;
        }

        // Sanity check: must have actual type...
        JavaType t = _valueType;
        if (t == null) {
            ctxt.reportBadDefinition((JavaType) null,
                    "No value type configured for ObjectReader");
        }
        // First: have we already seen it?
        JsonDeserializer<Object> deser = _rootDeserializers.get(t);
        if (deser != null) {
            return deser;
        }
        // Nope: need to ask provider to resolve it
        deser = ctxt.findRootValueDeserializer(t);
        if (deser == null) { // can this happen?
            ctxt.reportBadDefinition(t, "Cannot find a deserializer for type "+t);
        }
        _rootDeserializers.put(t, deser);
        return deser;
    }

    /**
     * @since 2.6
     */
    protected JsonDeserializer<Object> _findTreeDeserializer(DeserializationContext ctxt)
        throws DatabindException
    {
        final JavaType nodeType = _jsonNodeType();
        JsonDeserializer<Object> deser = _rootDeserializers.get(nodeType);
        if (deser == null) {
            // Nope: need to ask provider to resolve it
            deser = ctxt.findRootValueDeserializer(nodeType);
            if (deser == null) { // can this happen?
                ctxt.reportBadDefinition(nodeType,
                        "Cannot find a deserializer for type "+nodeType);
            }
            _rootDeserializers.put(nodeType, deser);
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
        if ((valueType == null) || !_config.isEnabled(DeserializationFeature.EAGER_DESERIALIZER_FETCH)) {
            return null;
        }
        // already cached?
        JsonDeserializer<Object> deser = _rootDeserializers.get(valueType);
        if (deser == null) {
            try {
                // If not, need to resolve; for which we need a temporary context as well:
                DeserializationContext ctxt = createDummyDeserializationContext();
                deser = ctxt.findRootValueDeserializer(valueType);
                if (deser != null) {
                    _rootDeserializers.put(valueType, deser);
                }
                return deser;
            } catch (JacksonException e) {
                // need to swallow?
            }
        }
        return deser;
    }

    /**
     * @since 2.10
     */
    protected final JavaType _jsonNodeType() {
        JavaType t = _jsonNodeType;
        if (t == null) {
            t = getTypeFactory().constructType(JsonNode.class);
            _jsonNodeType = t;
        }
        return t;
    }

    protected final void _assertNotNull(String paramName, Object src) {
        if (src == null) {
            throw new IllegalArgumentException(String.format("argument \"%s\" is null", paramName));
        }
    }
}
