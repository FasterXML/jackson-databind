package com.fasterxml.jackson.databind;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.io.SegmentedStringWriter;
import com.fasterxml.jackson.core.type.ResolvedType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.*;
import com.fasterxml.jackson.databind.cfg.*;
import com.fasterxml.jackson.databind.deser.*;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.introspect.*;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.*;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.jsontype.impl.StdSubtypeResolver;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.databind.ser.*;
import com.fasterxml.jackson.databind.type.*;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.RootNameLookup;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.databind.util.TokenBuffer;

/**
 * ObjectMapper provides functionality for reading and writing JSON,
 * either to and from basic POJOs (Plain Old Java Objects), or to and from
 * a general-purpose JSON Tree Model ({@link JsonNode}), as well as
 * related functionality for performing conversions.
 * It is also highly customizable to work both with different styles of JSON
 * content, and to support more advanced Object concepts such as
 * polymorphism and Object identity.
 * <code>ObjectMapper</code> also acts as a factory for more advanced {@link ObjectReader}
 * and {@link ObjectWriter} classes.
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
 * The main conversion API is defined in {@link ObjectCodec}, so that
 * implementation details of this class need not be exposed to
 * streaming parser and generator classes. Usage via {@link ObjectCodec} is,
 * however, usually only for cases where dependency to {@link ObjectMapper} is
 * either not possible (from Streaming API), or undesireable (when only relying
 * on Streaming API).
 *<p> 
 * Mapper instances are fully thread-safe provided that ALL configuration of the
 * instance occurs before ANY read or write calls. If configuration of a mapper instance
 * is modified after first usage, changes may or may not take effect, and configuration
 * calls themselves may fail.
 * If you need to use different configuration, you have two main possibilities:
 *<ul>
 * <li>Construct and use {@link ObjectReader} for reading, {@link ObjectWriter} for writing.
 *    Both types are fully immutable and you can freely create new instances with different
 *    configuration using either factory methods of {@link ObjectMapper}, or readers/writers
 *    themselves. Construction of new {@link ObjectReader}s and {@link ObjectWriter}s is
 *    a very light-weight operation so it is usually appropriate to create these on per-call
 *    basis, as needed, for configuring things like optional indentation of JSON.
 *  </li>
 * <li>If the specific kind of configurability is not available via {@link ObjectReader} and
 *   {@link ObjectWriter}, you may need to use multiple {@link ObjectMapper} instead (for example:
 *   you cannot change mix-in annotations on-the-fly; or, set of custom (de)serializers).
 *   To help with this usage, you may want to use method {@link #copy()} which creates a clone
 *   of the mapper with specific configuration, and allows configuration of the copied instance
 *   before it gets used. Note that {@link #copy} operation is as expensive as constructing
 *   a new {@link ObjectMapper} instance: if possible, you should still pool and reuse mappers
 *   if you intend to use them for multiple operations.
 *  </li>
 * </ul>
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
 *<p>
 * Notes on security: use "default typing" feature (see {@link #enableDefaultTyping()})
 * is a potential security risk, if used with untrusted content (content generated by
 * untrusted external parties). If so, you may want to construct a custom 
 * {@link TypeResolverBuilder} implementation to limit possible types to instantiate,
 * (using {@link #setDefaultTyping}).
 */
