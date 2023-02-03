package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.type.*;

/**
 * Abstract class that defines API used by {@link DeserializationContext}
 * to construct actual
 * {@link JsonDeserializer} instances (which are then cached by
 * context and/or dedicated cache).
 *<p>
 * Since there are multiple broad categories of deserializers, there are
 * multiple factory methods:
 *<ul>
 * <li>For JSON "Array" type, we need 2 methods: one to deal with expected
 *   Java arrays ({@link #createArrayDeserializer})
 *   and the other for other Java containers like {@link java.util.List}s
 *   and {@link java.util.Set}s ({@link #createCollectionDeserializer}).
 *   Actually there is also a third method for "Collection-like" types;
 *   things like Scala collections that act like JDK collections but do not
 *   implement same interfaces.
 *  </li>
 * <li>For JSON "Object" type, we need 2 methods: one to deal with
 *   expected Java {@link java.util.Map}s
 *   ({@link #createMapDeserializer}), and another for POJOs
 *   ({@link #createBeanDeserializer}.
 *   As an additional twist there is also a callback for "Map-like" types,
 *   mostly to make it possible to support Scala Maps (which are NOT JDK
 *   Map compatible).
 *  </li>
 * <li>For Tree Model ({@link com.fasterxml.jackson.databind.JsonNode}) properties there is
 *    {@link #createTreeDeserializer}
 * <li>For enumerated types ({@link java.lang.Enum}) there is
 *    {@link #createEnumDeserializer}
 *  </li>
 * <li>For all other types, {@link #createBeanDeserializer} is used.
 * </ul>
 *<p>
 */
public abstract class DeserializerFactory
{
    protected final static Deserializers[] NO_DESERIALIZERS = new Deserializers[0];

    /*
    /********************************************************
    /* Configuration handling
    /********************************************************
     */

    /**
     * Convenience method for creating a new factory instance with additional deserializer
     * provider.
     */
    public abstract DeserializerFactory withAdditionalDeserializers(Deserializers additional);

    /**
     * Convenience method for creating a new factory instance with additional
     * {@link KeyDeserializers}.
     */
    public abstract DeserializerFactory withAdditionalKeyDeserializers(KeyDeserializers additional);

    /**
     * Convenience method for creating a new factory instance with additional
     * {@link BeanDeserializerModifier}.
     */
    public abstract DeserializerFactory withDeserializerModifier(BeanDeserializerModifier modifier);

    /**
     * Convenience method for creating a new factory instance with additional
     * {@link AbstractTypeResolver}.
     */
    public abstract DeserializerFactory withAbstractTypeResolver(AbstractTypeResolver resolver);

    /**
     * Convenience method for creating a new factory instance with additional
     * {@link ValueInstantiators}.
     */
    public abstract DeserializerFactory withValueInstantiators(ValueInstantiators instantiators);

    /*
    /**********************************************************
    /* Basic DeserializerFactory API:
    /**********************************************************
     */

    /**
     * Method that can be called to try to resolve an abstract type
     * (interface, abstract class) into a concrete type, or at least
     * something "more concrete" (abstract class instead of interface).
     * Will either return passed type, or a more specific type.
     */
    public abstract JavaType mapAbstractType(DeserializationConfig config, JavaType type)
        throws JsonMappingException;

    /**
     * Method that is to find all creators (constructors, factory methods)
     * for the bean type to deserialize.
     */
    public abstract ValueInstantiator findValueInstantiator(DeserializationContext ctxt,
            BeanDescription beanDesc)
        throws JsonMappingException;

    /**
     * Method called to create (or, for completely immutable deserializers,
     * reuse) a deserializer that can convert JSON content into values of
     * specified Java "bean" (POJO) type.
     * At this point it is known that the type is not otherwise recognized
     * as one of structured types (array, Collection, Map) or a well-known
     * JDK type (enum, primitives/wrappers, String); this method only
     * gets called if other options are exhausted. This also means that
     * this method can be overridden to add support for custom types.
     *
     * @param type Type to be deserialized
     */
    public abstract JsonDeserializer<Object> createBeanDeserializer(DeserializationContext ctxt,
            JavaType type, BeanDescription beanDesc)
        throws JsonMappingException;

