package tools.jackson.databind;

import tools.jackson.databind.introspect.AnnotatedClass;

/**
 * Helper class needed by tests to access state of {@link ObjectMapper} that is
 * only accessible from the same package
 *
 * @since 3.0
 */
public abstract class ObjectMapperTestAccess
{
    public static BeanDescription beanDescriptionForDeser(ObjectMapper mapper, Class<?> type) {
        return mapper._deserializationContext().introspectBeanDescription(mapper.constructType(type));
    }

    public static BeanDescription beanDescriptionForSer(ObjectMapper mapper, Class<?> type) {
        return mapper._serializationContext().introspectBeanDescription(mapper.constructType(type));
    }

    public static AnnotatedClass annotatedClassForDeser(ObjectMapper mapper, Class<?> type) {
        return mapper._deserializationContext().introspectClassAnnotations(type);
    }

    public static AnnotatedClass annotatedClassForSer(ObjectMapper mapper, Class<?> type) {
        return mapper._serializationContext().introspectClassAnnotations(type);
    }
}
