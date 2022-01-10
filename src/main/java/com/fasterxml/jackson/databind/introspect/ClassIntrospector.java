package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.databind.*;
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
    protected ClassIntrospector() { }

    /**
     * Method called to create an instance to be exclusive used by specified
     * mapper. Needed to ensure that no sharing through cache occurs.
     *<p>
     * Basic implementation just returns instance itself.
     *
     * @since 3.0
     */
    public abstract ClassIntrospector forMapper();

    /**
     * Method called to further create an instance to be used for a single operation
     * (read or write, typically matching {@link ObjectMapper} {@code readValue()} or
     * {@code writeValue()}).
     */
    public abstract ClassIntrospector forOperation(MapperConfig<?> config);
    
    /*
    /**********************************************************************
    /* Public API: annotation introspection
    /**********************************************************************
     */

    /**
     * Factory method that constructs an introspector that only has
     * information regarding annotations class itself (or its supertypes) has,
     * but nothing on methods or constructors.
     */
    public abstract AnnotatedClass introspectClassAnnotations(JavaType type);

    /**
     * Factory method that constructs an introspector that only has
     * information regarding annotations class itself has (but NOT including
     * its supertypes), but nothing on methods or constructors.
     */
    public abstract AnnotatedClass introspectDirectClassAnnotations(JavaType type);

    /*
    /**********************************************************************
    /* Public API: bean property introspection
    /**********************************************************************
     */

    /**
     * Factory method that constructs an introspector that has all
     * information needed for serialization purposes.
     */
    public abstract BeanDescription introspectForSerialization(JavaType type);

    /**
     * Factory method that constructs an introspector that has all
     * information needed for deserialization purposes.
     */
    public abstract BeanDescription introspectForDeserialization(JavaType type);

    /**
     * Factory method that constructs an introspector that has all
     * information needed for constructing deserializers that use
     * intermediate Builder objects.
     */
    public abstract BeanDescription introspectForDeserializationWithBuilder(JavaType builderType,
            BeanDescription valueTypeDesc);

    /**
     * Factory method that constructs an introspector that has
     * information necessary for creating instances of given
     * class ("creator"), as well as class annotations, but
     * no information on member methods
     */
    public abstract BeanDescription introspectForCreation(JavaType type);
}
