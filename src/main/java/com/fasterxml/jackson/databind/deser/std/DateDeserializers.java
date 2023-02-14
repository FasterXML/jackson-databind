package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.text.*;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.StdDateFormat;

/**
 * Container class for core JDK date/time type deserializers.
 */
@SuppressWarnings("serial")
public class DateDeserializers
{
    private final static HashSet<String> _utilClasses = new HashSet<String>();
    static {
        _utilClasses.add("java.util.Calendar");
        _utilClasses.add("java.util.GregorianCalendar");
        _utilClasses.add("java.util.Date");
    }

    public static JsonDeserializer<?> find(Class<?> rawType, String clsName)
    {
        if (_utilClasses.contains(clsName)) {
            // Start with the most common type
            if (rawType == java.util.Calendar.class) {
                return new CalendarDeserializer();
            }
            if (rawType == java.util.Date.class) {
                return DateDeserializer.instance;
            }
            if (rawType == java.util.GregorianCalendar.class) {
                return new CalendarDeserializer(GregorianCalendar.class);
            }
        }
        return null;
    }

    // @since 2.11
    public static boolean hasDeserializerFor(Class<?> rawType) {
        return _utilClasses.contains(rawType.getName());
    }

    /*
    /**********************************************************
    /* Intermediate class for Date-based ones
    /**********************************************************
     */

    protected abstract static class DateBasedDeserializer<T>
        extends StdScalarDeserializer<T>
        implements ContextualDeserializer
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
        public JsonDeserializer<?> createContextual(DeserializationContext ctxt,
                BeanProperty property)
           throws JsonMappingException
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
            throws IOException
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

    /*
    /**********************************************************
    /* Deserializer implementations for Date types
    /**********************************************************
     */

    @JacksonStdImpl
    public static class CalendarDeserializer extends DateBasedDeserializer<Calendar>
    {
        /**
         * We may know actual expected type; if so, it will be
         * used for instantiation.
         *
         * @since 2.9
         */
        protected final Constructor<Calendar> _defaultCtor;

        public CalendarDeserializer() {
            super(Calendar.class);
            _defaultCtor = null;
        }

        @SuppressWarnings("unchecked")
        public CalendarDeserializer(Class<? extends Calendar> cc) {
            super(cc);
            _defaultCtor = (Constructor<Calendar>) ClassUtil.findConstructor(cc, false);
        }

        public CalendarDeserializer(CalendarDeserializer src, DateFormat df, String formatString) {
            super(src, df, formatString);
            _defaultCtor = src._defaultCtor;
        }

        @Override
        protected CalendarDeserializer withDateFormat(DateFormat df, String formatString) {
            return new CalendarDeserializer(this, df, formatString);
        }

        @Override // since 2.12
        public Object getEmptyValue(DeserializationContext ctxt) {
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTimeInMillis(0L);
            return cal;
        }

        @Override
        public Calendar deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
        {
            Date d = _parseDate(p, ctxt);
            if (d == null) {
                return null;
            }
            if (_defaultCtor == null) {
                return ctxt.constructCalendar(d);
            }
            try {
                Calendar c = _defaultCtor.newInstance();
                c.setTimeInMillis(d.getTime());
                TimeZone tz = ctxt.getTimeZone();
                if (tz != null) {
                    c.setTimeZone(tz);
                }
                return c;
            } catch (Exception e) {
                return (Calendar) ctxt.handleInstantiationProblem(handledType(), d, e);
            }
        }
    }

    /**
     * Simple deserializer for handling {@link java.util.Date} values.
     *<p>
     * One way to customize Date formats accepted is to override method
     * {@link DeserializationContext#parseDate} that this basic
     * deserializer calls.
     */
    @JacksonStdImpl
    public static class DateDeserializer extends DateBasedDeserializer<Date>
    {
        public final static DateDeserializer instance = new DateDeserializer();

        public DateDeserializer() { super(Date.class); }
        public DateDeserializer(DateDeserializer base, DateFormat df, String formatString) {
            super(base, df, formatString);
        }

        @Override
        protected DateDeserializer withDateFormat(DateFormat df, String formatString) {
            return new DateDeserializer(this, df, formatString);
        }

        @Override // since 2.12
        public Object getEmptyValue(DeserializationContext ctxt) {
            return new Date(0L);
        }

        @Override
        public java.util.Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return _parseDate(p, ctxt);
        }
    }

    /**
     * Compared to plain old {@link java.util.Date}, SQL version is easier
     * to deal with: mostly because it is more limited.
     */
    public static class SqlDateDeserializer
        extends DateBasedDeserializer<java.sql.Date>
    {
        public SqlDateDeserializer() { super(java.sql.Date.class); }
        public SqlDateDeserializer(SqlDateDeserializer src, DateFormat df, String formatString) {
            super(src, df, formatString);
        }

        @Override
        protected SqlDateDeserializer withDateFormat(DateFormat df, String formatString) {
            return new SqlDateDeserializer(this, df, formatString);
        }

        @Override // since 2.12
        public Object getEmptyValue(DeserializationContext ctxt) {
            return new java.sql.Date(0L);
        }

        @Override
        public java.sql.Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            Date d = _parseDate(p, ctxt);
            return (d == null) ? null : new java.sql.Date(d.getTime());
        }
    }

    /**
     * Simple deserializer for handling {@link java.sql.Timestamp} values.
     *<p>
     * One way to customize Timestamp formats accepted is to override method
     * {@link DeserializationContext#parseDate} that this basic
     * deserializer calls.
     */
    public static class TimestampDeserializer extends DateBasedDeserializer<java.sql.Timestamp>
    {
        public TimestampDeserializer() { super(java.sql.Timestamp.class); }
        public TimestampDeserializer(TimestampDeserializer src, DateFormat df, String formatString) {
            super(src, df, formatString);
        }

        @Override
        protected TimestampDeserializer withDateFormat(DateFormat df, String formatString) {
            return new TimestampDeserializer(this, df, formatString);
        }

        @Override // since 2.12
        public Object getEmptyValue(DeserializationContext ctxt) {
            return new java.sql.Timestamp(0L);
        }

        @Override
        public java.sql.Timestamp deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
        {
            Date d = _parseDate(p, ctxt);
            return (d == null) ? null : new java.sql.Timestamp(d.getTime());
        }
    }
}