public class ObjectMapper
    extends ObjectCodec
    implements Versioned,
        java.io.Serializable // as of 2.1
{
    private static final long serialVersionUID = 2L; // as of 2.9

    /*
    /**********************************************************
    /* Helper classes, enums
    /**********************************************************
     */

    /**
     * Enumeration used with {@link ObjectMapper#activateDefaultTyping(PolymorphicTypeValidator)}
     * to specify what kind of types (classes) default typing should
     * be used for. It will only be used if no explicit type information
     * is found, but this enumeration further limits subset of those types.
     *<p>
     * Since 2.4 there are special exceptions for JSON Tree model
     * types (sub-types of {@link TreeNode}: default typing is never
     * applied to them.
     * Since 2.8(.4) additional checks are made to avoid attempts at default
     * typing primitive-valued properties.
     *<p>
     * NOTE: use of Default Typing can be a potential security risk if incoming
     * content comes from untrusted sources, and it is recommended that this
     * is either not done, or, if enabled, make sure to {@code activateDefaultTyping(...)}
     * methods that take {@link PolymorphicTypeValidator} that limits applicability
     * to known trusted types.
     */
    public enum DefaultTyping {
        /**
         * This value means that only properties that have
         * {@link java.lang.Object} as declared type (including
         * generic types without explicit type) will use default
         * typing.
         */
        JAVA_LANG_OBJECT,

        /**
         * Value that means that default typing will be used for
         * properties with declared type of {@link java.lang.Object}
         * or an abstract type (abstract class or interface).
         * Note that this does <b>not</b> include array types.
         *<p>
         * Since 2.4, this does NOT apply to {@link TreeNode} and its subtypes.
         */
        OBJECT_AND_NON_CONCRETE,

        /**
         * Value that means that default typing will be used for
         * all types covered by {@link #OBJECT_AND_NON_CONCRETE}
         * plus all array types for them.
         *<p>
         * Since 2.4, this does NOT apply to {@link TreeNode} and its subtypes.
         */
        NON_CONCRETE_AND_ARRAYS,

        /**
         * Value that means that default typing will be used for
         * all non-final types, with exception of small number of
         * "natural" types (String, Boolean, Integer, Double), which
         * can be correctly inferred from JSON; as well as for
         * all arrays of non-final types.
         *<p>
         * Since 2.4, this does NOT apply to {@link TreeNode} and its subtypes.
         */
        NON_FINAL,

        /**
         * Value that means that default typing will be used for
         * all types, with exception of small number of
         * "natural" types (String, Boolean, Integer, Double) that
         * can be correctly inferred from JSON, and primitives (which
         * can not be polymorphic either).
         * Typing is also enabled for all array types.
         *<p>
         * WARNING: most of the time this is <b>NOT</b> the setting you want
         * as it tends to add Type Ids everywhere, even in cases
         * where type can not be anything other than declared (for example
         * if declared value type of a property is {@code final} -- for example,
         * properties of type {@code long} (or wrapper {@code Long}).
         *<p>
         * Note that this is rarely the option you should use as it results
         * in adding type information in many places where it should not be needed:
         * make sure you understand its behavior.
         * The only known use case for this setting is for serialization
         * when passing instances of final class, and base type is not
         * separately specified.
         *
         * @since 2.10
         */
        EVERYTHING
    }

    /**
     * Customized {@link TypeResolverBuilder} that provides type resolver builders
     * used with so-called "default typing"
     * (see {@link ObjectMapper#activateDefaultTyping(PolymorphicTypeValidator)} for details).
     *<p>
     * Type resolver construction is based on configuration: implementation takes care
     * of only providing builders in cases where type information should be applied.
     * This is important since build calls may be sent for any and all types, and
     * type information should NOT be applied to all of them.
     */
    public static class DefaultTypeResolverBuilder
        extends StdTypeResolverBuilder
        implements java.io.Serializable
    {
        private static final long serialVersionUID = 1L;

        /**
         * Definition of what types is this default typer valid for.
         */
        protected final DefaultTyping _appliesFor;

        /**
         * {@link PolymorphicTypeValidator} top use for validating that the subtypes
         * resolved are valid for use (usually to protect against possible
         * security issues)
         *
         * @since 2.10
         */
        protected final PolymorphicTypeValidator _subtypeValidator;

        /**
         * @deprecated Since 2.10
         */
        @Deprecated // since 2.10
        public DefaultTypeResolverBuilder(DefaultTyping t) {
            this(t, LaissezFaireSubTypeValidator.instance);
        }

        /**
         * @since 2.10
         */
        public DefaultTypeResolverBuilder(DefaultTyping t, PolymorphicTypeValidator ptv) {
            _appliesFor = _requireNonNull(t, "Can not pass `null` DefaultTyping");
            _subtypeValidator = _requireNonNull(ptv, "Can not pass `null` PolymorphicTypeValidator");
        }

        // @since 2.13
        protected DefaultTypeResolverBuilder(DefaultTypeResolverBuilder base, Class<?> defaultImpl) {
            super(base, defaultImpl);
            _appliesFor = base._appliesFor;
            _subtypeValidator = base._subtypeValidator;
        }

        // 20-Jan-2020: as per [databind#2599] Objects.requireNonNull() from JDK7 not in all Android so
        private static <T> T _requireNonNull(T value, String msg) {
            // Replacement for: return Objects.requireNonNull(t, msg);
            if (value == null) {
                throw new NullPointerException(msg);
            }
            return value;
        }

        /**
         * @since 2.10
         */
        public static DefaultTypeResolverBuilder construct(DefaultTyping t,
                PolymorphicTypeValidator ptv) {
            return new DefaultTypeResolverBuilder(t, ptv);
        }

        @Override // since 2.13
        public DefaultTypeResolverBuilder withDefaultImpl(Class<?> defaultImpl) {
            if (_defaultImpl == defaultImpl) {
                return this;
            }
            ClassUtil.verifyMustOverride(DefaultTypeResolverBuilder.class, this, "withDefaultImpl");
            return new DefaultTypeResolverBuilder(this, defaultImpl);
        }

        @Override // since 2.10
        public PolymorphicTypeValidator subTypeValidator(MapperConfig<?> config) {
            return _subtypeValidator;
        }

        @Override
        public TypeDeserializer buildTypeDeserializer(DeserializationConfig config,
                JavaType baseType, Collection<NamedType> subtypes)
        {
            return useForType(baseType) ? super.buildTypeDeserializer(config, baseType, subtypes) : null;
        }

        @Override
        public TypeSerializer buildTypeSerializer(SerializationConfig config,
                JavaType baseType, Collection<NamedType> subtypes)
        {
            return useForType(baseType) ? super.buildTypeSerializer(config, baseType, subtypes) : null;            
        }

        /**
         * Method called to check if the default type handler should be
         * used for given type.
         * Note: "natural types" (String, Boolean, Integer, Double) will never
         * use typing; that is both due to them being concrete and final,
         * and since actual serializers and deserializers will also ignore any
         * attempts to enforce typing.
         */
        public boolean useForType(JavaType t)
        {
            // 03-Oct-2016, tatu: As per [databind#1395], need to skip
            //  primitive types too, regardless
            if (t.isPrimitive()) {
                return false;
            }

            switch (_appliesFor) {
            case NON_CONCRETE_AND_ARRAYS:
                while (t.isArrayType()) {
                    t = t.getContentType();
                }
                // fall through
            case OBJECT_AND_NON_CONCRETE:
                // 19-Apr-2016, tatu: ReferenceType like Optional also requires similar handling:
                while (t.isReferenceType()) {
                    t = t.getReferencedType();
                }
                return t.isJavaLangObject()
                        || (!t.isConcrete()
                                // [databind#88] Should not apply to JSON tree models:
                                && !TreeNode.class.isAssignableFrom(t.getRawClass()));

            case NON_FINAL:
                while (t.isArrayType()) {
                    t = t.getContentType();
                }
                // 19-Apr-2016, tatu: ReferenceType like Optional also requires similar handling:
                while (t.isReferenceType()) {
                    t = t.getReferencedType();
                }
                // [databind#88] Should not apply to JSON tree models:
                return !t.isFinal() && !TreeNode.class.isAssignableFrom(t.getRawClass());
            case EVERYTHING:
                // So, excluding primitives (handled earlier) and "Natural types" (handled
                // before this method is called), applied to everything
                return true;
            default:
            case JAVA_LANG_OBJECT:
                return t.isJavaLangObject();
            }
        }
    }

    /*
    /**********************************************************
    /* Internal constants, singletons
    /**********************************************************
     */

    // 16-May-2009, tatu: Ditto ^^^
    protected final static AnnotationIntrospector DEFAULT_ANNOTATION_INTROSPECTOR = new JacksonAnnotationIntrospector();

    /**
     * Base settings contain defaults used for all {@link ObjectMapper}
     * instances.
     */
    protected final static BaseSettings DEFAULT_BASE = new BaseSettings(
            null, // cannot share global ClassIntrospector any more (2.5+)
            DEFAULT_ANNOTATION_INTROSPECTOR,
             null, TypeFactory.defaultInstance(),
            null, StdDateFormat.instance, null,
            Locale.getDefault(),
            null, // to indicate "use Jackson default TimeZone" (UTC since Jackson 2.7)
            Base64Variants.getDefaultVariant(),
            // Only for 2.x; 3.x will use more restrictive default
            LaissezFaireSubTypeValidator.instance,
            // Since 2.12:
            new DefaultAccessorNamingStrategy.Provider()
    );

    /*
    /**********************************************************
    /* Configuration settings, shared
    /**********************************************************
     */

    /**
     * Factory used to create {@link JsonParser} and {@link JsonGenerator}
     * instances as necessary.
     */
    protected final JsonFactory _jsonFactory;

    /**
     * Specific factory used for creating {@link JavaType} instances;
     * needed to allow modules to add more custom type handling
     * (mostly to support types of non-Java JVM languages)
     */
    protected TypeFactory _typeFactory;

    /**
     * Provider for values to inject in deserialized POJOs.
     */
    protected InjectableValues _injectableValues;

    /**
     * Thing used for registering sub-types, resolving them to
     * super/sub-types as needed.
     */
    protected SubtypeResolver _subtypeResolver;

    /**
     * Currently active per-type configuration overrides, accessed by
     * declared type of property.
     *
     * @since 2.9
     */
    protected final ConfigOverrides _configOverrides;

    /**
     * Current set of coercion configuration definitions that define allowed
     * (and not allowed) coercions from secondary shapes.
     *
     * @since 2.12
     */
    protected final CoercionConfigs _coercionConfigs;

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
     * 
     * @since 2.6 (earlier was a simple {@link java.util.Map}
     */
    protected SimpleMixInResolver _mixIns;

    /*
    /**********************************************************
    /* Configuration settings, serialization
    /**********************************************************
     */

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
    protected DefaultSerializerProvider _serializerProvider;

    /**
     * Serializer factory used for constructing serializers.
     */
    protected SerializerFactory _serializerFactory;

    /*
    /**********************************************************
    /* Configuration settings, deserialization
    /**********************************************************
     */

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
    protected DefaultDeserializationContext _deserializationContext;

    /*
    /**********************************************************
    /* Module-related
    /**********************************************************
     */

    /**
     * Set of module types (as per {@link Module#getTypeId()} that have been
     * registered; kept track of iff {@link MapperFeature#IGNORE_DUPLICATE_MODULE_REGISTRATIONS}
     * is enabled, so that duplicate registration calls can be ignored
     * (to avoid adding same handlers multiple times, mostly).
     * 
     * @since 2.5
     */
    protected Set<Object> _registeredModuleTypes;

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
    final protected ConcurrentHashMap<JavaType, JsonDeserializer<Object>> _rootDeserializers
        = new ConcurrentHashMap<JavaType, JsonDeserializer<Object>>(64, 0.6f, 2);

    /*
    /**********************************************************
    /* Life-cycle: constructing instance
    /**********************************************************
     */

    /**
     * Default constructor, which will construct the default
     * {@link JsonFactory} as necessary, use
     * {@link SerializerProvider} as its
     * {@link SerializerProvider}, and
     * {@link BeanSerializerFactory} as its
     * {@link SerializerFactory}.
     * This means that it
     * can serialize all standard JDK types, as well as regular
     * Java Beans (based on method names and Jackson-specific annotations),
     * but does not support JAXB annotations.
     */
    public ObjectMapper() {
        this(null, null, null);
    }

    /**
     * Constructs instance that uses specified {@link JsonFactory}
     * for constructing necessary {@link JsonParser}s and/or
     * {@link JsonGenerator}s.
     */
    public ObjectMapper(JsonFactory jf) {
        this(jf, null, null);
    }

    /**
     * Copy-constructor, mostly used to support {@link #copy}.
     * 
     * @since 2.1
     */
    protected ObjectMapper(ObjectMapper src)
    {
        _jsonFactory = src._jsonFactory.copy();
        _jsonFactory.setCodec(this);
        _subtypeResolver = src._subtypeResolver.copy();
        _typeFactory = src._typeFactory;
        _injectableValues = src._injectableValues;
        _configOverrides = src._configOverrides.copy();
        _coercionConfigs = src._coercionConfigs.copy();
        _mixIns = src._mixIns.copy();

        RootNameLookup rootNames = new RootNameLookup();
        _serializationConfig = new SerializationConfig(src._serializationConfig,
                _subtypeResolver, _mixIns, rootNames, _configOverrides);
        _deserializationConfig = new DeserializationConfig(src._deserializationConfig,
                _subtypeResolver, _mixIns, rootNames, _configOverrides,
                _coercionConfigs);
        _serializerProvider = src._serializerProvider.copy();
        _deserializationContext = src._deserializationContext.copy();

        // Default serializer factory is stateless, can just assign
        _serializerFactory = src._serializerFactory;

        // as per [databind#922], [databind#1078] make sure to copy registered modules as appropriate
        Set<Object> reg = src._registeredModuleTypes;
        if (reg == null) {
            _registeredModuleTypes = null;
        } else {
            _registeredModuleTypes = new LinkedHashSet<Object>(reg);
        }
    }

    /**
     * Constructs instance that uses specified {@link JsonFactory}
     * for constructing necessary {@link JsonParser}s and/or
     * {@link JsonGenerator}s, and uses given providers for accessing
     * serializers and deserializers.
     * 
     * @param jf JsonFactory to use: if null, a new {@link MappingJsonFactory} will be constructed
     * @param sp SerializerProvider to use: if null, a {@link SerializerProvider} will be constructed
     * @param dc Blueprint deserialization context instance to use for creating
     *    actual context objects; if null, will construct standard
     *    {@link DeserializationContext}
     */
    public ObjectMapper(JsonFactory jf,
            DefaultSerializerProvider sp, DefaultDeserializationContext dc)
    {
        // 02-Mar-2009, tatu: Important: we MUST default to using the mapping factory,
        //  otherwise tree serialization will have problems with POJONodes.
        if (jf == null) {
            _jsonFactory = new MappingJsonFactory(this);
        } else {
            _jsonFactory = jf;
            if (jf.getCodec() == null) { // as per [JACKSON-741]
                _jsonFactory.setCodec(this);
            }
        }
        _subtypeResolver = new StdSubtypeResolver();
        RootNameLookup rootNames = new RootNameLookup();
        // and default type factory is shared one
        _typeFactory = TypeFactory.defaultInstance();

        SimpleMixInResolver mixins = new SimpleMixInResolver(null);
        _mixIns = mixins;
        BaseSettings base = DEFAULT_BASE.withClassIntrospector(defaultClassIntrospector());
        _configOverrides = new ConfigOverrides();
        _coercionConfigs = new CoercionConfigs();
        _serializationConfig = new SerializationConfig(base,
                    _subtypeResolver, mixins, rootNames, _configOverrides);
        _deserializationConfig = new DeserializationConfig(base,
                    _subtypeResolver, mixins, rootNames, _configOverrides,
                    _coercionConfigs);

        // Some overrides we may need
        final boolean needOrder = _jsonFactory.requiresPropertyOrdering();
        if (needOrder ^ _serializationConfig.isEnabled(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)) {
            configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, needOrder);
        }
        
        _serializerProvider = (sp == null) ? new DefaultSerializerProvider.Impl() : sp;
        _deserializationContext = (dc == null) ?
                new DefaultDeserializationContext.Impl(BeanDeserializerFactory.instance) : dc;

        // Default serializer factory is stateless, can just assign
        _serializerFactory = BeanSerializerFactory.instance;
    }

    /**
     * Overridable helper method used to construct default {@link ClassIntrospector}
     * to use.
     * 
     * @since 2.5
     */
    protected ClassIntrospector defaultClassIntrospector() {
        return new BasicClassIntrospector();
    }

    /*
    /**********************************************************
    /* Methods sub-classes MUST override
    /**********************************************************
     */
    
    /**
     * Method for creating a new {@link ObjectMapper} instance that
     * has same initial configuration as this instance. Note that this
     * also requires making a copy of the underlying {@link JsonFactory}
     * instance.
     *<p>
     * Method is typically
     * used when multiple, differently configured mappers are needed.
     * Although configuration is shared, cached serializers and deserializers
     * are NOT shared, which means that the new instance may be re-configured
     * before use; meaning that it behaves the same way as if an instance
     * was constructed from scratch.
     * 
     * @since 2.1
     */
    public ObjectMapper copy() {
        _checkInvalidCopy(ObjectMapper.class);
        return new ObjectMapper(this);
    }

    /**
     * @since 2.1
     */
    protected void _checkInvalidCopy(Class<?> exp)
    {
        if (getClass() != exp) {
            // 10-Nov-2016, tatu: could almost use `ClassUtil.verifyMustOverride()` but not quite
            throw new IllegalStateException("Failed copy(): "+getClass().getName()
                    +" (version: "+version()+") does not override copy(); it has to");
        }
    }

    /*
    /**********************************************************
    /* Methods sub-classes MUST override if providing custom
    /* ObjectReader/ObjectWriter implementations
    /**********************************************************
     */
    
    /**
     * Factory method sub-classes must override, to produce {@link ObjectReader}
     * instances of proper sub-type
     * 
     * @since 2.5
     */
    protected ObjectReader _newReader(DeserializationConfig config) {
        return new ObjectReader(this, config);
    }

    /**
     * Factory method sub-classes must override, to produce {@link ObjectReader}
     * instances of proper sub-type
     * 
     * @since 2.5
     */
    protected ObjectReader _newReader(DeserializationConfig config,
            JavaType valueType, Object valueToUpdate,
            FormatSchema schema, InjectableValues injectableValues) {
        return new ObjectReader(this, config, valueType, valueToUpdate, schema, injectableValues);
    }

    /**
     * Factory method sub-classes must override, to produce {@link ObjectWriter}
     * instances of proper sub-type
     * 
     * @since 2.5
     */
    protected ObjectWriter _newWriter(SerializationConfig config) {
        return new ObjectWriter(this, config);
    }

    /**
     * Factory method sub-classes must override, to produce {@link ObjectWriter}
     * instances of proper sub-type
     * 
     * @since 2.5
     */
    protected ObjectWriter _newWriter(SerializationConfig config, FormatSchema schema) {
        return new ObjectWriter(this, config, schema);
    }

    /**
     * Factory method sub-classes must override, to produce {@link ObjectWriter}
     * instances of proper sub-type
     * 
     * @since 2.5
     */
    protected ObjectWriter _newWriter(SerializationConfig config,
            JavaType rootType, PrettyPrinter pp) {
        return new ObjectWriter(this, config, rootType, pp);
    }

    /*
    /**********************************************************
    /* Versioned impl
    /**********************************************************
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
    /**********************************************************
    /* Module registration, discovery
    /**********************************************************
     */

    /**
     * Method for registering a module that can extend functionality
     * provided by this mapper; for example, by adding providers for
     * custom serializers and deserializers.
     * 
     * @param module Module to register
     */
    public ObjectMapper registerModule(Module module)
    {
        _assertNotNull("module", module);
        // Let's ensure we have access to name and version information, 
        // even if we do not have immediate use for either. This way we know
        // that they will be available from beginning
        String name = module.getModuleName();
        if (name == null) {
            throw new IllegalArgumentException("Module without defined name");
        }
        Version version = module.version();
        if (version == null) {
            throw new IllegalArgumentException("Module without defined version");
        }

        // [databind#2432]: Modules may depend on other modules; if so, register those first
        for (Module dep : module.getDependencies()) {
            registerModule(dep);
        }

        // then module itself
        if (isEnabled(MapperFeature.IGNORE_DUPLICATE_MODULE_REGISTRATIONS)) {
            Object typeId = module.getTypeId();
            if (typeId != null) {
                if (_registeredModuleTypes == null) {
                    // plus let's keep them in order too, easier to debug or expose
                    // in registration order if that matter
                    _registeredModuleTypes = new LinkedHashSet<Object>();
                }
                // try adding; if already had it, should skip
                if (!_registeredModuleTypes.add(typeId)) {
                    return this;
                }
            }
        }

        // And then call registration
        module.setupModule(new Module.SetupContext()
        {
            // // // Accessors

            @Override
            public Version getMapperVersion() {
                return version();
            }

            @SuppressWarnings("unchecked")
            @Override
            public <C extends ObjectCodec> C getOwner() {
                // why do we need the cast here?!?
                return (C) ObjectMapper.this;
            }

            @Override
            public TypeFactory getTypeFactory() {
                return _typeFactory;
            }
            
            @Override
            public boolean isEnabled(MapperFeature f) {
                return ObjectMapper.this.isEnabled(f);
            }

            @Override
            public boolean isEnabled(DeserializationFeature f) {
                return ObjectMapper.this.isEnabled(f);
            }
            
            @Override
            public boolean isEnabled(SerializationFeature f) {
                return ObjectMapper.this.isEnabled(f);
            }

            @Override
            public boolean isEnabled(JsonFactory.Feature f) {
                return ObjectMapper.this.isEnabled(f);
            }

            @Override
            public boolean isEnabled(JsonParser.Feature f) {
                return ObjectMapper.this.isEnabled(f);
            }

            @Override
            public boolean isEnabled(JsonGenerator.Feature f) {
                return ObjectMapper.this.isEnabled(f);
            }

            // // // Mutant accessors

            @Override
            public MutableConfigOverride configOverride(Class<?> type) {
                return ObjectMapper.this.configOverride(type);
            }

            // // // Methods for registering handlers: deserializers

            @Override
            public void addDeserializers(Deserializers d) {
                DeserializerFactory df = _deserializationContext._factory.withAdditionalDeserializers(d);
                _deserializationContext = _deserializationContext.with(df);
            }

            @Override
            public void addKeyDeserializers(KeyDeserializers d) {
                DeserializerFactory df = _deserializationContext._factory.withAdditionalKeyDeserializers(d);
                _deserializationContext = _deserializationContext.with(df);
            }

            @Override
            public void addBeanDeserializerModifier(BeanDeserializerModifier modifier) {
                DeserializerFactory df = _deserializationContext._factory.withDeserializerModifier(modifier);
                _deserializationContext = _deserializationContext.with(df);
            }
            
            // // // Methods for registering handlers: serializers
            
            @Override
            public void addSerializers(Serializers s) {
                _serializerFactory = _serializerFactory.withAdditionalSerializers(s);
            }

            @Override
            public void addKeySerializers(Serializers s) {
                _serializerFactory = _serializerFactory.withAdditionalKeySerializers(s);
            }
            
            @Override
            public void addBeanSerializerModifier(BeanSerializerModifier modifier) {
                _serializerFactory = _serializerFactory.withSerializerModifier(modifier);
            }

            // // // Methods for registering handlers: other
            
            @Override
            public void addAbstractTypeResolver(AbstractTypeResolver resolver) {
                DeserializerFactory df = _deserializationContext._factory.withAbstractTypeResolver(resolver);
                _deserializationContext = _deserializationContext.with(df);
            }

            @Override
            public void addTypeModifier(TypeModifier modifier) {
                TypeFactory f = _typeFactory;
                f = f.withModifier(modifier);
                setTypeFactory(f);
            }

            @Override
            public void addValueInstantiators(ValueInstantiators instantiators) {
                DeserializerFactory df = _deserializationContext._factory.withValueInstantiators(instantiators);
                _deserializationContext = _deserializationContext.with(df);
            }

            @Override
            public void setClassIntrospector(ClassIntrospector ci) {
                _deserializationConfig = _deserializationConfig.with(ci);
                _serializationConfig = _serializationConfig.with(ci);
            }

            @Override
            public void insertAnnotationIntrospector(AnnotationIntrospector ai) {
                _deserializationConfig = _deserializationConfig.withInsertedAnnotationIntrospector(ai);
                _serializationConfig = _serializationConfig.withInsertedAnnotationIntrospector(ai);
            }
            
            @Override
            public void appendAnnotationIntrospector(AnnotationIntrospector ai) {
                _deserializationConfig = _deserializationConfig.withAppendedAnnotationIntrospector(ai);
                _serializationConfig = _serializationConfig.withAppendedAnnotationIntrospector(ai);
            }

            @Override
            public void registerSubtypes(Class<?>... subtypes) {
                ObjectMapper.this.registerSubtypes(subtypes);
            }

            @Override
            public void registerSubtypes(NamedType... subtypes) {
                ObjectMapper.this.registerSubtypes(subtypes);
            }

            @Override
            public void registerSubtypes(Collection<Class<?>> subtypes) {
                ObjectMapper.this.registerSubtypes(subtypes);
            }

            @Override
            public void setMixInAnnotations(Class<?> target, Class<?> mixinSource) {
                addMixIn(target, mixinSource);
            }
            
            @Override
            public void addDeserializationProblemHandler(DeserializationProblemHandler handler) {
                addHandler(handler);
            }

            @Override
            public void setNamingStrategy(PropertyNamingStrategy naming) {
                setPropertyNamingStrategy(naming);
            }
        });

        return this;
    }

    /**
     * Convenience method for registering specified modules in order;
     * functionally equivalent to:
     *<pre>
     *   for (Module module : modules) {
     *      registerModule(module);
     *   }
     *</pre>
     * 
     * @since 2.2
     */
    public ObjectMapper registerModules(Module... modules)
    {
        for (Module module : modules) {
            registerModule(module);
        }
        return this;
    }

    /**
     * Convenience method for registering specified modules in order;
     * functionally equivalent to:
     *<pre>
     *   for (Module module : modules) {
     *      registerModule(module);
     *   }
     *</pre>
     * 
     * @since 2.2
     */
    public ObjectMapper registerModules(Iterable<? extends Module> modules)
    {
        _assertNotNull("modules", modules);
        for (Module module : modules) {
            registerModule(module);
        }
        return this;
    }

    /**
     * The set of {@link Module} typeIds that are registered in this
     * ObjectMapper, if (and only if!)
     * {@link MapperFeature#IGNORE_DUPLICATE_MODULE_REGISTRATIONS}
     * is enabled AND module being added returns non-{@code null} value
     * for its {@link Module#getTypeId()}.
     *<p>
     * NOTE: when using the default {@link com.fasterxml.jackson.databind.module.SimpleModule}
     * constructor, its id is specified as {@code null} and as a consequence such
     * module is NOT included in returned set.
     *
     * @since 2.9.6
     */
    public Set<Object> getRegisteredModuleIds()
    {
        return (_registeredModuleTypes == null) ?
                Collections.emptySet() : Collections.unmodifiableSet(_registeredModuleTypes);
    }

    /**
     * Method for locating available methods, using JDK {@link ServiceLoader}
     * facility, along with module-provided SPI.
     *<p>
     * Note that method does not do any caching, so calls should be considered
     * potentially expensive.
     * 
     * @since 2.2
     */
    public static List<Module> findModules() {
        return findModules(null);
    }

    /**
     * Method for locating available methods, using JDK {@link ServiceLoader}
     * facility, along with module-provided SPI.
     *<p>
     * Note that method does not do any caching, so calls should be considered
     * potentially expensive.
     * 
     * @since 2.2
     */
    public static List<Module> findModules(ClassLoader classLoader)
    {
        ArrayList<Module> modules = new ArrayList<Module>();
        ServiceLoader<Module> loader = secureGetServiceLoader(Module.class, classLoader);
        for (Module module : loader) {
            modules.add(module);
        }
        return modules;
    }

    private static <T> ServiceLoader<T> secureGetServiceLoader(final Class<T> clazz, final ClassLoader classLoader) {
        final SecurityManager sm = System.getSecurityManager();
        if (sm == null) {
            return (classLoader == null) ?
                    ServiceLoader.load(clazz) : ServiceLoader.load(clazz, classLoader);
        }
        return AccessController.doPrivileged(new PrivilegedAction<ServiceLoader<T>>() {
            @Override
            public ServiceLoader<T> run() {
                return (classLoader == null) ?
                        ServiceLoader.load(clazz) : ServiceLoader.load(clazz, classLoader);
            }
        });
    }

    /**
     * Convenience method that is functionally equivalent to:
     *<code>
     *   mapper.registerModules(mapper.findModules());
     *</code>
     *<p>
     * As with {@link #findModules()}, no caching is done for modules, so care
     * needs to be taken to either create and share a single mapper instance;
     * or to cache introspected set of modules.
     *
     * @since 2.2
     */
    public ObjectMapper findAndRegisterModules() {
        return registerModules(findModules());
    }

    /*
    /**********************************************************
    /* Factory methods for creating JsonGenerators (added in 2.11)
    /**********************************************************
     */

    /**
     * Factory method for constructing properly initialized {@link JsonGenerator}
     * to write content using specified {@link OutputStream}.
     * Generator is not managed (or "owned") by mapper: caller is responsible
     * for properly closing it once content generation is complete.
     *
     * @since 2.11
     */
    public JsonGenerator createGenerator(OutputStream out) throws IOException {
        _assertNotNull("out", out);
        JsonGenerator g = _jsonFactory.createGenerator(out, JsonEncoding.UTF8);
        _serializationConfig.initialize(g);
        return g;
    }

    /**
     * Factory method for constructing properly initialized {@link JsonGenerator}
     * to write content using specified {@link OutputStream} and encoding.
     * Generator is not managed (or "owned") by mapper: caller is responsible
     * for properly closing it once content generation is complete.
     *
     * @since 2.11
     */
    public JsonGenerator createGenerator(OutputStream out, JsonEncoding enc) throws IOException {
        _assertNotNull("out", out);
        JsonGenerator g = _jsonFactory.createGenerator(out, enc);
        _serializationConfig.initialize(g);
        return g;
    }

    /**
     * Factory method for constructing properly initialized {@link JsonGenerator}
     * to write content using specified {@link Writer}.
     * Generator is not managed (or "owned") by mapper: caller is responsible
     * for properly closing it once content generation is complete.
     *
     * @since 2.11
     */
    public JsonGenerator createGenerator(Writer w) throws IOException {
        _assertNotNull("w", w);
        JsonGenerator g = _jsonFactory.createGenerator(w);
        _serializationConfig.initialize(g);
        return g;
    }

    /**
     * Factory method for constructing properly initialized {@link JsonGenerator}
     * to write content to specified {@link File}, using specified encoding.
     * Generator is not managed (or "owned") by mapper: caller is responsible
     * for properly closing it once content generation is complete.
     *
     * @since 2.11
     */
    public JsonGenerator createGenerator(File outputFile, JsonEncoding enc) throws IOException {
        _assertNotNull("outputFile", outputFile);
        JsonGenerator g = _jsonFactory.createGenerator(outputFile, enc);
        _serializationConfig.initialize(g);
        return g;
    }

    /**
     * Factory method for constructing properly initialized {@link JsonGenerator}
     * to write content using specified {@link DataOutput}.
     * Generator is not managed (or "owned") by mapper: caller is responsible
     * for properly closing it once content generation is complete.
     *
     * @since 2.11
     */
    public JsonGenerator createGenerator(DataOutput out) throws IOException {
        _assertNotNull("out", out);
        JsonGenerator g = _jsonFactory.createGenerator(out);
        _serializationConfig.initialize(g);
        return g;
    }

    /*
    /**********************************************************
    /* Factory methods for creating JsonParsers (added in 2.11)
    /**********************************************************
     */

    /**
     * Factory method for constructing properly initialized {@link JsonParser}
     * to read content from specified {@link File}.
     * Parser is not managed (or "owned") by ObjectMapper: caller is responsible
     * for properly closing it once content reading is complete.
     *
     * @since 2.11
     */
    public JsonParser createParser(File src) throws IOException {
        _assertNotNull("src", src);
        return _deserializationConfig.initialize(_jsonFactory.createParser(src));
    }

    /**
     * Factory method for constructing properly initialized {@link JsonParser}
     * to read content from specified {@link File}.
     * Parser is not managed (or "owned") by ObjectMapper: caller is responsible
     * for properly closing it once content reading is complete.
     *
     * @since 2.11
     */
    public JsonParser createParser(URL src) throws IOException {
        _assertNotNull("src", src);
        return _deserializationConfig.initialize(_jsonFactory.createParser(src));
    }

    /**
     * Factory method for constructing properly initialized {@link JsonParser}
     * to read content using specified {@link InputStream}.
     * Parser is not managed (or "owned") by ObjectMapper: caller is responsible
     * for properly closing it once content reading is complete.
     *
     * @since 2.11
     */
    public JsonParser createParser(InputStream in) throws IOException {
        _assertNotNull("in", in);
        return _deserializationConfig.initialize(_jsonFactory.createParser(in));
    }

    /**
     * Factory method for constructing properly initialized {@link JsonParser}
     * to read content using specified {@link Reader}.
     * Parser is not managed (or "owned") by ObjectMapper: caller is responsible
     * for properly closing it once content reading is complete.
     *
     * @since 2.11
     */
    public JsonParser createParser(Reader r) throws IOException {
        _assertNotNull("r", r);
        return _deserializationConfig.initialize(_jsonFactory.createParser(r));
    }

    /**
     * Factory method for constructing properly initialized {@link JsonParser}
     * to read content from specified byte array.
     * Parser is not managed (or "owned") by ObjectMapper: caller is responsible
     * for properly closing it once content reading is complete.
     *
     * @since 2.11
     */
    public JsonParser createParser(byte[] content) throws IOException {
        _assertNotNull("content", content);
        return _deserializationConfig.initialize(_jsonFactory.createParser(content));
    }

    /**
     * Factory method for constructing properly initialized {@link JsonParser}
     * to read content from specified byte array.
     * Parser is not managed (or "owned") by ObjectMapper: caller is responsible
     * for properly closing it once content reading is complete.
     *
     * @since 2.11
     */
    public JsonParser createParser(byte[] content, int offset, int len) throws IOException {
        _assertNotNull("content", content);
        return _deserializationConfig.initialize(_jsonFactory.createParser(content, offset, len));
    }

    /**
     * Factory method for constructing properly initialized {@link JsonParser}
     * to read content from specified String.
     * Parser is not managed (or "owned") by ObjectMapper: caller is responsible
     * for properly closing it once content reading is complete.
     *
     * @since 2.11
     */
    public JsonParser createParser(String content) throws IOException {
        _assertNotNull("content", content);
        return _deserializationConfig.initialize(_jsonFactory.createParser(content));
    }

    /**
     * Factory method for constructing properly initialized {@link JsonParser}
     * to read content from specified character array
     * Parser is not managed (or "owned") by ObjectMapper: caller is responsible
     * for properly closing it once content reading is complete.
     *
     * @since 2.11
     */
    public JsonParser createParser(char[] content) throws IOException {
        _assertNotNull("content", content);
        return _deserializationConfig.initialize(_jsonFactory.createParser(content));
    }

    /**
     * Factory method for constructing properly initialized {@link JsonParser}
     * to read content from specified character array.
     * Parser is not managed (or "owned") by ObjectMapper: caller is responsible
     * for properly closing it once content reading is complete.
     *
     * @since 2.11
     */
    public JsonParser createParser(char[] content, int offset, int len) throws IOException {
        _assertNotNull("content", content);
        return _deserializationConfig.initialize(_jsonFactory.createParser(content, offset, len));
    }

    /**
     * Factory method for constructing properly initialized {@link JsonParser}
     * to read content using specified {@link DataInput}.
     * Parser is not managed (or "owned") by ObjectMapper: caller is responsible
     * for properly closing it once content reading is complete.
     *
     * @since 2.11
     */
    public JsonParser createParser(DataInput content) throws IOException {
        _assertNotNull("content", content);
        return _deserializationConfig.initialize(_jsonFactory.createParser(content));
    }

    /**
     * Factory method for constructing properly initialized {@link JsonParser}
     * to read content using non-blocking (asynchronous) mode.
     * Parser is not managed (or "owned") by ObjectMapper: caller is responsible
     * for properly closing it once content reading is complete.
     *
     * @since 2.11
     */
    public JsonParser createNonBlockingByteArrayParser() throws IOException {
        return _deserializationConfig.initialize(_jsonFactory.createNonBlockingByteArrayParser());
    }

    /*
    /**********************************************************
    /* Configuration: main config object access
    /**********************************************************
     */

    /**
     * Method that returns the shared default {@link SerializationConfig}
     * object that defines configuration settings for serialization.
     *<p>
     * Note that since instances are immutable, you can NOT change settings
     * by accessing an instance and calling methods: this will simply create
     * new instance of config object.
     */
    public SerializationConfig getSerializationConfig() {
        return _serializationConfig;
    }

    /**
     * Method that returns
     * the shared default {@link DeserializationConfig} object
     * that defines configuration settings for deserialization.
     *<p>
     * Note that since instances are immutable, you can NOT change settings
     * by accessing an instance and calling methods: this will simply create
     * new instance of config object.
     */
    public DeserializationConfig getDeserializationConfig() {
        return _deserializationConfig;
    }
    
    /**
     * Method for getting current {@link DeserializationContext}.
      *<p>
     * Note that since instances are immutable, you can NOT change settings
     * by accessing an instance and calling methods: this will simply create
     * new instance of context object.
    */
    public DeserializationContext getDeserializationContext() {
        return _deserializationContext;
    }

    /*
    /**********************************************************
    /* Configuration: ser/deser factory, provider access
    /**********************************************************
     */
    
    /**
     * Method for setting specific {@link SerializerFactory} to use
     * for constructing (bean) serializers.
     */
    public ObjectMapper setSerializerFactory(SerializerFactory f) {
        _serializerFactory = f;
        return this;
    }

    /**
     * Method for getting current {@link SerializerFactory}.
      *<p>
     * Note that since instances are immutable, you can NOT change settings
     * by accessing an instance and calling methods: this will simply create
     * new instance of factory object.
     */
    public SerializerFactory getSerializerFactory() {
        return _serializerFactory;
    }

    /**
     * Method for setting "blueprint" {@link SerializerProvider} instance
     * to use as the base for actual provider instances to use for handling
     * caching of {@link JsonSerializer} instances.
     */
    public ObjectMapper setSerializerProvider(DefaultSerializerProvider p) {
        _serializerProvider = p;
        return this;
    }

    /**
     * Accessor for the "blueprint" (or, factory) instance, from which instances
     * are created by calling {@link DefaultSerializerProvider#createInstance}.
     * Note that returned instance cannot be directly used as it is not properly
     * configured: to get a properly configured instance to call, use
     * {@link #getSerializerProviderInstance()} instead.
     */
    public SerializerProvider getSerializerProvider() {
        return _serializerProvider;
    }

    /**
     * Accessor for constructing and returning a {@link SerializerProvider}
     * instance that may be used for accessing serializers. This is same as
     * calling {@link #getSerializerProvider}, and calling <code>createInstance</code>
     * on it.
     *
     * @since 2.7
     */
    public SerializerProvider getSerializerProviderInstance() {
        return _serializerProvider(_serializationConfig);
    }

    /*
    /**********************************************************
    /* Configuration: mix-in annotations
    /**********************************************************
     */

    /**
     * Method to use for defining mix-in annotations to use for augmenting
     * annotations that processable (serializable / deserializable)
     * classes have.
     * Mixing in is done when introspecting class annotations and properties.
     * Map passed contains keys that are target classes (ones to augment
     * with new annotation overrides), and values that are source classes
     * (have annotations to use for augmentation).
     * Annotations from source classes (and their supertypes)
     * will <b>override</b>
     * annotations that target classes (and their super-types) have.
     *<p>
     * Note that this method will CLEAR any previously defined mix-ins
     * for this mapper.
     *
     * @since 2.5
     */
    public ObjectMapper setMixIns(Map<Class<?>, Class<?>> sourceMixins)
    {
        // NOTE: does NOT change possible externally configured resolver, just local defs
        _mixIns.setLocalDefinitions(sourceMixins);
        return this;
    }

    /**
     * Method to use for adding mix-in annotations to use for augmenting
     * specified class or interface. All annotations from
     * <code>mixinSource</code> are taken to override annotations
     * that <code>target</code> (or its supertypes) has.
     *
     * @param target Class (or interface) whose annotations to effectively override
     * @param mixinSource Class (or interface) whose annotations are to
     *   be "added" to target's annotations, overriding as necessary
     *
     * @since 2.5
     */
    public ObjectMapper addMixIn(Class<?> target, Class<?> mixinSource)
    {
        _mixIns.addLocalDefinition(target, mixinSource);
        return this;
    }

    /**
     * Method that can be called to specify given resolver for locating
     * mix-in classes to use, overriding directly added mappings.
     * Note that direct mappings are not cleared, but they are only applied
     * if resolver does not provide mix-in matches.
     *
     * @since 2.6
     */
    public ObjectMapper setMixInResolver(ClassIntrospector.MixInResolver resolver)
    {
        SimpleMixInResolver r = _mixIns.withOverrides(resolver);
        if (r != _mixIns) {
            _mixIns = r;
            _deserializationConfig = new DeserializationConfig(_deserializationConfig, r);
            _serializationConfig = new SerializationConfig(_serializationConfig, r);
        }
        return this;
    }
    
    public Class<?> findMixInClassFor(Class<?> cls) {
        return _mixIns.findMixInClassFor(cls);
    }

    // For testing only:
    public int mixInCount() {
        return _mixIns.localSize();
    }

    /**
     * @deprecated Since 2.5: replaced by a fluent form of the method; {@link #setMixIns}.
     */
    @Deprecated
    public void setMixInAnnotations(Map<Class<?>, Class<?>> sourceMixins) {
        setMixIns(sourceMixins);
    }

    /**
     * @deprecated Since 2.5: replaced by a fluent form of the method; {@link #addMixIn(Class, Class)}.
     */
    @Deprecated
    public final void addMixInAnnotations(Class<?> target, Class<?> mixinSource) {
        addMixIn(target, mixinSource);
    }

    /*
    /**********************************************************
    /* Configuration, introspection
    /**********************************************************
     */

    /**
     * Method for accessing currently configured visibility checker;
     * object used for determining whether given property element
     * (method, field, constructor) can be auto-detected or not.
     */
    public VisibilityChecker<?> getVisibilityChecker() {
        return _serializationConfig.getDefaultVisibilityChecker();
    }

    /**
     * Method for setting currently configured default {@link VisibilityChecker},
     * object used for determining whether given property element
     * (method, field, constructor) can be auto-detected or not.
     * This default checker is used as the base visibility:
     * per-class overrides (both via annotations and per-type config overrides)
     * can further change these settings.
     * 
     * @since 2.6
     */
    public ObjectMapper setVisibility(VisibilityChecker<?> vc) {
        _configOverrides.setDefaultVisibility(vc);
        return this;
    }

    /**
     * Convenience method that allows changing configuration for
     * underlying {@link VisibilityChecker}s, to change details of what kinds of
     * properties are auto-detected.
     * Basically short cut for doing:
     *<pre>
     *  mapper.setVisibilityChecker(
     *     mapper.getVisibilityChecker().withVisibility(forMethod, visibility)
     *  );
     *</pre>
     * one common use case would be to do:
     *<pre>
     *  mapper.setVisibility(JsonMethod.FIELD, Visibility.ANY);
     *</pre>
     * which would make all member fields serializable without further annotations,
     * instead of just public fields (default setting).
     * 
     * @param forMethod Type of property descriptor affected (field, getter/isGetter,
     *     setter, creator)
     * @param visibility Minimum visibility to require for the property descriptors of type
     * 
     * @return Modified mapper instance (that is, "this"), to allow chaining
     *    of configuration calls
     */
    public ObjectMapper setVisibility(PropertyAccessor forMethod, JsonAutoDetect.Visibility visibility)
    {
        VisibilityChecker<?> vc = _configOverrides.getDefaultVisibility();
        vc = vc.withVisibility(forMethod, visibility);
        _configOverrides.setDefaultVisibility(vc);
        return this;
    }

    /**
     * Method for accessing subtype resolver in use.
     */
    public SubtypeResolver getSubtypeResolver() {
        return _subtypeResolver;
    }

    /**
     * Method for setting custom subtype resolver to use.
     */
    public ObjectMapper setSubtypeResolver(SubtypeResolver str) {
        _subtypeResolver = str;
        _deserializationConfig = _deserializationConfig.with(str);
        _serializationConfig = _serializationConfig.with(str);
        return this;
    }

    /**
     * Method for setting {@link AnnotationIntrospector} used by this
     * mapper instance for both serialization and deserialization.
     * Note that doing this will replace the current introspector, which
     * may lead to unavailability of core Jackson annotations.
     * If you want to combine handling of multiple introspectors,
     * have a look at {@link com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair}.
     * 
     * @see com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair
     */
    public ObjectMapper setAnnotationIntrospector(AnnotationIntrospector ai) {
        _serializationConfig = _serializationConfig.with(ai);
        _deserializationConfig = _deserializationConfig.with(ai);
        return this;
    }

    /**
     * Method for changing {@link AnnotationIntrospector} instances used
     * by this mapper instance for serialization and deserialization,
     * specifying them separately so that different introspection can be
     * used for different aspects
     * 
     * @since 2.1
     * 
     * @param serializerAI {@link AnnotationIntrospector} to use for configuring
     *    serialization
     * @param deserializerAI {@link AnnotationIntrospector} to use for configuring
     *    deserialization
     * 
     * @see com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair
     */
    public ObjectMapper setAnnotationIntrospectors(AnnotationIntrospector serializerAI,
            AnnotationIntrospector deserializerAI) {
        _serializationConfig = _serializationConfig.with(serializerAI);
        _deserializationConfig = _deserializationConfig.with(deserializerAI);
        return this;
    }

    /**
     * Method for setting custom property naming strategy to use.
     */
    public ObjectMapper setPropertyNamingStrategy(PropertyNamingStrategy s) {
        _serializationConfig = _serializationConfig.with(s);
        _deserializationConfig = _deserializationConfig.with(s);
        return this;
    }

    /**
     * @since 2.5
     */
    public PropertyNamingStrategy getPropertyNamingStrategy() {
        // arbitrary choice but let's do:
        return _serializationConfig.getPropertyNamingStrategy();
    }

    /**
     * Method for setting custom accessor naming strategy to use.
     *
     * @since 2.12
     */
    public ObjectMapper setAccessorNaming(AccessorNamingStrategy.Provider s) {
        _serializationConfig = _serializationConfig.with(s);
        _deserializationConfig = _deserializationConfig.with(s);
        return this;
    }

    /**
     * Method for specifying {@link PrettyPrinter} to use when "default pretty-printing"
     * is enabled (by enabling {@link SerializationFeature#INDENT_OUTPUT})
     * 
     * @param pp Pretty printer to use by default.
     * 
     * @return This mapper, useful for call-chaining
     * 
     * @since 2.6
     */
    public ObjectMapper setDefaultPrettyPrinter(PrettyPrinter pp) {
        _serializationConfig = _serializationConfig.withDefaultPrettyPrinter(pp);
        return this;
    }

    /**
     * @deprecated Since 2.6 use {@link #setVisibility(VisibilityChecker)} instead.
     */
    @Deprecated
    public void setVisibilityChecker(VisibilityChecker<?> vc) {
        setVisibility(vc);
    }

    /**
     * Method for specifying {@link PolymorphicTypeValidator} to use for validating
     * polymorphic subtypes used with explicit polymorphic types (annotation-based),
     * but NOT one with "default typing" (see {@link #activateDefaultTyping(PolymorphicTypeValidator)}
     * for details).
     *
     * @since 2.10
     */
    public ObjectMapper setPolymorphicTypeValidator(PolymorphicTypeValidator ptv) {
        BaseSettings s = _deserializationConfig.getBaseSettings().with(ptv);
        _deserializationConfig = _deserializationConfig._withBase(s);
        return this;
    }

    /**
     * Accessor for configured {@link PolymorphicTypeValidator} used for validating
     * polymorphic subtypes used with explicit polymorphic types (annotation-based),
     * but NOT one with "default typing" (see {@link #activateDefaultTyping(PolymorphicTypeValidator)}
     * for details).
     *
     * @since 2.10
     */
    public PolymorphicTypeValidator getPolymorphicTypeValidator() {
        return _deserializationConfig.getBaseSettings().getPolymorphicTypeValidator();
    }

    /*
    /**********************************************************
    /* Configuration: global-default/per-type override settings
    /**********************************************************
     */
    
    /**
     * Convenience method, equivalent to calling:
     *<pre>
     *  setPropertyInclusion(JsonInclude.Value.construct(incl, incl));
     *</pre>
     *<p>
     * NOTE: behavior differs slightly from 2.8, where second argument was
     * implied to be <code>JsonInclude.Include.ALWAYS</code>.
     */
    public ObjectMapper setSerializationInclusion(JsonInclude.Include incl) {
        setPropertyInclusion(JsonInclude.Value.construct(incl, incl));
        return this;
    }

    /**
     * @since 2.7
     * @deprecated Since 2.9 use {@link #setDefaultPropertyInclusion}
     */
    @Deprecated
    public ObjectMapper setPropertyInclusion(JsonInclude.Value incl) {
        return setDefaultPropertyInclusion(incl);
    }

    /**
     * Method for setting default POJO property inclusion strategy for serialization,
     * applied for all properties for which there are no per-type or per-property
     * overrides (via annotations or config overrides).
     *
     * @since 2.9 (basically rename of <code>setPropertyInclusion</code>)
     */
    public ObjectMapper setDefaultPropertyInclusion(JsonInclude.Value incl) {
        _configOverrides.setDefaultInclusion(incl);
        return this;
    }

    /**
     * Short-cut for:
     *<pre>
     *  setDefaultPropertyInclusion(JsonInclude.Value.construct(incl, incl));
     *</pre>
     *
     * @since 2.9 (basically rename of <code>setPropertyInclusion</code>)
     */
    public ObjectMapper setDefaultPropertyInclusion(JsonInclude.Include incl) {
        _configOverrides.setDefaultInclusion(JsonInclude.Value.construct(incl, incl));
        return this;
    }

    /**
     * Method for setting default Setter configuration, regarding things like
     * merging, null-handling; used for properties for which there are
     * no per-type or per-property overrides (via annotations or config overrides).
     *
     * @since 2.9
     */
    public ObjectMapper setDefaultSetterInfo(JsonSetter.Value v) {
        _configOverrides.setDefaultSetterInfo(v);
        return this;
    }

    /**
     * Method for setting auto-detection visibility definition
     * defaults, which are in effect unless overridden by
     * annotations (like <code>JsonAutoDetect</code>) or per-type
     * visibility overrides.
     *
     * @since 2.9
     */
    public ObjectMapper setDefaultVisibility(JsonAutoDetect.Value vis) {
        _configOverrides.setDefaultVisibility(VisibilityChecker.Std.construct(vis));
        return this;
    }

    /**
     * Method for setting default Setter configuration, regarding things like
     * merging, null-handling; used for properties for which there are
     * no per-type or per-property overrides (via annotations or config overrides).
     *
     * @since 2.9
     */
    public ObjectMapper setDefaultMergeable(Boolean b) {
        _configOverrides.setDefaultMergeable(b);
        return this;
    }

    /**
     * @since 2.10
     */
    public ObjectMapper setDefaultLeniency(Boolean b) {
        _configOverrides.setDefaultLeniency(b);
        return this;
    }

    /*
    /**********************************************************
    /* Subtype registration
    /**********************************************************
     */

    /**
     * Method for registering specified class as a subtype, so that
     * typename-based resolution can link supertypes to subtypes
     * (as an alternative to using annotations).
     * Type for given class is determined from appropriate annotation;
     * or if missing, default name (unqualified class name)
     */
    public void registerSubtypes(Class<?>... classes) {
        getSubtypeResolver().registerSubtypes(classes);
    }

    /**
     * Method for registering specified class as a subtype, so that
     * typename-based resolution can link supertypes to subtypes
     * (as an alternative to using annotations).
     * Name may be provided as part of argument, but if not will
     * be based on annotations or use default name (unqualified
     * class name).
     */
    public void registerSubtypes(NamedType... types) {
        getSubtypeResolver().registerSubtypes(types);
    }

    /**
     * @since 2.9
     */
    public void registerSubtypes(Collection<Class<?>> subtypes) {
        getSubtypeResolver().registerSubtypes(subtypes);
    }

    /*
    /**********************************************************
    /* Default typing (automatic polymorphic types): current (2.10)
    /**********************************************************
     */

    /**
     * Convenience method that is equivalent to calling
     *<pre>
     *  activateDefaultTyping(ptv, DefaultTyping.OBJECT_AND_NON_CONCRETE);
     *</pre>
     *<p>
     * NOTE: choice of {@link PolymorphicTypeValidator} to pass is critical for security
     * as allowing all subtypes can be risky for untrusted content.
     *
     * @param ptv Validator used to verify that actual subtypes to deserialize are valid against
     *    whatever criteria validator uses: important in case where untrusted content is deserialized.
     *
     * @since 2.10
     */
    public ObjectMapper activateDefaultTyping(PolymorphicTypeValidator ptv) {
        return activateDefaultTyping(ptv, DefaultTyping.OBJECT_AND_NON_CONCRETE);
    }

    /**
     * Convenience method that is equivalent to calling
     *<pre>
     *  activateDefaultTyping(ptv, dti, JsonTypeInfo.As.WRAPPER_ARRAY);
     *</pre>
     *<p>
     * NOTE: choice of {@link PolymorphicTypeValidator} to pass is critical for security
     * as allowing all subtypes can be risky for untrusted content.
     *
     * @param ptv Validator used to verify that actual subtypes to deserialize are valid against
     *    whatever criteria validator uses: important in case where untrusted content is deserialized.
     * @param applicability Defines kinds of types for which additional type information
     *    is added; see {@link DefaultTyping} for more information.
     *
     * @since 2.10
     */
    public ObjectMapper activateDefaultTyping(PolymorphicTypeValidator ptv,
            DefaultTyping applicability) {
        return activateDefaultTyping(ptv, applicability, JsonTypeInfo.As.WRAPPER_ARRAY);
    }

    /**
     * Method for enabling automatic inclusion of type information ("Default Typing"),
     * needed for proper deserialization of polymorphic types (unless types
     * have been annotated with {@link com.fasterxml.jackson.annotation.JsonTypeInfo}).
     *<P>
     * NOTE: use of {@code JsonTypeInfo.As#EXTERNAL_PROPERTY} <b>NOT SUPPORTED</b>;
     * and attempts of do so will throw an {@link IllegalArgumentException} to make
     * this limitation explicit.
     *<p>
     * NOTE: choice of {@link PolymorphicTypeValidator} to pass is critical for security
     * as allowing all subtypes can be risky for untrusted content.
     *
     * @param ptv Validator used to verify that actual subtypes to deserialize are valid against
     *    whatever criteria validator uses: important in case where untrusted content is deserialized.
     * @param applicability Defines kinds of types for which additional type information
     *    is added; see {@link DefaultTyping} for more information.
     * @param includeAs
     *
     * @since 2.10
     */
    public ObjectMapper activateDefaultTyping(PolymorphicTypeValidator ptv,
            DefaultTyping applicability, JsonTypeInfo.As includeAs)
    {
        // 18-Sep-2014, tatu: Let's add explicit check to ensure no one tries to
        //   use "As.EXTERNAL_PROPERTY", since that will not work (with 2.5+)
        if (includeAs == JsonTypeInfo.As.EXTERNAL_PROPERTY) {
            throw new IllegalArgumentException("Cannot use includeAs of "+includeAs);
        }
        
        TypeResolverBuilder<?> typer = _constructDefaultTypeResolverBuilder(applicability, ptv);
        // we'll always use full class name, when using defaulting
        typer = typer.init(JsonTypeInfo.Id.CLASS, null);
        typer = typer.inclusion(includeAs);
        return setDefaultTyping(typer);
    }

    /**
     * Method for enabling automatic inclusion of type information ("Default Typing")
     * -- needed for proper deserialization of polymorphic types (unless types
     * have been annotated with {@link com.fasterxml.jackson.annotation.JsonTypeInfo}) --
     * using "As.PROPERTY" inclusion mechanism and specified property name
     * to use for inclusion (default being "@class" since default type information
     * always uses class name as type identifier)
     *<p>
     * NOTE: choice of {@link PolymorphicTypeValidator} to pass is critical for security
     * as allowing all subtypes can be risky for untrusted content.
     *
     * @param ptv Validator used to verify that actual subtypes to deserialize are valid against
     *    whatever criteria validator uses: important in case where untrusted content is deserialized.
     * @param applicability Defines kinds of types for which additional type information
     *    is added; see {@link DefaultTyping} for more information.
     * @param propertyName Name of property used for including type id for polymorphic values.
     *
     * @since 2.10
     */
    public ObjectMapper activateDefaultTypingAsProperty(PolymorphicTypeValidator ptv,
            DefaultTyping applicability, String propertyName)
    {
        TypeResolverBuilder<?> typer = _constructDefaultTypeResolverBuilder(applicability,
                ptv);
        // we'll always use full class name, when using defaulting
        typer = typer.init(JsonTypeInfo.Id.CLASS, null);
        typer = typer.inclusion(JsonTypeInfo.As.PROPERTY);
        typer = typer.typeProperty(propertyName);
        return setDefaultTyping(typer);
    }

    /**
     * Method for disabling automatic inclusion of type information; if so, only
     * explicitly annotated types (ones with
     * {@link com.fasterxml.jackson.annotation.JsonTypeInfo}) will have
     * additional embedded type information.
     *
     * @since 2.10
     */
    public ObjectMapper deactivateDefaultTyping() {
        return setDefaultTyping(null);
    }

    /**
     * Method for enabling automatic inclusion of type information ("Default Typing"),
     * using specified handler object for determining which types this affects,
     * as well as details of how information is embedded.
     *<p>
     * NOTE: use of Default Typing can be a potential security risk if incoming
     * content comes from untrusted sources, so care should be taken to use
     * a {@link TypeResolverBuilder} that can limit allowed classes to
     * deserialize. Note in particular that
     * {@link com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder}
     * DOES NOT limit applicability but creates type (de)serializers for all
     * types.
     *
     * @param typer Type information inclusion handler
     */
    public ObjectMapper setDefaultTyping(TypeResolverBuilder<?> typer) {
        _deserializationConfig = _deserializationConfig.with(typer);
        _serializationConfig = _serializationConfig.with(typer);
        return this;
    }

    /*
    /**********************************************************
    /* Default typing (automatic polymorphic types): deprecated (pre-2.10)
    /**********************************************************
     */
    
    /**
     * @deprecated Since 2.10 use {@link #activateDefaultTyping(PolymorphicTypeValidator)} instead
     */
    @Deprecated
    public ObjectMapper enableDefaultTyping() {
        return activateDefaultTyping(getPolymorphicTypeValidator());
    }

    /**
     * @deprecated Since 2.10 use {@link #activateDefaultTyping(PolymorphicTypeValidator,DefaultTyping)} instead
     */
    @Deprecated
    public ObjectMapper enableDefaultTyping(DefaultTyping dti) {
        return enableDefaultTyping(dti, JsonTypeInfo.As.WRAPPER_ARRAY);
    }

    /**
     * @deprecated Since 2.10 use {@link #activateDefaultTyping(PolymorphicTypeValidator,DefaultTyping,JsonTypeInfo.As)} instead
     */
    @Deprecated
    public ObjectMapper enableDefaultTyping(DefaultTyping applicability, JsonTypeInfo.As includeAs) {
        return activateDefaultTyping(getPolymorphicTypeValidator(), applicability, includeAs);
    }

    /**
     * @deprecated Since 2.10 use {@link #activateDefaultTypingAsProperty(PolymorphicTypeValidator,DefaultTyping,String)} instead
     */
    @Deprecated
    public ObjectMapper enableDefaultTypingAsProperty(DefaultTyping applicability, String propertyName) {
        return activateDefaultTypingAsProperty(getPolymorphicTypeValidator(), applicability, propertyName);
    }

    /**
     * @deprecated Since 2.10 use {@link #deactivateDefaultTyping} instead
     */
    @Deprecated
    public ObjectMapper disableDefaultTyping() {
        return setDefaultTyping(null);
    }

    /*
    /**********************************************************
    /* Configuration, config, coercion overrides
    /**********************************************************
     */

    /**
     * Accessor for getting a mutable configuration override object for
     * given type, needed to add or change per-type overrides applied
     * to properties of given type.
     * Usage is through returned object by calling "setter" methods, which
     * directly modify override object and take effect directly.
     * For example you can do
     *<pre>
     *   mapper.configOverride(java.util.Date.class)
     *       .setFormat(JsonFormat.Value.forPattern("yyyy-MM-dd"));
     *</pre>
     * to change the default format to use for properties of type
     * {@link java.util.Date} (possibly further overridden by per-property
     * annotations)
     *
     * @since 2.8
     */
    public MutableConfigOverride configOverride(Class<?> type) {
        return _configOverrides.findOrCreateOverride(type);
    }

    /*
    /**********************************************************
    /* Configuration, coercion config (2.x only)
    /**********************************************************
     */

    /**
     * Accessor for {@link MutableCoercionConfig} through which
     * default (fallback) coercion configurations can be changed.
     * Note that such settings are only applied if more specific
     * (by logical and physical type) configuration have
     * not been defined.
     *
     * @since 2.12
     */
    public MutableCoercionConfig coercionConfigDefaults() {
        return _coercionConfigs.defaultCoercions();
    }

    /**
     * Accessor for {@link MutableCoercionConfig} through which
     * coercion configuration for specified logical target type can be set.
     *
     * @since 2.12
     */
    public MutableCoercionConfig coercionConfigFor(LogicalType logicalType) {
        return _coercionConfigs.findOrCreateCoercion(logicalType);
    }

    /**
     * Accessor for {@link MutableCoercionConfig} through which
     * coercion configuration for specified physical target type can be set.
     *
     * @since 2.12
     */
    public MutableCoercionConfig coercionConfigFor(Class<?> physicalType) {
        return _coercionConfigs.findOrCreateCoercion(physicalType);
    }

    /*
    /**********************************************************
    /* Configuration, basic type handling
    /**********************************************************
     */

    /**
     * Accessor for getting currently configured {@link TypeFactory} instance.
     */
    public TypeFactory getTypeFactory() {
        return _typeFactory;
    }

    /**
     * Method that can be used to override {@link TypeFactory} instance
     * used by this mapper.
     *<p>
     * Note: will also set {@link TypeFactory} that deserialization and
     * serialization config objects use.
     */
    public ObjectMapper setTypeFactory(TypeFactory f)
    {
        _typeFactory = f;
        _deserializationConfig = _deserializationConfig.with(f);
        _serializationConfig = _serializationConfig.with(f);
        return this;
    }

    /**
     * Convenience method for constructing {@link JavaType} out of given
     * type (typically <code>java.lang.Class</code>), but without explicit
     * context.
     */
    public JavaType constructType(Type t) {
        _assertNotNull("t", t);
        return _typeFactory.constructType(t);
    }

    /**
     * Convenience method for constructing {@link JavaType} out of given
     * type reference.
     *
     * @since 2.12
     */
    public JavaType constructType(TypeReference<?> typeRef) {
        _assertNotNull("typeRef", typeRef);
        return _typeFactory.constructType(typeRef);
    }

    /*
    /**********************************************************
    /* Configuration, deserialization
    /**********************************************************
     */

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

    /**
     * Method for specifying {@link JsonNodeFactory} to use for
     * constructing root level tree nodes (via method
     * {@link #createObjectNode}
     */
    public ObjectMapper setNodeFactory(JsonNodeFactory f) {
        _deserializationConfig = _deserializationConfig.with(f);
        return this;
    }

    /**
     * Method for specifying {@link ConstructorDetector} to use for
     * determining some aspects of creator auto-detection (specifically
     * auto-detection of constructor, and in particular behavior with
     * single-argument constructors).
     *
     * @since 2.12
     */
    public ObjectMapper setConstructorDetector(ConstructorDetector cd) {
        _deserializationConfig = _deserializationConfig.with(cd);
        return this;
    }

    /**
     * Method for adding specified {@link DeserializationProblemHandler}
     * to be used for handling specific problems during deserialization.
     */
    public ObjectMapper addHandler(DeserializationProblemHandler h) {
        _deserializationConfig = _deserializationConfig.withHandler(h);
        return this;
    }

    /**
     * Method for removing all registered {@link DeserializationProblemHandler}s
     * instances from this mapper.
     */
    public ObjectMapper clearProblemHandlers() {
        _deserializationConfig = _deserializationConfig.withNoProblemHandlers();
        return this;
    }

    /**
     * Method that allows overriding of the underlying {@link DeserializationConfig}
     * object.
     * It is added as a fallback method that may be used if no other configuration
     * modifier method works: it should not be used if there are alternatives,
     * and its use is generally discouraged.
     *<p>
     * <b>NOTE</b>: only use this method if you know what you are doing -- it allows
     * by-passing some of checks applied to other configuration methods.
     * Also keep in mind that as with all configuration of {@link ObjectMapper},
     * this is only thread-safe if done before calling any deserialization methods.
     * 
     * @since 2.4
     */
    public ObjectMapper setConfig(DeserializationConfig config) {
        _assertNotNull("config", config);
        _deserializationConfig = config;
        return this;
    }

    /*
    /**********************************************************
    /* Configuration, serialization
    /**********************************************************
     */

    /**
     * @deprecated Since 2.6, use {@link #setFilterProvider} instead (allows chaining)
     */
    @Deprecated
    public void setFilters(FilterProvider filterProvider) {
        _serializationConfig = _serializationConfig.withFilters(filterProvider);
    }

    /**
     * Method for configuring this mapper to use specified {@link FilterProvider} for
     * mapping Filter Ids to actual filter instances.
     *<p>
     * Note that usually it is better to use method {@link #writer(FilterProvider)};
     * however, sometimes
     * this method is more convenient. For example, some frameworks only allow configuring
     * of ObjectMapper instances and not {@link ObjectWriter}s.
     * 
     * @since 2.6
     */
    public ObjectMapper setFilterProvider(FilterProvider filterProvider) {
        _serializationConfig = _serializationConfig.withFilters(filterProvider);
        return this;
    }

    /**
     * Method that will configure default {@link Base64Variant} that
     * <code>byte[]</code> serializers and deserializers will use.
     * 
     * @param v Base64 variant to use
     * 
     * @return This mapper, for convenience to allow chaining
     * 
     * @since 2.1
     */
    public ObjectMapper setBase64Variant(Base64Variant v) {
        _serializationConfig = _serializationConfig.with(v);
        _deserializationConfig = _deserializationConfig.with(v);
        return this;
    }

    /**
     * Method that allows overriding of the underlying {@link SerializationConfig}
     * object, which contains serialization-specific configuration settings.
     * It is added as a fallback method that may be used if no other configuration
     * modifier method works: it should not be used if there are alternatives,
     * and its use is generally discouraged.
     *<p>
     * <b>NOTE</b>: only use this method if you know what you are doing -- it allows
     * by-passing some of checks applied to other configuration methods.
     * Also keep in mind that as with all configuration of {@link ObjectMapper},
     * this is only thread-safe if done before calling any serialization methods.
     * 
     * @since 2.4
     */
    public ObjectMapper setConfig(SerializationConfig config) {
        _assertNotNull("config", config);
        _serializationConfig = config;
        return this;
    }
    
    /*
    /**********************************************************
    /* Configuration, other
    /**********************************************************
     */

    /**
     * Method that can be used to get hold of {@link JsonFactory} that this
     * mapper uses if it needs to construct {@link JsonParser}s
     * and/or {@link JsonGenerator}s.
     *<p>
     * WARNING: note that all {@link ObjectReader} and {@link ObjectWriter}
     * instances created by this mapper usually share the same configured
     * {@link JsonFactory}, so changes to its configuration will "leak".
     * To avoid such observed changes you should always use "with()" and
     * "without()" method of {@link ObjectReader} and {@link ObjectWriter}
     * for changing {@link com.fasterxml.jackson.core.JsonParser.Feature}
     * and {@link com.fasterxml.jackson.core.JsonGenerator.Feature}
     * settings to use on per-call basis.
     *
     * @return {@link JsonFactory} that this mapper uses when it needs to
     *   construct Json parser and generators
     *
     * @since 2.10
     */
    public JsonFactory tokenStreamFactory() { return _jsonFactory; }

    @Override
    public JsonFactory getFactory() { return _jsonFactory; }

    /**
     * Method for configuring the default {@link DateFormat} to use when serializing time
     * values as Strings, and deserializing from JSON Strings.
     * This is preferably to directly modifying {@link SerializationConfig} and
     * {@link DeserializationConfig} instances.
     * If you need per-request configuration, use {@link #writer(DateFormat)} to
     * create properly configured {@link ObjectWriter} and use that; this because
     * {@link ObjectWriter}s are thread-safe whereas ObjectMapper itself is only
     * thread-safe when configuring methods (such as this one) are NOT called.
     */
    public ObjectMapper setDateFormat(DateFormat dateFormat)
    {
        _deserializationConfig = _deserializationConfig.with(dateFormat);
        _serializationConfig = _serializationConfig.with(dateFormat);
        return this;
    }

    /**
     * @since 2.5
     */
    public DateFormat getDateFormat() {
        // arbitrary choice but let's do:
        return _serializationConfig.getDateFormat();
    }
    
    /**
     * Method for configuring {@link HandlerInstantiator} to use for creating
     * instances of handlers (such as serializers, deserializers, type and type
     * id resolvers), given a class.
     *
     * @param hi Instantiator to use; if null, use the default implementation
     */
    public Object setHandlerInstantiator(HandlerInstantiator hi)
    {
        _deserializationConfig = _deserializationConfig.with(hi);
        _serializationConfig = _serializationConfig.with(hi);
        return this;
    }
    
    /**
     * Method for configuring {@link InjectableValues} which used to find
     * values to inject.
     */
    public ObjectMapper setInjectableValues(InjectableValues injectableValues) {
        _injectableValues = injectableValues;
        return this;
    }

    /**
     * @since 2.6
     */
    public InjectableValues getInjectableValues() {
        return _injectableValues;
    }

    /**
     * Method for overriding default locale to use for formatting.
     * Default value used is {@link Locale#getDefault()}.
     */
    public ObjectMapper setLocale(Locale l) {
        _deserializationConfig = _deserializationConfig.with(l);
        _serializationConfig = _serializationConfig.with(l);
        return this;
    }

    /**
     * Method for overriding default TimeZone to use for formatting.
     * Default value used is UTC (NOT default TimeZone of JVM).
     */
    public ObjectMapper setTimeZone(TimeZone tz) {
        _deserializationConfig = _deserializationConfig.with(tz);
        _serializationConfig = _serializationConfig.with(tz);
        return this;
    }

    /**
     *<p>
     * NOTE: preferred way to set the defaults is to use {@code Builder} style
     * construction, see {@link com.fasterxml.jackson.databind.json.JsonMapper#builder}
     * (and {@link MapperBuilder#defaultAttributes}).
     *
     * @since 2.13
     */
    public ObjectMapper setDefaultAttributes(ContextAttributes attrs) {
        _deserializationConfig = _deserializationConfig.with(attrs);
        _serializationConfig = _serializationConfig.with(attrs);
        return this;
    }

    /*
    /**********************************************************
    /* Configuration, simple features: MapperFeature
    /**********************************************************
     */

    /**
     * Method for checking whether given {@link MapperFeature} is enabled.
     */
    public boolean isEnabled(MapperFeature f) {
        // ok to use either one, should be kept in sync
        return _serializationConfig.isEnabled(f);
    }

    /**
     * @deprecated Since 2.13 use {@code JsonMapper.builder().configure(...)}
     */
    @Deprecated
    public ObjectMapper configure(MapperFeature f, boolean state) {
        _serializationConfig = state ?
                _serializationConfig.with(f) : _serializationConfig.without(f);
        _deserializationConfig = state ?
                _deserializationConfig.with(f) : _deserializationConfig.without(f);
        return this;
    }

    /**
     * @deprecated Since 2.13 use {@code JsonMapper.builder().enable(...)}
     */
    @Deprecated
    public ObjectMapper enable(MapperFeature... f) {
        _deserializationConfig = _deserializationConfig.with(f);
        _serializationConfig = _serializationConfig.with(f);
        return this;
    }

    /**
     * @deprecated Since 2.13 use {@code JsonMapper.builder().disable(...)}
     */
    @Deprecated
    public ObjectMapper disable(MapperFeature... f) {
        _deserializationConfig = _deserializationConfig.without(f);
        _serializationConfig = _serializationConfig.without(f);
        return this;
    }

    /*
    /**********************************************************
    /* Configuration, simple features: SerializationFeature
    /**********************************************************
     */

    /**
     * Method for checking whether given serialization-specific
     * feature is enabled.
     */
    public boolean isEnabled(SerializationFeature f) {
        return _serializationConfig.isEnabled(f);
    }

    /**
     * Method for changing state of an on/off serialization feature for
     * this object mapper.
     */
    public ObjectMapper configure(SerializationFeature f, boolean state) {
        _serializationConfig = state ?
                _serializationConfig.with(f) : _serializationConfig.without(f);
        return this;
    }

    /**
     * Method for enabling specified {@link DeserializationConfig} feature.
     * Modifies and returns this instance; no new object is created.
     */
    public ObjectMapper enable(SerializationFeature f) {
        _serializationConfig = _serializationConfig.with(f);
        return this;
    }

    /**
     * Method for enabling specified {@link DeserializationConfig} features.
     * Modifies and returns this instance; no new object is created.
     */
    public ObjectMapper enable(SerializationFeature first,
            SerializationFeature... f) {
        _serializationConfig = _serializationConfig.with(first, f);
        return this;
    }
    
    /**
     * Method for enabling specified {@link DeserializationConfig} features.
     * Modifies and returns this instance; no new object is created.
     */
    public ObjectMapper disable(SerializationFeature f) {
        _serializationConfig = _serializationConfig.without(f);
        return this;
    }

    /**
     * Method for enabling specified {@link DeserializationConfig} features.
     * Modifies and returns this instance; no new object is created.
     */
    public ObjectMapper disable(SerializationFeature first,
            SerializationFeature... f) {
        _serializationConfig = _serializationConfig.without(first, f);
        return this;
    }
    
    /*
    /**********************************************************
    /* Configuration, simple features: DeserializationFeature
    /**********************************************************
     */

    /**
     * Method for checking whether given deserialization-specific
     * feature is enabled.
     */
    public boolean isEnabled(DeserializationFeature f) {
        return _deserializationConfig.isEnabled(f);
    }

    /**
     * Method for changing state of an on/off deserialization feature for
     * this object mapper.
     */
    public ObjectMapper configure(DeserializationFeature f, boolean state) {
        _deserializationConfig = state ?
                _deserializationConfig.with(f) : _deserializationConfig.without(f);
        return this;
    }

    /**
     * Method for enabling specified {@link DeserializationConfig} features.
     * Modifies and returns this instance; no new object is created.
     */
    public ObjectMapper enable(DeserializationFeature feature) {
        _deserializationConfig = _deserializationConfig.with(feature);
        return this;
    }

    /**
     * Method for enabling specified {@link DeserializationConfig} features.
     * Modifies and returns this instance; no new object is created.
     */
    public ObjectMapper enable(DeserializationFeature first,
            DeserializationFeature... f) {
        _deserializationConfig = _deserializationConfig.with(first, f);
        return this;
    }
    
    /**
     * Method for enabling specified {@link DeserializationConfig} features.
     * Modifies and returns this instance; no new object is created.
     */
    public ObjectMapper disable(DeserializationFeature feature) {
        _deserializationConfig = _deserializationConfig.without(feature);
        return this;
    }

    /**
     * Method for enabling specified {@link DeserializationConfig} features.
     * Modifies and returns this instance; no new object is created.
     */
    public ObjectMapper disable(DeserializationFeature first,
            DeserializationFeature... f) {
        _deserializationConfig = _deserializationConfig.without(first, f);
        return this;
    }
    
    /*
    /**********************************************************
    /* Configuration, simple features: JsonParser.Feature
    /**********************************************************
     */

    public boolean isEnabled(JsonParser.Feature f) {
        return _deserializationConfig.isEnabled(f, _jsonFactory);
    }

    /**
     * Method for changing state of specified {@link com.fasterxml.jackson.core.JsonParser.Feature}s
     * for parser instances this object mapper creates.
     *<p>
     * Note that this is equivalent to directly calling same method
     * on {@link #getFactory}.
     *<p>
     * WARNING: since this method directly modifies state of underlying {@link JsonFactory},
     * it will change observed configuration by {@link ObjectReader}s as well -- to avoid
     * this, use {@link ObjectReader#with(JsonParser.Feature)} instead.
     */
    public ObjectMapper configure(JsonParser.Feature f, boolean state) {
        _jsonFactory.configure(f, state);
        return this;
    }

    /**
     * Method for enabling specified {@link com.fasterxml.jackson.core.JsonParser.Feature}s
     * for parser instances this object mapper creates.
     *<p>
     * Note that this is equivalent to directly calling same method on {@link #getFactory}.
     *<p>
     * WARNING: since this method directly modifies state of underlying {@link JsonFactory},
     * it will change observed configuration by {@link ObjectReader}s as well -- to avoid
     * this, use {@link ObjectReader#with(JsonParser.Feature)} instead.
     *
     * @since 2.5
     */
    public ObjectMapper enable(JsonParser.Feature... features) {
        for (JsonParser.Feature f : features) {
            _jsonFactory.enable(f);
        }
        return this;
    }
    
    /**
     * Method for disabling specified {@link com.fasterxml.jackson.core.JsonParser.Feature}s
     * for parser instances this object mapper creates.
     *<p>
     * Note that this is equivalent to directly calling same method on {@link #getFactory}.
     *<p>
     * WARNING: since this method directly modifies state of underlying {@link JsonFactory},
     * it will change observed configuration by {@link ObjectReader}s as well -- to avoid
     * this, use {@link ObjectReader#without(JsonParser.Feature)} instead.
     *
     * @since 2.5
     */
    public ObjectMapper disable(JsonParser.Feature... features) {
        for (JsonParser.Feature f : features) {
            _jsonFactory.disable(f);
        }
        return this;
    }
    
    /*
    /**********************************************************
    /* Configuration, simple features: JsonGenerator.Feature
    /**********************************************************
     */

    public boolean isEnabled(JsonGenerator.Feature f) {
        return _serializationConfig.isEnabled(f, _jsonFactory);
    }

    /**
     * Method for changing state of an on/off {@link JsonGenerator} feature for
     * generator instances this object mapper creates.
     *<p>
     * Note that this is equivalent to directly calling same method
     * on {@link #getFactory}.
     *<p>
     * WARNING: since this method directly modifies state of underlying {@link JsonFactory},
     * it will change observed configuration by {@link ObjectWriter}s as well -- to avoid
     * this, use {@link ObjectWriter#with(JsonGenerator.Feature)} instead.
     */
    public ObjectMapper configure(JsonGenerator.Feature f, boolean state) {
        _jsonFactory.configure(f,  state);
        return this;
    }

    /**
     * Method for enabling specified {@link com.fasterxml.jackson.core.JsonGenerator.Feature}s
     * for parser instances this object mapper creates.
     *<p>
     * Note that this is equivalent to directly calling same method on {@link #getFactory}.
     *<p>
     * WARNING: since this method directly modifies state of underlying {@link JsonFactory},
     * it will change observed configuration by {@link ObjectWriter}s as well -- to avoid
     * this, use {@link ObjectWriter#with(JsonGenerator.Feature)} instead.
     *
     * @since 2.5
     */
    public ObjectMapper enable(JsonGenerator.Feature... features) {
        for (JsonGenerator.Feature f : features) {
            _jsonFactory.enable(f);
        }
        return this;
    }

    /**
     * Method for disabling specified {@link com.fasterxml.jackson.core.JsonGenerator.Feature}s
     * for parser instances this object mapper creates.
     *<p>
     * Note that this is equivalent to directly calling same method on {@link #getFactory}.
     *<p>
     * WARNING: since this method directly modifies state of underlying {@link JsonFactory},
     * it will change observed configuration by {@link ObjectWriter}s as well -- to avoid
     * this, use {@link ObjectWriter#without(JsonGenerator.Feature)} instead.
     *
     * @since 2.5
     */
    public ObjectMapper disable(JsonGenerator.Feature... features) {
        for (JsonGenerator.Feature f : features) {
            _jsonFactory.disable(f);
        }
        return this;
    }

    /*
    /**********************************************************
    /* Configuration, simple features: JsonFactory.Feature
    /**********************************************************
     */
    
    /**
     * Convenience method, equivalent to:
     *<pre>
     *  getFactory().isEnabled(f);
     *</pre>
     */
    public boolean isEnabled(JsonFactory.Feature f) {
        return _jsonFactory.isEnabled(f);
    }

    /*
    /**********************************************************
    /* Configuration, 2.10+ stream features
    /**********************************************************
     */

    /**
     * @since 2.10
     */
    public boolean isEnabled(StreamReadFeature f) {
        return isEnabled(f.mappedFeature());
    }

    /**
     * @since 2.10
     */
    public boolean isEnabled(StreamWriteFeature f) {
        return isEnabled(f.mappedFeature());
    }
    
    /*
    /**********************************************************
    /* Public API (from ObjectCodec): deserialization
    /* (mapping from JSON to Java types); main methods
    /**********************************************************
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
     * @throws StreamReadException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws DatabindException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T readValue(JsonParser p, Class<T> valueType)
        throws IOException, StreamReadException, DatabindException
    {
        _assertNotNull("p", p);
        return (T) _readValue(getDeserializationConfig(), p, _typeFactory.constructType(valueType));
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
     * @throws StreamReadException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws DatabindException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T readValue(JsonParser p, TypeReference<T> valueTypeRef)
        throws IOException, StreamReadException, DatabindException
    {
        _assertNotNull("p", p);
        return (T) _readValue(getDeserializationConfig(), p, _typeFactory.constructType(valueTypeRef));
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
     * @throws StreamReadException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws DatabindException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     */
    @Override
    @SuppressWarnings("unchecked")
    public final <T> T readValue(JsonParser p, ResolvedType valueType)
        throws IOException, StreamReadException, DatabindException
    {
        _assertNotNull("p", p);
        return (T) _readValue(getDeserializationConfig(), p, (JavaType) valueType);
    }

    /**
     * Type-safe overloaded method, basically alias for {@link #readValue(JsonParser, Class)}.
     * 
     * @throws IOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws StreamReadException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws DatabindException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(JsonParser p, JavaType valueType)
        throws IOException, StreamReadException, DatabindException
    {
        _assertNotNull("p", p);
        return (T) _readValue(getDeserializationConfig(), p, valueType);
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
     * @throws IOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws StreamReadException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     */
    @Override
    public <T extends TreeNode> T readTree(JsonParser p)
        throws IOException
    {
        _assertNotNull("p", p);
        // Must check for EOF here before calling readValue(), since that'll choke on it otherwise
        DeserializationConfig cfg = getDeserializationConfig();
        JsonToken t = p.currentToken();
        if (t == null) {
            t = p.nextToken();
            if (t == null) {
                return null;
            }
        }
        // NOTE! _readValue() will check for trailing tokens
        JsonNode n = (JsonNode) _readValue(cfg, p, constructType(JsonNode.class));
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
    @Override
    public <T> MappingIterator<T> readValues(JsonParser p, ResolvedType valueType)
        throws IOException
    {
        return readValues(p, (JavaType) valueType);
    }

    /**
     * Convenience method, equivalent in function to:
     *<pre>
     *   readerFor(valueType).readValues(p);
     *</pre>
     *<p>
     * Type-safe overload of {@link #readValues(JsonParser, ResolvedType)}.
     */
    public <T> MappingIterator<T> readValues(JsonParser p, JavaType valueType)
        throws IOException
    {
        _assertNotNull("p", p);
        DeserializationConfig config = getDeserializationConfig();
        DeserializationContext ctxt = createDeserializationContext(p, config);
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
     * Type-safe overload of {@link #readValues(JsonParser, ResolvedType)}.
     */
    @Override
    public <T> MappingIterator<T> readValues(JsonParser p, Class<T> valueType)
        throws IOException
    {
        return readValues(p, _typeFactory.constructType(valueType));
    }

    /**
     * Method for reading sequence of Objects from parser stream.
     */
    @Override
    public <T> MappingIterator<T> readValues(JsonParser p, TypeReference<T> valueTypeRef)
        throws IOException
    {
        return readValues(p, _typeFactory.constructType(valueTypeRef));
    }
    
    /*
    /**********************************************************
    /* Public API not included in ObjectCodec: deserialization
    /* (mapping from JSON to Java types)
    /**********************************************************
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
    public JsonNode readTree(InputStream in) throws IOException
    {
        _assertNotNull("in", in);
        return _readTreeAndClose(_jsonFactory.createParser(in));
    }

    /**
     * Same as {@link #readTree(InputStream)} except content accessed through
     * passed-in {@link Reader}
     */
    public JsonNode readTree(Reader r) throws IOException {
        _assertNotNull("r", r);
        return _readTreeAndClose(_jsonFactory.createParser(r));
    }

    /**
     * Same as {@link #readTree(InputStream)} except content read from
     * passed-in {@link String}
     */
    public JsonNode readTree(String content) throws JsonProcessingException, JsonMappingException
    {
        _assertNotNull("content", content);
        try { // since 2.10 remove "impossible" IOException as per [databind#1675]
            return _readTreeAndClose(_jsonFactory.createParser(content));
        } catch (JsonProcessingException e) {
            throw e;
        } catch (IOException e) { // shouldn't really happen but being declared need to
            throw JsonMappingException.fromUnexpectedIOE(e);
        }
    }

    /**
     * Same as {@link #readTree(InputStream)} except content read from
     * passed-in byte array.
     */
    public JsonNode readTree(byte[] content) throws IOException {
        _assertNotNull("content", content);
        return _readTreeAndClose(_jsonFactory.createParser(content));
    }

    /**
     * Same as {@link #readTree(InputStream)} except content read from
     * passed-in byte array.
     */
    public JsonNode readTree(byte[] content, int offset, int len) throws IOException {
        _assertNotNull("content", content);
        return _readTreeAndClose(_jsonFactory.createParser(content, offset, len));
    }

    /**
     * Same as {@link #readTree(InputStream)} except content read from
     * passed-in {@link File}.
     */
    public JsonNode readTree(File file) throws IOException
    {
        _assertNotNull("file", file);
        return _readTreeAndClose(_jsonFactory.createParser(file));
    }

    /**
     * Same as {@link #readTree(InputStream)} except content read from
     * passed-in {@link URL}.
     *<p>
     * NOTE: handling of {@link java.net.URL} is delegated to
     * {@link JsonFactory#createParser(java.net.URL)} and usually simply
     * calls {@link java.net.URL#openStream()}, meaning no special handling
     * is done. If different HTTP connection options are needed you will need
     * to create {@link java.io.InputStream} separately.
     */
    public JsonNode readTree(URL source) throws IOException
    {
        _assertNotNull("source", source);
        return _readTreeAndClose(_jsonFactory.createParser(source));
    }

    /*
    /**********************************************************
    /* Public API (from ObjectCodec): serialization
    /* (mapping from Java types to Json)
    /**********************************************************
     */

    /**
     * Method that can be used to serialize any Java value as
     * JSON output, using provided {@link JsonGenerator}.
     */
    @Override
    public void writeValue(JsonGenerator g, Object value)
        throws IOException, StreamWriteException, DatabindException
    {
        _assertNotNull("g", g);
        SerializationConfig config = getSerializationConfig();

        /* 12-May-2015/2.6, tatu: Looks like we do NOT want to call the usual
         *    'config.initialize(g)` here, since it is assumed that generator
         *    has been configured by caller. But for some reason we don't
         *    trust indentation settings...
         */
        // 10-Aug-2012, tatu: as per [Issue#12], must handle indentation:
        if (config.isEnabled(SerializationFeature.INDENT_OUTPUT)) {
            if (g.getPrettyPrinter() == null) {
                g.setPrettyPrinter(config.constructDefaultPrettyPrinter());
            }
        }
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
    /* Public API (from TreeCodec via ObjectCodec): Tree Model support
    /**********************************************************
     */

    @Override
    public void writeTree(JsonGenerator g, TreeNode rootNode)
        throws IOException
    {
        _assertNotNull("g", g);
        SerializationConfig config = getSerializationConfig();
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
        throws IOException
    {
        _assertNotNull("g", g);
        SerializationConfig config = getSerializationConfig();
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

    @Override // since 2.10
    public JsonNode missingNode() {
        return _deserializationConfig.getNodeFactory().missingNode();
    }

    @Override // since 2.10
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
        return new TreeTraversingParser((JsonNode) n, this);
    }

    /**
     * Convenience conversion method that will bind data given JSON tree
     * contains into specific value (usually bean) type.
     *<p>
     * Functionally equivalent to:
     *<pre>
     *   objectMapper.convertValue(n, valueClass);
     *</pre>
     *<p>
     * Note: inclusion of {@code throws JsonProcessingException} is not accidental
     * since while there can be no input decoding problems, it is possible that content
     * does not match target type: in such case various {@link DatabindException}s
     * are possible. In addition {@link IllegalArgumentException} is possible in some
     * cases, depending on whether {@link DeserializationFeature#WRAP_EXCEPTIONS}
     * is enabled or not.
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T treeToValue(TreeNode n, Class<T> valueType)
        throws IllegalArgumentException,
            JsonProcessingException
    {
        if (n == null) {
            return null;
        }
        try {
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
        } catch (JsonProcessingException e) {
            // 12-Nov-2020, tatu: These can legit happen, during conversion, especially
            //   with things like Builders that validate arguments.
            throw e;
        } catch (IOException e) { // should not occur, no real i/o...
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /**
     * Same as {@link #treeToValue(TreeNode, Class)} but target type specified
     * using fully resolved {@link JavaType}.
     *
     * @since 2.13
     */
    @SuppressWarnings("unchecked")
    public <T> T treeToValue(TreeNode n, JavaType valueType)
        throws IllegalArgumentException,
            JsonProcessingException
    {
        // Implementation copied from the type-erased variant
        if (n == null) {
            return null;
        }
        try {
            if (valueType.isTypeOrSubTypeOf(TreeNode.class)
                    && valueType.isTypeOrSuperTypeOf(n.getClass())) {
                return (T) n;
            }
            final JsonToken tt = n.asToken();
            if (tt == JsonToken.VALUE_EMBEDDED_OBJECT) {
                if (n instanceof POJONode) {
                    Object ob = ((POJONode) n).getPojo();
                    if ((ob == null) || valueType.isTypeOrSuperTypeOf(ob.getClass())) {
                        return (T) ob;
                    }
                }
            }
            return (T) readValue(treeAsTokens(n), valueType);
        } catch (JsonProcessingException e) {
            // 12-Nov-2020, tatu: These can legit happen, during conversion, especially
            //   with things like Builders that validate arguments.
            throw e;
        } catch (IOException e) { // should not occur, no real i/o...
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /**
     * Method that is reverse of {@link #treeToValue}: it
     * will convert given Java value (usually bean) into its
     * equivalent Tree mode {@link JsonNode} representation.
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
    @SuppressWarnings({ "unchecked", "resource" })
    public <T extends JsonNode> T valueToTree(Object fromValue)
        throws IllegalArgumentException
    {
        // [databind#2430]: `null` should become "null node":
        if (fromValue == null) {
            return (T) getNodeFactory().nullNode();
        }

        // inlined 'writeValue' with minor changes:
        // first: disable wrapping when writing
        final SerializationConfig config = getSerializationConfig().without(SerializationFeature.WRAP_ROOT_VALUE);
        final DefaultSerializerProvider context = _serializerProvider(config);
        
        // Then create TokenBuffer to use as JsonGenerator
        TokenBuffer buf = context.bufferForValueConversion(this);
        if (isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)) {
            buf = buf.forceUseOfBigDecimal(true);
        }
        try {
            context.serializeValue(buf, fromValue);
            try (JsonParser p = buf.asParser()) {
                return readTree(p);
            }
        } catch (IOException e) { // should not occur, no real i/o...
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /*
    /**********************************************************
    /* Extended Public API, accessors
    /**********************************************************
     */

    /**
     * Method that can be called to check whether mapper thinks
     * it could serialize an instance of given Class.
     * Check is done
     * by checking whether a serializer can be found for the type.
     *<p>
     * NOTE: since this method does NOT throw exceptions, but internal
     * processing may, caller usually has little information as to why
     * serialization would fail. If you want access to internal {@link Exception},
     * call {@link #canSerialize(Class, AtomicReference)} instead.
     *
     * @return True if mapper can find a serializer for instances of
     *  given class (potentially serializable), false otherwise (not
     *  serializable)
     */
    public boolean canSerialize(Class<?> type) {
        return _serializerProvider(getSerializationConfig()).hasSerializerFor(type, null);
    }

    /**
     * Method similar to {@link #canSerialize(Class)} but that can return
     * actual {@link Throwable} that was thrown when trying to construct
     * serializer: this may be useful in figuring out what the actual problem is.
     * 
     * @since 2.3
     */
    public boolean canSerialize(Class<?> type, AtomicReference<Throwable> cause) {
        return _serializerProvider(getSerializationConfig()).hasSerializerFor(type, cause);
    }
    
    /**
     * Method that can be called to check whether mapper thinks
     * it could deserialize an Object of given type.
     * Check is done by checking whether a registered deserializer can
     * be found or built for the type; if not (either by no mapping being
     * found, or through an <code>Exception</code> being thrown, false
     * is returned.
     *<p>
     * <b>NOTE</b>: in case an exception is thrown during course of trying
     * co construct matching deserializer, it will be effectively swallowed.
     * If you want access to that exception, call
     * {@link #canDeserialize(JavaType, AtomicReference)} instead.
     *
     * @return True if mapper can find a serializer for instances of
     *  given class (potentially serializable), false otherwise (not
     *  serializable)
     */
    public boolean canDeserialize(JavaType type)
    {
        return createDeserializationContext(null,
                getDeserializationConfig()).hasValueDeserializerFor(type, null);
    }

    /**
     * Method similar to {@link #canDeserialize(JavaType)} but that can return
     * actual {@link Throwable} that was thrown when trying to construct
     * serializer: this may be useful in figuring out what the actual problem is.
     * 
     * @since 2.3
     */
    public boolean canDeserialize(JavaType type, AtomicReference<Throwable> cause)
    {
        return createDeserializationContext(null,
                getDeserializationConfig()).hasValueDeserializerFor(type, cause);
    }

    /*
    /**********************************************************
    /* Extended Public API, deserialization,
    /* convenience methods
    /**********************************************************
     */

    /**
     * Method to deserialize JSON content from given file into given Java type.
     * 
     * @throws IOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws StreamReadException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws DatabindException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(File src, Class<T> valueType)
        throws IOException, StreamReadException, DatabindException
    {
        _assertNotNull("src", src);
        return (T) _readMapAndClose(_jsonFactory.createParser(src), _typeFactory.constructType(valueType));
    } 

    /**
     * Method to deserialize JSON content from given file into given Java type.
     * 
     * @throws IOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws StreamReadException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws DatabindException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     */
    @SuppressWarnings({ "unchecked" })
    public <T> T readValue(File src, TypeReference<T> valueTypeRef)
        throws IOException, StreamReadException, DatabindException
    {
        _assertNotNull("src", src);
        return (T) _readMapAndClose(_jsonFactory.createParser(src), _typeFactory.constructType(valueTypeRef));
    } 

    /**
     * Method to deserialize JSON content from given file into given Java type.
     * 
     * @throws IOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws StreamReadException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws DatabindException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(File src, JavaType valueType)
        throws IOException, StreamReadException, DatabindException
    {
        _assertNotNull("src", src);
        return (T) _readMapAndClose(_jsonFactory.createParser(src), valueType);
    }

    /**
     * Method to deserialize JSON content from given resource into given Java type.
     *<p>
     * NOTE: handling of {@link java.net.URL} is delegated to
     * {@link JsonFactory#createParser(java.net.URL)} and usually simply
     * calls {@link java.net.URL#openStream()}, meaning no special handling
     * is done. If different HTTP connection options are needed you will need
     * to create {@link java.io.InputStream} separately.
     * 
     * @throws IOException if a low-level I/O problem (unexpected end-of-input,
     *   network error) occurs (passed through as-is without additional wrapping -- note
     *   that this is one case where {@link DeserializationFeature#WRAP_EXCEPTIONS}
     *   does NOT result in wrapping of exception even if enabled)
     * @throws StreamReadException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws DatabindException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(URL src, Class<T> valueType)
        throws IOException, StreamReadException, DatabindException
    {
        _assertNotNull("src", src);
        return (T) _readMapAndClose(_jsonFactory.createParser(src), _typeFactory.constructType(valueType));
    } 

    /**
     * Same as {@link #readValue(java.net.URL, Class)} except that target specified by {@link TypeReference}.
     */
    @SuppressWarnings({ "unchecked" })
    public <T> T readValue(URL src, TypeReference<T> valueTypeRef)
        throws IOException, StreamReadException, DatabindException
    {
        _assertNotNull("src", src);
        return (T) _readMapAndClose(_jsonFactory.createParser(src), _typeFactory.constructType(valueTypeRef));
    } 

    /**
     * Same as {@link #readValue(java.net.URL, Class)} except that target specified by {@link JavaType}.
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(URL src, JavaType valueType)
        throws IOException, StreamReadException, DatabindException
    {
        _assertNotNull("src", src);
        return (T) _readMapAndClose(_jsonFactory.createParser(src), valueType);
    }

    /**
     * Method to deserialize JSON content from given JSON content String.
     *
     * @throws StreamReadException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws DatabindException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     */
    public <T> T readValue(String content, Class<T> valueType)
        throws JsonProcessingException, JsonMappingException
    {
        _assertNotNull("content", content);
        return readValue(content, _typeFactory.constructType(valueType));
    } 

    /**
     * Method to deserialize JSON content from given JSON content String.
     *
     * @throws StreamReadException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws DatabindException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     */
    public <T> T readValue(String content, TypeReference<T> valueTypeRef)
        throws JsonProcessingException, JsonMappingException
    {
        _assertNotNull("content", content);
        return readValue(content, _typeFactory.constructType(valueTypeRef));
    } 

    /**
     * Method to deserialize JSON content from given JSON content String.
     *
     * @throws StreamReadException if underlying input contains invalid content
     *    of type {@link JsonParser} supports (JSON for default case)
     * @throws DatabindException if the input JSON structure does not match structure
     *   expected for result type (or has other mismatch issues)
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(String content, JavaType valueType)
        throws JsonProcessingException, JsonMappingException
    {
        _assertNotNull("content", content);
        try { // since 2.10 remove "impossible" IOException as per [databind#1675]
            return (T) _readMapAndClose(_jsonFactory.createParser(content), valueType);
        } catch (JsonProcessingException e) {
            throw e;
        } catch (IOException e) { // shouldn't really happen but being declared need to
            throw JsonMappingException.fromUnexpectedIOE(e);
        }
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(Reader src, Class<T> valueType)
        throws IOException, StreamReadException, DatabindException
    {
        _assertNotNull("src", src);
        return (T) _readMapAndClose(_jsonFactory.createParser(src), _typeFactory.constructType(valueType));
    } 

    @SuppressWarnings({ "unchecked" })
    public <T> T readValue(Reader src, TypeReference<T> valueTypeRef)
        throws IOException, StreamReadException, DatabindException
    {
        _assertNotNull("src", src);
        return (T) _readMapAndClose(_jsonFactory.createParser(src), _typeFactory.constructType(valueTypeRef));
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(Reader src, JavaType valueType)
        throws IOException, StreamReadException, DatabindException
    {
        _assertNotNull("src", src);
        return (T) _readMapAndClose(_jsonFactory.createParser(src), valueType);
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(InputStream src, Class<T> valueType)
        throws IOException, StreamReadException, DatabindException
    {
        _assertNotNull("src", src);
        return (T) _readMapAndClose(_jsonFactory.createParser(src), _typeFactory.constructType(valueType));
    } 

    @SuppressWarnings({ "unchecked" })
    public <T> T readValue(InputStream src, TypeReference<T> valueTypeRef)
        throws IOException, StreamReadException, DatabindException
    {
        _assertNotNull("src", src);
        return (T) _readMapAndClose(_jsonFactory.createParser(src), _typeFactory.constructType(valueTypeRef));
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(InputStream src, JavaType valueType)
        throws IOException, StreamReadException, DatabindException
    {
        _assertNotNull("src", src);
        return (T) _readMapAndClose(_jsonFactory.createParser(src), valueType);
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(byte[] src, Class<T> valueType)
        throws IOException, StreamReadException, DatabindException
    {
        _assertNotNull("src", src);
        return (T) _readMapAndClose(_jsonFactory.createParser(src), _typeFactory.constructType(valueType));
    } 
    
    @SuppressWarnings("unchecked")
    public <T> T readValue(byte[] src, int offset, int len, 
                               Class<T> valueType)
        throws IOException, StreamReadException, DatabindException
    {
        _assertNotNull("src", src);
        return (T) _readMapAndClose(_jsonFactory.createParser(src, offset, len), _typeFactory.constructType(valueType));
    } 

    @SuppressWarnings({ "unchecked" })
    public <T> T readValue(byte[] src, TypeReference<T> valueTypeRef)
        throws IOException, StreamReadException, DatabindException
    {
        _assertNotNull("src", src);
        return (T) _readMapAndClose(_jsonFactory.createParser(src), _typeFactory.constructType(valueTypeRef));
    } 
    
    @SuppressWarnings({ "unchecked" })
    public <T> T readValue(byte[] src, int offset, int len, TypeReference<T> valueTypeRef)
        throws IOException, StreamReadException, DatabindException
    {
        _assertNotNull("src", src);
        return (T) _readMapAndClose(_jsonFactory.createParser(src, offset, len), _typeFactory.constructType(valueTypeRef));
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(byte[] src, JavaType valueType)
        throws IOException, StreamReadException, DatabindException
    {
        _assertNotNull("src", src);
        return (T) _readMapAndClose(_jsonFactory.createParser(src), valueType);
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(byte[] src, int offset, int len, JavaType valueType)
        throws IOException, StreamReadException, DatabindException
    {
        _assertNotNull("src", src);
        return (T) _readMapAndClose(_jsonFactory.createParser(src, offset, len), valueType);
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(DataInput src, Class<T> valueType) throws IOException
    {
        _assertNotNull("src", src);
        return (T) _readMapAndClose(_jsonFactory.createParser(src),
                _typeFactory.constructType(valueType));
    }

    @SuppressWarnings("unchecked")
    public <T> T readValue(DataInput src, JavaType valueType) throws IOException
    {
        _assertNotNull("src", src);
        return (T) _readMapAndClose(_jsonFactory.createParser(src), valueType);
    }

    /*
    /**********************************************************
    /* Extended Public API: serialization
    /* (mapping from Java types to JSON)
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
     * @since 2.8
     */
    public void writeValue(DataOutput out, Object value) throws IOException
    {
        _writeValueAndClose(createGenerator(out), value);
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
            _writeValueAndClose(createGenerator(sw), value);
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
     *<p>
     * Note: prior to version 2.1, throws clause included {@link IOException}; 2.1 removed it.
     */
    public byte[] writeValueAsBytes(Object value)
        throws JsonProcessingException
    {
        // Although 'close()' is NOP, use auto-close to avoid lgtm complaints
        try (ByteArrayBuilder bb = new ByteArrayBuilder(_jsonFactory._getBufferRecycler())) {
            _writeValueAndClose(createGenerator(bb, JsonEncoding.UTF8), value);
            final byte[] result = bb.toByteArray();
            bb.release();
            return result;
        } catch (JsonProcessingException e) { // to support [JACKSON-758]
            throw e;
        } catch (IOException e) { // shouldn't really happen, but is declared as possibility so:
            throw JsonMappingException.fromUnexpectedIOE(e);
        }
    }

    /*
    /**********************************************************
    /* Extended Public API: constructing ObjectWriters
    /* for more advanced configuration
    /**********************************************************
     */

    /**
     * Convenience method for constructing {@link ObjectWriter}
     * with default settings.
     */
    public ObjectWriter writer() {
        return _newWriter(getSerializationConfig());
    }

    /**
     * Factory method for constructing {@link ObjectWriter} with
     * specified feature enabled (compared to settings that this
     * mapper instance has).
     */
    public ObjectWriter writer(SerializationFeature feature) {
        return _newWriter(getSerializationConfig().with(feature));
    }

    /**
     * Factory method for constructing {@link ObjectWriter} with
     * specified features enabled (compared to settings that this
     * mapper instance has).
     */
    public ObjectWriter writer(SerializationFeature first,
            SerializationFeature... other) {
        return _newWriter(getSerializationConfig().with(first, other));
    }
    
    /**
     * Factory method for constructing {@link ObjectWriter} that will
     * serialize objects using specified {@link DateFormat}; or, if
     * null passed, using timestamp (64-bit number.
     */
    public ObjectWriter writer(DateFormat df) {
        return _newWriter(getSerializationConfig().with(df));
    }
    
    /**
     * Factory method for constructing {@link ObjectWriter} that will
     * serialize objects using specified JSON View (filter).
     */
    public ObjectWriter writerWithView(Class<?> serializationView) {
        return _newWriter(getSerializationConfig().withView(serializationView));
    }
    
    /**
     * Factory method for constructing {@link ObjectWriter} that will
     * serialize objects using specified root type, instead of actual
     * runtime type of value. Type must be a super-type of runtime type.
     *<p>
     * Main reason for using this method is performance, as writer is able
     * to pre-fetch serializer to use before write, and if writer is used
     * more than once this avoids addition per-value serializer lookups.
     * 
     * @since 2.5
     */
    public ObjectWriter writerFor(Class<?> rootType) {
        return _newWriter(getSerializationConfig(),
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
     * 
     * @since 2.5
     */
    public ObjectWriter writerFor(TypeReference<?> rootType) {
        return _newWriter(getSerializationConfig(),
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
     * 
     * @since 2.5
     */
    public ObjectWriter writerFor(JavaType rootType) {
        return _newWriter(getSerializationConfig(), rootType, /*PrettyPrinter*/null);
    }

    /**
     * Factory method for constructing {@link ObjectWriter} that will
     * serialize objects using specified pretty printer for indentation
     * (or if null, no pretty printer)
     */
    public ObjectWriter writer(PrettyPrinter pp) {
        if (pp == null) { // need to use a marker to indicate explicit disabling of pp
            pp = ObjectWriter.NULL_PRETTY_PRINTER;
        }
        return _newWriter(getSerializationConfig(), /*root type*/ null, pp);
    }
    
    /**
     * Factory method for constructing {@link ObjectWriter} that will
     * serialize objects using the default pretty printer for indentation
     */
    public ObjectWriter writerWithDefaultPrettyPrinter() {
        SerializationConfig config = getSerializationConfig();
        return _newWriter(config,
                /*root type*/ null, config.getDefaultPrettyPrinter());
    }
    
    /**
     * Factory method for constructing {@link ObjectWriter} that will
     * serialize objects using specified filter provider.
     */
    public ObjectWriter writer(FilterProvider filterProvider) {
        return _newWriter(getSerializationConfig().withFilters(filterProvider));
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
        return _newWriter(getSerializationConfig(), schema);
    }

    /**
     * Factory method for constructing {@link ObjectWriter} that will
     * use specified Base64 encoding variant for Base64-encoded binary data.
     * 
     * @since 2.1
     */
    public ObjectWriter writer(Base64Variant defaultBase64) {
        return _newWriter(getSerializationConfig().with(defaultBase64));
    }

    /**
     * Factory method for constructing {@link ObjectReader} that will
     * use specified character escaping details for output.
     * 
     * @since 2.3
     */
    public ObjectWriter writer(CharacterEscapes escapes) {
        return _newWriter(getSerializationConfig()).with(escapes);
    }

    /**
     * Factory method for constructing {@link ObjectWriter} that will
     * use specified default attributes.
     * 
     * @since 2.3
     */
    public ObjectWriter writer(ContextAttributes attrs) {
        return _newWriter(getSerializationConfig().with(attrs));
    }

    /**
     * @deprecated Since 2.5, use {@link #writerFor(Class)} instead
     */
    @Deprecated
    public ObjectWriter writerWithType(Class<?> rootType) {
        return _newWriter(getSerializationConfig(),
                // 15-Mar-2013, tatu: Important! Indicate that static typing is needed:
                ((rootType == null) ? null :_typeFactory.constructType(rootType)),
                /*PrettyPrinter*/null);
    }

    /**
     * @deprecated Since 2.5, use {@link #writerFor(TypeReference)} instead
     */
    @Deprecated
    public ObjectWriter writerWithType(TypeReference<?> rootType) {
        return _newWriter(getSerializationConfig(),
                // 15-Mar-2013, tatu: Important! Indicate that static typing is needed:
                ((rootType == null) ? null : _typeFactory.constructType(rootType)),
                /*PrettyPrinter*/null);
    }

    /**
     * @deprecated Since 2.5, use {@link #writerFor(JavaType)} instead
     */
    @Deprecated
    public ObjectWriter writerWithType(JavaType rootType) {
        return _newWriter(getSerializationConfig(), rootType, /*PrettyPrinter*/null);
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
        return _newReader(getDeserializationConfig()).with(_injectableValues);
    }

    /**
     * Factory method for constructing {@link ObjectReader} with
     * specified feature enabled (compared to settings that this
     * mapper instance has).
     * Note that the resulting instance is NOT usable as is,
     * without defining expected value type.
     */
    public ObjectReader reader(DeserializationFeature feature) {
        return _newReader(getDeserializationConfig().with(feature));
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
        return _newReader(getDeserializationConfig().with(first, other));
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
        return _newReader(getDeserializationConfig(), t, valueToUpdate,
                null, _injectableValues);
    }

    /**
     * Factory method for constructing {@link ObjectReader} that will
     * read or update instances of specified type
     * 
     * @since 2.6
     */
    public ObjectReader readerFor(JavaType type) {
        return _newReader(getDeserializationConfig(), type, null,
                null, _injectableValues);
    }

    /**
     * Factory method for constructing {@link ObjectReader} that will
     * read or update instances of specified type
     * 
     * @since 2.6
     */
    public ObjectReader readerFor(Class<?> type) {
        return _newReader(getDeserializationConfig(), _typeFactory.constructType(type), null,
                null, _injectableValues);
    }

    /**
     * Factory method for constructing {@link ObjectReader} that will
     * read or update instances of specified type
     * 
     * @since 2.6
     */
    public ObjectReader readerFor(TypeReference<?> type) {
        return _newReader(getDeserializationConfig(), _typeFactory.constructType(type), null,
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
        return _newReader(getDeserializationConfig(),
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
        return _newReader(getDeserializationConfig(),
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
        return _newReader(getDeserializationConfig(),
                _typeFactory.constructMapType(Map.class, String.class, type), null,
                null, _injectableValues);
    }

    /**
     * Factory method for constructing {@link ObjectReader} that will
     * use specified {@link JsonNodeFactory} for constructing JSON trees.
     */
    public ObjectReader reader(JsonNodeFactory f) {
        return _newReader(getDeserializationConfig()).with(f);
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
        return _newReader(getDeserializationConfig(), null, null,
                schema, _injectableValues);
    }

    /**
     * Factory method for constructing {@link ObjectReader} that will
     * use specified injectable values.
     * 
     * @param injectableValues Injectable values to use
     */
    public ObjectReader reader(InjectableValues injectableValues) {
        return _newReader(getDeserializationConfig(), null, null,
                null, injectableValues);
    }

    /**
     * Factory method for constructing {@link ObjectReader} that will
     * deserialize objects using specified JSON View (filter).
     */
    public ObjectReader readerWithView(Class<?> view) {
        return _newReader(getDeserializationConfig().withView(view));
    }

    /**
     * Factory method for constructing {@link ObjectReader} that will
     * use specified Base64 encoding variant for Base64-encoded binary data.
     * 
     * @since 2.1
     */
    public ObjectReader reader(Base64Variant defaultBase64) {
        return _newReader(getDeserializationConfig().with(defaultBase64));
    }

    /**
     * Factory method for constructing {@link ObjectReader} that will
     * use specified default attributes.
     * 
     * @since 2.3
     */
    public ObjectReader reader(ContextAttributes attrs) {
        return _newReader(getDeserializationConfig().with(attrs));
    }

    /**
     * @deprecated Since 2.5, use {@link #readerFor(JavaType)} instead
     */
    @Deprecated
    public ObjectReader reader(JavaType type) {
        return _newReader(getDeserializationConfig(), type, null,
                null, _injectableValues);
    }

    /**
     * @deprecated Since 2.5, use {@link #readerFor(Class)} instead
     */
    @Deprecated
    public ObjectReader reader(Class<?> type) {
        return _newReader(getDeserializationConfig(), _typeFactory.constructType(type), null,
                null, _injectableValues);
    }

    /**
     * @deprecated Since 2.5, use {@link #readerFor(TypeReference)} instead
     */
    @Deprecated
    public ObjectReader reader(TypeReference<?> type) {
        return _newReader(getDeserializationConfig(), _typeFactory.constructType(type), null,
                null, _injectableValues);
    }

    /*
    /**********************************************************
    /* Extended Public API: convenience type conversion
    /**********************************************************
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
     * Finally, this functionality is not designed to support "advanced" use
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
        // inlined 'writeValue' with minor changes:
        // first: disable wrapping when writing
        final SerializationConfig config = getSerializationConfig().without(SerializationFeature.WRAP_ROOT_VALUE);
        final DefaultSerializerProvider context = _serializerProvider(config);
        
        // Then create TokenBuffer to use as JsonGenerator
        TokenBuffer buf = context.bufferForValueConversion(this);
        if (isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)) {
            buf = buf.forceUseOfBigDecimal(true);
        }
        try {
            // no need to check for closing of TokenBuffer
            context.serializeValue(buf, fromValue);

            // then matching read, inlined 'readValue' with minor mods:
            final JsonParser p = buf.asParser();
            Object result;
            // ok to pass in existing feature flags; unwrapping handled by mapper
            final DeserializationConfig deserConfig = getDeserializationConfig();
            JsonToken t = _initForReading(p, toValueType);
            if (t == JsonToken.VALUE_NULL) {
                DeserializationContext ctxt = createDeserializationContext(p, deserConfig);
                result = _findRootDeserializer(ctxt, toValueType).getNullValue(ctxt);
            } else if (t == JsonToken.END_ARRAY || t == JsonToken.END_OBJECT) {
                result = null;
            } else { // pointing to event other than null
                DeserializationContext ctxt = createDeserializationContext(p, deserConfig);
                JsonDeserializer<Object> deser = _findRootDeserializer(ctxt, toValueType);
                // note: no handling of unwrapping
                result = deser.deserialize(p, ctxt);
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
     * {@link #configOverride(Class)} and {@link #setDefaultMergeable(Boolean)}).
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
     * 
     * @since 2.9
     */
    @SuppressWarnings("resource")
    public <T> T updateValue(T valueToUpdate, Object overrides)
        throws JsonMappingException
    {
        T result = valueToUpdate;
        if ((valueToUpdate != null) && (overrides != null)) {
            final SerializationConfig config = getSerializationConfig().
                    without(SerializationFeature.WRAP_ROOT_VALUE);
            final DefaultSerializerProvider context = _serializerProvider(config);
            // Then create TokenBuffer to use as JsonGenerator
            TokenBuffer buf = context.bufferForValueConversion(this);
            if (isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)) {
                buf = buf.forceUseOfBigDecimal(true);
            }
            try {
                context.serializeValue(buf, overrides);
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
        }
        return result;
    }

    /*
    /**********************************************************
    /* Extended Public API: JSON Schema generation
    /**********************************************************
     */

    /**
     * Generate <a href="http://json-schema.org/">Json-schema</a>
     * instance for specified class.
     *
     * @param t The class to generate schema for
     * @return Constructed JSON schema.
     * 
     * @deprecated Since 2.6 use external JSON Schema generator (https://github.com/FasterXML/jackson-module-jsonSchema)
     *    (which under the hood calls {@link #acceptJsonFormatVisitor(JavaType, JsonFormatVisitorWrapper)})
     */
    @Deprecated
    public com.fasterxml.jackson.databind.jsonschema.JsonSchema generateJsonSchema(Class<?> t)
            throws JsonMappingException {
        return _serializerProvider(getSerializationConfig()).generateJsonSchema(t);
    }

    /**
     * Method for visiting type hierarchy for given type, using specified visitor.
     *<p>
     * This method can be used for things like
     * generating <a href="http://json-schema.org/">JSON Schema</a>
     * instance for specified type.
     *
     * @param type Type to generate schema for (possibly with generic signature)
     * 
     * @since 2.1
     */
    public void acceptJsonFormatVisitor(Class<?> type, JsonFormatVisitorWrapper visitor)
        throws JsonMappingException
    {
        acceptJsonFormatVisitor(_typeFactory.constructType(type), visitor);
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
     * 
     * @since 2.1
     */
    public void acceptJsonFormatVisitor(JavaType type, JsonFormatVisitorWrapper visitor)
        throws JsonMappingException
    {
        if (type == null) {
            throw new IllegalArgumentException("type must be provided");
        }
        _serializerProvider(getSerializationConfig()).acceptJsonFormatVisitor(type, visitor);
    }

    /*
    /**********************************************************
    /* Internal factory methods for type ids, overridable
    /**********************************************************
     */

    /**
     * Overridable factory method, separate to allow format-specific mappers (and specifically
     * XML-backed one, currently) to offer custom {@link TypeResolverBuilder} subtypes.
     *
     * @since 2.10
     */
    protected TypeResolverBuilder<?> _constructDefaultTypeResolverBuilder(DefaultTyping applicability,
            PolymorphicTypeValidator ptv) {
        return DefaultTypeResolverBuilder.construct(applicability, ptv);
    }

    /*
    /**********************************************************
    /* Internal methods for serialization, overridable
    /**********************************************************
     */

    /**
     * Overridable helper method used for constructing
     * {@link SerializerProvider} to use for serialization.
     */
    protected DefaultSerializerProvider _serializerProvider(SerializationConfig config) {
        return _serializerProvider.createInstance(config, _serializerFactory);
    }

    /**
     * Method called to configure the generator as necessary and then
     * call write functionality
     *
     * @since 2.11.2
     */
    protected final void _writeValueAndClose(JsonGenerator g, Object value)
        throws IOException
    {
        SerializationConfig cfg = getSerializationConfig();
        if (cfg.isEnabled(SerializationFeature.CLOSE_CLOSEABLE) && (value instanceof Closeable)) {
            _writeCloseable(g, value, cfg);
            return;
        }
        try {
            _serializerProvider(cfg).serializeValue(g, value);
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
    private final void _writeCloseable(JsonGenerator g, Object value, SerializationConfig cfg)
        throws IOException
    {
        Closeable toClose = (Closeable) value;
        try {
            _serializerProvider(cfg).serializeValue(g, value);
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
    private final void _writeCloseableValue(JsonGenerator g, Object value, SerializationConfig cfg)
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

    /**
     * @deprecated Since 2.11.2 Use {@link #_writeValueAndClose} instead
     */
    @Deprecated // since 2.11.2 (to remove earliest from 2.13)
    protected final void _configAndWriteValue(JsonGenerator g, Object value) throws IOException {
        getSerializationConfig().initialize(g);
        _writeValueAndClose(g, value);
    }

    /*
    /**********************************************************
    /* Internal methods for deserialization, overridable
    /**********************************************************
     */

    /**
     * Actual implementation of value reading+binding operation.
     */
    protected Object _readValue(DeserializationConfig cfg, JsonParser p,
            JavaType valueType)
        throws IOException
    {
        // First: may need to read the next token, to initialize
        // state (either before first read from parser, or after
        // previous token has been cleared)
        final Object result;
        JsonToken t = _initForReading(p, valueType);
        final DefaultDeserializationContext ctxt = createDeserializationContext(p, cfg);
        if (t == JsonToken.VALUE_NULL) {
            // Ask JsonDeserializer what 'null value' to use:
            result = _findRootDeserializer(ctxt, valueType).getNullValue(ctxt);
        } else if (t == JsonToken.END_ARRAY || t == JsonToken.END_OBJECT) {
            result = null;
        } else { // pointing to event other than null
            result = ctxt.readRootValue(p, valueType, _findRootDeserializer(ctxt, valueType), null);
        }
        // Need to consume the token too
        p.clearCurrentToken();
        if (cfg.isEnabled(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)) {
            _verifyNoTrailingTokens(p, ctxt, valueType);
        }
        return result;
    }

    protected Object _readMapAndClose(JsonParser p0, JavaType valueType)
        throws IOException
    {
        try (JsonParser p = p0) {
            final Object result;
            final DeserializationConfig cfg = getDeserializationConfig();
            final DefaultDeserializationContext ctxt = createDeserializationContext(p, cfg);
            JsonToken t = _initForReading(p, valueType);
            if (t == JsonToken.VALUE_NULL) {
                // Ask JsonDeserializer what 'null value' to use:
                result = _findRootDeserializer(ctxt, valueType).getNullValue(ctxt);
            } else if (t == JsonToken.END_ARRAY || t == JsonToken.END_OBJECT) {
                result = null;
            } else {
                result = ctxt.readRootValue(p, valueType,
                        _findRootDeserializer(ctxt, valueType), null);
                ctxt.checkUnresolvedObjectId();
            }
            if (cfg.isEnabled(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)) {
                _verifyNoTrailingTokens(p, ctxt, valueType);
            }
            return result;
        }
    }

    /**
     * Similar to {@link #_readMapAndClose} but specialized for <code>JsonNode</code>
     * reading.
     *
     * @since 2.9
     */
    protected JsonNode _readTreeAndClose(JsonParser p0) throws IOException
    {
        try (JsonParser p = p0) {
            final JavaType valueType = constructType(JsonNode.class);

            DeserializationConfig cfg = getDeserializationConfig();
            // 27-Oct-2016, tatu: Need to inline `_initForReading()` due to
            //   special requirements by tree reading (no fail on eof)
            
            cfg.initialize(p); // since 2.5
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

            final DefaultDeserializationContext ctxt = createDeserializationContext(p, cfg);
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
    protected DefaultDeserializationContext createDeserializationContext(JsonParser p,
            DeserializationConfig cfg) {
        return _deserializationContext.createInstance(cfg, p, _injectableValues);
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
     */
    protected JsonToken _initForReading(JsonParser p, JavaType targetType) throws IOException
    {
        _deserializationConfig.initialize(p); // since 2.5

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

    /**
     * @since 2.9
     */
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
    /**********************************************************
    /* Internal methods, other
    /**********************************************************
     */

    /**
     * Method called to locate deserializer for the passed root-level value.
     */
    protected JsonDeserializer<Object> _findRootDeserializer(DeserializationContext ctxt,
            JavaType valueType)
        throws DatabindException
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

    /**
     * @since 2.2
     */
    protected void _verifySchemaType(FormatSchema schema)
    {
        if (schema != null) {
            if (!_jsonFactory.canUseSchema(schema)) {
                    throw new IllegalArgumentException("Cannot use FormatSchema of type "+schema.getClass().getName()
                            +" for format "+_jsonFactory.getFormatName());
            }
        }
    }

    protected final void _assertNotNull(String paramName, Object src) {
        if (src == null) {
            throw new IllegalArgumentException(String.format("argument \"%s\" is null", paramName));
        }
    }
}
