package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.databind.BeanDescription;
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
     * getter method in context of
     * type introspected, and if so, what is the logical property it is associated with
     * (which in turn suggest external name for property)
     *<p>
     * Note that signature acceptability has already been checked (no arguments,
     * has return value) but NOT the specific limitation that return type should
     * be of boolean type -- implementation should apply latter check, if so desired
     * (some languages may use different criteria). It is also possible that some
     * implementations allow different return types than boolean types.
     *<p>
     * Note that visibility checks are applied separately; strategy does not need
     * to be concerned with that aspect.
     *
     * @param method Method to check
     * @param name Name to check (usually same as {@link AnnotatedMethod#getName()}
     *
     * @return Implied property name for is-getter method, if match; {@code null} to indicate
     *    that the name does not conform to expected naming convention
     */
    public abstract String findNameForIsGetter(AnnotatedMethod method, String name);

    /**
     * Method called to find whether given method would be considered a "regular"
     * getter method in context of
     * type introspected, and if so, what is the logical property it is associated with
     * (which in turn suggest external name for property)
     *<p>
     * Note that signature acceptability has already been checked (no arguments,
     * does have a return value) by caller.
     *<p>
     * Note that this method MAY be called for potential "is-getter" methods too
     * (before {@link #findNameForIsGetter})
     *<p>
     * Note that visibility checks are applied separately; strategy does not need
     * to be concerned with that aspect.
     *
     * @param method Method to check
     * @param name Name to check (usually same as {@link AnnotatedMethod#getName()}
     *
     * @return Implied property name for getter method, if match; {@code null} to indicate
     *    that the name does not conform to expected naming convention
     */
    public abstract String findNameForRegularGetter(AnnotatedMethod method, String name);

    /**
     * Method called to find whether given method would be considered a "mutator"
     * (usually setter, but for builders "with-method" or similar) in context of
     * type introspected, and if so, what is the logical property it is associated with
     * (which in turn suggest external name for property)
     *<p>
     * Note that signature acceptability has already been checked (exactly one parameter)
     * by caller.
     *<p>
     * Note that visibility checks are applied separately; strategy does not need
     * to be concerned with that aspect.
     *
     * @param method Method to check
     * @param name Name to check (usually same as {@link AnnotatedMethod#getName()}
     *
     * @return Implied property name for mutator method, if match; {@code null}
     *   to indicate that the name does not conform to expected naming convention
     */
    public abstract String findNameForMutator(AnnotatedMethod method, String name);

    /**
     * Method called to find the name of logical property that given field should
     * be associated with, if any.
     *<p>
     * Note that visibility checks are applied separately; strategy does not need
     * to be concerned with that aspect.
     *
     * @param field Field to check
     * @param name Name to check (usually same as {@link AnnotatedField#getName()}
     *
     * @return Implied property name matching given field (often field name as-is) or {@code null}
     *   to indicate that the name does not conform to expected naming convention
     *   (and will not be considered for property access)
     */
    public abstract String modifyFieldName(AnnotatedField field, String name);

    /**
     * Helper class that implements all abstract methods with dummy implementations.
     * Behavior is as follows:
     *<ul>
     * <li>No getter or is-getter methods are recognized: relevant methods return {@code null}
     *  <li>
     * <li>No setter methods are recognized: relevant methods return {@code null}
     *  <li>
     * <li>Names of fields are returned as-is, without modifications (meaning they may be
     * discovered if they are otherwise visible
     *  <li>
     * </ul>
     */
    public static class Base
        extends AccessorNamingStrategy
        implements java.io.Serializable // since one configured with Mapper/MapperBuilder
    {
        private static final long serialVersionUID = 1L;

        @Override
        public String findNameForIsGetter(AnnotatedMethod method, String name) {
            return null;
        }

        @Override
        public String findNameForRegularGetter(AnnotatedMethod method, String name) {
            return null;
        }

        @Override
        public String findNameForMutator(AnnotatedMethod method, String name) {
            return null;
        }

        @Override
        public String modifyFieldName(AnnotatedField field, String name) {
            return name;
        }
    }

    /**
     * Interface for provider (factory) for constructing {@link AccessorNamingStrategy}
     * for given type of deserialization target
     */
    public abstract static class Provider
        implements java.io.Serializable // since one configured with Mapper/MapperBuilder
    {
        private static final long serialVersionUID = 1L;

        /**
         * Factory method for creating strategy instance for a "regular" POJO,
         * called if none of the other factory methods is applicable.
         *
         * @param config Current mapper configuration
         * @param valueClass Information about value type
         *
         * @return Naming strategy instance to use
         */
        public abstract AccessorNamingStrategy forPOJO(MapperConfig<?> config,
                AnnotatedClass valueClass);

        /**
         * Factory method for creating strategy instance for POJOs
         * that are deserialized using Builder type: in this case eventual
         * target (value) type is different from type of "builder" object that is
         * used by databinding to accumulate state.
         *
         * @param config Current mapper configuration
         * @param builderClass Information about builder type
         * @param valueTypeDesc Information about the eventual target (value) type
         *
         * @return Naming strategy instance to use
         */
        public abstract AccessorNamingStrategy forBuilder(MapperConfig<?> config,
                AnnotatedClass builderClass, BeanDescription valueTypeDesc);

        /**
         * Factory method for creating strategy instance for special {@code java.lang.Record}
         * type (new in JDK 14).
         *
         * @param config Current mapper configuration
         * @param recordClass Information about value type (of type {@code java.lang.Record})
         *
         * @return Naming strategy instance to use
         */
        public abstract AccessorNamingStrategy forRecord(MapperConfig<?> config,
                AnnotatedClass recordClass);
    }
}
