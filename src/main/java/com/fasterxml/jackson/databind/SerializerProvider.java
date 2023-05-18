package com.fasterxml.jackson.databind;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.cfg.ContextAttributes;
import com.fasterxml.jackson.databind.cfg.DatatypeFeature;
import com.fasterxml.jackson.databind.cfg.DatatypeFeatures;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.*;
import com.fasterxml.jackson.databind.ser.impl.FailingSerializer;
import com.fasterxml.jackson.databind.ser.impl.ReadOnlyClassToSerializerMap;
import com.fasterxml.jackson.databind.ser.impl.TypeWrappedSerializer;
import com.fasterxml.jackson.databind.ser.impl.UnknownSerializer;
import com.fasterxml.jackson.databind.ser.impl.WritableObjectId;
import com.fasterxml.jackson.databind.ser.std.NullSerializer;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.TokenBuffer;

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
{
    /**
     * Setting for determining whether mappings for "unknown classes" should be
     * cached for faster resolution. Usually this isn't needed, but maybe it
     * is in some cases?
     */
    protected final static boolean CACHE_UNKNOWN_MAPPINGS = false;

    public final static JsonSerializer<Object> DEFAULT_NULL_KEY_SERIALIZER =
        new FailingSerializer("Null key for a Map not allowed in JSON (use a converting NullKeySerializer?)");

    /**
     * Placeholder serializer used when <code>java.lang.Object</code> typed property
     * is marked to be serialized.
     *<br>
     * NOTE: starting with 2.6, this instance is NOT used for any other types, and
     * separate instances are constructed for "empty" Beans.
     *<p>
     * NOTE: changed to <code>protected</code> for 2.3; no need to be publicly available.
     */
    protected final static JsonSerializer<Object> DEFAULT_UNKNOWN_SERIALIZER = new UnknownSerializer();

    /*
    /**********************************************************
    /* Configuration, general
    /**********************************************************
     */

    /**
     * Serialization configuration to use for serialization processing.
     */
    protected final SerializationConfig _config;

    /**
     * View used for currently active serialization, if any.
     * Only set for non-blueprint instances.
     */
    protected final Class<?> _serializationView;

    /*
    /**********************************************************
    /* Configuration, factories
    /**********************************************************
     */

    /**
     * Factory used for constructing actual serializer instances.
     * Only set for non-blueprint instances.
     */
    protected final SerializerFactory _serializerFactory;

    /*
    /**********************************************************
    /* Helper objects for caching, reuse
    /**********************************************************
     */

    /**
     * Cache for doing type-to-value-serializer lookups.
     */
    protected final SerializerCache _serializerCache;

    /**
     * Lazily-constructed holder for per-call attributes.
     * Only set for non-blueprint instances.
     *
     * @since 2.3
     */
    protected transient ContextAttributes _attributes;

    /*
    /**********************************************************
    /* Configuration, specialized serializers
    /**********************************************************
     */

    /**
     * Serializer that gets called for values of types for which no
     * serializers can be constructed.
     *<p>
     * The default serializer will simply thrown an exception.
     */
    protected JsonSerializer<Object> _unknownTypeSerializer = DEFAULT_UNKNOWN_SERIALIZER;

    /**
     * Serializer used to output non-null keys of Maps (which will get
     * output as JSON Objects), if not null; if null, us the standard
     * default key serializer.
     */
    protected JsonSerializer<Object> _keySerializer;

    /**
     * Serializer used to output a null value. Default implementation
     * writes nulls using {@link JsonGenerator#writeNull}.
     */
    protected JsonSerializer<Object> _nullValueSerializer = NullSerializer.instance;

    /**
     * Serializer used to (try to) output a null key, due to an entry of
     * {@link java.util.Map} having null key.
     * The default implementation will throw an exception if this happens;
     * alternative implementation (like one that would write an Empty String)
     * can be defined.
     */
    protected JsonSerializer<Object> _nullKeySerializer = DEFAULT_NULL_KEY_SERIALIZER;

    /*
    /**********************************************************
    /* State, for non-blueprint instances: generic
    /**********************************************************
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
     *
     * @since 2.3
     */
    protected final boolean _stdNullValueSerializer;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    /**
     * Constructor for creating master (or "blue-print") provider object,
     * which is only used as the template for constructing per-binding
     * instances.
     */
    public SerializerProvider()
    {
        _config = null;
        _serializerFactory = null;
        _serializerCache = new SerializerCache();
        // Blueprints doesn't have access to any serializers...
        _knownSerializers = null;

        _serializationView = null;
        _attributes = null;

        // not relevant for blueprint instance, could set either way:
        _stdNullValueSerializer = true;
    }

    /**
     * "Copy-constructor", used by sub-classes when creating actual non-blueprint
     * instances to use.
     *
     * @param src Blueprint object used as the baseline for this instance
     */
    protected SerializerProvider(SerializerProvider src,
            SerializationConfig config, SerializerFactory f)
    {
        _serializerFactory = f;
        _config = config;

        _serializerCache = src._serializerCache;
        _unknownTypeSerializer = src._unknownTypeSerializer;
        _keySerializer = src._keySerializer;
        _nullValueSerializer = src._nullValueSerializer;
        _nullKeySerializer = src._nullKeySerializer;

        _stdNullValueSerializer = (_nullValueSerializer == DEFAULT_NULL_KEY_SERIALIZER);

        _serializationView = config.getActiveView();
        _attributes = config.getAttributes();

        /* Non-blueprint instances do have a read-only map; one that doesn't
         * need synchronization for lookups.
         */
        _knownSerializers = _serializerCache.getReadOnlyLookupMap();
    }

    /**
     * Copy-constructor used when making a copy of a blueprint instance.
     *
     * @since 2.5
     */
    protected SerializerProvider(SerializerProvider src)
    {
        // since this is assumed to be a blue-print instance, many settings missing:
        _config = null;
        _serializationView = null;
        _serializerFactory = null;
        _knownSerializers = null;

        // and others initialized to default empty state
        _serializerCache = new SerializerCache();

        _unknownTypeSerializer = src._unknownTypeSerializer;
        _keySerializer = src._keySerializer;
        _nullValueSerializer = src._nullValueSerializer;
        _nullKeySerializer = src._nullKeySerializer;

        _stdNullValueSerializer = src._stdNullValueSerializer;
    }

    /*
    /**********************************************************
    /* Methods for configuring default settings
    /**********************************************************
     */

    /**
     * Method that can be used to specify serializer to use for serializing
     * all non-null JSON property names, unless more specific key serializer
     * is found (i.e. if not custom key serializer has been registered for
     * Java type).
     *<p>
     * Note that key serializer registration are different from value serializer
     * registrations.
     */
    public void setDefaultKeySerializer(JsonSerializer<Object> ks)
    {
        if (ks == null) {
            throw new IllegalArgumentException("Cannot pass null JsonSerializer");
        }
        _keySerializer = ks;
    }

    /**
     * Method that can be used to specify serializer that will be
     * used to write JSON values matching Java null values
     * instead of default one (which simply writes JSON null).
     *<p>
     * Note that you can get finer control over serializer to use by overriding
     * {@link #findNullValueSerializer}, which gets called once per each
     * property.
     */
    public void setNullValueSerializer(JsonSerializer<Object> nvs)
    {
        if (nvs == null) {
            throw new IllegalArgumentException("Cannot pass null JsonSerializer");
        }
        _nullValueSerializer = nvs;
    }

    /**
     * Method that can be used to specify serializer that will be
     * used to write JSON property names matching null keys for Java
     * Maps (which will otherwise throw an exception if try write such property name)
     */
    public void setNullKeySerializer(JsonSerializer<Object> nks)
    {
        if (nks == null) {
            throw new IllegalArgumentException("Cannot pass null JsonSerializer");
        }
        _nullKeySerializer = nks;
    }

    /*
    /**********************************************************
    /* DatabindContext implementation (and closely related but ser-specific)
    /**********************************************************
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

    @Override // since 2.11
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
    public final Class<?> getActiveView() { return _serializationView; }

    @Override
    public final boolean canOverrideAccessModifiers() {
        return _config.canOverrideAccessModifiers();
    }

    @Override
    public final boolean isEnabled(MapperFeature feature) {
        return _config.isEnabled(feature);
    }

    @Override // @since 2.14
    public final boolean isEnabled(DatatypeFeature feature) {
        return _config.isEnabled(feature);
    }

    @Override // @since 2.15
    public final DatatypeFeatures getDatatypeFeatures() {
        return _config.getDatatypeFeatures();
    }

    @Override
    public final JsonFormat.Value getDefaultPropertyFormat(Class<?> baseType) {
        return _config.getDefaultPropertyFormat(baseType);
    }

    /**
     * @since 2.8
     */
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
    /**********************************************************
    /* Generic attributes (2.3+)
    /**********************************************************
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
    /**********************************************************
    /* Access to general configuration
    /**********************************************************
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
     *
     * @since 2.3
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

    /**
     *<p>
     * NOTE: current implementation simply returns `null` as generator is not yet
     * assigned to this provider.
     *
     * @since 2.8
     */
    public JsonGenerator getGenerator() {
        return null;
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
     *
     * @since 2.13
     */
    public TokenBuffer bufferForValueConversion(ObjectCodec oc) {
        // false -> no native type/object ids
        return new TokenBuffer(oc, false);
    }

    /**
     * Specialized factory method used when we are converting values and do not
     * typically have or use "real" parsers or generators.
     *
     * @since 2.13
     */
    public final TokenBuffer bufferForValueConversion() {
        return bufferForValueConversion(null);
    }

    /*
    /**********************************************************
    /* Access to Object Id aspects
    /**********************************************************
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
    /**********************************************************
    /* General serializer locating functionality
    /**********************************************************
     */

    /**
     * Method called to get hold of a serializer for a value of given type;
     * or if no such serializer can be found, a default handler (which
     * may do a best-effort generic serialization or just simply
     * throw an exception when invoked).
     *<p>
     * Note: this method is only called for non-null values; not for keys
     * or null values. For these, check out other accessor methods.
     *<p>
     * Note that serializers produced should NOT handle polymorphic serialization
     * aspects; separate {@link TypeSerializer} is to be constructed by caller
     * if and as necessary.
     *
     * @throws JsonMappingException if there are fatal problems with
     *   accessing suitable serializer; including that of not
     *   finding any serializer
     */
    @SuppressWarnings("unchecked")
    public JsonSerializer<Object> findValueSerializer(Class<?> valueType, BeanProperty property)
        throws JsonMappingException
    {
        // Fast lookup from local lookup thingy works?
        JsonSerializer<Object> ser = _knownSerializers.untypedValueSerializer(valueType);
        if (ser == null) {
            // If not, maybe shared map already has it?
            ser = _serializerCache.untypedValueSerializer(valueType);
            if (ser == null) {
                // ... possibly as fully typed?
                ser = _serializerCache.untypedValueSerializer(_config.constructType(valueType));
                if (ser == null) {
                    // If neither, must create
                    ser = _createAndCacheUntypedSerializer(valueType);
                    // Not found? Must use the unknown type serializer, which will report error later on
                    if (ser == null) {
                        ser = getUnknownTypeSerializer(valueType);
                        // Should this be added to lookups?
                        if (CACHE_UNKNOWN_MAPPINGS) {
                            _serializerCache.addAndResolveNonTypedSerializer(valueType, ser, this);
                        }
                        return ser;
                    }
                }
            }
        }
        // at this point, resolution has occured, but not contextualization
        return (JsonSerializer<Object>) handleSecondaryContextualization(ser, property);
    }

    /**
     * Similar to {@link #findValueSerializer(Class,BeanProperty)}, but takes
     * full generics-aware type instead of raw class.
     * This is necessary for accurate handling of external type information,
     * to handle polymorphic types.
     *<p>
     * Note: this call will also contextualize serializer before returning it.
     *
     * @param property When creating secondary serializers, property for which
     *   serializer is needed: annotations of the property (or bean that contains it)
     *   may be checked to create contextual serializers.
     */
    @SuppressWarnings("unchecked")
    public JsonSerializer<Object> findValueSerializer(JavaType valueType, BeanProperty property)
        throws JsonMappingException
    {
        if (valueType == null) {
            reportMappingProblem("Null passed for `valueType` of `findValueSerializer()`");
        }
        // (see comments from above method)
        JsonSerializer<Object> ser = _knownSerializers.untypedValueSerializer(valueType);
        if (ser == null) {
            ser = _serializerCache.untypedValueSerializer(valueType);
            if (ser == null) {
                ser = _createAndCacheUntypedSerializer(valueType);
                if (ser == null) {
                    ser = getUnknownTypeSerializer(valueType.getRawClass());
                    if (CACHE_UNKNOWN_MAPPINGS) {
                        _serializerCache.addAndResolveNonTypedSerializer(valueType, ser, this);
                    }
                    return ser;
                }
            }
        }
        return (JsonSerializer<Object>) handleSecondaryContextualization(ser, property);
    }

    /**
     * Method variant used when we do NOT want contextualization to happen; it will need
     * to be handled at a later point, but caller wants to be able to do that
     * as needed; sometimes to avoid infinite loops
     *
     * @since 2.5
     */
    public JsonSerializer<Object> findValueSerializer(Class<?> valueType) throws JsonMappingException
    {
        // (see comments from above method)
        JsonSerializer<Object> ser = _knownSerializers.untypedValueSerializer(valueType);
        if (ser == null) {
            ser = _serializerCache.untypedValueSerializer(valueType);
            if (ser == null) {
                ser = _serializerCache.untypedValueSerializer(_config.constructType(valueType));
                if (ser == null) {
                    ser = _createAndCacheUntypedSerializer(valueType);
                    if (ser == null) {
                        ser = getUnknownTypeSerializer(valueType);
                        if (CACHE_UNKNOWN_MAPPINGS) {
                            _serializerCache.addAndResolveNonTypedSerializer(valueType, ser, this);
                        }
                    }
                }
            }
        }
        return ser;
    }

    /**
     * Method variant used when we do NOT want contextualization to happen; it will need
     * to be handled at a later point, but caller wants to be able to do that
     * as needed; sometimes to avoid infinite loops
     *
     * @since 2.5
     */
    public JsonSerializer<Object> findValueSerializer(JavaType valueType)
        throws JsonMappingException
    {
        // (see comments from above method)
        JsonSerializer<Object> ser = _knownSerializers.untypedValueSerializer(valueType);
        if (ser == null) {
            ser = _serializerCache.untypedValueSerializer(valueType);
            if (ser == null) {
                ser = _createAndCacheUntypedSerializer(valueType);
                if (ser == null) {
                    ser = getUnknownTypeSerializer(valueType.getRawClass());
                    if (CACHE_UNKNOWN_MAPPINGS) {
                        _serializerCache.addAndResolveNonTypedSerializer(valueType, ser, this);
                    }
                }
            }
        }
        return ser;
    }

    /**
     * Similar to {@link #findValueSerializer(JavaType, BeanProperty)}, but used
     * when finding "primary" property value serializer (one directly handling
     * value of the property). Difference has to do with contextual resolution,
     * and method(s) called: this method should only be called when caller is
     * certain that this is the primary property value serializer.
     *
     * @param valueType Type of values to serialize
     * @param property Property that is being handled; will never be null, and its
     *    type has to match <code>valueType</code> parameter.
     *
     * @since 2.3
     */
    @SuppressWarnings("unchecked")
    public JsonSerializer<Object> findPrimaryPropertySerializer(JavaType valueType, BeanProperty property)
        throws JsonMappingException
    {
        JsonSerializer<Object> ser = _knownSerializers.untypedValueSerializer(valueType);
        if (ser == null) {
            ser = _serializerCache.untypedValueSerializer(valueType);
            if (ser == null) {
                ser = _createAndCacheUntypedSerializer(valueType);
                if (ser == null) {
                    ser = getUnknownTypeSerializer(valueType.getRawClass());
                    // Should this be added to lookups?
                    if (CACHE_UNKNOWN_MAPPINGS) {
                        _serializerCache.addAndResolveNonTypedSerializer(valueType, ser, this);
                    }
                    return ser;
                }
            }
        }
        return (JsonSerializer<Object>) handlePrimaryContextualization(ser, property);
    }

    /**
     * See {@link #findPrimaryPropertySerializer(JavaType, BeanProperty)}
     *
     * @since 2.3
     */
    @SuppressWarnings("unchecked")
    public JsonSerializer<Object> findPrimaryPropertySerializer(Class<?> valueType,
            BeanProperty property)
        throws JsonMappingException
    {
        JsonSerializer<Object> ser = _knownSerializers.untypedValueSerializer(valueType);
        if (ser == null) {
            ser = _serializerCache.untypedValueSerializer(valueType);
            if (ser == null) {
                ser = _serializerCache.untypedValueSerializer(_config.constructType(valueType));
                if (ser == null) {
                    ser = _createAndCacheUntypedSerializer(valueType);
                    if (ser == null) {
                        ser = getUnknownTypeSerializer(valueType);
                        if (CACHE_UNKNOWN_MAPPINGS) {
                            _serializerCache.addAndResolveNonTypedSerializer(valueType, ser, this);
                        }
                        return ser;
                    }
                }
            }
        }
        return (JsonSerializer<Object>) handlePrimaryContextualization(ser, property);
    }

    /**
     * Alternative to {@link #findPrimaryPropertySerializer(JavaType, BeanProperty)} called not
     * for primary value, but "content" of such primary serializer: element of an array or
     * {@link java.util.Collection}, value of {@link java.util.Map} entry and so on.
     * This means that {@code property} passed (if any) does NOT represent value for which
     * serializer is requested but its secondary type (or secondary type of that type,
     * recursively).
     *<p>
     * Serializer returned SHOULD NOT handle type information; caller will (have to) add
     * suitable wrapping if necessary.
     *<p>
     * Note: this call will also contextualize serializer (call {@code createContextual()}
     * before returning it, if applicable (implements {@code ContextualSerializer})
     *
     * @param valueType Type of values to serialize
     * @param property Property that indirectly refers to value being serialized (optional,
     *    may be {@code null} for root level serializers)
     *
     * @since 2.11
     */
    @SuppressWarnings("unchecked")
    public JsonSerializer<Object> findContentValueSerializer(JavaType valueType, BeanProperty property)
            throws JsonMappingException
    {
        JsonSerializer<Object> ser = _knownSerializers.untypedValueSerializer(valueType);
        if (ser == null) {
            ser = _serializerCache.untypedValueSerializer(valueType);
            if (ser == null) {
                ser = _createAndCacheUntypedSerializer(valueType);
                if (ser == null) {
                    ser = getUnknownTypeSerializer(valueType.getRawClass());
                    // Should this be added to lookups?
                    if (CACHE_UNKNOWN_MAPPINGS) {
                        _serializerCache.addAndResolveNonTypedSerializer(valueType, ser, this);
                    }
                    return ser;
                }
            }
        }
        return (JsonSerializer<Object>) handleSecondaryContextualization(ser, property);
    }

    /**
     * See {@link #findContentValueSerializer(JavaType, BeanProperty)}.
     *
     * @since 2.11
     */
    @SuppressWarnings("unchecked")
    public JsonSerializer<Object> findContentValueSerializer(Class<?> valueType,
            BeanProperty property)
        throws JsonMappingException
    {
        JsonSerializer<Object> ser = _knownSerializers.untypedValueSerializer(valueType);
        if (ser == null) {
            ser = _serializerCache.untypedValueSerializer(valueType);
            if (ser == null) {
                ser = _serializerCache.untypedValueSerializer(_config.constructType(valueType));
                if (ser == null) {
                    ser = _createAndCacheUntypedSerializer(valueType);
                    if (ser == null) {
                        ser = getUnknownTypeSerializer(valueType);
                        if (CACHE_UNKNOWN_MAPPINGS) {
                            _serializerCache.addAndResolveNonTypedSerializer(valueType, ser, this);
                        }
                        return ser;
                    }
                }
            }
        }
        return (JsonSerializer<Object>) handleSecondaryContextualization(ser, property);
    }

    /**
     * Method called to locate regular serializer, matching type serializer,
     * and if both found, wrap them in a serializer that calls both in correct
     * sequence. This method is currently only used for root-level serializer
     * handling to allow for simpler caching. A call can always be replaced
     * by equivalent calls to access serializer and type serializer separately.
     *
     * @param valueType Type for purpose of locating a serializer; usually dynamic
     *   runtime type, but can also be static declared type, depending on configuration
     * @param cache Whether resulting value serializer should be cached or not; this is just
     *    a hint
     * @param property When creating secondary serializers, property for which
     *   serializer is needed: annotations of the property (or bean that contains it)
     *   may be checked to create contextual serializers.
     */
    public JsonSerializer<Object> findTypedValueSerializer(Class<?> valueType,
            boolean cache, BeanProperty property)
        throws JsonMappingException
    {
        // Two-phase lookups; local non-shared cache, then shared:
        JsonSerializer<Object> ser = _knownSerializers.typedValueSerializer(valueType);
        if (ser != null) {
            return ser;
        }
        // If not, maybe shared map already has it?
        ser = _serializerCache.typedValueSerializer(valueType);
        if (ser != null) {
            return ser;
        }

        // Well, let's just compose from pieces:
        ser = findValueSerializer(valueType, property);
        TypeSerializer typeSer = _serializerFactory.createTypeSerializer(_config,
                _config.constructType(valueType));
        if (typeSer != null) {
            typeSer = typeSer.forProperty(property);
            ser = new TypeWrappedSerializer(typeSer, ser);
        }
        if (cache) {
            _serializerCache.addTypedSerializer(valueType, ser);
        }
        return ser;
    }

    /**
     * Method called to locate regular serializer, matching type serializer,
     * and if both found, wrap them in a serializer that calls both in correct
     * sequence. This method is currently only used for root-level serializer
     * handling to allow for simpler caching. A call can always be replaced
     * by equivalent calls to access serializer and type serializer separately.
     *
     * @param valueType Declared type of value being serialized (which may not
     *    be actual runtime type); used for finding both value serializer and
     *    type serializer to use for adding polymorphic type (if any)
     * @param cache Whether resulting value serializer should be cached or not; this is just
     *    a hint
     * @param property When creating secondary serializers, property for which
     *   serializer is needed: annotations of the property (or bean that contains it)
     *   may be checked to create contextual serializers.
     */
    public JsonSerializer<Object> findTypedValueSerializer(JavaType valueType, boolean cache,
            BeanProperty property)
        throws JsonMappingException
    {
        // Two-phase lookups; local non-shared cache, then shared:
        JsonSerializer<Object> ser = _knownSerializers.typedValueSerializer(valueType);
        if (ser != null) {
            return ser;
        }
        // If not, maybe shared map already has it?
        ser = _serializerCache.typedValueSerializer(valueType);
        if (ser != null) {
            return ser;
        }

        // Well, let's just compose from pieces:
        ser = findValueSerializer(valueType, property);
        TypeSerializer typeSer = _serializerFactory.createTypeSerializer(_config, valueType);
        if (typeSer != null) {
            typeSer = typeSer.forProperty(property);
            ser = new TypeWrappedSerializer(typeSer, ser);
        }
        if (cache) {
            _serializerCache.addTypedSerializer(valueType, ser);
        }
        return ser;
    }

    /**
     * Method called to get the {@link TypeSerializer} to use for including Type Id necessary
     * for serializing for the given Java class.
     * Useful for schema generators.
     *
     * @since 2.6
     */
    public TypeSerializer findTypeSerializer(JavaType javaType) throws JsonMappingException {
        return _serializerFactory.createTypeSerializer(_config, javaType);
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
        JsonSerializer<Object> ser = _serializerFactory.createKeySerializer(this, keyType, _keySerializer);
        // 25-Feb-2011, tatu: As per [JACKSON-519], need to ensure contextuality works here, too
        return _handleContextualResolvable(ser, property);
    }

    /**
     * @since 2.7
     */
    public JsonSerializer<Object> findKeySerializer(Class<?> rawKeyType, BeanProperty property)
        throws JsonMappingException
    {
        return findKeySerializer(_config.constructType(rawKeyType), property);
    }

    /*
    /********************************************************
    /* Accessors for specialized serializers
    /********************************************************
     */

    /**
     * @since 2.0
     */
    public JsonSerializer<Object> getDefaultNullKeySerializer() {
        return _nullKeySerializer;
    }

    /**
     * @since 2.0
     */
    public JsonSerializer<Object> getDefaultNullValueSerializer() {
        return _nullValueSerializer;
    }

    /**
     * Method called to get the serializer to use for serializing
     * Map keys that are nulls: this is needed since JSON does not allow
     * any non-String value as key, including null.
     *<p>
     * Typically, returned serializer
     * will either throw an exception, or use an empty String; but
     * other behaviors are possible.
     */
    /**
     * Method called to find a serializer to use for null values for given
     * declared type. Note that type is completely based on declared type,
     * since nulls in Java have no type and thus runtime type cannot be
     * determined.
     *
     * @since 2.0
     */
    public JsonSerializer<Object> findNullKeySerializer(JavaType serializationType,
            BeanProperty property)
        throws JsonMappingException
    {
        return _nullKeySerializer;
    }

    /**
     * Method called to get the serializer to use for serializing null
     * values for specified property.
     *<p>
     * Default implementation simply calls {@link #getDefaultNullValueSerializer()};
     * can be overridden to add custom null serialization for properties
     * of certain type or name. This gives method full granularity to basically
     * override null handling for any specific property or class of properties.
     *
     * @since 2.0
     */
    public JsonSerializer<Object> findNullValueSerializer(BeanProperty property)
        throws JsonMappingException {
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
            return _unknownTypeSerializer;
        }
        // otherwise construct explicit instance with property handled type
        return new UnknownSerializer(unknownType);
    }

    /**
     * Helper method called to see if given serializer is considered to be
     * something returned by {@link #getUnknownTypeSerializer}, that is, something
     * for which no regular serializer was found or constructed.
     *
     * @since 2.5
     */
    public boolean isUnknownTypeSerializer(JsonSerializer<?> ser) {
        if ((ser == _unknownTypeSerializer) || (ser == null)) {
            return true;
        }
        // 23-Apr-2015, tatu: "empty" serializer is trickier; needs to consider
        //    error handling
        if (isEnabled(SerializationFeature.FAIL_ON_EMPTY_BEANS)) {
            if (ser.getClass() == UnknownSerializer.class) {
                return true;
            }
        }
        return false;
    }

    /*
    /**********************************************************
    /* Methods for creating instances based on annotations
    /**********************************************************
     */

    /**
     * Method that can be called to construct and configure serializer instance,
     * either given a {@link Class} to instantiate (with default constructor),
     * or an uninitialized serializer instance.
     * Either way, serialize will be properly resolved
     * (via {@link com.fasterxml.jackson.databind.ser.ResolvableSerializer}) and/or contextualized
     * (via {@link com.fasterxml.jackson.databind.ser.ContextualSerializer}) as necessary.
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
     *
     * @since 2.9
     */
    public abstract Object includeFilterInstance(BeanPropertyDefinition forProperty,
            Class<?> filterClass)
        throws JsonMappingException;

    /**
     * Follow-up method that may be called after calling {@link #includeFilterInstance},
     * to check handling of `null` values by the filter.
     *
     * @since 2.9
     */
    public abstract boolean includeFilterSuppressNulls(Object filter)
        throws JsonMappingException;

    /*
    /**********************************************************
    /* Support for contextualization
    /**********************************************************
     */

    /**
     * Method called for primary property serializers (ones
     * directly created to serialize values of a POJO property),
     * to handle details of resolving
     * {@link ContextualSerializer} with given property context.
     *
     * @param property Property for which the given primary serializer is used; never null.
     *
     * @since 2.3
     */
    public JsonSerializer<?> handlePrimaryContextualization(JsonSerializer<?> ser,
            BeanProperty property)
        throws JsonMappingException
    {
        if (ser != null) {
            if (ser instanceof ContextualSerializer) {
                ser = ((ContextualSerializer) ser).createContextual(this, property);
            }
        }
        return ser;
    }

    /**
     * Method called for secondary property serializers (ones
     * NOT directly created to serialize values of a POJO property
     * but instead created as a dependant serializer -- such as value serializers
     * for structured types, or serializers for root values)
     * to handle details of resolving
     * {@link ContextualDeserializer} with given property context.
     * Given that these serializers are not directly related to given property
     * (or, in case of root value property, to any property), annotations
     * accessible may or may not be relevant.
     *
     * @param property Property for which serializer is used, if any; null
     *    when deserializing root values
     *
     * @since 2.3
     */
    public JsonSerializer<?> handleSecondaryContextualization(JsonSerializer<?> ser,
            BeanProperty property)
        throws JsonMappingException
    {
        if (ser != null) {
            if (ser instanceof ContextualSerializer) {
                ser = ((ContextualSerializer) ser).createContextual(this, property);
            }
        }
        return ser;
    }

    /*
    /********************************************************
    /* Convenience methods for serializing using default methods
    /********************************************************
     */

    /**
     * Convenience method that will serialize given value (which can be
     * null) using standard serializer locating functionality. It can
     * be called for all values including field and Map values, but usually
     * field values are best handled calling
     * {@link #defaultSerializeField} instead.
     */
    public final void defaultSerializeValue(Object value, JsonGenerator gen) throws IOException
    {
        if (value == null) {
            if (_stdNullValueSerializer) { // minor perf optimization
                gen.writeNull();
            } else {
                _nullValueSerializer.serialize(null, gen, this);
            }
        } else {
            Class<?> cls = value.getClass();
            findTypedValueSerializer(cls, true, null).serialize(value, gen, this);
        }
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
        if (value == null) {
            /* Note: can't easily check for suppression at this point
             * any more; caller must check it.
             */
            if (_stdNullValueSerializer) { // minor perf optimization
                gen.writeNull();
            } else {
                _nullValueSerializer.serialize(null, gen, this);
            }
        } else {
            Class<?> cls = value.getClass();
            findTypedValueSerializer(cls, true, null).serialize(value, gen, this);
        }
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

    public final void defaultSerializeNull(JsonGenerator gen) throws IOException
    {
        if (_stdNullValueSerializer) { // minor perf optimization
            gen.writeNull();
        } else {
            _nullValueSerializer.serialize(null, gen, this);
        }
    }

    /*
    /********************************************************
    /* Error reporting
    /********************************************************
     */

    /**
     * Helper method called to indicate problem; default behavior is to construct and
     * throw a {@link JsonMappingException}, but in future may collect more than one
     * and only throw after certain number, or at the end of serialization.
     *
     * @since 2.8
     */
    public void reportMappingProblem(String message, Object... args) throws JsonMappingException {
        throw mappingException(message, args);
    }

    /**
     * Helper method called to indicate problem in POJO (serialization) definitions or settings
     * regarding specific Java type, unrelated to actual JSON content to map.
     * Default behavior is to construct and throw a {@link JsonMappingException}.
     *
     * @since 2.9
     */
    public <T> T reportBadTypeDefinition(BeanDescription bean,
            String msg, Object... msgArgs) throws JsonMappingException {
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
     *
     * @since 2.9
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
        throw InvalidDefinitionException.from(getGenerator(), msg, type)
            .withCause(cause);
    }

    /**
     * @since 2.9
     */
    public <T> T reportBadDefinition(Class<?> raw, String msg, Throwable cause)
            throws JsonMappingException {
        throw InvalidDefinitionException.from(getGenerator(), msg, constructType(raw))
            .withCause(cause);
    }

    /**
     * Helper method called to indicate problem; default behavior is to construct and
     * throw a {@link JsonMappingException}, but in future may collect more than one
     * and only throw after certain number, or at the end of serialization.
     *
     * @since 2.8
     */
    public void reportMappingProblem(Throwable t, String message, Object... msgArgs) throws JsonMappingException {
        message = _format(message, msgArgs);
        throw JsonMappingException.from(getGenerator(), message, t);
    }

    @Override
    public JsonMappingException invalidTypeIdException(JavaType baseType, String typeId,
            String extraDesc) {
        String msg = String.format("Could not resolve type id '%s' as a subtype of %s",
                typeId, ClassUtil.getTypeDescription(baseType));
        return InvalidTypeIdException.from(null, _colonConcat(msg, extraDesc), baseType, typeId);
    }

    /*
    /********************************************************
    /* Error reporting, deprecated methods
    /********************************************************
     */

    /**
     * Factory method for constructing a {@link JsonMappingException};
     * usually only indirectly used by calling
     * {@link #reportMappingProblem(String, Object...)}.
     *
     * @since 2.6
     *
     * @deprecated Since 2.9
     */
    @Deprecated // since 2.9
    public JsonMappingException mappingException(String message, Object... msgArgs) {
        return JsonMappingException.from(getGenerator(), _format(message, msgArgs));
    }

    /**
     * Factory method for constructing a {@link JsonMappingException};
     * usually only indirectly used by calling
     * {@link #reportMappingProblem(Throwable, String, Object...)}
     *
     * @since 2.8
     *
     * @deprecated Since 2.9
     */
    @Deprecated // since 2.9
    protected JsonMappingException mappingException(Throwable t, String message, Object... msgArgs) {
        return JsonMappingException.from(getGenerator(), _format(message, msgArgs), t);
    }

    /*
    /********************************************************
    /* Helper methods
    /********************************************************
     */

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

    /**
     * Method that will try to find a serializer, either from cache
     * or by constructing one; but will not return an "unknown" serializer
     * if this cannot be done but rather returns null.
     *
     * @return Serializer if one can be found, null if not.
     */
    protected JsonSerializer<Object> _findExplicitUntypedSerializer(Class<?> runtimeType)
        throws JsonMappingException
    {
        // Fast lookup from local lookup thingy works?
        JsonSerializer<Object> ser = _knownSerializers.untypedValueSerializer(runtimeType);
        if (ser == null) {
            // If not, maybe shared map already has it?
            ser = _serializerCache.untypedValueSerializer(runtimeType);
            if (ser == null) {
                ser = _createAndCacheUntypedSerializer(runtimeType);
            }
        }
        /* 18-Sep-2014, tatu: This is unfortunate patch over related change
         *    that pushes creation of "unknown type" serializer deeper down
         *    in BeanSerializerFactory; as a result, we need to "undo" creation
         *    here.
         */
        if (isUnknownTypeSerializer(ser)) {
            return null;
        }
        return ser;
    }

    /*
    /**********************************************************
    /* Low-level methods for actually constructing and initializing
    /* serializers
    /**********************************************************
     */

    /**
     * Method that will try to construct a value serializer; and if
     * one is successfully created, cache it for reuse.
     */
    protected JsonSerializer<Object> _createAndCacheUntypedSerializer(Class<?> rawType)
        throws JsonMappingException
    {
        JavaType fullType = _config.constructType(rawType);
        JsonSerializer<Object> ser;
        try {
            ser = _createUntypedSerializer(fullType);
        } catch (IllegalArgumentException iae) {
            // We better only expose checked exceptions, since those
            // are what caller is expected to handle
            reportBadDefinition(fullType, ClassUtil.exceptionMessage(iae));
            ser = null; // doesn't matter but compiler whines otherwise
        }

        if (ser != null) {
            // 21-Dec-2015, tatu: Best to cache for both raw and full-type key
            _serializerCache.addAndResolveNonTypedSerializer(rawType, fullType, ser, this);
        }
        return ser;
    }

    protected JsonSerializer<Object> _createAndCacheUntypedSerializer(JavaType type)
        throws JsonMappingException
    {
        JsonSerializer<Object> ser;
        try {
            ser = _createUntypedSerializer(type);
        } catch (IllegalArgumentException iae) {
            // We better only expose checked exceptions, since those
            // are what caller is expected to handle
            ser = null;
            reportMappingProblem(iae, ClassUtil.exceptionMessage(iae));
        }

        if (ser != null) {
            // 21-Dec-2015, tatu: Should we also cache using raw key?
            _serializerCache.addAndResolveNonTypedSerializer(type, ser, this);
        }
        return ser;
    }

    /**
     * @since 2.1
     */
    protected JsonSerializer<Object> _createUntypedSerializer(JavaType type)
        throws JsonMappingException
    {
        /* 27-Mar-2015, tatu: Wish I knew exactly why/what, but [databind#738]
         *    can be prevented by synchronizing on cache (not on 'this', however,
         *    since there's one instance per serialization).
         *   Perhaps not-yet-resolved instance might be exposed too early to callers.
         */
        // 13-Apr-2018, tatu: Problem does NOT occur any more with late 2.8.x and 2.9.x
        //    versions, likely due to concurrency fixes for `AnnotatedClass` introspection.
        //    This sync block could probably be removed; but to minimize any risk of
        //    regression sync block will only be removed from 3.0.
        // 23-Oct-2019, tatu: Due to continuation of 2.x line, removed from 2.11
//        synchronized (_serializerCache) {
            return (JsonSerializer<Object>)_serializerFactory.createSerializer(this, type);
//        }
    }

    /**
     * Helper method called to resolve and contextualize given
     * serializer, if and as necessary.
     */
    @SuppressWarnings("unchecked")
    protected JsonSerializer<Object> _handleContextualResolvable(JsonSerializer<?> ser,
            BeanProperty property)
        throws JsonMappingException
    {
        if (ser instanceof ResolvableSerializer) {
            ((ResolvableSerializer) ser).resolve(this);
        }
        return (JsonSerializer<Object>) handleSecondaryContextualization(ser, property);
    }

    @SuppressWarnings("unchecked")
    protected JsonSerializer<Object> _handleResolvable(JsonSerializer<?> ser)
        throws JsonMappingException
    {
        if (ser instanceof ResolvableSerializer) {
            ((ResolvableSerializer) ser).resolve(this);
        }
        return (JsonSerializer<Object>) ser;
    }

    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */

    protected final DateFormat _dateFormat()
    {
        if (_dateFormat != null) {
            return _dateFormat;
        }
        /* At this point, all timezone configuration should have occurred, with respect
         * to default dateformat configuration. But we still better clone
         * an instance as formatters are stateful, not thread-safe.
         */
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
