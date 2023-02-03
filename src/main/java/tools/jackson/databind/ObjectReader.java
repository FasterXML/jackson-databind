package tools.jackson.databind;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import tools.jackson.core.*;
import tools.jackson.core.exc.WrappedIOException;
import tools.jackson.core.filter.FilteringParserDelegate;
import tools.jackson.core.filter.JsonPointerBasedFilter;
import tools.jackson.core.filter.TokenFilter;
import tools.jackson.core.filter.TokenFilter.Inclusion;
import tools.jackson.core.type.ResolvedType;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.cfg.ContextAttributes;
import tools.jackson.databind.cfg.DatatypeFeature;
import tools.jackson.databind.cfg.DeserializationContexts;
import tools.jackson.databind.deser.DeserializationContextExt;
import tools.jackson.databind.deser.DeserializationProblemHandler;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.TreeTraversingParser;
import tools.jackson.databind.type.SimpleType;
import tools.jackson.databind.type.TypeFactory;
import tools.jackson.databind.util.ClassUtil;

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
 * non-final mostly to allow frameworks  that require byte code generation for proxying
 * and similar use cases, but there is no expectation that functionality
 * should be extended by sub-classing.
 */
public class ObjectReader
    implements Versioned, TreeCodec

    // NOTE: since 3.x, NO LONGER JDK Serializable
{
    protected final static JavaType JSON_NODE_TYPE = SimpleType.constructUnsafe(JsonNode.class);

    /*
    /**********************************************************************
    /* Immutable configuration from ObjectMapper
    /**********************************************************************
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
    protected final DeserializationContexts _contexts;

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
    /**********************************************************************
    /* Configuration that can be changed during building
    /**********************************************************************
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
    protected final ValueDeserializer<Object> _rootDeserializer;

    /**
     * Instance to update with data binding; if any. If null,
     * a new instance is created, if non-null, properties of
     * this value object will be updated instead.
     * Note that value can be of almost any type, except not
     * {@link tools.jackson.databind.type.ArrayType}; array
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
    /**********************************************************************
    /* Caching
    /**********************************************************************
     */

    /**
     * Root-level cached deserializers.
     * Passed by {@link ObjectMapper}, shared with it.
     */
    final protected ConcurrentHashMap<JavaType, ValueDeserializer<Object>> _rootDeserializers;

    /*
    /**********************************************************************
    /* Life-cycle, construction
    /**********************************************************************
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
        _contexts = mapper._deserializationContexts;
        _rootDeserializers = mapper._rootDeserializers;
        _parserFactory = mapper._streamFactory;
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
            JavaType valueType, ValueDeserializer<Object> rootDeser, Object valueToUpdate,
            FormatSchema schema, InjectableValues injectableValues)
    {
        _config = config;
        _contexts = base._contexts;

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
        _contexts = base._contexts;

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

    protected ObjectReader(ObjectReader base, TokenFilter filter) {
        _config = base._config;
        _contexts = base._contexts;
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
        return tools.jackson.databind.cfg.PackageVersion.VERSION;
    }

    /*
    /**********************************************************************
    /* Helper methods used internally for invoking constructors
    /* Need to be overridden if sub-classing (not recommended)
    /* is used.
    /**********************************************************************
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
            JavaType valueType, ValueDeserializer<Object> rootDeser, Object valueToUpdate,
            FormatSchema schema, InjectableValues injectableValues) {
        return new ObjectReader(base, config, valueType, rootDeser,  valueToUpdate,
                 schema,  injectableValues);
    }

    /**
     * Factory method used to create {@link MappingIterator} instances;
     * either default, or custom subtype.
     */
    protected <T> MappingIterator<T> _newIterator(JsonParser p, DeserializationContext ctxt,
            ValueDeserializer<?> deser, boolean parserManaged)
    {
        return new MappingIterator<T>(_valueType, p, ctxt,
                deser, parserManaged, _valueToUpdate);
    }

    /*
    /**********************************************************************
    /* Methods for initializing parser instance to use
    /**********************************************************************
     */

    protected JsonToken _initForReading(DeserializationContextExt ctxt, JsonParser p)
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
    protected void _initForMultiRead(DeserializationContextExt ctxt, JsonParser p)
    {
        ctxt.assignParser(p);
    }

    /*
    /**********************************************************************
    /* Life-cycle, fluent factory methods for DeserializationFeatures
    /**********************************************************************
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
    /* Life-cycle, fluent factory methods for DatatypeFeatures
    /**********************************************************************
     */

    /**
     * Method for constructing a new reader instance that is configured
     * with specified feature enabled.
     */
    public ObjectReader with(DatatypeFeature feature) {
        return _with(_config.with(feature));
    }

    /**
     * Method for constructing a new reader instance that is configured
     * with specified features enabled.
     */
    public ObjectReader withFeatures(DatatypeFeature... features) {
        return _with(_config.withFeatures(features));
    }

    /**
     * Method for constructing a new reader instance that is configured
     * with specified feature disabled.
     */
    public ObjectReader without(DatatypeFeature feature) {
        return _with(_config.without(feature));
    }

    /**
     * Method for constructing a new reader instance that is configured
     * with specified features disabled.
     */
    public ObjectReader withoutFeatures(DatatypeFeature... features) {
        return _with(_config.withoutFeatures(features));
    }

    /*
    /**********************************************************
    /* Life-cycle, fluent factory methods for StreamReadFeatures
    /**********************************************************
     */

    /**
     * Method for constructing a new reader instance that is configured
     * with specified feature enabled.
     */
    public ObjectReader with(StreamReadFeature feature) {
        return _with(_config.with(feature));
    }

    /**
     * Method for constructing a new reader instance that is configured
     * with specified features enabled.
     */
    public ObjectReader withFeatures(StreamReadFeature... features) {
        return _with(_config.withFeatures(features));
    }

    /**
     * Method for constructing a new reader instance that is configured
     * with specified feature disabled.
     */
    public ObjectReader without(StreamReadFeature feature) {
        return _with(_config.without(feature));
    }

    /**
     * Method for constructing a new reader instance that is configured
     * with specified features disabled.
     */
    public ObjectReader withoutFeatures(StreamReadFeature... features) {
        return _with(_config.withoutFeatures(features));
    }

    /*
    /**********************************************************************
    /* Life-cycle, fluent factory methods for FormatFeature
    /**********************************************************************
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
    /**********************************************************************
    /* Life-cycle, fluent factory methods, other
    /**********************************************************************
     */

    /**
     * Convenience method to bind from {@link JsonPointer}.
     * {@link JsonPointerBasedFilter} is registered and will be used for parsing later.
     */
    public ObjectReader at(String pointerExpr) {
        _assertNotNull("pointerExpr", pointerExpr);
        return new ObjectReader(this, new JsonPointerBasedFilter(pointerExpr));
    }

    /**
     * Convenience method to bind from {@link JsonPointer}
      * {@link JsonPointerBasedFilter} is registered and will be used for parsing later.
     */
    public ObjectReader at(JsonPointer pointer) {
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
        ValueDeserializer<Object> rootDeser = _prefetchRootDeserializer(valueType);
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

    /**
     * Mutant factory for overriding set of (default) attributes for
     * {@link ObjectReader} to use.
     *<p>
     * Note that this will replace defaults passed by {@link ObjectMapper}.
     *
     * @param attrs Default {@link ContextAttributes} to use with a reader
     *
     * @return {@link ObjectReader} instance with specified default attributes (which
     *    is usually a newly constructed reader instance with otherwise identical settings)
     */
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
    /**********************************************************************
    /* Internal factory methods
    /**********************************************************************
     */

    protected final ObjectReader _with(DeserializationConfig newConfig) {
        if (newConfig == _config) {
            return this;
        }
        return _new(this, newConfig);
    }

    /*
    /**********************************************************************
    /* Simple accessors
    /**********************************************************************
     */

    public boolean isEnabled(DeserializationFeature f) {
        return _config.isEnabled(f);
    }

    public boolean isEnabled(MapperFeature f) {
        return _config.isEnabled(f);
    }

    public boolean isEnabled(DatatypeFeature f) {
        return _config.isEnabled(f);
    }

    public boolean isEnabled(StreamReadFeature f) {
        return _config.isEnabled(f);
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

    public JavaType getValueType() {
        return _valueType;
    }

    /**
     * @deprecated Since 3.0 use {@link #typeFactory}
     */
    @Deprecated
    public TypeFactory getTypeFactory() {
        return typeFactory();
    }

    /*
    /**********************************************************************
    /* Public API: constructing Parsers that are properly linked
    /* to `ObjectReadContext`
    /**********************************************************************
     */

    /**
     * Factory method for constructing {@link JsonParser} that is properly
     * wired to allow callbacks for deserialization: basically
     * constructs a {@link ObjectReadContext} and then calls
     * {@link TokenStreamFactory#createParser(ObjectReadContext,java.io.File)}.
     *
     * @since 3.0
     */
    public JsonParser createParser(File src) throws JacksonException {
        _assertNotNull("src", src);
        DeserializationContextExt ctxt = _deserializationContext();
        return ctxt.assignAndReturnParser(_parserFactory.createParser(ctxt, src));
    }

    /**
     * Factory method for constructing {@link JsonParser} that is properly
     * wired to allow callbacks for deserialization: basically
     * constructs a {@link ObjectReadContext} and then calls
     * {@link TokenStreamFactory#createParser(ObjectReadContext,Path)}.
     *
     * @since 3.0
     */
    public JsonParser createParser(Path src) throws JacksonException {
        _assertNotNull("src", src);
        DeserializationContextExt ctxt = _deserializationContext();
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
    public JsonParser createParser(URL src) throws JacksonException {
        _assertNotNull("src", src);
        DeserializationContextExt ctxt = _deserializationContext();
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
    public JsonParser createParser(InputStream src) throws JacksonException {
        _assertNotNull("src", src);
        DeserializationContextExt ctxt = _deserializationContext();
        return ctxt.assignAndReturnParser(_parserFactory.createParser(ctxt, src));
    }

    /**
     * Factory method for constructing {@link JsonParser} that is properly
     * wired to allow callbacks for deserialization: basically
     * constructs a {@link ObjectReadContext} and then calls
     * {@link TokenStreamFactory#createParser(ObjectReadContext,Reader)}.
     *
     * @since 3.0
     */
    public JsonParser createParser(Reader src) throws JacksonException {
        _assertNotNull("src", src);
        DeserializationContextExt ctxt = _deserializationContext();
        return ctxt.assignAndReturnParser(_parserFactory.createParser(ctxt, src));
    }

    /**
     * Factory method for constructing {@link JsonParser} that is properly
     * wired to allow callbacks for deserialization: basically
     * constructs a {@link ObjectReadContext} and then calls
     * {@link TokenStreamFactory#createParser(ObjectReadContext,byte[])}.
     *
     * @since 3.0
     */
    public JsonParser createParser(byte[] content) throws JacksonException {
        _assertNotNull("content", content);
        DeserializationContextExt ctxt = _deserializationContext();
        return ctxt.assignAndReturnParser(_parserFactory.createParser(ctxt, content));
    }

    /**
     * Factory method for constructing {@link JsonParser} that is properly
     * wired to allow callbacks for deserialization: basically
     * constructs a {@link ObjectReadContext} and then calls
     * {@link TokenStreamFactory#createParser(ObjectReadContext,byte[],int,int)}.
     *
     * @since 3.0
     */
    public JsonParser createParser(byte[] content, int offset, int len) throws JacksonException {
        _assertNotNull("content", content);
        DeserializationContextExt ctxt = _deserializationContext();
        return ctxt.assignAndReturnParser(_parserFactory.createParser(ctxt, content, offset, len));
    }

    /**
     * Factory method for constructing {@link JsonParser} that is properly
     * wired to allow callbacks for deserialization: basically
     * constructs a {@link ObjectReadContext} and then calls
     * {@link TokenStreamFactory#createParser(ObjectReadContext,String)}.
     *
     * @since 3.0
     */
    public JsonParser createParser(String content) throws JacksonException {
        _assertNotNull("content", content);
        DeserializationContextExt ctxt = _deserializationContext();
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
    public JsonParser createParser(char[] content) throws JacksonException {
        _assertNotNull("content", content);
        DeserializationContextExt ctxt = _deserializationContext();
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
    public JsonParser createParser(char[] content, int offset, int len) throws JacksonException {
        _assertNotNull("content", content);
        DeserializationContextExt ctxt = _deserializationContext();
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
    public JsonParser createParser(DataInput content) throws JacksonException {
        _assertNotNull("content", content);
        DeserializationContextExt ctxt = _deserializationContext();
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
    public JsonParser createNonBlockingByteArrayParser() throws JacksonException {
        DeserializationContextExt ctxt = _deserializationContext();
        return ctxt.assignAndReturnParser(_parserFactory.createNonBlockingByteArrayParser(ctxt));
    }

    /*
    /**********************************************************************
    /* TreeCodec implementation
    /**********************************************************************
     */

    @Override
    public ObjectNode createObjectNode() {
        return _config.getNodeFactory().objectNode();
    }

    @Override
    public ArrayNode createArrayNode() {
        return _config.getNodeFactory().arrayNode();
    }

    @Override
    public JsonNode booleanNode(boolean b) {
        return _config.getNodeFactory().booleanNode(b);
    }

    @Override
    public JsonNode stringNode(String text) {
        return _config.getNodeFactory().textNode(text);
    }

    @Override
    public JsonNode missingNode() {
        return _config.getNodeFactory().missingNode();
    }

    @Override
    public JsonNode nullNode() {
        return _config.getNodeFactory().nullNode();
    }

    @Override
    public JsonParser treeAsTokens(TreeNode n) {
        _assertNotNull("n", n);
        return treeAsTokens((JsonNode) n, _deserializationContext());
    }

    protected JsonParser treeAsTokens(JsonNode n, DeserializationContext ctxt) {
        _assertNotNull("n", n);
        return new TreeTraversingParser(n, ctxt);
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
     */
    @SuppressWarnings("unchecked")
    @Override
    public JsonNode readTree(JsonParser p) throws JacksonException {
        _assertNotNull("p", p);
        return _bindAsTreeOrNull(_deserializationContext(p), p);
    }

    // Alas, can't really support this part...
    @Override
    public void writeTree(JsonGenerator g, TreeNode tree) {
        throw new UnsupportedOperationException();
    }

    /*
    /**********************************************************************
    /* Deserialization methods; first ones for pre-constructed parsers
    /**********************************************************************
     */

    /**
     * Method that binds content read using given parser, using
     * configuration of this reader, including expected result type.
     * Value return is either newly constructed, or root value that
     * was specified with {@link #withValueToUpdate(Object)}.
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(JsonParser p) throws JacksonException {
        _assertNotNull("p", p);
        DeserializationContextExt ctxt = _deserializationContext(p);
        return (T) _bind(ctxt, p, _valueToUpdate);
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
     */
    public <T> Iterator<T> readValues(JsonParser p, Class<T> valueType)
        throws JacksonException
    {
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
     */
    public <T> Iterator<T> readValues(JsonParser p, TypeReference<T> valueTypeRef)
        throws JacksonException
    {
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
     */
    public <T> Iterator<T> readValues(JsonParser p, ResolvedType valueType) throws JacksonException {
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
     */
    public <T> Iterator<T> readValues(JsonParser p, JavaType valueType) throws JacksonException {
        _assertNotNull("p", p);
        return forType(valueType).readValues(p);
    }

    /*
    /**********************************************************************
    /* Deserialization methods; others similar to what ObjectMapper has
    /**********************************************************************
     */

    /**
     * Method that binds content read from given input source,
     * using configuration of this reader.
     * Value return is either newly constructed, or root value that
     * was specified with {@link #withValueToUpdate(Object)}.
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(InputStream in) throws JacksonException
    {
        _assertNotNull("in", in);
        DeserializationContextExt ctxt = _deserializationContext();
        return (T) _bindAndClose(ctxt,
                _considerFilter(_parserFactory.createParser(ctxt, in), false));
    }

    /**
     * Method that binds content read from given input source,
     * using configuration of this reader.
     * Value return is either newly constructed, or root value that
     * was specified with {@link #withValueToUpdate(Object)}.
     *
     * @param r Source to read content from
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(Reader r) throws JacksonException
    {
        _assertNotNull("r", r);
        DeserializationContextExt ctxt = _deserializationContext();
        return (T) _bindAndClose(ctxt,
                _considerFilter(_parserFactory.createParser(ctxt, r), false));
    }

    /**
     * Method that binds content read from given JSON string,
     * using configuration of this reader.
     * Value return is either newly constructed, or root value that
     * was specified with {@link #withValueToUpdate(Object)}.
     *
     * @param content String that contains content to read
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(String content) throws JacksonException
    {
        _assertNotNull("content", content);
        DeserializationContextExt ctxt = _deserializationContext();
        return (T) _bindAndClose(ctxt,
                _considerFilter(_parserFactory.createParser(ctxt, content), false));
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
    public <T> T readValue(byte[] content) throws JacksonException
    {
        _assertNotNull("content", content);
        DeserializationContextExt ctxt = _deserializationContext();
        return (T) _bindAndClose(ctxt,
                _considerFilter(_parserFactory.createParser(ctxt, content), false));
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
    public <T> T readValue(byte[] buffer, int offset, int length) throws JacksonException
    {
        _assertNotNull("buffer", buffer);
        DeserializationContextExt ctxt = _deserializationContext();
        return (T) _bindAndClose(ctxt,
                _considerFilter(_parserFactory.createParser(ctxt, buffer, offset, length), false));
    }

    /**
     * Method that binds content read from given {@link File}
     * using configuration of this reader.
     * Value return is either newly constructed, or root value that
     * was specified with {@link #withValueToUpdate(Object)}.
     *
     * @param f File that contains content to read
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(File f) throws JacksonException
    {
        _assertNotNull("f", f);
        DeserializationContextExt ctxt = _deserializationContext();
        return (T) _bindAndClose(ctxt,
                _considerFilter(_parserFactory.createParser(ctxt, f), false));
    }

    /**
     * Method that binds content read from given {@link Path}
     * using configuration of this reader.
     * Value return is either newly constructed, or root value that
     * was specified with {@link #withValueToUpdate(Object)}.
     *
     * @param path Path that contains content to read
     *
     * @since 3.0
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(Path path) throws JacksonException
    {
        _assertNotNull("path", path);
        DeserializationContextExt ctxt = _deserializationContext();
        return (T) _bindAndClose(ctxt,
                _considerFilter(_parserFactory.createParser(ctxt, path), false));
    }

    /**
     * Method that binds content read from given input source,
     * using configuration of this reader.
     * Value return is either newly constructed, or root value that
     * was specified with {@link #withValueToUpdate(Object)}.
     *<p>
     *<p>
     * NOTE: handling of {@link java.net.URL} is delegated to
     * {@link TokenStreamFactory#createParser(ObjectReadContext, java.net.URL)} and usually simply
     * calls {@link java.net.URL#openStream()}, meaning no special handling
     * is done. If different HTTP connection options are needed you will need
     * to create {@link java.io.InputStream} separately.
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(URL url) throws JacksonException
    {
        _assertNotNull("src", url);
        DeserializationContextExt ctxt = _deserializationContext();
        return (T) _bindAndClose(ctxt,
                _considerFilter(_parserFactory.createParser(ctxt, url), false));
    }

    /**
     * Convenience method for converting results from given JSON tree into given
     * value type. Basically short-cut for:
     *<pre>
     *   objectReader.readValue(src.traverse())
     *</pre>
     *
     * @param node Tree that contains content to convert
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(JsonNode node) throws JacksonException
    {
        _assertNotNull("node", node);
        DeserializationContextExt ctxt = _deserializationContext();
        return (T) _bindAndClose(ctxt,
                _considerFilter(treeAsTokens(node, ctxt), false));
    }

    @SuppressWarnings("unchecked")
    public <T> T readValue(DataInput input) throws JacksonException
    {
        _assertNotNull("input", input);
        DeserializationContextExt ctxt = _deserializationContext();
        return (T) _bindAndClose(ctxt,
                _considerFilter(_parserFactory.createParser(ctxt, input), false));
    }

    /*
    /**********************************************************************
    /* Deserialization methods; JsonNode ("tree")
    /**********************************************************************
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
    public JsonNode readTree(InputStream src) throws JacksonException
    {
        _assertNotNull("src", src);
        DeserializationContextExt ctxt = _deserializationContext();
        return _bindAndCloseAsTree(ctxt,
                _considerFilter(_parserFactory.createParser(ctxt, src), false));
    }

    /**
     * Same as {@link #readTree(InputStream)} except content accessed through
     * passed-in {@link Reader}
     */
    public JsonNode readTree(Reader src) throws JacksonException
    {
        _assertNotNull("src", src);
        DeserializationContextExt ctxt = _deserializationContext();
        return _bindAndCloseAsTree(ctxt,
                _considerFilter(_parserFactory.createParser(ctxt, src), false));
    }

    /**
     * Same as {@link #readTree(InputStream)} except content read from
     * passed-in {@link String}
     */
    public JsonNode readTree(String content) throws JacksonException
    {
        _assertNotNull("content", content);
        DeserializationContextExt ctxt = _deserializationContext();
        return _bindAndCloseAsTree(ctxt,
                _considerFilter(_parserFactory.createParser(ctxt, content), false));
    }

    /**
     * Same as {@link #readTree(InputStream)} except content read from
     * passed-in byte array.
     */
    public JsonNode readTree(byte[] content) throws JacksonException
    {
        _assertNotNull("content", content);
        DeserializationContextExt ctxt = _deserializationContext();
        return _bindAndCloseAsTree(ctxt,
                _considerFilter(_parserFactory.createParser(ctxt, content), false));
    }

    /**
     * Same as {@link #readTree(InputStream)} except content read from
     * passed-in byte array.
     */
    public JsonNode readTree(byte[] content, int offset, int len) throws JacksonException
    {
        _assertNotNull("content", content);
        DeserializationContextExt ctxt = _deserializationContext();
        return _bindAndCloseAsTree(ctxt,
                _considerFilter(_parserFactory.createParser(ctxt, content, offset, len), false));
    }

    /**
     * Same as {@link #readTree(InputStream)} except content read using
     * passed-in {@link DataInput}.
     */
    public JsonNode readTree(DataInput content) throws JacksonException
    {
        _assertNotNull("content", content);
        DeserializationContextExt ctxt = _deserializationContext();
        return _bindAndCloseAsTree(ctxt,
                _considerFilter(_parserFactory.createParser(ctxt, content), false));
    }

    /*
    /**********************************************************************
    /* Deserialization methods; reading sequence of values
    /**********************************************************************
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
        throws JacksonException
    {
        _assertNotNull("p", p);
        DeserializationContext ctxt = _deserializationContext(p);
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
    public <T> MappingIterator<T> readValues(InputStream src) throws JacksonException
    {
        _assertNotNull("src", src);
        DeserializationContextExt ctxt = _deserializationContext();
        return _bindAndReadValues(ctxt,
                _considerFilter(_parserFactory.createParser(ctxt, src), true));
    }

    /**
     * Overloaded version of {@link #readValue(InputStream)}.
     */
    @SuppressWarnings("resource")
    public <T> MappingIterator<T> readValues(Reader src) throws JacksonException
    {
        _assertNotNull("src", src);
        DeserializationContextExt ctxt = _deserializationContext();
        JsonParser p = _considerFilter(_parserFactory.createParser(ctxt, src), true);
        _initForMultiRead(ctxt, p);
        p.nextToken();
        return _newIterator(p, ctxt, _findRootDeserializer(ctxt), true);
    }

    /**
     * Overloaded version of {@link #readValue(InputStream)}.
     *
     * @param content String that contains JSON content to parse
     */
    @SuppressWarnings("resource")
    public <T> MappingIterator<T> readValues(String content) throws JacksonException
    {
        _assertNotNull("content", content);
        DeserializationContextExt ctxt = _deserializationContext();
        JsonParser p = _considerFilter(_parserFactory.createParser(ctxt, content), true);
        _initForMultiRead(ctxt, p);
        p.nextToken();
        return _newIterator(p, ctxt, _findRootDeserializer(ctxt), true);
    }

    /**
     * Overloaded version of {@link #readValue(InputStream)}.
     */
    public <T> MappingIterator<T> readValues(byte[] content, int offset, int length) throws JacksonException
    {
        _assertNotNull("content", content);
        DeserializationContextExt ctxt = _deserializationContext();
        return _bindAndReadValues(ctxt,
                _considerFilter(_parserFactory.createParser(ctxt, content, offset, length), true));
    }

    /**
     * Overloaded version of {@link #readValue(InputStream)}.
     */
    public final <T> MappingIterator<T> readValues(byte[] content) throws JacksonException {
        _assertNotNull("content", content);
        return readValues(content, 0, content.length);
    }

    /**
     * Overloaded version of {@link #readValue(InputStream)}.
     */
    public <T> MappingIterator<T> readValues(File src) throws JacksonException
    {
        _assertNotNull("src", src);
        DeserializationContextExt ctxt = _deserializationContext();
        return _bindAndReadValues(ctxt,
                _considerFilter(_parserFactory.createParser(ctxt, src), true));
    }

    /**
     * Overloaded version of {@link #readValues(InputStream)}.
     *
     * @since 3.0
     */
    public <T> MappingIterator<T> readValues(Path src) throws JacksonException
    {
        _assertNotNull("src", src);
        DeserializationContextExt ctxt = _deserializationContext();
        return _bindAndReadValues(ctxt,
                _considerFilter(_parserFactory.createParser(ctxt, src), true));
    }

    /**
     * Overloaded version of {@link #readValue(InputStream)}.
     *<p>
     * NOTE: handling of {@link java.net.URL} is delegated to
     * {@link TokenStreamFactory#createParser(ObjectReadContext, java.net.URL)} and usually simply
     * calls {@link java.net.URL#openStream()}, meaning no special handling
     * is done. If different HTTP connection options are needed you will need
     * to create {@link java.io.InputStream} separately.
     *
     * @param src URL to read to access JSON content to parse.
     */
    public <T> MappingIterator<T> readValues(URL src) throws JacksonException
    {
        _assertNotNull("src", src);
        DeserializationContextExt ctxt = _deserializationContext();
        return _bindAndReadValues(ctxt,
                _considerFilter(_parserFactory.createParser(ctxt, src), true));
    }

    public <T> MappingIterator<T> readValues(DataInput src) throws JacksonException
    {
        _assertNotNull("src", src);
        DeserializationContextExt ctxt = _deserializationContext();
        return _bindAndReadValues(ctxt,
                _considerFilter(_parserFactory.createParser(ctxt, src), true));
    }

    /*
    /**********************************************************************
    /* Implementation of rest of ObjectCodec methods
    /**********************************************************************
     */

    public <T> T treeToValue(TreeNode n, Class<T> valueType) throws JacksonException
    {
        _assertNotNull("n", n);
        return forType(valueType).readValue(treeAsTokens(n));
    }

    /**
     * Same as {@link #treeToValue(TreeNode, Class)} but with type-resolved target value type.
     */
    public <T> T treeToValue(TreeNode n, JavaType valueType) throws JacksonException
    {
        _assertNotNull("n", n);
        return forType(valueType).readValue(treeAsTokens(n));
    }

    /*
    /**********************************************************************
    /* Helper methods, data-binding
    /**********************************************************************
     */

    /**
     * Actual implementation of value reading+binding operation.
     */
    protected Object _bind(DeserializationContextExt ctxt,
            JsonParser p, Object valueToUpdate) throws JacksonException
    {
        // First: may need to read the next token, to initialize state (either
        // before first read from parser, or after previous token has been cleared)
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
            result = ctxt.readRootValue(p, _valueType, _findRootDeserializer(ctxt), _valueToUpdate);
        }
        // Need to consume the token too
        p.clearCurrentToken();
        if (_config.isEnabled(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)) {
            _verifyNoTrailingTokens(p, ctxt, _valueType);
        }
        return result;
    }

    protected Object _bindAndClose(DeserializationContextExt ctxt,
            JsonParser p0) throws JacksonException
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
                result = ctxt.readRootValue(p, _valueType, _findRootDeserializer(ctxt), _valueToUpdate);
            }
            // No need to consume the token as parser gets closed anyway
            if (_config.isEnabled(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)) {
                _verifyNoTrailingTokens(p, ctxt, _valueType);
            }
            return result;
        }
    }

    protected final JsonNode _bindAndCloseAsTree(DeserializationContextExt ctxt,
            JsonParser p0) throws JacksonException {
        try (JsonParser p = ctxt.assignAndReturnParser(p0)) {
            return _bindAsTree(ctxt, p);
        }
    }

    protected final JsonNode _bindAsTree(DeserializationContextExt ctxt,
            JsonParser p)
        throws JacksonException
    {
        // 16-Apr-2021, tatu: Should usually NOT be called this way but
        //    as per [databind#3122] should still work
        if (_valueToUpdate != null) {
            return (JsonNode) _bind(ctxt, p, _valueToUpdate);
        }

        JsonToken t = p.currentToken();
        if (t == null) {
            t = p.nextToken();
            if (t == null) {
                // [databind#2211]: return `MissingNode` (supercedes [databind#1406] which dictated
                // returning `null`
                return _config.getNodeFactory().missingNode();
            }
        }
        final JsonNode resultNode;
        if (t == JsonToken.VALUE_NULL) {
            resultNode = ctxt.getNodeFactory().nullNode();
        } else {
            // Will not be called for merge (need not pass _valueToUpdate)
            resultNode = (JsonNode) ctxt.readRootValue(p, JSON_NODE_TYPE, _findTreeDeserializer(ctxt), null);
        }
        // Need to consume the token too
        p.clearCurrentToken();
        if (_config.isEnabled(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)) {
            _verifyNoTrailingTokens(p, ctxt, JSON_NODE_TYPE);
        }
        return resultNode;
    }

    /**
     * Same as {@link #_bindAsTree} except end-of-input is reported by returning
     * {@code null}, not "missing node"
     */
    protected final JsonNode _bindAsTreeOrNull(DeserializationContextExt ctxt,
            JsonParser p) throws JacksonException
    {
        // 16-Apr-2021, tatu: Should usually NOT be called this way but
        //    as per [databind#3122] should still work
        if (_valueToUpdate != null) {
            return (JsonNode) _bind(ctxt, p, _valueToUpdate);
        }

        ctxt.assignParser(p);
        JsonToken t = p.currentToken();
        if (t == null) {
            t = p.nextToken();
            if (t == null) { // unlike above, here we do return `null`
                return null;
            }
        }
        final JsonNode resultNode;
        if (t == JsonToken.VALUE_NULL) {
            resultNode = ctxt.getNodeFactory().nullNode();
        } else {
            // Will not be called for merge (need not pass _valueToUpdate)
            resultNode = (JsonNode) ctxt.readRootValue(p, JSON_NODE_TYPE, _findTreeDeserializer(ctxt), null);
        }
        // Need to consume the token too
        p.clearCurrentToken();
        if (_config.isEnabled(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)) {
            _verifyNoTrailingTokens(p, ctxt, JSON_NODE_TYPE);
        }
        return resultNode;
    }

    protected <T> MappingIterator<T> _bindAndReadValues(DeserializationContextExt ctxt,
            JsonParser p) throws JacksonException
    {
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

    protected final void _verifyNoTrailingTokens(JsonParser p, DeserializationContext ctxt,
            JavaType bindType)
        throws JacksonException
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
    /**********************************************************************
    /* Internal methods, other
    /**********************************************************************
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
    protected DeserializationContextExt _deserializationContext() {
        return _contexts.createContext(_config, _schema, _injectableValues);
    }

    protected DeserializationContextExt _deserializationContext(JsonParser p) {
        return _contexts.createContext(_config, _schema, _injectableValues)
                .assignParser(p);
    }

    protected InputStream _inputStream(URL src) throws JacksonException {
        try {
            return src.openStream();
        } catch (IOException e) {
            throw WrappedIOException.construct(e);
        }
    }

    protected InputStream _inputStream(File f) throws JacksonException {
        try {
            return new FileInputStream(f);
        } catch (IOException e) {
            throw WrappedIOException.construct(e);
        }
    }

    protected InputStream _inputStream(Path path) throws JacksonException {
        try {
            return Files.newInputStream(path);
        } catch (IOException e) {
            throw WrappedIOException.construct(e);
        }
    }

    protected final void _assertNotNull(String paramName, Object src) {
        if (src == null){
            throw new IllegalArgumentException(String.format("argument \"%s\" is null", paramName));
        }
    }

    /*
    /**********************************************************************
    /* Helper methods, locating deserializers etc
    /**********************************************************************
     */

    /**
     * Method called to locate deserializer for the passed root-level value.
     */
    protected ValueDeserializer<Object> _findRootDeserializer(DeserializationContext ctxt)
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
        ValueDeserializer<Object> deser = _rootDeserializers.get(t);
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

    protected ValueDeserializer<Object> _findTreeDeserializer(DeserializationContext ctxt)
        throws DatabindException
    {
        ValueDeserializer<Object> deser = _rootDeserializers.get(JSON_NODE_TYPE);
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
    protected ValueDeserializer<Object> _prefetchRootDeserializer(JavaType valueType)
    {
        if ((valueType == null) || !_config.isEnabled(DeserializationFeature.EAGER_DESERIALIZER_FETCH)) {
            return null;
        }
        // already cached?
        ValueDeserializer<Object> deser = _rootDeserializers.get(valueType);
        if (deser == null) {
            try {
                // If not, need to resolve; for which we need a temporary context as well:
                DeserializationContext ctxt = _deserializationContext();
                deser = ctxt.findRootValueDeserializer(valueType);
                if (deser != null) {
                    _rootDeserializers.put(valueType, deser);
                }
                return deser;
            } catch (JacksonException e) {
                // need to swallow?
                // 20-Jan-2021, tatu: Not 100% sure actually... but was that way in 2.x
                //    so leaving for now
            }
        }
        return deser;
    }
}
