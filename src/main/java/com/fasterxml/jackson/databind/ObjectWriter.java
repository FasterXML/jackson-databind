package com.fasterxml.jackson.databind;

import java.io.*;
import java.text.*;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.io.SegmentedStringWriter;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.*;
import com.fasterxml.jackson.databind.cfg.ContextAttributes;
import com.fasterxml.jackson.databind.cfg.DatatypeFeature;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.*;
import com.fasterxml.jackson.databind.ser.impl.TypeWrappedSerializer;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.ClassUtil;

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
     * We may pre-fetch serializer if root type
     * is known (has been explicitly declared), and if so, reuse it afterwards.
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

        if (rootType == null) {
            _prefetch = Prefetch.empty;
        } else if (rootType.hasRawClass(Object.class)) {
            // 15-Sep-2019, tatu: There is no "untyped serializer", but...
            //     as per [databind#1093] we do need `TypeSerializer`
            _prefetch = Prefetch.empty.forRootType(this, rootType);
        } else {
            _prefetch = Prefetch.empty.forRootType(this, rootType.withStaticTyping());
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

        _generatorSettings = GeneratorSettings.empty;
        _prefetch = Prefetch.empty;
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

        _generatorSettings = (s == null) ? GeneratorSettings.empty
                : new GeneratorSettings(null, s, null, null);
        _prefetch = Prefetch.empty;
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
        _generatorFactory = f;

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
    /**********************************************************************
    /* Internal factory methods, for convenience
    /**********************************************************************
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
        if (config == _config) {
            return this;
        }
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
        if ((_generatorSettings == genSettings) && (_prefetch == prefetch)) {
            return this;
        }
        return new ObjectWriter(this, _config, genSettings, prefetch);
    }

    /**
     * Overridable factory method called by {@link #writeValues(OutputStream)}
     * method (and its various overrides), and initializes it as necessary.
     *
     * @since 2.5
     */
    @SuppressWarnings("resource")
    protected SequenceWriter _newSequenceWriter(boolean wrapInArray,
            JsonGenerator gen, boolean managedInput)
        throws IOException
    {
        return new SequenceWriter(_serializerProvider(),
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
        return _new(this,  _config.with(feature));
    }

    /**
     * Method for constructing a new instance that is configured
     * with specified features enabled.
     */
    public ObjectWriter with(SerializationFeature first, SerializationFeature... other) {
        return _new(this, _config.with(first, other));
    }

    /**
     * Method for constructing a new instance that is configured
     * with specified features enabled.
     */
    public ObjectWriter withFeatures(SerializationFeature... features) {
        return _new(this, _config.withFeatures(features));
    }

    /**
     * Method for constructing a new instance that is configured
     * with specified feature disabled.
     */
    public ObjectWriter without(SerializationFeature feature) {
        return _new(this, _config.without(feature));
    }

    /**
     * Method for constructing a new instance that is configured
     * with specified features disabled.
     */
    public ObjectWriter without(SerializationFeature first, SerializationFeature... other) {
        return _new(this, _config.without(first, other));
    }

    /**
     * Method for constructing a new instance that is configured
     * with specified features disabled.
     */
    public ObjectWriter withoutFeatures(SerializationFeature... features) {
        return _new(this, _config.withoutFeatures(features));
    }

    /*
    /**********************************************************************
    /* Life-cycle, fluent factories for DatatypeFeature (2.14+)
    /**********************************************************************
     */

    /**
     * Method for constructing a new instance that is configured
     * with specified feature enabled.
     *
     * @since 2.14
     */
    public ObjectWriter with(DatatypeFeature feature)  {
        return _new(this,  _config.with(feature));
    }

    /**
     * Method for constructing a new instance that is configured
     * with specified features enabled.
     *
     * @since 2.14
     */
    public ObjectWriter withFeatures(DatatypeFeature... features) {
        return _new(this, _config.withFeatures(features));
    }

    /**
     * Method for constructing a new instance that is configured
     * with specified feature disabled.
     *
     * @since 2.14
     */
    public ObjectWriter without(DatatypeFeature feature) {
        return _new(this, _config.without(feature));
    }

    /**
     * Method for constructing a new instance that is configured
     * with specified features disabled.
     *
     * @since 2.14
     */
    public ObjectWriter withoutFeatures(DatatypeFeature... features) {
        return _new(this, _config.withoutFeatures(features));
    }

    /*
    /**********************************************************
    /* Life-cycle, fluent factories for JsonGenerator.Feature (2.5)
    /**********************************************************
     */

    /**
     * @since 2.5
     */
    public ObjectWriter with(JsonGenerator.Feature feature)  {
        return _new(this, _config.with(feature));
    }

    /**
     * @since 2.5
     */
    public ObjectWriter withFeatures(JsonGenerator.Feature... features) {
        return _new(this, _config.withFeatures(features));
    }

    /**
     * @since 2.5
     */
    public ObjectWriter without(JsonGenerator.Feature feature) {
        return _new(this, _config.without(feature));
    }

    /**
     * @since 2.5
     */
    public ObjectWriter withoutFeatures(JsonGenerator.Feature... features) {
        return _new(this, _config.withoutFeatures(features));
    }

    /*
    /**********************************************************
    /* Life-cycle, fluent factories for StreamWriteFeature (2.11)
    /**********************************************************
     */

    /**
     * @since 2.11
     */
    public ObjectWriter with(StreamWriteFeature feature)  {
        return _new(this, _config.with(feature.mappedFeature()));
    }

    /**
     * @since 2.11
     */
    public ObjectWriter without(StreamWriteFeature feature) {
        return _new(this, _config.without(feature.mappedFeature()));
    }

    /*
    /**********************************************************
    /* Life-cycle, fluent factories for FormatFeature (2.7)
    /**********************************************************
     */

    /**
     * @since 2.7
     */
    public ObjectWriter with(FormatFeature feature)  {
        return _new(this, _config.with(feature));
    }

    /**
     * @since 2.7
     */
    public ObjectWriter withFeatures(FormatFeature... features) {
        return _new(this, _config.withFeatures(features));
    }

    /**
     * @since 2.7
     */
    public ObjectWriter without(FormatFeature feature) {
        return _new(this, _config.without(feature));
    }

    /**
     * @since 2.7
     */
    public ObjectWriter withoutFeatures(FormatFeature... features) {
        return _new(this, _config.withoutFeatures(features));
    }

    /*
    /**********************************************************
    /* Life-cycle, fluent factories, type-related
    /**********************************************************
     */

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
    public ObjectWriter forType(JavaType rootType) {
        return _new(_generatorSettings, _prefetch.forRootType(this, rootType));
    }

    /**
     * Method that will construct a new instance that uses specific type
     * as the root type for serialization, instead of runtime dynamic
     * type of the root object itself.
     *
     * @since 2.5
     */
    public ObjectWriter forType(Class<?> rootType) {
        return forType(_config.constructType(rootType));
    }

    /**
     * Method that will construct a new instance that uses specific type
     * as the root type for serialization, instead of runtime dynamic
     * type of the root object itself.
     *
     * @since 2.5
     */
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
        return _new(this, _config.with(df));
    }

    /**
     * Method that will construct a new instance that will use the default
     * pretty printer for serialization.
     */
    public ObjectWriter withDefaultPrettyPrinter() {
        return with(_config.getDefaultPrettyPrinter());
    }

    /**
     * Method that will construct a new instance that uses specified
     * provider for resolving filter instances by id.
     */
    public ObjectWriter with(FilterProvider filterProvider) {
        if (filterProvider == _config.getFilterProvider()) {
            return this;
        }
        return _new(this, _config.withFilters(filterProvider));
    }

    /**
     * Method that will construct a new instance that will use specified pretty
     * printer (or, if null, will not do any pretty-printing)
     */
    public ObjectWriter with(PrettyPrinter pp) {
        return _new(_generatorSettings.with(pp), _prefetch);
    }

    /**
     * Method for constructing a new instance with configuration that
     * specifies what root name to use for "root element wrapping".
     * See {@link SerializationConfig#withRootName(String)} for details.
     *<p>
     * Note that method does NOT change state of this reader, but
     * rather construct and returns a newly configured instance.
     *
     * @param rootName Root name to use, if non-empty; `null` for "use defaults",
     *    and empty String ("") for "do NOT add root wrapper"
     */
    public ObjectWriter withRootName(String rootName) {
        return _new(this, _config.withRootName(rootName));
    }

    /**
     * @since 2.6
     */
    public ObjectWriter withRootName(PropertyName rootName) {
        return _new(this, _config.withRootName(rootName));
    }

    /**
     * Convenience method that is same as calling:
     *<code>
     *   withRootName("")
     *</code>
     * which will forcibly prevent use of root name wrapping when writing
     * values with this {@link ObjectWriter}.
     *
     * @since 2.6
     */
    public ObjectWriter withoutRootName() {
        return _new(this, _config.withRootName(PropertyName.NO_NAME));
    }

    /**
     * Method that will construct a new instance that uses specific format schema
     * for serialization.
     *<p>
     * Note that method does NOT change state of this reader, but
     * rather construct and returns a newly configured instance.
     */
    public ObjectWriter with(FormatSchema schema) {
        _verifySchemaType(schema);
        return _new(_generatorSettings.with(schema), _prefetch);
    }

    /**
     * @deprecated Since 2.5 use {@link #with(FormatSchema)} instead
     */
    @Deprecated
    public ObjectWriter withSchema(FormatSchema schema) {
        return with(schema);
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
        return _new(this, _config.withView(view));
    }

    public ObjectWriter with(Locale l) {
        return _new(this, _config.with(l));
    }

    public ObjectWriter with(TimeZone tz) {
        return _new(this, _config.with(tz));
    }

    /**
     * Method that will construct a new instance that uses specified default
     * {@link Base64Variant} for base64 encoding
     *
     * @since 2.1
     */
    public ObjectWriter with(Base64Variant b64variant) {
        return _new(this, _config.with(b64variant));
    }

    /**
     * @since 2.3
     */
    public ObjectWriter with(CharacterEscapes escapes) {
        return _new(_generatorSettings.with(escapes), _prefetch);
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
        return _new(this, _config.with(attrs));
    }

    /**
     * Mutant factory method that allows construction of a new writer instance
     * that uses specified set of default attribute values.
     *
     * @since 2.3
     */
    public ObjectWriter withAttributes(Map<?,?> attrs) {
        return _new(this, _config.withAttributes(attrs));
    }

    /**
     * @since 2.3
     */
    public ObjectWriter withAttribute(Object key, Object value) {
        return _new(this, _config.withAttribute(key, value));
    }

    /**
     * @since 2.3
     */
    public ObjectWriter withoutAttribute(Object key) {
        return _new(this, _config.withoutAttribute(key));
    }

    /**
     * @since 2.5
     */
    public ObjectWriter withRootValueSeparator(String sep) {
        return _new(_generatorSettings.withRootValueSeparator(sep), _prefetch);
    }

    /**
     * @since 2.5
     */
    public ObjectWriter withRootValueSeparator(SerializableString sep) {
        return _new(_generatorSettings.withRootValueSeparator(sep), _prefetch);
    }

    /*
    /**********************************************************
    /* Factory methods for creating JsonGenerators (added in 2.11)
    /**********************************************************
     */

    /**
     * Factory method for constructing properly initialized {@link JsonGenerator}
     * to write content using specified {@link OutputStream}.
     * Generator is not managed (or "owned") by ObjectWriter: caller is responsible
     * for properly closing it once content generation is complete.
     *
     * @since 2.11
     */
    public JsonGenerator createGenerator(OutputStream out) throws IOException {
        _assertNotNull("out", out);
        return _configureGenerator(_generatorFactory.createGenerator(out, JsonEncoding.UTF8));
    }

    /**
     * Factory method for constructing properly initialized {@link JsonGenerator}
     * to write content using specified {@link OutputStream} and encoding.
     * Generator is not managed (or "owned") by ObjectWriter: caller is responsible
     * for properly closing it once content generation is complete.
     *
     * @since 2.11
     */
    public JsonGenerator createGenerator(OutputStream out, JsonEncoding enc) throws IOException {
        _assertNotNull("out", out);
        return _configureGenerator(_generatorFactory.createGenerator(out, enc));
    }

    /**
     * Factory method for constructing properly initialized {@link JsonGenerator}
     * to write content using specified {@link Writer}.
     * Generator is not managed (or "owned") by ObjectWriter: caller is responsible
     * for properly closing it once content generation is complete.
     *
     * @since 2.11
     */
    public JsonGenerator createGenerator(Writer w) throws IOException {
        _assertNotNull("w", w);
        return _configureGenerator(_generatorFactory.createGenerator(w));
    }

    /**
     * Factory method for constructing properly initialized {@link JsonGenerator}
     * to write content to specified {@link File}, using specified encoding.
     * Generator is not managed (or "owned") by ObjectWriter: caller is responsible
     * for properly closing it once content generation is complete.
     *
     * @since 2.11
     */
    public JsonGenerator createGenerator(File outputFile, JsonEncoding enc) throws IOException {
        _assertNotNull("outputFile", outputFile);
        return _configureGenerator(_generatorFactory.createGenerator(outputFile, enc));
    }

    /**
     * Factory method for constructing properly initialized {@link JsonGenerator}
     * to write content using specified {@link DataOutput}.
     * Generator is not managed (or "owned") by ObjectWriter: caller is responsible
     * for properly closing it once content generation is complete.
     *
     * @since 2.11
     */
    public JsonGenerator createGenerator(DataOutput out) throws IOException {
        _assertNotNull("out", out);
        return _configureGenerator(_generatorFactory.createGenerator(out));
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
        return _newSequenceWriter(false, createGenerator(out, JsonEncoding.UTF8), true);
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
     * @param g Low-level generator caller has already constructed that will
     *   be used for actual writing of token stream.
     *
     * @since 2.5
     */
    public SequenceWriter writeValues(JsonGenerator g) throws IOException {
        _assertNotNull("g", g);
        return _newSequenceWriter(false, _configureGenerator(g), false);
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
        return _newSequenceWriter(false, createGenerator(out), true);
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
        return _newSequenceWriter(false, createGenerator(out, JsonEncoding.UTF8), true);
    }

    /**
     * @since 2.8
     */
    public SequenceWriter writeValues(DataOutput out) throws IOException {
        return _newSequenceWriter(false, createGenerator(out), true);
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
        return _newSequenceWriter(true, createGenerator(out, JsonEncoding.UTF8), true);
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
        _assertNotNull("gen", gen);
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
        return _newSequenceWriter(true, createGenerator(out), true);
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
        return _newSequenceWriter(true, createGenerator(out, JsonEncoding.UTF8), true);
    }

    /**
     * @since 2.8
     */
    public SequenceWriter writeValuesAsArray(DataOutput out) throws IOException {
        return _newSequenceWriter(true, createGenerator(out), true);
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

    /**
     * @since 2.14
     */
    public boolean isEnabled(DatatypeFeature f) {
        return _config.isEnabled(f);
    }

    /**
     * @since 2.9
     */
    @Deprecated
    public boolean isEnabled(JsonParser.Feature f) {
        return _generatorFactory.isEnabled(f);
    }

    /**
     * @since 2.9
     */
    public boolean isEnabled(JsonGenerator.Feature f) {
        return _generatorFactory.isEnabled(f);
    }

    /**
     * @since 2.11
     */
    public boolean isEnabled(StreamWriteFeature f) {
        return _generatorFactory.isEnabled(f);
    }

    /**
     * @since 2.2
     */
    public SerializationConfig getConfig() {
        return _config;
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
     *<p>
     * Note that the given {@link JsonGenerator} is not closed; caller
     * is expected to handle that as necessary.
     */
    public void writeValue(JsonGenerator g, Object value) throws IOException
    {
        _assertNotNull("g", g);
        _configureGenerator(g);
        if (_config.isEnabled(SerializationFeature.CLOSE_CLOSEABLE)
                && (value instanceof Closeable)) {

            Closeable toClose = (Closeable) value;
            try {
                _prefetch.serialize(g, value, _serializerProvider());
                if (_config.isEnabled(SerializationFeature.FLUSH_AFTER_WRITE_VALUE)) {
                    g.flush();
                }
            } catch (Exception e) {
                ClassUtil.closeOnFailAndThrowAsIOE(null, toClose, e);
                return;
            }
            toClose.close();
        } else {
            _prefetch.serialize(g, value, _serializerProvider());
            if (_config.isEnabled(SerializationFeature.FLUSH_AFTER_WRITE_VALUE)) {
                g.flush();
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
        throws IOException, StreamWriteException, DatabindException
    {
        _writeValueAndClose(createGenerator(resultFile, JsonEncoding.UTF8), value);
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
        throws IOException, StreamWriteException, DatabindException
    {
        _writeValueAndClose(createGenerator(out, JsonEncoding.UTF8), value);
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
        throws IOException, StreamWriteException, DatabindException
    {
        _writeValueAndClose(createGenerator(w), value);
    }

    /**
     * @since 2.8
     */
    public void writeValue(DataOutput out, Object value)
        throws IOException, StreamWriteException, DatabindException
    {
        _writeValueAndClose(createGenerator(out), value);
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
        final BufferRecycler br = _generatorFactory._getBufferRecycler();
        try (SegmentedStringWriter sw = new SegmentedStringWriter(br)) {
            _writeValueAndClose(createGenerator(sw), value);
            return sw.getAndClear();
        } catch (JsonProcessingException e) {
            throw e;
        } catch (IOException e) { // shouldn't really happen, but is declared as possibility so:
            throw JsonMappingException.fromUnexpectedIOE(e);
        } finally {
            br.releaseToPool(); // since 2.17
        }
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
        final BufferRecycler br = _generatorFactory._getBufferRecycler();
        try (ByteArrayBuilder bb = new ByteArrayBuilder(br)) {
            _writeValueAndClose(createGenerator(bb, JsonEncoding.UTF8), value);
            return bb.getClearAndRelease();
        } catch (JsonProcessingException e) { // to support [JACKSON-758]
            throw e;
        } catch (IOException e) { // shouldn't really happen, but is declared as possibility so:
            throw JsonMappingException.fromUnexpectedIOE(e);
        } finally {
            br.releaseToPool(); // since 2.17
        }
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
        _assertNotNull("type", type);
        _assertNotNull("visitor", visitor);
        _serializerProvider().acceptJsonFormatVisitor(type, visitor);
    }

    /**
     * Since 2.6
     */
    public void acceptJsonFormatVisitor(Class<?> type, JsonFormatVisitorWrapper visitor)
        throws JsonMappingException
    {
        _assertNotNull("type", type);
        _assertNotNull("visitor", visitor);
        acceptJsonFormatVisitor(_config.constructType(type), visitor);
    }

    /**
     * Method that can be called to check whether {@code ObjectWriter} thinks
     * it could serialize an instance of given Class.
     *
     * @deprecated Since 2.18 use discouraged; method to be removed from Jackson 3.0
     */
    @Deprecated // @since 2.18
    public boolean canSerialize(Class<?> type) {
        _assertNotNull("type", type);
        return _serializerProvider().hasSerializerFor(type, null);
    }

    /**
     * Method for checking whether instances of given type can be serialized,
     * and optionally why (as per {@link Throwable} returned).
     *
     * @since 2.3
     *
     * @deprecated Since 2.18 use discouraged; method to be removed from Jackson 3.0
     */
    @Deprecated // @since 2.18
    public boolean canSerialize(Class<?> type, AtomicReference<Throwable> cause) {
        _assertNotNull("type", type);
        return _serializerProvider().hasSerializerFor(type, cause);
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
    protected DefaultSerializerProvider _serializerProvider() {
        return _serializerProvider.createInstance(_config, _serializerFactory);
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
                    throw new IllegalArgumentException("Cannot use FormatSchema of type "+schema.getClass().getName()
                            +" for format "+_generatorFactory.getFormatName());
            }
        }
    }

    /**
     * Method called to configure the generator as necessary and then
     * call write functionality
     *
     * @since 2.11.2
     */
    protected final void _writeValueAndClose(JsonGenerator gen, Object value) throws IOException
    {
        if (_config.isEnabled(SerializationFeature.CLOSE_CLOSEABLE) && (value instanceof Closeable)) {
            _writeCloseable(gen, value);
            return;
        }
        try {
            _prefetch.serialize(gen, value, _serializerProvider());
        } catch (Exception e) {
            ClassUtil.closeOnFailAndThrowAsIOE(gen, e);
            return;
        }
        gen.close();
    }

    /**
     * Helper method used when value to serialize is {@link Closeable} and its <code>close()</code>
     * method is to be called right after serialization has been called
     */
    private final void _writeCloseable(JsonGenerator gen, Object value)
        throws IOException
    {
        Closeable toClose = (Closeable) value;
        try {
            _prefetch.serialize(gen, value, _serializerProvider());
            Closeable tmpToClose = toClose;
            toClose = null;
            tmpToClose.close();
        } catch (Exception e) {
            ClassUtil.closeOnFailAndThrowAsIOE(gen, toClose, e);
            return;
        }
        gen.close();
    }

    /**
     * Helper method called to set or override settings of passed-in
     * {@link JsonGenerator}
     *
     * @since 2.5
     */
    protected final JsonGenerator _configureGenerator(JsonGenerator gen)
    {
        // order is slightly significant: both may change PrettyPrinter
        // settings.
        _config.initialize(gen); // since 2.5
        _generatorSettings.initialize(gen);
        return gen;
    }

    protected final void _assertNotNull(String paramName, Object src) {
        if (src == null) {
            throw new IllegalArgumentException(String.format("argument \"%s\" is null", paramName));
        }
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
                return new GeneratorSettings(prettyPrinter, schema, characterEscapes, null);
            }
            if (sep.equals(_rootValueSeparatorAsString())) {
                return this;
            }
            return new GeneratorSettings(prettyPrinter, schema, characterEscapes,
                    new SerializedString(sep));
        }

        public GeneratorSettings withRootValueSeparator(SerializableString sep) {
            if (sep == null) {
                if (rootValueSeparator == null) {
                    return this;
                }
                return new GeneratorSettings(prettyPrinter, schema, characterEscapes, null);
            }
            if (sep.equals(rootValueSeparator)) {
                return this;
            }
            return new GeneratorSettings(prettyPrinter, schema, characterEscapes, sep);
        }

        private final String _rootValueSeparatorAsString() {
            return (rootValueSeparator == null) ? null : rootValueSeparator.getValue();
        }

        /**
         * @since 2.6
         */
        public void initialize(JsonGenerator gen)
        {
            PrettyPrinter pp = prettyPrinter;
            if (prettyPrinter != null) {
                if (pp == NULL_PRETTY_PRINTER) {
                    gen.setPrettyPrinter(null);
                } else {
                    if (pp instanceof Instantiatable<?>) {
                        pp = (PrettyPrinter) ((Instantiatable<?>) pp).createInstance();
                    }
                    gen.setPrettyPrinter(pp);
                }
            }
            if (characterEscapes != null) {
                gen.setCharacterEscapes(characterEscapes);
            }
            if (schema != null) {
                gen.setSchema(schema);
            }
            if (rootValueSeparator != null) {
                gen.setRootValueSeparator(rootValueSeparator);
            }
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
         * (parent class or interface it implements).
         */
        private final JavaType rootType;

        /**
         * We may pre-fetch serializer if {@link #rootType}
         * is known, and if so, reuse it afterwards.
         * This allows avoiding further serializer lookups and increases
         * performance a bit on cases where readers are reused.
         */
        private final JsonSerializer<Object> valueSerializer;

        /**
         * When dealing with polymorphic types, we cannot pre-fetch
         * serializer, but can pre-fetch {@link TypeSerializer}.
         */
        private final TypeSerializer typeSerializer;

        private Prefetch(JavaType rootT,
                JsonSerializer<Object> ser, TypeSerializer typeSer)
        {
            rootType = rootT;
            valueSerializer = ser;
            typeSerializer = typeSer;
        }

        public Prefetch forRootType(ObjectWriter parent, JavaType newType) {
            // First: if nominal type not defined not thing much to do
            if (newType == null) {
                if ((rootType == null) || (valueSerializer == null)) {
                    return this;
                }
                return new Prefetch(null, null, null);
            }

            // Second: if no change, nothing to do either
            if (newType.equals(rootType)) {
                return this;
            }

            // But one more trick: `java.lang.Object` has no serialized, but may
            // have `TypeSerializer` to use
            if (newType.isJavaLangObject()) {
                DefaultSerializerProvider prov = parent._serializerProvider();
                TypeSerializer typeSer;

                try {
                    typeSer = prov.findTypeSerializer(newType);
                } catch (JsonMappingException e) {
                    // Unlike with value serializer pre-fetch, let's not allow exception
                    // for TypeSerializer be swallowed
                    throw new RuntimeJsonMappingException(e);
                }
                return new Prefetch(null, null, typeSer);
            }

            if (parent.isEnabled(SerializationFeature.EAGER_SERIALIZER_FETCH)) {
                DefaultSerializerProvider prov = parent._serializerProvider();
                // 17-Dec-2014, tatu: Need to be bit careful here; TypeSerializers are NOT cached,
                //   so although it'd seem like a good idea to look for those first, and avoid
                //   serializer for polymorphic types, it is actually more efficient to do the
                //   reverse here.
                try {
                    JsonSerializer<Object> ser = prov.findTypedValueSerializer(newType, true, null);
                    // Important: for polymorphic types, "unwrap"...
                    if (ser instanceof TypeWrappedSerializer) {
                        return new Prefetch(newType, null,
                                ((TypeWrappedSerializer) ser).typeSerializer());
                    }
                    return new Prefetch(newType, ser, null);
                } catch (DatabindException e) {
                    // need to swallow?
                }
            }
            return new Prefetch(newType, null, typeSerializer);
        }

        public final JsonSerializer<Object> getValueSerializer() {
            return valueSerializer;
        }

        public final TypeSerializer getTypeSerializer() {
            return typeSerializer;
        }

        public boolean hasSerializer() {
            return (valueSerializer != null) || (typeSerializer != null);
        }

        public void serialize(JsonGenerator gen, Object value, DefaultSerializerProvider prov)
            throws IOException
        {
            if (typeSerializer != null) {
                prov.serializePolymorphic(gen, value, rootType, valueSerializer, typeSerializer);
            } else  if (valueSerializer != null) {
                prov.serializeValue(gen, value, rootType, valueSerializer);
            } else if (rootType != null) {
                prov.serializeValue(gen, value, rootType);
            } else {
                prov.serializeValue(gen, value);
            }
        }
    }
}
