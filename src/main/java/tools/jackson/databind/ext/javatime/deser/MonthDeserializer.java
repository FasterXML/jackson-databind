package tools.jackson.databind.ext.javatime.deser;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import tools.jackson.core.*;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.cfg.DateTimeFeature;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Deserializer for Java 8 temporal {@link Month}s.
 */
public class MonthDeserializer extends JSR310DateTimeDeserializerBase<Month>
{
    public static final MonthDeserializer INSTANCE = new MonthDeserializer();

    // @since 3.1
    private final Map<String, Month> _byNameLookup = Arrays.stream(Month.values())
            .collect(Collectors.toUnmodifiableMap(Month::name, Function.identity()));

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
            Boolean leniency, DateTimeFormatter formatter, JsonFormat.Shape shape) {
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
    public Month deserialize(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        if (p.hasToken(JsonToken.VALUE_STRING)) {
            return _fromString(p, ctxt, p.getString());
        }
        // Support numeric scalar input
        if (p.hasToken(JsonToken.VALUE_NUMBER_INT)) {
            final int monthIndex = p.getIntValue();
            if (ctxt.isEnabled(DateTimeFeature.ONE_BASED_MONTHS)) {
                return _decode1BasedMonth(monthIndex, ctxt);
            }
            return _decode0BasedMonth(monthIndex, ctxt);
        }
        // 30-Sep-2020, tatu: New! "Scalar from Object" (mostly for XML)
        if (p.isExpectedStartObjectToken()) {
            final String str = ctxt.extractScalarFromObject(p, this, handledType());
            // 17-May-2025, tatu: [databind#4656] need to check for `null`
            if (str != null) {
                return _fromString(p, ctxt, str);
            }
            // fall through
        } else if (p.isExpectedStartArrayToken()) {
            // [databind#5957]: Delegate to standard array handling so empty arrays
            // and single-element unwrapping respect coercion / UNWRAP_SINGLE_VALUE_ARRAYS.
            return _deserializeFromArray(p, ctxt);
        } else if (p.hasToken(JsonToken.VALUE_EMBEDDED_OBJECT)) {
            return (Month) p.getEmbeddedObject();
        }
        return _handleUnexpectedToken(ctxt, p,
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
                    int monthIndex = Integer.parseInt(string);
                    if (ctxt.isEnabled(DateTimeFeature.ONE_BASED_MONTHS)) {
                        return _decode1BasedMonth(monthIndex, ctxt);
                    }
                    return _decode0BasedMonth(monthIndex, ctxt);
                } catch (NumberFormatException nfe) {
                    // fall through – treat as textual month name
                }
                // Second: try textual input
                // Handle English month names such as "JANUARY" from the actual Month Enum names
                Month m = _byNameLookup.get(string);
                if (m != null) {
                    return m;
                }
                return (Month) ctxt.handleWeirdStringValue(handledType(), string, 
                        "not one of known `Month` values: %s",
                                Arrays.toString(Month.values()));
            }
            return Month.from(_formatter.parse(string));
        } catch (DateTimeException e) {
            return _handleDateTimeFormatException(ctxt, e, _formatter, string);
        } catch (NumberFormatException e) {
            throw ctxt.weirdStringException(string, handledType(),
                    "not a valid Month value");
        }
    }

    /**
     * Validate and convert a 1‑based month number to {@link Month}.
     */
    private Month _decode1BasedMonth(int monthIndex, DeserializationContext ctxt)
        throws JacksonException
    {
        if (Month.JANUARY.getValue() <= monthIndex && monthIndex <= Month.DECEMBER.getValue()) {
            return Month.of(monthIndex);
        }
        return (Month) ctxt.handleWeirdNumberValue(handledType(),
                monthIndex, "month number outside 1-12 range for 1-based `Month`s");
    }

    /**
     * Validate and convert a 0‑based month number to {@link Month}.
     */
    private Month _decode0BasedMonth(int monthIndex, DeserializationContext ctxt)
        throws JacksonException
    {
        if (monthIndex < 0 || monthIndex >= 12) { // invalid for 0‑based
            return (Month) ctxt.handleWeirdNumberValue(handledType(),
                    monthIndex, "month number outside 0-11 range for 0-based `Month`s");
        }
        return Month.values()[monthIndex]; // 0‑based mapping
    }
}
