package tools.jackson.databind;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.ObjectIdGenerator;

import tools.jackson.core.*;
import tools.jackson.core.io.CharacterEscapes;
import tools.jackson.core.tree.ArrayTreeNode;
import tools.jackson.core.tree.ObjectTreeNode;
import tools.jackson.core.util.JacksonFeatureSet;
import tools.jackson.databind.cfg.ContextAttributes;
import tools.jackson.databind.cfg.DatatypeFeature;
import tools.jackson.databind.cfg.DatatypeFeatures;
import tools.jackson.databind.cfg.GeneratorSettings;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.exc.InvalidTypeIdException;
import tools.jackson.databind.introspect.Annotated;
import tools.jackson.databind.introspect.AnnotatedClass;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.introspect.BeanPropertyDefinition;
import tools.jackson.databind.introspect.ClassIntrospector;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.*;
import tools.jackson.databind.ser.impl.ReadOnlyClassToSerializerMap;
import tools.jackson.databind.ser.impl.TypeWrappedSerializer;
import tools.jackson.databind.ser.impl.UnknownSerializer;
import tools.jackson.databind.ser.std.NullSerializer;
import tools.jackson.databind.type.TypeFactory;
import tools.jackson.databind.util.ClassUtil;
import tools.jackson.databind.util.TokenBuffer;

/**
 * Class that defines API used by {@link ObjectMapper} and
 * {@link ValueSerializer}s to obtain serializers capable of serializing
 * instances of specific types; as well as the default implementation
 * of the functionality.
 *<p>
 * Provider handles caching aspects of serializer handling; all construction
 * details are delegated to {@link SerializerFactory} instance.
 *<p>
 */
