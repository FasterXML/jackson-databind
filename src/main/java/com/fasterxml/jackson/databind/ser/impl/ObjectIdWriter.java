package com.fasterxml.jackson.databind.ser.impl;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.databind.*;

/**
 * Object that knows how to serialize Object Ids.
 */
public final class ObjectIdWriter
{
    public final JavaType idType;

    /**
     * Name of id property to write, if not null: if null, should
     * only write references, but id property is handled by some
     * other entity.
     */
    public final SerializableString propertyName;

    /**
     * Blueprint generator instance: actual instance will be
     * fetched from {@link SerializerProvider} using this as
     * the key.
     */
    public final ObjectIdGenerator<?> generator;

    /**
     * Serializer used for serializing id values.
     */
    public final JsonSerializer<Object> serializer;

    /**
     * Marker that indicates what the first reference is to be
     * serialized as full POJO, or as Object Id (other references
     * will always be serialized as Object Id)
     *
     * @since 2.1
     */
    public final boolean alwaysAsId;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    @SuppressWarnings("unchecked")
    protected ObjectIdWriter(JavaType t, SerializableString propName,
            ObjectIdGenerator<?> gen, JsonSerializer<?> ser, boolean alwaysAsId)
    {
        idType = t;
        propertyName = propName;
        generator = gen;
        serializer = (JsonSerializer<Object>) ser;
        this.alwaysAsId = alwaysAsId;
    }

    /**
     * Factory method called by {@link com.fasterxml.jackson.databind.ser.std.BeanSerializerBase}
     * with the initial information based on standard settings for the type
     * for which serializer is being built.
     *
     * @since 2.3
     */
    public static ObjectIdWriter construct(JavaType idType, PropertyName propName,
            ObjectIdGenerator<?> generator, boolean alwaysAsId)
    {
        String simpleName = (propName == null) ? null : propName.getSimpleName();
        SerializableString serName = (simpleName == null) ? null : new SerializedString(simpleName);
        return new ObjectIdWriter(idType, serName, generator, null, alwaysAsId);
    }

    public ObjectIdWriter withSerializer(JsonSerializer<?> ser) {
        return new ObjectIdWriter(idType, propertyName, generator, ser, alwaysAsId);
    }

    /**
     * @since 2.1
     */
    public ObjectIdWriter withAlwaysAsId(boolean newState) {
        if (newState == alwaysAsId) {
            return this;
        }
        return new ObjectIdWriter(idType, propertyName, generator, serializer, newState);
    }
}
