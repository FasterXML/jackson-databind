package com.fasterxml.jackson.databind.jsontype;

import java.util.Collection;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;

/**
 * Helper object used for handling registration on resolving of super-types
 * to sub-types.
 */
public abstract class SubtypeResolver
{
    /**
     * Method for registering specified subtypes (possibly including type
     * names); for type entries without name, non-qualified class name
     * as used as name (unless overridden by annotation).
     */
    public abstract void registerSubtypes(NamedType... types);

    public abstract void registerSubtypes(Class<?>... classes);

    /**
     * Method for finding out all reachable subtypes for a property specified
     * by given element (method or field)
     * 
     * @param baseType Effective property base type to use; may differ from
     *    actual type of property; for structured types it is content (value) type and NOT
     *    structured type.
     * 
     * @since 2.1
     */
    public abstract Collection<NamedType> collectAndResolveSubtypes(AnnotatedMember property,
            MapperConfig<?> config, AnnotationIntrospector ai, JavaType baseType);
    
    /**
     * Method for finding out all reachable subtypes for given type.
     */
    public abstract Collection<NamedType> collectAndResolveSubtypes(AnnotatedClass basetype,
            MapperConfig<?> config, AnnotationIntrospector ai);
}
