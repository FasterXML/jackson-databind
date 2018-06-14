package com.fasterxml.jackson.databind;


/**
 * Defines interface for resolvers that can resolve abstract types into concrete
 * ones; either by using static mappings, or possibly by materializing
 * implementations dynamically.
 */
public abstract class AbstractTypeResolver
{
    /**
     * Try to locate a subtype for given abstract type, to either resolve
     * to a concrete type, or at least to a more-specific (and hopefully supported)
     * abstract type, one which may have registered deserializers.
     * Method is called before trying to locate registered deserializers
     * (as well as standard abstract type defaulting that core Jackson does),
     * so it is typically implemented to add custom mappings of common abstract
     * types (like specify which concrete implementation to use for binding
     * {@link java.util.List}s).
     *<p>
     * Note that this method does not necessarily have to do full resolution
     * of bindings; that is, it is legal to return type that could be further
     * resolved: caller is expected to keep calling this method on registered
     * resolvers, until a concrete type is located.
     * 
     * @param config Configuration in use; should always be of type
     *    {@code DeserializationConfig}
     */
    public JavaType findTypeMapping(DeserializationConfig config, JavaType type) {
        return null;
    }

    /**
     * Method called to try to resolve an abstract type into
     * concrete type (usually for purposes of deserializing),
     * when no concrete implementation was found.
     * It will be called after checking all other possibilities,
     * including defaulting.
     *
     * @param config Configuration in use; should always be of type
     *    <code>DeserializationConfig</code>
     * @param typeDesc Description of the POJO type to resolve
     * 
     * @return Resolved concrete type (which should retain generic
     *    type parameters of input type, if any), if resolution succeeds;
     *    null if resolver does not know how to resolve given type
     */
    public JavaType resolveAbstractType(DeserializationConfig config,
            BeanDescription typeDesc) {
        return null;
    }
}
