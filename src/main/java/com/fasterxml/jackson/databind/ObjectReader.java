package com.fasterxml.jackson.databind;

import java.io.*;
import java.net.URL;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.core.type.ResolvedType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.deser.DeserializerCache;
import com.fasterxml.jackson.databind.deser.StdDeserializationContext;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;
import com.fasterxml.jackson.databind.type.SimpleType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.RootNameLookup;

/**
 * Builder object that can be used for per-serialization configuration of
 * deserialization parameters, such as root type to use or object
 * to update (instead of constructing new instance).
 *<p>
 * Uses "fluent" (aka builder) pattern so that instances are immutable
 * (and thus fully thread-safe with no external synchronization);
 * new instances are constructed for different configurations.
 * Instances are initially constructed by {@link ObjectMapper} and can be
 * reused, shared, cached; both because of thread-safety and because
 * instances are relatively light-weight.
 */
public class ObjectReader
    extends ObjectCodec
    implements Versioned
{
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
     * Flag that indicates whether root values are expected to be unwrapped or not
     */
    protected final boolean _unwrapRoot;
    
    /**
     * Root-level cached deserializers
     */
    final protected ConcurrentHashMap<JavaType, JsonDeserializer<Object>> _rootDeserializers;
   
    protected final DeserializerCache _deserializerCache;

    /**
     * Factory used for constructing {@link JsonGenerator}s
     */
    protected final JsonFactory _jsonFactory;

    /**
     * Cache for root names used when root-wrapping is enabled.
     */
    protected final RootNameLookup _rootNames;
    
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
    
    /*
    /**********************************************************
    /* Life-cycle, construction
    /**********************************************************
     */

    /**
     * Constructor used by {@link ObjectMapper} for initial instantiation
     */
    protected ObjectReader(ObjectMapper mapper, DeserializationConfig config)
    {
        this(mapper, config, null, null, null, null);
    }

    protected ObjectReader(ObjectMapper mapper, DeserializationConfig config,
            JavaType valueType, Object valueToUpdate, FormatSchema schema,
            InjectableValues injectableValues)
    {
        _config = config;
        _rootDeserializers = mapper._rootDeserializers;
        _deserializerCache = mapper._deserializerCache;
        _jsonFactory = mapper._jsonFactory;
        _rootNames = mapper._rootNames;
        _valueType = valueType;
        _valueToUpdate = valueToUpdate;
        if (valueToUpdate != null && valueType.isArrayType()) {
            throw new IllegalArgumentException("Can not update an array value");
        }
        _schema = schema;
        _injectableValues = injectableValues;
        _unwrapRoot = config.useRootWrapping();
    }
    
    /**
     * Copy constructor used for building variations.
     */
    protected ObjectReader(ObjectReader base, DeserializationConfig config,
            JavaType valueType, Object valueToUpdate, FormatSchema schema,
            InjectableValues injectableValues)
    {
        _config = config;

        _rootDeserializers = base._rootDeserializers;
        _deserializerCache = base._deserializerCache;
        _jsonFactory = base._jsonFactory;
        _rootNames = base._rootNames;

        _valueType = valueType;
        _valueToUpdate = valueToUpdate;
        if (valueToUpdate != null && valueType.isArrayType()) {
            throw new IllegalArgumentException("Can not update an array value");
        }
        _schema = schema;
        _injectableValues = injectableValues;
        _unwrapRoot = config.useRootWrapping();
    }

    /**
     * Copy constructor used when modifying simple feature flags
     */
    protected ObjectReader(ObjectReader base, DeserializationConfig config)
    {
        _config = config;

        _rootDeserializers = base._rootDeserializers;
        _deserializerCache = base._deserializerCache;
        _jsonFactory = base._jsonFactory;
        _rootNames = base._rootNames;

        _valueType = base._valueType;
        _valueToUpdate = base._valueToUpdate;
        _schema = base._schema;
        _injectableValues = base._injectableValues;
        _unwrapRoot = config.useRootWrapping();
    }
    
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
    /* Life-cycle, fluent factory methods
    /**********************************************************
     */

    /**
     * Method for constructing a new reader instance that is configured
     * with specified feature enabled.
     */
    public ObjectReader with(DeserializationConfig.Feature feature) 
    {
        DeserializationConfig newConfig = _config.with(feature);
        return (newConfig == _config) ? this : new ObjectReader(this, newConfig);
    }    

    /**
     * Method for constructing a new reader instance that is configured
     * with specified features enabled.
     */
    public ObjectReader with(DeserializationConfig.Feature first,
            DeserializationConfig.Feature... other)
    {
        DeserializationConfig newConfig = _config.with(first, other);
        return (newConfig == _config) ? this : new ObjectReader(this, newConfig);
    }    

    /**
     * Method for constructing a new reader instance that is configured
     * with specified features enabled.
     */
    public ObjectReader withFeatures(DeserializationConfig.Feature... features)
    {
        DeserializationConfig newConfig = _config.withFeatures(features);
        return (newConfig == _config) ? this : new ObjectReader(this, newConfig);
    }    
    
    /**
     * Method for constructing a new reader instance that is configured
     * with specified feature disabled.
     */
    public ObjectReader without(DeserializationConfig.Feature feature) 
    {
        DeserializationConfig newConfig = _config.without(feature);
        return (newConfig == _config) ? this : new ObjectReader(this, newConfig);
    }    

    /**
     * Method for constructing a new reader instance that is configured
     * with specified features disabled.
     */
    public ObjectReader without(DeserializationConfig.Feature first,
            DeserializationConfig.Feature... other)
    {
        DeserializationConfig newConfig = _config.without(first, other);
        return (newConfig == _config) ? this : new ObjectReader(this, newConfig);
    }    

    /**
     * Method for constructing a new reader instance that is configured
     * with specified features disabled.
     */
    public ObjectReader withoutFeatures(DeserializationConfig.Feature... features)
    {
        DeserializationConfig newConfig = _config.withoutFeatures(features);
        return (newConfig == _config) ? this : new ObjectReader(this, newConfig);
    }    
    
    /**
     * Method for constructing a new instance with configuration that uses
     * passed {@link InjectableValues} to provide injectable values.
     *<p>
     * Note that the method does NOT change state of this reader, but
     * rather construct and returns a newly configured instance.
     */
    public ObjectReader withInjectableValues(InjectableValues injectableValues)
    {
        if (_injectableValues == injectableValues) {
            return this;
        }
        return new ObjectReader(this, _config, _valueType, _valueToUpdate,
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
    public ObjectReader withNodeFactory(JsonNodeFactory f)
    {
        DeserializationConfig newConfig = _config.withNodeFactory(f);
        return (newConfig == _config) ? this :  new ObjectReader(this, newConfig);
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
    public ObjectReader withRootName(String rootName)
    {
        DeserializationConfig newConfig = _config.withRootName(rootName);
        return (newConfig == _config) ? this :  new ObjectReader(this, newConfig);
    }
    
    /**
     * Method for constructing a new instance with configuration that
     * passes specified {@link FormatSchema} to {@link JsonParser} that
     * is constructed for parsing content.
     *<p>
     * Note that the method does NOT change state of this reader, but
     * rather construct and returns a newly configured instance.
     */
    public ObjectReader withSchema(FormatSchema schema)
    {
        if (_schema == schema) {
            return this;
        }
        return new ObjectReader(this, _config, _valueType, _valueToUpdate,
                schema, _injectableValues);
    }
    
    /**
     * Method for constructing a new reader instance that is configured
     * to data bind into specified type.
     *<p>
     * Note that the method does NOT change state of this reader, but
     * rather construct and returns a newly configured instance.
     */
    public ObjectReader withType(JavaType valueType)
    {
        if (valueType != null && valueType.equals(_valueType)) return this;
        // type is stored here, no need to make a copy of config
        return new ObjectReader(this, _config, valueType, _valueToUpdate,
                _schema, _injectableValues);
    }    

    /**
     * Method for constructing a new reader instance that is configured
     * to data bind into specified type.
     *<p>
     * Note that the method does NOT change state of this reader, but
     * rather construct and returns a newly configured instance.
     */
    public ObjectReader withType(Class<?> valueType) {
        return withType(_config.constructType(valueType));
    }    

    /**
     * Method for constructing a new reader instance that is configured
     * to data bind into specified type.
     *<p>
     * Note that the method does NOT change state of this reader, but
     * rather construct and returns a newly configured instance.
     */
    public ObjectReader withType(java.lang.reflect.Type valueType) {
        return withType(_config.getTypeFactory().constructType(valueType));
    }    

    /**
     * Method for constructing a new reader instance that is configured
     * to data bind into specified type.
     *<p>
     * Note that the method does NOT change state of this reader, but
     * rather construct and returns a newly configured instance.
     */
    public ObjectReader withType(TypeReference<?> valueTypeRef) {
        return withType(_config.getTypeFactory().constructType(valueTypeRef.getType()));
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
            throw new IllegalArgumentException("cat not update null value");
        }
        JavaType t = (_valueType == null) ? _config.constructType(value.getClass()) : _valueType;
        return new ObjectReader(this, _config, t, value,
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
        DeserializationConfig newConfig = _config.withView(activeView);
        return (newConfig == _config) ? this : new ObjectReader(this, newConfig);
    }

    /*
    /**********************************************************
    /* Simple accessors
    /**********************************************************
     */
    
    public boolean isEnabled(DeserializationConfig.Feature f) {
        return _config.isEnabled(f);
    }

    public boolean isEnabled(MapperConfig.Feature f) {
        return _config.isEnabled(f);
    }

    public boolean isEnabled(JsonParser.Feature f) {
        return _jsonFactory.isEnabled(f);
    }

    @Override
    public JsonFactory getJsonFactory() {
        return _jsonFactory;
    }

    public TypeFactory getTypeFactory() {
        return _config.getTypeFactory();
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
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(JsonParser jp)
        throws IOException, JsonProcessingException
    {
        return (T) _bind(jp);
    }

    /**
     * Convenience method that binds content read using given parser, using
     * configuration of this reader, except that expected value type
     * is specified with the call (instead of currently configured root type).
     * Value return is either newly constructed, or root value that
     * was specified with {@link #withValueToUpdate(Object)}.
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T readValue(JsonParser jp, Class<T> valueType)
        throws IOException, JsonProcessingException
    {
        return (T) withType(valueType).readValue(jp);
    }

    /**
     * Convenience method that binds content read using given parser, using
     * configuration of this reader, except that expected value type
     * is specified with the call (instead of currently configured root type).
     * Value return is either newly constructed, or root value that
     * was specified with {@link #withValueToUpdate(Object)}.
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T readValue(JsonParser jp, TypeReference<?> valueTypeRef)
        throws IOException, JsonProcessingException
    {
        return (T) withType(valueTypeRef).readValue(jp);
    }

    /**
     * Convenience method that binds content read using given parser, using
     * configuration of this reader, except that expected value type
     * is specified with the call (instead of currently configured root type).
     * Value return is either newly constructed, or root value that
     * was specified with {@link #withValueToUpdate(Object)}.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T readValue(JsonParser jp, ResolvedType valueType) throws IOException, JsonProcessingException {
        return (T) withType((JavaType)valueType).readValue(jp);
    }

    /**
     * Type-safe overloaded method, basically alias for {@link #readValue(JsonParser, ResolvedType)}.
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(JsonParser jp, JavaType valueType) throws IOException, JsonProcessingException {
        return (T) withType(valueType).readValue(jp);
    }
    
    /**
     * Convenience method that binds content read using given parser, using
     * configuration of this reader, except that content is bound as
     * JSON tree instead of configured root value type.
     *<p>
     * Note: if an object was specified with {@link #withValueToUpdate}, it
     * will be ignored.
     */
    @Override
    public JsonNode readTree(JsonParser jp)
        throws IOException, JsonProcessingException
    {
        return _bindAsTree(jp);
    }

    /**
     * Convenience method that is equivalent to:
     *<pre>
     *   withType(valueType).readValues(jp);
     *</pre>
     */
    @Override
    public <T> Iterator<T> readValues(JsonParser jp, Class<T> valueType)
        throws IOException, JsonProcessingException {
        return withType(valueType).readValues(jp);
    }

    /**
     * Convenience method that is equivalent to:
     *<pre>
     *   withType(valueTypeRef).readValues(jp);
     *</pre>
     */
    @Override
    public <T> Iterator<T> readValues(JsonParser jp, TypeReference<?> valueTypeRef)
        throws IOException, JsonProcessingException {
        return withType(valueTypeRef).readValues(jp);
    }
    
    /**
     * Convenience method that is equivalent to:
     *<pre>
     *   withType(valueType).readValues(jp);
     *</pre>
     */
    @Override
    public <T> Iterator<T> readValues(JsonParser jp, ResolvedType valueType)
        throws IOException, JsonProcessingException {
        return readValues(jp, (JavaType) valueType);
    }

    /**
     * Type-safe overloaded method, basically alias for {@link #readValues(JsonParser, ResolvedType)}.
     */
    public <T> Iterator<T> readValues(JsonParser jp, JavaType valueType)
        throws IOException, JsonProcessingException {
        return withType(valueType).readValues(jp);
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
        return (T) _bindAndClose(_jsonFactory.createJsonParser(src));
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
        return (T) _bindAndClose(_jsonFactory.createJsonParser(src));
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
        return (T) _bindAndClose(_jsonFactory.createJsonParser(src));
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
        return (T) _bindAndClose(_jsonFactory.createJsonParser(src));
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
        return (T) _bindAndClose(_jsonFactory.createJsonParser(src, offset, length));
    }
    
    @SuppressWarnings("unchecked")
    public <T> T readValue(File src)
        throws IOException, JsonProcessingException
    {
        return (T) _bindAndClose(_jsonFactory.createJsonParser(src));
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
        return (T) _bindAndClose(_jsonFactory.createJsonParser(src));
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
        return (T) _bindAndClose(treeAsTokens(src));
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
        return _bindAndCloseAsTree(_jsonFactory.createJsonParser(in));
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
        return _bindAndCloseAsTree(_jsonFactory.createJsonParser(r));
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
    public JsonNode readTree(String content)
        throws IOException, JsonProcessingException
    {
        return _bindAndCloseAsTree(_jsonFactory.createJsonParser(content));
    }

    /*
    /**********************************************************
    /* Deserialization methods; reading sequence of values
    /**********************************************************
     */
    
    /**
     * Method for reading sequence of Objects from parser stream.
     */
    public <T> MappingIterator<T> readValues(JsonParser jp)
        throws IOException, JsonProcessingException
    {
        DeserializationContext ctxt = _createDeserializationContext(jp, _config);
        // false -> do not close as caller gave parser instance
        return new MappingIterator<T>(_valueType, jp, ctxt,
                _findRootDeserializer(ctxt, _valueType),
                false, _valueToUpdate);
    }
    
    /**
     * Method for reading sequence of Objects from parser stream.
     */
    public <T> MappingIterator<T> readValues(InputStream src)
        throws IOException, JsonProcessingException
    {
        JsonParser jp = _jsonFactory.createJsonParser(src);
        if (_schema != null) {
            jp.setSchema(_schema);
        }
        DeserializationContext ctxt = _createDeserializationContext(jp, _config);
        return new MappingIterator<T>(_valueType, jp, ctxt, 
                _findRootDeserializer(ctxt, _valueType),
                true, _valueToUpdate);
    }

    /**
     * Method for reading sequence of Objects from parser stream.
     */
    public <T> MappingIterator<T> readValues(Reader src)
        throws IOException, JsonProcessingException
    {
        JsonParser jp = _jsonFactory.createJsonParser(src);
        if (_schema != null) {
            jp.setSchema(_schema);
        }
        DeserializationContext ctxt = _createDeserializationContext(jp, _config);
        return new MappingIterator<T>(_valueType, jp, ctxt,
                _findRootDeserializer(ctxt, _valueType), true, _valueToUpdate);
    }
    
    /**
     * Method for reading sequence of Objects from parser stream.
     */
    public <T> MappingIterator<T> readValues(String json)
        throws IOException, JsonProcessingException
    {
        JsonParser jp = _jsonFactory.createJsonParser(json);
        if (_schema != null) {
            jp.setSchema(_schema);
        }
        DeserializationContext ctxt = _createDeserializationContext(jp, _config);
        return new MappingIterator<T>(_valueType, jp, ctxt,
                _findRootDeserializer(ctxt, _valueType), true, _valueToUpdate);
    }

    /**
     * Method for reading sequence of Objects from parser stream.
     */
    public <T> MappingIterator<T> readValues(byte[] src, int offset, int length)
        throws IOException, JsonProcessingException
    {
        JsonParser jp = _jsonFactory.createJsonParser(src, offset, length);
        if (_schema != null) {
            jp.setSchema(_schema);
        }
        DeserializationContext ctxt = _createDeserializationContext(jp, _config);
        return new MappingIterator<T>(_valueType, jp, ctxt,
                _findRootDeserializer(ctxt, _valueType), true, _valueToUpdate);
    }

    public final <T> MappingIterator<T> readValues(byte[] src)
            throws IOException, JsonProcessingException {
        return readValues(src, 0, src.length);
    }
    
    /**
     * Method for reading sequence of Objects from parser stream.
     */
    public <T> MappingIterator<T> readValues(File src)
        throws IOException, JsonProcessingException
    {
        JsonParser jp = _jsonFactory.createJsonParser(src);
        if (_schema != null) {
            jp.setSchema(_schema);
        }
        DeserializationContext ctxt = _createDeserializationContext(jp, _config);
        return new MappingIterator<T>(_valueType, jp, ctxt,
                _findRootDeserializer(ctxt, _valueType), true, _valueToUpdate);
    }

    /**
     * Method for reading sequence of Objects from parser stream.
     */
    public <T> MappingIterator<T> readValues(URL src)
        throws IOException, JsonProcessingException
    {
        JsonParser jp = _jsonFactory.createJsonParser(src);
        if (_schema != null) {
            jp.setSchema(_schema);
        }
        DeserializationContext ctxt = _createDeserializationContext(jp, _config);
        return new MappingIterator<T>(_valueType, jp, ctxt,
                _findRootDeserializer(ctxt, _valueType), true, _valueToUpdate);
    }
    
    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    /**
     * Actual implementation of value reading+binding operation.
     */
    protected Object _bind(JsonParser jp)
        throws IOException, JsonParseException, JsonMappingException
    {
        /* First: may need to read the next token, to initialize state (either
         * before first read from parser, or after previous token has been cleared)
         */
        Object result;
        JsonToken t = _initForReading(jp);
        if (t == JsonToken.VALUE_NULL) {
            if (_valueToUpdate == null) {
                DeserializationContext ctxt = _createDeserializationContext(jp, _config);
                result = _findRootDeserializer(ctxt, _valueType).getNullValue();
            } else {
                result = _valueToUpdate;
            }
        } else if (t == JsonToken.END_ARRAY || t == JsonToken.END_OBJECT) {
            result = _valueToUpdate;
        } else { // pointing to event other than null
            DeserializationContext ctxt = _createDeserializationContext(jp, _config);
            JsonDeserializer<Object> deser = _findRootDeserializer(ctxt, _valueType);
            if (_unwrapRoot) {
                result = _unwrapAndDeserialize(jp, ctxt, _valueType, deser);
            } else {
                if (_valueToUpdate == null) {
                    result = deser.deserialize(jp, ctxt);
                } else {
                    deser.deserialize(jp, ctxt, _valueToUpdate);
                    result = _valueToUpdate;
                }
            }
        }
        // Need to consume the token too
        jp.clearCurrentToken();
        return result;
    }
    
    protected Object _bindAndClose(JsonParser jp)
        throws IOException, JsonParseException, JsonMappingException
    {
        if (_schema != null) {
            jp.setSchema(_schema);
        }
        try {
            Object result;
            JsonToken t = _initForReading(jp);
            if (t == JsonToken.VALUE_NULL) {
                if (_valueToUpdate == null) {
                    DeserializationContext ctxt = _createDeserializationContext(jp, _config);
                    result = _findRootDeserializer(ctxt, _valueType).getNullValue();
                } else {
                    result = _valueToUpdate;
                }
            } else if (t == JsonToken.END_ARRAY || t == JsonToken.END_OBJECT) {
                result = _valueToUpdate;
            } else {
                DeserializationContext ctxt = _createDeserializationContext(jp, _config);
                JsonDeserializer<Object> deser = _findRootDeserializer(ctxt, _valueType);
                if (_unwrapRoot) {
                    result = _unwrapAndDeserialize(jp, ctxt, _valueType, deser);
                } else {
                    if (_valueToUpdate == null) {
                        result = deser.deserialize(jp, ctxt);
                    } else {
                        deser.deserialize(jp, ctxt, _valueToUpdate);
                        result = _valueToUpdate;                    
                    }
                }
            }
            return result;
        } finally {
            try {
                jp.close();
            } catch (IOException ioe) { }
        }
    }

    protected JsonNode _bindAsTree(JsonParser jp)
        throws IOException, JsonParseException, JsonMappingException
    {
        JsonNode result;
        JsonToken t = _initForReading(jp);
        if (t == JsonToken.VALUE_NULL || t == JsonToken.END_ARRAY || t == JsonToken.END_OBJECT) {
            result = NullNode.instance;
        } else {
            DeserializationContext ctxt = _createDeserializationContext(jp, _config);
            JsonDeserializer<Object> deser = _findRootDeserializer(ctxt, JSON_NODE_TYPE);
            if (_unwrapRoot) {
                result = (JsonNode) _unwrapAndDeserialize(jp, ctxt, JSON_NODE_TYPE, deser);
            } else {
                result = (JsonNode) deser.deserialize(jp, ctxt);
            }
        }
        // Need to consume the token too
        jp.clearCurrentToken();
        return result;
    }
    
    protected JsonNode _bindAndCloseAsTree(JsonParser jp)
        throws IOException, JsonParseException, JsonMappingException
    {
        if (_schema != null) {
            jp.setSchema(_schema);
        }
        try {
            return _bindAsTree(jp);
        } finally {
            try {
                jp.close();
            } catch (IOException ioe) { }
        }
    }
    
    protected static JsonToken _initForReading(JsonParser jp)
        throws IOException, JsonParseException, JsonMappingException
    {
        /* First: must point to a token; if not pointing to one, advance.
         * This occurs before first read from JsonParser, as well as
         * after clearing of current token.
         */
        JsonToken t = jp.getCurrentToken();
        if (t == null) { // and then we must get something...
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

    /**
     * Method called to locate deserializer for the passed root-level value.
     */
    protected JsonDeserializer<Object> _findRootDeserializer(DeserializationContext ctxt,
            JavaType valueType)
        throws JsonMappingException
    {
        // Sanity check: must have actual type...
        if (valueType == null) {
            throw new JsonMappingException("No value type configured for ObjectReader");
        }
        
        // First: have we already seen it?
        JsonDeserializer<Object> deser = _rootDeserializers.get(valueType);
        if (deser != null) {
            return deser;
        }
        // Nope: need to ask provider to resolve it
        deser = ctxt.findTypedValueDeserializer(valueType, null);
        if (deser == null) { // can this happen?
            throw new JsonMappingException("Can not find a deserializer for type "+valueType);
        }
        _rootDeserializers.put(valueType, deser);
        return deser;
    }
    
    protected DeserializationContext _createDeserializationContext(JsonParser jp, DeserializationConfig cfg) {
        // 04-Jan-2010, tatu: we do actually need the provider too... (for polymorphic deser)
        return new StdDeserializationContext(cfg, jp, _deserializerCache, _injectableValues);
    }

    protected Object _unwrapAndDeserialize(JsonParser jp, DeserializationContext ctxt,
            JavaType rootType, JsonDeserializer<Object> deser)
        throws IOException, JsonParseException, JsonMappingException
    {
        String expName = _config.getRootName();
        if (expName == null) {
            SerializedString sstr = _rootNames.findRootName(rootType, _config);
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
        Object result;
        if (_valueToUpdate == null) {
            result = deser.deserialize(jp, ctxt);
        } else {
            deser.deserialize(jp, ctxt, _valueToUpdate);
            result = _valueToUpdate;                    
        }
        // and last, verify that we now get matching END_OBJECT
        if (jp.nextToken() != JsonToken.END_OBJECT) {
            throw JsonMappingException.from(jp, "Current token not END_OBJECT (to match wrapper object with root name '"
                    +expName+"'), but "+jp.getCurrentToken());
        }
        return result;
    }
    
    /*
    /**********************************************************
    /* Implementation of rest of ObjectCodec methods
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
    public JsonParser treeAsTokens(JsonNode n) {
        return new TreeTraversingParser(n, this);
    }

    @Override
    public <T> T treeToValue(JsonNode n, Class<T> valueType)
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
     * NOTE: NOT implemented for {@link ObjectReader}.
     */
    @Override
    public void writeTree(JsonGenerator jgen, JsonNode rootNode) throws IOException, JsonProcessingException
    {
        throw new UnsupportedOperationException("Not implemented for ObjectReader");
    }

    @Override
    public void writeValue(JsonGenerator jgen, Object value) throws IOException, JsonProcessingException
    {
        throw new UnsupportedOperationException("Not implemented for ObjectReader");
    }
}
