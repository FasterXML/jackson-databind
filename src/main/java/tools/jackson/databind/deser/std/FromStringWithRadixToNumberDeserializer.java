package tools.jackson.databind.deser.std;

import java.math.BigInteger;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.util.ClassUtil;

/**
 * Deserializer used for a string that represents a number in specific radix (base).
 *
 * @since 3.1
 */
public class FromStringWithRadixToNumberDeserializer
    extends StdScalarDeserializer<Number>
{
    private final int radix;

    public FromStringWithRadixToNumberDeserializer(StdScalarDeserializer<?> src, int radix) {
        super(src);
        this.radix = radix;
    }

    @Override
    public Number deserialize(JsonParser p, DeserializationContext ctxt) {
        Class<?> handledType = handledType();

        // Should we allow (Integer) numbers? At least with "lenient" format settings?
        if (!p.hasToken(JsonToken.VALUE_STRING)) {
            ctxt.reportInputMismatch(handledType,
                    "Need String when deserializing a value using `FromStringWithRadixToNumberDeserializer` (radix: %d)",
                    radix);
        }

        String text = p.getString();

        // First, DoS check
        p.streamReadConstraints().validateIntegerLength(text.length());

        try {
            if (handledType.equals(BigInteger.class)) {
                return new BigInteger(text, radix);
            }
            // Map from wrappers to primitive
            Class<?> primitiveType = ClassUtil.primitiveType(handledType);

            // start with more likely types
            if (primitiveType == long.class) {
                return Long.parseLong(text, radix);
            }
            if (primitiveType == int.class) {
                return Integer.parseInt(text, radix);
            }
            if (primitiveType == short.class) {
                return Short.parseShort(text, radix);
            }
            if (primitiveType == byte.class) {
                return Byte.parseByte(text, radix);
            }
        } catch (IllegalArgumentException iae) {
            return (Number) ctxt.handleWeirdStringValue(handledType, text,
                    "not a valid representation of %s value with radix %d",
                    ClassUtil.nameOf(handledType), radix);
        }
        // Is this really true?
        return ctxt.reportInputMismatch(handledType,
                "Trying to deserialize a non-whole number with `NumberToStringWithRadixSerializer`");
    }
}
