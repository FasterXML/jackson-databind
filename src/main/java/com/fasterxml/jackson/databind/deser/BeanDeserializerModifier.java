package com.fasterxml.jackson.databind.deser;

import java.util.List;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.type.ArrayType;
import com.fasterxml.jackson.databind.type.CollectionLikeType;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapLikeType;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.ReferenceType;

/**
 * Abstract class that defines API for objects that can be registered
 * to participate in constructing {@link JsonDeserializer} instances
 * (via {@link DeserializerFactory}).
 * This is typically done by modules that want alter some aspects of deserialization
 * process; and is preferable to sub-classing of {@link BeanDeserializerFactory}.
 *<p>
 * Note that Jackson 2.2 adds more methods for customization; with earlier versions
 * only {@link BeanDeserializer} instances could be modified, but with 2.2 all types
 * of deserializers can be changed.
 *<p>
 * Sequence in which callback methods are called for {@link BeanDeserializer} is:
 * <ol>
 *  <li>{@link #updateProperties} is called once all property definitions are
 *    collected, and initial filtering (by ignorable type and explicit ignoral-by-bean)
 *    has been performed.
 *   </li>
 *  <li>{@link #updateBuilder} is called once all initial pieces for building deserializer
 *    have been collected
 *   </li>
 *  <li>{@link #modifyDeserializer} is called after deserializer has been built
 *    by {@link BeanDeserializerBuilder}
 *    but before it is returned to be used
 *   </li>
 * </ol>
 *<p>
 * For other types of deserializers, methods called depend on type of values for
 * which deserializer is being constructed; and only a single method is called
 * since the process does not involve builders (unlike that of {@link BeanDeserializer}.
 *<p>
 * Default method implementations are "no-op"s, meaning that methods are implemented
 * but have no effect; this is mostly so that new methods can be added in later
 * versions.
 */
public abstract class BeanDeserializerModifier
{
    /**
     * Method called by {@link BeanDeserializerFactory} when it has collected
     * initial list of {@link BeanPropertyDefinition}s, and done basic by-name
     * and by-type filtering, but before constructing builder or actual
     * property handlers; or arranging order.
     *
     * The most common changes to make at this point are to completely remove
     * specified properties, or rename then: other modifications are easier
     * to make at later points.
     */
    public List<BeanPropertyDefinition> updateProperties(DeserializationConfig config,
            BeanDescription beanDesc, List<BeanPropertyDefinition> propDefs) {
        return propDefs;
    }

    /**
     * Method called by {@link BeanDeserializerFactory} when it has collected
     * basic information such as tentative list of properties to deserialize.
     *
     * Implementations may choose to modify state of builder (to affect deserializer being
     * built), or even completely replace it (if they want to build different kind of
     * deserializer). Typically changes mostly concern set of properties to deserialize.
     */
    public BeanDeserializerBuilder updateBuilder(DeserializationConfig config,
            BeanDescription beanDesc, BeanDeserializerBuilder builder) {
        return builder;
    }

    /**
     * Method called by {@link BeanDeserializerFactory} after constructing default
     * bean deserializer instance with properties collected and ordered earlier.
     * Implementations can modify or replace given deserializer and return deserializer
     * to use. Note that although initial deserializer being passed is usually of type
     * {@link BeanDeserializer}, modifiers may return deserializers of other types;
     * and this is why implementations must check for type before casting.
     *<p>
     * Since 2.10 this is also called for custom deserializers for types not deemed to
     * be of any more specific (reference, enum, array, collection(-like), map(-like),
     * node type)
     */
    public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config,
            BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
        return deserializer;
    }

    /*
    /**********************************************************
    /* Callback methods for other types (since 2.2)
    /**********************************************************
     */

    /**
     * Method called by {@link BeanDeserializerFactory} after constructing default
     * enum type deserializer instance.
     *
     * @since 2.2
     */
    public JsonDeserializer<?> modifyEnumDeserializer(DeserializationConfig config,
            JavaType type, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
        return deserializer;
    }

    /**
     * Method called by {@link BeanDeserializerFactory} after constructing default
     * {@link ReferenceType} deserializer instance.
     *
     * @since 2.7
     */
    public JsonDeserializer<?> modifyReferenceDeserializer(DeserializationConfig config,
            ReferenceType type, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
        return deserializer;
    }

    /**
     * Method called by {@link DeserializerFactory} after it has constructed the
     * standard deserializer for given
     * {@link ArrayType}
     * to make it possible to either replace or augment this deserializer with
     * additional functionality.
     *
     * @param config Configuration in use
     * @param valueType Type of the value deserializer is used for.
     * @param beanDesc Description f
     * @param deserializer Default deserializer that would be used.
     *
     * @return Deserializer to use; either <code>deserializer</code> that was passed
     *   in, or an instance method constructed.
     *
     * @since 2.2
     */
    public JsonDeserializer<?> modifyArrayDeserializer(DeserializationConfig config,
            ArrayType valueType, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
        return deserializer;
    }

    /**
     * Method called by {@link BeanDeserializerFactory} after constructing default
     * {@link CollectionType} deserializer instance.
     *
     * @since 2.2
     */
    public JsonDeserializer<?> modifyCollectionDeserializer(DeserializationConfig config,
            CollectionType type, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
        return deserializer;
    }

    /**
     * Method called by {@link BeanDeserializerFactory} after constructing default
     * {@link CollectionLikeType} deserializer instance.
     *
     * @since 2.2
     */
    public JsonDeserializer<?> modifyCollectionLikeDeserializer(DeserializationConfig config,
            CollectionLikeType type, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
        return deserializer;
    }

    /**
     * Method called by {@link BeanDeserializerFactory} after constructing default
     * {@link MapType} deserializer instance.
     *
     * @since 2.2
     */
    public JsonDeserializer<?> modifyMapDeserializer(DeserializationConfig config,
            MapType type, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
        return deserializer;
    }

    /**
     * Method called by {@link BeanDeserializerFactory} after constructing default
     * {@link MapLikeType} deserializer instance.
     *
     * @since 2.2
     */
    public JsonDeserializer<?> modifyMapLikeDeserializer(DeserializationConfig config,
            MapLikeType type, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
        return deserializer;
    }

    /**
     * Method called by {@link DeserializerFactory} after it has constructed the
     * standard key deserializer for given key type.
     * This make it possible to replace the default key deserializer, or augment
     * it somehow (including optional use of default deserializer with occasional
     * override).
     *
     * @since 2.2
     */
    public KeyDeserializer modifyKeyDeserializer(DeserializationConfig config,
            JavaType type, KeyDeserializer deserializer) {
        return deserializer;
    }
}