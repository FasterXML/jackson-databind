package tools.jackson.databind.datetime.ser;

import java.time.Month;

import tools.jackson.core.JsonGenerator;

import tools.jackson.databind.*;

/**
 * @since 2.17
 */
public class OneBasedMonthSerializer extends ValueSerializer<Month> {
    private final ValueSerializer<Object> _defaultSerializer;

    @SuppressWarnings("unchecked")
    public OneBasedMonthSerializer(ValueSerializer<?> defaultSerializer) 
    {
        _defaultSerializer = (ValueSerializer<Object>) defaultSerializer;
    }

    @Override
    public void serialize(Month value, JsonGenerator gen, SerializationContext ctxt)
    {
        // 15-Jan-2024, tatu: [modules-java8#274] This is not really sufficient
        //   (see `jackson-databind` `EnumSerializer` for full logic), but has to
        //   do for now. May need to add `@JsonFormat.shape` handling in future.
        if (ctxt.isEnabled(SerializationFeature.WRITE_ENUMS_USING_INDEX)) {
            gen.writeNumber(value.ordinal() + 1);
            return;
        }
        _defaultSerializer.serialize(value, gen, ctxt);
    }
}
