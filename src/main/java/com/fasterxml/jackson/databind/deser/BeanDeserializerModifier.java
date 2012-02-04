package com.fasterxml.jackson.databind.deser;

import java.util.List;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerFactory;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;

/**
 * Abstract class that defines API for objects that can be registered (for {@link BeanDeserializerFactory}
 * to participate in constructing {@link BeanDeserializer} instances.
 * This is typically done by modules that want alter some aspects of deserialization
 * process; and is preferable to sub-classing of {@link BeanDeserializerFactory}.
 *<p>
 * Sequence in which callback methods are called is as follows:
 *  <li>{@link #updateProperties} is called once all property definitions are
 *    collected, and initial filtering (by ignorable type and explicit ignoral-by-bean)
 *    has been performed.
 *  <li>{@link #updateBuilder} is called once all initial pieces for building deserializer
 *    have been collected
 *   </li>
 *  <li>{@link #modifyDeserializer} is called after deserializer has been built
 *    by {@link BeanDeserializerBuilder}
 *    but before it is returned to be used
 *   </li>
 * </ol>
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
     * to use. Note that although initial deserializer being passed is of type
     * {@link BeanDeserializer}, modifiers may return deserializers of other types;
     * and this is why implementations must check for type before casting.
     */
    public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config,
            BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
        return deserializer;
    }
}