package com.fasterxml.jackson.databind;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.io.SegmentedStringWriter;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.*;
import com.fasterxml.jackson.databind.cfg.ContextAttributes;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.*;
import com.fasterxml.jackson.databind.ser.impl.TypeWrappedSerializer;
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
    private static final long serialVersionUID = 1; // since 2.5

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
    protected final JsonFactory _generatorFactory;

    /*
    /**********************************************************
    /* Configuration that can be changed via mutant factories
    /**********************************************************
     */

    /**
     * Container for settings that need to be passed to {@link JsonGenerator}
     * constructed for serializing values.
     *
     * @since 2.5
     */
    protected final GeneratorSettings _generatorSettings;

    /**
     * We may pre-fetch serializer if {@link #_rootType}
     * is known, and if so, reuse it afterwards.
     * This allows avoiding further serializer lookups and increases
     * performance a bit on cases where readers are reused.
     *
     * @since 2.5
     */
    protected final Prefetch _prefetch;
    
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
        _generatorFactory = mapper._jsonFactory;
        _generatorSettings = (pp == null) ? GeneratorSettings.empty
                : new GeneratorSettings(pp, null, null, null);

        // 29-Apr-2014, tatu: There is no "untyped serializer", so:
        if (rootType == null || rootType.hasRawClass(Object.class)) {
            _prefetch = Prefetch.empty;
        } else {
            rootType = rootType.withStaticTyping();
            _prefetch = _prefetchRootSerializer(config, rootType);
        }
    }

    /**
     * Alternative constructor for initial instantiation by {@link ObjectMapper}
     */
    protected ObjectWriter(ObjectMapper mapper, SerializationConfig config)
    {
        _config = config;
        _serializerProvider = mapper._serializerProvider;
        _serializerFactory = mapper._serializerFactory;
        _generatorFactory = mapper._jsonFactory;

        _prefetch = Prefetch.empty;
        _generatorSettings = GeneratorSettings.empty;
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
        _generatorFactory = mapper._jsonFactory;

        _prefetch = Prefetch.empty;
        _generatorSettings = (s == null) ? GeneratorSettings.empty
                : new GeneratorSettings(null, s, null, null);
    }
    
    /**
     * Copy constructor used for building variations.
     */
    protected ObjectWriter(ObjectWriter base, SerializationConfig config,
            GeneratorSettings genSettings, Prefetch prefetch)
    {
        _config = config;

        _serializerProvider = base._serializerProvider;
        _serializerFactory = base._serializerFactory;
        _generatorFactory = base._generatorFactory;

        _generatorSettings = genSettings;
        _prefetch = prefetch;
    }

    /**
     * Copy constructor used for building variations.
     */
    protected ObjectWriter(ObjectWriter base, SerializationConfig config)
    {
        _config = config;

        _serializerProvider = base._serializerProvider;
        _serializerFactory = base._serializerFactory;
        _generatorFactory = base._generatorFactory;
        _generatorSettings = base._generatorSettings;
        _prefetch = base._prefetch;
    }

    /**
     * @since 2.3
     */
    protected ObjectWriter(ObjectWriter base, JsonFactory f)
    {
        // may need to override ordering, based on data format capabilities
        _config = base._config
            .with(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, f.requiresPropertyOrdering());

        _serializerProvider = base._serializerProvider;
        _serializerFactory = base._serializerFactory;
        _generatorFactory = base._generatorFactory;
        _generatorSettings = base._generatorSettings;
        _prefetch = base._prefetch;
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
    /* writer instances, (re)configuring parser instances.
    /* Added in 2.5
    /**********************************************************
     */

    /**
     * Overridable factory method called by various "withXxx()" methods
     * 
     * @since 2.5
     */
    protected ObjectWriter _new(ObjectWriter base, JsonFactory f) {
        return new ObjectWriter(base, f);
    }

    /**
     * Overridable factory method called by various "withXxx()" methods
     * 
     * @since 2.5
     */
    protected ObjectWriter _new(ObjectWriter base, SerializationConfig config) {
        return new ObjectWriter(base, config);
    }

    /**
     * Overridable factory method called by various "withXxx()" methods.
     * It assumes `this` as base for settings other than those directly
     * passed in.
     * 
     * @since 2.5
     */
    protected ObjectWriter _new(GeneratorSettings genSettings, Prefetch prefetch) {
        return new ObjectWriter(this, _config, genSettings, prefetch);
    }

    /**
     * Overridable factory method called by {@link #createSequenceWriter(JsonGenerator)}
     * method (and its various overrides), and initializes it as necessary.
     * 
     * @since 2.5
     */
    @SuppressWarnings("resource")
    protected SequenceWriter _newSequenceWriter(boolean wrapInArray,
            JsonGenerator gen, boolean managedInput)
        throws IOException
    {
        return new SequenceWriter(_serializerProvider(_config),
                _configureGenerator(gen), managedInput, _prefetch)
            .init(wrapInArray);
    }

    /*
    /**********************************************************
    /* Life-cycle, fluent factories for SerializationFeature
    /**********************************************************
     */

    /**
     * Method for constructing a new instance that is configured
     * with specified feature enabled.
     */
    public ObjectWriter with(SerializationFeature feature)  {
        SerializationConfig newConfig = _config.with(feature);
        return (newConfig == _config) ? this : _new(this, newConfig);
    }

    /**
     * Method for constructing a new instance that is configured
     * with specified features enabled.
     */
    public ObjectWriter with(SerializationFeature first, SerializationFeature... other) {
        SerializationConfig newConfig = _config.with(first, other);
        return (newConfig == _config) ? this : _new(this, newConfig);
    }    

    /**
     * Method for constructing a new instance that is configured
     * with specified features enabled.
     */
    public ObjectWriter withFeatures(SerializationFeature... features) {
        SerializationConfig newConfig = _config.withFeatures(features);
        return (newConfig == _config) ? this : _new(this, newConfig);
    }    
    
    /**
     * Method for constructing a new instance that is configured
     * with specified feature enabled.
     */
    public ObjectWriter without(SerializationFeature feature) {
        SerializationConfig newConfig = _config.without(feature);
        return (newConfig == _config) ? this : _new(this, newConfig);
    }    

    /**
     * Method for constructing a new instance that is configured
     * with specified features enabled.
     */
    public ObjectWriter without(SerializationFeature first, SerializationFeature... other) {
        SerializationConfig newConfig = _config.without(first, other);
        return (newConfig == _config) ? this : _new(this, newConfig);
    }    

    /**
     * Method for constructing a new instance that is configured
     * with specified features enabled.
     */
    public ObjectWriter withoutFeatures(SerializationFeature... features) {
        SerializationConfig newConfig = _config.withoutFeatures(features);
        return (newConfig == _config) ? this : _new(this, newConfig);
    }

    /*
    /**********************************************************
    /* Life-cycle, fluent factories for JsonGenerator.Feature
    /**********************************************************
     */

    /**
     * @since 2.5
     */
    public ObjectWriter with(JsonGenerator.Feature feature)  {
        SerializationConfig newConfig = _config.with(feature);
        return (newConfig == _config) ? this : _new(this, newConfig);
    }

    /**
     * @since 2.5
     */
    public ObjectWriter withFeatures(JsonGenerator.Feature... features) {
        SerializationConfig newConfig = _config.withFeatures(features);
        return (newConfig == _config) ? this : _new(this, newConfig);
    }

    /**
     * @since 2.5
     */
    public ObjectWriter without(JsonGenerator.Feature feature) {
        SerializationConfig newConfig = _config.without(feature);
        return (newConfig == _config) ? this : _new(this, newConfig);
    }

    /**
     * @since 2.5
     */
    public ObjectWriter withoutFeatures(JsonGenerator.Feature... features) {
        SerializationConfig newConfig = _config.withoutFeatures(features);
        return (newConfig == _config) ? this : _new(this, newConfig);
    }

    /*
    /**********************************************************
    /* Life-cycle, fluent factories, other
    /**********************************************************
     */

    /**
     * Fluent factory method that will construct a new writer instance that will
     * use specified date format for serializing dates; or if null passed, one
     * that will serialize dates as numeric timestamps.
     *<p>
     * Note that the method does NOT change state of this reader, but
     * rather construct and returns a newly configured instance.
     */
    public ObjectWriter with(DateFormat df) {
        SerializationConfig newConfig = _config.with(df);
        return (newConfig == _config) ? this : _new(this, newConfig);
    }

    /**
     * Method that will construct a new instance that will use the default
     * pretty printer for serialization.
     */
    public ObjectWriter withDefaultPrettyPrinter() {
        return with(new DefaultPrettyPrinter());
    }

    /**
     * Method that will construct a new instance that uses specified
     * provider for resolving filter instances by id.
     */
    public ObjectWriter with(FilterProvider filterProvider) {
        return (filterProvider == _config.getFilterProvider()) ? this
                 : _new(this, _config.withFilters(filterProvider));
    }

    /**
     * Method that will construct a new instance that will use specified pretty
     * printer (or, if null, will not do any pretty-printing)
     */
    public ObjectWriter with(PrettyPrinter pp) {
        GeneratorSettings genSet = _generatorSettings.with(pp);
        if (genSet == _generatorSettings) {
            return this;
        }
        return _new(genSet, _prefetch);
    }

    /**
     * Method for constructing a new instance with configuration that
     * specifies what root name to use for "root element wrapping".
     * See {@link SerializationConfig#withRootName(String)} for details.
     *<p>
     * Note that method does NOT change state of this reader, but
     * rather construct and returns a newly configured instance.
     */
    public ObjectWriter withRootName(String rootName) {
        SerializationConfig newConfig = _config.withRootName(rootName);
        return (newConfig == _config) ? this :  _new(this, newConfig);
    }

    /**
     * Method that will construct a new instance that uses specific format schema
     * for serialization.
     *<p>
     * Note that method does NOT change state of this reader, but
     * rather construct and returns a newly configured instance.
     */
    public ObjectWriter with(FormatSchema schema) {
        GeneratorSettings genSet = _generatorSettings.with(schema);
        if (genSet == _generatorSettings) {
            return this;
        }
        _verifySchemaType(schema);
        return _new(genSet, _prefetch);
    }

    /**
     * @deprecated Since 2.5 use {@link #with(FormatSchema)} instead
     */
    @Deprecated
    public ObjectWriter withSchema(FormatSchema schema) {
        return with(schema);
    }

    /**
     * Method that will construct a new instance that uses specific type
     * as the root type for serialization, instead of runtime dynamic
     * type of the root object itself.
     *<p>
     * Note that method does NOT change state of this reader, but
     * rather construct and returns a newly configured instance.
     * 
     * @since 2.5
     */
    public ObjectWriter forType(JavaType rootType)
    {
        Prefetch pf;
        if (rootType == null || rootType.hasRawClass(Object.class)) {
            pf = Prefetch.empty;
        } else {
            // 15-Mar-2013, tatu: Important! Indicate that static typing is needed:
            rootType = rootType.withStaticTyping();
            pf = _prefetchRootSerializer(_config, rootType);
        }
        return (pf == _prefetch) ? this : _new(_generatorSettings, pf);
    }    

    /**
     * Method that will construct a new instance that uses specific type
     * as the root type for serialization, instead of runtime dynamic
     * type of the root object itself.
     * 
     * @since 2.5
     */
    public ObjectWriter forType(Class<?> rootType) {
        if (rootType == Object.class) {
            return forType((JavaType) null);
        }
        return forType(_config.constructType(rootType));
    }

    public ObjectWriter forType(TypeReference<?> rootType) {
        return forType(_config.getTypeFactory().constructType(rootType.getType()));
    }

    /**
     * @deprecated since 2.5 Use {@link #forType(JavaType)} instead
     */
    @Deprecated // since 2.5
    public ObjectWriter withType(JavaType rootType) {
        return forType(rootType);
    }

    /**
     * @deprecated since 2.5 Use {@link #forType(Class)} instead
     */
    @Deprecated // since 2.5
    public ObjectWriter withType(Class<?> rootType) {
        return forType(rootType);
    }

    /**
     * @deprecated since 2.5 Use {@link #forType(TypeReference)} instead
     */
    @Deprecated // since 2.5
    public ObjectWriter withType(TypeReference<?> rootType) {
        return forType(rootType);
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
        return (newConfig == _config) ? this :  _new(this, newConfig);
    }    

    public ObjectWriter with(Locale l) {
        SerializationConfig newConfig = _config.with(l);
        return (newConfig == _config) ? this :  _new(this, newConfig);
    }

    public ObjectWriter with(TimeZone tz) {
        SerializationConfig newConfig = _config.with(tz);
        return (newConfig == _config) ? this :  _new(this, newConfig);
    }

    /**
     * Method that will construct a new instance that uses specified default
     * {@link Base64Variant} for base64 encoding
     * 
     * @since 2.1
     */
    public ObjectWriter with(Base64Variant b64variant) {
        SerializationConfig newConfig = _config.with(b64variant);
        return (newConfig == _config) ? this :  _new(this, newConfig);
    }

    /**
     * @since 2.3
     */
    public ObjectWriter with(CharacterEscapes escapes) {
        GeneratorSettings genSet = _generatorSettings.with(escapes);
        if (genSet == _generatorSettings) {
            return this;
        }
        return _new(genSet, _prefetch);
    }

    /**
     * @since 2.3
     */
    public ObjectWriter with(JsonFactory f) {
        return (f == _generatorFactory) ? this : _new(this, f);
    }    

    /**
     * @since 2.3
     */
    public ObjectWriter with(ContextAttributes attrs) {
        SerializationConfig newConfig = _config.with(attrs);
        return (newConfig == _config) ? this :  _new(this, newConfig);
    }

    /**
     * @since 2.3
     */
    public ObjectWriter withAttributes(Map<Object,Object> attrs) {
        SerializationConfig newConfig = _config.withAttributes(attrs);
        return (newConfig == _config) ? this :  _new(this, newConfig);
    }

    /**
     * @since 2.3
     */
    public ObjectWriter withAttribute(Object key, Object value) {
        SerializationConfig newConfig = _config.withAttribute(key, value);
        return (newConfig == _config) ? this :  _new(this, newConfig);
    }

    /**
     * @since 2.3
     */
    public ObjectWriter withoutAttribute(Object key) {
        SerializationConfig newConfig = _config.withoutAttribute(key);
        return (newConfig == _config) ? this :  _new(this, newConfig);
    }

    /**
     * @since 2.5
     */
    public ObjectWriter withRootValueSeparator(String sep) {
        GeneratorSettings genSet = _generatorSettings.withRootValueSeparator(sep);
        if (genSet == _generatorSettings) {
            return this;
        }
        return _new(genSet, _prefetch);
    }

    /**
     * @since 2.5
     */
    public ObjectWriter withRootValueSeparator(SerializableString sep) {
        GeneratorSettings genSet = _generatorSettings.withRootValueSeparator(sep);
        if (genSet == _generatorSettings) {
            return this;
        }
        return _new(genSet, _prefetch);
    }

    /*
    /**********************************************************
    /* Factory methods for sequence writers (2.5)
    /**********************************************************
     */

    /**
     * Method for creating a {@link SequenceWriter} to write a sequence of root
     * values using configuration of this {@link ObjectWriter}.
     * Sequence is not surrounded by JSON array; some backend types may not
     * support writing of such sequences as root level.
     * Resulting writer needs to be {@link SequenceWriter#close()}d after all
     * values have been written to ensure closing of underlying generator and
     * output stream.
     *
     * @param out Target file to write value sequence to.
     *
     * @since 2.5
     */
    public SequenceWriter writeValues(File out) throws IOException {
        return _newSequenceWriter(false,
                _generatorFactory.createGenerator(out, JsonEncoding.UTF8), true);
    }

    /**
     * Method for creating a {@link SequenceWriter} to write a sequence of root
     * values using configuration of this {@link ObjectWriter}.
     * Sequence is not surrounded by JSON array; some backend types may not
     * support writing of such sequences as root level.
     * Resulting writer needs to be {@link SequenceWriter#close()}d after all
     * values have been written to ensure that all content gets flushed by
     * the generator. However, since a {@link JsonGenerator} is explicitly passed,
     * it will NOT be closed when {@link SequenceWriter#close()} is called.
     *
     * @param gen Low-level generator caller has already constructed that will
     *   be used for actual writing of token stream.
     *
     * @since 2.5
     */
    public SequenceWriter writeValues(JsonGenerator gen) throws IOException {
        return _newSequenceWriter(false, _configureGenerator(gen), false);
    }

    /**
     * Method for creating a {@link SequenceWriter} to write a sequence of root
     * values using configuration of this {@link ObjectWriter}.
     * Sequence is not surrounded by JSON array; some backend types may not
     * support writing of such sequences as root level.
     * Resulting writer needs to be {@link SequenceWriter#close()}d after all
     * values have been written to ensure closing of underlying generator and
     * output stream.
     *
     * @param out Target writer to use for writing the token stream
     *
     * @since 2.5
     */
    public SequenceWriter writeValues(Writer out) throws IOException {
        return _newSequenceWriter(false,
                _generatorFactory.createGenerator(out), true);
    }

    /**
     * Method for creating a {@link SequenceWriter} to write a sequence of root
     * values using configuration of this {@link ObjectWriter}.
     * Sequence is not surrounded by JSON array; some backend types may not
     * support writing of such sequences as root level.
     * Resulting writer needs to be {@link SequenceWriter#close()}d after all
     * values have been written to ensure closing of underlying generator and
     * output stream.
     *
     * @param out Physical output stream to use for writing the token stream
     *
     * @since 2.5
     */
    public SequenceWriter writeValues(OutputStream out) throws IOException {
        return _newSequenceWriter(false,
                _generatorFactory.createGenerator(out, JsonEncoding.UTF8), true);
    }

    /**
     * Method for creating a {@link SequenceWriter} to write an array of
     * root-level values, using configuration of this {@link ObjectWriter}.
     * Resulting writer needs to be {@link SequenceWriter#close()}d after all
     * values have been written to ensure closing of underlying generator and
     * output stream.
     *<p>
     * Note that the type to use with {@link ObjectWriter#forType(Class)} needs to
     * be type of individual values (elements) to write and NOT matching array
     * or {@link java.util.Collection} type.
     *
     * @param out File to write token stream to
     *
     * @since 2.5
     */
    public SequenceWriter writeValuesAsArray(File out) throws IOException {
        return _newSequenceWriter(true,
                _generatorFactory.createGenerator(out, JsonEncoding.UTF8), true);
    }

    /**
     * Method for creating a {@link SequenceWriter} to write an array of
     * root-level values, using configuration of this {@link ObjectWriter}.
     * Resulting writer needs to be {@link SequenceWriter#close()}d after all
     * values have been written to ensure that all content gets flushed by
     * the generator. However, since a {@link JsonGenerator} is explicitly passed,
     * it will NOT be closed when {@link SequenceWriter#close()} is called.
     *<p>
     * Note that the type to use with {@link ObjectWriter#forType(Class)} needs to
     * be type of individual values (elements) to write and NOT matching array
     * or {@link java.util.Collection} type.
     *
     * @param gen Underlying generator to use for writing the token stream
     *
     * @since 2.5
     */
    public SequenceWriter writeValuesAsArray(JsonGenerator gen) throws IOException {
        return _newSequenceWriter(true, gen, false);
    }

    /**
     * Method for creating a {@link SequenceWriter} to write an array of
     * root-level values, using configuration of this {@link ObjectWriter}.
     * Resulting writer needs to be {@link SequenceWriter#close()}d after all
     * values have been written to ensure closing of underlying generator and
     * output stream.
     *<p>
     * Note that the type to use with {@link ObjectWriter#forType(Class)} needs to
     * be type of individual values (elements) to write and NOT matching array
     * or {@link java.util.Collection} type.
     *
     * @param out Writer to use for writing the token stream
     *
     * @since 2.5
     */
    public SequenceWriter writeValuesAsArray(Writer out) throws IOException {
        return _newSequenceWriter(true, _generatorFactory.createGenerator(out), true);
    }

    /**
     * Method for creating a {@link SequenceWriter} to write an array of
     * root-level values, using configuration of this {@link ObjectWriter}.
     * Resulting writer needs to be {@link SequenceWriter#close()}d after all
     * values have been written to ensure closing of underlying generator and
     * output stream.
     *<p>
     * Note that the type to use with {@link ObjectWriter#forType(Class)} needs to
     * be type of individual values (elements) to write and NOT matching array
     * or {@link java.util.Collection} type.
     *
     * @param out Physical output stream to use for writing the token stream
     *
     * @since 2.5
     */
    public SequenceWriter writeValuesAsArray(OutputStream out) throws IOException {
        return _newSequenceWriter(true,
                _generatorFactory.createGenerator(out, JsonEncoding.UTF8), true);
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
        return _generatorFactory.isEnabled(f);
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
        return _generatorFactory;
    }

    /**
     * @since 2.2
     */
    public JsonFactory getFactory() {
        return _generatorFactory;
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
        return _prefetch.hasSerializer();
    }

    /**
     * @since 2.3
     */
    public ContextAttributes getAttributes() {
        return _config.getAttributes();
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
    public void writeValue(JsonGenerator gen, Object value)
        throws IOException, JsonGenerationException, JsonMappingException
    {
        _configureGenerator(gen);
        if (_config.isEnabled(SerializationFeature.CLOSE_CLOSEABLE)
                && (value instanceof Closeable)) {
            _writeCloseableValue(gen, value, _config);
        } else {
            if (_prefetch.valueSerializer != null) {
                _serializerProvider(_config).serializeValue(gen, value, _prefetch.rootType,
                        _prefetch.valueSerializer);
            } else if (_prefetch.typeSerializer != null) {
                _serializerProvider(_config).serializePolymorphic(gen, value, _prefetch.typeSerializer);
            } else {
                _serializerProvider(_config).serializeValue(gen, value);
            }
            if (_config.isEnabled(SerializationFeature.FLUSH_AFTER_WRITE_VALUE)) {
                gen.flush();
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
        _configAndWriteValue(_generatorFactory.createGenerator(resultFile, JsonEncoding.UTF8), value);
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
        _configAndWriteValue(_generatorFactory.createGenerator(out, JsonEncoding.UTF8), value);
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
        _configAndWriteValue(_generatorFactory.createGenerator(w), value);
    }

    /**
     * Method that can be used to serialize any Java value as
     * a String. Functionally equivalent to calling
     * {@link #writeValue(Writer,Object)} with {@link java.io.StringWriter}
     * and constructing String, but more efficient.
     *<p>
     * Note: prior to version 2.1, throws clause included {@link IOException}; 2.1 removed it.
     */
    @SuppressWarnings("resource")
    public String writeValueAsString(Object value)
        throws JsonProcessingException
    {        
        // alas, we have to pull the recycler directly here...
        SegmentedStringWriter sw = new SegmentedStringWriter(_generatorFactory._getBufferRecycler());
        try {
            _configAndWriteValue(_generatorFactory.createGenerator(sw), value);
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
    @SuppressWarnings("resource")
    public byte[] writeValueAsBytes(Object value)
        throws JsonProcessingException
    {
        ByteArrayBuilder bb = new ByteArrayBuilder(_generatorFactory._getBufferRecycler());
        try {
            _configAndWriteValue(_generatorFactory.createGenerator(bb, JsonEncoding.UTF8), value);
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
        return _serializerProvider(_config).hasSerializerFor(type, null);
    }

    /**
     * Method for checking whether instances of given type can be serialized,
     * and optionally why (as per {@link Throwable} returned).
     * 
     * @since 2.3
     */
    public boolean canSerialize(Class<?> type, AtomicReference<Throwable> cause) {
        return _serializerProvider(_config).hasSerializerFor(type, cause);
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
            if (!_generatorFactory.canUseSchema(schema)) {
                    throw new IllegalArgumentException("Can not use FormatSchema of type "+schema.getClass().getName()
                            +" for format "+_generatorFactory.getFormatName());
            }
        }
    }

    /**
     * Method called to configure the generator as necessary and then
     * call write functionality
     */
    protected final void _configAndWriteValue(JsonGenerator gen, Object value) throws IOException
    {
        _configureGenerator(gen);
        // [JACKSON-282]: consider Closeable
        if (_config.isEnabled(SerializationFeature.CLOSE_CLOSEABLE) && (value instanceof Closeable)) {
            _writeCloseable(gen, value, _config);
            return;
        }
        boolean closed = false;
        try {
            if (_prefetch.valueSerializer != null) {
                _serializerProvider(_config).serializeValue(gen, value, _prefetch.rootType,
                        _prefetch.valueSerializer);
            } else if (_prefetch.typeSerializer != null) {
                _serializerProvider(_config).serializePolymorphic(gen, value, _prefetch.typeSerializer);
            } else {
                _serializerProvider(_config).serializeValue(gen, value);
            }
            closed = true;
            gen.close();
        } finally {
            /* won't try to close twice; also, must catch exception (so it 
             * will not mask exception that is pending)
             */
            if (!closed) {
                /* 04-Mar-2014, tatu: But! Let's try to prevent auto-closing of
                 *    structures, which typically causes more damage.
                 */
                gen.disable(JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT);
                try {
                    gen.close();
                } catch (IOException ioe) { }
            }
        }
    }

    /**
     * Helper method used when value to serialize is {@link Closeable} and its <code>close()</code>
     * method is to be called right after serialization has been called
     */
    private final void _writeCloseable(JsonGenerator gen, Object value, SerializationConfig cfg)
        throws IOException
    {
        Closeable toClose = (Closeable) value;
        try {
            if (_prefetch.valueSerializer != null) {
                _serializerProvider(cfg).serializeValue(gen, value, _prefetch.rootType,
                        _prefetch.valueSerializer);
            } else if (_prefetch.typeSerializer != null) {
                _serializerProvider(cfg).serializePolymorphic(gen, value, _prefetch.typeSerializer);
            } else {
                _serializerProvider(cfg).serializeValue(gen, value);
            }
            JsonGenerator tmpGen = gen;
            gen = null;
            tmpGen.close();
            Closeable tmpToClose = toClose;
            toClose = null;
            tmpToClose.close();
        } finally {
            /* Need to close both generator and value, as long as they haven't yet
             * been closed
             */
            if (gen != null) {
                /* 04-Mar-2014, tatu: But! Let's try to prevent auto-closing of
                 *    structures, which typically causes more damage.
                 */
                gen.disable(JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT);
                try {
                    gen.close();
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
    private final void _writeCloseableValue(JsonGenerator gen, Object value, SerializationConfig cfg)
        throws IOException
    {
        Closeable toClose = (Closeable) value;
        try {
            if (_prefetch.valueSerializer != null) {
                _serializerProvider(cfg).serializeValue(gen, value, _prefetch.rootType,
                        _prefetch.valueSerializer);
            } else if (_prefetch.typeSerializer != null) {
                _serializerProvider(cfg).serializePolymorphic(gen, value, _prefetch.typeSerializer);
            } else {
                _serializerProvider(cfg).serializeValue(gen, value);
            }
            if (_config.isEnabled(SerializationFeature.FLUSH_AFTER_WRITE_VALUE)) {
                gen.flush();
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
    protected Prefetch _prefetchRootSerializer(SerializationConfig config, JavaType valueType)
    {
        if (valueType != null && _config.isEnabled(SerializationFeature.EAGER_SERIALIZER_FETCH)) {
            /* 17-Dec-2014, tatu: Need to be bit careful here; TypeSerializers are NOT cached,
             *   so although it'd seem like a good idea to look for those first, and avoid
             *   serializer for polymorphic types, it is actually more efficient to do the
             *   reverse here.
             */
            try {
                JsonSerializer<Object> ser = _serializerProvider(config).findTypedValueSerializer(valueType, true, null);
                // Important: for polymorphic types, "unwrap"...
                if (ser instanceof TypeWrappedSerializer) {
                    return Prefetch.construct(valueType, ((TypeWrappedSerializer) ser).typeSerializer());
                }
                return Prefetch.construct(valueType,  ser);
            } catch (JsonProcessingException e) {
                // need to swallow?
                ;
            }
        }
        return Prefetch.empty;
    }
    
    /**
     * Helper method called to set or override settings of passed-in
     * {@link JsonGenerator}
     * 
     * @since 2.1
     * 
     * @deprecated Since 2.5 (to be removed from 2.6 or later)
     */
    @Deprecated
    protected void _configureJsonGenerator(JsonGenerator gen) {
        _configureGenerator(gen);
    }

    /**
     * Helper method called to set or override settings of passed-in
     * {@link JsonGenerator}
     * 
     * @since 2.5
     */
    protected JsonGenerator _configureGenerator(JsonGenerator gen)
    {
        GeneratorSettings genSet = _generatorSettings;
        PrettyPrinter pp = genSet.prettyPrinter;
        if (pp != null) {
            if (pp == NULL_PRETTY_PRINTER) {
                gen.setPrettyPrinter(null);
            } else {
                /* [JACKSON-851]: Better take care of stateful PrettyPrinters...
                 *   like the DefaultPrettyPrinter.
                 */
                if (pp instanceof Instantiatable<?>) {
                    pp = (PrettyPrinter) ((Instantiatable<?>) pp).createInstance();
                }
                gen.setPrettyPrinter(pp);
            }
        } else if (_config.isEnabled(SerializationFeature.INDENT_OUTPUT)) {
            gen.useDefaultPrettyPrinter();
        }
        CharacterEscapes esc = genSet.characterEscapes;
        if (esc != null) {
            gen.setCharacterEscapes(esc);
        }
        FormatSchema sch = genSet.schema;
        if (sch != null) {
            gen.setSchema(sch);
        }
        SerializableString sep = genSet.rootValueSeparator;
        if (sep != null) {
            gen.setRootValueSeparator(sep);
        }
        _config.initialize(gen); // since 2.5
        return gen;
    }

    /*
    /**********************************************************
    /* Helper classes for configuration
    /**********************************************************
     */

    /**
     * Helper class used for containing settings specifically related
     * to (re)configuring {@link JsonGenerator} constructed for
     * writing output.
     * 
     * @since 2.5
     */
    public final static class GeneratorSettings
        implements java.io.Serializable
    {
        private static final long serialVersionUID = 1L;

        public final static GeneratorSettings empty = new GeneratorSettings(null, null, null, null);

        /**
         * To allow for dynamic enabling/disabling of pretty printing,
         * pretty printer can be optionally configured for writer
         * as well
         */
        public final PrettyPrinter prettyPrinter;

        /**
         * When using data format that uses a schema, schema is passed
         * to generator.
         */
        public final FormatSchema schema;

        /**
         * Caller may want to specify character escaping details, either as
         * defaults, or on call-by-call basis.
         */
        public final CharacterEscapes characterEscapes;

        /**
         * Caller may want to override so-called "root value separator",
         * String added (verbatim, with no quoting or escaping) between
         * values in root context. Default value is a single space character,
         * but this is often changed to linefeed.
         */
        public final SerializableString rootValueSeparator;

        public GeneratorSettings(PrettyPrinter pp, FormatSchema sch,
                CharacterEscapes esc, SerializableString rootSep) {
            prettyPrinter = pp;
            schema = sch;
            characterEscapes = esc;
            rootValueSeparator = rootSep;
        }

        public GeneratorSettings with(PrettyPrinter pp) {
            // since null would mean "don't care", need to use placeholder to indicate "disable"
            if (pp == null) {
                pp = NULL_PRETTY_PRINTER;
            }
            return (pp == prettyPrinter) ? this
                    : new GeneratorSettings(pp, schema, characterEscapes, rootValueSeparator);
        }

        public GeneratorSettings with(FormatSchema sch) {
            return (schema == sch) ? this
                    : new GeneratorSettings(prettyPrinter, sch, characterEscapes, rootValueSeparator);
        }

        public GeneratorSettings with(CharacterEscapes esc) {
            return (characterEscapes == esc) ? this
                    : new GeneratorSettings(prettyPrinter, schema, esc, rootValueSeparator);
        }

        public GeneratorSettings withRootValueSeparator(String sep) {
            if (sep == null) {
                if (rootValueSeparator == null) {
                    return this;
                }
            } else if (sep.equals(rootValueSeparator)) {
                return this;
            }
            return new GeneratorSettings(prettyPrinter, schema, characterEscapes,
                    (sep == null) ? null : new SerializedString(sep));
        }

        public GeneratorSettings withRootValueSeparator(SerializableString sep) {
            if (sep == null) {
                if (rootValueSeparator == null) {
                    return this;
                }
            } else {
                if (rootValueSeparator != null
                        && sep.getValue().equals(rootValueSeparator.getValue())) {
                    return this;
                }
            }
            return new GeneratorSettings(prettyPrinter, schema, characterEscapes, sep);
        }
    }

    /**
     * As a minor optimization, we will make an effort to pre-fetch a serializer,
     * or at least relevant <code>TypeSerializer</code>, if given enough
     * information.
     * 
     * @since 2.5
     */
    public final static class Prefetch
        implements java.io.Serializable
    {
        private static final long serialVersionUID = 1L;

        public final static Prefetch empty = new Prefetch(null, null, null);
        
        /**
         * Specified root serialization type to use; can be same
         * as runtime type, but usually one of its super types
         */
        public final JavaType rootType;

        /**
         * We may pre-fetch serializer if {@link #rootType}
         * is known, and if so, reuse it afterwards.
         * This allows avoiding further serializer lookups and increases
         * performance a bit on cases where readers are reused.
         */
        public final JsonSerializer<Object> valueSerializer;

        /**
         * When dealing with polymorphic types, we can not pre-fetch
         * serializer, but we can pre-fetch {@link TypeSerializer}.
         */
        public final TypeSerializer typeSerializer;
        
        private Prefetch(JavaType type, JsonSerializer<Object> ser, TypeSerializer typeSer)
        {
            rootType = type;
            valueSerializer = ser;
            typeSerializer = typeSer;
        }

        public static Prefetch construct(JavaType type, JsonSerializer<Object> ser) {
            if (type == null && ser == null) {
                return empty;
            }
            return new Prefetch(type, ser, null);
        }
        
        public static Prefetch construct(JavaType type, TypeSerializer typeSer) {
            if (type == null && typeSer == null) {
                return empty;
            }
            return new Prefetch(type, null, typeSer);
        }

        public boolean hasSerializer() {
            return (valueSerializer != null) || (typeSerializer != null);
        }
    }
}
