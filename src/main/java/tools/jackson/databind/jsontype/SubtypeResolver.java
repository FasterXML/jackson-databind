package tools.jackson.databind.jsontype;

import java.util.Collection;

import tools.jackson.core.util.Snapshottable;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.AnnotatedClass;
import tools.jackson.databind.introspect.AnnotatedMember;

/**
 * Helper object used for handling registration on resolving of super-types
 * to sub-types.
 */
public abstract class SubtypeResolver
    implements Snapshottable<SubtypeResolver>
{
    /*
    /**********************************************************************
    /* Snapshottable
    /**********************************************************************
     */

    /**
     * Method that has to create a new instance that contains
     * same registration information as this instance, but is not
     * linked to this instance.
     *
     * @since 3.0
     */
    @Override
    public abstract SubtypeResolver snapshot();

    /*
    /**********************************************************************
    /* Methods for registering external subtype definitions
    /**********************************************************************
     */

    /**
     * Method for registering specified subtypes (possibly including type
     * names); for type entries without name, non-qualified class name
     * as used as name (unless overridden by annotation).
     */
    public abstract SubtypeResolver registerSubtypes(NamedType... types);

    public abstract SubtypeResolver registerSubtypes(Class<?>... classes);

    public abstract SubtypeResolver registerSubtypes(Collection<Class<?>> subtypes);

    /*
    /**********************************************************************
    /* Subtype resolution
    /**********************************************************************
     */

    /**
     * Method for finding out all reachable subtypes for a property specified
     * by given element (method or field),
     * such that access is by type,
     * typically needed for serialization (converting from type to type name).
     *
     * @param baseType Effective property base type to use; may differ from
     *    actual type of property; for structured types it is content (value) type and NOT
     *    structured type.
     */
    public abstract Collection<NamedType> collectAndResolveSubtypesByClass(MapperConfig<?> config,
            AnnotatedMember property, JavaType baseType);

    /**
     * Method for finding out all reachable subtypes for given type,
     * such that access is by type,
     * typically needed for serialization (converting from type to type name).
     *
     * @param baseType Effective property base type to use; may differ from
     *    actual type of property; for structured types it is content (value) type and NOT
     *    structured type.
     */
    public abstract Collection<NamedType> collectAndResolveSubtypesByClass(MapperConfig<?> config,
            AnnotatedClass baseType);

    /**
     * Method for finding out all reachable subtypes for a property specified
     * by given element (method or field),
     * such that access is by type id,
     * typically needed for deserialization (converting from type id to type).
     *
     * @param baseType Effective property base type to use; may differ from
     *    actual type of property; for structured types it is content (value) type and NOT
     *    structured type.
     */
    public abstract Collection<NamedType> collectAndResolveSubtypesByTypeId(MapperConfig<?> config,
            AnnotatedMember property, JavaType baseType);

    /**
     * Method for finding out all reachable subtypes for given type,
     * such that access is by type id,
     * typically needed for deserialization (converting from type id to type).
     *
     * @param baseType Effective property base type to use; may differ from
     *    actual type of property; for structured types it is content (value) type and NOT
     *    structured type.
     */
    public abstract Collection<NamedType> collectAndResolveSubtypesByTypeId(MapperConfig<?> config,
            AnnotatedClass baseType);
}
