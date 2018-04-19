package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.core.util.Snapshottable;
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
    implements Snapshottable<ClassIntrospector>
{
    protected ClassIntrospector() { }

//    public abstract ClassIntrospector snapshot();

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
     */
    public abstract BeanDescription forDeserializationWithBuilder(DeserializationConfig cfg,
    		JavaType type, MixInResolver r);
    
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
