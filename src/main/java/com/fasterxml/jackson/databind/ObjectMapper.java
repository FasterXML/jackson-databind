package com.fasterxml.jackson.databind;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URL;
import java.text.DateFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.io.SegmentedStringWriter;
import com.fasterxml.jackson.core.json.JsonFactory;
import com.fasterxml.jackson.core.type.ResolvedType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.*;

import com.fasterxml.jackson.databind.cfg.*;
import com.fasterxml.jackson.databind.deser.*;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.introspect.*;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.*;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.databind.ser.*;
import com.fasterxml.jackson.databind.type.*;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.RootNameLookup;
import com.fasterxml.jackson.databind.util.TokenBuffer;

/**
 * ObjectMapper provides functionality for reading and writing JSON,
 * either to and from basic POJOs (Plain Old Java Objects), or to and from
 * a general-purpose JSON Tree Model ({@link JsonNode}), as well as
 * related functionality for performing conversions.
 * In addition to directly reading and writing JSON (and with different underlying
 * {@link TokenStreamFactory} configuration, other formats), it is also the
 * mechanism for creating {@link ObjectReader}s and {@link ObjectWriter}s which
 * offer more advancing reading/writing functionality.
 *<p>
 * Construction of mapper instances proceeds either via no-arguments constructor
 * (producting instance with default configuration); or through one of two build
 * methods.
 * First build method is the static {@link #builder}
 * and second {@link #rebuild()} method method on an existing mapper.
 * Former starts with default configuration (same as one that no-arguments constructor
 * created mapper has), and latter starts with configuration of the mapper it is called
 * on.
 * In both cases, after configuration (including addition of {@link Module}s) is complete,
 * instance is created by calling {@link MapperBuilder#build()} method.
 *<p>
 * Mapper (and {@link ObjectReader}s, {@link ObjectWriter}s it constructs) will
 * use instances of {@link JsonParser} and {@link JsonGenerator}
 * for implementing actual reading/writing of JSON.
 * Note that although most read and write methods are exposed through this class,
 * some of the functionality is only exposed via {@link ObjectReader} and
 * {@link ObjectWriter}: specifically, reading/writing of longer sequences of
 * values is only available through {@link ObjectReader#readValues(InputStream)}
 * and {@link ObjectWriter#writeValues(OutputStream)}.
 *<p>
Simplest usage is of form:
<pre>
  final ObjectMapper mapper = new ObjectMapper(); // can use static singleton, inject: just make sure to reuse!
  MyValue value = new MyValue();
  // ... and configure
  File newState = new File("my-stuff.json");
  mapper.writeValue(newState, value); // writes JSON serialization of MyValue instance
  // or, read
  MyValue older = mapper.readValue(new File("my-older-stuff.json"), MyValue.class);

  // Or if you prefer JSON Tree representation:
  JsonNode root = mapper.readTree(newState);
  // and find values by, for example, using a {@link com.fasterxml.jackson.core.JsonPointer} expression:
  int age = root.at("/personal/age").getValueAsInt(); 
</pre>
 *<p> 
 * Mapper instances are fully thread-safe as of Jackson 3.0.
 *<p>
 * Note on caching: root-level deserializers are always cached, and accessed
 * using full (generics-aware) type information. This is different from
 * caching of referenced types, which is more limited and is done only
 * for a subset of all deserializer types. The main reason for difference
 * is that at root-level there is no incoming reference (and hence no
 * referencing property, no referral information or annotations to
 * produce differing deserializers), and that the performance impact
 * greatest at root level (since it'll essentially cache the full
 * graph of deserializers involved).
 */
