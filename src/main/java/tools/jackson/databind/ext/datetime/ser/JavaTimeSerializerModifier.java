package tools.jackson.databind.ext.datetime.ser;

import java.time.Month;

import tools.jackson.databind.*;
import tools.jackson.databind.ser.ValueSerializerModifier;

/**
 * @since 2.17
 */
public class JavaTimeSerializerModifier extends ValueSerializerModifier {
    private static final long serialVersionUID = 1L;

    private final boolean _oneBaseMonths;

    public JavaTimeSerializerModifier(boolean oneBaseMonths) {
        _oneBaseMonths = oneBaseMonths;
    }

    @Override
    public ValueSerializer<?> modifyEnumSerializer(SerializationConfig config, JavaType valueType,
            BeanDescription beanDesc, ValueSerializer<?> serializer) {
        if (_oneBaseMonths && valueType.hasRawClass(Month.class)) {
            return new OneBasedMonthSerializer(serializer);
        }
        return serializer;
    }
}
