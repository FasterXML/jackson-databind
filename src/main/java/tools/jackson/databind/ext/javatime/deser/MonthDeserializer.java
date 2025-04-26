package tools.jackson.databind.ext.javatime.deser;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import tools.jackson.core.*;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.exc.InvalidFormatException;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Deserializer for Java 8 temporal {@link Month}s.
 */
public class MonthDeserializer extends JSR310DateTimeDeserializerBase<Month>
{
    public static final MonthDeserializer INSTANCE = new MonthDeserializer();

    private final Set<String> possibleMonthStringValues = Arrays.stream(Month.values()).map(Month::name).collect(Collectors.toSet());

    /**
     * NOTE: only {@code public} so that use via annotations (see [modules-java8#202])
     * is possible
     */
    public MonthDeserializer() {
        this(null);
    }

    public MonthDeserializer(DateTimeFormatter formatter) {
        super(Month.class, formatter);
    }

    protected MonthDeserializer(MonthDeserializer base, Boolean leniency) {
        super(base, leniency);
    }

    protected MonthDeserializer(MonthDeserializer base,
                                   Boolean leniency,
                                   DateTimeFormatter formatter,
                                   JsonFormat.Shape shape) {
        super(base, leniency, formatter, shape);
    }

    @Override
    protected MonthDeserializer withLeniency(Boolean leniency) {
        return new MonthDeserializer(this, leniency);
    }

    @Override
    protected MonthDeserializer withDateFormat(DateTimeFormatter dtf) {
        return new MonthDeserializer(this, _isLenient, dtf, _shape);
    }

    @Override
    public Month deserialize(JsonParser parser, DeserializationContext context)
            throws JacksonException
    {
        if (parser.hasToken(JsonToken.VALUE_STRING)) {
            return _fromString(parser, context, parser.getString());
        }
        // Support numeric scalar input
        if (parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
            final int raw = parser.getIntValue();
            if (context.isEnabled(DateTimeFeature.ONE_BASED_MONTHS)) {
                return _decodeMonth(raw, context);
            }
            // default: 0‑based index (0 == JANUARY)
            if (raw < 0 || raw >= 12) {
                context.handleWeirdNumberValue(handledType(),
                        raw, "Month index (%s) outside 0-11 range", raw);
                return null; // never gets here, but compiler doesn't know
            }
            return Month.values()[raw];
        }
        // 30-Sep-2020, tatu: New! "Scalar from Object" (mostly for XML)
        if (parser.isExpectedStartObjectToken()) {
            return _fromString(parser, context,
                    context.extractScalarFromObject(parser, this, handledType()));
        }
        if (parser.isExpectedStartArrayToken()) {
            JsonToken t = parser.nextToken();
            if (t == JsonToken.END_ARRAY) {
                return null;
            }
            if ((t == JsonToken.VALUE_STRING || t == JsonToken.VALUE_EMBEDDED_OBJECT)
                    && context.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
                final Month parsed = deserialize(parser, context);
                if (parser.nextToken() != JsonToken.END_ARRAY) {
                    handleMissingEndArrayForSingle(parser, context);
                }
                return parsed;
            }
            if (t != JsonToken.VALUE_NUMBER_INT) {
                _reportWrongToken(context, JsonToken.VALUE_NUMBER_INT, Integer.class.getName());
            }
            int month = parser.getIntValue();
            if (parser.nextToken() != JsonToken.END_ARRAY) {
                throw context.wrongTokenException(parser, handledType(), JsonToken.END_ARRAY,
                        "Expected array to end");
            }
            return Month.of(month);
        }
        if (parser.hasToken(JsonToken.VALUE_EMBEDDED_OBJECT)) {
            return (Month) parser.getEmbeddedObject();
        }
        return _handleUnexpectedToken(context, parser,
                JsonToken.VALUE_STRING, JsonToken.START_ARRAY);
    }

    protected Month _fromString(JsonParser p, DeserializationContext ctxt,
                                   String string0)
            throws JacksonException
    {
        String string = string0.trim();
        if (string.length() == 0) {
            // 22-Oct-2020, tatu: not sure if we should pass original (to distinguish
            //   b/w empty and blank); for now don't which will allow blanks to be
            //   handled like "regular" empty (same as pre-2.12)
            return _fromEmptyString(p, ctxt, string);
        }
        try {
            if (_formatter == null) {
                // First: try purely numeric input
                try {
                    int oneBasedMonthNumber = Integer.parseInt(string);
                    if (ctxt.isEnabled(DateTimeFeature.ONE_BASED_MONTHS)) {
                        return _decodeMonth(oneBasedMonthNumber, ctxt);
                    }
                    if (oneBasedMonthNumber < 0 || oneBasedMonthNumber >= 12) { // invalid for 0‑based
                        throw new InvalidFormatException(p, "Month number " + oneBasedMonthNumber + " not allowed for 1-based Month.", oneBasedMonthNumber, Integer.class);
                    }
                    return Month.values()[oneBasedMonthNumber]; // 0‑based mapping
                } catch (NumberFormatException nfe) {
                    // fall through – treat as textual month name
                }
                // Second: try textual input
                // Handle English month names such as "JANUARY" from the actual Month Enum names
                if (possibleMonthStringValues.contains(string)) {
                    return Month.valueOf(string);
                } else {
                    throw new InvalidFormatException(p, String.format("Cannot deserialize value of type `java.time.Month` from String \"%s\": not one of the values accepted for Enum class: %s", string, Arrays.toString(Month.values())), string, Month.class);
                }
            }
            return Month.from(_formatter.parse(string));
        } catch (DateTimeException e) {
            return _handleDateTimeFormatException(ctxt, e, _formatter, string);
        } catch (NumberFormatException e) {
            throw ctxt.weirdStringException(string, handledType(),
                    "not a valid month value");
        }
    }

    /**
     * Validate and convert a 1‑based month number to {@link Month}.
     */
    private Month _decodeMonth(int oneBasedMonthNumber, DeserializationContext ctxt)
            throws JacksonException
    {
        if (Month.JANUARY.getValue() <= oneBasedMonthNumber && oneBasedMonthNumber <= Month.DECEMBER.getValue()) {
            return Month.of(oneBasedMonthNumber);
        }
        // If out of range, throw an exception
        ctxt.handleWeirdNumberValue(handledType(),
                oneBasedMonthNumber, "Month number %s not allowed for 1-based Month.", oneBasedMonthNumber);
        return null; // never gets here, but compiler doesn't know
    }
}
