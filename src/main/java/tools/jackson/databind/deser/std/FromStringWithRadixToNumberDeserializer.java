package tools.jackson.databind.deser.std;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;

import java.math.BigInteger;

/**
 * Deserializer used for a string that represents a number in specific radix (base).
 *
 * @since 3.1
 */
public class FromStringWithRadixToNumberDeserializer
        extends StdDeserializer<Number>
{
    private final int radix;

    public FromStringWithRadixToNumberDeserializer(StdDeserializer<?> src, int radix) {
        super(src);
        this.radix = radix;
    }

    @Override
    public Number deserialize(JsonParser p, DeserializationContext ctxt) {
        Class<?> handledType = handledType();

        if (p.currentToken() != JsonToken.VALUE_STRING) {
            ctxt.reportInputMismatch(handledType,
                    "Read something other than string when deserializing a value using FromStringWithRadixToNumberDeserializer");
        }

        String text = p.getString();

        if (handledType.equals(BigInteger.class)) {
            return new BigInteger(text, radix);
        } else if (handledType.equals(byte.class) || handledType.equals(Byte.class)) {
            return Byte.parseByte(text, radix);
        } else if (handledType.equals(short.class) || handledType.equals(Short.class)) {
            return Short.parseShort(text, radix);
        } else if (handledType.equals(int.class) || handledType.equals(Integer.class)) {
            return Integer.parseInt(text, radix);
        } else if (handledType.equals(long.class) || handledType.equals(Long.class)) {
            return Long.parseLong(text, radix);
        }
        return ctxt.reportInputMismatch(handledType,
                    "Trying to deserialize a non-whole number with NumberToStringWithRadixSerializer");
    }
}
