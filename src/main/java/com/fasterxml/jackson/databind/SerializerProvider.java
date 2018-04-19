package com.fasterxml.jackson.databind;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.ObjectIdGenerator;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.tree.ArrayTreeNode;
import com.fasterxml.jackson.core.tree.ObjectTreeNode;

import com.fasterxml.jackson.databind.cfg.ContextAttributes;
import com.fasterxml.jackson.databind.cfg.GeneratorSettings;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.*;
import com.fasterxml.jackson.databind.ser.impl.ReadOnlyClassToSerializerMap;
import com.fasterxml.jackson.databind.ser.impl.TypeWrappedSerializer;
import com.fasterxml.jackson.databind.ser.impl.UnknownSerializer;
import com.fasterxml.jackson.databind.ser.impl.WritableObjectId;
import com.fasterxml.jackson.databind.ser.std.NullSerializer;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * Class that defines API used by {@link ObjectMapper} and
 * {@link JsonSerializer}s to obtain serializers capable of serializing
 * instances of specific types; as well as the default implementation
 * of the functionality.
 *<p>
 * Provider handles caching aspects of serializer handling; all construction
 * details are delegated to {@link SerializerFactory} instance.
 *<p>
 * Object life-cycle is such that an initial instance ("blueprint") is created
 * and referenced by {@link ObjectMapper} and {@link ObjectWriter} intances;
 * but for actual usage, a configured instance is created by using
 * a create method in sub-class
 * {@link com.fasterxml.jackson.databind.ser.DefaultSerializerProvider}.
 * Only this instance can be used for actual serialization calls; blueprint
 * object is only to be used for creating instances.
 */
