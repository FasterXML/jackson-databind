package com.fasterxml.jackson.databind;

import java.io.*;
import java.text.DateFormat;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.SegmentedStringWriter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.ByteArrayBuilder;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import com.fasterxml.jackson.databind.cfg.DatabindVersion;
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
 * 
 * @author tatu
 */
public class ObjectWriter
    implements Versioned
{
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

        _rootType = rootType;
        _prettyPrinter = pp;
        _schema = null;
    }

    /**
     * Alternative constructor for initial instantiation.
     */
    protected ObjectWriter(ObjectMapper mapper, SerializationConfig config)
    {
        _config = config;

        _serializerProvider = mapper._serializerProvider;
        _serializerFactory = mapper._serializerFactory;
        _jsonFactory = mapper._jsonFactory;

        _rootType = null;
        _prettyPrinter = null;
        _schema = null;
    }

    /**
     * Alternative constructor for initial instantiation.
     */
    protected ObjectWriter(ObjectMapper mapper, SerializationConfig config,
            FormatSchema s)
    {
        _config = config;

        _serializerProvider = mapper._serializerProvider;
        _serializerFactory = mapper._serializerFactory;
        _jsonFactory = mapper._jsonFactory;

        _rootType = null;
        _prettyPrinter = null;
        _schema = s;
    }
    
    /**
     * Copy constructor used for building variations.
     */
    protected ObjectWriter(ObjectWriter base, SerializationConfig config,
            JavaType rootType, PrettyPrinter pp, FormatSchema s)
    {
        _config = config;

        _serializerProvider = base._serializerProvider;
        _serializerFactory = base._serializerFactory;
        _jsonFactory = base._jsonFactory;
        
        _rootType = rootType;
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
        _prettyPrinter = base._prettyPrinter;
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
    public ObjectWriter withDateFormat(DateFormat df)
    {
        SerializationConfig newConfig = _config.withDateFormat(df);
        return (newConfig == _config) ? this : new ObjectWriter(this, newConfig);
    }

    /**
     * Method that will construct a new instance that will use the default
     * pretty printer for serialization.
     */
    public ObjectWriter withDefaultPrettyPrinter()
    {
        return withPrettyPrinter(new DefaultPrettyPrinter());
    }

    /**
     * Method that will construct a new instance that uses specified
     * provider for resolving filter instances by id.
     */
    public ObjectWriter withFilters(FilterProvider filterProvider)
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
    public ObjectWriter withPrettyPrinter(PrettyPrinter pp)
    {
        if (pp == _prettyPrinter) {
            return this;
        }
        // since null would mean "don't care", need to use placeholder to indicate "disable"
        if (pp == null) {
            pp = NULL_PRETTY_PRINTER;
        }
        return new ObjectWriter(this, _config, _rootType, pp, _schema);
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
        return (_schema == schema) ? this :
            new ObjectWriter(this, _config, _rootType, _prettyPrinter, schema);
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
        return (rootType == _rootType) ? this
        // type is stored here, no need to make a copy of config
            : new ObjectWriter(this, _config, rootType, _prettyPrinter, _schema);
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

    public JsonFactory getJsonFactory() {
        return _jsonFactory;
    }
    
    public TypeFactory getTypeFactory() {
        return _config.getTypeFactory();
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
        if (_config.isEnabled(SerializationFeature.CLOSE_CLOSEABLE) && (value instanceof Closeable)) {
            _writeCloseableValue(jgen, value, _config);
        } else {
            if (_rootType == null) {
                _serializerProvider(_config).serializeValue(jgen, value);
            } else {
                _serializerProvider(_config).serializeValue(jgen, value, _rootType);
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
        _configAndWriteValue(_jsonFactory.createJsonGenerator(resultFile, JsonEncoding.UTF8), value);
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
        _configAndWriteValue(_jsonFactory.createJsonGenerator(out, JsonEncoding.UTF8), value);
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
        _configAndWriteValue(_jsonFactory.createJsonGenerator(w), value);
    }

    /**
     * Method that can be used to serialize any Java value as
     * a String. Functionally equivalent to calling
     * {@link #writeValue(Writer,Object)} with {@link java.io.StringWriter}
     * and constructing String, but more efficient.
     */
    public String writeValueAsString(Object value)
        throws IOException, JsonGenerationException, JsonMappingException
    {        
        // alas, we have to pull the recycler directly here...
        SegmentedStringWriter sw = new SegmentedStringWriter(_jsonFactory._getBufferRecycler());
        _configAndWriteValue(_jsonFactory.createJsonGenerator(sw), value);
        return sw.getAndClear();
    }
    
    /**
     * Method that can be used to serialize any Java value as
     * a byte array. Functionally equivalent to calling
     * {@link #writeValue(Writer,Object)} with {@link java.io.ByteArrayOutputStream}
     * and getting bytes, but more efficient.
     * Encoding used will be UTF-8.
     */
    public byte[] writeValueAsBytes(Object value)
        throws IOException, JsonGenerationException, JsonMappingException
    {        
        ByteArrayBuilder bb = new ByteArrayBuilder(_jsonFactory._getBufferRecycler());
        _configAndWriteValue(_jsonFactory.createJsonGenerator(bb, JsonEncoding.UTF8), value);
        byte[] result = bb.toByteArray();
        bb.release();
        return result;
    }

    /*
    /**********************************************************
    /* Other public methods
    /**********************************************************
     */

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
     * Method called to configure the generator as necessary and then
     * call write functionality
     */
    protected final void _configAndWriteValue(JsonGenerator jgen, Object value)
        throws IOException, JsonGenerationException, JsonMappingException
    {
        if (_prettyPrinter != null) {
            PrettyPrinter pp = _prettyPrinter;
            jgen.setPrettyPrinter((pp == NULL_PRETTY_PRINTER) ? null : pp);
        } else if (_config.isEnabled(SerializationFeature.INDENT_OUTPUT)) {
            jgen.useDefaultPrettyPrinter();
        }
        // [JACKSON-520]: add support for pass-through schema:
        if (_schema != null) {
            jgen.setSchema(_schema);
        }
        // [JACKSON-282]: consider Closeable
        if (_config.isEnabled(SerializationFeature.CLOSE_CLOSEABLE) && (value instanceof Closeable)) {
            _configAndWriteCloseable(jgen, value, _config);
            return;
        }
        boolean closed = false;
        try {
            if (_rootType == null) {
                _serializerProvider(_config).serializeValue(jgen, value);
            } else {
                _serializerProvider(_config).serializeValue(jgen, value, _rootType);                
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
    private final void _configAndWriteCloseable(JsonGenerator jgen, Object value, SerializationConfig cfg)
        throws IOException, JsonGenerationException, JsonMappingException
    {
        Closeable toClose = (Closeable) value;
        try {
            if (_rootType == null) {
                _serializerProvider(cfg).serializeValue(jgen, value);
            } else {
                _serializerProvider(cfg).serializeValue(jgen, value);
            }
            // [JACKSON-520]: add support for pass-through schema:
            if (_schema != null) {
                jgen.setSchema(_schema);
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
                _serializerProvider(cfg).serializeValue(jgen, value, _rootType);
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
}
