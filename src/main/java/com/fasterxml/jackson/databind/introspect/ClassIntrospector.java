package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.cfg.MapperConfig;

/**
 * Helper class used to introspect features of POJO value classes
 * used with Jackson. The main use is for finding out
 * POJO construction (creator) and value access (getters, setters)
 * methods and annotations that define configuration of using
 * those methods.
 */
public abstract class ClassIntrospector
{
    /*
    /**********************************************************
    /* Helper interfaces
    /**********************************************************
     */

    /**
     * Interface used for decoupling details of how mix-in annotation
     * definitions are accessed (via this interface), and how
     * they are stored (defined by classes that implement the interface)
     */
    public interface MixInResolver
    {
        /**
         * Method that will check if there are "mix-in" classes (with mix-in
         * annotations) for given class
         */
        public Class<?> findMixInClassFor(Class<?> cls);

        /**
         * Method called to create a new, non-shared copy, to be used by different
         * <code>ObjectMapper</code> instance, and one that should not be connected
         * to this instance, if resolver has mutable state.
         * If resolver is immutable may simply return `this`.
         *
         * @since 2.6
         */
        public MixInResolver copy();
    }

    protected ClassIntrospector() { }

    /**
     * Method that may be needed when `copy()`ing `ObjectMapper` instances.
     *
     * @since 2.9.6
     */
    public abstract ClassIntrospector copy();

    /*
    /**********************************************************
    /* Public API: factory methods
    /**********************************************************
     */

    /**
     * Factory method that constructs an introspector that has all
     * information needed for serialization purposes.
     */
    public abstract BeanDescription forSerialization(SerializationConfig cfg,
    		JavaType type, MixInResolver r);

    /**
     * Factory method that constructs an introspector that has all
     * information needed for deserialization purposes.
     */
    public abstract BeanDescription forDeserialization(DeserializationConfig cfg,
    		JavaType type, MixInResolver r);

    /**
     * Factory method that constructs an introspector that has all
     * information needed for constructing deserializers that use
     * intermediate Builder objects.
     *
     * @since 2.12
     */
    public abstract BeanDescription forDeserializationWithBuilder(DeserializationConfig cfg,
            JavaType builderType, MixInResolver r, BeanDescription valueTypeDesc);

    /**
     * @deprecated Since 2.12 use overload that take value type description
     */
    @Deprecated
    public abstract BeanDescription forDeserializationWithBuilder(DeserializationConfig cfg,
            JavaType builderType, MixInResolver r);

    /**
     * Factory method that constructs an introspector that has
     * information necessary for creating instances of given
     * class ("creator"), as well as class annotations, but
     * no information on member methods
     */
    public abstract BeanDescription forCreation(DeserializationConfig cfg, JavaType type,
            MixInResolver r);

    /**
     * Factory method that constructs an introspector that only has
     * information regarding annotations class itself (or its supertypes) has,
     * but nothing on methods or constructors.
     */
    public abstract BeanDescription forClassAnnotations(MapperConfig<?> cfg, JavaType type,
            MixInResolver r);

    /**
     * Factory method that constructs an introspector that only has
     * information regarding annotations class itself has (but NOT including
     * its supertypes), but nothing on methods or constructors.
     */
    public abstract BeanDescription forDirectClassAnnotations(MapperConfig<?> cfg, JavaType type,
            MixInResolver r);
}
