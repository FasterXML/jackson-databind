package tools.jackson.databind;

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
        return mapper._serializerProvider().introspectBeanDescription(mapper.constructType(type));
    }
}
