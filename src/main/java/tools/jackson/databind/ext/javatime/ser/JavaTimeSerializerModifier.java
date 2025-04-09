package tools.jackson.databind.ext.javatime.ser;

import java.time.Month;

import tools.jackson.databind.*;
import tools.jackson.databind.ser.ValueSerializerModifier;

public class JavaTimeSerializerModifier extends ValueSerializerModifier {
    private static final long serialVersionUID = 1L;

    public JavaTimeSerializerModifier() { }

    @Override
    public ValueSerializer<?> modifyEnumSerializer(SerializationConfig config, JavaType valueType,
            BeanDescription beanDesc, ValueSerializer<?> serializer) {
        if (valueType.hasRawClass(Month.class)) {
            return new OneBasedMonthSerializer(serializer);
        }
        return serializer;
    }
}
