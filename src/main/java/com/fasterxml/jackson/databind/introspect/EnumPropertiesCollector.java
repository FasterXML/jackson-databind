package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.EnumNamingStrategy;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.annotation.EnumNaming;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
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
     * Helper method to resolve the an {@link EnumNamingStrategy} for the current {@link MapperConfig}
     * using {@link AnnotationIntrospector}.
     *
     * @return An instance of a subtype of {@link EnumNamingStrategy} specified through
     * {@link EnumNaming#value()} or null if current Enum class is not annotated with {@link EnumNaming} or
     * {@link EnumNaming#value()} is set to {@link EnumNamingStrategy} interface itself.
     * @since 2.15
     */
    public static EnumNamingStrategy findEnumNamingStrategy(MapperConfig<?> config, Class<?> handledType) {
        AnnotatedClass classDef = _findAnnotatedClass(config, handledType);
        Object namingDef = config.getAnnotationIntrospector().findEnumNamingStrategy(classDef);
        return _findEnumNamingStrategy(namingDef, config.canOverrideAccessModifiers());

    }

    /*
    /**********************************************************
    * Actual Implementation
    **********************************************************
    */

    /**
     * @since 2.15
     */
    private static EnumNamingStrategy _findEnumNamingStrategy(Object namingDef, boolean canOverrideAccessModifiers) {
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
    protected static AnnotatedClass _findAnnotatedClass(MapperConfig<?> ctxt, Class<?> handledType) {
        JavaType javaType = ctxt.constructType(handledType);
        return AnnotatedClassResolver.resolve(ctxt, javaType, ctxt);
    }

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
