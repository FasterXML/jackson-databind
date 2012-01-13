package com.fasterxml.jackson.databind;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Date;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.jsonschema.JsonSchema;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.SerializerFactory;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * Abstract class that defines API used by {@link ObjectMapper} and
 * {@link JsonSerializer}s to obtain serializers capable of serializing
 * instances of specific types.
 *<p>
 * Note about usage: for {@link JsonSerializer} instances, only accessors
 * for locating other (sub-)serializers are to be used. {@link ObjectMapper},
 * on the other hand, is to initialize recursive serialization process by
 * calling {@link #serializeValue}.
 */
public abstract class SerializerProvider
{
    protected final static JavaType TYPE_OBJECT = TypeFactory.defaultInstance().uncheckedSimpleType(Object.class);
    
    /**
     * Serialization configuration to use for serialization processing.
     */
    protected final SerializationConfig _config;

    /**
     * View used for currently active serialization
     */
    protected final Class<?> _serializationView;

    protected SerializerProvider(SerializationConfig config)
    {
        _config = config;
        _serializationView = (config == null) ? null : _config.getSerializationView();
    }

    /*
    /**********************************************************
    /* Methods for configuring default settings
    /**********************************************************
     */

    /**
     * Method that can be used to specify serializer that will be
     * used to write JSON property names matching null keys for Java
     * Maps (which will throw an exception if try write such property
     * name)
     */
    public abstract void setNullKeySerializer(JsonSerializer<Object> nks);

    /**
     * Method that can be used to specify serializer that will be
     * used to write JSON values matching Java null values
     * instead of default one (which simply writes JSON null)
     */
    public abstract void setNullValueSerializer(JsonSerializer<Object> nvs);
    
    /**
     * Method that can be used to specify serializer to use for serializing
     * all non-null JSON property names, unless more specific key serializer
     * is found (i.e. if not custom key serializer has been registered for
     * Java type).
     *<p>
     * Note that key serializer registration are different from value serializer
     * registrations.
     */
    public abstract void setDefaultKeySerializer(JsonSerializer<Object> ks);
    
    /*
    /**********************************************************
    /* Methods that ObjectMapper will call
    /**********************************************************
     */

    /**
     * The method to be called by {@link ObjectMapper} to
     * execute recursive serialization, using serializers that
     * this provider has access to.
     *
     * @param jsf Underlying factory object used for creating serializers
     *    as needed
     */
    public abstract void serializeValue(SerializationConfig cfg, JsonGenerator jgen,
            Object value, SerializerFactory jsf)
        throws IOException, JsonGenerationException;

    /**
     * The method to be called by {@link ObjectMapper} to
     * execute recursive serialization, using serializers that
     * this provider has access to; and using specified root type
     * for locating first-level serializer.
     * 
     * @param rootType Type to use for locating serializer to use, instead of actual
     *    runtime type. Must be actual type, or one of its super types
     */
    public abstract void serializeValue(SerializationConfig cfg, JsonGenerator jgen,
            Object value, JavaType rootType, SerializerFactory jsf)
        throws IOException, JsonGenerationException;
    
    /**
     * Generate <a href="http://json-schema.org/">Json-schema</a> for
     * given type.
     *
     * @param type The type for which to generate schema
     */
    public abstract JsonSchema generateJsonSchema(Class<?> type, SerializationConfig config, SerializerFactory jsf)
        throws JsonMappingException;

    /**
     * Method that can be called to see if this serializer provider
     * can find a serializer for an instance of given class.
     *<p>
     * Note that no Exceptions are thrown, including unchecked ones:
     * implementations are to swallow exceptions if necessary.
     */
    public abstract boolean hasSerializerFor(SerializationConfig cfg,
            Class<?> cls, SerializerFactory jsf);

    /*
    /**********************************************************
    /* Access to configuration
    /**********************************************************
     */

    /**
     * Method for accessing configuration for the serialization processing.
     */
    public final SerializationConfig getConfig() { return _config; }

    /**
     * Convenience method for checking whether specified serialization
     * feature is enabled or not.
     * Shortcut for:
     *<pre>
     *  getConfig().isEnabled(feature);
     *</pre>
     */
    public final boolean isEnabled(SerializationConfig.Feature feature) {
        return _config.isEnabled(feature);
    }

    /**
     * Convenience method for accessing serialization view in use (if any); equivalent to:
     *<pre>
     *   getConfig().getSerializationView();
     *</pre>
     */
    public final Class<?> getSerializationView() { return _serializationView; }

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
     * Convenience method for constructing {@link JavaType} for given JDK
     * type (usually {@link java.lang.Class})
     */
    public JavaType constructType(Type type) {
         return _config.getTypeFactory().constructType(type);
    }

    /**
     * Convenience method for constructing subtypes, retaining generic
     * type parameter (if any)
     */
    public JavaType constructSpecializedType(JavaType baseType, Class<?> subclass) {
        return _config.constructSpecializedType(baseType, subclass);
    }
    
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
     * Note that starting with version 1.5, serializers should also be type-aware
     * if they handle polymorphic types. That means that it may be necessary
     * to also use a {@link TypeSerializer} based on declared (static) type
     * being serializer (whereas actual data may be serialized using dynamic
     * type)
     *
     * @throws JsonMappingException if there are fatal problems with
     *   accessing suitable serializer; including that of not
     *   finding any serializer
     */
    public abstract JsonSerializer<Object> findValueSerializer(Class<?> runtimeType,
            BeanProperty property)
        throws JsonMappingException;

    /**
     * Similar to {@link #findValueSerializer(Class)}, but takes full generics-aware
     * type instead of raw class.
     */
    public abstract JsonSerializer<Object> findValueSerializer(JavaType serializationType,
            BeanProperty property)
        throws JsonMappingException;
    
    /**
     * Method called to locate regular serializer, matching type serializer,
     * and if both found, wrap them in a serializer that calls both in correct
     * sequence. This method is currently only used for root-level serializer
     * handling to allow for simpler caching. A call can always be replaced
     * by equivalent calls to access serializer and type serializer separately.
     * 
     * @param valueType Type for purpose of locating a serializer; usually dynamic
     *   runtime type, but can also be static declared type, depending on configuration
     * 
     * @param cache Whether resulting value serializer should be cached or not; this is just
     *    a hint
     */
    public abstract JsonSerializer<Object> findTypedValueSerializer(Class<?> valueType,
            boolean cache, BeanProperty property)
        throws JsonMappingException;

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
     * 
     * @param cache Whether resulting value serializer should be cached or not; this is just
     *    a hint 
     */
    public abstract JsonSerializer<Object> findTypedValueSerializer(JavaType valueType,
            boolean cache, BeanProperty property)
        throws JsonMappingException;

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
    public abstract JsonSerializer<Object> findKeySerializer(JavaType keyType,
            BeanProperty property)
        throws JsonMappingException;
    
    /*
    /********************************************************
    /* Accessors for specialized serializers
    /********************************************************
     */

    /**
     * @since 2.0
     */
    public abstract JsonSerializer<Object> getDefaultNullKeySerializer()
        throws JsonMappingException;

    /**
     * @since 2.0
     */
    public abstract JsonSerializer<Object> getDefaultNullValueSerializer()
        throws JsonMappingException;
    
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
     * since nulls in Java have no type and thus runtime type can not be
     * determined.
     * 
     * @since 2.0
     */
    public JsonSerializer<Object> findNullKeySerializer(JavaType serializationType,
            BeanProperty property)
        throws JsonMappingException {
        return getDefaultNullKeySerializer();
    }

    /**
     * Method called to get the serializer to use for serializing null
     * property values.
     *<p>
     * Default implementation simply calls {@link #getDefaultNullValueSerializer()};
     * can be overridden to add custom null serialization for properties
     * of certain type or name.
     * 
     * @since 2.0
     */
    public JsonSerializer<Object> findNullValueSerializer(BeanProperty property)
        throws JsonMappingException {
        return getDefaultNullValueSerializer();
    }

    /**
     * Method called to get the serializer to use if provider
     * can not determine an actual type-specific serializer
     * to use; typically when none of {@link SerializerFactory}
     * instances are able to construct a serializer.
     *<p>
     * Typically, returned serializer will throw an exception,
     * although alternatively {@link com.fasterxml.jackson.databind.ser.ToStringSerializer} could
     * be returned as well.
     *
     * @param unknownType Type for which no serializer is found
     */
    public abstract JsonSerializer<Object> getUnknownTypeSerializer(Class<?> unknownType);

    /*
    /********************************************************
    /* Convenience methods
    /********************************************************
     */

    /**
     * Convenience method that will serialize given value (which can be
     * null) using standard serializer locating functionality. It can
     * be called for all values including field and Map values, but usually
     * field values are best handled calling
     * {@link #defaultSerializeField} instead.
     */
    public final void defaultSerializeValue(Object value, JsonGenerator jgen)
        throws IOException, JsonProcessingException
    {
        if (value == null) {
            getDefaultNullValueSerializer().serialize(null, jgen, this);
        } else {
            Class<?> cls = value.getClass();
            findTypedValueSerializer(cls, true, null).serialize(value, jgen, this);
        }
    }
    
    /**
     * Convenience method that will serialize given field with specified
     * value. Value may be null. Serializer is done using the usual
     * null) using standard serializer locating functionality.
     */
    public final void defaultSerializeField(String fieldName, Object value, JsonGenerator jgen)
        throws IOException, JsonProcessingException
    {
        jgen.writeFieldName(fieldName);
        if (value == null) {
            /* Note: can't easily check for suppression at this point
             * any more; caller must check it.
             */
            getDefaultNullValueSerializer().serialize(null, jgen, this);
        } else {
            Class<?> cls = value.getClass();
            findTypedValueSerializer(cls, true, null).serialize(value, jgen, this);
        }
    }

    /**
     * Method that will handle serialization of Date(-like) values, using
     * {@link SerializationConfig} settings to determine expected serialization
     * behavior.
     * Note: date here means "full" date, that is, date AND time, as per
     * Java convention (and not date-only values like in SQL)
     */
    public abstract void defaultSerializeDateValue(long timestamp, JsonGenerator jgen)
        throws IOException, JsonProcessingException;

    /**
     * Method that will handle serialization of Date(-like) values, using
     * {@link SerializationConfig} settings to determine expected serialization
     * behavior.
     * Note: date here means "full" date, that is, date AND time, as per
     * Java convention (and not date-only values like in SQL)
     */
    public abstract void defaultSerializeDateValue(Date date, JsonGenerator jgen)
        throws IOException, JsonProcessingException;


    /**
     * Method that will handle serialization of Dates used as {@link java.util.Map} keys,
     * based on {@link SerializationConfig.Feature#WRITE_DATE_KEYS_AS_TIMESTAMPS}
     * value (and if using textual representation, configured date format)
     */
    public abstract void defaultSerializeDateKey(long timestamp, JsonGenerator jgen)
        throws IOException, JsonProcessingException;

    /**
     * Method that will handle serialization of Dates used as {@link java.util.Map} keys,
     * based on {@link SerializationConfig.Feature#WRITE_DATE_KEYS_AS_TIMESTAMPS}
     * value (and if using textual representation, configured date format)
     */
    public abstract void defaultSerializeDateKey(Date date, JsonGenerator jgen)
        throws IOException, JsonProcessingException;
    
    public final void defaultSerializeNull(JsonGenerator jgen)
        throws IOException, JsonProcessingException
    {
        getDefaultNullValueSerializer().serialize(null, jgen, this);
    }
    
    /*
    /********************************************************
    /* Access to caching details
    /********************************************************
     */

    /**
     * Method that can be used to determine how many serializers this
     * provider is caching currently
     * (if it does caching: default implementation does)
     * Exact count depends on what kind of serializers get cached;
     * default implementation caches all serializers, including ones that
     * are eagerly constructed (for optimal access speed)
     *<p> 
     * The main use case for this method is to allow conditional flushing of
     * serializer cache, if certain number of entries is reached.
     */
    public abstract int cachedSerializersCount();

    /**
     * Method that will drop all serializers currently cached by this provider.
     * This can be used to remove memory usage (in case some serializers are
     * only used once or so), or to force re-construction of serializers after
     * configuration changes for mapper than owns the provider.
     */
    public abstract void flushCachedSerializers();
}
