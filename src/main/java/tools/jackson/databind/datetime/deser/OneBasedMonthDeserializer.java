package tools.jackson.databind.datetime.deser;

import java.time.Month;
import java.util.regex.Pattern;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;

import tools.jackson.databind.*;
import tools.jackson.databind.deser.std.DelegatingDeserializer;
import tools.jackson.databind.exc.InvalidFormatException;

/**
 * @since 2.17
 */
public class OneBasedMonthDeserializer extends DelegatingDeserializer {
    private static final Pattern HAS_ONE_OR_TWO_DIGITS = Pattern.compile("^\\d{1,2}$");

    public OneBasedMonthDeserializer(ValueDeserializer<?> defaultDeserializer) {
        super(defaultDeserializer);
    }

    @Override
    public Object deserialize(JsonParser parser, DeserializationContext context) {
        JsonToken token = parser.currentToken();
        Month zeroBaseMonth = (Month) getDelegatee().deserialize(parser, context);
        if (!_isNumericValue(parser.getString(), token)) {
            return zeroBaseMonth;
        }
        if (zeroBaseMonth == Month.JANUARY) {
            throw new InvalidFormatException(parser, "Month.JANUARY value not allowed for 1-based Month.", zeroBaseMonth, Month.class);
        }
        return zeroBaseMonth.minus(1);
    }

    private boolean _isNumericValue(String text, JsonToken token) {
        return token == JsonToken.VALUE_NUMBER_INT || _isNumberAsString(text, token);
    }

    private boolean _isNumberAsString(String text, JsonToken token) {
        return token == JsonToken.VALUE_STRING && HAS_ONE_OR_TWO_DIGITS.matcher(text).matches();
    }

    @Override
    protected ValueDeserializer<?> newDelegatingInstance(ValueDeserializer<?> newDelegatee) {
        return new OneBasedMonthDeserializer(newDelegatee);
    }
}
