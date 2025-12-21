package tools.jackson.databind.deser.jdk;

import com.fasterxml.jackson.annotation.JsonFormat;

import tools.jackson.databind.deser.std.FromStringWithRadixToNumberDeserializer;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.deser.std.StdScalarDeserializer;

/**
 * Factory class for {@link FromStringWithRadixToNumberDeserializer} for deserializers in {@link tools.jackson.databind.deser.jdk.NumberDeserializers}
 *
 * @since 3.1
 */
public class RadixDeserializerFactory
{
    public static StdDeserializer<? extends Number> createRadixStringDeserializer(
            StdScalarDeserializer<? extends  Number> initialDeser,
            JsonFormat.Value formatOverrides)
    {
        if (formatOverrides != null && formatOverrides.getShape() == JsonFormat.Shape.STRING) {
            if (isSerializeWithRadixOverride(formatOverrides)) {
                int radix = formatOverrides.getRadix();
                return new FromStringWithRadixToNumberDeserializer(initialDeser, radix);
            }
        }
        return initialDeser;
    }

    /**
     * Check if we have a proper {@link JsonFormat} annotation for serializing a number
     * using an alternative radix specified in the annotation.
     */
    private static boolean isSerializeWithRadixOverride(JsonFormat.Value format) {
        return format.hasNonDefaultRadix();
    }
}
