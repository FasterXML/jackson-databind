package tools.jackson.databind;

import java.io.*;
import java.nio.file.Path;
import java.text.*;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import tools.jackson.core.*;
import tools.jackson.core.exc.JacksonIOException;
import tools.jackson.core.io.CharacterEscapes;
import tools.jackson.core.io.SegmentedStringWriter;
import tools.jackson.core.type.TypeReference;
import tools.jackson.core.util.*;
import tools.jackson.databind.cfg.ContextAttributes;
import tools.jackson.databind.cfg.DatatypeFeature;
import tools.jackson.databind.cfg.GeneratorSettings;
import tools.jackson.databind.cfg.SerializationContexts;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.ser.*;
import tools.jackson.databind.ser.impl.TypeWrappedSerializer;
import tools.jackson.databind.type.TypeFactory;
import tools.jackson.databind.util.ClassUtil;
import tools.jackson.databind.util.TokenBuffer;

/**
 * Builder object that can be used for per-serialization configuration of
 * serialization parameters, such as JSON View and root type to use.
 * (and thus fully thread-safe with no external synchronization);
 * new instances are constructed for different configurations.
 * Instances are initially constructed by {@link ObjectMapper} and can be
 * reused in completely thread-safe manner with no explicit synchronization
 */
public class ObjectWriter
    implements Versioned
    // NOTE: since 3.x, NO LONGER JDK Serializable
{
    /*
    /**********************************************************************
    /* Immutable configuration from ObjectMapper
    /**********************************************************************
     */

    /**
     * General serialization configuration settings
     */
    protected final SerializationConfig _config;

    /**
     * Factory used for constructing per-call {@link SerializerProvider}s.
     *<p>
     * Note: while serializers are only exposed {@link SerializerProvider},
     * mappers and readers need to access additional API defined by
     * {@link SerializationContextExt}
     */
    protected final SerializationContexts _serializationContexts;

    /**
     * Factory used for constructing {@link JsonGenerator}s
     */
    protected final TokenStreamFactory _generatorFactory;

    /*
    /**********************************************************************
    /* Configuration that can be changed via mutant factories
    /**********************************************************************
     */

    /**
     * Container for settings that need to be passed to {@link JsonGenerator}
     * constructed for serializing values.
     */
    protected final GeneratorSettings _generatorSettings;

    /**
     * We may pre-fetch serializer if root type
     * is known (has been explicitly declared), and if so, reuse it afterwards.
     * This allows avoiding further serializer lookups and increases
     * performance a bit on cases where readers are reused.
     */
    protected final Prefetch _prefetch;

    /*
    /**********************************************************************
    /* Life-cycle, constructors
    /**********************************************************************
     */

    /**
     * Constructor used by {@link ObjectMapper} for initial instantiation
     */
    protected ObjectWriter(ObjectMapper mapper, SerializationConfig config,
            JavaType rootType, PrettyPrinter pp)
    {
        _config = config;
        _serializationContexts = mapper._serializationContexts;
        _generatorFactory = mapper._streamFactory;
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
        _serializationContexts = mapper._serializationContexts;
        _generatorFactory = mapper._streamFactory;

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

        _serializationContexts = mapper._serializationContexts;
        _generatorFactory = mapper._streamFactory;

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

        _serializationContexts = base._serializationContexts;
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

        _serializationContexts = base._serializationContexts;
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
        return tools.jackson.databind.cfg.PackageVersion.VERSION;
    }

    /*
    /**********************************************************************
    /* Helper methods to simplify mutant-factory implementation
    /**********************************************************************
     */

    /**
     * Overridable factory method called by various "withXxx()" methods
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
     */
    @SuppressWarnings("resource")
    protected final SequenceWriter _newSequenceWriter(SerializationContextExt ctxt,
            boolean wrapInArray, JsonGenerator gen, boolean managedInput)
        throws JacksonException
    {
        return new SequenceWriter(ctxt, gen, managedInput, _prefetch)
            .init(wrapInArray);
    }

    /*
    /**********************************************************************
    /* Life-cycle, fluent factories for SerializationFeature
    /**********************************************************************
     */

    /**
     * Method for constructing a new instance that is configured
     * with specified feature enabled.
     */
    public ObjectWriter with(SerializationFeature feature)  {
        return _new(this, _config.with(feature));
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
    /* Life-cycle, fluent factories for DatatypeFeature
    /**********************************************************************
     */

    /**
     * Method for constructing a new instance that is configured
     * with specified feature enabled.
     */
    public ObjectWriter with(DatatypeFeature feature)  {
        return _new(this,  _config.with(feature));
    }

    /**
     * Method for constructing a new instance that is configured
     * with specified features enabled.
     */
    public ObjectWriter withFeatures(DatatypeFeature... features) {
        return _new(this, _config.withFeatures(features));
    }

    /**
     * Method for constructing a new instance that is configured
     * with specified feature disabled.
     */
    public ObjectWriter without(DatatypeFeature feature) {
        return _new(this, _config.without(feature));
    }

    /**
     * Method for constructing a new instance that is configured
     * with specified features disabled.
     */
    public ObjectWriter withoutFeatures(DatatypeFeature... features) {
        return _new(this, _config.withoutFeatures(features));
    }

    /*
    /**********************************************************
    /* Life-cycle, fluent factories for StreamWriteFeature
     */

    public ObjectWriter with(StreamWriteFeature feature)  {
        return _new(this, _config.with(feature));
    }

    public ObjectWriter withFeatures(StreamWriteFeature... features) {
        return _new(this, _config.withFeatures(features));
    }

    public ObjectWriter without(StreamWriteFeature feature) {
        return _new(this, _config.without(feature));
    }

    public ObjectWriter withoutFeatures(StreamWriteFeature... features) {
        return _new(this, _config.withoutFeatures(features));
    }

    /*
    /**********************************************************************
    /* Life-cycle, fluent factories for FormatFeature
    /**********************************************************************
     */

    public ObjectWriter with(FormatFeature feature)  {
        return _new(this, _config.with(feature));
    }

    public ObjectWriter withFeatures(FormatFeature... features) {
        return _new(this, _config.withFeatures(features));
    }

    public ObjectWriter without(FormatFeature feature) {
        return _new(this, _config.without(feature));
    }

    public ObjectWriter withoutFeatures(FormatFeature... features) {
        return _new(this, _config.withoutFeatures(features));
    }

    /*
    /**********************************************************************
    /* Life-cycle, fluent factories, type-related
    /**********************************************************************
     */

    /**
     * Method that will construct a new instance that uses specific type
     * as the root type for serialization, instead of runtime dynamic
     * type of the root object itself.
     *<p>
     * Note that method does NOT change state of this reader, but
     * rather construct and returns a newly configured instance.
     */
    public ObjectWriter forType(JavaType rootType) {
        return _new(_generatorSettings, _prefetch.forRootType(this, rootType));
    }

    /**
     * Method that will construct a new instance that uses specific type
     * as the root type for serialization, instead of runtime dynamic
     * type of the root object itself.
     */
    public ObjectWriter forType(Class<?> rootType) {
        return forType(_config.constructType(rootType));
    }

    /**
     * Method that will construct a new instance that uses specific type
     * as the root type for serialization, instead of runtime dynamic
     * type of the root object itself.
     */
    public ObjectWriter forType(TypeReference<?> rootType) {
        return forType(_config.getTypeFactory().constructType(rootType.getType()));
    }

    /*
    /**********************************************************************
    /* Life-cycle, fluent factories, other
    /**********************************************************************
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
     */
    public ObjectWriter with(Base64Variant b64variant) {
        return _new(this, _config.with(b64variant));
    }

    public ObjectWriter with(CharacterEscapes escapes) {
        return _new(_generatorSettings.with(escapes), _prefetch);
    }

    /**
     * Mutant factory for overriding set of (default) attributes for
     * {@link ObjectWriter} to use.
     *<p>
     * Note that this will replace defaults passed by {@link ObjectMapper}.
     *
     * @param attrs Default {@link ContextAttributes} to use with a writer
     *
     * @return {@link ObjectWriter} instance with specified default attributes (which
     *    is usually a newly constructed writer instance with otherwise identical settings)
     */
    public ObjectWriter with(ContextAttributes attrs) {
        return _new(this, _config.with(attrs));
    }

    /**
     * Mutant factory method that allows construction of a new writer instance
     * that uses specified set of default attribute values.
     */
    public ObjectWriter withAttributes(Map<?,?> attrs) {
        return _new(this, _config.withAttributes(attrs));
    }

    public ObjectWriter withAttribute(Object key, Object value) {
        return _new(this, _config.withAttribute(key, value));
    }

    public ObjectWriter withoutAttribute(Object key) {
        return _new(this, _config.withoutAttribute(key));
    }

    public ObjectWriter withRootValueSeparator(String sep) {
        return _new(_generatorSettings.withRootValueSeparator(sep), _prefetch);
    }

    public ObjectWriter withRootValueSeparator(SerializableString sep) {
        return _new(_generatorSettings.withRootValueSeparator(sep), _prefetch);
    }

    /*
    /**********************************************************************
    /* Public API: constructing Generator that are properly linked to `ObjectWriteContext`
    /**********************************************************************
     */

    /**
     * Factory method for constructing {@link JsonGenerator} that is properly
     * wired to allow callbacks for serialization: basically
     * constructs a {@link ObjectWriteContext} and then calls
     * {@link TokenStreamFactory#createGenerator(ObjectWriteContext,OutputStream)}.
     *
     * @since 3.0
     */
    public JsonGenerator createGenerator(OutputStream target) {
        _assertNotNull("target", target);
        return _generatorFactory.createGenerator(_serializerProvider(), target);
    }

    /**
     * Factory method for constructing {@link JsonGenerator} that is properly
     * wired to allow callbacks for serialization: basically
     * constructs a {@link ObjectWriteContext} and then calls
     * {@link TokenStreamFactory#createGenerator(ObjectWriteContext,OutputStream,JsonEncoding)}.
     *
     * @since 3.0
     */
    public JsonGenerator createGenerator(OutputStream target, JsonEncoding enc) {
        _assertNotNull("target", target);
        return _generatorFactory.createGenerator(_serializerProvider(), target, enc);
    }

    /**
     * Factory method for constructing {@link JsonGenerator} that is properly
     * wired to allow callbacks for serialization: basically
     * constructs a {@link ObjectWriteContext} and then calls
     * {@link TokenStreamFactory#createGenerator(ObjectWriteContext,Writer)}.
     *
     * @since 3.0
     */
    public JsonGenerator createGenerator(Writer target) {
        _assertNotNull("target", target);
        return _generatorFactory.createGenerator(_serializerProvider(), target);
    }

    /**
     * Factory method for constructing {@link JsonGenerator} that is properly
     * wired to allow callbacks for serialization: basically
     * constructs a {@link ObjectWriteContext} and then calls
     * {@link TokenStreamFactory#createGenerator(ObjectWriteContext,File,JsonEncoding)}.
     *
     * @since 3.0
     */
    public JsonGenerator createGenerator(File target, JsonEncoding enc) {
        _assertNotNull("target", target);
        return _generatorFactory.createGenerator(_serializerProvider(), target, enc);
    }

    /**
     * Factory method for constructing {@link JsonGenerator} that is properly
     * wired to allow callbacks for serialization: basically
     * constructs a {@link ObjectWriteContext} and then calls
     * {@link TokenStreamFactory#createGenerator(ObjectWriteContext,Path,JsonEncoding)}.
     *
     * @since 3.0
     */
    public JsonGenerator createGenerator(Path target, JsonEncoding enc) {
        _assertNotNull("target", target);
        return _generatorFactory.createGenerator(_serializerProvider(), target, enc);
    }

    /**
     * Factory method for constructing {@link JsonGenerator} that is properly
     * wired to allow callbacks for serialization: basically
     * constructs a {@link ObjectWriteContext} and then calls
     * {@link TokenStreamFactory#createGenerator(ObjectWriteContext,DataOutput)}.
     *
     * @since 3.0
     */
    public JsonGenerator createGenerator(DataOutput target) {
        _assertNotNull("target", target);
        return _generatorFactory.createGenerator(_serializerProvider(), target);
    }

    /*
    /**********************************************************************
    /* Convenience methods for JsonNode creation
    /**********************************************************************
     */

    public ObjectNode createObjectNode() {
        return _config.getNodeFactory().objectNode();
    }

    public ArrayNode createArrayNode() {
        return _config.getNodeFactory().arrayNode();
    }

    /*
    /**********************************************************************
    /* Factory methods for sequence writers
    /**********************************************************************
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
     * @param target Target file to write value sequence to.
     */
    public SequenceWriter writeValues(File target)
        throws JacksonException
    {
        _assertNotNull("target", target);
        SerializationContextExt ctxt = _serializerProvider();
        return _newSequenceWriter(ctxt, false,
                _generatorFactory.createGenerator(ctxt, target, JsonEncoding.UTF8), true);
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
     * @param target Target path to write value sequence to.
     *
     * @since 3.0
     */
    public SequenceWriter writeValues(Path target)
        throws JacksonException
    {
        _assertNotNull("target", target);
        SerializationContextExt ctxt = _serializerProvider();
        return _newSequenceWriter(ctxt, false,
                _generatorFactory.createGenerator(ctxt, target, JsonEncoding.UTF8), true);
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
     */
    public SequenceWriter writeValues(JsonGenerator g)
        throws JacksonException
    {
        _assertNotNull("target", g);
        return _newSequenceWriter(_serializerProvider(), false, g, false);
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
     * @param target Target writer to use for writing the token stream
     */
    public SequenceWriter writeValues(Writer target) throws JacksonException {
        _assertNotNull("target", target);
        SerializationContextExt ctxt = _serializerProvider();
        return _newSequenceWriter(ctxt, false,
                _generatorFactory.createGenerator(ctxt, target), true);
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
     * @param target Physical output stream to use for writing the token stream
     */
    public SequenceWriter writeValues(OutputStream target) throws JacksonException {
        _assertNotNull("target", target);
        SerializationContextExt ctxt = _serializerProvider();
        return _newSequenceWriter(ctxt, false,
                _generatorFactory.createGenerator(ctxt, target, JsonEncoding.UTF8), true);
    }

    public SequenceWriter writeValues(DataOutput target) throws JacksonException {
        _assertNotNull("target", target);
        SerializationContextExt ctxt = _serializerProvider();
        return _newSequenceWriter(ctxt, false,
                _generatorFactory.createGenerator(ctxt, target), true);
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
     * @param target File to write token stream to
     */
    public SequenceWriter writeValuesAsArray(File target)
        throws JacksonException
    {
        _assertNotNull("target", target);
        SerializationContextExt ctxt = _serializerProvider();
        return _newSequenceWriter(ctxt, true,
                _generatorFactory.createGenerator(ctxt, target, JsonEncoding.UTF8), true);
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
     * @param target Path to write token stream to
     *
     * @since 3.0
     */
    public SequenceWriter writeValuesAsArray(Path target)
        throws JacksonException
    {
        _assertNotNull("target", target);
        SerializationContextExt ctxt = _serializerProvider();
        return _newSequenceWriter(ctxt, true,
                _generatorFactory.createGenerator(ctxt, target, JsonEncoding.UTF8), true);
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
     * @param g Underlying generator to use for writing the token stream
     */
    public SequenceWriter writeValuesAsArray(JsonGenerator g)
        throws JacksonException
    {
        _assertNotNull("g", g);
        return _newSequenceWriter(_serializerProvider(), true, g, false);
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
     * @param target Writer to use for writing the token stream
     */
    public SequenceWriter writeValuesAsArray(Writer target) throws JacksonException {
        _assertNotNull("target", target);
        SerializationContextExt ctxt = _serializerProvider();
        return _newSequenceWriter(ctxt, true,
                _generatorFactory.createGenerator(ctxt, target), true);
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
     * @param target Physical output stream to use for writing the token stream
     */
    public SequenceWriter writeValuesAsArray(OutputStream target) throws JacksonException {
        _assertNotNull("target", target);
        SerializationContextExt ctxt = _serializerProvider();
        return _newSequenceWriter(ctxt, true,
                _generatorFactory.createGenerator(ctxt, target, JsonEncoding.UTF8), true);
    }

    public SequenceWriter writeValuesAsArray(DataOutput target) throws JacksonException {
        _assertNotNull("target", target);
        SerializationContextExt ctxt = _serializerProvider();
        return _newSequenceWriter(ctxt, true,
                _generatorFactory.createGenerator(ctxt, target), true);
    }

    /*
    /**********************************************************************
    /* Simple accessors
    /**********************************************************************
     */

    public boolean isEnabled(SerializationFeature f) {
        return _config.isEnabled(f);
    }

    public boolean isEnabled(MapperFeature f) {
        return _config.isEnabled(f);
    }

    public boolean isEnabled(DatatypeFeature f) {
        return _config.isEnabled(f);
    }

    public boolean isEnabled(StreamWriteFeature f) {
        // !!! 09-Oct-2017, tatu: Actually for full answer we really should check
        //   what actual combined settings are....
        return _generatorFactory.isEnabled(f);
    }

    public SerializationConfig getConfig() {
        return _config;
    }

    /**
     * @since 3.0
     */
    public TokenStreamFactory generatorFactory() {
        return _generatorFactory;
    }

    public TypeFactory typeFactory() {
        return _config.getTypeFactory();
    }

    /**
     * Diagnostics method that can be called to check whether this writer
     * has pre-fetched serializer to use: pre-fetching improves performance
     * when writer instances are reused as it avoids a per-call serializer
     * lookup.
     */
    public boolean hasPrefetchedSerializer() {
        return _prefetch.hasSerializer();
    }

    public ContextAttributes getAttributes() {
        return _config.getAttributes();
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
    /* Serialization methods; ones from ObjectCodec first
    /**********************************************************************
     */

    /**
     * Method that can be used to serialize any Java value as
     * JSON output, using provided {@link JsonGenerator}.
     */
    public void writeValue(JsonGenerator g, Object value)
        throws JacksonException
    {
        _assertNotNull("g", g);
        if (_config.isEnabled(SerializationFeature.CLOSE_CLOSEABLE)
                && (value instanceof Closeable))
        {
            Closeable toClose = (Closeable) value;
            try {
                _prefetch.serialize(g, value, _serializerProvider());
                if (_config.isEnabled(SerializationFeature.FLUSH_AFTER_WRITE_VALUE)) {
                    g.flush();
                }
            } catch (Exception e) {
                ClassUtil.closeOnFailAndThrowAsJacksonE(null, toClose, e);
                return;
            }
            try {
                toClose.close();
            } catch (IOException e) {
                throw JacksonIOException.construct(e, g);
            }
        } else {
            _prefetch.serialize(g, value, _serializerProvider());
            if (_config.isEnabled(SerializationFeature.FLUSH_AFTER_WRITE_VALUE)) {
                g.flush();
            }
        }
    }

    /*
    /**********************************************************************
    /* Serialization methods, others
    /**********************************************************************
     */

    /**
     * Method that can be used to serialize any Java value as
     * JSON output, written to File provided.
     */
    public void writeValue(File target, Object value)
        throws JacksonException
    {
        _assertNotNull("target", target);
        SerializationContextExt ctxt = _serializerProvider();
        _configAndWriteValue(ctxt,
                _generatorFactory.createGenerator(ctxt, target, JsonEncoding.UTF8), value);
    }

    /**
     * Method that can be used to serialize any Java value as
     * JSON output, written to Path provided.
     *
     * @since 3.0
     */
    public void writeValue(Path target, Object value)
        throws JacksonException
    {
        _assertNotNull("target", target);
        SerializationContextExt ctxt = _serializerProvider();
        _configAndWriteValue(ctxt,
                _generatorFactory.createGenerator(ctxt, target, JsonEncoding.UTF8), value);
    }

    /**
     * Method that can be used to serialize any Java value as
     * JSON output, using output stream provided (using encoding
     * {@link JsonEncoding#UTF8}).
     *<p>
     * Note: method does not close the underlying stream explicitly
     * here; however, {@link TokenStreamFactory} this mapper uses may choose
     * to close the stream depending on its settings (by default,
     * it will try to close it when {@link JsonGenerator} we construct
     * is closed).
     */
    public void writeValue(OutputStream target, Object value) throws JacksonException
    {
        _assertNotNull("target", target);
        SerializationContextExt ctxt = _serializerProvider();
        _configAndWriteValue(ctxt,
                _generatorFactory.createGenerator(ctxt, target, JsonEncoding.UTF8), value);
    }

    /**
     * Method that can be used to serialize any Java value as
     * JSON output, using Writer provided.
     *<p>
     * Note: method does not close the underlying stream explicitly
     * here; however, {@link TokenStreamFactory} this mapper uses may choose
     * to close the stream depending on its settings (by default,
     * it will try to close it when {@link JsonGenerator} we construct
     * is closed).
     */
    public void writeValue(Writer target, Object value) throws JacksonException
    {
        _assertNotNull("target", target);
        SerializationContextExt ctxt = _serializerProvider();
        _configAndWriteValue(ctxt,
                _generatorFactory.createGenerator(ctxt, target), value);
    }

    public void writeValue(DataOutput target, Object value) throws JacksonException
    {
        _assertNotNull("target", target);
        SerializationContextExt ctxt = _serializerProvider();
        _configAndWriteValue(ctxt,
                _generatorFactory.createGenerator(ctxt, target), value);
    }

    /**
     * Method that can be used to serialize any Java value as
     * a String. Functionally equivalent to calling
     * {@link #writeValue(Writer,Object)} with {@link java.io.StringWriter}
     * and constructing String, but more efficient.
     */
    public String writeValueAsString(Object value) throws JacksonException
    {
        // alas, we have to pull the recycler directly here...
        final BufferRecycler br = _generatorFactory._getBufferRecycler();
        try (SegmentedStringWriter sw = new SegmentedStringWriter(br)) {
            final SerializationContextExt ctxt = _serializerProvider();
            _configAndWriteValue(ctxt,
                    _generatorFactory.createGenerator(ctxt, sw), value);
            return sw.getAndClear();
        } finally {
            br.releaseToPool();
        }
    }

    /**
     * Method that can be used to serialize any Java value as
     * a byte array. Functionally equivalent to calling
     * {@link #writeValue(Writer,Object)} with {@link java.io.ByteArrayOutputStream}
     * and getting bytes, but more efficient.
     * Encoding used will be UTF-8.
     */
    public byte[] writeValueAsBytes(Object value) throws JacksonException
    {
        final BufferRecycler br = _generatorFactory._getBufferRecycler();
        try (ByteArrayBuilder bb = new ByteArrayBuilder(br)) {
            final SerializationContextExt ctxt = _serializerProvider();
            _configAndWriteValue(ctxt,
                    _generatorFactory.createGenerator(ctxt, bb, JsonEncoding.UTF8), value);
            return bb.getClearAndRelease();
        } finally {
            br.releaseToPool(); // since 2.17
        }
    }

    /**
     * Method called to configure the generator as necessary and then
     * call write functionality
     */
    protected final void _configAndWriteValue(SerializationContextExt ctxt,
            JsonGenerator gen, Object value) throws JacksonException
    {
        if (_config.isEnabled(SerializationFeature.CLOSE_CLOSEABLE) && (value instanceof Closeable)) {
            _writeCloseable(gen, value);
            return;
        }
        try {
            _prefetch.serialize(gen, value, ctxt);
        } catch (Exception e) {
            ClassUtil.closeOnFailAndThrowAsJacksonE(gen, e);
            return;
        }
        gen.close();
    }

    /**
     * Helper method used when value to serialize is {@link Closeable} and its <code>close()</code>
     * method is to be called right after serialization has been called
     */
    private final void _writeCloseable(JsonGenerator gen, Object value)
        throws JacksonException
    {
        Closeable toClose = (Closeable) value;
        try {
            _prefetch.serialize(gen, value, _serializerProvider());
            Closeable tmpToClose = toClose;
            toClose = null;
            tmpToClose.close();
        } catch (Exception e) {
            ClassUtil.closeOnFailAndThrowAsJacksonE(gen, toClose, e);
            return;
        }
        gen.close();
    }

    /*
    /**********************************************************************
    /* Serialization-like methods
    /**********************************************************************
     */

    /**
     * Method that
     * will convert given Java value (usually bean) into its
     * equivalent Tree model {@link JsonNode} representation.
     * Functionally similar to serializing value into token stream and parsing that
     * stream back as tree model node,
     * but more efficient as {@link TokenBuffer} is used to contain the intermediate
     * representation instead of fully serialized contents.
     *<p>
     * NOTE: while results are usually identical to that of serialization followed
     * by deserialization, this is not always the case. In some cases serialization
     * into intermediate representation will retain encapsulation of things like
     * raw value ({@link tools.jackson.databind.util.RawValue}) or basic
     * node identity ({@link JsonNode}). If so, result is a valid tree, but values
     * are not re-constructed through actual format representation. So if transformation
     * requires actual materialization of encoded content,
     * it will be necessary to do actual serialization.
     *
     * @param <T> Actual node type; usually either basic {@link JsonNode} or
     *  {@link tools.jackson.databind.node.ObjectNode}
     * @param fromValue Java value to convert
     *
     * @return (non-null) Root node of the resulting content tree: in case of
     *   {@code null} value node for which {@link JsonNode#isNull()} returns {@code true}.
     *
     * @since 3.0
     */
    public <T extends JsonNode> T valueToTree(Object fromValue)
        throws JacksonException
    {
        return _serializerProvider().valueToTree(fromValue);
    }

    /*
    /**********************************************************************
    /* Other public methods
    /**********************************************************************
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
     */
    public void acceptJsonFormatVisitor(JavaType type, JsonFormatVisitorWrapper visitor)
    {
        _assertNotNull("type", type);
        _assertNotNull("visitor", visitor);
        _serializerProvider().acceptJsonFormatVisitor(type, visitor);
    }

    public void acceptJsonFormatVisitor(Class<?> type, JsonFormatVisitorWrapper visitor)
    {
        _assertNotNull("type", type);
        _assertNotNull("visitor", visitor);
        acceptJsonFormatVisitor(_config.constructType(type), visitor);
    }

    /*
    /**********************************************************************
    /* Overridable helper methods
    /**********************************************************************
     */

    /**
     * Overridable helper method used for constructing
     * {@link SerializerProvider} to use for serialization.
     */
    protected final SerializationContextExt _serializerProvider() {
        return _serializationContexts.createContext(_config, _generatorSettings);
    }

    /*
    /**********************************************************************
    /* Internal methods
    /**********************************************************************
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

    protected final void _assertNotNull(String paramName, Object src) {
        if (src == null){
            throw new IllegalArgumentException(String.format("argument \"%s\" is null", paramName));
        }
    }

    /*
    /**********************************************************************
    /* Helper classes for configuration
    /**********************************************************************
     */

    /**
     * As a minor optimization, we will make an effort to pre-fetch a serializer,
     * or at least relevant <code>TypeSerializer</code>, if given enough
     * information.
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
        private final ValueSerializer<Object> valueSerializer;

        /**
         * When dealing with polymorphic types, we cannot pre-fetch
         * serializer, but can pre-fetch {@link TypeSerializer}.
         */
        private final TypeSerializer typeSerializer;

        private Prefetch(JavaType rootT,
                ValueSerializer<Object> ser, TypeSerializer typeSer)
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
                SerializationContextExt ctxt = parent._serializerProvider();
                TypeSerializer typeSer = ctxt.findTypeSerializer(newType);
                return new Prefetch(null, null, typeSer);
            }

            if (parent.isEnabled(SerializationFeature.EAGER_SERIALIZER_FETCH)) {
                SerializationContextExt ctxt = parent._serializerProvider();
                // 17-Dec-2014, tatu: Need to be bit careful here; TypeSerializers are NOT cached,
                //   so although it'd seem like a good idea to look for those first, and avoid
                //   serializer for polymorphic types, it is actually more efficient to do the
                //   reverse here.
                try {
                    ValueSerializer<Object> ser = ctxt.findTypedValueSerializer(newType, true);
                    // Important: for polymorphic types, "unwrap"...
                    if (ser instanceof TypeWrappedSerializer) {
                        return new Prefetch(newType, null,
                                ((TypeWrappedSerializer) ser).typeSerializer());
                    }
                    return new Prefetch(newType, ser, null);
                } catch (JacksonException e) {
                    // need to swallow?
                }
            }
            return new Prefetch(newType, null, typeSerializer);
        }

        public final ValueSerializer<Object> getValueSerializer() {
            return valueSerializer;
        }

        public final TypeSerializer getTypeSerializer() {
            return typeSerializer;
        }

        public boolean hasSerializer() {
            return (valueSerializer != null) || (typeSerializer != null);
        }

        public void serialize(JsonGenerator gen, Object value, SerializationContextExt ctxt)
            throws JacksonException
        {
            if (typeSerializer != null) {
                ctxt.serializePolymorphic(gen, value, rootType, valueSerializer, typeSerializer);
            } else  if (valueSerializer != null) {
                ctxt.serializeValue(gen, value, rootType, valueSerializer);
            } else if (rootType != null) {
                ctxt.serializeValue(gen, value, rootType);
            } else {
                ctxt.serializeValue(gen, value);
            }
        }
    }
}
