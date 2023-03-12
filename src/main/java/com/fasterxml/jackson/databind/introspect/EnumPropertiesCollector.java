package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.databind.EnumNamingStrategy;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * Helper class used for aggregating information about all possible
 * properties of a Enum.
 *
 * @since 2.15
 */
public class EnumPropertiesCollector {

    private EnumPropertiesCollector() {}

    /*
    /**********************************************************
    /* Public API: main-level collection
    /**********************************************************
     */

    /**
     * Factory method for creating an instance of {@link EnumNamingStrategy} from a provided {@code namingDef}.
     *
     * @param namingDef subclass of {@link EnumNamingStrategy} to initialize an instance of.
     * @param canOverrideAccessModifiers whether to override access modifiers when instantiating the naming strategy.
     *
     * @throws IllegalArgumentException if {@code namingDef} is not an instance of {@link java.lang.Class} or
     *              not a subclass of {@link EnumNamingStrategy}.
     *
     * @return an instance of {@link EnumNamingStrategy} if {@code namingDef} is a subclass of {@link EnumNamingStrategy},
     *         {@code null} if {@code namingDef} is {@code null},
     *         and an instance of {@link EnumNamingStrategy} if {@code namingDef} already is one.
     *
     * @since 2.15
     */
    public static EnumNamingStrategy createEnumNamingStrategyInstance(Object namingDef, boolean canOverrideAccessModifiers) {
        if (namingDef == null) {
            return null;
        }
        if (namingDef instanceof EnumNamingStrategy) {
            return (EnumNamingStrategy) namingDef;
        }
        if (!(namingDef instanceof Class)) {
            reportProblem("AnnotationIntrospector returned EnumNamingStrategy definition of type %s"
                + "; expected type `Class<EnumNamingStrategy>` instead", ClassUtil.classNameOf(namingDef));
        }

        Class<?> namingClass = (Class<?>) namingDef;

        if (namingClass == EnumNamingStrategy.class) {
            return null;
        }
        if (!EnumNamingStrategy.class.isAssignableFrom(namingClass)) {
            reportProblem("AnnotationIntrospector returned Class %s; expected `Class<EnumNamingStrategy>`",
                ClassUtil.classNameOf(namingClass));
        }
        return (EnumNamingStrategy) ClassUtil.createInstance(namingClass, canOverrideAccessModifiers);
    }

    /*
     *********************************************************
     * Internal methods; helpers
     **********************************************************
     */

    /**
     * @throws IllegalArgumentException with provided message.
     *
     * @since 2.15
     */
    protected static void reportProblem(String msg, Object... args) {
        if (args.length > 0) {
            msg = String.format(msg, args);
        }
        throw new IllegalArgumentException("Problem with " + msg);
    }

}
