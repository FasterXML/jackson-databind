package tools.jackson.databind.util;

import java.util.Arrays;
import java.util.List;

import tools.jackson.databind.AnnotationIntrospector;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.AnnotatedClass;

/**
 * Encapsulation of a {@link java.lang.Enum} type definition with its elements
 * and explicitly annotated names for elements.
 *
 * @since 3.0.3
 */
public class EnumDefinition
{
    private final MapperConfig<?> _config;
    private final Class<Enum<?>> _enumClass;
    private final Enum<?>[] _enumConstants;
    private final String[] _explicitNames;

    private EnumDefinition(MapperConfig<?> config, Class<Enum<?>> enumClass,
            Enum<?>[] enumConstants,
            String[] explicitNames)
    {
        _config = config;
        _enumClass = enumClass;
        _enumConstants = enumConstants;
        _explicitNames = explicitNames;
    }

    public static EnumDefinition construct(MapperConfig<?> config,
            AnnotatedClass annotatedClass)
    {
        final Class<?> enumCls0 = annotatedClass.getRawType();
        final Enum<?>[] enumConstants = _enumConstants(enumCls0);
        String[] explicitNames = new String[enumConstants.length];

        final AnnotationIntrospector ai = config.getAnnotationIntrospector();
        if (ai != null) {
            explicitNames = ai.findEnumValues(config, annotatedClass, 
                    enumConstants, explicitNames);
        }
        return new EnumDefinition(config, _enumClass(enumCls0), enumConstants,
                explicitNames);
        
    }

    public int size() {
        return _enumConstants.length;
    }

    public Class<Enum<?>> enumClass() {
        return _enumClass;
    }

    public Enum<?>[] enumConstants() {
        return _enumConstants;
    }

    public List<String> explicitNames() {
        return Arrays.asList(_explicitNames);
    }

    @SuppressWarnings("unchecked")
    private static Class<Enum<?>> _enumClass(Class<?> enumCls0) {
        return (Class<Enum<?>>) enumCls0;
    }

    private static Enum<?>[] _enumConstants(Class<?> enumCls) {
        final Enum<?>[] enumValues = ClassUtil.findEnumType(enumCls).getEnumConstants();
        if (enumValues == null) {
            throw new IllegalArgumentException("Internal error: no Enum constants for Class "+enumCls.getName());
        }
        return enumValues;
    }
}
