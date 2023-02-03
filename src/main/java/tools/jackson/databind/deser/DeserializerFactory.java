package tools.jackson.databind.deser;

import tools.jackson.databind.*;
import tools.jackson.databind.type.*;

/**
 * Abstract class that defines API used by {@link DeserializationContext}
 * to construct actual
 * {@link ValueDeserializer} instances (which are then cached by
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
 * <li>For Tree Model ({@link tools.jackson.databind.JsonNode}) properties there is
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
    /**********************************************************************
    /* Basic DeserializerFactory API
    /**********************************************************************
     */

    /**
     * Method that is to find all creators (constructors, factory methods)
     * for the bean type to deserialize.
     */
    public abstract ValueInstantiator findValueInstantiator(DeserializationContext ctxt,
            BeanDescription beanDesc);

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
    public abstract ValueDeserializer<Object> createBeanDeserializer(DeserializationContext ctxt,
            JavaType type, BeanDescription beanDesc);

    /**
     * Method called to create a deserializer that will use specified Builder
     * class for building value instances.
     */
    public abstract ValueDeserializer<Object> createBuilderBasedDeserializer(
    		DeserializationContext ctxt, JavaType type, BeanDescription beanDesc,
    		Class<?> builderClass);

    public abstract ValueDeserializer<?> createEnumDeserializer(DeserializationContext ctxt,
            JavaType type, BeanDescription beanDesc);

    public abstract ValueDeserializer<?> createReferenceDeserializer(DeserializationContext ctxt,
            ReferenceType type, BeanDescription beanDesc);

    /**
     * Method called to create and return a deserializer that can construct
     * JsonNode(s) from JSON content.
     */
    public abstract ValueDeserializer<?> createTreeDeserializer(DeserializationConfig config,
            JavaType type, BeanDescription beanDesc);

    /**
     * Method called to create (or, for completely immutable deserializers,
     * reuse) a deserializer that can convert JSON content into values of
     * specified Java type.
     *
     * @param type Type to be deserialized
     */
    public abstract ValueDeserializer<?> createArrayDeserializer(DeserializationContext ctxt,
            ArrayType type, BeanDescription beanDesc);

    public abstract ValueDeserializer<?> createCollectionDeserializer(DeserializationContext ctxt,
            CollectionType type, BeanDescription beanDesc);

    public abstract ValueDeserializer<?> createCollectionLikeDeserializer(DeserializationContext ctxt,
            CollectionLikeType type, BeanDescription beanDesc);

    public abstract ValueDeserializer<?> createMapDeserializer(DeserializationContext ctxt,
            MapType type, BeanDescription beanDesc);

    public abstract ValueDeserializer<?> createMapLikeDeserializer(DeserializationContext ctxt,
            MapLikeType type, BeanDescription beanDesc);

    /**
     * Method called to find if factory knows how to create a key deserializer
     * for specified type; currently this means checking if a module has registered
     * possible deserializers.
     *
     * @return Key deserializer to use for specified type, if one found; null if not
     *   (and default key deserializer should be used)
     */
    public abstract KeyDeserializer createKeyDeserializer(DeserializationContext ctxt,
            JavaType type);

    /**
     * Method that can be used to check if databind module has explicitly declared deserializer
     * for given (likely JDK) type, explicit meaning that there is specific deserializer for
     * given type as opposed to auto-generated "Bean" deserializer. Factory itself will check
     * for known JDK-provided types, but registered {@link tools.jackson.databind.JacksonModule}s
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
     */
    public abstract boolean hasExplicitDeserializerFor(DatabindContext ctxt,
            Class<?> valueType);

    /*
    /**********************************************************************
    /* Mutant factories for registering additional configuration
    /**********************************************************************
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
     * {@link ValueDeserializerModifier}.
     */
    public abstract DeserializerFactory withDeserializerModifier(ValueDeserializerModifier modifier);

    /**
     * Convenience method for creating a new factory instance with additional
     * {@link ValueInstantiators}.
     */
    public abstract DeserializerFactory withValueInstantiators(ValueInstantiators instantiators);
}
