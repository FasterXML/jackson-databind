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
import com.fasterxml.jackson.databind.deser.DefaultDeserializationContext;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;
import com.fasterxml.jackson.databind.type.SimpleType;
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
 * NOTE: this class is NOT meant as sub-classable by users. It is left as
 * non-final mostly to allow frameworks  that require bytecode generation for proxying
 * and similar use cases, but there is no expectation that functionality
 * should be extended by sub-classing.
 */
public class ObjectReader
    implements Versioned, java.io.Serializable
{
    private static final long serialVersionUID = 3L;

    protected final static JavaType JSON_NODE_TYPE = SimpleType.constructUnsafe(JsonNode.class);

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
     * Factory used for constructing {@link JsonParser}s
     */
    protected final TokenStreamFactory _parserFactory;

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
        _schema = schema;
        _injectableValues = injectableValues;
        _unwrapRoot = config.useRootWrapping();

        _rootDeserializer = _prefetchRootDeserializer(valueType);
        _filter = null;
    }
    
    /**
     * Copy constructor used for building variations.
     */
    protected ObjectReader(ObjectReader base, DeserializationConfig config,
            JavaType valueType, JsonDeserializer<Object> rootDeser, Object valueToUpdate,
            FormatSchema schema, InjectableValues injectableValues)
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
        _filter = base._filter;
    }
    
    protected ObjectReader(ObjectReader base, TokenStreamFactory f)
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
     * Factory method called by various "withXxx()" methods
     */
    protected ObjectReader _new(ObjectReader base, DeserializationConfig config) {
        return new ObjectReader(base, config);
    }

    /**
     * Factory method called by various "withXxx()" methods
     */
    protected ObjectReader _new(ObjectReader base, DeserializationConfig config,
            JavaType valueType, JsonDeserializer<Object> rootDeser, Object valueToUpdate,
            FormatSchema schema, InjectableValues injectableValues) {
        return new ObjectReader(base, config, valueType, rootDeser,  valueToUpdate,
                 schema,  injectableValues);
    }

    /**
     * Factory method used to create {@link MappingIterator} instances;
     * either default, or custom subtype.
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

    protected JsonToken _initForReading(DefaultDeserializationContext ctxt, JsonParser p)
        throws IOException
    {
        ctxt.assignParser(p);

        // First: must point to a token; if not pointing to one, advance.
        // This occurs before first read from JsonParser, as well as
        // after clearing of current token.
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
     */
    protected void _initForMultiRead(DefaultDeserializationContext ctxt, JsonParser p)
        throws IOException
    {
        ctxt.assignParser(p);
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
     */
    public ObjectReader with(FormatFeature feature) {
        return _with(_config.with(feature));
    }

    /**
     * Method for constructing a new reader instance that is configured
     * with specified features enabled.
     */
    public ObjectReader withFeatures(FormatFeature... features) {
        return _with(_config.withFeatures(features));
    }    

    /**
     * Method for constructing a new reader instance that is configured
     * with specified feature disabled.
     */
    public ObjectReader without(FormatFeature feature) {
        return _with(_config.without(feature)); 
    }

    /**
     * Method for constructing a new reader instance that is configured
     * with specified features disabled.
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
     */
    public ObjectReader at(String value) {
        return new ObjectReader(this, new JsonPointerBasedFilter(value));
    }

    /**
     * Convenience method to bind from {@link JsonPointer}
      * {@link JsonPointerBasedFilter} is registered and will be used for parsing later.
     */
    public ObjectReader at(JsonPointer pointer) {
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
                _schema, injectableValues);
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
                schema, _injectableValues);
    }

    /**
     * Method for constructing a new reader instance that is configured
     * to data bind into specified type.
     *<p>
     * Note that the method does NOT change state of this reader, but
     * rather construct and returns a newly configured instance.
     */
    public ObjectReader forType(JavaType valueType)
    {
        if (valueType != null && valueType.equals(_valueType)) {
            return this;
        }
        JsonDeserializer<Object> rootDeser = _prefetchRootDeserializer(valueType);
        return _new(this, _config, valueType, rootDeser,
                _valueToUpdate, _schema, _injectableValues);
    }    

    /**
     * Method for constructing a new reader instance that is configured
     * to data bind into specified type.
     *<p>
     * Note that the method does NOT change state of this reader, but
     * rather construct and returns a newly configured instance.
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
     */
    public ObjectReader forType(TypeReference<?> valueTypeRef) {
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
                    _schema, _injectableValues);
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
                _schema, _injectableValues);
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

    public ObjectReader with(ContextAttributes attrs) {
        return _with(_config.with(attrs));
    }

    public ObjectReader withAttributes(Map<?,?> attrs) {
        return _with(_config.withAttributes(attrs));
    }

    public ObjectReader withAttribute(Object key, Object value) {
        return _with( _config.withAttribute(key, value));
    }

    public ObjectReader withoutAttribute(Object key) {
        return _with(_config.withoutAttribute(key));
    }

    /*
    /**********************************************************
    /* Internal factory methods
    /**********************************************************
     */
    
    protected final ObjectReader _with(DeserializationConfig newConfig) {
        if (newConfig == _config) {
            return this;
        }
        return _new(this, newConfig);
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
        // !!! 09-Oct-2017, tatu: Actually for full answer we really should check
        //   what actual combined settings are....
        return _parserFactory.isEnabled(f);
    }

    public DeserializationConfig getConfig() {
        return _config;
    }

    /**
     * @since 3.0
     */
    public TokenStreamFactory parserFactory() {
        return _parserFactory;
    }

    /**
     * @since 3.0
     */
    public TypeFactory typeFactory() {
        return _config.getTypeFactory();
    }

    public ContextAttributes getAttributes() {
        return _config.getAttributes();
    }

    public InjectableValues getInjectableValues() {
        return _injectableValues;
    }

    /**
     * @deprecated Since 3.0 use {@link #parserFactory}
     */
    @Deprecated
    public TokenStreamFactory getFactory() {
        return parserFactory();
    }

    /**
     * @deprecated Since 3.0 use {@link #typeFactory}
     */
    @Deprecated
    public TypeFactory getTypeFactory() {
        return typeFactory();
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
        return ctxt.assignAndReturnParser(_parserFactory.createParser(ctxt, src));
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
        return ctxt.assignAndReturnParser(_parserFactory.createParser(ctxt, src));
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
        return ctxt.assignAndReturnParser(_parserFactory.createParser(ctxt, in));
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
        return ctxt.assignAndReturnParser(_parserFactory.createParser(ctxt, r));
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
        return ctxt.assignAndReturnParser(_parserFactory.createParser(ctxt, data));
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
        return ctxt.assignAndReturnParser(_parserFactory.createParser(ctxt, data, offset, len));
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
        return ctxt.assignAndReturnParser(_parserFactory.createParser(ctxt, content));
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
        return ctxt.assignAndReturnParser(_parserFactory.createParser(ctxt, content));
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
        return ctxt.assignAndReturnParser(_parserFactory.createParser(ctxt, content, offset, len));
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
        return ctxt.assignAndReturnParser(_parserFactory.createParser(ctxt, content));
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
        return ctxt.assignAndReturnParser(_parserFactory.createNonBlockingByteArrayParser(ctxt));
    }
    
    /*
    /**********************************************************
    /* Convenience methods for JsonNode creation
    /**********************************************************
     */

    public ObjectNode createObjectNode() {
        return _config.getNodeFactory().objectNode();
    }

    public ArrayNode createArrayNode() {
        return _config.getNodeFactory().arrayNode();
    }

    /*
    /**********************************************************
    /* Deserialization methods; first ones for pre-constructed
    /* parsers
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
        DefaultDeserializationContext ctxt = createDeserializationContext(p);
        return (T) _bind(ctxt, p, _valueToUpdate);
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
    public <T> T readValue(JsonParser p, TypeReference<T> valueTypeRef) throws IOException
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
    @SuppressWarnings("unchecked")
    public <T> T readValue(JsonParser p, ResolvedType valueType) throws IOException {
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
    public <T> Iterator<T> readValues(JsonParser p, TypeReference<T> valueTypeRef) throws IOException {
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

    public JsonParser treeAsTokens(TreeNode n) {
        return treeAsTokens((JsonNode) n, createDeserializationContext());
    }

    protected JsonParser treeAsTokens(JsonNode n, DeserializationContext ctxt) {
        return new TreeTraversingParser(n, ctxt);
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
    public <T extends TreeNode> T readTree(JsonParser p) throws IOException {
        return (T) _bindAsTree(createDeserializationContext(p), p);
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
    public <T> T readValue(InputStream src) throws IOException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _bindAndClose(ctxt,
                _considerFilter(_parserFactory.createParser(ctxt, src), false));
    }

    /**
     * Method that binds content read from given input source,
     * using configuration of this reader.
     * Value return is either newly constructed, or root value that
     * was specified with {@link #withValueToUpdate(Object)}.
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(Reader src) throws IOException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _bindAndClose(ctxt,
                _considerFilter(_parserFactory.createParser(ctxt, src), false));
    }

    /**
     * Method that binds content read from given JSON string,
     * using configuration of this reader.
     * Value return is either newly constructed, or root value that
     * was specified with {@link #withValueToUpdate(Object)}.
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(String src) throws IOException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _bindAndClose(ctxt,
                _considerFilter(_parserFactory.createParser(ctxt, src), false));
    }

    /**
     * Method that binds content read from given byte array,
     * using configuration of this reader.
     * Value return is either newly constructed, or root value that
     * was specified with {@link #withValueToUpdate(Object)}.
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(byte[] src) throws IOException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _bindAndClose(ctxt,
                _considerFilter(_parserFactory.createParser(ctxt, src), false));
    }

    /**
     * Method that binds content read from given byte array,
     * using configuration of this reader.
     * Value return is either newly constructed, or root value that
     * was specified with {@link #withValueToUpdate(Object)}.
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(byte[] src, int offset, int length) throws IOException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _bindAndClose(ctxt,
                _considerFilter(_parserFactory.createParser(ctxt, src, offset, length), false));
    }
    
    @SuppressWarnings("unchecked")
    public <T> T readValue(File src) throws IOException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _bindAndClose(ctxt,
                _considerFilter(_parserFactory.createParser(ctxt, src), false));
    }

    /**
     * Method that binds content read from given input source,
     * using configuration of this reader.
     * Value return is either newly constructed, or root value that
     * was specified with {@link #withValueToUpdate(Object)}.
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(URL src) throws IOException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _bindAndClose(ctxt,
                _considerFilter(_parserFactory.createParser(ctxt, src), false));
    }

    /**
     * Convenience method for converting results from given JSON tree into given
     * value type. Basically short-cut for:
     *<pre>
     *   objectReader.readValue(src.traverse())
     *</pre>
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(JsonNode src) throws IOException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _bindAndClose(ctxt,
                _considerFilter(treeAsTokens(src, ctxt), false));
    }

    @SuppressWarnings("unchecked")
    public <T> T readValue(DataInput src) throws IOException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _bindAndClose(ctxt,
                _considerFilter(_parserFactory.createParser(ctxt, src), false));
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
    public JsonNode readTree(InputStream in) throws IOException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return _bindAndCloseAsTree(ctxt,
                _considerFilter(_parserFactory.createParser(ctxt, in), false));
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
    public JsonNode readTree(Reader r) throws IOException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return _bindAndCloseAsTree(ctxt,
                _considerFilter(_parserFactory.createParser(ctxt, r), false));
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
    public JsonNode readTree(String json) throws IOException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return _bindAndCloseAsTree(ctxt,
                _considerFilter(_parserFactory.createParser(ctxt, json), false));
    }

    public JsonNode readTree(DataInput src) throws IOException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return _bindAndCloseAsTree(ctxt,
                _considerFilter(_parserFactory.createParser(ctxt, src), false));
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
        throws IOException
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
    public <T> MappingIterator<T> readValues(InputStream src) throws IOException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return _bindAndReadValues(ctxt,
                _considerFilter(_parserFactory.createParser(ctxt, src), true));
    }
    
    /**
     * Overloaded version of {@link #readValue(InputStream)}.
     */
    @SuppressWarnings("resource")
    public <T> MappingIterator<T> readValues(Reader src) throws IOException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        JsonParser p = _considerFilter(_parserFactory.createParser(ctxt, src), true);
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
    public <T> MappingIterator<T> readValues(String json) throws IOException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        JsonParser p = _considerFilter(_parserFactory.createParser(ctxt, json), true);
        _initForMultiRead(ctxt, p);
        p.nextToken();
        return _newIterator(p, ctxt, _findRootDeserializer(ctxt), true);
    }

    /**
     * Overloaded version of {@link #readValue(InputStream)}.
     */
    public <T> MappingIterator<T> readValues(byte[] src, int offset, int length) throws IOException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return _bindAndReadValues(ctxt,
                _considerFilter(_parserFactory.createParser(ctxt, src, offset, length), true));
    }

    /**
     * Overloaded version of {@link #readValue(InputStream)}.
     */
    public final <T> MappingIterator<T> readValues(byte[] src) throws IOException {
        return readValues(src, 0, src.length);
    }

    /**
     * Overloaded version of {@link #readValue(InputStream)}.
     */
    public <T> MappingIterator<T> readValues(File src) throws IOException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return _bindAndReadValues(ctxt,
                _considerFilter(_parserFactory.createParser(ctxt, src), true));
    }

    /**
     * Overloaded version of {@link #readValue(InputStream)}.
     * 
     * @param src URL to read to access JSON content to parse.
     */
    public <T> MappingIterator<T> readValues(URL src) throws IOException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return _bindAndReadValues(ctxt,
                _considerFilter(_parserFactory.createParser(ctxt, src), true));
    }

    public <T> MappingIterator<T> readValues(DataInput src) throws IOException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return _bindAndReadValues(ctxt,
                _considerFilter(_parserFactory.createParser(ctxt, src), true));
    }

    /*
    /**********************************************************
    /* Implementation of rest of ObjectCodec methods
    /**********************************************************
     */

    public <T> T treeToValue(TreeNode n, Class<T> valueType) throws JsonProcessingException
    {
        try {
            return readValue(treeAsTokens(n), valueType);
        } catch (JsonProcessingException e) {
            throw e;
        } catch (IOException e) { // should not occur, no real i/o...
            throw JsonMappingException.fromUnexpectedIOE(e);
        }
    }

    /*
    /**********************************************************
    /* Helper methods, data-binding
    /**********************************************************
     */

    /**
     * Actual implementation of value reading+binding operation.
     */
    protected Object _bind(DefaultDeserializationContext ctxt,
            JsonParser p, Object valueToUpdate) throws IOException
    {
        /* First: may need to read the next token, to initialize state (either
         * before first read from parser, or after previous token has been cleared)
         */
        Object result;
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
                    // 20-Mar-2017, tatu: Important! May be different from `valueToUpdate`
                    //   for immutable Objects like Java arrays; logical result
                    result = deser.deserialize(p, ctxt, valueToUpdate);
                }
            }
        }
        // Need to consume the token too
        p.clearCurrentToken();
        if (_config.isEnabled(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)) {
            _verifyNoTrailingTokens(p, ctxt, _valueType);
        }
        return result;
    }

    protected Object _bindAndClose(DefaultDeserializationContext ctxt,
            JsonParser p0) throws IOException
    {
        try (JsonParser p = p0) {
            Object result;

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
            if (_config.isEnabled(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)) {
                _verifyNoTrailingTokens(p, ctxt, _valueType);
            }
            return result;
        }
    }

    protected final JsonNode _bindAndCloseAsTree(DefaultDeserializationContext ctxt,
            JsonParser p0) throws IOException {
        try (JsonParser p = p0) {
            return _bindAsTree(ctxt, p);
        }
    }

    protected final JsonNode _bindAsTree(DefaultDeserializationContext ctxt,
            JsonParser p) throws IOException
    {
        JsonToken t = p.currentToken();
        if (t == null) {
            t = p.nextToken();
            if (t == null) { // [databind#1406]: expose end-of-input as `null`
                return null;
            }
        }
        Object result;
        if (t == JsonToken.VALUE_NULL) {
            result = ctxt.getNodeFactory().nullNode();
        } else {
            JsonDeserializer<Object> deser = _findTreeDeserializer(ctxt);
            if (_unwrapRoot) {
                // NOTE: will do "check if trailing" check in call
                return (JsonNode) _unwrapAndDeserialize(p, ctxt, JSON_NODE_TYPE, deser);
            }
            result = deser.deserialize(p, ctxt);
        }
        if (_config.isEnabled(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)) {
            _verifyNoTrailingTokens(p, ctxt, JSON_NODE_TYPE);
        }
        return (JsonNode) result;
    }

    protected <T> MappingIterator<T> _bindAndReadValues(DefaultDeserializationContext ctxt,
            JsonParser p) throws IOException
    {
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
        Object result;
        if (_valueToUpdate == null) {
            result = deser.deserialize(p, ctxt);
        } else {
            deser.deserialize(p, ctxt, _valueToUpdate);
            result = _valueToUpdate;                    
        }
        // and last, verify that we now get matching END_OBJECT
        if (p.nextToken() != JsonToken.END_OBJECT) {
            ctxt.reportWrongTokenException(rootType, JsonToken.END_OBJECT,
                    "Current token not END_OBJECT (to match wrapper object with root name '%s'), but %s",
                    expSimpleName, p.currentToken());
        }
        if (_config.isEnabled(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)) {
            _verifyNoTrailingTokens(p, ctxt, _valueType);
        }
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
    /* Internal methods, other
    /**********************************************************
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
    protected DefaultDeserializationContext createDeserializationContext() {
        return _context.createInstance(_config, _schema, _injectableValues);
    }

    protected DefaultDeserializationContext createDeserializationContext(JsonParser p) {
        return _context.createInstance(_config, _schema, _injectableValues)
                .assignParser(p);
    }
    
    protected InputStream _inputStream(URL src) throws IOException {
        return src.openStream();
    }

    protected InputStream _inputStream(File f) throws IOException {
        return new FileInputStream(f);
    }

    protected void _reportUndetectableSource(Object src) throws JsonProcessingException
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
        throws JsonMappingException
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

    protected JsonDeserializer<Object> _findTreeDeserializer(DeserializationContext ctxt)
        throws JsonMappingException
    {
        JsonDeserializer<Object> deser = _rootDeserializers.get(JSON_NODE_TYPE);
        if (deser == null) {
            // Nope: need to ask provider to resolve it
            deser = ctxt.findRootValueDeserializer(JSON_NODE_TYPE);
            if (deser == null) { // can this happen?
                ctxt.reportBadDefinition(JSON_NODE_TYPE,
                        "Cannot find a deserializer for type "+JSON_NODE_TYPE);
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
        if ((valueType == null) || !_config.isEnabled(DeserializationFeature.EAGER_DESERIALIZER_FETCH)) {
            return null;
        }
        // already cached?
        JsonDeserializer<Object> deser = _rootDeserializers.get(valueType);
        if (deser == null) {
            try {
                // If not, need to resolve; for which we need a temporary context as well:
                DeserializationContext ctxt = createDeserializationContext();
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
