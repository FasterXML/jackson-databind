package tools.jackson.databind.introspect;

import tools.jackson.databind.EnumNamingStrategy;
import tools.jackson.databind.util.ClassUtil;

/**
 * Helper class used for aggregating information about all possible
 * properties of a Enum.
 */
public class EnumNamingStrategyFactory
{
    /**
     * Factory method for creating an instance of {@link EnumNamingStrategy} from a provided {@code namingDef}.
     *
     * @param namingDef                  subclass of {@link EnumNamingStrategy} to initialize an instance of.
     * @param canOverrideAccessModifiers whether to override access modifiers when instantiating the naming strategy.
     * @param defaultNamingStrategy      configured global {@link EnumNamingStrategy} to use in case {@code namingDef} is not provided.
     *
     * @return an instance of {@link EnumNamingStrategy} if {@code namingDef} is a subclass of {@link EnumNamingStrategy},
     * {@code defaultNamingStrategy} if {@code namingDef} is {@code null},
     * and an instance of {@link EnumNamingStrategy} if {@code namingDef} already is one.
     *
     * @throws IllegalArgumentException if {@code namingDef} is not an instance of {@link java.lang.Class} or
     *                                  not a subclass of {@link EnumNamingStrategy}.
     */
    public static EnumNamingStrategy createEnumNamingStrategyInstance(Object namingDef,
            boolean canOverrideAccessModifiers,
            EnumNamingStrategy defaultNamingStrategy) {
        if (namingDef == null) {
            return defaultNamingStrategy;
        }
        if (namingDef instanceof EnumNamingStrategy strategy) {
            return strategy;
        }
        if (!(namingDef instanceof Class)) {
            throw new IllegalArgumentException(String.format(
                "AnnotationIntrospector returned EnumNamingStrategy definition of type %s; " +
                    "expected type `Class<EnumNamingStrategy>` instead", ClassUtil.classNameOf(namingDef)));
        }

        Class<?> namingClass = (Class<?>) namingDef;

        if (namingClass == EnumNamingStrategy.class) {
            return null;
        }
        if (!EnumNamingStrategy.class.isAssignableFrom(namingClass)) {
            throw new IllegalArgumentException(String.format(
                "Problem with AnnotationIntrospector returned Class %s; " +
                    "expected `Class<EnumNamingStrategy>`", ClassUtil.classNameOf(namingClass)));
        }
        return (EnumNamingStrategy) ClassUtil.createInstance(namingClass, canOverrideAccessModifiers);
    }
}
