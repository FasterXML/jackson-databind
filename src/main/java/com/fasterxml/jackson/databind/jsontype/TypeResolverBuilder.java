package com.fasterxml.jackson.databind.jsontype;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.databind.*;

/**
 * Interface that defines builders that are configured based on
 * annotations (like {@link com.fasterxml.jackson.annotation.JsonTypeInfo} or JAXB annotations),
 * and produce type serializers and deserializers used for
 * handling type information embedded in JSON to allow for safe
 * polymorphic type handling.
 *<p>
 * Builder is first initialized by calling {@link #init} method.
 * Finally, after calling all configuration methods,
 * {@link #buildTypeSerializer} or {@link #buildTypeDeserializer}
 * will be called to get actual type resolver constructed
 * and used for resolving types for configured base type and its
 * subtypes.
 *<p>
 * Note that instances are used for two related but distinct use cases:
 *<ul>
 * <li>To create builders to use with explicit type information
 *    inclusion (usually via <code>@JsonTypeInfo</code> annotation)
 *   </li>
 * <li>To create builders when "default typing" is used; if so, type information
 *   is automatically included for certain kind of types, regardless of annotations
 *   </li>
 *</ul>
 * Important distinction between the cases is that in first case, calls to
 * create builders are only made when builders are certainly needed; whereas
 * in second case builder has to first verify whether type information is
 * applicable for given type, and if not, just return null to indicate this.
 */
public interface TypeResolverBuilder<T extends TypeResolverBuilder<T>>
{
    /*
    /**********************************************************************
    /* Accessors
    /**********************************************************************
     */

    /**
     * Accessor for currently configured default type; implementation
     * class that may be used in case no valid type information is
     * available during type resolution
     */
    public Class<?> getDefaultImpl();

    /*
    /**********************************************************************
    /* Actual builder methods
    /**********************************************************************
     */

    /**
     * Method for building type serializer based on current configuration
     * of this builder.
     * 
     * @param baseType Base type that constructed resolver will
     *    handle; super type of all types it will be used for.
     */
    public TypeSerializer buildTypeSerializer(SerializerProvider ctxt,
            JavaType baseType, Collection<NamedType> subtypes);

    /**
     * Method for building type deserializer based on current configuration
     * of this builder.
     * 
     * @param baseType Base type that constructed resolver will
     *    handle; super type of all types it will be used for.
     * @param subtypes Known subtypes of the base type.
     */
    public TypeDeserializer buildTypeDeserializer(DeserializationContext ctxt,
            JavaType baseType, Collection<NamedType> subtypes);

    /*
    /**********************************************************************
    /* Initialization method(s) that must be called before other configuration
    /**********************************************************************
     */

    /**
     * Initialization method that is called right after constructing
     * the builder instance, in cases where information could not be
     * passed directly (for example when instantiated for an annotation)
     *
     * @param settings Configuration settings to apply.
     * 
     * @return Resulting builder instance (usually this builder,
     *   but not necessarily)
     */
    public T init(JsonTypeInfo.Value settings, TypeIdResolver res);

    /*
    /**********************************************************************
    /* Mutant factories
    /**********************************************************************
     */

    /**
     * "Mutant factory" method for creating a new instance with different
     * default implementation to use if type id 
     * is either not available, or cannot be resolved.
     *<p>
     * In Jackson 2.x there was instead method {@code defaultImpl()} which
     * was a mutator: this method MUST NOT change the underlying state. 
     *
     * @return Either this instance (if nothing changed) or a new instance with
     *    different default implementation
     */
    public T withDefaultImpl(Class<?> defaultImpl);
}
