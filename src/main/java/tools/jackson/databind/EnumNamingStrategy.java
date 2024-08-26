package tools.jackson.databind;

import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.AnnotatedClass;
import tools.jackson.databind.introspect.AnnotatedMethod;

/**
 * Defines how the string representation of an enum is converted into an external property name for mapping
 * during deserialization.
 */
public interface EnumNamingStrategy {

    /**
     * Translates the given <code>enumName</code> into an external property name according to
     * the implementation of this {@link EnumNamingStrategy}.
     *
     * @param enumName the name of the enum value to translate
     * @param config the mapper configuration
     * @param cls the annotated class
     *
     * @return the external property name that corresponds to the given <code>enumName</code>
     * according to the implementation of this {@link EnumNamingStrategy}.
     */
    public String convertEnumToExternalName(MapperConfig<?> config, AnnotatedClass cls,
                String enumName);

}
