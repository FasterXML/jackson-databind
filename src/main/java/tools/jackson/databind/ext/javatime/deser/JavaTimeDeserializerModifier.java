package tools.jackson.databind.ext.javatime.deser;

import tools.jackson.databind.*;
import tools.jackson.databind.deser.ValueDeserializerModifier;

public class JavaTimeDeserializerModifier extends ValueDeserializerModifier
{
    private static final long serialVersionUID = 1L;

    public JavaTimeDeserializerModifier() { }

    @Override
    public ValueDeserializer<?> modifyEnumDeserializer(DeserializationConfig config, JavaType type,
            BeanDescription.Supplier beanDescRef, ValueDeserializer<?> defaultDeserializer) {

        return defaultDeserializer;
    }
}
