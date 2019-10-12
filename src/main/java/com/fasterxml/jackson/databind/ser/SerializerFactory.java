package com.fasterxml.jackson.databind.ser;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.*;

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
     * @param ctxt (not null) Context that needs to be used to resolve annotation-provided
     *    serializers (but NOT for others)
     * @param formatOverride (nullable) Possible format overrides (from property annotations)
     *    to use, above and beyond what `beanDesc` defines
     *
     * @since 3.0 (last argument added)
     */
    public abstract JsonSerializer<Object> createSerializer(SerializerProvider ctxt,
            JavaType baseType, BeanDescription beanDesc, JsonFormat.Value formatOverride)
        throws JsonMappingException;

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
    public abstract JsonSerializer<Object> createKeySerializer(SerializerProvider ctxt,
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
     * @deprecated Since 3.0 use variant that takes {@code JsonFormat.Value} argument
     */
    @Deprecated // since 3.0
    public JsonSerializer<Object> createSerializer(SerializerProvider ctxt, JavaType baseType)
        throws JsonMappingException
    {
        BeanDescription beanDesc = ctxt.introspectBeanDescription(baseType);
        return createSerializer(ctxt, baseType, beanDesc, null);
    }
}
