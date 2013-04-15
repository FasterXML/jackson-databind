package com.fasterxml.jackson.databind;

import java.io.*;
import java.text.DateFormat;
import java.util.Locale;
import java.util.TimeZone;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.SegmentedStringWriter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.ByteArrayBuilder;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Instantiatable;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;

import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.SerializerFactory;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * Builder object that can be used for per-serialization configuration of
 * serialization parameters, such as JSON View and root type to use.
 * (and thus fully thread-safe with no external synchronization);
 * new instances are constructed for different configurations.
 * Instances are initially constructed by {@link ObjectMapper} and can be
 * reused in completely thread-safe manner with no explicit synchronization
 */
public class ObjectWriter
    implements Versioned,
        java.io.Serializable // since 2.1
{
    private static final long serialVersionUID = -7024829992408267532L;

    /**
     * We need to keep track of explicit disabling of pretty printing;
     * easiest to do by a token value.
     */
    protected final static PrettyPrinter NULL_PRETTY_PRINTER = new MinimalPrettyPrinter();
    
    /*
    /**********************************************************
    /* Immutable configuration from ObjectMapper
    /**********************************************************
     */

    /**
     * General serialization configuration settings
     */
    protected final SerializationConfig _config;
   
    protected final DefaultSerializerProvider _serializerProvider;

    protected final SerializerFactory _serializerFactory;

    /**
     * Factory used for constructing {@link JsonGenerator}s
     */
    protected final JsonFactory _jsonFactory;
    
    /*
    /**********************************************************
    /* Configuration that can be changed during building
    /**********************************************************
     */

    /**
     * Specified root serialization type to use; can be same
     * as runtime type, but usually one of its super types
     */
    protected final JavaType _rootType;

    /**
     * We may pre-fetch serializer if {@link #_rootType}
     * is known, and if so, reuse it afterwards.
     * This allows avoiding further serializer lookups and increases
     * performance a bit on cases where readers are reused.
     * 
     * @since 2.1
     */
    protected final JsonSerializer<Object> _rootSerializer;
    
    /**
     * To allow for dynamic enabling/disabling of pretty printing,
     * pretty printer can be optionally configured for writer
     * as well
     */
    protected final PrettyPrinter _prettyPrinter;
    
    /**
     * When using data format that uses a schema, schema is passed
     * to generator.
     */
    protected final FormatSchema _schema;
    
    /*
    /**********************************************************
    /* Life-cycle, constructors
    /**********************************************************
     */

    /**
     * Constructor used by {@link ObjectMapper} for initial instantiation
     */
    protected ObjectWriter(ObjectMapper mapper, SerializationConfig config,
            JavaType rootType, PrettyPrinter pp)
    {
        _config = config;

        _serializerProvider = mapper._serializerProvider;
        _serializerFactory = mapper._serializerFactory;
        _jsonFactory = mapper._jsonFactory;

        if (rootType != null) {
            rootType = rootType.withStaticTyping();
        }
        _rootType = rootType;
        _prettyPrinter = pp;
        _schema = null;

        _rootSerializer = _prefetchRootSerializer(config, rootType);
    }

    /**
     * Alternative constructor for initial instantiation by {@link ObjectMapper}
     */
    protected ObjectWriter(ObjectMapper mapper, SerializationConfig config)
    {
        _config = config;

        _serializerProvider = mapper._serializerProvider;
        _serializerFactory = mapper._serializerFactory;
        _jsonFactory = mapper._jsonFactory;

        _rootType = null;
        _rootSerializer = null;
        _prettyPrinter = null;
        _schema = null;
    }

    /**
     * Alternative constructor for initial instantiation by {@link ObjectMapper}
     */
    protected ObjectWriter(ObjectMapper mapper, SerializationConfig config,
            FormatSchema s)
    {
        _config = config;

        _serializerProvider = mapper._serializerProvider;
        _serializerFactory = mapper._serializerFactory;
        _jsonFactory = mapper._jsonFactory;

        _rootType = null;
        _rootSerializer = null;
        _prettyPrinter = null;
        _schema = s;
    }
    
    /**
     * Copy constructor used for building variations.
     */
    protected ObjectWriter(ObjectWriter base, SerializationConfig config,
            JavaType rootType, JsonSerializer<Object> rootSer,
            PrettyPrinter pp, FormatSchema s)
    {
        _config = config;

        _serializerProvider = base._serializerProvider;
        _serializerFactory = base._serializerFactory;
        _jsonFactory = base._jsonFactory;

        _rootType = rootType;
        _rootSerializer = rootSer;
        _prettyPrinter = pp;
        _schema = s;
    }

    /**
     * Copy constructor used for building variations.
     */
    protected ObjectWriter(ObjectWriter base, SerializationConfig config)
    {
        _config = config;

        _serializerProvider = base._serializerProvider;
        _serializerFactory = base._serializerFactory;
        _jsonFactory = base._jsonFactory;
        _schema = base._schema;

        _rootType = base._rootType;
        _rootSerializer = base._rootSerializer;
        _prettyPrinter = base._prettyPrinter;
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
    /* Life-cycle, fluent factories
    /**********************************************************
     */

    /**
     * Method for constructing a new instance that is configured
     * with specified feature enabled.
     */
    public ObjectWriter with(SerializationFeature feature) 
    {
        SerializationConfig newConfig = _config.with(feature);
        return (newConfig == _config) ? this : new ObjectWriter(this, newConfig);
    }

    /**
     * Method for constructing a new instance that is configured
     * with specified features enabled.
     */
    public ObjectWriter with(SerializationFeature first,
            SerializationFeature... other)
    {
        SerializationConfig newConfig = _config.with(first, other);
        return (newConfig == _config) ? this : new ObjectWriter(this, newConfig);
    }    

    /**
     * Method for constructing a new instance that is configured
     * with specified features enabled.
     */
    public ObjectWriter withFeatures(SerializationFeature... features)
    {
        SerializationConfig newConfig = _config.withFeatures(features);
        return (newConfig == _config) ? this : new ObjectWriter(this, newConfig);
    }    
    
    /**
     * Method for constructing a new instance that is configured
     * with specified feature enabled.
     */
    public ObjectWriter without(SerializationFeature feature) 
    {
        SerializationConfig newConfig = _config.without(feature);
        return (newConfig == _config) ? this : new ObjectWriter(this, newConfig);
    }    

    /**
     * Method for constructing a new instance that is configured
     * with specified features enabled.
     */
    public ObjectWriter without(SerializationFeature first,
            SerializationFeature... other)
    {
        SerializationConfig newConfig = _config.without(first, other);
        return (newConfig == _config) ? this : new ObjectWriter(this, newConfig);
    }    

    /**
     * Method for constructing a new instance that is configured
     * with specified features enabled.
     */
    public ObjectWriter withoutFeatures(SerializationFeature... features)
    {
        SerializationConfig newConfig = _config.withoutFeatures(features);
        return (newConfig == _config) ? this : new ObjectWriter(this, newConfig);
    }    
    
    /**
     * Fluent factory method that will construct a new writer instance that will
     * use specified date format for serializing dates; or if null passed, one
     * that will serialize dates as numeric timestamps.
     *<p>
     * Note that the method does NOT change state of this reader, but
     * rather construct and returns a newly configured instance.
     */
    public ObjectWriter with(DateFormat df)
    {
        SerializationConfig newConfig = _config.with(df);
        return (newConfig == _config) ? this : new ObjectWriter(this, newConfig);
    }

    /**
     * Method that will construct a new instance that will use the default
     * pretty printer for serialization.
     */
    public ObjectWriter withDefaultPrettyPrinter()
    {
        return with(new DefaultPrettyPrinter());
    }

    /**
     * Method that will construct a new instance that uses specified
     * provider for resolving filter instances by id.
     */
    public ObjectWriter with(FilterProvider filterProvider)
    {
        if (filterProvider == _config.getFilterProvider()) { // no change?
            return this;
        }
        return new ObjectWriter(this, _config.withFilters(filterProvider));
    }

    /**
     * Method that will construct a new instance that will use specified pretty
     * printer (or, if null, will not do any pretty-printing)
     */
    public ObjectWriter with(PrettyPrinter pp)
    {
        if (pp == _prettyPrinter) {
            return this;
        }
        // since null would mean "don't care", need to use placeholder to indicate "disable"
        if (pp == null) {
            pp = NULL_PRETTY_PRINTER;
        }
        return new ObjectWriter(this, _config, _rootType, _rootSerializer, pp, _schema);
    }

    /**
     * Method for constructing a new instance with configuration that
     * specifies what root name to use for "root element wrapping".
     * See {@link SerializationConfig#withRootName(String)} for details.
     *<p>
     * Note that method does NOT change state of this reader, but
     * rather construct and returns a newly configured instance.
     */
    public ObjectWriter withRootName(String rootName)
    {
        SerializationConfig newConfig = _config.withRootName(rootName);
        return (newConfig == _config) ? this :  new ObjectWriter(this, newConfig);
    }

    /**
     * Method that will construct a new instance that uses specific format schema
     * for serialization.
     *<p>
     * Note that method does NOT change state of this reader, but
     * rather construct and returns a newly configured instance.
     */
    
    public ObjectWriter withSchema(FormatSchema schema)
    {
        if (_schema == schema) {
            return this;
        }
        _verifySchemaType(schema);
        return new ObjectWriter(this, _config, _rootType, _rootSerializer, _prettyPrinter, schema);
    }

    /**
     * Method that will construct a new instance that uses specific type
     * as the root type for serialization, instead of runtime dynamic
     * type of the root object itself.
     *<p>
     * Note that method does NOT change state of this reader, but
     * rather construct and returns a newly configured instance.
     */
    public ObjectWriter withType(JavaType rootType)
    {
        // 15-Mar-2013, tatu: Important! Indicate that static typing is needed:
        rootType = rootType.withStaticTyping();
        JsonSerializer<Object> rootSer = _prefetchRootSerializer(_config, rootType);
        return new ObjectWriter(this, _config, rootType, rootSer, _prettyPrinter, _schema);
    }    

    /**
     * Method that will construct a new instance that uses specific type
     * as the root type for serialization, instead of runtime dynamic
     * type of the root object itself.
     */
    public ObjectWriter withType(Class<?> rootType) {
        return withType(_config.constructType(rootType));
    }

    public ObjectWriter withType(TypeReference<?> rootType) {
        return withType(_config.getTypeFactory().constructType(rootType.getType()));
    }

    /**
     * Method that will construct a new instance that uses specified
     * serialization view for serialization (with null basically disables
     * view processing)
     *<p>
     * Note that the method does NOT change state of this reader, but
     * rather construct and returns a newly configured instance.
     */
    public ObjectWriter withView(Class<?> view) {
        SerializationConfig newConfig = _config.withView(view);
        return (newConfig == _config) ? this :  new ObjectWriter(this, newConfig);
    }    

    public ObjectWriter with(Locale l) {
        SerializationConfig newConfig = _config.with(l);
        return (newConfig == _config) ? this :  new ObjectWriter(this, newConfig);
    }

    public ObjectWriter with(TimeZone tz) {
        SerializationConfig newConfig = _config.with(tz);
        return (newConfig == _config) ? this :  new ObjectWriter(this, newConfig);
    }

    /**
     * Method that will construct a new instance that uses specified default
     * {@link Base64Variant} for base64 encoding
     * 
     * @since 2.1
     */
    public ObjectWriter with(Base64Variant b64variant) {
        SerializationConfig newConfig = _config.with(b64variant);
        return (newConfig == _config) ? this :  new ObjectWriter(this, newConfig);
    }
    
    /*
    /**********************************************************
    /* Simple accessors
    /**********************************************************
     */

    public boolean isEnabled(SerializationFeature f) {
        return _config.isEnabled(f);
    }

    public boolean isEnabled(MapperFeature f) {
        return _config.isEnabled(f);
    }

    public boolean isEnabled(JsonParser.Feature f) {
        return _jsonFactory.isEnabled(f);
    }

    /**
     * @since 2.2
     */
    public SerializationConfig getConfig() {
        return _config;
    }

    /**
     * @deprecated Since 2.2, use {@link #getFactory} instead.
     */
    @Deprecated
    public JsonFactory getJsonFactory() {
        return _jsonFactory;
    }

    /**
     * @since 2.2
     */
    public JsonFactory getFactory() {
        return _jsonFactory;
    }
    
    public TypeFactory getTypeFactory() {
        return _config.getTypeFactory();
    }

    /**
     * Diagnostics method that can be called to check whether this writer
     * has pre-fetched serializer to use: pre-fetching improves performance
     * when writer instances are reused as it avoids a per-call serializer
     * lookup.
     * 
     * @since 2.2
     */
    public boolean hasPrefetchedSerializer() {
        return _rootSerializer != null;
    }
    
    /*
    /**********************************************************
    /* Serialization methods; ones from ObjectCodec first
    /**********************************************************
     */

    /**
     * Method that can be used to serialize any Java value as
     * JSON output, using provided {@link JsonGenerator}.
     */
    public void writeValue(JsonGenerator jgen, Object value)
        throws IOException, JsonGenerationException, JsonMappingException
    {
        // 10-Aug-2012, tatu: As per [Issue#12], may need to force PrettyPrinter settings, so:
        _configureJsonGenerator(jgen);
        if (_config.isEnabled(SerializationFeature.CLOSE_CLOSEABLE)
                && (value instanceof Closeable)) {
            _writeCloseableValue(jgen, value, _config);
        } else {
            if (_rootType == null) {
                _serializerProvider(_config).serializeValue(jgen, value);
            } else {
                _serializerProvider(_config).serializeValue(jgen, value, _rootType, _rootSerializer);
            }
            if (_config.isEnabled(SerializationFeature.FLUSH_AFTER_WRITE_VALUE)) {
                jgen.flush();
            }
        }
    }
    
    /*
    /**********************************************************
    /* Serialization methods, others
    /**********************************************************
     */

    /**
     * Method that can be used to serialize any Java value as
     * JSON output, written to File provided.
     */
    public void writeValue(File resultFile, Object value)
        throws IOException, JsonGenerationException, JsonMappingException
    {
        _configAndWriteValue(_jsonFactory.createGenerator(resultFile, JsonEncoding.UTF8), value);
    }

    /**
     * Method that can be used to serialize any Java value as
     * JSON output, using output stream provided (using encoding
     * {@link JsonEncoding#UTF8}).
     *<p>
     * Note: method does not close the underlying stream explicitly
     * here; however, {@link JsonFactory} this mapper uses may choose
     * to close the stream depending on its settings (by default,
     * it will try to close it when {@link JsonGenerator} we construct
     * is closed).
     */
    public void writeValue(OutputStream out, Object value)
        throws IOException, JsonGenerationException, JsonMappingException
    {
        _configAndWriteValue(_jsonFactory.createGenerator(out, JsonEncoding.UTF8), value);
    }

    /**
     * Method that can be used to serialize any Java value as
     * JSON output, using Writer provided.
     *<p>
     * Note: method does not close the underlying stream explicitly
     * here; however, {@link JsonFactory} this mapper uses may choose
     * to close the stream depending on its settings (by default,
     * it will try to close it when {@link JsonGenerator} we construct
     * is closed).
     */
    public void writeValue(Writer w, Object value)
        throws IOException, JsonGenerationException, JsonMappingException
    {
        _configAndWriteValue(_jsonFactory.createGenerator(w), value);
    }

    /**
     * Method that can be used to serialize any Java value as
     * a String. Functionally equivalent to calling
     * {@link #writeValue(Writer,Object)} with {@link java.io.StringWriter}
     * and constructing String, but more efficient.
     *<p>
     * Note: prior to version 2.1, throws clause included {@link IOException}; 2.1 removed it.
     */
    public String writeValueAsString(Object value)
        throws JsonProcessingException
    {        
        // alas, we have to pull the recycler directly here...
        SegmentedStringWriter sw = new SegmentedStringWriter(_jsonFactory._getBufferRecycler());
        try {
            _configAndWriteValue(_jsonFactory.createGenerator(sw), value);
        } catch (JsonProcessingException e) { // to support [JACKSON-758]
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
     *<p>
     * Note: prior to version 2.1, throws clause included {@link IOException}; 2.1 removed it.
     */
    public byte[] writeValueAsBytes(Object value)
        throws JsonProcessingException
    {
        ByteArrayBuilder bb = new ByteArrayBuilder(_jsonFactory._getBufferRecycler());
        try {
            _configAndWriteValue(_jsonFactory.createGenerator(bb, JsonEncoding.UTF8), value);
        } catch (JsonProcessingException e) { // to support [JACKSON-758]
            throw e;
        } catch (IOException e) { // shouldn't really happen, but is declared as possibility so:
            throw JsonMappingException.fromUnexpectedIOE(e);
        }
        byte[] result = bb.toByteArray();
        bb.release();
        return result;
    }

    /*
    /**********************************************************
    /* Other public methods
    /**********************************************************
     */

    /**
     * Method for visiting type hierarchy for given type, using specified visitor.
     * Visitation uses <code>Serializer</code> hierarchy and related properties
     *<p>
     * This method can be used for things like
     * generating <a href="http://json-schema.org/">Json Schema</a>
     * instance for specified type.
     *
     * @param type Type to generate schema for (possibly with generic signature)
     * 
     * @since 2.2
     */
    public void acceptJsonFormatVisitor(JavaType type, JsonFormatVisitorWrapper visitor)
        throws JsonMappingException
    {
        if (type == null) {
            throw new IllegalArgumentException("type must be provided");
        }
        _serializerProvider(_config).acceptJsonFormatVisitor(type, visitor);
    }
    
    public boolean canSerialize(Class<?> type) {
        return _serializerProvider(_config).hasSerializerFor(type);
    }

    /*
    /**********************************************************
    /* Overridable helper methods
    /**********************************************************
     */
    
    /**
     * Overridable helper method used for constructing
     * {@link SerializerProvider} to use for serialization.
     */
    protected DefaultSerializerProvider _serializerProvider(SerializationConfig config) {
        return _serializerProvider.createInstance(config, _serializerFactory);
    }
    
    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */

    /**
     * @since 2.2
     */
    protected void _verifySchemaType(FormatSchema schema)
    {
        if (schema != null) {
            if (!_jsonFactory.canUseSchema(schema)) {
                    throw new IllegalArgumentException("Can not use FormatSchema of type "+schema.getClass().getName()
                            +" for format "+_jsonFactory.getFormatName());
            }
        }
    }

    /**
     * Method called to configure the generator as necessary and then
     * call write functionality
     */
    protected final void _configAndWriteValue(JsonGenerator jgen, Object value)
        throws IOException, JsonGenerationException, JsonMappingException
    {
        _configureJsonGenerator(jgen);
        // [JACKSON-282]: consider Closeable
        if (_config.isEnabled(SerializationFeature.CLOSE_CLOSEABLE) && (value instanceof Closeable)) {
            _writeCloseable(jgen, value, _config);
            return;
        }
        boolean closed = false;
        try {
            if (_rootType == null) {
                _serializerProvider(_config).serializeValue(jgen, value);
            } else {
                _serializerProvider(_config).serializeValue(jgen, value, _rootType, _rootSerializer);
            }
            closed = true;
            jgen.close();
        } finally {
            /* won't try to close twice; also, must catch exception (so it 
             * will not mask exception that is pending)
             */
            if (!closed) {
                try {
                    jgen.close();
                } catch (IOException ioe) { }
            }
        }
    }

    /**
     * Helper method used when value to serialize is {@link Closeable} and its <code>close()</code>
     * method is to be called right after serialization has been called
     */
    private final void _writeCloseable(JsonGenerator jgen, Object value, SerializationConfig cfg)
        throws IOException, JsonGenerationException, JsonMappingException
    {
        Closeable toClose = (Closeable) value;
        try {
            if (_rootType == null) {
                _serializerProvider(cfg).serializeValue(jgen, value);
            } else {
                _serializerProvider(cfg).serializeValue(jgen, value, _rootType, _rootSerializer);
            }
            JsonGenerator tmpJgen = jgen;
            jgen = null;
            tmpJgen.close();
            Closeable tmpToClose = toClose;
            toClose = null;
            tmpToClose.close();
        } finally {
            /* Need to close both generator and value, as long as they haven't yet
             * been closed
             */
            if (jgen != null) {
                try {
                    jgen.close();
                } catch (IOException ioe) { }
            }
            if (toClose != null) {
                try {
                    toClose.close();
                } catch (IOException ioe) { }
            }
        }
    }
    
    /**
     * Helper method used when value to serialize is {@link Closeable} and its <code>close()</code>
     * method is to be called right after serialization has been called
     */
    private final void _writeCloseableValue(JsonGenerator jgen, Object value, SerializationConfig cfg)
        throws IOException, JsonGenerationException, JsonMappingException
    {
        Closeable toClose = (Closeable) value;
        try {
            if (_rootType == null) {
                _serializerProvider(cfg).serializeValue(jgen, value);
            } else {
                _serializerProvider(cfg).serializeValue(jgen, value, _rootType, _rootSerializer);
            }
            if (_config.isEnabled(SerializationFeature.FLUSH_AFTER_WRITE_VALUE)) {
                jgen.flush();
            }
            Closeable tmpToClose = toClose;
            toClose = null;
            tmpToClose.close();
        } finally {
            if (toClose != null) {
                try {
                    toClose.close();
                } catch (IOException ioe) { }
            }
        }
    }

    /**
     * Method called to locate (root) serializer ahead of time, if permitted
     * by configuration. Method also is NOT to throw an exception if
     * access fails.
     */
    protected JsonSerializer<Object> _prefetchRootSerializer(
            SerializationConfig config, JavaType valueType)
    {
        if (valueType == null || !_config.isEnabled(SerializationFeature.EAGER_SERIALIZER_FETCH)) {
            return null;
        }
        try {
            return _serializerProvider(config).findTypedValueSerializer(valueType, true, null);
        } catch (JsonProcessingException e) {
            // need to swallow?
            return null;
        }
    }
    
    /**
     * Helper method called to set or override settings of passed-in
     * {@link JsonGenerator}
     * 
     * @since 2.1
     */
    private void _configureJsonGenerator(JsonGenerator jgen)
    {
        if (_prettyPrinter != null) {
            PrettyPrinter pp = _prettyPrinter;
            if (pp == NULL_PRETTY_PRINTER) {
                jgen.setPrettyPrinter(null);
            } else {
                /* [JACKSON-851]: Better take care of stateful PrettyPrinters...
                 *   like the DefaultPrettyPrinter.
                 */
                if (pp instanceof Instantiatable<?>) {
                    pp = (PrettyPrinter) ((Instantiatable<?>) pp).createInstance();
                }
                jgen.setPrettyPrinter(pp);
            }
        } else if (_config.isEnabled(SerializationFeature.INDENT_OUTPUT)) {
            jgen.useDefaultPrettyPrinter();
        }
        // [JACKSON-520]: add support for pass-through schema:
        if (_schema != null) {
            jgen.setSchema(_schema);
        }
    }
}
