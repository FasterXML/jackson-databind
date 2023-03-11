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
     * @since 2.15
     */
    public static EnumNamingStrategy createEnumNamingStrategyInstance(Object namingDef, boolean canOverrideAccessModifiers) {
        if (namingDef == null) {
            return null;
        }

        if (namingDef instanceof EnumNamingStrategy) {
            return (EnumNamingStrategy) namingDef;
        }
        // Alas, there's no way to force return type of "either class
        // X or Y" -- need to throw an exception after the fact
        if (!(namingDef instanceof Class)) {
            reportProblem("AnnotationIntrospector returned EnumNamingStrategy definition of type %s"
                + "; expected type `Class<EnumNamingStrategy>` instead", ClassUtil.classNameOf(namingDef));
        }

        Class<?> namingClass = (Class<?>) namingDef;
        // 09-Nov-2015, tatu: Need to consider pseudo-value of STD, which means "use default"
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
     * @since 2.15
     */
    protected static void reportProblem(String msg, Object... args) {
        if (args.length > 0) {
            msg = String.format(msg, args);
        }
        throw new IllegalArgumentException("Problem with " + msg);
    }

}
