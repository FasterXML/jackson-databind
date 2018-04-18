package com.fasterxml.jackson.databind.ser;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

/**
 * Abstract class that defines API used by {@link SerializerProvider}
 * to obtain actual
 * {@link JsonSerializer} instances from multiple distinct factories.
 */
public abstract class SerializerFactory
{
    /*
    /**********************************************************************
    /* Basic `SerializerFactory` API
    /**********************************************************************
     */

    /**
     * Method called to create (or, for immutable serializers, reuse) a serializer for given type. 
     * 
     * @param prov (not null) Provider that needs to be used to resolve annotation-provided
     *    serializers (but NOT for others)
     * @param format (not null) Format definition for serializer to create
     *
     * @since 3.0 (last argument added)
     */
    public abstract JsonSerializer<Object> createSerializer(SerializerProvider prov,
            JavaType baseType, BeanDescription beanDesc, JsonFormat.Value format)
        throws JsonMappingException;

    /**
     * Method called to create a type information serializer for given base type,
     * if one is needed. If not needed (no polymorphic handling configured), should
     * return null.
     *
     * @param baseType Declared type to use as the base type for type information serializer
     * 
     * @return Type serializer to use for the base type, if one is needed; null if not.
     */
    public abstract TypeSerializer findTypeSerializer(SerializationConfig config,
            JavaType baseType, BeanDescription beanDesc)
        throws JsonMappingException;

    public TypeSerializer findTypeSerializer(SerializationConfig config,
            JavaType baseType) throws JsonMappingException
    {
        return findTypeSerializer(config, baseType,
                config.introspectClassAnnotations(baseType));
    }

    /**
     * Method called to create serializer to use for serializing JSON property names (which must
     * be output as <code>JsonToken.FIELD_NAME</code>) for Map that has specified declared
     * key type, and is for specified property (or, if property is null, as root value)
     * 
     * @param type Declared type for Map keys
     * @param defaultImpl Default key serializer implementation to use, if no custom ones
     *    are found (may be null)
     * 
     * @return Serializer to use, if factory knows it; null if not (in which case default
     *   serializer is to be used)
     */
    public abstract JsonSerializer<Object> createKeySerializer(SerializationConfig config,
            JavaType type, JsonSerializer<Object> defaultImpl)
        throws JsonMappingException;

    /**
     * Returns serializer used to (try to) output a null key, due to an entry of
     * {@link java.util.Map} having null key.
     * The default implementation will throw an exception if this happens;
     * alternative implementation (like one that would write an Empty String)
     * can be defined.
     */
    public abstract JsonSerializer<Object> getDefaultNullKeySerializer();

    public abstract JsonSerializer<Object> getDefaultNullValueSerializer();

    /*
    /**********************************************************************
    /* Additional mutant factories for registering serializer overrides
    /**********************************************************************
     */

    /**
     * Mutant factory method for creating a new factory instance with additional serializer
     * provider: provider will get inserted as the first one to be checked.
     */
    public abstract SerializerFactory withAdditionalSerializers(Serializers additional);

    /**
     * Mutant factory method for creating a new factory instance with additional key serializer
     * provider: provider will get inserted as the first one to be checked.
     */
    public abstract SerializerFactory withAdditionalKeySerializers(Serializers additional);

    /**
     * Mutant factory method for creating a new factory instance with additional serializer modifier:
     * modifier will get inserted as the first one to be checked.
     */
    public abstract SerializerFactory withSerializerModifier(BeanSerializerModifier modifier);

    /**
     * @since 3.0
     */
    public abstract SerializerFactory withNullValueSerializer(JsonSerializer<?> nvs);

    /**
     * @since 3.0
     */
    public abstract SerializerFactory withNullKeySerializer(JsonSerializer<?> nks);

    /*
    /**********************************************************************
    /* Deprecated (for 2.x migration, compatibility)
    /**********************************************************************
     */

    /**
     * @deprecated Since 3.0 use variant that takes {@link JsonFormat.Value} argument
     */
    @Deprecated // since 3.0
    public JsonSerializer<Object> createSerializer(SerializerProvider prov, JavaType baseType)
        throws JsonMappingException
    {
        BeanDescription beanDesc = prov.getConfig().introspect(baseType);
        return createSerializer(prov, baseType, beanDesc,
                beanDesc.findExpectedFormat());
    }
}