public class ObjectMapper
    implements Versioned,
        java.io.Serializable
{
    private static final long serialVersionUID = 3L;

    /*
    /**********************************************************
    /* Helper classes, enums
    /**********************************************************
     */

    /**
     * Base implementation for "Vanilla" {@link ObjectMapper}, used with JSON backend
     * as well as for some of simpler formats that do not require mapper level overrides.
     *
     * @since 3.0
     */
    public static class Builder extends MapperBuilder<ObjectMapper, Builder>
    {
        public Builder(TokenStreamFactory tsf) {
            super(tsf);
        }

        @Override
        public ObjectMapper build() {
            return new ObjectMapper(this);
        }

        @Override
        protected MapperBuilderState _saveState() {
            return new StateImpl(this);
        }

        public Builder(MapperBuilderState state) {
            super(state);
        }

        /**
         * We also need actual instance of state as base class can not implement logic
         * for reinstating mapper (via mapper builder) from state.
         */
        static class StateImpl extends MapperBuilderState {
            private static final long serialVersionUID = 3L;

            public StateImpl(Builder b) {
                super(b);
            }

            @Override
            protected Object readResolve() {
                return new Builder(this).build();
            }
        }
    }

    /*
    /**********************************************************
    /* Internal constants, singletons
    /**********************************************************
     */
    
    // Quick little shortcut, to avoid having to use global TypeFactory instance...
    // 19-Oct-2015, tatu: Not sure if this is really safe to do; let's at least allow
    //   some amount of introspection
    private final static JavaType JSON_NODE_TYPE =
            SimpleType.constructUnsafe(JsonNode.class);
//            TypeFactory.defaultInstance().constructType(JsonNode.class);

    /*
    /**********************************************************
    /* Configuration settings, shared
    /**********************************************************
     */

    /**
     * Factory used to create {@link JsonParser} and {@link JsonGenerator}
     * instances as necessary.
     */
    protected final TokenStreamFactory _streamFactory;

    /**
     * Specific factory used for creating {@link JavaType} instances;
     * needed to allow modules to add more custom type handling
     * (mostly to support types of non-Java JVM languages)
     */
    protected final TypeFactory _typeFactory;

    /**
     * Provider for values to inject in deserialized POJOs.
     */
    protected final InjectableValues _injectableValues;

    /**
     * Thing used for registering sub-types, resolving them to
     * super/sub-types as needed.
     */
    protected final SubtypeResolver _subtypeResolver;

    /**
     * Currently active per-type configuration overrides, accessed by
     * declared type of property.
     */
    protected final ConfigOverrides _configOverrides;

    /*
    /**********************************************************
    /* Configuration settings: mix-in annotations
    /**********************************************************
     */

    /**
     * Mapping that defines how to apply mix-in annotations: key is
     * the type to received additional annotations, and value is the
     * type that has annotations to "mix in".
     *<p>
     * Annotations associated with the value classes will be used to
     * override annotations of the key class, associated with the
     * same field or method. They can be further masked by sub-classes:
     * you can think of it as injecting annotations between the target
     * class and its sub-classes (or interfaces)
     */
    protected final MixInHandler _mixIns;

    /*
    /**********************************************************
    /* Configuration settings, serialization
    /**********************************************************
     */

    // !!! TODO 15-Mar-2018: Only mutable for Default Typing
    /**
     * Configuration object that defines basic global
     * settings for the serialization process
     */
    protected SerializationConfig _serializationConfig;

    /**
     * Object that manages access to serializers used for serialization,
     * including caching.
     * It is configured with {@link #_serializerFactory} to allow
     * for constructing custom serializers.
     *<p>
     * Note: while serializers are only exposed {@link SerializerProvider},
     * mappers and readers need to access additional API defined by
     * {@link DefaultSerializerProvider}
     */
    protected final DefaultSerializerProvider _serializerProvider;

    /**
     * Serializer factory used for constructing serializers.
     */
    protected final SerializerFactory _serializerFactory;

    /*
    /**********************************************************
    /* Configuration settings, deserialization
    /**********************************************************
     */

    // !!! TODO 15-Mar-2018: Only mutable for Default Typing
    /**
     * Configuration object that defines basic global
     * settings for the serialization process
     */
    protected DeserializationConfig _deserializationConfig;

    /**
     * Blueprint context object; stored here to allow custom
     * sub-classes. Contains references to objects needed for
     * deserialization construction (cache, factory).
     */
    protected final DefaultDeserializationContext _deserializationContext;

    /*
    /**********************************************************
    /* Caching
    /**********************************************************
     */

    /* Note: handling of serializers and deserializers is not symmetric;
     * and as a result, only root-level deserializers can be cached here.
     * This is mostly because typing and resolution for deserializers is
     * fully static; whereas it is quite dynamic for serialization.
     */

    /**
     * We will use a separate main-level Map for keeping track
     * of root-level deserializers. This is where most successful
     * cache lookups get resolved.
     * Map will contain resolvers for all kinds of types, including
     * container types: this is different from the component cache
     * which will only cache bean deserializers.
     *<p>
     * Given that we don't expect much concurrency for additions
     * (should very quickly converge to zero after startup), let's
     * explicitly define a low concurrency setting.
     *<p>
     * These may are either "raw" deserializers (when
     * no type information is needed for base type), or type-wrapped
     * deserializers (if it is needed)
     */
    protected final ConcurrentHashMap<JavaType, JsonDeserializer<Object>> _rootDeserializers
        = new ConcurrentHashMap<JavaType, JsonDeserializer<Object>>(64, 0.6f, 2);

    /*
    /**********************************************************
    /* Saved state to allow re-building
    /**********************************************************
     */

    /**
     * Minimal state retained to allow both re-building (by
     * creating new builder) and JDK serialization of this mapper.
     *
     * @since 3.0
     */
    protected final MapperBuilderState _savedBuilderState;

    /*
    /**********************************************************************
    /* Life-cycle: legacy constructors
    /**********************************************************************
     */

    /**
     * Default constructor, which will construct the default JSON-handling
     * {@link TokenStreamFactory} as necessary and all other unmodified
     * default settings, and no additional registered modules.
     */
    public ObjectMapper() {
        this(new Builder(new JsonFactory()));
    }

    /**
     * Constructs instance that uses specified {@link TokenStreamFactory}
     * for constructing necessary {@link JsonParser}s and/or
     * {@link JsonGenerator}s, but without registering additional modules.
     */
    public ObjectMapper(TokenStreamFactory streamFactory) {
        this(new Builder(streamFactory));
    }

    /*
    /**********************************************************************
    /* Life-cycle: builder-style construction
    /**********************************************************************
     */

    /**
     * Constructor usually called either by {@link MapperBuilder#build} or
     * by sub-class constructor: will get all the settings through passed-in
     * builder, including registration of any modules added to builder.
     */
    protected ObjectMapper(MapperBuilder<?,?> builder)
    {
        // First things first: finalize building process:
        _savedBuilderState = builder.saveStateApplyModules();

        // General framework factories
        _streamFactory = builder.streamFactory();
        // bit tricky as we do NOT want to expose simple accessors (to a mutable thing)
        {
            final AtomicReference<ConfigOverrides> ref = new AtomicReference<>();
            builder.withAllConfigOverrides(overrides -> ref.set(overrides));
            _configOverrides = ref.get();
        }
        // general type handling
        _typeFactory = builder.typeFactory();

        _subtypeResolver = builder.subtypeResolver();
        
        // Ser/deser framework factories
        _serializerProvider = builder.serializerProvider();
        _serializerFactory = builder.serializerFactory();

        _deserializationContext = builder.deserializationContext();
        _injectableValues = builder.injectableValues();

        RootNameLookup rootNames = new RootNameLookup();

        _mixIns = builder.mixInHandler();
        _serializationConfig = builder.buildSerializationConfig(_mixIns, rootNames);
        _deserializationConfig = builder.buildDeserializationConfig(_mixIns, rootNames);
    }

    // 16-Feb-2018, tatu: Arggghh. Due to Java Type Erasure rules, override, even static methods
    //    are apparently bound to compatibility rules (despite them not being real overrides at all).
    //    And because there is no "JsonMapper" we need to use odd weird typing here. Instead of simply
    //    using `MapperBuilder` we already go
    
    /**
     * Short-cut for:
     *<pre>
     *   return builder(new JsonFactory());
     *</pre>
     *
     * @since 3.0
     */
    @SuppressWarnings("unchecked")
    public static <M extends ObjectMapper, B extends MapperBuilder<M,B>> MapperBuilder<M,B> builder() {
        return (MapperBuilder<M,B>) jsonBuilder();
    }

    // But here we can just use simple typing. Since there are no overloads of any kind.
    public static ObjectMapper.Builder jsonBuilder() {
        return new ObjectMapper.Builder(new JsonFactory());
    }
    
    public static ObjectMapper.Builder builder(TokenStreamFactory streamFactory) {
        return new ObjectMapper.Builder(streamFactory);
    }

    /**
     * Method for creating a new {@link MapperBuilder} for constructing differently configured
     * {@link ObjectMapper} instance, starting with current configuration including base settings
     * and registered modules.
     *
     * @since 3.0
     */
    @SuppressWarnings("unchecked")
    public <M extends ObjectMapper, B extends MapperBuilder<M,B>> MapperBuilder<M,B> rebuild() {
        // 27-Feb-2018, tatu: since we still have problem with `ObjectMapper` being both API
        //    and implementation for JSON, need more checking here
        ClassUtil.verifyMustOverride(ObjectMapper.class, this, "rebuild");
        return (MapperBuilder<M,B>) new ObjectMapper.Builder(_savedBuilderState);
    }

    /*
    /**********************************************************************
    /* Life-cycle: JDK serialization support
    /**********************************************************************
     */

    // Logic here is simple: instead of serializing mapper via its contents,
    // we have pre-packaged `MapperBuilderState` in a way that makes serialization
    // easier, and we go with that.
    // But note that return direction has to be supported, then, by that state object
    // and NOT anything in here.
    protected Object writeReplace() {
        return _savedBuilderState;
    }

    // Just as a sanity check verify there is no attempt at directly instantiating mapper here
    protected Object readResolve() {
        throw new IllegalStateException("Should never deserialize `"+getClass().getName()+"` directly");
    }

    /*
    /**********************************************************************
    /* Versioned impl
    /**********************************************************************
     */

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
    /* Configuration: main config object access
    /**********************************************************************
     */

    /**
     * Accessor for internal configuration object that contains settings for
     * serialization operations (<code>writeValue(...)</code> methods)
     *<br>
     * NOTE: Not to be used by application code; needed by some tests
     */
    public SerializationConfig serializationConfig() {
        return _serializationConfig;
    }

    /**
     * Accessor for internal configuration object that contains settings for
     * deserialization operations (<code>readValue(...)</code> methods)
     *<br>
     * NOTE: Not to be used by application code; needed by some tests
     */
    public DeserializationConfig deserializationConfig() {
        return _deserializationConfig;
    }

    /**
     * Method that can be used to get hold of {@link TokenStreamFactory} that this
     * mapper uses if it needs to construct {@link JsonParser}s
     * and/or {@link JsonGenerator}s.
     *<p>
     * WARNING: note that all {@link ObjectReader} and {@link ObjectWriter}
     * instances created by this mapper usually share the same configured
     * {@link TokenStreamFactory}, so changes to its configuration will "leak".
     * To avoid such observed changes you should always use "with()" and
     * "without()" method of {@link ObjectReader} and {@link ObjectWriter}
     * for changing {@link com.fasterxml.jackson.core.JsonParser.Feature}
     * and {@link com.fasterxml.jackson.core.JsonGenerator.Feature}
     * settings to use on per-call basis.
     *
     * @return {@link TokenStreamFactory} that this mapper uses when it needs to
     *   construct Json parser and generators
     *
     * @since 3.0
     */
    public TokenStreamFactory tokenStreamFactory() { return _streamFactory; }

    /**
     * Method that can be used to get hold of {@link JsonNodeFactory}
     * that this mapper will use when directly constructing
     * root {@link JsonNode} instances for Trees.
     *<p>
     * Note: this is just a shortcut for calling
     *<pre>
     *   getDeserializationConfig().getNodeFactory()
     *</pre>
     */
    public JsonNodeFactory getNodeFactory() {
        return _deserializationConfig.getNodeFactory();
    }

    public InjectableValues getInjectableValues() {
        return _injectableValues;
    }

    /*
    /**********************************************************************
    /* Configuration, access to type factory, type resolution
    /**********************************************************************
     */

    /**
     * Accessor for getting currently configured {@link TypeFactory} instance.
     */
    public TypeFactory getTypeFactory() {
        return _typeFactory;
    }

    /**
     * Convenience method for constructing {@link JavaType} out of given
     * type (typically <code>java.lang.Class</code>), but without explicit
     * context.
     */
    public JavaType constructType(Type t) {
        return _typeFactory.constructType(t);
    }


    /*
    /**********************************************************************
    /* Configuration, accessing features
    /**********************************************************************
     */

    public boolean isEnabled(JsonFactory.Feature f) {
        return _streamFactory.isEnabled(f);
    }

    public boolean isEnabled(JsonParser.Feature f) {
        return _deserializationConfig.isEnabled(f);
    }

    public boolean isEnabled(JsonGenerator.Feature f) {
        return _serializationConfig.isEnabled(f);
    }

    /**
     * Method for checking whether given {@link MapperFeature} is enabled.
     */
    public boolean isEnabled(MapperFeature f) {
        // ok to use either one, should be kept in sync
        return _serializationConfig.isEnabled(f);
    }

    /**
     * Method for checking whether given deserialization-specific
     * feature is enabled.
     */
    public boolean isEnabled(DeserializationFeature f) {
        return _deserializationConfig.isEnabled(f);
    }

    /**
     * Method for checking whether given serialization-specific
     * feature is enabled.
     */
    public boolean isEnabled(SerializationFeature f) {
        return _serializationConfig.isEnabled(f);
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
    public JsonParser createParser(File src) throws IOException {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return ctxt.assignAndReturnParser(_streamFactory.createParser(ctxt, src));
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
        return ctxt.assignAndReturnParser(_streamFactory.createParser(ctxt, src));
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
        return ctxt.assignAndReturnParser(_streamFactory.createParser(ctxt, in));
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
        return ctxt.assignAndReturnParser(_streamFactory.createParser(ctxt, r));
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
        return ctxt.assignAndReturnParser(_streamFactory.createParser(ctxt, data));
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
        return ctxt.assignAndReturnParser(_streamFactory.createParser(ctxt, data, offset, len));
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
        return ctxt.assignAndReturnParser(_streamFactory.createParser(ctxt, content));
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
        return ctxt.assignAndReturnParser(_streamFactory.createParser(ctxt, content));
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
        return ctxt.assignAndReturnParser(_streamFactory.createParser(ctxt, content, offset, len));
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
        return ctxt.assignAndReturnParser(_streamFactory.createParser(ctxt, content));
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
        return ctxt.assignAndReturnParser(_streamFactory.createNonBlockingByteArrayParser(ctxt));
    }

    /*
    /**********************************************************************
    /* Public API: constructing Generator that are properly linked
    /* to `ObjectWriteContext`
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
    public JsonGenerator createGenerator(OutputStream out) throws IOException {
        return _streamFactory.createGenerator(_serializerProvider(), out);
    }

    /**
     * Factory method for constructing {@link JsonGenerator} that is properly
     * wired to allow callbacks for serialization: basically
     * constructs a {@link ObjectWriteContext} and then calls
     * {@link TokenStreamFactory#createGenerator(ObjectWriteContext,OutputStream,JsonEncoding)}.
     *
     * @since 3.0
     */
    public JsonGenerator createGenerator(OutputStream out, JsonEncoding enc) throws IOException {
        return _streamFactory.createGenerator(_serializerProvider(), out, enc);
    }

    /**
     * Factory method for constructing {@link JsonGenerator} that is properly
     * wired to allow callbacks for serialization: basically
     * constructs a {@link ObjectWriteContext} and then calls
     * {@link TokenStreamFactory#createGenerator(ObjectWriteContext,Writer)}.
     *
     * @since 3.0
     */
    public JsonGenerator createGenerator(Writer w) throws IOException {
        return _streamFactory.createGenerator(_serializerProvider(), w);
    }

    /**
     * Factory method for constructing {@link JsonGenerator} that is properly
     * wired to allow callbacks for serialization: basically
     * constructs a {@link ObjectWriteContext} and then calls
     * {@link TokenStreamFactory#createGenerator(ObjectWriteContext,File,JsonEncoding)}.
     *
     * @since 3.0
     */
    public JsonGenerator createGenerator(File f, JsonEncoding enc)
        throws IOException {
        return _streamFactory.createGenerator(_serializerProvider(), f, enc);
    }

    /**
     * Factory method for constructing {@link JsonGenerator} that is properly
     * wired to allow callbacks for serialization: basically
     * constructs a {@link ObjectWriteContext} and then calls
     * {@link TokenStreamFactory#createGenerator(ObjectWriteContext,DataOutput)}.
     *
     * @since 3.0
     */
    public JsonGenerator createGenerator(DataOutput out) throws IOException {
        return _streamFactory.createGenerator(_serializerProvider(), out);
    }

    /*
    /**********************************************************************
    /* Public API deserialization, main methods
    /**********************************************************************
     */

    /**
     * Method to deserialize JSON content into a non-container
     * type (it can be an array type, however): typically a bean, array
     * or a wrapper type (like {@link java.lang.Boolean}).
     *<p>
     * Note: this method should NOT be used if the result type is a
     * container ({@link java.util.Collection} or {@link java.util.Map}.
     * The reason is that due to type erasure, key and value types
     * cannot be introspected when using this method.
     * 
     * @throws IOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws JsonParseException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws JsonMappingException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(JsonParser p, Class<T> valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        DeserializationContext ctxt = createDeserializationContext(p);
        return (T) _readValue(ctxt, p, _typeFactory.constructType(valueType));
    } 

    /**
     * Method to deserialize JSON content into a Java type, reference
     * to which is passed as argument. Type is passed using so-called
     * "super type token" (see )
     * and specifically needs to be used if the root type is a 
     * parameterized (generic) container type.
     * 
     * @throws IOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws JsonParseException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws JsonMappingException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(JsonParser p, TypeReference<T> valueTypeRef)
        throws IOException, JsonParseException, JsonMappingException
    {
        DeserializationContext ctxt = createDeserializationContext(p);
        return (T) _readValue(ctxt, p, _typeFactory.constructType(valueTypeRef));
    }

    /**
     * Method to deserialize JSON content into a Java type, reference
     * to which is passed as argument. Type is passed using 
     * Jackson specific type; instance of which can be constructed using
     * {@link TypeFactory}.
     * 
     * @throws IOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws JsonParseException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws JsonMappingException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     */
    @SuppressWarnings("unchecked")
    public final <T> T readValue(JsonParser p, ResolvedType valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        DeserializationContext ctxt = createDeserializationContext(p);
        return (T) _readValue(ctxt, p, (JavaType) valueType);
    }

    /**
     * Type-safe overloaded method, basically alias for {@link #readValue(JsonParser, Class)}.
     * 
     * @throws IOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws JsonParseException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws JsonMappingException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(JsonParser p, JavaType valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        DeserializationContext ctxt = createDeserializationContext(p);
        return (T) _readValue(ctxt, p, valueType);
    }

    /**
     * Method to deserialize JSON content as tree expressed
     * using set of {@link JsonNode} instances. Returns
     * root of the resulting tree (where root can consist
     * of just a single node if the current event is a
     * value event, not container).
     * 
     * @return a {@link JsonNode}, if valid JSON content found; null
     *   if input has no content to bind -- note, however, that if
     *   JSON <code>null</code> token is found, it will be represented
     *   as a non-null {@link JsonNode} (one that returns <code>true</code>
     *   for {@link JsonNode#isNull()}
     * 
     * @throws IOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws JsonParseException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     */
    public <T extends TreeNode> T readTree(JsonParser p)
        throws IOException, JsonProcessingException
    {
        /* 02-Mar-2009, tatu: One twist; deserialization provider
         *   will map JSON null straight into Java null. But what
         *   we want to return is the "null node" instead.
         */
        /* 05-Aug-2011, tatu: Also, must check for EOF here before
         *   calling readValue(), since that'll choke on it otherwise
         */
        JsonToken t = p.currentToken();
        if (t == null) {
            t = p.nextToken();
            if (t == null) {
                return null;
            }
        }
        DeserializationContext ctxt = createDeserializationContext(p);
        JsonNode n = (JsonNode) _readValue(ctxt, p, JSON_NODE_TYPE);
        if (n == null) {
            n = getNodeFactory().nullNode();
        }
        @SuppressWarnings("unchecked")
        T result = (T) n;
        return result;
    }

    /**
     * Convenience method, equivalent in function to:
     *<pre>
     *   readerFor(valueType).readValues(p);
     *</pre>
     *<p>
     * Method for reading sequence of Objects from parser stream.
     * Sequence can be either root-level "unwrapped" sequence (without surrounding
     * JSON array), or a sequence contained in a JSON Array.
     * In either case {@link JsonParser} <b>MUST</b> point to the first token of
     * the first element, OR not point to any token (in which case it is advanced
     * to the next token). This means, specifically, that for wrapped sequences,
     * parser MUST NOT point to the surrounding <code>START_ARRAY</code> (one that
     * contains values to read) but rather to the token following it which is the first
     * token of the first value to read.
     *<p>
     * Note that {@link ObjectReader} has more complete set of variants.
     */
    public <T> MappingIterator<T> readValues(JsonParser p, JavaType valueType)
        throws IOException, JsonProcessingException
    {
        DeserializationContext ctxt = createDeserializationContext(p);
        JsonDeserializer<?> deser = _findRootDeserializer(ctxt, valueType);
        // false -> do NOT close JsonParser (since caller passed it)
        return new MappingIterator<T>(valueType, p, ctxt, deser,
                false, null);
    }

    /**
     * Convenience method, equivalent in function to:
     *<pre>
     *   readerFor(valueType).readValues(p);
     *</pre>
     *<p>
     * Type-safe overload of {@link #readValues(JsonParser, JavaType)}.
     */
    public <T> MappingIterator<T> readValues(JsonParser p, Class<T> valueType)
        throws IOException, JsonProcessingException
    {
        return readValues(p, _typeFactory.constructType(valueType));
    }

    /*
    /**********************************************************************
    /* Public API: deserialization
    /* (mapping from token stream to Java types)
    /**********************************************************************
     */

    /**
     * Method to deserialize JSON content as tree expressed
     * using set of {@link JsonNode} instances.
     * Returns root of the resulting tree (where root can consist
     * of just a single node if the current event is a
     * value event, not container).
     *<p>
     * If a low-level I/O problem (missing input, network error) occurs,
     * a {@link IOException} will be thrown.
     * If a parsing problem occurs (invalid JSON),
     * {@link JsonParseException} will be thrown.
     * If no content is found from input (end-of-input), Java
     * <code>null</code> will be returned.
     * 
     * @param in Input stream used to read JSON content
     *   for building the JSON tree.
     * 
     * @return a {@link JsonNode}, if valid JSON content found; null
     *   if input has no content to bind -- note, however, that if
     *   JSON <code>null</code> token is found, it will be represented
     *   as a non-null {@link JsonNode} (one that returns <code>true</code>
     *   for {@link JsonNode#isNull()}
     *   
     * @throws JsonParseException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     */
    public JsonNode readTree(InputStream in) throws IOException
    {
        DeserializationContext ctxt = createDeserializationContext();
        return _readTreeAndClose(ctxt, _streamFactory.createParser(ctxt, in));
    }

    /**
     * Method to deserialize JSON content as tree expressed
     * using set of {@link JsonNode} instances.
     * Returns root of the resulting tree (where root can consist
     * of just a single node if the current event is a
     * value event, not container).
     *<p>
     * If a low-level I/O problem (missing input, network error) occurs,
     * a {@link IOException} will be thrown.
     * If a parsing problem occurs (invalid JSON),
     * {@link JsonParseException} will be thrown.
     * If no content is found from input (end-of-input), Java
     * <code>null</code> will be returned.
     *
     * @param r Reader used to read JSON content
     *   for building the JSON tree.
     * 
     * @return a {@link JsonNode}, if valid JSON content found; null
     *   if input has no content to bind -- note, however, that if
     *   JSON <code>null</code> token is found, it will be represented
     *   as a non-null {@link JsonNode} (one that returns <code>true</code>
     *   for {@link JsonNode#isNull()}
     */
    public JsonNode readTree(Reader r) throws IOException {
        DeserializationContext ctxt = createDeserializationContext();
        return _readTreeAndClose(ctxt, _streamFactory.createParser(ctxt, r));
    }

    /**
     * Method to deserialize JSON content as tree expressed using set of {@link JsonNode} instances.
     * Returns root of the resulting tree (where root can consist of just a single node if the current
     * event is a value event, not container).
     *<p>
     * If a low-level I/O problem (missing input, network error) occurs,
     * a {@link IOException} will be thrown.
     * If a parsing problem occurs (invalid JSON),
     * {@link JsonParseException} will be thrown.
     * If no content is found from input (end-of-input), Java
     * <code>null</code> will be returned.
     *
     * @param content JSON content to parse to build the JSON tree.
     * 
     * @return a {@link JsonNode}, if valid JSON content found; null
     *   if input has no content to bind -- note, however, that if
     *   JSON <code>null</code> token is found, it will be represented
     *   as a non-null {@link JsonNode} (one that returns <code>true</code>
     *   for {@link JsonNode#isNull()}
     *
     * @throws JsonParseException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     */
    public JsonNode readTree(String content) throws IOException {
        DeserializationContext ctxt = createDeserializationContext();
        return _readTreeAndClose(ctxt, _streamFactory.createParser(ctxt, content));
    }

    /**
     * Method to deserialize JSON content as tree expressed using set of {@link JsonNode} instances.
     * Returns root of the resulting tree (where root can consist of just a single node if the current
     * event is a value event, not container).
     *
     * @param content JSON content to parse to build the JSON tree.
     * 
     * @return a {@link JsonNode}, if valid JSON content found; null
     *   if input has no content to bind -- note, however, that if
     *   JSON <code>null</code> token is found, it will be represented
     *   as a non-null {@link JsonNode} (one that returns <code>true</code>
     *   for {@link JsonNode#isNull()}
     *
     * @throws JsonParseException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     */
    public JsonNode readTree(byte[] content) throws IOException {
        DeserializationContext ctxt = createDeserializationContext();
        return _readTreeAndClose(ctxt, _streamFactory.createParser(ctxt, content));
    }
    
    /**
     * Method to deserialize JSON content as tree expressed using set of {@link JsonNode} instances.
     * Returns root of the resulting tree (where root can consist of just a single node if the current
     * event is a value event, not container).
     *
     * @param file File of which contents to parse as JSON for building a tree instance
     * 
     * @return a {@link JsonNode}, if valid JSON content found; null
     *   if input has no content to bind -- note, however, that if
     *   JSON <code>null</code> token is found, it will be represented
     *   as a non-null {@link JsonNode} (one that returns <code>true</code>
     *   for {@link JsonNode#isNull()}
     * 
     * @throws IOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws JsonParseException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     */
    public JsonNode readTree(File file)
        throws IOException, JsonProcessingException
    {
        DeserializationContext ctxt = createDeserializationContext();
        return _readTreeAndClose(ctxt, _streamFactory.createParser(ctxt, file));
    }

    /**
     * Method to deserialize JSON content as tree expressed using set of {@link JsonNode} instances.
     * Returns root of the resulting tree (where root can consist of just a single node if the current
     * event is a value event, not container).
     *
     * @param source URL to use for fetching contents to parse as JSON for building a tree instance
     * 
     * @return a {@link JsonNode}, if valid JSON content found; null
     *   if input has no content to bind -- note, however, that if
     *   JSON <code>null</code> token is found, it will be represented
     *   as a non-null {@link JsonNode} (one that returns <code>true</code>
     *   for {@link JsonNode#isNull()}
     * 
     * @throws IOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws JsonParseException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     */
    public JsonNode readTree(URL source) throws IOException {
        DeserializationContext ctxt = createDeserializationContext();
        return _readTreeAndClose(ctxt, _streamFactory.createParser(ctxt, source));
    }

    /*
    /**********************************************************************
    /* Public API serialization
    /* (mapping from Java types to token streams)
    /**********************************************************************
     */

    /**
     * Method that can be used to serialize any Java value as
     * JSON output, using provided {@link JsonGenerator}.
     */
    public void writeValue(JsonGenerator g, Object value)
        throws IOException, JsonGenerationException, JsonMappingException
    {
        SerializationConfig config = serializationConfig();
        // 04-Oct-2017, tatu: Generator should come properly configured and we should not
        //   change its state in any way, I think (at least with Jackson 3.0)
        /*
        if (config.isEnabled(SerializationFeature.INDENT_OUTPUT)) {
            if (g.getPrettyPrinter() == null) {
                g.setPrettyPrinter(config.constructDefaultPrettyPrinter());
            }
        }
        */
        if (config.isEnabled(SerializationFeature.CLOSE_CLOSEABLE) && (value instanceof Closeable)) {
            _writeCloseableValue(g, value, config);
        } else {
            _serializerProvider(config).serializeValue(g, value);
            if (config.isEnabled(SerializationFeature.FLUSH_AFTER_WRITE_VALUE)) {
                g.flush();
            }
        }
    }

    /*
    /**********************************************************
    /* Public API: Tree Model support
    /**********************************************************
     */

    public void writeTree(JsonGenerator g, TreeNode rootNode)
        throws IOException, JsonProcessingException
    {
        SerializationConfig config = serializationConfig();
        _serializerProvider(config).serializeValue(g, rootNode);
        if (config.isEnabled(SerializationFeature.FLUSH_AFTER_WRITE_VALUE)) {
            g.flush();
        }
    }
    
    /**
     * Method to serialize given JSON Tree, using generator
     * provided.
     */
    public void writeTree(JsonGenerator g, JsonNode rootNode)
        throws IOException, JsonProcessingException
    {
        SerializationConfig config = serializationConfig();
        _serializerProvider(config).serializeValue(g, rootNode);
        if (config.isEnabled(SerializationFeature.FLUSH_AFTER_WRITE_VALUE)) {
            g.flush();
        }
    }
    
    /**
     *<p>
     * Note: return type is co-variant, as basic ObjectCodec
     * abstraction cannot refer to concrete node types (as it's
     * part of core package, whereas impls are part of mapper
     * package)
     */
    public ObjectNode createObjectNode() {
        return _deserializationConfig.getNodeFactory().objectNode();
    }

    /**
     *<p>
     * Note: return type is co-variant, as basic ObjectCodec
     * abstraction cannot refer to concrete node types (as it's
     * part of core package, whereas impls are part of mapper
     * package)
     */
    public ArrayNode createArrayNode() {
        return _deserializationConfig.getNodeFactory().arrayNode();
    }

    /**
     * Method for constructing a {@link JsonParser} out of JSON tree
     * representation.
     * 
     * @param n Root node of the tree that resulting parser will read from
     */
    public JsonParser treeAsTokens(TreeNode n) {
        DeserializationContext ctxt = createDeserializationContext();
        return new TreeTraversingParser((JsonNode) n, ctxt);
    }

    /**
     * Convenience conversion method that will bind data given JSON tree
     * contains into specific value (usually bean) type.
     *<p>
     * Functionally equivalent to:
     *<pre>
     *   objectMapper.convertValue(n, valueClass);
     *</pre>
     */
    @SuppressWarnings("unchecked")
    public <T> T treeToValue(TreeNode n, Class<T> valueType)
        throws JsonProcessingException
    {
        try {
            // Simple cast when we just want to cast to, say, ObjectNode
            // ... one caveat; while everything is Object.class, let's not take shortcut
            if (valueType != Object.class && valueType.isAssignableFrom(n.getClass())) {
                return (T) n;
            }
            // 20-Apr-2016, tatu: Another thing: for VALUE_EMBEDDED_OBJECT, assume similar
            //    short-cut coercion
            if (n.asToken() == JsonToken.VALUE_EMBEDDED_OBJECT) {
                if (n instanceof POJONode) {
                    Object ob = ((POJONode) n).getPojo();
                    if ((ob == null) || valueType.isInstance(ob)) {
                        return (T) ob;
                    }
                }
            }
            return readValue(treeAsTokens(n), valueType);
        } catch (JsonProcessingException e) {
            throw e;
        } catch (IOException e) { // should not occur, no real i/o...
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /**
     * Reverse of {@link #treeToValue}; given a value (usually bean), will
     * construct equivalent JSON Tree representation. Functionally similar
     * to serializing value into JSON and parsing JSON as tree, but
     * more efficient.
     *<p>
     * NOTE: while results are usually identical to that of serialization followed
     * by deserialization, this is not always the case. In some cases serialization
     * into intermediate representation will retain encapsulation of things like
     * raw value ({@link com.fasterxml.jackson.databind.util.RawValue}) or basic
     * node identity ({@link JsonNode}). If so, result is a valid tree, but values
     * are not re-constructed through actual JSON representation. So if transformation
     * requires actual materialization of JSON (or other data format that this mapper
     * produces), it will be necessary to do actual serialization.
     * 
     * @param <T> Actual node type; usually either basic {@link JsonNode} or
     *  {@link com.fasterxml.jackson.databind.node.ObjectNode}
     * @param fromValue Bean value to convert
     * @return Root node of the resulting JSON tree
     */
    @SuppressWarnings({ "unchecked", "resource" })
    public <T extends JsonNode> T valueToTree(Object fromValue)
        throws IllegalArgumentException
    {
        if (fromValue == null) {
            return null;
        }
        // 06-Oct-2017, tatu: `convertValue()` disables root value wrapping so
        //   do it here too
        SerializationConfig config = serializationConfig()
            .without(SerializationFeature.WRAP_ROOT_VALUE);
        DefaultSerializerProvider prov = _serializerProvider(config);
        TokenBuffer buf = TokenBuffer.forValueConversion(prov);
        // Would like to let buffer decide, but it won't have deser config to check so...
        if (isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)) {
            buf = buf.forceUseOfBigDecimal(true);
        }
        JsonNode result;
        try {
            // Equivalent to `writeValue()`, basically:
            prov.serializeValue(buf, fromValue);
            JsonParser p = buf.asParser();
            result = readTree(p);
            p.close();
        } catch (IOException e) { // should not occur, no real i/o...
            throw new IllegalArgumentException(e.getMessage(), e);
        }
        return (T) result;
    }

    /*
    /**********************************************************
    /* Public API, deserialization,
    /**********************************************************
     */

    /**
     * Method to deserialize JSON content from given file into given Java type.
     * 
     * @throws IOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws JsonParseException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws JsonMappingException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(File src, Class<T> valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _readMapAndClose(ctxt, _streamFactory.createParser(ctxt, src),
                _typeFactory.constructType(valueType));
    } 

    /**
     * Method to deserialize JSON content from given file into given Java type.
     * 
     * @throws IOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws JsonParseException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws JsonMappingException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> T readValue(File src, TypeReference valueTypeRef)
        throws IOException, JsonParseException, JsonMappingException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _readMapAndClose(ctxt, _streamFactory.createParser(ctxt, src),
                _typeFactory.constructType(valueTypeRef));
    } 

    /**
     * Method to deserialize JSON content from given file into given Java type.
     * 
     * @throws IOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws JsonParseException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws JsonMappingException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(File src, JavaType valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _readMapAndClose(ctxt, _streamFactory.createParser(ctxt, src), valueType);
    }

    /**
     * Method to deserialize JSON content from given resource into given Java type.
     * 
     * @throws IOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws JsonParseException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws JsonMappingException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(URL src, Class<T> valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _readMapAndClose(ctxt,
                _streamFactory.createParser(ctxt, src), _typeFactory.constructType(valueType));
    } 

    /**
     * Method to deserialize JSON content from given resource into given Java type.
     * 
     * @throws IOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws JsonParseException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws JsonMappingException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> T readValue(URL src, TypeReference valueTypeRef)
        throws IOException, JsonParseException, JsonMappingException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _readMapAndClose(ctxt,
                _streamFactory.createParser(ctxt, src), _typeFactory.constructType(valueTypeRef));
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(URL src, JavaType valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _readMapAndClose(ctxt,
                _streamFactory.createParser(ctxt, src), valueType);
    } 

    /**
     * Method to deserialize JSON content from given JSON content String.
     * 
     * @throws IOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws JsonParseException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws JsonMappingException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(String content, Class<T> valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _readMapAndClose(ctxt,
                _streamFactory.createParser(ctxt, content), _typeFactory.constructType(valueType));
    } 

    /**
     * Method to deserialize JSON content from given JSON content String.
     * 
     * @throws IOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws JsonParseException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws JsonMappingException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> T readValue(String content, TypeReference valueTypeRef)
        throws IOException, JsonParseException, JsonMappingException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _readMapAndClose(ctxt,
                _streamFactory.createParser(ctxt, content), _typeFactory.constructType(valueTypeRef));
    } 

    /**
     * Method to deserialize JSON content from given JSON content String.
     * 
     * @throws IOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws JsonParseException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws JsonMappingException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(String content, JavaType valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _readMapAndClose(ctxt,
                _streamFactory.createParser(ctxt, content), valueType);
    }

    @SuppressWarnings("unchecked")
    public <T> T readValue(Reader src, Class<T> valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _readMapAndClose(ctxt,
                _streamFactory.createParser(ctxt, src), _typeFactory.constructType(valueType));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> T readValue(Reader src, TypeReference valueTypeRef)
        throws IOException, JsonParseException, JsonMappingException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _readMapAndClose(ctxt,
                _streamFactory.createParser(ctxt, src), _typeFactory.constructType(valueTypeRef));
    }

    @SuppressWarnings("unchecked")
    public <T> T readValue(Reader src, JavaType valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _readMapAndClose(ctxt,
                _streamFactory.createParser(ctxt, src), valueType);
    }

    @SuppressWarnings("unchecked")
    public <T> T readValue(InputStream src, Class<T> valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _readMapAndClose(ctxt,
                _streamFactory.createParser(ctxt, src), _typeFactory.constructType(valueType));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> T readValue(InputStream src, TypeReference valueTypeRef)
        throws IOException, JsonParseException, JsonMappingException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _readMapAndClose(ctxt,
                _streamFactory.createParser(ctxt, src), _typeFactory.constructType(valueTypeRef));
    }

    @SuppressWarnings("unchecked")
    public <T> T readValue(InputStream src, JavaType valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _readMapAndClose(ctxt,
                _streamFactory.createParser(ctxt, src), valueType);
    }

    @SuppressWarnings("unchecked")
    public <T> T readValue(byte[] src, Class<T> valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _readMapAndClose(ctxt,
                _streamFactory.createParser(ctxt, src), _typeFactory.constructType(valueType));
    }

    @SuppressWarnings("unchecked")
    public <T> T readValue(byte[] src, int offset, int len, Class<T> valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _readMapAndClose(ctxt,
                _streamFactory.createParser(ctxt, src, offset, len), _typeFactory.constructType(valueType));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> T readValue(byte[] src, TypeReference valueTypeRef)
        throws IOException, JsonParseException, JsonMappingException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _readMapAndClose(ctxt,
                _streamFactory.createParser(ctxt, src), _typeFactory.constructType(valueTypeRef));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> T readValue(byte[] src, int offset, int len, TypeReference valueTypeRef)
        throws IOException, JsonParseException, JsonMappingException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _readMapAndClose(ctxt,
                _streamFactory.createParser(ctxt, src, offset, len), _typeFactory.constructType(valueTypeRef));
    }

    @SuppressWarnings("unchecked")
    public <T> T readValue(byte[] src, JavaType valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _readMapAndClose(ctxt,
                _streamFactory.createParser(ctxt, src), valueType);
    }

    @SuppressWarnings("unchecked")
    public <T> T readValue(byte[] src, int offset, int len,
                           JavaType valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _readMapAndClose(ctxt,
                _streamFactory.createParser(ctxt, src, offset, len), valueType);
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(DataInput src, Class<T> valueType) throws IOException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _readMapAndClose(ctxt,
                _streamFactory.createParser(ctxt, src), _typeFactory.constructType(valueType));
    }

    @SuppressWarnings("unchecked")
    public <T> T readValue(DataInput src, JavaType valueType) throws IOException
    {
        DefaultDeserializationContext ctxt = createDeserializationContext();
        return (T) _readMapAndClose(ctxt,
                _streamFactory.createParser(ctxt, src), valueType);
    }

    /*
    /**********************************************************
    /* Public API: serialization
    /* (mapping from Java types to JSON)
    /**********************************************************
     */

    /**
     * Method that can be used to serialize any Java value as
     * JSON output, written to File provided.
     */
    public void writeValue(File resultFile, Object value)
        throws IOException, JsonGenerationException, JsonMappingException
    {
        DefaultSerializerProvider prov = _serializerProvider();
        _configAndWriteValue(prov,
                _streamFactory.createGenerator(prov, resultFile, JsonEncoding.UTF8), value);
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
    public void writeValue(OutputStream out, Object value)
        throws IOException, JsonGenerationException, JsonMappingException
    {
        DefaultSerializerProvider prov = _serializerProvider();
        _configAndWriteValue(prov,
                _streamFactory.createGenerator(prov, out, JsonEncoding.UTF8), value);
    }

    public void writeValue(DataOutput out, Object value)
        throws IOException
    {
        DefaultSerializerProvider prov = _serializerProvider();
        _configAndWriteValue(prov,
                _streamFactory.createGenerator(prov, out), value);
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
    public void writeValue(Writer w, Object value)
        throws IOException, JsonGenerationException, JsonMappingException
    {
        DefaultSerializerProvider prov = _serializerProvider();
        _configAndWriteValue(prov, _streamFactory.createGenerator(prov, w), value);
    }

    /**
     * Method that can be used to serialize any Java value as
     * a String. Functionally equivalent to calling
     * {@link #writeValue(Writer,Object)} with {@link java.io.StringWriter}
     * and constructing String, but more efficient.
     */
    @SuppressWarnings("resource")
    public String writeValueAsString(Object value)
        throws JsonProcessingException
    {
        // alas, we have to pull the recycler directly here...
        SegmentedStringWriter sw = new SegmentedStringWriter(_streamFactory._getBufferRecycler());
        DefaultSerializerProvider prov = _serializerProvider();
        try {
            _configAndWriteValue(prov, _streamFactory.createGenerator(prov, sw), value);
        } catch (JsonProcessingException e) {
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
     */
    @SuppressWarnings("resource")
    public byte[] writeValueAsBytes(Object value)
        throws JsonProcessingException
    {
        DefaultSerializerProvider prov = _serializerProvider();
        ByteArrayBuilder bb = new ByteArrayBuilder(_streamFactory._getBufferRecycler());
        try {
            _configAndWriteValue(prov,
                    _streamFactory.createGenerator(prov, bb, JsonEncoding.UTF8), value);
        } catch (JsonProcessingException e) {
            throw e;
        } catch (IOException e) { // shouldn't really happen, but is declared as possibility so:
            throw JsonMappingException.fromUnexpectedIOE(e);
        }
        byte[] result = bb.toByteArray();
        bb.release();
        return result;
    }

    /**
     * Method called to configure the generator as necessary and then
     * call write functionality
     */
    protected final void _configAndWriteValue(DefaultSerializerProvider prov,
            JsonGenerator g, Object value)
        throws IOException
    {
        if (prov.isEnabled(SerializationFeature.CLOSE_CLOSEABLE) && (value instanceof Closeable)) {
            _configAndWriteCloseable(prov, g, value);
            return;
        }
        try {
            prov.serializeValue(g, value);
        } catch (Exception e) {
            ClassUtil.closeOnFailAndThrowAsIOE(g, e);
            return;
        }
        g.close();
    }

    /**
     * Helper method used when value to serialize is {@link Closeable} and its <code>close()</code>
     * method is to be called right after serialization has been called
     */
    private final void _configAndWriteCloseable(DefaultSerializerProvider prov,
            JsonGenerator g, Object value)
        throws IOException
    {
        Closeable toClose = (Closeable) value;
        try {
            prov.serializeValue(g, value);
            Closeable tmpToClose = toClose;
            toClose = null;
            tmpToClose.close();
        } catch (Exception e) {
            ClassUtil.closeOnFailAndThrowAsIOE(g, toClose, e);
            return;
        }
        g.close();
    }

    /**
     * Helper method used when value to serialize is {@link Closeable} and its <code>close()</code>
     * method is to be called right after serialization has been called
     */
    protected final void _writeCloseableValue(JsonGenerator g, Object value, SerializationConfig cfg)
        throws IOException
    {
        Closeable toClose = (Closeable) value;
        try {
            _serializerProvider(cfg).serializeValue(g, value);
            if (cfg.isEnabled(SerializationFeature.FLUSH_AFTER_WRITE_VALUE)) {
                g.flush();
            }
        } catch (Exception e) {
            ClassUtil.closeOnFailAndThrowAsIOE(null, toClose, e);
            return;
        }
        toClose.close();
    }

    /*
    /**********************************************************
    /* Public API: constructing ObjectWriters
    /* for more advanced configuration
    /**********************************************************
     */

    /**
     * Convenience method for constructing {@link ObjectWriter}
     * with default settings.
     */
    public ObjectWriter writer() {
        return _newWriter(serializationConfig());
    }

    /**
     * Factory method for constructing {@link ObjectWriter} with
     * specified feature enabled (compared to settings that this
     * mapper instance has).
     */
    public ObjectWriter writer(SerializationFeature feature) {
        return _newWriter(serializationConfig().with(feature));
    }

    /**
     * Factory method for constructing {@link ObjectWriter} with
     * specified features enabled (compared to settings that this
     * mapper instance has).
     */
    public ObjectWriter writer(SerializationFeature first,
            SerializationFeature... other) {
        return _newWriter(serializationConfig().with(first, other));
    }
    
    /**
     * Factory method for constructing {@link ObjectWriter} that will
     * serialize objects using specified {@link DateFormat}; or, if
     * null passed, using timestamp (64-bit number.
     */
    public ObjectWriter writer(DateFormat df) {
        return _newWriter(serializationConfig().with(df));
    }
    
    /**
     * Factory method for constructing {@link ObjectWriter} that will
     * serialize objects using specified JSON View (filter).
     */
    public ObjectWriter writerWithView(Class<?> serializationView) {
        return _newWriter(serializationConfig().withView(serializationView));
    }
    
    /**
     * Factory method for constructing {@link ObjectWriter} that will
     * serialize objects using specified root type, instead of actual
     * runtime type of value. Type must be a super-type of runtime type.
     *<p>
     * Main reason for using this method is performance, as writer is able
     * to pre-fetch serializer to use before write, and if writer is used
     * more than once this avoids addition per-value serializer lookups.
     */
    public ObjectWriter writerFor(Class<?> rootType) {
        return _newWriter(serializationConfig(),
                ((rootType == null) ? null :_typeFactory.constructType(rootType)),
                /*PrettyPrinter*/null);
    }

    /**
     * Factory method for constructing {@link ObjectWriter} that will
     * serialize objects using specified root type, instead of actual
     * runtime type of value. Type must be a super-type of runtime type.
     *<p>
     * Main reason for using this method is performance, as writer is able
     * to pre-fetch serializer to use before write, and if writer is used
     * more than once this avoids addition per-value serializer lookups.
     */
    public ObjectWriter writerFor(TypeReference<?> rootType) {
        return _newWriter(serializationConfig(),
                ((rootType == null) ? null : _typeFactory.constructType(rootType)),
                /*PrettyPrinter*/null);
    }

    /**
     * Factory method for constructing {@link ObjectWriter} that will
     * serialize objects using specified root type, instead of actual
     * runtime type of value. Type must be a super-type of runtime type.
     *<p>
     * Main reason for using this method is performance, as writer is able
     * to pre-fetch serializer to use before write, and if writer is used
     * more than once this avoids addition per-value serializer lookups.
     */
    public ObjectWriter writerFor(JavaType rootType) {
        return _newWriter(serializationConfig(), rootType, /*PrettyPrinter*/null);
    }

    /**
     * Factory method for constructing {@link ObjectWriter} that will
     * serialize objects using the default pretty printer for indentation
     */
    public ObjectWriter writerWithDefaultPrettyPrinter() {
        SerializationConfig config = serializationConfig();
        return _newWriter(config,
                /*root type*/ null, config.getDefaultPrettyPrinter());
    }
    
    /**
     * Factory method for constructing {@link ObjectWriter} that will
     * serialize objects using specified filter provider.
     */
    public ObjectWriter writer(FilterProvider filterProvider) {
        return _newWriter(serializationConfig().withFilters(filterProvider));
    }
    
    /**
     * Factory method for constructing {@link ObjectWriter} that will
     * pass specific schema object to {@link JsonGenerator} used for
     * writing content.
     * 
     * @param schema Schema to pass to generator
     */
    public ObjectWriter writer(FormatSchema schema) {
        _verifySchemaType(schema);
        return _newWriter(serializationConfig(), schema);
    }

    /**
     * Factory method for constructing {@link ObjectWriter} that will
     * use specified Base64 encoding variant for Base64-encoded binary data.
     */
    public ObjectWriter writer(Base64Variant defaultBase64) {
        return _newWriter(serializationConfig().with(defaultBase64));
    }

    /**
     * Factory method for constructing {@link ObjectReader} that will
     * use specified character escaping details for output.
     */
    public ObjectWriter writer(CharacterEscapes escapes) {
        return _newWriter(serializationConfig()).with(escapes);
    }

    /**
     * Factory method for constructing {@link ObjectWriter} that will
     * use specified default attributes.
     */
    public ObjectWriter writer(ContextAttributes attrs) {
        return _newWriter(serializationConfig().with(attrs));
    }

    /*
    /**********************************************************
    /* Extended Public API: constructing ObjectReaders
    /* for more advanced configuration
    /**********************************************************
     */

    /**
     * Factory method for constructing {@link ObjectReader} with
     * default settings. Note that the resulting instance is NOT usable as is,
     * without defining expected value type.
     */
    public ObjectReader reader() {
        return _newReader(deserializationConfig()).with(_injectableValues);
    }

    /**
     * Factory method for constructing {@link ObjectReader} with
     * specified feature enabled (compared to settings that this
     * mapper instance has).
     * Note that the resulting instance is NOT usable as is,
     * without defining expected value type.
     */
    public ObjectReader reader(DeserializationFeature feature) {
        return _newReader(deserializationConfig().with(feature));
    }

    /**
     * Factory method for constructing {@link ObjectReader} with
     * specified features enabled (compared to settings that this
     * mapper instance has).
     * Note that the resulting instance is NOT usable as is,
     * without defining expected value type.
     */
    public ObjectReader reader(DeserializationFeature first,
            DeserializationFeature... other) {
        return _newReader(deserializationConfig().with(first, other));
    }
    
    /**
     * Factory method for constructing {@link ObjectReader} that will
     * update given Object (usually Bean, but can be a Collection or Map
     * as well, but NOT an array) with JSON data. Deserialization occurs
     * normally except that the root-level value in JSON is not used for
     * instantiating a new object; instead give updateable object is used
     * as root.
     * Runtime type of value object is used for locating deserializer,
     * unless overridden by other factory methods of {@link ObjectReader}
     */
    public ObjectReader readerForUpdating(Object valueToUpdate) {
        JavaType t = _typeFactory.constructType(valueToUpdate.getClass());
        return _newReader(deserializationConfig(), t, valueToUpdate,
                null, _injectableValues);
    }

    /**
     * Factory method for constructing {@link ObjectReader} that will
     * read or update instances of specified type
     */
    public ObjectReader readerFor(JavaType type) {
        return _newReader(deserializationConfig(), type, null,
                null, _injectableValues);
    }

    /**
     * Factory method for constructing {@link ObjectReader} that will
     * read or update instances of specified type
     */
    public ObjectReader readerFor(Class<?> type) {
        return _newReader(deserializationConfig(), _typeFactory.constructType(type), null,
                null, _injectableValues);
    }

    /**
     * Factory method for constructing {@link ObjectReader} that will
     * read or update instances of specified type
     */
    public ObjectReader readerFor(TypeReference<?> type) {
        return _newReader(deserializationConfig(), _typeFactory.constructType(type), null,
                null, _injectableValues);
    }

    /**
     * Factory method for constructing {@link ObjectReader} that will
     * use specified {@link JsonNodeFactory} for constructing JSON trees.
     */
    public ObjectReader reader(JsonNodeFactory f) {
        return _newReader(deserializationConfig()).with(f);
    }

    /**
     * Factory method for constructing {@link ObjectReader} that will
     * pass specific schema object to {@link JsonParser} used for
     * reading content.
     * 
     * @param schema Schema to pass to parser
     */
    public ObjectReader reader(FormatSchema schema) {
        _verifySchemaType(schema);
        return _newReader(deserializationConfig(), null, null,
                schema, _injectableValues);
    }

    /**
     * Factory method for constructing {@link ObjectReader} that will
     * use specified injectable values.
     * 
     * @param injectableValues Injectable values to use
     */
    public ObjectReader reader(InjectableValues injectableValues) {
        return _newReader(deserializationConfig(), null, null,
                null, injectableValues);
    }

    /**
     * Factory method for constructing {@link ObjectReader} that will
     * deserialize objects using specified JSON View (filter).
     */
    public ObjectReader readerWithView(Class<?> view) {
        return _newReader(deserializationConfig().withView(view));
    }

    /**
     * Factory method for constructing {@link ObjectReader} that will
     * use specified Base64 encoding variant for Base64-encoded binary data.
     */
    public ObjectReader reader(Base64Variant defaultBase64) {
        return _newReader(deserializationConfig().with(defaultBase64));
    }

    /**
     * Factory method for constructing {@link ObjectReader} that will
     * use specified default attributes.
     */
    public ObjectReader reader(ContextAttributes attrs) {
        return _newReader(deserializationConfig().with(attrs));
    }

    /*
    /**********************************************************
    /* Extended Public API: convenience type conversion
    /**********************************************************
     */

    /**
     * Convenience method for doing two-step conversion from given value, into
     * instance of given value type, if (but only if!) conversion is needed.
     * If given value is already of requested type, value is returned as is.
     *<p>
     * This method is functionally similar to first
     * serializing given value into JSON, and then binding JSON data into value
     * of given type, but should be more efficient since full serialization does
     * not (need to) occur.
     * However, same converters (serializers, deserializers) will be used as for
     * data binding, meaning same object mapper configuration works.
     *<p>
     * Note that it is possible that in some cases behavior does differ from
     * full serialize-then-deserialize cycle: in most case differences are
     * unintentional (that is, flaws to fix) and should be reported.
     * It is not guaranteed, however, that the behavior is 100% the same:
     * the goal is just to allow efficient value conversions for structurally
     * compatible Objects, according to standard Jackson configuration.
     *<p>
     * Further note that this functionality is not designed to support "advanced" use
     * cases, such as conversion of polymorphic values, or cases where Object Identity
     * is used.
     *      
     * @throws IllegalArgumentException If conversion fails due to incompatible type;
     *    if so, root cause will contain underlying checked exception data binding
     *    functionality threw
     */
    @SuppressWarnings("unchecked")
    public <T> T convertValue(Object fromValue, Class<T> toValueType)
        throws IllegalArgumentException
    {
        return (T) _convert(fromValue, _typeFactory.constructType(toValueType));
    } 

    /**
     * See {@link #convertValue(Object, Class)}
     */
    @SuppressWarnings("unchecked")
    public <T> T convertValue(Object fromValue, TypeReference<T> toValueTypeRef)
        throws IllegalArgumentException
    {
        return (T) _convert(fromValue, _typeFactory.constructType(toValueTypeRef));
    } 

    /**
     * See {@link #convertValue(Object, Class)}
     */
    @SuppressWarnings("unchecked")
    public <T> T convertValue(Object fromValue, JavaType toValueType)
        throws IllegalArgumentException
    {
        return (T) _convert(fromValue, toValueType);
    } 

    /**
     * Actual conversion implementation: instead of using existing read
     * and write methods, much of code is inlined. Reason for this is
     * that we must avoid root value wrapping/unwrapping both for efficiency and
     * for correctness. If root value wrapping/unwrapping is actually desired,
     * caller must use explicit <code>writeValue</code> and
     * <code>readValue</code> methods.
     */
    @SuppressWarnings("resource")
    protected Object _convert(Object fromValue, JavaType toValueType)
        throws IllegalArgumentException
    {
        // [databind#1433] Do not shortcut null values.
        // This defaults primitives and fires deserializer getNullValue hooks.
        if (fromValue != null) {
            // also, as per [databind#11], consider case for simple cast
            // But with caveats: one is that while everything is Object.class, we don't
            // want to "optimize" that out; and the other is that we also do not want
            // to lose conversions of generic types.
            Class<?> targetType = toValueType.getRawClass();
            if (targetType != Object.class
                    && !toValueType.hasGenericTypes()
                    && targetType.isAssignableFrom(fromValue.getClass())) {
                return fromValue;
            }
        }

        // inlined 'writeValue' with minor changes:
        // first: disable wrapping when writing
        SerializationConfig config = serializationConfig()
                .without(SerializationFeature.WRAP_ROOT_VALUE);
        DefaultSerializerProvider prov = _serializerProvider(config);
        TokenBuffer buf = TokenBuffer.forValueConversion(prov);
        // Would like to let buffer decide, but it won't have deser config to check so...
        if (isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)) {
            buf = buf.forceUseOfBigDecimal(true);
        }
        try {
            // no need to check for closing of TokenBuffer
            prov.serializeValue(buf, fromValue);

            // then matching read, inlined 'readValue' with minor mods:
            DefaultDeserializationContext readCtxt = createDeserializationContext();
            final JsonParser p = buf.asParser(readCtxt);
            readCtxt.assignParser(p);
            Object result;
            // ok to pass in existing feature flags; unwrapping handled by mapper
            JsonToken t = _initForReading(p, toValueType);
            if (t == JsonToken.VALUE_NULL) {
                result = _findRootDeserializer(readCtxt, toValueType).getNullValue(readCtxt);
            } else if (t == JsonToken.END_ARRAY || t == JsonToken.END_OBJECT) {
                result = null;
            } else { // pointing to event other than null
                JsonDeserializer<Object> deser = _findRootDeserializer(readCtxt, toValueType);
                // note: no handling of unwrapping
                result = deser.deserialize(p, readCtxt);
            }
            p.close();
            return result;
        } catch (IOException e) { // should not occur, no real i/o...
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /**
     * Convenience method similar to {@link #convertValue(Object, JavaType)} but one
     * in which 
     *<p>
     * Implementation is approximately as follows:
     *<ol>
     * <li>Serialize `updateWithValue` into {@link TokenBuffer}</li>
     * <li>Construct {@link ObjectReader} with `valueToUpdate` (using {@link #readerForUpdating(Object)})
     *   </li>
     * <li>Construct {@link JsonParser} (using {@link TokenBuffer#asParser()})
     *   </li>
     * <li>Update using {@link ObjectReader#readValue(JsonParser)}.
     *   </li>
     * <li>Return `valueToUpdate`
     *   </li>
     *</ol>
     *<p>
     * Note that update is "shallow" in that only first level of properties (or, immediate contents
     * of container to update) are modified, unless properties themselves indicate that
     * merging should be applied for contents. Such merging can be specified using
     * annotations (see <code>JsonMerge</code>) as well as using "config overrides" (see
     * {@link MapperBuilder#withConfigOverride} and {@link MapperBuilder#defaultMergeable}).
     *
     * @param valueToUpdate Object to update
     * @param overrides Object to conceptually serialize and merge into value to
     *     update; can be thought of as a provider for overrides to apply.
     * 
     * @return Either the first argument (`valueToUpdate`), if it is mutable; or a result of
     *     creating new instance that is result of "merging" values (for example, "updating" a
     *     Java array will create a new array)
     *
     * @throws JsonMappingException if there are structural incompatibilities that prevent update.
     */
    @SuppressWarnings("resource")
    public <T> T updateValue(T valueToUpdate, Object overrides)
        throws JsonMappingException
    {
        if ((valueToUpdate == null) || (overrides == null)) {
            return valueToUpdate;
        }
        SerializationConfig config = serializationConfig()
                .without(SerializationFeature.WRAP_ROOT_VALUE);
        DefaultSerializerProvider prov = _serializerProvider(config);
        TokenBuffer buf = TokenBuffer.forValueConversion(prov);
        // Would like to let buffer decide, but it won't have deser config to check so...
        if (isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)) {
            buf = buf.forceUseOfBigDecimal(true);
        }
        T result;
        try {
            prov.serializeValue(buf, overrides);
            JsonParser p = buf.asParser();
            result = readerForUpdating(valueToUpdate).readValue(p);
            p.close();
        } catch (IOException e) { // should not occur, no real i/o...
            if (e instanceof JsonMappingException) {
                throw (JsonMappingException) e;
            }
            // 17-Mar-2017, tatu: Really ought not happen...
            throw JsonMappingException.fromUnexpectedIOE(e);
        }
        return result;
    }

    /*
    /**********************************************************
    /* Extended Public API: JSON Schema generation
    /**********************************************************
     */

    /**
     * Method for visiting type hierarchy for given type, using specified visitor.
     *<p>
     * This method can be used for things like
     * generating <a href="http://json-schema.org/">JSON Schema</a>
     * instance for specified type.
     *
     * @param type Type to generate schema for (possibly with generic signature)
     */
    public void acceptJsonFormatVisitor(Class<?> type, JsonFormatVisitorWrapper visitor)
        throws JsonMappingException
    {
        acceptJsonFormatVisitor(_typeFactory.constructType(type), visitor);
    }

    public void acceptJsonFormatVisitor(TypeReference<?> typeRef, JsonFormatVisitorWrapper visitor)
        throws JsonMappingException
    {
        acceptJsonFormatVisitor(_typeFactory.constructType(typeRef), visitor);
    }

    /**
     * Method for visiting type hierarchy for given type, using specified visitor.
     * Visitation uses <code>Serializer</code> hierarchy and related properties
     *<p>
     * This method can be used for things like
     * generating <a href="http://json-schema.org/">JSON Schema</a>
     * instance for specified type.
     *
     * @param type Type to generate schema for (possibly with generic signature)
     */
    public void acceptJsonFormatVisitor(JavaType type, JsonFormatVisitorWrapper visitor)
        throws JsonMappingException
    {
        if (type == null) {
            throw new IllegalArgumentException("type must be provided");
        }
        _serializerProvider().acceptJsonFormatVisitor(type, visitor);
    }

    /*
    /**********************************************************************
    /* Internal methods for serialization, overridable
    /**********************************************************************
     */

    /**
     * Overridable helper method used for constructing
     * {@link SerializerProvider} to use for serialization.
     */
    protected DefaultSerializerProvider _serializerProvider(SerializationConfig config) {
        // 03-Oct-2017, tatu: Should be ok to pass "empty" generator settings...
        return _serializerProvider.createInstance(config,
                GeneratorSettings.empty(), _serializerFactory);
    }

    protected DefaultSerializerProvider _serializerProvider() {
        // 03-Oct-2017, tatu: Should be ok to pass "empty" generator settings...
        return _serializerProvider.createInstance(serializationConfig(),
                GeneratorSettings.empty(), _serializerFactory);
    }

    /*
    /**********************************************************************
    /* Internal methods for deserialization, overridable
    /**********************************************************************
     */

    /**
     * Actual implementation of value reading+binding operation.
     */
    protected Object _readValue(DeserializationContext ctxt, JsonParser p,
            JavaType valueType)
        throws IOException
    {
        /* First: may need to read the next token, to initialize
         * state (either before first read from parser, or after
         * previous token has been cleared)
         */
        Object result;
        JsonToken t = _initForReading(p, valueType);
        final DeserializationConfig config = ctxt.getConfig();
        if (t == JsonToken.VALUE_NULL) {
            // Ask JsonDeserializer what 'null value' to use:
            result = _findRootDeserializer(ctxt, valueType).getNullValue(ctxt);
        } else if (t == JsonToken.END_ARRAY || t == JsonToken.END_OBJECT) {
            result = null;
        } else { // pointing to event other than null
            JsonDeserializer<Object> deser = _findRootDeserializer(ctxt, valueType);
            // ok, let's get the value
            if (config.useRootWrapping()) {
                result = _unwrapAndDeserialize(p, ctxt, config, valueType, deser);
            } else {
                result = deser.deserialize(p, ctxt);
            }
        }
        // Need to consume the token too
        p.clearCurrentToken();
        if (config.isEnabled(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)) {
            _verifyNoTrailingTokens(p, ctxt, valueType);
        }
        return result;
    }

    protected Object _readMapAndClose(DefaultDeserializationContext ctxt,
            JsonParser p0, JavaType valueType)
        throws IOException
    {
        ctxt.assignParser(p0);
        try (JsonParser p = p0) {
            Object result;
            JsonToken t = _initForReading(p, valueType);
            if (t == JsonToken.VALUE_NULL) {
                // Ask JsonDeserializer what 'null value' to use:
                result = _findRootDeserializer(ctxt, valueType).getNullValue(ctxt);
            } else if (t == JsonToken.END_ARRAY || t == JsonToken.END_OBJECT) {
                result = null;
            } else {
                final DeserializationConfig config = ctxt.getConfig();
                JsonDeserializer<Object> deser = _findRootDeserializer(ctxt, valueType);
                if (config.useRootWrapping()) {
                    result = _unwrapAndDeserialize(p, ctxt, config, valueType, deser);
                } else {
                    result = deser.deserialize(p, ctxt);
                }
                ctxt.checkUnresolvedObjectId();
            }
            if (ctxt.isEnabled(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)) {
                _verifyNoTrailingTokens(p, ctxt, valueType);
            }
            return result;
        }
    }

    /**
     * Similar to {@link #_readMapAndClose} but specialized for <code>JsonNode</code> reading.
     */
    protected JsonNode _readTreeAndClose(DeserializationContext ctxt,
            JsonParser p0) throws IOException
    {
        try (JsonParser p = p0) {
            final JavaType valueType = JSON_NODE_TYPE;
            DeserializationConfig cfg = deserializationConfig();

            // 27-Oct-2016, tatu: Need to inline `_initForReading()` due to
            //   special requirements by tree reading (no fail on eof)
            JsonToken t = p.currentToken();
            if (t == null) {
                t = p.nextToken();
                if (t == null) { // [databind#1406]: expose end-of-input as `null`
                    return null;
                }
            }
            if (t == JsonToken.VALUE_NULL) {
                return cfg.getNodeFactory().nullNode();
            }
            JsonDeserializer<Object> deser = _findRootDeserializer(ctxt, valueType);
            Object result;
            if (cfg.useRootWrapping()) {
                result = _unwrapAndDeserialize(p, ctxt, cfg, valueType, deser);
            } else {
                result = deser.deserialize(p, ctxt);
                if (cfg.isEnabled(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)) {
                    _verifyNoTrailingTokens(p, ctxt, valueType);
                }
            }
            // No ObjectIds so can ignore
//            ctxt.checkUnresolvedObjectId();
            return (JsonNode) result;
        }
    }

    protected Object _unwrapAndDeserialize(JsonParser p, DeserializationContext ctxt, 
            DeserializationConfig config,
            JavaType rootType, JsonDeserializer<Object> deser)
        throws IOException
    {
        PropertyName expRootName = config.findRootName(rootType);
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
        Object result = deser.deserialize(p, ctxt);
        // and last, verify that we now get matching END_OBJECT
        if (p.nextToken() != JsonToken.END_OBJECT) {
            ctxt.reportWrongTokenException(rootType, JsonToken.END_OBJECT,
                    "Current token not END_OBJECT (to match wrapper object with root name '%s'), but %s",
                    expSimpleName, p.currentToken());
        }
        if (config.isEnabled(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)) {
            _verifyNoTrailingTokens(p, ctxt, rootType);
        }
        return result;
    }

    /**
     * Internal helper method called to create an instance of {@link DeserializationContext}
     * for deserializing a single root value.
     * Can be overridden if a custom context is needed.
     */
    protected DefaultDeserializationContext createDeserializationContext(JsonParser p) {
        return _deserializationContext.createInstance(deserializationConfig(),
                /* FormatSchema */ null, _injectableValues)
                .assignParser(p);
    }

    protected DefaultDeserializationContext createDeserializationContext() {
        return _deserializationContext.createInstance(deserializationConfig(),
                /* FormatSchema */ null, _injectableValues);
    }

    protected DefaultDeserializationContext createDeserializationContext(DeserializationConfig config,
            JsonParser p) {
        return _deserializationContext.createInstance(config,
                /* FormatSchema */ null, _injectableValues)
                .assignParser(p);
    }
    
    /**
     * Method called to ensure that given parser is ready for reading
     * content for data binding.
     *
     * @return First token to be used for data binding after this call:
     *  can never be null as exception will be thrown if parser cannot
     *  provide more tokens.
     *
     * @throws IOException if the underlying input source has problems during
     *   parsing
     * @throws JsonParseException if parser has problems parsing content
     * @throws JsonMappingException if the parser does not have any more
     *   content to map (note: Json "null" value is considered content;
     *   enf-of-stream not)
     */
    protected JsonToken _initForReading(JsonParser p, JavaType targetType) throws IOException
    {
        // First: must point to a token; if not pointing to one, advance.
        // This occurs before first read from JsonParser, as well as
        // after clearing of current token.
        JsonToken t = p.currentToken();
        if (t == null) {
            // and then we must get something...
            t = p.nextToken();
            if (t == null) {
                // Throw mapping exception, since it's failure to map,
                //   not an actual parsing problem
                throw MismatchedInputException.from(p, targetType,
                        "No content to map due to end-of-input");
            }
        }
        return t;
    }

    protected final void _verifyNoTrailingTokens(JsonParser p, DeserializationContext ctxt,
            JavaType bindType)
        throws IOException
    {
        JsonToken t = p.nextToken();
        if (t != null) {
            Class<?> bt = ClassUtil.rawClass(bindType);
            ctxt.reportTrailingTokens(bt, p, t);
        }
    }

    /*
    /**********************************************************************
    /* Internal factory methods for ObjectReaders/-Writers
    /**********************************************************************
     */
    
    /**
     * Factory method sub-classes must override, to produce {@link ObjectReader}
     * instances of proper sub-type
     */
    protected ObjectReader _newReader(DeserializationConfig config) {
        return new ObjectReader(this, config);
    }

    /**
     * Factory method sub-classes must override, to produce {@link ObjectReader}
     * instances of proper sub-type
     */
    protected ObjectReader _newReader(DeserializationConfig config,
            JavaType valueType, Object valueToUpdate,
            FormatSchema schema, InjectableValues injectableValues) {
        return new ObjectReader(this, config, valueType, valueToUpdate, schema, injectableValues);
    }

    /**
     * Factory method sub-classes must override, to produce {@link ObjectWriter}
     * instances of proper sub-type
     */
    protected ObjectWriter _newWriter(SerializationConfig config) {
        return new ObjectWriter(this, config);
    }

    /**
     * Factory method sub-classes must override, to produce {@link ObjectWriter}
     * instances of proper sub-type
     */
    protected ObjectWriter _newWriter(SerializationConfig config, FormatSchema schema) {
        return new ObjectWriter(this, config, schema);
    }
    
    /**
     * Factory method sub-classes must override, to produce {@link ObjectWriter}
     * instances of proper sub-type
     */
    protected ObjectWriter _newWriter(SerializationConfig config,
            JavaType rootType, PrettyPrinter pp) {
        return new ObjectWriter(this, config, rootType, pp);
    }

    /*
    /**********************************************************************
    /* Internal methods, other
    /**********************************************************************
     */

    /**
     * Method called to locate deserializer for the passed root-level value.
     */
    protected JsonDeserializer<Object> _findRootDeserializer(DeserializationContext ctxt,
            JavaType valueType)
        throws JsonMappingException
    {
        // First: have we already seen it?
        JsonDeserializer<Object> deser = _rootDeserializers.get(valueType);
        if (deser != null) {
            return deser;
        }
        // Nope: need to ask provider to resolve it
        deser = ctxt.findRootValueDeserializer(valueType);
        if (deser == null) { // can this happen?
            return ctxt.reportBadDefinition(valueType,
                    "Cannot find a deserializer for type "+valueType);
        }
        _rootDeserializers.put(valueType, deser);
        return deser;
    }

    protected void _verifySchemaType(FormatSchema schema)
    {
        if (schema != null) {
            if (!_streamFactory.canUseSchema(schema)) {
                    throw new IllegalArgumentException("Cannot use FormatSchema of type "+schema.getClass().getName()
                            +" for format "+_streamFactory.getFormatName());
            }
        }
    }
}
