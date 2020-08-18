package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.databind.cfg.MapperConfig;

/**
 * API for handlers used to "mangle" names of "getter" and "setter" methods to
 * find implicit property names.
 *
 * @since 2.12
 */
public abstract class AccessorNamingStrategy
{
    /**
     * Method called to find whether given method would be considered an "is-getter"
     * method in context of type introspected.
     *<p>
     * Note that signature acceptability has already been checked (no arguments,
     * has return value) but NOT the specific limitation that return type should
     * be of boolean type -- implementation should apply latter check, if so desired
     * (some languages may use different criteria).
     */
    public abstract String findNameForIsGetter(AnnotatedMethod am, String name);

    /**
     * Method called to find whether given method would be considered a "regular"
     * getter method in context of type introspected.
     *<p>
     * Note that signature acceptability has already been checked (no arguments,
     * does have a return value) by caller.
     *<p>
     * Note that this method MAY be called for potential "is-getter" methods too
     * (before {@link #findNameForIsGetter})
     */
    public abstract String findNameForRegularGetter(AnnotatedMethod am, String name);

    /**
     * Method called to find whether given method would be considered a "mutator"
     * (usually setter, but for builders "with-method" or similar) in context of
     * type introspected.
     *<p>
     * Note that signature acceptability has already been checked (exactly one parameter)
     * by caller.
     */
    public abstract String findNameForMutator(AnnotatedMethod am, String name);

    // FunctionalInterface
    /**
     * Interface for provider (factory) for constructing {@link AccessorNamingStrategy} for given
     * type.
     */
    public abstract static class Provider
        implements java.io.Serializable // since one configured with Mapper/MapperBuilder
    {
        private static final long serialVersionUID = 1L;

        public abstract AccessorNamingStrategy forPOJO(MapperConfig<?> config, AnnotatedClass valueClass);

        public abstract AccessorNamingStrategy forBuilder(MapperConfig<?> config,
                AnnotatedClass builderClass);
    }
}
