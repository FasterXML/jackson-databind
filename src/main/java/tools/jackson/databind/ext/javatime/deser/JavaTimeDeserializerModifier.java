package tools.jackson.databind.ext.javatime.deser;

import java.time.Month;

import tools.jackson.databind.*;
import tools.jackson.databind.deser.ValueDeserializerModifier;

/* 08-Apr-2025, tatu: we really should rewrite things to have "native"
 *   {@code MonthDeserializer} and not rely on {@code EnumDeserializer}.
 */
public class JavaTimeDeserializerModifier extends ValueDeserializerModifier
{
    private static final long serialVersionUID = 1L;

    public JavaTimeDeserializerModifier() { }

    @Override
    public ValueDeserializer<?> modifyEnumDeserializer(DeserializationConfig config, JavaType type,
            BeanDescription.Supplier beanDescRef, ValueDeserializer<?> defaultDeserializer) {
        if (type.hasRawClass(Month.class)) {
            return new OneBasedMonthDeserializer(defaultDeserializer);
        }
        return defaultDeserializer;
    }
}
