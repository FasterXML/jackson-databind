package tools.jackson.databind.deser.jdk;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

import com.fasterxml.jackson.annotation.JsonFormat;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.deser.std.StdScalarDeserializer;
import tools.jackson.databind.type.LogicalType;
import tools.jackson.databind.util.StdDateFormat;

public abstract class DateBasedDeserializer<T>
    extends StdScalarDeserializer<T>
{
    /**
     * Specific format to use, if non-null; if null will
     * just use default format.
     */
    protected final DateFormat _customFormat;

    /**
     * Let's also keep format String for reference, to use for error messages
     */
    protected final String _formatString;

    protected DateBasedDeserializer(Class<?> clz) {
        super(clz);
        _customFormat = null;
        _formatString = null;
    }

    protected DateBasedDeserializer(DateBasedDeserializer<T> base,
            DateFormat format, String formatStr) {
        super(base._valueClass);
        _customFormat = format;
        _formatString = formatStr;
    }

    protected abstract DateBasedDeserializer<T> withDateFormat(DateFormat df, String formatStr);

    @Override // since 2.12
    public LogicalType logicalType() {
        return LogicalType.DateTime;
    }

    @Override
    public ValueDeserializer<?> createContextual(DeserializationContext ctxt,
            BeanProperty property)
    {
        final JsonFormat.Value format = findFormatOverrides(ctxt, property,
                handledType());

        if (format != null) {
            TimeZone tz = format.getTimeZone();
            final Boolean lenient = format.getLenient();

            // First: fully custom pattern?
            if (format.hasPattern()) {
                final String pattern = format.getPattern();
                final Locale loc = format.hasLocale() ? format.getLocale() : ctxt.getLocale();
                SimpleDateFormat df = new SimpleDateFormat(pattern, loc);
                if (tz == null) {
                    tz = ctxt.getTimeZone();
                }
                df.setTimeZone(tz);
                if (lenient != null) {
                    df.setLenient(lenient);
                }
                return withDateFormat(df, pattern);
            }
            // But if not, can still override timezone
            if (tz != null) {
                DateFormat df = ctxt.getConfig().getDateFormat();
                // one shortcut: with our custom format, can simplify handling a bit
                if (df.getClass() == StdDateFormat.class) {
                    final Locale loc = format.hasLocale() ? format.getLocale() : ctxt.getLocale();
                    StdDateFormat std = (StdDateFormat) df;
                    std = std.withTimeZone(tz);
                    std = std.withLocale(loc);
                    if (lenient != null) {
                        std = std.withLenient(lenient);
                    }
                    df = std;
                } else {
                    // otherwise need to clone, re-set timezone:
                    df = (DateFormat) df.clone();
                    df.setTimeZone(tz);
                    if (lenient != null) {
                        df.setLenient(lenient);
                    }
                }
                return withDateFormat(df, _formatString);
            }
            // or maybe even just leniency?
            if (lenient != null) {
                DateFormat df = ctxt.getConfig().getDateFormat();
                String pattern = _formatString;
                // one shortcut: with our custom format, can simplify handling a bit
                if (df.getClass() == StdDateFormat.class) {
                    StdDateFormat std = (StdDateFormat) df;
                    std = std.withLenient(lenient);
                    df = std;
                    pattern = std.toPattern();
                } else {
                    // otherwise need to clone,
                    df = (DateFormat) df.clone();
                    df.setLenient(lenient);
                    if (df instanceof SimpleDateFormat) {
                        ((SimpleDateFormat) df).toPattern();
                    }
                }
                if (pattern == null) {
                    pattern = "[unknown]";
                }
                return withDateFormat(df, pattern);
            }
        }
        return this;
    }

    @Override
    protected java.util.Date _parseDate(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        if (_customFormat != null) {
            if (p.hasToken(JsonToken.VALUE_STRING)) {
                String str = p.getText().trim();
                if (str.isEmpty()) {
                    final CoercionAction act = _checkFromStringCoercion(ctxt, str);
                    switch (act) { // note: Fail handled above
                    case AsEmpty:
                        return new java.util.Date(0L);
                    case AsNull:
                    case TryConvert:
                    default:
                    }
                    return null;
                }
                synchronized (_customFormat) {
                    try {
                        return _customFormat.parse(str);
                    } catch (ParseException e) {
                        return (java.util.Date) ctxt.handleWeirdStringValue(handledType(), str,
                                "expected format \"%s\"", _formatString);
                    }
                }
            }
        }
        return super._parseDate(p, ctxt);
    }
}
