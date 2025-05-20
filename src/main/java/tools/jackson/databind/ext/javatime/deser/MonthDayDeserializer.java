package tools.jackson.databind.ext.javatime.deser;

import java.time.DateTimeException;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonFormat;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;

/**
 * Deserializer for Java 8 temporal {@link MonthDay}s.
 */
public class MonthDayDeserializer extends JSR310DateTimeDeserializerBase<MonthDay>
{
    public static final MonthDayDeserializer INSTANCE = new MonthDayDeserializer();

    /**
     * NOTE: only {@code public} so that use via annotations (see [modules-java8#202])
     * is possible
     */
    public MonthDayDeserializer() {
        this(null);
    }

    public MonthDayDeserializer(DateTimeFormatter formatter) {
        super(MonthDay.class, formatter);
    }

    /**
     * Since 2.12
     */
    protected MonthDayDeserializer(MonthDayDeserializer base, Boolean leniency) {
        super(base, leniency);
    }

    /**
     * Since 2.16
     */
    protected MonthDayDeserializer(MonthDayDeserializer base,
            Boolean leniency,
            DateTimeFormatter formatter,
            JsonFormat.Shape shape) {
        super(base, leniency, formatter, shape);
    }

    @Override
    protected MonthDayDeserializer withLeniency(Boolean leniency) {
        return new MonthDayDeserializer(this, leniency);
    }

    @Override
    protected MonthDayDeserializer withDateFormat(DateTimeFormatter dtf) {
        return new MonthDayDeserializer(this, _isLenient, dtf, _shape);
    }

    @Override
    public MonthDay deserialize(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        if (p.hasToken(JsonToken.VALUE_STRING)) {
            return _fromString(p, ctxt, p.getString());
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
            JsonToken t = p.nextToken();
            if (t == JsonToken.END_ARRAY) {
                return null;
            }
            if ((t == JsonToken.VALUE_STRING || t == JsonToken.VALUE_EMBEDDED_OBJECT)
                    && ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
                final MonthDay parsed = deserialize(p, ctxt);
                if (p.nextToken() != JsonToken.END_ARRAY) {
                    handleMissingEndArrayForSingle(p, ctxt);
                }
                return parsed;
            }
            if (t != JsonToken.VALUE_NUMBER_INT) {
                _reportWrongToken(ctxt, JsonToken.VALUE_NUMBER_INT, "month");
            }
            int month = p.getIntValue();
            int day = p.nextIntValue(-1);
            if (day == -1) {
                if (!p.hasToken(JsonToken.VALUE_NUMBER_INT)) {
                    _reportWrongToken(ctxt, JsonToken.VALUE_NUMBER_INT, "day");
                }
                day = p.getIntValue();
            }
            if (p.nextToken() != JsonToken.END_ARRAY) {
                throw ctxt.wrongTokenException(p, handledType(), JsonToken.END_ARRAY,
                        "Expected array to end");
            }
            return MonthDay.of(month, day);
        } else if (p.hasToken(JsonToken.VALUE_EMBEDDED_OBJECT)) {
            return (MonthDay) p.getEmbeddedObject();
        }
        return _handleUnexpectedToken(ctxt, p,
                JsonToken.VALUE_STRING, JsonToken.START_ARRAY);
    }

    protected MonthDay _fromString(JsonParser p, DeserializationContext ctxt,
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
                return MonthDay.parse(string);
            }
            return MonthDay.parse(string, _formatter);
        } catch (DateTimeException e) {
            return _handleDateTimeFormatException(ctxt, e, _formatter, string);
        }
    }
}