public abstract class SerializerProvider
    extends DatabindContext
    implements java.io.Serializable, // because we don't have no-args constructor
        ObjectWriteContext // 3.0, for use by jackson-core
{
    private static final long serialVersionUID = 3L;

    /**
     * Placeholder serializer used when <code>java.lang.Object</code> typed property
     * is marked to be serialized.
     *<br>
     * NOTE: starting with 2.6, this instance is NOT used for any other types, and
     * separate instances are constructed for "empty" Beans.
     */
    protected final static JsonSerializer<Object> DEFAULT_UNKNOWN_SERIALIZER = new UnknownSerializer();

    /*
    /**********************************************************************
    /* Configuration, general
    /**********************************************************************
     */
    
    /**
     * Serialization configuration to use for serialization processing.
     */
    final protected SerializationConfig _config;

    /**
     * Low-level {@link TokenStreamFactory} that may be used for constructing
     * embedded generators.
     */
    final protected TokenStreamFactory _streamFactory;
    
    /**
     * View used for currently active serialization, if any.
     * Only set for non-blueprint instances.
     */
    final protected Class<?> _activeView;

    /**
     * Configuration to be used by streaming generator when it is constructed.
     *
     * @since 3.0
     */
    final protected GeneratorSettings _generatorConfig;

    /*
    /**********************************************************************
    /* Configuration, factories
    /**********************************************************************
     */

    /**
     * Factory used for constructing actual serializer instances.
     * Only set for non-blueprint instances.
     */
    final protected SerializerFactory _serializerFactory;

    /*
    /**********************************************************************
    /* Helper objects for caching, reuse
    /**********************************************************************
     */
    
    /**
     * Cache for doing type-to-value-serializer lookups.
     */
    final protected SerializerCache _serializerCache;

    /**
     * Lazily-constructed holder for per-call attributes.
     * Only set for non-blueprint instances.
     */
    protected transient ContextAttributes _attributes;
    
    /*
    /**********************************************************************
    /* Configuration, specialized serializers
    /**********************************************************************
     */

    /**
     * Serializer used to output a null value. Default implementation
     * writes nulls using {@link JsonGenerator#writeNull}.
     */
    protected final JsonSerializer<Object> _nullValueSerializer;

    /*
    /**********************************************************************
    /* State, for non-blueprint instances
    /**********************************************************************
     */

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
     * Flag set to indicate that we are using vanilla null value serialization
     */
    protected final boolean _stdNullValueSerializer;

    /**
     * Token stream generator actively used; only set for per-call instances
     *
     * @since 3.0
     */
    protected transient JsonGenerator _generator;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    /**
     * Constructor for creating master (or "blue-print") provider object,
     * which is only used as the template for constructing per-binding
     * instances.
     */
    public SerializerProvider(TokenStreamFactory streamFactory)
    {
        _streamFactory = streamFactory;
        _config = null;
        _generatorConfig = null;
        _serializerFactory = null;
        _serializerCache = new SerializerCache();
        // Blueprints doesn't have access to any serializers...
        _knownSerializers = null;

        _activeView = null;
        _attributes = null;

        // not relevant for blueprint instance, could set either way:
        _stdNullValueSerializer = true;
        _nullValueSerializer = null;
    }

    /**
     * "Copy-constructor", used by sub-classes when creating actual non-blueprint
     * instances to use.
     *
     * @param src Blueprint object used as the baseline for this instance
     */
    protected SerializerProvider(SerializerProvider src,
            SerializationConfig config, GeneratorSettings generatorConfig,
            SerializerFactory f)
    {
        _streamFactory = src._streamFactory;
        _serializerFactory = f;
        _config = config;
        _generatorConfig = generatorConfig;

        _serializerCache = src._serializerCache;

        // Default null key, value serializers configured via SerializerFactory
        {
            JsonSerializer<Object> ser = f.getDefaultNullValueSerializer();
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

    /**
     * Copy-constructor used when making a copy of a blueprint instance.
     */
    protected SerializerProvider(SerializerProvider src)
    {
        _streamFactory = src._streamFactory;

        // since this is assumed to be a blue-print instance, many settings missing:
        _config = null;
        _generatorConfig = null;
        _activeView = src._activeView;
        _serializerFactory = null;
        _knownSerializers = null;

        // need to ensure cache is clear()ed
        _serializerCache = src._serializerCache.snapshot();

        // and others initialized to default empty state
        _nullValueSerializer = src._nullValueSerializer;
        _stdNullValueSerializer = src._stdNullValueSerializer;
    }

    /*
    /**********************************************************************
    /* ObjectWriteContext impl, config access
    /**********************************************************************
     */

    @Override
    public TokenStreamFactory getGeneratorFactory() {
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
    public SerializableString getRootValueSeparator(SerializableString defaultSeparator) {
        return _generatorConfig.getRootValueSeparator(defaultSeparator);
    }

    @Override
    public int getGeneratorFeatures(int defaults) {
        return _config.getGeneratorFeatures();
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
    public void writeValue(JsonGenerator gen, Object value) throws IOException
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
    public void writeTree(JsonGenerator gen, TreeNode tree) throws IOException
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
    public final JsonFormat.Value getDefaultPropertyFormat(Class<?> baseType) {
        return _config.getDefaultPropertyFormat(baseType);
    }

    public final JsonInclude.Value getDefaultPropertyInclusion(Class<?> baseType) {
        return _config.getDefaultPropertyInclusion();
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
    /* Access to general configuration
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
    public JsonSerializer<Object> findTypedValueSerializer(Class<?> rawType,
            boolean cache)
        throws JsonMappingException
    {
        // First: do we have it cached?
        JsonSerializer<Object> ser = _knownSerializers.typedValueSerializer(rawType);
        if (ser != null) {
            return ser;
        }
        // If not, compose from pieces:
        JavaType fullType = _config.constructType(rawType);
        ser = handleRootContextualization(findValueSerializer(rawType));
        TypeSerializer typeSer = _serializerFactory.findTypeSerializer(_config, fullType);
        if (typeSer != null) {
            typeSer = typeSer.forProperty(null);
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
    public JsonSerializer<Object> findTypedValueSerializer(JavaType valueType, boolean cache)
        throws JsonMappingException
    {

        JsonSerializer<Object> ser = _knownSerializers.typedValueSerializer(valueType);
        if (ser != null) {
            return ser;
        }
        ser = handleRootContextualization(findValueSerializer(valueType));
        TypeSerializer typeSer = _serializerFactory.findTypeSerializer(_config, valueType);
        if (typeSer != null) {
            typeSer = typeSer.forProperty(null);
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
    public JsonSerializer<Object> findRootValueSerializer(Class<?> rawType) throws JsonMappingException
    {
        JsonSerializer<Object> ser = _knownSerializers.untypedValueSerializer(rawType);
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
    public JsonSerializer<Object> findRootValueSerializer(JavaType valueType)
        throws JsonMappingException
    {
        JsonSerializer<Object> ser = _knownSerializers.untypedValueSerializer(valueType);
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
    public JsonSerializer<Object> findPrimaryPropertySerializer(JavaType valueType,
            BeanProperty property)
        throws JsonMappingException
    {
        JsonSerializer<Object> ser = _knownSerializers.untypedValueSerializer(valueType);
        if (ser == null) {
            ser = _createAndCacheUntypedSerializer(valueType);
        }
        return handlePrimaryContextualization(ser, property);
    }

    public JsonSerializer<Object> findPrimaryPropertySerializer(Class<?> rawType,
            BeanProperty property)
        throws JsonMappingException
    {
        JsonSerializer<Object> ser = _knownSerializers.untypedValueSerializer(rawType);
        if (ser == null) {
            JavaType fullType = _config.constructType(rawType);
            ser = _serializerCache.untypedValueSerializer(fullType);
            if (ser == null) {
                ser = _createAndCacheUntypedSerializer(rawType, fullType);
            }
        }
        return handlePrimaryContextualization(ser, property);
    }

    public JsonSerializer<Object> findSecondaryPropertySerializer(JavaType valueType,
            BeanProperty property)
        throws JsonMappingException
    {
        JsonSerializer<Object> ser = _knownSerializers.untypedValueSerializer(valueType);
        if (ser == null) {
            ser = _createAndCacheUntypedSerializer(valueType);
        }
        return handleSecondaryContextualization(ser, property);
    }

    public JsonSerializer<Object> findSecondaryPropertySerializer(Class<?> rawType,
            BeanProperty property)
        throws JsonMappingException
    {
        JsonSerializer<Object> ser = _knownSerializers.untypedValueSerializer(rawType);
        if (ser == null) {
            JavaType fullType = _config.constructType(rawType);
            ser = _serializerCache.untypedValueSerializer(fullType);
            if (ser == null) {
                ser = _createAndCacheUntypedSerializer(rawType, fullType);
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
    public JsonSerializer<Object> findValueSerializer(Class<?> rawType) throws JsonMappingException
    {
        JsonSerializer<Object> ser = _knownSerializers.untypedValueSerializer(rawType);
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
    public JsonSerializer<Object> findValueSerializer(JavaType valueType)
        throws JsonMappingException
    {
        // (see comments from above method)
        JsonSerializer<Object> ser = _knownSerializers.untypedValueSerializer(valueType);
        if (ser == null) {
            ser = _createAndCacheUntypedSerializer(valueType);
        }
        return ser;
    }

    /*
    /**********************************************************************
    /* Serializer discovery: other kinds of serializers; type, key
    /**********************************************************************
     */
    
    /**
     * Method called to get the {@link TypeSerializer} to use for including Type Id necessary
     * for serializing for the given Java class.
     * Useful for schema generators.
     */
    public TypeSerializer findTypeSerializer(JavaType javaType) throws JsonMappingException {
        return _serializerFactory.findTypeSerializer(_config, javaType);
    }

    /**
     * Method called to get the serializer to use for serializing
     * non-null Map keys. Separation from regular
     * {@link #findValueSerializer} method is because actual write
     * method must be different (@link JsonGenerator#writeFieldName};
     * but also since behavior for some key types may differ.
     *<p>
     * Note that the serializer itself can be called with instances
     * of any Java object, but not nulls.
     */
    public JsonSerializer<Object> findKeySerializer(JavaType keyType, BeanProperty property)
        throws JsonMappingException
    {
        // 16-Mar-2018, tatu: Used to have "default key serializer" in 2.x; dropped to let/make
        //    custom code use Module interface or similar to provide key serializers
        JsonSerializer<Object> ser = _serializerFactory.createKeySerializer(_config, keyType, null);
        // _handleContextualResolvable(ser, property):
        ser.resolve(this);
        return handleSecondaryContextualization(ser, property);
    }

    public JsonSerializer<Object> findKeySerializer(Class<?> rawKeyType, BeanProperty property)
        throws JsonMappingException
    {
        return findKeySerializer(_config.constructType(rawKeyType), property);
    }

    public JsonSerializer<Object> getDefaultNullValueSerializer() {
        return _nullValueSerializer;
    }

    /**
     * Method called to find a serializer to use for null values for given
     * declared type. Note that type is completely based on declared type,
     * since nulls in Java have no type and thus runtime type cannot be
     * determined.
     */
    public JsonSerializer<Object> findNullKeySerializer(JavaType serializationType,
            BeanProperty property)
        throws JsonMappingException
    {
        // rarely needed (that is, not on critical perf path), delegate to factory
        return _serializerFactory.getDefaultNullKeySerializer();
    }

    /**
     * Method called to get the serializer to use for serializing null
     * values for specified property.
     *<p>
     * Default implementation simply calls {@link #getDefaultNullValueSerializer()};
     * can be overridden to add custom null serialization for properties
     * of certain type or name. This gives method full granularity to basically
     * override null handling for any specific property or class of properties.
     */
    public JsonSerializer<Object> findNullValueSerializer(BeanProperty property)
        throws JsonMappingException
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
     * although alternatively {@link com.fasterxml.jackson.databind.ser.std.ToStringSerializer}
     * could be returned as well.
     *
     * @param unknownType Type for which no serializer is found
     */
    public JsonSerializer<Object> getUnknownTypeSerializer(Class<?> unknownType) {
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
    public boolean isUnknownTypeSerializer(JsonSerializer<?> ser) {
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
    protected JsonSerializer<Object> _createAndCacheUntypedSerializer(Class<?> rawType,
            JavaType fullType)
            
        throws JsonMappingException
    {
        // Important: must introspect all annotations, not just class
        BeanDescription beanDesc = _config.introspect(fullType);
        JsonFormat.Value format = beanDesc.findExpectedFormat();
        JsonSerializer<Object> ser;
        try {
            ser = _serializerFactory.createSerializer(this, fullType, beanDesc, format);
        } catch (IllegalArgumentException iae) {
            // We better only expose checked exceptions, since those are what caller is expected to handle
            throw _mappingProblem(iae, iae.getMessage());
        }
        // Always cache -- and in this case both for raw and full type
        _serializerCache.addAndResolveNonTypedSerializer(rawType, fullType, ser, this);
        return ser;
    }

    protected JsonSerializer<Object> _createAndCacheUntypedSerializer(JavaType type)
        throws JsonMappingException
    {
        // Important: must introspect all annotations, not just class
        BeanDescription beanDesc = _config.introspect(type);
        JsonFormat.Value format = beanDesc.findExpectedFormat();
        JsonSerializer<Object> ser;
        try {
            ser = _serializerFactory.createSerializer(this, type, beanDesc, format);
        } catch (IllegalArgumentException iae) {
            // We better only expose checked exceptions, since those are what caller is expected to handle
            throw _mappingProblem(iae, iae.getMessage());
        }
        // always cache -- but only full type (may be parameterized)
        _serializerCache.addAndResolveNonTypedSerializer(type, ser, this);
        return ser;
    }

    @SuppressWarnings("unchecked")
    protected JsonSerializer<Object> _handleResolvable(JsonSerializer<?> ser)
        throws JsonMappingException
    {
        ser.resolve(this);
        return (JsonSerializer<Object>) ser;
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
     * (via {@link com.fasterxml.jackson.databind.JsonSerializer#resolve}).
     * 
     * @param annotated Annotated entity that contained definition
     * @param serDef Serializer definition: either an instance or class
     */
    public abstract JsonSerializer<Object> serializerInstance(Annotated annotated,
            Object serDef)
        throws JsonMappingException;

    /**
     * Method that can be called to construct and configure {@link JsonInclude}
     * filter instance,
     * given a {@link Class} to instantiate (with default constructor, by default).
     *
     * @param forProperty (optional) If filter is created for a property, that property;
     *    `null` if filter created via defaulting, global or per-type.
     */
    public abstract Object includeFilterInstance(BeanPropertyDefinition forProperty,
            Class<?> filterClass)
        throws JsonMappingException;

    /**
     * Follow-up method that may be called after calling {@link #includeFilterInstance},
     * to check handling of `null` values by the filter.
     */
    public abstract boolean includeFilterSuppressNulls(Object filter)
        throws JsonMappingException;

    /*
    /**********************************************************************
    /* Support for contextualization
    /**********************************************************************
     */

    /**
     * Method called for primary property serializers (ones
     * directly created to serialize values of a POJO property),
     * to handle details of contextualization, calling
     * {@link JsonSerializer#createContextual(SerializerProvider, BeanProperty)} with given property context.
     * 
     * @param property Property for which the given primary serializer is used; never null.
     */
    @SuppressWarnings("unchecked")
    public JsonSerializer<Object> handlePrimaryContextualization(JsonSerializer<?> ser,
            BeanProperty property)
        throws JsonMappingException
    {
        if (ser != null) {
            ser = ser.createContextual(this, property);
        }
        return (JsonSerializer<Object>) ser;
    }

    /**
     * Method called for secondary property serializers (ones
     * NOT directly created to serialize values of a POJO property
     * but instead created as a dependant serializer -- such as value serializers
     * for structured types, or serializers for root values)
     * to handle details of contextualization, calling
     * {@link JsonSerializer#createContextual(SerializerProvider, BeanProperty)} with given property context.
     * Given that these serializers are not directly related to given property
     * (or, in case of root value property, to any property), annotations
     * accessible may or may not be relevant.
     * 
     * @param property Property for which serializer is used, if any; null
     *    when deserializing root values
     */
    @SuppressWarnings("unchecked")
    public JsonSerializer<Object> handleSecondaryContextualization(JsonSerializer<?> ser,
            BeanProperty property)
        throws JsonMappingException
    {
        if (ser != null) {
            ser = ser.createContextual(this, property);
        }
        return (JsonSerializer<Object>) ser;
    }

    /**
     * @since 3.0
     */
    @SuppressWarnings("unchecked")
    public JsonSerializer<Object> handleRootContextualization(JsonSerializer<?> ser)
        throws JsonMappingException
    {
        if (ser != null) {
            ser = ser.createContextual(this, null);
        }
        return (JsonSerializer<Object>) ser;
    }

    /*
    /**********************************************************************
    /* Convenience methods for serializing using default methods
    /**********************************************************************
     */

    /**
     * Convenience method that will serialize given value (which can be
     * null) using standard serializer locating functionality. It can
     * be called for all values including field and Map values, but usually
     * field values are best handled calling
     * {@link #defaultSerializeField} instead.
     *
     * @deprecated Use {@link #writeValue(JsonGenerator, Object)} instead
     */
    @Deprecated // since 3.0
    public final void defaultSerializeValue(Object value, JsonGenerator gen) throws IOException {
        writeValue(gen, value);
    }

    /**
     * Convenience method that will serialize given field with specified
     * value. Value may be null. Serializer is done using the usual
     * null) using standard serializer locating functionality.
     */
    public final void defaultSerializeField(String fieldName, Object value, JsonGenerator gen)
        throws IOException
    {
        gen.writeFieldName(fieldName);
        writeValue(gen, value);
    }

    /**
     * Method that will handle serialization of Date(-like) values, using
     * {@link SerializationConfig} settings to determine expected serialization
     * behavior.
     * Note: date here means "full" date, that is, date AND time, as per
     * Java convention (and not date-only values like in SQL)
     */
    public final void defaultSerializeDateValue(long timestamp, JsonGenerator gen)
        throws IOException
    {
        if (isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)) {
            gen.writeNumber(timestamp);
        } else {
            gen.writeString(_dateFormat().format(new Date(timestamp)));
        }
    }

    /**
     * Method that will handle serialization of Date(-like) values, using
     * {@link SerializationConfig} settings to determine expected serialization
     * behavior.
     * Note: date here means "full" date, that is, date AND time, as per
     * Java convention (and not date-only values like in SQL)
     */
    public final void defaultSerializeDateValue(Date date, JsonGenerator gen) throws IOException
    {
        if (isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)) {
            gen.writeNumber(date.getTime());
        } else {
            gen.writeString(_dateFormat().format(date));
        }
    }

    /**
     * Method that will handle serialization of Dates used as {@link java.util.Map} keys,
     * based on {@link SerializationFeature#WRITE_DATE_KEYS_AS_TIMESTAMPS}
     * value (and if using textual representation, configured date format)
     */
    public void defaultSerializeDateKey(long timestamp, JsonGenerator gen) throws IOException
    {
        if (isEnabled(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS)) {
            gen.writeFieldName(String.valueOf(timestamp));
        } else {
            gen.writeFieldName(_dateFormat().format(new Date(timestamp)));
        }
    }

    /**
     * Method that will handle serialization of Dates used as {@link java.util.Map} keys,
     * based on {@link SerializationFeature#WRITE_DATE_KEYS_AS_TIMESTAMPS}
     * value (and if using textual representation, configured date format)
     */
    public void defaultSerializeDateKey(Date date, JsonGenerator gen) throws IOException
    {
        if (isEnabled(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS)) {
            gen.writeFieldName(String.valueOf(date.getTime()));
        } else {
            gen.writeFieldName(_dateFormat().format(date));
        }
    }

    /**
     * Method to call when serializing a `null` value (POJO property, Map entry value,
     * Collection/array element) using configured standard mechanism. Note that this
     * does NOT consider filtering any more as value is expected.
     *
     * @since 3.0 (in 2.x was called <code>defaultSerializeNull</code>)
     */
    public final void defaultSerializeNullValue(JsonGenerator gen) throws IOException
    {
        if (_stdNullValueSerializer) { // minor perf optimization
            gen.writeNull();
        } else {
            _nullValueSerializer.serialize(null, gen, this);
        }
    }

    /*
    /**********************************************************************
    /* Error reporting
    /**********************************************************************
     */

    /**
     * Helper method called to indicate problem in POJO (serialization) definitions or settings
     * regarding specific Java type, unrelated to actual JSON content to map.
     * Default behavior is to construct and throw a {@link JsonMappingException}.
     */
    public <T> T reportBadTypeDefinition(BeanDescription bean,
            String msg, Object... msgArgs) throws JsonMappingException
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
     * Default behavior is to construct and throw a {@link JsonMappingException}.
     */
    public <T> T reportBadPropertyDefinition(BeanDescription bean, BeanPropertyDefinition prop,
            String message, Object... msgArgs) throws JsonMappingException {
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
    public <T> T reportBadDefinition(JavaType type, String msg) throws JsonMappingException {
        throw InvalidDefinitionException.from(getGenerator(), msg, type);
    }

    /**
     * @since 2.9
     */
    public <T> T reportBadDefinition(JavaType type, String msg, Throwable cause)
            throws JsonMappingException {
        InvalidDefinitionException e = InvalidDefinitionException.from(getGenerator(), msg, type);
        e.initCause(cause);
        throw e;
    }

    /**
     * @since 2.9
     */
    public <T> T reportBadDefinition(Class<?> raw, String msg, Throwable cause)
            throws JsonMappingException {
        InvalidDefinitionException e = InvalidDefinitionException.from(getGenerator(), msg, constructType(raw));
        e.initCause(cause);
        throw e;
    }

    /**
     * Helper method called to indicate problem; default behavior is to construct and
     * throw a {@link JsonMappingException}, but in future may collect more than one
     * and only throw after certain number, or at the end of serialization.
     */
    public void reportMappingProblem(Throwable t, String message, Object... msgArgs) throws JsonMappingException {
        throw _mappingProblem(t, message, msgArgs);
    }

    protected JsonMappingException _mappingProblem(Throwable t, String message, Object... msgArgs) {
        return JsonMappingException.from(getGenerator(), _format(message, msgArgs), t);
    }
    
    /**
     * Helper method called to indicate problem; default behavior is to construct and
     * throw a {@link JsonMappingException}, but in future may collect more than one
     * and only throw after certain number, or at the end of serialization.
     */
    public void reportMappingProblem(String message, Object... msgArgs) throws JsonMappingException {
        throw JsonMappingException.from(getGenerator(), _format(message, msgArgs));
    }

    @Override
    public JsonMappingException invalidTypeIdException(JavaType baseType, String typeId,
            String extraDesc) {
        String msg = String.format("Could not resolve type id '%s' as a subtype of %s",
                typeId, baseType);
        return InvalidTypeIdException.from(null, _colonConcat(msg, extraDesc), baseType, typeId);
    }

    protected void _reportIncompatibleRootType(Object value, JavaType rootType) throws IOException
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