    /**
     * Method called to create a deserializer that will use specified Builder
     * class for building value instances.
     */
    public abstract JsonDeserializer<Object> createBuilderBasedDeserializer(
    		DeserializationContext ctxt, JavaType type, BeanDescription beanDesc,
    		Class<?> builderClass)
        throws JsonMappingException;


    public abstract JsonDeserializer<?> createEnumDeserializer(DeserializationContext ctxt,
            JavaType type, BeanDescription beanDesc)
        throws JsonMappingException;

    /**
     * @since 2.7
     */
    public abstract JsonDeserializer<?> createReferenceDeserializer(DeserializationContext ctxt,
            ReferenceType type, BeanDescription beanDesc)
        throws JsonMappingException;

    /**
     * Method called to create and return a deserializer that can construct
     * JsonNode(s) from JSON content.
     */
    public abstract JsonDeserializer<?> createTreeDeserializer(DeserializationConfig config,
            JavaType type, BeanDescription beanDesc)
        throws JsonMappingException;

    /**
     * Method called to create (or, for completely immutable deserializers,
     * reuse) a deserializer that can convert JSON content into values of
     * specified Java type.
     *
     * @param type Type to be deserialized
     */
    public abstract JsonDeserializer<?> createArrayDeserializer(DeserializationContext ctxt,
            ArrayType type, BeanDescription beanDesc)
        throws JsonMappingException;

    public abstract JsonDeserializer<?> createCollectionDeserializer(DeserializationContext ctxt,
            CollectionType type, BeanDescription beanDesc)
        throws JsonMappingException;

    public abstract JsonDeserializer<?> createCollectionLikeDeserializer(DeserializationContext ctxt,
            CollectionLikeType type, BeanDescription beanDesc)
        throws JsonMappingException;

    public abstract JsonDeserializer<?> createMapDeserializer(DeserializationContext ctxt,
            MapType type, BeanDescription beanDesc)
        throws JsonMappingException;

    public abstract JsonDeserializer<?> createMapLikeDeserializer(DeserializationContext ctxt,
            MapLikeType type, BeanDescription beanDesc)
        throws JsonMappingException;

    /**
     * Method called to find if factory knows how to create a key deserializer
     * for specified type; currently this means checking if a module has registered
     * possible deserializers.
     *
     * @return Key deserializer to use for specified type, if one found; null if not
     *   (and default key deserializer should be used)
     */
    public abstract KeyDeserializer createKeyDeserializer(DeserializationContext ctxt,
            JavaType type)
        throws JsonMappingException;

    /**
     * Method called to find and create a type information deserializer for given base type,
     * if one is needed. If not needed (no polymorphic handling configured for type),
     * should return null.
     *<p>
     * Note that this method is usually only directly called for values of container (Collection,
     * array, Map) types and root values, but not for bean property values.
     *
     * @param baseType Declared base type of the value to deserializer (actual
     *    deserializer type will be this type or its subtype)
     *
     * @return Type deserializer to use for given base type, if one is needed; null if not.
     */
    public abstract TypeDeserializer findTypeDeserializer(DeserializationConfig config,
            JavaType baseType)
        throws JsonMappingException;

    /**
     * Method that can be used to check if databind module has explicitly declared deserializer
     * for given (likely JDK) type, explicit meaning that there is specific deserializer for
     * given type as opposed to auto-generated "Bean" deserializer. Factory itself will check
     * for known JDK-provided types, but registered {@link com.fasterxml.jackson.databind.Module}s
     * are also called to see if they might provide explicit deserializer.
     *<p>
     * Main use for this method is with Safe Default Typing (and generally Safe Polymorphic
     * Deserialization), during which it is good to be able to check that given raw type
     * is explicitly supported and as such "known type" (as opposed to potentially
     * dangerous "gadget type" which could be exploited).
     *<p>
     * This matches {@code Deserializers.Base.hasDeserializerFor(Class)} method, which is
     * the mechanism used to determine if a {@code Module} might provide an explicit
     * deserializer instead of core databind.
     *
     * @since 2.11
     */
    public abstract boolean hasExplicitDeserializerFor(DeserializationConfig config,
            Class<?> valueType);
}