public abstract class SerializerProvider
    extends DatabindContext
    implements // NOTE: not JDK serializable with 3.x (factory that creates these is)
        ObjectWriteContext // 3.0, for use by jackson-core
{
    /**
     * Placeholder serializer used when <code>java.lang.Object</code> typed property
     * is marked to be serialized.
     *<br>
     * NOTE: starting with 2.6, this instance is NOT used for any other types, and
     * separate instances are constructed for "empty" Beans.
     */
    protected final static ValueSerializer<Object> DEFAULT_UNKNOWN_SERIALIZER = new UnknownSerializer();

    /*
    /**********************************************************************
    /* Configuration, general
    /**********************************************************************
     */

    /**
     * Serialization configuration to use for serialization processing.
     */
    protected final SerializationConfig _config;

    /**
     * Configuration to be used by streaming generator when it is constructed.
     *
     * @since 3.0
     */
    protected final GeneratorSettings _generatorConfig;

    /**
     * Low-level {@link TokenStreamFactory} that may be used for constructing
     * embedded generators.
     */
    protected final TokenStreamFactory _streamFactory;

    /**
     * Token stream generator actively used; only set for per-call instances
     *
     * @since 3.0
     */
    protected transient JsonGenerator _generator;

    /**
     * Capabilities of the output format.
     *
     * @since 3.0
     */
    protected JacksonFeatureSet<StreamWriteCapability> _writeCapabilities;

    /**
     * View used for currently active serialization, if any.
     */
    protected final Class<?> _activeView;

    /*
    /**********************************************************************
    /* Configuration, serializer access
    /**********************************************************************
     */

    /**
     * Factory used for constructing actual serializer instances.
     * Only set for non-blueprint instances.
     */
    protected final SerializerFactory _serializerFactory;

    /**
     * Serializer used to output a null value. Default implementation
     * writes nulls using {@link JsonGenerator#writeNull}.
     */
    protected final ValueSerializer<Object> _nullValueSerializer;

    /**
     * Flag set to indicate that we are using vanilla null value serialization
     */
    protected final boolean _stdNullValueSerializer;

    /*
    /**********************************************************************
    /* Helper objects for caching, reuse
    /**********************************************************************
     */

    /**
     * Cache for doing type-to-value-serializer lookups.
     */
    protected final SerializerCache _serializerCache;

    /**
     * For fast lookups, we will have a local non-shared read-only
     * map that contains serializers previously fetched.
     */
    protected final ReadOnlyClassToSerializerMap _knownSerializers;

    /**
     * Lazily acquired and instantiated formatter object: initialized
     * first time it is needed, reused afterwards. Used via instances
     * (not blueprints), so that access need not be thread-safe.
     */
    protected DateFormat _dateFormat;

    /**
     * Lazily constructed {@link ClassIntrospector} instance: created from "blueprint"
     */
    protected transient ClassIntrospector _classIntrospector;

    /*
    /**********************************************************************
    /* Other state
    /**********************************************************************
     */

    /**
     * Lazily-constructed holder for per-call attributes.
     * Only set for non-blueprint instances.
     */
    protected ContextAttributes _attributes;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    protected SerializerProvider(TokenStreamFactory streamFactory,
            SerializationConfig config, GeneratorSettings generatorConfig,
            SerializerFactory f, SerializerCache cache)
    {
        _streamFactory = streamFactory;
        _serializerFactory = f;
        _config = config;
        _generatorConfig = generatorConfig;

        _serializerCache = cache;

        // Default null key, value serializers configured via SerializerFactory
        {
            ValueSerializer<Object> ser = f.getDefaultNullValueSerializer();
            if (ser == null) {
                _stdNullValueSerializer = true;
                ser = NullSerializer.instance;
            } else {
                _stdNullValueSerializer = false;
            }
            _nullValueSerializer = ser;
        }

        _activeView = config.getActiveView();
        _attributes = config.getAttributes();

        // Non-blueprint instances do have a read-only map; one that doesn't
        // need synchronization for lookups.
        _knownSerializers = _serializerCache.getReadOnlyLookupMap();
    }

    protected SerializerProvider(SerializerProvider src, SerializerCache serializerCache)
    {
        _streamFactory = src._streamFactory;
        _serializerFactory = src._serializerFactory;
        _config = src._config;
        _generatorConfig = src._generatorConfig;

        _serializerCache = serializerCache;

        _stdNullValueSerializer = src._stdNullValueSerializer;
        _nullValueSerializer = src._nullValueSerializer;
        _activeView = src._activeView;
        _attributes = src._attributes;

        _knownSerializers = src._knownSerializers;
    }

    /*
    /**********************************************************************
    /* ObjectWriteContext impl, config access
    /**********************************************************************
     */

    @Override
    public TokenStreamFactory tokenStreamFactory() {
        return _streamFactory;
    }

    @Override
    public FormatSchema getSchema() { return _generatorConfig.getSchema(); }

    @Override
    public CharacterEscapes getCharacterEscapes() { return _generatorConfig.getCharacterEscapes(); }

    @Override
    public PrettyPrinter getPrettyPrinter() {
        PrettyPrinter pp = _generatorConfig.getPrettyPrinter();
        if (pp == null) {
            if (isEnabled(SerializationFeature.INDENT_OUTPUT)) {
                pp = _config.constructDefaultPrettyPrinter();
            }
        }
        return pp;
    }

    @Override
    public boolean hasPrettyPrinter() {
        return _generatorConfig.hasPrettyPrinter()
                || isEnabled(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    public SerializableString getRootValueSeparator(SerializableString defaultSeparator) {
        return _generatorConfig.getRootValueSeparator(defaultSeparator);
    }

    @Override
    public int getStreamWriteFeatures(int defaults) {
        return _config.getStreamWriteFeatures();
    }

    @Override
    public int getFormatWriteFeatures(int defaults) {
        return _config.getFormatWriteFeatures();
    }

    /*
    /**********************************************************************
    /* ObjectWriteContext impl, databind integration
    /**********************************************************************
     */

    @Override
    public ArrayTreeNode createArrayNode() {
        return _config.getNodeFactory().arrayNode();
    }

    @Override
    public ObjectTreeNode createObjectNode() {
        return _config.getNodeFactory().objectNode();
    }

    @Override
    public void writeValue(JsonGenerator gen, Object value) throws JacksonException
    {
        // Let's keep track of active generator; useful mostly for error reporting...
        JsonGenerator prevGen = _generator;
        _generator = gen;
        try {
            if (value == null) {
                if (_stdNullValueSerializer) { // minor perf optimization
                    gen.writeNull();
                } else {
                    _nullValueSerializer.serialize(null, gen, this);
                }
                return;
            }
            Class<?> cls = value.getClass();
            findTypedValueSerializer(cls, true).serialize(value, gen, this);
        } finally {
            _generator = prevGen;
        }
    }

    @Override
    public void writeTree(JsonGenerator gen, TreeNode tree) throws JacksonException
    {
        // 05-Oct-2017, tatu: Should probably optimize or something? Or not?
        writeValue(gen, tree);
    }

    /*
    /**********************************************************************
    /* DatabindContext implementation (and closely related but ser-specific)
    /**********************************************************************
     */

    /**
     * Method for accessing configuration for the serialization processing.
     */
    @Override
    public final SerializationConfig getConfig() { return _config; }

    @Override
    public final AnnotationIntrospector getAnnotationIntrospector() {
        return _config.getAnnotationIntrospector();
    }

    @Override
    public final TypeFactory getTypeFactory() {
        return _config.getTypeFactory();
    }

    @Override
    public JavaType constructSpecializedType(JavaType baseType, Class<?> subclass)
        throws IllegalArgumentException
    {
        if (baseType.hasRawClass(subclass)) {
            return baseType;
        }
        // Need little bit different handling due to [databind#2632]; pass `true` for
        // "relaxed" type assingment checks.
        return getConfig().getTypeFactory().constructSpecializedType(baseType, subclass, true);
    }

    @Override
    public final Class<?> getActiveView() { return _activeView; }

    @Override
    public final boolean canOverrideAccessModifiers() {
        return _config.canOverrideAccessModifiers();
    }

    @Override
    public final boolean isEnabled(MapperFeature feature) {
        return _config.isEnabled(feature);
    }

    @Override
    public final boolean isEnabled(DatatypeFeature feature) {
        return _config.isEnabled(feature);
    }

    @Override
    public final DatatypeFeatures getDatatypeFeatures() {
        return _config.getDatatypeFeatures();
    }

    @Override
    public final JsonFormat.Value getDefaultPropertyFormat(Class<?> baseType) {
        return _config.getDefaultPropertyFormat(baseType);
    }

    public final JsonInclude.Value getDefaultPropertyInclusion(Class<?> baseType) {
        return _config.getDefaultPropertyInclusion(baseType);
    }

    /**
     * Method for accessing default Locale to use: convenience method for
     *<pre>
     *   getConfig().getLocale();
     *</pre>
     */
    @Override
    public Locale getLocale() {
        return _config.getLocale();
    }

    /**
     * Method for accessing default TimeZone to use: convenience method for
     *<pre>
     *   getConfig().getTimeZone();
     *</pre>
     */
    @Override
    public TimeZone getTimeZone() {
        return _config.getTimeZone();
    }

    /*
    /**********************************************************************
    /* Annotation, BeanDescription introspection
    /**********************************************************************
     */

    @Override
    protected ClassIntrospector classIntrospector() {
        if (_classIntrospector == null) {
            _classIntrospector = _config.classIntrospectorInstance();
        }
        return _classIntrospector;
    }

    @Override
    public BeanDescription introspectBeanDescription(JavaType type) {
        return classIntrospector().introspectForSerialization(type);
    }

    /*
    /**********************************************************************
    /* Misc config access
    /**********************************************************************
     */

    @Override
    public PropertyName findRootName(JavaType rootType) {
        return _config.findRootName(this, rootType);
    }

    @Override
    public PropertyName findRootName(Class<?> rawRootType) {
        return _config.findRootName(this, rawRootType);
    }

    /*
    /**********************************************************************
    /* Generic attributes
    /**********************************************************************
     */

    @Override
    public Object getAttribute(Object key) {
        return _attributes.getAttribute(key);
    }

    @Override
    public SerializerProvider setAttribute(Object key, Object value)
    {
        _attributes = _attributes.withPerCallAttribute(key, value);
        return this;
    }

    /*
    /**********************************************************************
    /* Access to other on/off features
    /**********************************************************************
     */

    /**
     * Convenience method for checking whether specified serialization
     * feature is enabled or not.
     * Shortcut for:
     *<pre>
     *  getConfig().isEnabled(feature);
     *</pre>
     */
    public final boolean isEnabled(SerializationFeature feature) {
        return _config.isEnabled(feature);
    }

    /**
     * "Bulk" access method for checking that all features specified by
     * mask are enabled.
     */
    public final boolean hasSerializationFeatures(int featureMask) {
        return _config.hasSerializationFeatures(featureMask);
    }

    /**
     * Accessor for checking whether input format has specified capability
     * or not.
     *
     * @return True if input format has specified capability; false if not
     */
    public final boolean isEnabled(StreamWriteCapability cap) {
        return _writeCapabilities.isEnabled(cap);
    }

    /*
    /**********************************************************************
    /* Access to other helper objects
    /**********************************************************************
     */

    /**
     * Convenience method for accessing provider to find serialization filters used,
     * equivalent to calling:
     *<pre>
     *   getConfig().getFilterProvider();
     *</pre>
     */
    public final FilterProvider getFilterProvider() {
        return _config.getFilterProvider();
    }

    public JsonGenerator getGenerator() {
        return _generator;
    }

    /*
    /**********************************************************************
    /* Factory methods for getting appropriate TokenBuffer instances
    /* (possibly overridden by backends for alternate data formats)
    /**********************************************************************
     */

    /**
     * Specialized factory method used when we are converting values and do not
     * typically have or use "real" parsers or generators.
     */
    public TokenBuffer bufferForValueConversion() {
        // 28-May-2021, tatu: Will directly call constructor from here, instead
        //    of adding a factory method, since alternate formats likely need to
        //    use different TokenBuffer sub[class:

        // false -> no native Object Ids available (or rather not needed)
        return new TokenBuffer(this, false);
    }

    /*
    /**********************************************************************
    /* Access to Object Id aspects
    /**********************************************************************
     */

    /**
     * Method called to find the Object Id for given POJO, if one
     * has been generated. Will always return a non-null Object;
     * contents vary depending on whether an Object Id already
     * exists or not.
     */
    public abstract WritableObjectId findObjectId(Object forPojo,
        ObjectIdGenerator<?> generatorType);

    /*
    /**********************************************************************
    /* Serializer discovery: root/non-property value serializers
    /**********************************************************************
     */

    /**
     * Method called to locate regular serializer, matching type serializer,
     * and if both found, wrap them in a serializer that calls both in correct
     * sequence. This method is mostly used for root-level serializer
     * handling to allow for simpler caching. A call can always be replaced
     * by equivalent calls to access serializer and type serializer separately.
     *
     * @param rawType Type for purpose of locating a serializer; usually dynamic
     *   runtime type, but can also be static declared type, depending on configuration
     * @param cache Whether resulting value serializer should be cached or not
     */
    public ValueSerializer<Object> findTypedValueSerializer(Class<?> rawType,
            boolean cache)
    {
        // First: do we have it cached?
        ValueSerializer<Object> ser = _knownSerializers.typedValueSerializer(rawType);
        if (ser != null) {
            return ser;
        }
        // If not, compose from pieces:
        JavaType fullType = _config.constructType(rawType);
        ser = handleRootContextualization(findValueSerializer(rawType));
        TypeSerializer typeSer = findTypeSerializer(fullType);
        if (typeSer != null) {
            typeSer = typeSer.forProperty(this, null);
            ser = new TypeWrappedSerializer(typeSer, ser);
        }
        if (cache) {
            _serializerCache.addTypedSerializer(rawType, ser);
        }
        return ser;
    }

    /**
     * Method called to locate regular serializer, matching type serializer,
     * and if both found, wrap them in a serializer that calls both in correct
     * sequence. This method is mostly used for root-level serializer
     * handling to allow for simpler caching. A call can always be replaced
     * by equivalent calls to access serializer and type serializer separately.
     *
     * @param valueType Declared type of value being serialized (which may not
     *    be actual runtime type); used for finding both value serializer and
     *    type serializer to use for adding polymorphic type (if any)
     * @param cache Whether resulting value serializer should be cached or not
     */
    public ValueSerializer<Object> findTypedValueSerializer(JavaType valueType, boolean cache)
    {

        ValueSerializer<Object> ser = _knownSerializers.typedValueSerializer(valueType);
        if (ser != null) {
            return ser;
        }
        ser = handleRootContextualization(findValueSerializer(valueType));
        TypeSerializer typeSer = findTypeSerializer(valueType);
        if (typeSer != null) {
            typeSer = typeSer.forProperty(this, null);
            ser = new TypeWrappedSerializer(typeSer, ser);
        }
        if (cache) {
            _serializerCache.addTypedSerializer(valueType, ser);
        }
        return ser;
    }

    /**
     * Method for finding (from cache) or creating (and caching) serializer for given type,
     * without checking for polymorphic typing, and then contextualizing without actual
     * property. This is most often used for root-level values (when writing
     * sequences), but may sometimes be used for more esoteric value handling for
     * delegation.
     *
     * @since 3.0
     */
    public ValueSerializer<Object> findRootValueSerializer(Class<?> rawType)
    {
        ValueSerializer<Object> ser = _knownSerializers.untypedValueSerializer(rawType);
        if (ser == null) {
            JavaType fullType = _config.constructType(rawType);
            ser = _serializerCache.untypedValueSerializer(fullType);
            if (ser == null) {
                ser = _createAndCacheUntypedSerializer(rawType, fullType);
            }
        }
        return handleRootContextualization(ser);
    }

    /**
     * Method for finding (from cache) or creating (and caching) serializer for given type,
     * without checking for polymorphic typing, and then contextualizing without actual
     * property. This is most often used for root-level values (when writing
     * sequences), but may sometimes be used for more esoteric value handling for
     * delegation.
     *
     * @since 3.0
     */
    public ValueSerializer<Object> findRootValueSerializer(JavaType valueType)
    {
        ValueSerializer<Object> ser = _knownSerializers.untypedValueSerializer(valueType);
        if (ser == null) {
            ser = _createAndCacheUntypedSerializer(valueType);
        }
        return handleRootContextualization(ser);
    }

    /*
    /**********************************************************************
    /* Serializer discovery: property value serializers
    /**********************************************************************
     */

    /**
     * Method used for locating "primary" property value serializer (one directly
     * handling value of the property). Difference (if any) has to do with contextual resolution,
     * and method(s) called: this method should only be called when caller is
     * certain that this is the primary property value serializer.
     *
     * @param property Property that is being handled; will never be null, and its
     *    type has to match <code>valueType</code> parameter.
     */
    public ValueSerializer<Object> findPrimaryPropertySerializer(JavaType valueType,
            BeanProperty property)
    {
        ValueSerializer<Object> ser = _knownSerializers.untypedValueSerializer(valueType);
        if (ser == null) {
            ser = _createAndCachePropertySerializer(valueType, property);
        }
        return handlePrimaryContextualization(ser, property);
    }

    public ValueSerializer<Object> findPrimaryPropertySerializer(Class<?> rawType,
            BeanProperty property)
    {
        ValueSerializer<Object> ser = _knownSerializers.untypedValueSerializer(rawType);
        if (ser == null) {
            JavaType fullType = _config.constructType(rawType);
            ser = _serializerCache.untypedValueSerializer(fullType);
            if (ser == null) {
                ser = _createAndCachePropertySerializer(rawType, fullType, property);
            }
        }
        return handlePrimaryContextualization(ser, property);
    }

    /**
     * Method similar to {@link #findPrimaryPropertySerializer(JavaType, BeanProperty)}
     * but used for "content values", secondary types used by "primary" serializers
     * for structured types like Arrays, {@link java.util.Collection}s, {@link java.util.Map}s
     * and so on.
     *<p>
     * Serializer will be contextualized, but will not have type serializer wrapped.
     *
     * @param valueType Type of (secondary / content) values being serialized
     * @param property (optional) Property that refers to values via primary type (so type
     *    DOES NOT necessarily match {@code valueType})
     */
    public ValueSerializer<Object> findContentValueSerializer(JavaType valueType,
            BeanProperty property)
    {
        ValueSerializer<Object> ser = _knownSerializers.untypedValueSerializer(valueType);
        if (ser == null) {
            ser = _createAndCachePropertySerializer(valueType, property);
        }
        return handleSecondaryContextualization(ser, property);
    }

    /**
     * See {@link #findContentValueSerializer(JavaType, BeanProperty)}.
     */
    public ValueSerializer<Object> findContentValueSerializer(Class<?> rawType,
            BeanProperty property)
    {
        ValueSerializer<Object> ser = _knownSerializers.untypedValueSerializer(rawType);
        if (ser == null) {
            JavaType fullType = _config.constructType(rawType);
            ser = _serializerCache.untypedValueSerializer(fullType);
            if (ser == null) {
                ser = _createAndCachePropertySerializer(rawType, fullType, property);
            }
        }
        return handleSecondaryContextualization(ser, property);
    }

    /*
    /**********************************************************************
    /* General serializer locating functionality
    /**********************************************************************
     */

    /**
     * Method variant used when we do NOT want contextualization to happen; it will need
     * to be handled at a later point, but caller wants to be able to do that
     * as needed; sometimes to avoid infinite loops
     */
    public ValueSerializer<Object> findValueSerializer(Class<?> rawType)
    {
        ValueSerializer<Object> ser = _knownSerializers.untypedValueSerializer(rawType);
        if (ser == null) {
            JavaType fullType = _config.constructType(rawType);
            ser = _serializerCache.untypedValueSerializer(fullType);
            if (ser == null) {
                ser = _createAndCacheUntypedSerializer(rawType, fullType);
            }
        }
        return ser;
    }

    /**
     * Method variant used when we do NOT want contextualization to happen; it will need
     * to be handled at a later point, but caller wants to be able to do that
     * as needed; sometimes to avoid infinite loops
     */
    public ValueSerializer<Object> findValueSerializer(JavaType valueType)
    {
        // (see comments from above method)
        ValueSerializer<Object> ser = _knownSerializers.untypedValueSerializer(valueType);
        if (ser == null) {
            ser = _createAndCacheUntypedSerializer(valueType);
        }
        return ser;
    }

    /*
    /**********************************************************************
    /* Serializer discovery: type serializers
    /**********************************************************************
     */

    /**
     * Method called to get the {@link TypeSerializer} to use for including Type Id necessary
     * for serializing for the given Java class.
     * Useful for schema generators.
     */
    public TypeSerializer findTypeSerializer(JavaType baseType) {
        return findTypeSerializer(baseType, introspectClassAnnotations(baseType));
    }

    /**
     * Method called to get the {@link TypeSerializer} to use for including Type Id necessary
     * for serializing for the given Java class.
     * Useful for schema generators.
     *
     * @since 3.0
     */
    public TypeSerializer findTypeSerializer(JavaType baseType, AnnotatedClass classAnnotations)
    {
        return _config.getTypeResolverProvider().findTypeSerializer(this, baseType,
                classAnnotations);
    }

    /**
     * Like {@link #findTypeSerializer(JavaType)}, but for use from specific POJO property.
     * Method called to create a type information serializer for values of given
     * non-container property
     * if one is needed. If not needed (no polymorphic handling configured), should
     * return null.
     *
     * @param baseType Declared type to use as the base type for type information serializer
     *
     * @return Type serializer to use for property values, if one is needed; null if not.
     *
     * @since 3.0
     */
    public TypeSerializer findPropertyTypeSerializer(JavaType baseType, AnnotatedMember accessor)
    {
        return _config.getTypeResolverProvider()
                .findPropertyTypeSerializer(this, accessor, baseType);
    }

    /*
    /**********************************************************************
    /* Serializer discovery: key serializers
    /**********************************************************************
     */

    /**
     * Method called to get the serializer to use for serializing
     * non-null Map keys. Separation from regular
     * {@link #findValueSerializer} method is because actual write
     * method must be different (@link JsonGenerator#writeName};
     * but also since behavior for some key types may differ.
     *<p>
     * Note that the serializer itself can be called with instances
     * of any Java object, but not nulls.
     */
    public ValueSerializer<Object> findKeySerializer(JavaType keyType, BeanProperty property)
    {
        // 16-Mar-2018, tatu: Used to have "default key serializer" in 2.x; dropped to let/make
        //    custom code use Module interface or similar to provide key serializers
        ValueSerializer<Object> ser = _serializerFactory.createKeySerializer(this, keyType);
        // _handleContextualResolvable(ser, property):
        ser.resolve(this);
        return handleSecondaryContextualization(ser, property);
    }

    public ValueSerializer<Object> findKeySerializer(Class<?> rawKeyType, BeanProperty property)
    {
        return findKeySerializer(_config.constructType(rawKeyType), property);
    }

    public ValueSerializer<Object> getDefaultNullValueSerializer() {
        return _nullValueSerializer;
    }

    /**
     * Method called to find a serializer to use for null values for given
     * declared type. Note that type is completely based on declared type,
     * since nulls in Java have no type and thus runtime type cannot be
     * determined.
     */
    public ValueSerializer<Object> findNullKeySerializer(JavaType serializationType,
            BeanProperty property)
    {
        // rarely needed (that is, not on critical perf path), delegate to factory
        return _serializerFactory.getDefaultNullKeySerializer();
    }

    /*
    /**********************************************************************
    /* Serializer discovery: other misc serializers, null value, unknown
    /**********************************************************************
     */

    /**
     * Method called to get the serializer to use for serializing null
     * values for specified property.
     *<p>
     * Default implementation simply calls {@link #getDefaultNullValueSerializer()};
     * can be overridden to add custom null serialization for properties
     * of certain type or name. This gives method full granularity to basically
     * override null handling for any specific property or class of properties.
     */
    public ValueSerializer<Object> findNullValueSerializer(BeanProperty property)
    {
        return _nullValueSerializer;
    }

    /**
     * Method called to get the serializer to use if provider
     * cannot determine an actual type-specific serializer
     * to use; typically when none of {@link SerializerFactory}
     * instances are able to construct a serializer.
     *<p>
     * Typically, returned serializer will throw an exception,
     * although alternatively {@link tools.jackson.databind.ser.std.ToStringSerializer}
     * could be returned as well.
     *
     * @param unknownType Type for which no serializer is found
     */
    public ValueSerializer<Object> getUnknownTypeSerializer(Class<?> unknownType) {
        // 23-Apr-2015, tatu: Only return shared instance if nominal type is Object.class
        if (unknownType == Object.class) {
            return DEFAULT_UNKNOWN_SERIALIZER;
        }
        // otherwise construct explicit instance with property handled type
        return new UnknownSerializer(unknownType);
    }

    /**
     * Helper method called to see if given serializer is considered to be
     * something returned by {@link #getUnknownTypeSerializer}, that is, something
     * for which no regular serializer was found or constructed.
     */
    public boolean isUnknownTypeSerializer(ValueSerializer<?> ser) {
        if ((ser == DEFAULT_UNKNOWN_SERIALIZER) || (ser == null)) {
            return true;
        }
        // 23-Apr-2015, tatu: "empty" serializer is trickier; needs to consider
        //    error handling
        if (isEnabled(SerializationFeature.FAIL_ON_EMPTY_BEANS)) {
            if (ser instanceof UnknownSerializer) {
                return true;
            }
        }
        return false;
    }

    /*
    /**********************************************************************
    /* Low-level methods for actually constructing and initializing serializers
    /**********************************************************************
     */

    /**
     * Method that will try to construct a value serializer; and if
     * one is successfully created, cache it for reuse.
     */
    protected ValueSerializer<Object> _createAndCacheUntypedSerializer(Class<?> rawType,
            JavaType fullType)
    {
        // Important: must introspect all annotations, not just class
        BeanDescription beanDesc = introspectBeanDescription(fullType);
        ValueSerializer<Object> ser;
        try {
            ser = _serializerFactory.createSerializer(this, fullType, beanDesc, null);
        } catch (IllegalArgumentException iae) {
            // We better only expose checked exceptions, since those are what caller is expected to handle
            reportBadTypeDefinition(beanDesc, ClassUtil.exceptionMessage(iae));
            ser = null; // never gets here
        }
        // Always cache -- and in this case both for raw and full type
        _serializerCache.addAndResolveNonTypedSerializer(rawType, fullType, ser, this);
        return ser;
    }

    protected ValueSerializer<Object> _createAndCacheUntypedSerializer(JavaType type)
    {
        // Important: must introspect all annotations, not just class
        BeanDescription beanDesc = introspectBeanDescription(type);
        ValueSerializer<Object> ser;
        try {
            ser = _serializerFactory.createSerializer(this, type, beanDesc, null);
        } catch (IllegalArgumentException iae) {
            // We better only expose checked exceptions, since those are what caller is expected to handle
            throw _mappingProblem(iae, ClassUtil.exceptionMessage(iae));
        }
        // always cache -- but only full type (may be parameterized)
        _serializerCache.addAndResolveNonTypedSerializer(type, ser, this);
        return ser;
    }

    /**
     * Alternative to {@link #_createAndCacheUntypedSerializer(Class, JavaType)}, used
     * when serializer is requested for given property.
     */
    protected ValueSerializer<Object> _createAndCachePropertySerializer(Class<?> rawType,
            JavaType fullType, BeanProperty prop)
    {
        BeanDescription beanDesc = introspectBeanDescription(fullType);
        ValueSerializer<Object> ser;
        try {
            ser = _serializerFactory.createSerializer(this, fullType, beanDesc, null);
        } catch (IllegalArgumentException iae) {
            throw _mappingProblem(iae, ClassUtil.exceptionMessage(iae));
        }
        _serializerCache.addAndResolveNonTypedSerializer(rawType, fullType, ser, this);
        // Fine, we have to base instance. But how about per-property format overrides?
        if (prop == null) {
            return ser;
        }
        return _checkShapeShifting(fullType, beanDesc, prop, ser);
    }

    /**
     * Alternative to {@link #_createAndCacheUntypedSerializer(JavaType)}, used
     * when serializer is requested for given property.
     */
    protected ValueSerializer<Object> _createAndCachePropertySerializer(JavaType type,
            BeanProperty prop)
    {
        BeanDescription beanDesc = introspectBeanDescription(type);
        ValueSerializer<Object> ser;
        try {
            ser = _serializerFactory.createSerializer(this, type, beanDesc, null);
        } catch (IllegalArgumentException iae) {
            throw _mappingProblem(iae, ClassUtil.exceptionMessage(iae));
        }
        _serializerCache.addAndResolveNonTypedSerializer(type, ser, this);
        // Fine, we have to base instance. But how about per-property format overrides?
        if (prop == null) {
            return ser;
        }
        return _checkShapeShifting(type, beanDesc, prop, ser);
    }

    @SuppressWarnings("unchecked")
    private ValueSerializer<Object> _checkShapeShifting(JavaType type, BeanDescription beanDesc,
            BeanProperty prop, ValueSerializer<?> ser)
    {
        JsonFormat.Value overrides = prop.findFormatOverrides(_config);
        if (overrides != null) {
            // First: it may be completely fine to use serializer, despite some overrides
            ValueSerializer<?> ser2 = ser.withFormatOverrides(_config, overrides);
            if (ser2 != null) {
                ser = ser2;
            } else {
                // But if not, we need to re-create it via factory
                ser = _serializerFactory.createSerializer(this, type, beanDesc, overrides);
            }
        }
        return (ValueSerializer<Object>) ser;
    }

    @SuppressWarnings("unchecked")
    protected ValueSerializer<Object> _handleResolvable(ValueSerializer<?> ser)
    {
        ser.resolve(this);
        return (ValueSerializer<Object>) ser;
    }

    /*
    /**********************************************************************
    /* Methods for creating instances based on annotations
    /**********************************************************************
     */

    /**
     * Method that can be called to construct and configure serializer instance,
     * either given a {@link Class} to instantiate (with default constructor),
     * or an uninitialized serializer instance.
     * Either way, serializer will be properly resolved
     * (via {@link tools.jackson.databind.ValueSerializer#resolve}).
     *
     * @param annotated Annotated entity that contained definition
     * @param serDef Serializer definition: either an instance or class
     */
    public abstract ValueSerializer<Object> serializerInstance(Annotated annotated,
            Object serDef);

    /**
     * Method that can be called to construct and configure {@link JsonInclude}
     * filter instance,
     * given a {@link Class} to instantiate (with default constructor, by default).
     *
     * @param forProperty (optional) If filter is created for a property, that property;
     *    `null` if filter created via defaulting, global or per-type.
     */
    public abstract Object includeFilterInstance(BeanPropertyDefinition forProperty,
            Class<?> filterClass);

    /**
     * Follow-up method that may be called after calling {@link #includeFilterInstance},
     * to check handling of `null` values by the filter.
     */
    public abstract boolean includeFilterSuppressNulls(Object filter);

    /*
    /**********************************************************************
    /* Support for contextualization
    /**********************************************************************
     */

    /**
     * Method called for primary property serializers (ones
     * directly created to serialize values of a POJO property),
     * to handle details of contextualization, calling
     * {@link ValueSerializer#createContextual(SerializerProvider, BeanProperty)} with given property context.
     *
     * @param property Property for which the given primary serializer is used; never null.
     */
    @SuppressWarnings("unchecked")
    public ValueSerializer<Object> handlePrimaryContextualization(ValueSerializer<?> ser,
            BeanProperty property)
    {
        if (ser != null) {
            ser = ser.createContextual(this, property);
        }
        return (ValueSerializer<Object>) ser;
    }

    /**
     * Method called for secondary property serializers (ones
     * NOT directly created to serialize values of a POJO property
     * but instead created as a dependant serializer -- such as value serializers
     * for structured types, or serializers for root values)
     * to handle details of contextualization, calling
     * {@link ValueSerializer#createContextual(SerializerProvider, BeanProperty)} with given property context.
     * Given that these serializers are not directly related to given property
     * (or, in case of root value property, to any property), annotations
     * accessible may or may not be relevant.
     *
     * @param property Property for which serializer is used, if any; null
     *    when deserializing root values
     */
    @SuppressWarnings("unchecked")
    public ValueSerializer<Object> handleSecondaryContextualization(ValueSerializer<?> ser,
            BeanProperty property)
    {
        if (ser != null) {
            ser = ser.createContextual(this, property);
        }
        return (ValueSerializer<Object>) ser;
    }

    /**
     * @since 3.0
     */
    @SuppressWarnings("unchecked")
    public ValueSerializer<Object> handleRootContextualization(ValueSerializer<?> ser)
    {
        if (ser != null) {
            ser = ser.createContextual(this, null);
        }
        return (ValueSerializer<Object>) ser;
    }

    /*
    /**********************************************************************
    /* Convenience methods for serializing using default methods
    /**********************************************************************
     */

    /**
     * Convenience method that will serialize given property with specified
     * value, using the default serializer for runtime type of {@code value}.
     */
    public final void defaultSerializeProperty(String propertyName, Object value, JsonGenerator g)
        throws JacksonException
    {
        g.writeName(propertyName);
        writeValue(g, value);
    }

    /**
     * Method that will handle serialization of Date(-like) values, using
     * {@link SerializationConfig} settings to determine expected serialization
     * behavior.
     * Note: date here means "full" date, that is, date AND time, as per
     * Java convention (and not date-only values like in SQL)
     */
    public final void defaultSerializeDateValue(long timestamp, JsonGenerator g)
        throws JacksonException
    {
        if (isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)) {
            g.writeNumber(timestamp);
        } else {
            g.writeString(_dateFormat().format(new Date(timestamp)));
        }
    }

    /**
     * Method that will handle serialization of Date(-like) values, using
     * {@link SerializationConfig} settings to determine expected serialization
     * behavior.
     * Note: date here means "full" date, that is, date AND time, as per
     * Java convention (and not date-only values like in SQL)
     */
    public final void defaultSerializeDateValue(Date date, JsonGenerator g)
        throws JacksonException
    {
        if (isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)) {
            g.writeNumber(date.getTime());
        } else {
            g.writeString(_dateFormat().format(date));
        }
    }

    /**
     * Method that will handle serialization of Dates used as {@link java.util.Map} keys,
     * based on {@link SerializationFeature#WRITE_DATE_KEYS_AS_TIMESTAMPS}
     * value (and if using textual representation, configured date format)
     */
    public void defaultSerializeDateKey(long timestamp, JsonGenerator g)
        throws JacksonException
    {
        if (isEnabled(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS)) {
            g.writeName(String.valueOf(timestamp));
        } else {
            g.writeName(_dateFormat().format(new Date(timestamp)));
        }
    }

    /**
     * Method that will handle serialization of Dates used as {@link java.util.Map} keys,
     * based on {@link SerializationFeature#WRITE_DATE_KEYS_AS_TIMESTAMPS}
     * value (and if using textual representation, configured date format)
     */
    public void defaultSerializeDateKey(Date date, JsonGenerator g) throws JacksonException
    {
        if (isEnabled(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS)) {
            g.writeName(String.valueOf(date.getTime()));
        } else {
            g.writeName(_dateFormat().format(date));
        }
    }

    /**
     * Method to call when serializing a {@code null} value (POJO property, Map entry value,
     * Collection/array element) using configured standard mechanism. Note that this
     * does NOT consider filtering any more as value is expected.
     *
     * @since 3.0 (in 2.x was called <code>defaultSerializeNull</code>)
     */
    public final void defaultSerializeNullValue(JsonGenerator g) throws JacksonException
    {
        if (_stdNullValueSerializer) { // minor perf optimization
            g.writeNull();
        } else {
            _nullValueSerializer.serialize(null, g, this);
        }
    }

    /*
    /**********************************************************************
    /* Serialization-like helper methods
    /**********************************************************************
     */

    /**
     * Method that will convert given Java value (usually bean) into its
     * equivalent Tree mode {@link JsonNode} representation.
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
     */
    public abstract <T extends JsonNode> T valueToTree(Object fromValue)
        throws JacksonException;

    /*
    /**********************************************************************
    /* Error reporting
    /**********************************************************************
     */

    /**
     * Helper method called to indicate problem in POJO (serialization) definitions or settings
     * regarding specific Java type, unrelated to actual JSON content to map.
     * Default behavior is to construct and throw a {@link InvalidDefinitionException}.
     */
    @Override
    public <T> T reportBadTypeDefinition(BeanDescription bean,
            String msg, Object... msgArgs)
        throws DatabindException
    {
        String beanDesc = "N/A";
        if (bean != null) {
            beanDesc = ClassUtil.nameOf(bean.getBeanClass());
        }
        msg = String.format("Invalid type definition for type %s: %s",
                beanDesc, _format(msg, msgArgs));
        throw InvalidDefinitionException.from(getGenerator(), msg, bean, null);
    }

    /**
     * Helper method called to indicate problem in POJO (serialization) definitions or settings
     * regarding specific property (of a type), unrelated to actual JSON content to map.
     * Default behavior is to construct and throw a {@link InvalidDefinitionException}.
     */
    public <T> T reportBadPropertyDefinition(BeanDescription bean, BeanPropertyDefinition prop,
            String message, Object... msgArgs)
        throws DatabindException
    {
        message = _format(message, msgArgs);
        String propName = "N/A";
        if (prop != null) {
            propName = _quotedString(prop.getName());
        }
        String beanDesc = "N/A";
        if (bean != null) {
            beanDesc = ClassUtil.nameOf(bean.getBeanClass());
        }
        message = String.format("Invalid definition for property %s (of type %s): %s",
                propName, beanDesc, message);
        throw InvalidDefinitionException.from(getGenerator(), message, bean, prop);
    }

    @Override
    public <T> T reportBadDefinition(JavaType type, String msg) throws DatabindException {
        throw InvalidDefinitionException.from(getGenerator(), msg, type);
    }

    public <T> T reportBadDefinition(JavaType type, String msg, Throwable cause)
        throws DatabindException
    {
        throw InvalidDefinitionException.from(getGenerator(), msg, type)
            .withCause(cause);
    }

    public <T> T reportBadDefinition(Class<?> raw, String msg, Throwable cause)
        throws DatabindException
    {
        throw InvalidDefinitionException.from(getGenerator(), msg, constructType(raw))
            .withCause(cause);
    }

    /**
     * Helper method called to indicate problem; default behavior is to construct and
     * throw a {@link DatabindException}, but in future may collect more than one
     * and only throw after certain number, or at the end of serialization.
     */
    public void reportMappingProblem(Throwable t, String message, Object... msgArgs)
        throws DatabindException
    {
        throw _mappingProblem(t, message, msgArgs);
    }

    protected DatabindException _mappingProblem(Throwable t, String message, Object... msgArgs)
    {
        return DatabindException.from(getGenerator(), _format(message, msgArgs), t);
    }

    /**
     * Helper method called to indicate problem; default behavior is to construct and
     * throw a {@link DatabindException}, but in future may collect more than one
     * and only throw after certain number, or at the end of serialization.
     */
    public void reportMappingProblem(String message, Object... msgArgs)
        throws DatabindException
    {
        throw DatabindException.from(getGenerator(), _format(message, msgArgs));
    }

    @Override
    public DatabindException invalidTypeIdException(JavaType baseType, String typeId,
            String extraDesc)
    {
        String msg = String.format("Could not resolve type id '%s' as a subtype of %s",
                typeId, ClassUtil.getTypeDescription(baseType));
        return InvalidTypeIdException.from(null, _colonConcat(msg, extraDesc), baseType, typeId);
    }

    protected void _reportIncompatibleRootType(Object value, JavaType rootType)
        throws JacksonException
    {
        // One special case: allow primitive/wrapper type coercion
        if (rootType.isPrimitive()) {
            Class<?> wrapperType = ClassUtil.wrapperType(rootType.getRawClass());
            // If it's just difference between wrapper, primitive, let it slide
            if (wrapperType.isAssignableFrom(value.getClass())) {
                return;
            }
        }
        reportBadDefinition(rootType, String.format(
                "Incompatible types: declared root type (%s) vs %s",
                rootType, ClassUtil.classNameOf(value)));
    }

    /*
    /**********************************************************************
    /* Internal methods, other
    /**********************************************************************
     */

    protected final DateFormat _dateFormat()
    {
        if (_dateFormat != null) {
            return _dateFormat;
        }
        // At this point, all timezone configuration should have occurred, with respect
        // to default dateformat configuration. But we still better clone
        // an instance as formatters are stateful, not thread-safe.
        DateFormat df = _config.getDateFormat();
        _dateFormat = df = (DateFormat) df.clone();
        // [databind#939]: 26-Sep-2015, tatu: With 2.6, formatter has been (pre)configured
        // with TimeZone, so we should NOT try overriding it unlike with earlier versions
        /*
        TimeZone tz = getTimeZone();
        if (tz != df.getTimeZone()) {
            df.setTimeZone(tz);
        }
        */
        return df;
    }
}
