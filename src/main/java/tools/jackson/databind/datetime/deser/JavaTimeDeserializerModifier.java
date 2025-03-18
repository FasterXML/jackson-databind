package tools.jackson.databind.datetime.deser;

import java.time.Month;

import tools.jackson.databind.*;
import tools.jackson.databind.deser.ValueDeserializerModifier;

/**
 * @since 2.17
 */
public class JavaTimeDeserializerModifier extends ValueDeserializerModifier {
    private static final long serialVersionUID = 1L;

    private final boolean _oneBaseMonths;

    public JavaTimeDeserializerModifier(boolean oneBaseMonths) {
        _oneBaseMonths = oneBaseMonths;
    }

    @Override
    public ValueDeserializer<?> modifyEnumDeserializer(DeserializationConfig config, JavaType type,
            BeanDescription beanDesc, ValueDeserializer<?> defaultDeserializer) {
        if (_oneBaseMonths && type.hasRawClass(Month.class)) {
            return new OneBasedMonthDeserializer(defaultDeserializer);
        }
        return defaultDeserializer;
    }
}
