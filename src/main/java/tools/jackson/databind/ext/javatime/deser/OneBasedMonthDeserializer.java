package tools.jackson.databind.ext.javatime.deser;

import java.time.Month;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;

import tools.jackson.databind.*;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.deser.std.DelegatingDeserializer;
import tools.jackson.databind.exc.InvalidFormatException;

public class OneBasedMonthDeserializer extends DelegatingDeserializer {
    public OneBasedMonthDeserializer(ValueDeserializer<?> defaultDeserializer) {
        super(defaultDeserializer);
    }

    @Override
    public Object deserialize(JsonParser parser, DeserializationContext context) {
        final boolean oneBased = context.isEnabled(DateTimeFeature.ONE_BASED_MONTHS);
        if (oneBased) {
            JsonToken token = parser.currentToken();
            switch (token) {
                case VALUE_NUMBER_INT:
                    return _decodeMonth(parser.getIntValue(), parser);
                case VALUE_STRING:
                    String monthSpec = parser.getString();
                    int oneBasedMonthNumber = _decodeNumber(monthSpec);
                    if (oneBasedMonthNumber >= 0) {
                        return _decodeMonth(oneBasedMonthNumber, parser);
                    }
                default:
                    // Otherwise fall through to default handling
                    break;
            }
            // fall-through
        }
        return getDelegatee().deserialize(parser, context);
    }

    /**
     * @return Numeric value of input text that represents a 1-digit or 2-digit number.
     *         Negative value in other cases (empty string, not a number, 3 or more digits).
     */
    private int _decodeNumber(String text) {
        int numValue;
        switch (text.length()) {
            case 1:
                char c = text.charAt(0);
                boolean cValid = ('0' <= c && c <= '9');
                numValue = cValid ? (c - '0') : -1;
                break;
            case 2:
                char c1 = text.charAt(0);
                char c2 = text.charAt(1);
                boolean c12valid = ('0' <= c1 && c1 <= '9' && '0' <= c2 && c2 <= '9');
                numValue = c12valid ? (10 * (c1 - '0') + (c2 - '0')) : -1;
                break;
            default:
                numValue = -1;
        }
        return numValue;
    }

    private Month _decodeMonth(int oneBasedMonthNumber, JsonParser parser) throws InvalidFormatException {
        if (Month.JANUARY.getValue() <= oneBasedMonthNumber && oneBasedMonthNumber <= Month.DECEMBER.getValue()) {
            return Month.of(oneBasedMonthNumber);
        }
        throw new InvalidFormatException(parser, "Month number " + oneBasedMonthNumber + " not allowed for 1-based Month.", oneBasedMonthNumber, Integer.class);
    }

    @Override
    protected ValueDeserializer<?> newDelegatingInstance(ValueDeserializer<?> newDelegatee) {
        return new OneBasedMonthDeserializer(newDelegatee);
    }
}
