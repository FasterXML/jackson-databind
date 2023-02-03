package com.fasterxml.jackson.databind.deser.impl;

import java.io.IOException;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.annotation.ObjectIdResolver;

import com.fasterxml.jackson.core.JsonParser;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;

/**
 * Object that knows how to deserialize Object Ids.
 */
public class ObjectIdReader
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    protected final JavaType _idType;

    public final PropertyName propertyName;

    /**
     * Blueprint generator instance: actual instance will be
     * fetched from {@link SerializerProvider} using this as
     * the key.
     */
    public final ObjectIdGenerator<?> generator;

    public final ObjectIdResolver resolver;

    /**
     * Deserializer used for deserializing id values.
     */
    protected final JsonDeserializer<Object> _deserializer;

    public final SettableBeanProperty idProperty;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    @SuppressWarnings("unchecked")
    protected ObjectIdReader(JavaType t, PropertyName propName, ObjectIdGenerator<?> gen,
            JsonDeserializer<?> deser, SettableBeanProperty idProp, ObjectIdResolver resolver)
    {
        _idType = t;
        propertyName = propName;
        generator = gen;
        this.resolver = resolver;
        _deserializer = (JsonDeserializer<Object>) deser;
        idProperty = idProp;
    }

    /**
     * Factory method called by {@link com.fasterxml.jackson.databind.ser.std.BeanSerializerBase}
     * with the initial information based on standard settings for the type
     * for which serializer is being built.
     */
    public static ObjectIdReader construct(JavaType idType, PropertyName propName,
            ObjectIdGenerator<?> generator, JsonDeserializer<?> deser,
            SettableBeanProperty idProp, ObjectIdResolver resolver)
    {
        return new ObjectIdReader(idType, propName, generator, deser, idProp, resolver);
    }

    /*
    /**********************************************************
    /* API
    /**********************************************************
     */

    public JsonDeserializer<Object> getDeserializer() {
        return _deserializer;
    }

    public JavaType getIdType() {
        return _idType;
    }

    /**
     * Convenience method, equivalent to calling:
     *<code>
     *  readerInstance.generator.maySerializeAsObject();
     *</code>
     * and used to determine whether Object Ids handled by the underlying
     * generator may be in form of (JSON) Objects.
     * Used for optimizing handling in cases where method returns false.
     *
     * @since 2.5
     */
    public boolean maySerializeAsObject() {
        return generator.maySerializeAsObject();
    }

    /**
     * Convenience method, equivalent to calling:
     *<code>
     *  readerInstance.generator.isValidReferencePropertyName(name, parser);
     *</code>
     * and used to determine whether Object Ids handled by the underlying
     * generator may be in form of (JSON) Objects.
     * Used for optimizing handling in cases where method returns false.
     *
     * @since 2.5
     */
    public boolean isValidReferencePropertyName(String name, JsonParser parser) {
        return generator.isValidReferencePropertyName(name, parser);
    }

    /**
     * Method called to read value that is expected to be an Object Reference
     * (that is, value of an Object Id used to refer to another object).
     *
     * @since 2.3
     */
    public Object readObjectReference(JsonParser jp, DeserializationContext ctxt) throws IOException {
        return _deserializer.deserialize(jp, ctxt);
    }
}
