package tools.jackson.databind.ext.javatime.deser;

import java.time.*;
import java.time.format.DateTimeFormatter;

import tools.jackson.core.*;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Deserializer for Java 8 temporal {@link Month}s.
 */
public class MonthDeserializer extends JSR310DateTimeDeserializerBase<Month>
{
    public static final MonthDeserializer INSTANCE = new MonthDeserializer();

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
                _reportWrongToken(context, JsonToken.VALUE_NUMBER_INT, "month");
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
                return Month.values()[Integer.parseInt(string)];
            }
            return Month.from(_formatter.parse(string));
        } catch (DateTimeException e) {
            return _handleDateTimeFormatException(ctxt, e, _formatter, string);
        } catch (NumberFormatException e) {
            throw ctxt.weirdStringException(string, handledType(),
                    "not a valid month value");
        }
    }
}
