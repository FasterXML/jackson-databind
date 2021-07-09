package com.fasterxml.jackson.databind;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.file.Path;
import java.text.DateFormat;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.exc.WrappedIOException;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.io.SegmentedStringWriter;
import com.fasterxml.jackson.core.json.JsonFactory;
import com.fasterxml.jackson.core.type.ResolvedType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.*;

import com.fasterxml.jackson.databind.cfg.*;
import com.fasterxml.jackson.databind.deser.DeserializationContextExt;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.introspect.*;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.SubtypeResolver;
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
 * (producing instance with default configuration); or through one of two build
 * methods.
 * First build method is the static <code>builder()</code> on exact type
 * and second {@link #rebuild()} method on an existing mapper.
 * Former starts with default configuration (same as one that no-arguments constructor
 * created mapper has), and latter starts with configuration of the mapper it is called
 * on.
 * In both cases, after configuration (including addition of {@link JacksonModule}s) is complete,
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
    implements TreeCodec, Versioned,
        java.io.Serializable
{
    private static final long serialVersionUID = 3L;

    /*
    /**********************************************************************
    /* Helper classes, enums
    /**********************************************************************
     */

    /**
     * Base implementation for "Vanilla" {@link ObjectMapper}, only defined to support
     * backwards-compatibility with some of 2.x usage patterns.
     */
    private static class PrivateBuilder extends MapperBuilder<ObjectMapper, PrivateBuilder>
    {
        public PrivateBuilder(TokenStreamFactory tsf) {
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

        public PrivateBuilder(MapperBuilderState state) {
            super(state);
        }

     // We also need actual instance of state as base class can not implement logic
     // for reinstating mapper (via mapper builder) from state.
        static class StateImpl extends MapperBuilderState {
            private static final long serialVersionUID = 3L;

            public StateImpl(PrivateBuilder b) {
                super(b);
            }

            @Override
            protected Object readResolve() {
                return new PrivateBuilder(this).build();
            }
        }
    }

    /*
    /**********************************************************************
    /* Internal constants, singletons
    /**********************************************************************
     */
    
    // Quick little shortcut, to avoid having to use global TypeFactory instance...
    // 19-Oct-2015, tatu: Not sure if this is really safe to do; let's at least allow
    //   some amount of introspection
    private final static JavaType JSON_NODE_TYPE =
            SimpleType.constructUnsafe(JsonNode.class);
//            TypeFactory.defaultInstance().constructType(JsonNode.class);

    /*
    /**********************************************************************
    /* Configuration settings, shared
    /**********************************************************************
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

    /*
    /**********************************************************************
    /* Configuration settings, serialization
    /**********************************************************************
     */

    /**
     * Factory used for constructing per-call {@link SerializerProvider}s.
     *<p>
     * Note: while serializers are only exposed {@link SerializerProvider},
     * mappers and readers need to access additional API defined by
     * {@link SerializationContextExt}
     */
    protected final SerializationContexts _serializationContexts;

    /**
     * Configuration object that defines basic global
     * settings for the serialization process
     */
    protected final SerializationConfig _serializationConfig;

    /*
    /**********************************************************************
    /* Configuration settings, deserialization
    /**********************************************************************
     */

    /**
     * Factory used for constructing per-call {@link DeserializationContext}s.
     */
    protected final DeserializationContexts _deserializationContexts;

    /**
     * Configuration object that defines basic global
     * settings for the serialization process
     */
    protected final DeserializationConfig _deserializationConfig;

    /*
    /**********************************************************************
    /* Caching
    /**********************************************************************
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
    protected final ConcurrentHashMap<JavaType, ValueDeserializer<Object>> _rootDeserializers
        = new ConcurrentHashMap<JavaType, ValueDeserializer<Object>>(64, 0.6f, 2);

    /*
    /**********************************************************************
    /* Saved state to allow re-building
    /**********************************************************************
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
        this(new PrivateBuilder(new JsonFactory()));
    }

    /**
     * Constructs instance that uses specified {@link TokenStreamFactory}
     * for constructing necessary {@link JsonParser}s and/or
     * {@link JsonGenerator}s, but without registering additional modules.
     */
    public ObjectMapper(TokenStreamFactory streamFactory) {
        this(new PrivateBuilder(streamFactory));
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
        // First things first: finalize building process. Saved state
        // consists of snapshots and is safe to keep references to; used
        // for rebuild()ing mapper instances

        _savedBuilderState = builder.saveStateApplyModules();

        // But we will ALSO need to take snapshot of anything builder has,
        // in case caller keeps on tweaking with builder. So rules are the
        // as with above call, or when creating new builder for rebuild()ing
        
        // General framework factories
        _streamFactory = builder.streamFactory();

        final ConfigOverrides configOverrides;
        {
            // bit tricky as we do NOT want to expose simple accessors (to a mutable thing)
            final AtomicReference<ConfigOverrides> ref = new AtomicReference<>();
            builder.withAllConfigOverrides(overrides -> ref.set(overrides));
            configOverrides = Snapshottable.takeSnapshot(ref.get());
        }
        final CoercionConfigs coercionConfigs;
        {
            final AtomicReference<CoercionConfigs> ref = new AtomicReference<>();
            builder.withAllCoercionConfigs(overrides -> ref.set(overrides));
            coercionConfigs = Snapshottable.takeSnapshot(ref.get());
        }

        // Handlers, introspection
        _typeFactory =  Snapshottable.takeSnapshot(builder.typeFactory());
        ClassIntrospector classIntr = builder.classIntrospector().forMapper();
        SubtypeResolver subtypeResolver =  Snapshottable.takeSnapshot(builder.subtypeResolver());
        MixInHandler mixIns = (MixInHandler) Snapshottable.takeSnapshot(builder.mixInHandler());
        // NOTE: TypeResolverProvider apparently ok without snapshot, hence config objects fetch
        // it directly from MapperBuilder, not passed by us.

        // Serialization factories
        _serializationContexts = builder.serializationContexts()
                .forMapper(this, _streamFactory, builder.serializerFactory());

        // Deserialization factories

        _deserializationContexts = builder.deserializationContexts()
                .forMapper(this, _streamFactory, builder.deserializerFactory());
        _injectableValues = Snapshottable.takeSnapshot(builder.injectableValues());

        // And then finalize serialization/deserialization Config containers

        RootNameLookup rootNames = new RootNameLookup();
        FilterProvider filterProvider = Snapshottable.takeSnapshot(builder.filterProvider());
        _deserializationConfig = builder.buildDeserializationConfig(configOverrides,
                mixIns, _typeFactory, classIntr, subtypeResolver,
                rootNames, coercionConfigs);
        _serializationConfig = builder.buildSerializationConfig(configOverrides,
                mixIns, _typeFactory, classIntr, subtypeResolver,
                rootNames, filterProvider);
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
        return (MapperBuilder<M,B>) new PrivateBuilder(_savedBuilderState);
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
     * for changing {@link StreamReadFeature}
     * and {@link StreamWriteFeature}
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
    public JavaType constructType(Type type) {
        _assertNotNull("type", type);
        return _typeFactory.constructType(type);
    }

    /**
     * Convenience method for constructing {@link JavaType} out of given
     * type reference.
     */
    public JavaType constructType(TypeReference<?> typeReference) {
        _assertNotNull("typeReference", typeReference);
        return _typeFactory.constructType(typeReference);
    }

    /*
    /**********************************************************************
    /* Configuration, accessing features
    /**********************************************************************
     */

    public boolean isEnabled(TokenStreamFactory.Feature f) {
        return _streamFactory.isEnabled(f);
    }

    public boolean isEnabled(StreamReadFeature f) {
        return _deserializationConfig.isEnabled(f);
    }

    public boolean isEnabled(StreamWriteFeature f) {
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
    /* Configuration, accessing module information
    /**********************************************************************
     */

    /**
     * Method that may be used to find out {@link JacksonModule}s that were registered
     * when creating this mapper (if any).
     *
     * @since 3.0
     */
    public Collection<JacksonModule> getRegisteredModules() {
        return _savedBuilderState.modules();
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
        return ctxt.assignAndReturnParser(_streamFactory.createParser(ctxt, src));
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
    public JsonParser createParser(URL src) throws JacksonException {
        _assertNotNull("src", src);
        DeserializationContextExt ctxt = _deserializationContext();
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
    public JsonParser createParser(InputStream in) throws JacksonException {
        _assertNotNull("in", in);
        DeserializationContextExt ctxt = _deserializationContext();
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
    public JsonParser createParser(Reader r) throws JacksonException {
        _assertNotNull("r", r);
        DeserializationContextExt ctxt = _deserializationContext();
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
    public JsonParser createParser(byte[] content) throws JacksonException {
        _assertNotNull("content", content);
        DeserializationContextExt ctxt = _deserializationContext();
        return ctxt.assignAndReturnParser(_streamFactory.createParser(ctxt, content));
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
        return ctxt.assignAndReturnParser(_streamFactory.createParser(ctxt, content, offset, len));
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
    public JsonParser createParser(char[] content) throws JacksonException {
        _assertNotNull("content", content);
        DeserializationContextExt ctxt = _deserializationContext();
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
    public JsonParser createParser(char[] content, int offset, int len) throws JacksonException {
        _assertNotNull("content", content);
        DeserializationContextExt ctxt = _deserializationContext();
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
    public JsonParser createParser(DataInput content) throws JacksonException {
        _assertNotNull("content", content);
        DeserializationContextExt ctxt = _deserializationContext();
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
    public JsonParser createNonBlockingByteArrayParser() throws JacksonException {
        DeserializationContextExt ctxt = _deserializationContext();
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
    public JsonGenerator createGenerator(OutputStream out) throws JacksonException {
        _assertNotNull("out", out);
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
    public JsonGenerator createGenerator(OutputStream out, JsonEncoding enc) throws JacksonException {
        _assertNotNull("out", out);
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
    public JsonGenerator createGenerator(Writer w) throws JacksonException {
        _assertNotNull("w", w);
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
    public JsonGenerator createGenerator(File f, JsonEncoding enc) throws JacksonException {
        _assertNotNull("f", f);
        return _streamFactory.createGenerator(_serializerProvider(), f, enc);
    }

    /**
     * Factory method for constructing {@link JsonGenerator} that is properly
     * wired to allow callbacks for serialization: basically
     * constructs a {@link ObjectWriteContext} and then calls
     * {@link TokenStreamFactory#createGenerator(ObjectWriteContext,Path,JsonEncoding)}.
     *
     * @since 3.0
     */
    public JsonGenerator createGenerator(Path path, JsonEncoding enc) throws JacksonException {
        _assertNotNull("path", path);
        return _streamFactory.createGenerator(_serializerProvider(), path, enc);
    }

    /**
     * Factory method for constructing {@link JsonGenerator} that is properly
     * wired to allow callbacks for serialization: basically
     * constructs a {@link ObjectWriteContext} and then calls
     * {@link TokenStreamFactory#createGenerator(ObjectWriteContext,DataOutput)}.
     *
     * @since 3.0
     */
    public JsonGenerator createGenerator(DataOutput out) throws JacksonException {
        _assertNotNull("out", out);
        return _streamFactory.createGenerator(_serializerProvider(), out);
    }

    /*
    /**********************************************************************
    /* TreeCodec implementation
    /**********************************************************************
     */

    /**
     *<p>
     * Note: return type is co-variant, as basic ObjectCodec
     * abstraction cannot refer to concrete node types (as it's
     * part of core package, whereas impls are part of mapper
     * package)
     */
    @Override
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
    @Override
    public ArrayNode createArrayNode() {
        return _deserializationConfig.getNodeFactory().arrayNode();
    }

    @Override
    public JsonNode booleanNode(boolean b) {
        return _deserializationConfig.getNodeFactory().booleanNode(b);
    }

    @Override
    public JsonNode stringNode(String text) {
        return _deserializationConfig.getNodeFactory().textNode(text);
    }
    
    @Override
    public JsonNode missingNode() {
        return _deserializationConfig.getNodeFactory().missingNode();
    }

    @Override
    public JsonNode nullNode() {
        return _deserializationConfig.getNodeFactory().nullNode();
    }

    /**
     * Method for constructing a {@link JsonParser} out of JSON tree
     * representation.
     * 
     * @param n Root node of the tree that resulting parser will read from
     */
    @Override
    public JsonParser treeAsTokens(TreeNode n) {
        _assertNotNull("n", n);
        DeserializationContext ctxt = _deserializationContext();
        return new TreeTraversingParser((JsonNode) n, ctxt);
    }
    
    /**
     * Method to deserialize JSON content as a tree {@link JsonNode}.
     * Returns {@link JsonNode} that represents the root of the resulting tree, if there
     * was content to read, or {@code null} if no more content is accessible
     * via passed {@link JsonParser}.
     *<p>
     * NOTE! Behavior with end-of-input (no more content) differs between this
     * {@code readTree} method, and all other methods that take input source: latter
     * will return "missing node", NOT {@code null}
     * 
     * @return a {@link JsonNode}, if valid JSON content found; null
     *   if input has no content to bind -- note, however, that if
     *   JSON <code>null</code> token is found, it will be represented
     *   as a non-null {@link JsonNode} (one that returns <code>true</code>
     *   for {@link JsonNode#isNull()}
     * 
     * @throws WrappedIOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws StreamReadException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     */
    @SuppressWarnings("unchecked")
    @Override
    public JsonNode readTree(JsonParser p) throws JacksonException
    {
        _assertNotNull("p", p);
        // Must check for EOF here before calling readValue(), since that'll choke on it otherwise
        JsonToken t = p.currentToken();
        if (t == null) {
            t = p.nextToken();
            if (t == null) {
                return null;
            }
        }
        // NOTE! _readValue() will check for trailing tokens
        JsonNode n = (JsonNode) _readValue(_deserializationContext(p), p, JSON_NODE_TYPE);
        if (n == null) {
            n = getNodeFactory().nullNode();
        }
        return n;
    }

    @Override
    public void writeTree(JsonGenerator g, TreeNode rootNode) throws JacksonException
    {
        _assertNotNull("g", g);
        SerializationConfig config = serializationConfig();
        _serializerProvider(config).serializeValue(g, rootNode);
        if (config.isEnabled(SerializationFeature.FLUSH_AFTER_WRITE_VALUE)) {
            g.flush();
        }
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
     * @throws WrappedIOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws StreamReadException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws DatabindException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(JsonParser p, Class<T> valueType) throws JacksonException
    {
        _assertNotNull("p", p);
        return (T) _readValue(_deserializationContext(p), p, _typeFactory.constructType(valueType));
    } 

    /**
     * Method to deserialize JSON content into a Java type, reference
     * to which is passed as argument. Type is passed using so-called
     * "super type token" (see )
     * and specifically needs to be used if the root type is a 
     * parameterized (generic) container type.
     * 
     * @throws WrappedIOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws StreamReadException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws DatabindException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(JsonParser p, TypeReference<T> valueTypeRef) throws JacksonException
    {
        _assertNotNull("p", p);
        return (T) _readValue(_deserializationContext(p), p, _typeFactory.constructType(valueTypeRef));
    }

    /**
     * Method to deserialize JSON content into a Java type, reference
     * to which is passed as argument. Type is passed using 
     * Jackson specific type; instance of which can be constructed using
     * {@link TypeFactory}.
     * 
     * @throws WrappedIOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws StreamReadException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws DatabindException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     */
    @SuppressWarnings("unchecked")
    public final <T> T readValue(JsonParser p, ResolvedType valueType) throws JacksonException
    {
        _assertNotNull("p", p);
        return (T) _readValue(_deserializationContext(p), p, (JavaType) valueType);
    }

    /**
     * Type-safe overloaded method, basically alias for {@link #readValue(JsonParser, Class)}.
     * 
     * @throws WrappedIOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws StreamReadException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws DatabindException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(JsonParser p, JavaType valueType) throws JacksonException
    {
        _assertNotNull("p", p);
        return (T) _readValue(_deserializationContext(p), p, valueType);
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
        throws JacksonException
    {
        _assertNotNull("p", p);
        DeserializationContext ctxt = _deserializationContext(p);
        ValueDeserializer<?> deser = _findRootDeserializer(ctxt, valueType);
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
        throws JacksonException
    {
        _assertNotNull("p", p);
        return readValues(p, _typeFactory.constructType(valueType));
    }

    // Used by Kotlin module
    public <T> MappingIterator<T> readValues(JsonParser p, TypeReference<T> valueType)
        throws JacksonException
    {
        _assertNotNull("p", p);
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
     * {@link StreamReadException} will be thrown.
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
     * @throws StreamReadException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     */
    public JsonNode readTree(InputStream in) throws JacksonException
    {
        _assertNotNull("in", in);
        DeserializationContextExt ctxt = _deserializationContext();
        return _readTreeAndClose(ctxt, _streamFactory.createParser(ctxt, in));
    }

    /**
     * Same as {@link #readTree(InputStream)} except content accessed through
     * passed-in {@link Reader}
     */
    public JsonNode readTree(Reader r) throws JacksonException {
        _assertNotNull("r", r);
        DeserializationContextExt ctxt = _deserializationContext();
        return _readTreeAndClose(ctxt, _streamFactory.createParser(ctxt, r));
    }

    /**
     * Same as {@link #readTree(InputStream)} except content read from
     * passed-in {@link String}
     */
    public JsonNode readTree(String content) throws JacksonException {
        _assertNotNull("content", content);
        DeserializationContextExt ctxt = _deserializationContext();
        return _readTreeAndClose(ctxt, _streamFactory.createParser(ctxt, content));
    }

    /**
     * Same as {@link #readTree(InputStream)} except content read from
     * passed-in byte array.
     */
    public JsonNode readTree(byte[] content) throws JacksonException {
        _assertNotNull("content", content);
        DeserializationContextExt ctxt = _deserializationContext();
        return _readTreeAndClose(ctxt, _streamFactory.createParser(ctxt, content));
    }

    /**
     * Same as {@link #readTree(InputStream)} except content read from
     * passed-in byte array.
     */
    public JsonNode readTree(byte[] content, int offset, int len) throws JacksonException {
        _assertNotNull("content", content);
        DeserializationContextExt ctxt = _deserializationContext();
        return _readTreeAndClose(ctxt, _streamFactory.createParser(ctxt, content, offset, len));
    }

    /**
     * Same as {@link #readTree(InputStream)} except content read from
     * passed-in {@link File}.
     */
    public JsonNode readTree(File file) throws JacksonException
    {
        _assertNotNull("file", file);
        DeserializationContextExt ctxt = _deserializationContext();
        return _readTreeAndClose(ctxt, _streamFactory.createParser(ctxt, file));
    }

    /**
     * Same as {@link #readTree(InputStream)} except content read from
     * passed-in {@link Path}.
     *
     * @since 3.0
     */
    public JsonNode readTree(Path path) throws JacksonException
    {
        _assertNotNull("path", path);
        DeserializationContextExt ctxt = _deserializationContext();
        return _readTreeAndClose(ctxt, _streamFactory.createParser(ctxt, path));
    }

    /**
     * Same as {@link #readTree(InputStream)} except content read from
     * passed-in {@link URL}.
     *<p>
     * NOTE: handling of {@link java.net.URL} is delegated to
     * {@link TokenStreamFactory#createParser(ObjectReadContext, java.net.URL)}s and usually simply
     * calls {@link java.net.URL#openStream()}, meaning no special handling
     * is done. If different HTTP connection options are needed you will need
     * to create {@link java.io.InputStream} separately.
     */
    public JsonNode readTree(URL src) throws JacksonException {
        _assertNotNull("src", src);
        DeserializationContextExt ctxt = _deserializationContext();
        return _readTreeAndClose(ctxt, _streamFactory.createParser(ctxt, src));
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
    public void writeValue(JsonGenerator g, Object value) throws JacksonException
    {
        _assertNotNull("g", g);
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
    /**********************************************************************
    /* Public API: Additional Tree Model support beyond TreeCodec
    /**********************************************************************
     */

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
        throws JacksonException
    {
        if (n == null) {
            return null;
        }
        // 25-Jan-2019, tatu: [databind#2220] won't prevent existing coercions here
        // Simple cast when we just want to cast to, say, ObjectNode
        if (TreeNode.class.isAssignableFrom(valueType)
                && valueType.isAssignableFrom(n.getClass())) {
            return (T) n;
        }
        final JsonToken tt = n.asToken();
        // 20-Apr-2016, tatu: Another thing: for VALUE_EMBEDDED_OBJECT, assume similar
        //    short-cut coercion
        if (tt == JsonToken.VALUE_EMBEDDED_OBJECT) {
            if (n instanceof POJONode) {
                Object ob = ((POJONode) n).getPojo();
                if ((ob == null) || valueType.isInstance(ob)) {
                    return (T) ob;
                }
            }
        }
        // 22-Aug-2019, tatu: [databind#2430] Consider "null node" (minor optimization)
        // 08-Dec-2020, tatu: Alas, lead to [databind#2972], optimization gets complicated
        //    so leave out for now...
        /*if (tt == JsonToken.VALUE_NULL) {
             return null;
        }*/
        return readValue(treeAsTokens(n), valueType);
    }

    /**
     * Method that is reverse of {@link #treeToValue}: it
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
     * raw value ({@link com.fasterxml.jackson.databind.util.RawValue}) or basic
     * node identity ({@link JsonNode}). If so, result is a valid tree, but values
     * are not re-constructed through actual format representation. So if transformation
     * requires actual materialization of encoded content,
     * it will be necessary to do actual serialization.
     * 
     * @param <T> Actual node type; usually either basic {@link JsonNode} or
     *  {@link com.fasterxml.jackson.databind.node.ObjectNode}
     * @param fromValue Java value to convert
     *
     * @return (non-null) Root node of the resulting content tree: in case of
     *   {@code null} value node for which {@link JsonNode#isNull()} returns {@code true}.
     */
    public <T extends JsonNode> T valueToTree(Object fromValue)
        throws JacksonException
    {
        // 02-Mar-2021, tatu: [databind#2411] Rewrite "valueToTree()" impl; old
        //   impl left for reference
        return _serializerProvider().valueToTree(fromValue);
    }

    /*
    /**********************************************************************
    /* Public API, deserialization (ext format to Java Objects)
    /**********************************************************************
     */

    /**
     * Method to deserialize JSON content from given file into given Java type.
     * 
     * @throws WrappedIOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws StreamReadException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws DatabindException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(File src, Class<T> valueType) throws JacksonException
    {
        _assertNotNull("src", src);
        DeserializationContextExt ctxt = _deserializationContext();
        return (T) _readMapAndClose(ctxt, _streamFactory.createParser(ctxt, src),
                _typeFactory.constructType(valueType));
    } 

    /**
     * Method to deserialize JSON content from given file into given Java type.
     * 
     * @throws WrappedIOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws StreamReadException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws DatabindException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     */
    @SuppressWarnings({ "unchecked" })
    public <T> T readValue(File src, TypeReference<T> valueTypeRef) throws JacksonException
    {
        _assertNotNull("src", src);
        DeserializationContextExt ctxt = _deserializationContext();
        return (T) _readMapAndClose(ctxt, _streamFactory.createParser(ctxt, src),
                _typeFactory.constructType(valueTypeRef));
    } 

    /**
     * Method to deserialize JSON content from given file into given Java type.
     * 
     * @throws WrappedIOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws StreamReadException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws DatabindException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(File src, JavaType valueType) throws JacksonException
    {
        _assertNotNull("src", src);
        DeserializationContextExt ctxt = _deserializationContext();
        return (T) _readMapAndClose(ctxt, _streamFactory.createParser(ctxt, src), valueType);
    }

    /**
     * Method to deserialize JSON content from given path into given Java type.
     *
     * @throws WrappedIOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws StreamReadException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws DatabindException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     *
     * @since 3.0
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(Path src, Class<T> valueType) throws JacksonException
    {
        _assertNotNull("src", src);
        DeserializationContextExt ctxt = _deserializationContext();
        return (T) _readMapAndClose(ctxt, _streamFactory.createParser(ctxt, src),
                _typeFactory.constructType(valueType));
    }

    /**
     * Method to deserialize JSON content from given path into given Java type.
     *
     * @throws WrappedIOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws StreamReadException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws DatabindException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     *
     * @since 3.0
     */
    @SuppressWarnings({ "unchecked" })
    public <T> T readValue(Path src, TypeReference<T> valueTypeRef) throws JacksonException
    {
        _assertNotNull("src", src);
        DeserializationContextExt ctxt = _deserializationContext();
        return (T) _readMapAndClose(ctxt, _streamFactory.createParser(ctxt, src),
                _typeFactory.constructType(valueTypeRef));
    }

    /**
     * Method to deserialize JSON content from given path into given Java type.
     *
     * @throws WrappedIOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws StreamReadException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws DatabindException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     *
     * @since 3.0
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(Path src, JavaType valueType) throws JacksonException
    {
        _assertNotNull("src", src);
        DeserializationContextExt ctxt = _deserializationContext();
        return (T) _readMapAndClose(ctxt, _streamFactory.createParser(ctxt, src), valueType);
    }

    /**
     * Method to deserialize JSON content from given resource into given Java type.
     *<p>
     * NOTE: handling of {@link java.net.URL} is delegated to
     * {@link TokenStreamFactory#createParser(ObjectReadContext, java.net.URL)} and usually simply
     * calls {@link java.net.URL#openStream()}, meaning no special handling
     * is done. If different HTTP connection options are needed you will need
     * to create {@link java.io.InputStream} separately.
     * 
     * @throws WrappedIOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws StreamReadException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws DatabindException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(URL src, Class<T> valueType) throws JacksonException
    {
        _assertNotNull("src", src);
        DeserializationContextExt ctxt = _deserializationContext();
        return (T) _readMapAndClose(ctxt,
                _streamFactory.createParser(ctxt, src), _typeFactory.constructType(valueType));
    } 

    /**
     * Same as {@link #readValue(java.net.URL, Class)} except that target specified by {@link TypeReference}.
     */
    @SuppressWarnings({ "unchecked" })
    public <T> T readValue(URL src, TypeReference<T> valueTypeRef) throws JacksonException
    {
        _assertNotNull("src", src);
        DeserializationContextExt ctxt = _deserializationContext();
        return (T) _readMapAndClose(ctxt,
                _streamFactory.createParser(ctxt, src), _typeFactory.constructType(valueTypeRef));
    } 

    /**
     * Same as {@link #readValue(java.net.URL, Class)} except that target specified by {@link JavaType}.
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(URL src, JavaType valueType) throws JacksonException
    {
        _assertNotNull("src", src);
        DeserializationContextExt ctxt = _deserializationContext();
        return (T) _readMapAndClose(ctxt,
                _streamFactory.createParser(ctxt, src), valueType);
    } 

    /**
     * Method to deserialize JSON content from given JSON content String.
     * 
     * @throws WrappedIOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws StreamReadException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws DatabindException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(String content, Class<T> valueType)
        throws JacksonException
    {
        _assertNotNull("content", content);
        DeserializationContextExt ctxt = _deserializationContext();
        return (T) _readMapAndClose(ctxt,
                _streamFactory.createParser(ctxt, content), _typeFactory.constructType(valueType));
    } 

    /**
     * Method to deserialize JSON content from given JSON content String.
     * 
     * @throws WrappedIOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws StreamReadException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws DatabindException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     */
    @SuppressWarnings({ "unchecked" })
    public <T> T readValue(String content, TypeReference<T> valueTypeRef) throws JacksonException
    {
        _assertNotNull("content", content);
        DeserializationContextExt ctxt = _deserializationContext();
        return (T) _readMapAndClose(ctxt,
                _streamFactory.createParser(ctxt, content), _typeFactory.constructType(valueTypeRef));
    } 

    /**
     * Method to deserialize JSON content from given JSON content String.
     * 
     * @throws WrappedIOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws StreamReadException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws DatabindException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(String content, JavaType valueType) throws JacksonException
    {
        _assertNotNull("content", content);
        DeserializationContextExt ctxt = _deserializationContext();
        return (T) _readMapAndClose(ctxt,
                _streamFactory.createParser(ctxt, content), valueType);
    }

    @SuppressWarnings("unchecked")
    public <T> T readValue(Reader src, Class<T> valueType) throws JacksonException
    {
        _assertNotNull("src", src);
        DeserializationContextExt ctxt = _deserializationContext();
        return (T) _readMapAndClose(ctxt,
                _streamFactory.createParser(ctxt, src), _typeFactory.constructType(valueType));
    }

    @SuppressWarnings({ "unchecked" })
    public <T> T readValue(Reader src, TypeReference<T> valueTypeRef) throws JacksonException
    {
        _assertNotNull("src", src);
        DeserializationContextExt ctxt = _deserializationContext();
        return (T) _readMapAndClose(ctxt,
                _streamFactory.createParser(ctxt, src), _typeFactory.constructType(valueTypeRef));
    }

    @SuppressWarnings("unchecked")
    public <T> T readValue(Reader src, JavaType valueType) throws JacksonException
    {
        _assertNotNull("src", src);
        DeserializationContextExt ctxt = _deserializationContext();
        return (T) _readMapAndClose(ctxt,
                _streamFactory.createParser(ctxt, src), valueType);
    }

    @SuppressWarnings("unchecked")
    public <T> T readValue(InputStream src, Class<T> valueType) throws JacksonException
    {
        _assertNotNull("src", src);
        DeserializationContextExt ctxt = _deserializationContext();
        return (T) _readMapAndClose(ctxt,
                _streamFactory.createParser(ctxt, src), _typeFactory.constructType(valueType));
    }

    @SuppressWarnings({ "unchecked" })
    public <T> T readValue(InputStream src, TypeReference<T> valueTypeRef) throws JacksonException
    {
        _assertNotNull("src", src);
        DeserializationContextExt ctxt = _deserializationContext();
        return (T) _readMapAndClose(ctxt,
                _streamFactory.createParser(ctxt, src), _typeFactory.constructType(valueTypeRef));
    }

    @SuppressWarnings("unchecked")
    public <T> T readValue(InputStream src, JavaType valueType) throws JacksonException
    {
        _assertNotNull("src", src);
        DeserializationContextExt ctxt = _deserializationContext();
        return (T) _readMapAndClose(ctxt,
                _streamFactory.createParser(ctxt, src), valueType);
    }

    @SuppressWarnings("unchecked")
    public <T> T readValue(byte[] content, Class<T> valueType) throws JacksonException
    {
        _assertNotNull("content", content);
        DeserializationContextExt ctxt = _deserializationContext();
        return (T) _readMapAndClose(ctxt,
                _streamFactory.createParser(ctxt, content), _typeFactory.constructType(valueType));
    }

    @SuppressWarnings("unchecked")
    public <T> T readValue(byte[] content, int offset, int len, Class<T> valueType)
        throws JacksonException
    {
        _assertNotNull("content", content);
        DeserializationContextExt ctxt = _deserializationContext();
        return (T) _readMapAndClose(ctxt,
                _streamFactory.createParser(ctxt, content, offset, len), _typeFactory.constructType(valueType));
    }

    @SuppressWarnings({ "unchecked" })
    public <T> T readValue(byte[] content, TypeReference<T> valueTypeRef) throws JacksonException
    {
        _assertNotNull("content", content);
        DeserializationContextExt ctxt = _deserializationContext();
        return (T) _readMapAndClose(ctxt,
                _streamFactory.createParser(ctxt, content), _typeFactory.constructType(valueTypeRef));
    }

    @SuppressWarnings({ "unchecked" })
    public <T> T readValue(byte[] content, int offset, int len, TypeReference<T> valueTypeRef)
        throws JacksonException
    {
        _assertNotNull("content", content);
        DeserializationContextExt ctxt = _deserializationContext();
        return (T) _readMapAndClose(ctxt,
                _streamFactory.createParser(ctxt, content, offset, len),
                _typeFactory.constructType(valueTypeRef));
    }

    @SuppressWarnings("unchecked")
    public <T> T readValue(byte[] content, JavaType valueType) throws JacksonException
    {
        _assertNotNull("content", content);
        DeserializationContextExt ctxt = _deserializationContext();
        return (T) _readMapAndClose(ctxt,
                _streamFactory.createParser(ctxt, content), valueType);
    }

    @SuppressWarnings("unchecked")
    public <T> T readValue(byte[] content, int offset, int len, JavaType valueType)
                    throws JacksonException
    {
        _assertNotNull("content", content);
        DeserializationContextExt ctxt = _deserializationContext();
        return (T) _readMapAndClose(ctxt,
                _streamFactory.createParser(ctxt, content, offset, len), valueType);
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(DataInput src, Class<T> valueType) throws JacksonException
    {
        _assertNotNull("src", src);
        DeserializationContextExt ctxt = _deserializationContext();
        return (T) _readMapAndClose(ctxt,
                _streamFactory.createParser(ctxt, src), _typeFactory.constructType(valueType));
    }

    @SuppressWarnings("unchecked")
    public <T> T readValue(DataInput src, JavaType valueType) throws JacksonException
    {
        _assertNotNull("src", src);
        DeserializationContextExt ctxt = _deserializationContext();
        return (T) _readMapAndClose(ctxt,
                _streamFactory.createParser(ctxt, src), valueType);
    }

    @SuppressWarnings("unchecked")
    public <T> T readValue(DataInput src, TypeReference<T> valueTypeRef) throws JacksonException
    {
        _assertNotNull("src", src);
        DeserializationContextExt ctxt = _deserializationContext();
        return (T) _readMapAndClose(ctxt,
                _streamFactory.createParser(ctxt, src), _typeFactory.constructType(valueTypeRef));
    }

    /*
    /**********************************************************************
    /* Public API: serialization (mapping from Java types to external format)
    /**********************************************************************
     */

    /**
     * Method that can be used to serialize any Java value as
     * JSON output, written to File provided.
     */
    public void writeValue(File file, Object value) throws JacksonException
    {
        _assertNotNull("file", file);
        SerializationContextExt prov = _serializerProvider();
        _configAndWriteValue(prov,
                _streamFactory.createGenerator(prov, file, JsonEncoding.UTF8), value);
    }

    /**
     * Method that can be used to serialize any Java value as
     * JSON output, written to Path provided.
     *
     * @since 3.0
     */
    public void writeValue(Path path, Object value) throws JacksonException
    {
        _assertNotNull("path", path);
        SerializationContextExt prov = _serializerProvider();
        _configAndWriteValue(prov,
                _streamFactory.createGenerator(prov, path, JsonEncoding.UTF8), value);
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
    public void writeValue(OutputStream out, Object value) throws JacksonException
    {
        _assertNotNull("out", out);
        SerializationContextExt prov = _serializerProvider();
        _configAndWriteValue(prov,
                _streamFactory.createGenerator(prov, out, JsonEncoding.UTF8), value);
    }

    public void writeValue(DataOutput out, Object value) throws JacksonException
    {
        _assertNotNull("out", out);
        SerializationContextExt prov = _serializerProvider();
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
    public void writeValue(Writer w, Object value) throws JacksonException
    {
        _assertNotNull("w", w);
        SerializationContextExt prov = _serializerProvider();
        _configAndWriteValue(prov, _streamFactory.createGenerator(prov, w), value);
    }

    /**
     * Method that can be used to serialize any Java value as
     * a String. Functionally equivalent to calling
     * {@link #writeValue(Writer,Object)} with {@link java.io.StringWriter}
     * and constructing String, but more efficient.
     */
    @SuppressWarnings("resource")
    public String writeValueAsString(Object value) throws JacksonException
    {
        // alas, we have to pull the recycler directly here...
        SegmentedStringWriter sw = new SegmentedStringWriter(_streamFactory._getBufferRecycler());
        SerializationContextExt prov = _serializerProvider();
        _configAndWriteValue(prov, _streamFactory.createGenerator(prov, sw), value);
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
    public byte[] writeValueAsBytes(Object value) throws JacksonException
    {
        // Although 'close()' is NOP, use auto-close to avoid lgtm complaints
        try (ByteArrayBuilder bb = new ByteArrayBuilder(_streamFactory._getBufferRecycler())) {
            final SerializationContextExt ctxt = _serializerProvider();
            _configAndWriteValue(ctxt,
                    _streamFactory.createGenerator(ctxt, bb, JsonEncoding.UTF8), value);
            byte[] result = bb.toByteArray();
            bb.release();
            return result;
        }
    }

    /**
     * Method called to configure the generator as necessary and then
     * call write functionality
     */
    protected final void _configAndWriteValue(SerializationContextExt prov,
            JsonGenerator g, Object value)
        throws JacksonException
    {
        if (prov.isEnabled(SerializationFeature.CLOSE_CLOSEABLE) && (value instanceof Closeable)) {
            _configAndWriteCloseable(prov, g, value);
            return;
        }
        try {
            prov.serializeValue(g, value);
        } catch (Exception e) {
            ClassUtil.closeOnFailAndThrowAsJacksonE(g, e);
            return;
        }
        g.close();
    }

    /**
     * Helper method used when value to serialize is {@link Closeable} and its <code>close()</code>
     * method is to be called right after serialization has been called
     */
    private final void _configAndWriteCloseable(SerializationContextExt prov,
            JsonGenerator g, Object value)
        throws JacksonException
    {
        Closeable toClose = (Closeable) value;
        try {
            prov.serializeValue(g, value);
            Closeable tmpToClose = toClose;
            toClose = null;
            tmpToClose.close();
        } catch (Exception e) {
            ClassUtil.closeOnFailAndThrowAsJacksonE(g, toClose, e);
            return;
        }
        g.close();
    }

    /**
     * Helper method used when value to serialize is {@link Closeable} and its <code>close()</code>
     * method is to be called right after serialization has been called
     */
    protected final void _writeCloseableValue(JsonGenerator g, Object value, SerializationConfig cfg)
        throws JacksonException
    {
        Closeable toClose = (Closeable) value;
        try {
            _serializerProvider(cfg).serializeValue(g, value);
            if (cfg.isEnabled(SerializationFeature.FLUSH_AFTER_WRITE_VALUE)) {
                g.flush();
            }
        } catch (Exception e) {
            ClassUtil.closeOnFailAndThrowAsJacksonE(null, toClose, e);
            return;
        }
        try {
            toClose.close();
        } catch (IOException e) {
            throw WrappedIOException.construct(e);
        }
    }

    /*
    /**********************************************************************
    /* Public API: constructing ObjectWriters
    /* for more advanced configuration
    /**********************************************************************
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
    /**********************************************************************
    /* Extended Public API: constructing ObjectReaders
    /* for more advanced configuration
    /**********************************************************************
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
     * read values of a type {@code List<type>}.
     * Functionally same as:
     *<pre>
     *    readerFor(type[].class);
     *</pre>
     *
     * @since 2.11
     */
    public ObjectReader readerForArrayOf(Class<?> type) {
        return _newReader(deserializationConfig(),
                _typeFactory.constructArrayType(type), null,
                null, _injectableValues);
    }

    /**
     * Factory method for constructing {@link ObjectReader} that will
     * read or update instances of a type {@code List<type>}.
     * Functionally same as:
     *<pre>
     *    readerFor(new TypeReference&lt;List&lt;type&gt;&gt;() { });
     *</pre>
     *
     * @since 2.11
     */
    public ObjectReader readerForListOf(Class<?> type) {
        return _newReader(deserializationConfig(),
                _typeFactory.constructCollectionType(List.class, type), null,
                null, _injectableValues);
    }

    /**
     * Factory method for constructing {@link ObjectReader} that will
     * read or update instances of a type {@code Map<String, type>}
     * Functionally same as:
     *<pre>
     *    readerFor(new TypeReference&lt;Map&lt;String, type&gt;&gt;() { });
     *</pre>
     *
     * @since 2.11
     */
    public ObjectReader readerForMapOf(Class<?> type) {
        return _newReader(deserializationConfig(),
                _typeFactory.constructMapType(Map.class, String.class, type), null,
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
    /**********************************************************************
    /* Extended Public API: convenience type conversion
    /**********************************************************************
     */

    /**
     * Convenience method for doing two-step conversion from given value, into
     * instance of given value type, by writing value into temporary buffer
     * and reading from the buffer into specified target type.
     *<p>
     * This method is functionally similar to first
     * serializing given value into JSON, and then binding JSON data into value
     * of given type, but should be more efficient since full serialization does
     * not (need to) occur.
     * However, same converters (serializers, deserializers) will be used as for
     * data binding, meaning same object mapper configuration works.
     *<p>
     * Note that behavior changed slightly between Jackson 2.9 and 2.10 so that
     * whereas earlier some optimizations were used to avoid write/read cycle
     * in case input was of target type, from 2.10 onwards full processing is
     * always performed. See
     * <a href="https://github.com/FasterXML/jackson-databind/issues/2220">databind#2220</a>
     * for full details of the change.
     *<p>
     * Further note that it is possible that in some cases behavior does differ
     * from full serialize-then-deserialize cycle: in most case differences are
     * unintentional (that is, flaws to fix) and should be reported, but
     * the behavior is not guaranteed to be 100% the same:
     * the goal is to allow efficient value conversions for structurally
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
        _assertNotNull("toValueType", toValueType);
        return (T) _convert(fromValue, _typeFactory.constructType(toValueType));
    } 

    /**
     * See {@link #convertValue(Object, Class)}
     */
    @SuppressWarnings("unchecked")
    public <T> T convertValue(Object fromValue, TypeReference<T> toValueTypeRef)
        throws IllegalArgumentException
    {
        _assertNotNull("toValueTypeRef", toValueTypeRef);
        return (T) _convert(fromValue, _typeFactory.constructType(toValueTypeRef));
    } 

    /**
     * See {@link #convertValue(Object, Class)}
     */
    @SuppressWarnings("unchecked")
    public <T> T convertValue(Object fromValue, JavaType toValueType)
        throws IllegalArgumentException
    {
        _assertNotNull("toValueType", toValueType);
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
        throws JacksonException
    {
        // 25-Jan-2019, tatu: [databind#2220] Let's NOT try to short-circuit anything

        // inlined 'writeValue' with minor changes:
        // first: disable wrapping when writing
        final SerializationConfig config = serializationConfig()
                .without(SerializationFeature.WRAP_ROOT_VALUE);
        final SerializationContextExt ctxt = _serializerProvider(config);
        TokenBuffer buf = ctxt.bufferForValueConversion();
        // Would like to let buffer decide, but it won't have deser config to check so...
        if (isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)) {
            buf = buf.forceUseOfBigDecimal(true);
        }
        ctxt.serializeValue(buf, fromValue);

        // then matching read, inlined 'readValue' with minor mods:
        DeserializationContextExt readCtxt = _deserializationContext();
        try (final JsonParser p = buf.asParser(readCtxt)) {
            readCtxt.assignParser(p);
            Object result;
            // ok to pass in existing feature flags; unwrapping handled by mapper
            JsonToken t = _initForReading(p, toValueType);
            if (t == JsonToken.VALUE_NULL) {
                result = _findRootDeserializer(readCtxt, toValueType).getNullValue(readCtxt);
            } else if (t == JsonToken.END_ARRAY || t == JsonToken.END_OBJECT) {
                result = null;
            } else { // pointing to event other than null
                ValueDeserializer<Object> deser = _findRootDeserializer(readCtxt, toValueType);
                // note: no handling of unwrapping
                result = deser.deserialize(p, readCtxt);
            }
            return result;
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
     * <li>Construct {@link JsonParser} (using {@link TokenBuffer#asParser(ObjectReadContext)})
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
     * @throws JacksonException if there are structural incompatibilities that prevent update.
     */
    @SuppressWarnings("resource")
    public <T> T updateValue(T valueToUpdate, Object overrides)
        throws JacksonException
    {
        if ((valueToUpdate == null) || (overrides == null)) {
            return valueToUpdate;
        }
        SerializationConfig config = serializationConfig()
                .without(SerializationFeature.WRAP_ROOT_VALUE);
        SerializationContextExt ctxt = _serializerProvider(config);
        TokenBuffer buf = ctxt.bufferForValueConversion();
        // Would like to let buffer decide, but it won't have deser config to check so...
        if (isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)) {
            buf = buf.forceUseOfBigDecimal(true);
        }
        ctxt.serializeValue(buf, overrides);
        // 11-Apr-2019, tatu: Should we create "real" DeserializationContext or is this ok?
        try (JsonParser p = buf.asParser(ObjectReadContext.empty())) {
            return readerForUpdating(valueToUpdate).readValue(p);
        }
    }

    /*
    /**********************************************************************
    /* Extended Public API: JSON Schema generation
    /**********************************************************************
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
    {
        _assertNotNull("type", type);
        _assertNotNull("visitor", visitor);
        acceptJsonFormatVisitor(_typeFactory.constructType(type), visitor);
    }

    public void acceptJsonFormatVisitor(TypeReference<?> typeRef, JsonFormatVisitorWrapper visitor)
    {
        _assertNotNull("typeRef", typeRef);
        _assertNotNull("visitor", visitor);
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
    {
        _assertNotNull("type", type);
        _assertNotNull("visitor", visitor);
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
    protected SerializationContextExt _serializerProvider(SerializationConfig config) {
        // 03-Oct-2017, tatu: Should be ok to pass "empty" generator settings...
        return _serializationContexts.createContext(config,
                GeneratorSettings.empty());
    }

    // NOTE: only public to allow for testing
    public SerializationContextExt _serializerProvider() {
        // 03-Oct-2017, tatu: Should be ok to pass "empty" generator settings...
        return _serializationContexts.createContext(serializationConfig(),
                GeneratorSettings.empty());
    }

    /*
    /**********************************************************************
    /* Internal methods for deserialization, overridable
    /**********************************************************************
     */

    /**
     * Actual implementation of value reading+binding operation.
     */
    protected Object _readValue(DeserializationContextExt ctxt, JsonParser p,
            JavaType valueType)
        throws JacksonException
    {
        // First: may need to read the next token, to initialize
        // state (either before first read from parser, or after
        // previous token has been cleared)
        final Object result;
        JsonToken t = _initForReading(p, valueType);

        if (t == JsonToken.VALUE_NULL) {
            // Ask deserializer what 'null value' to use:
            result = _findRootDeserializer(ctxt, valueType).getNullValue(ctxt);
        } else if (t == JsonToken.END_ARRAY || t == JsonToken.END_OBJECT) {
            result = null;
        } else { // pointing to event other than null
            result = ctxt.readRootValue(p, valueType, _findRootDeserializer(ctxt, valueType), null);
        }
        // Need to consume the token too
        p.clearCurrentToken();
        if (ctxt.isEnabled(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)) {
            _verifyNoTrailingTokens(p, ctxt, valueType);
        }
        return result;
    }

    protected Object _readMapAndClose(DeserializationContextExt ctxt,
            JsonParser p0, JavaType valueType)
        throws JacksonException
    {
        ctxt.assignParser(p0);
        try (JsonParser p = p0) {
            Object result;
            JsonToken t = _initForReading(p, valueType);
            if (t == JsonToken.VALUE_NULL) {
                // Ask deserializer what 'null value' to use:
                result = _findRootDeserializer(ctxt, valueType).getNullValue(ctxt);
            } else if (t == JsonToken.END_ARRAY || t == JsonToken.END_OBJECT) {
                result = null;
            } else {
                result = ctxt.readRootValue(p, valueType,
                        _findRootDeserializer(ctxt, valueType), null);
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
    protected JsonNode _readTreeAndClose(DeserializationContextExt ctxt,
            JsonParser p0) throws JacksonException
    {
        try (JsonParser p = ctxt.assignAndReturnParser(p0)) {
            
            final JavaType valueType = JSON_NODE_TYPE;
            DeserializationConfig cfg = deserializationConfig();

            // 27-Oct-2016, tatu: Need to inline `_initForReading()` due to
            //   special requirements by tree reading (no fail on eof)
            JsonToken t = p.currentToken();
            if (t == null) {
                t = p.nextToken();
                if (t == null) {
                    // [databind#2211]: return `MissingNode` (supercedes [databind#1406] which dictated
                    // returning `null`
                    return cfg.getNodeFactory().missingNode();
                }
            }
            final JsonNode resultNode;
            if (t == JsonToken.VALUE_NULL) {
                resultNode = cfg.getNodeFactory().nullNode();
            } else {
                resultNode = (JsonNode) ctxt.readRootValue(p, valueType,
                        _findRootDeserializer(ctxt, valueType), null);
                // No ObjectIds so can ignore
//              ctxt.checkUnresolvedObjectId();
            }
            if (cfg.isEnabled(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)) {
                _verifyNoTrailingTokens(p, ctxt, valueType);
            }
            return resultNode;
        }
    }

    /**
     * Internal helper method called to create an instance of {@link DeserializationContext}
     * for deserializing a single root value.
     * Can be overridden if a custom context is needed.
     */
    protected DeserializationContextExt _deserializationContext(JsonParser p) {
        return _deserializationContexts.createContext(deserializationConfig(),
                /* FormatSchema */ null, _injectableValues)
                .assignParser(p);
    }

    // NOTE: only public to allow for testing
    public DeserializationContextExt _deserializationContext() {
        return _deserializationContexts.createContext(deserializationConfig(),
                /* FormatSchema */ null, _injectableValues);
    }

    protected DeserializationContextExt _deserializationContext(DeserializationConfig config,
            JsonParser p) {
        return _deserializationContexts.createContext(config,
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
     * @throws JacksonException if the initialization fails during initialization
     *   of the streaming parser
     */
    protected JsonToken _initForReading(JsonParser p, JavaType targetType)
        throws JacksonException
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
        throws JacksonException
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
    protected ValueDeserializer<Object> _findRootDeserializer(DeserializationContext ctxt,
            JavaType valueType)
        throws JacksonException
    {
        // First: have we already seen it?
        ValueDeserializer<Object> deser = _rootDeserializers.get(valueType);
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

    protected final void _assertNotNull(String paramName, Object src) {
        if (src == null){
            throw new IllegalArgumentException(String.format("argument \"%s\" is null", paramName));
        }
    }
}
