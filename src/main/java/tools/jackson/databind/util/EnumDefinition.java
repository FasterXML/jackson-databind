package tools.jackson.databind.util;

import java.util.Arrays;
import java.util.List;

import tools.jackson.databind.AnnotationIntrospector;
import tools.jackson.databind.EnumNamingStrategy;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.AnnotatedClass;
import tools.jackson.databind.introspect.EnumNamingStrategyFactory;

/**
 * Encapsulation of a {@link java.lang.Enum} type definition with its elements
 * and explicitly annotated names for elements.
 *
 * @since 3.0.3
 */
public class EnumDefinition
{
    private final AnnotatedClass _annotatedClass;
    private final EnumNamingStrategy _enumNamingStrategy;
    private final Enum<?>[] _enumConstants;
    private final String[] _explicitNames;

    private EnumDefinition(AnnotatedClass annotatedClass,
            EnumNamingStrategy enumNamingStrategy,
            Enum<?>[] enumConstants,
            String[] explicitNames)
    {
        _annotatedClass = annotatedClass;
        _enumNamingStrategy = enumNamingStrategy;
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
        Object namingDef = config.getAnnotationIntrospector().findEnumNamingStrategy(config, annotatedClass);
        EnumNamingStrategy enumNamingStrategy = EnumNamingStrategyFactory.createEnumNamingStrategyInstance(
            namingDef, config.canOverrideAccessModifiers(), config.getEnumNamingStrategy());
        
        return new EnumDefinition(annotatedClass, enumNamingStrategy,
                enumConstants, explicitNames);
    }

    public EnumValuesToWrite valuesToWrite(MapperConfig<?> config) {
        return EnumValuesToWrite.construct(config, _annotatedClass,
                _enumNamingStrategy,
                _enumConstants, _explicitNames);
    }
    
    public int size() {
        return _enumConstants.length;
    }

    @SuppressWarnings("unchecked")
    public Class<Enum<?>> enumClass() {
        Class<?> cls = _annotatedClass.getRawType();
        return (Class<Enum<?>>) cls;
    }

    public Enum<?>[] enumConstants() {
        return _enumConstants;
    }

    public List<String> explicitNames() {
        return Arrays.asList(_explicitNames);
    }

    private static Enum<?>[] _enumConstants(Class<?> enumCls) {
        final Enum<?>[] enumValues = ClassUtil.findEnumType(enumCls).getEnumConstants();
        if (enumValues == null) {
            throw new IllegalArgumentException("Internal error: no Enum constants for Class "+enumCls.getName());
        }
        return enumValues;
    }
}
