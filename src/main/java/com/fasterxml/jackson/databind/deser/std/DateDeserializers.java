package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.*;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.util.StdDateFormat;

/**
 * Container class for core JDK date/time type deserializers.
 */
@SuppressWarnings("serial")
public class DateDeserializers
{
    private final static HashSet<String> _classNames = new HashSet<String>();
    static {
        Class<?>[] numberTypes = new Class<?>[] {
            Calendar.class,
            GregorianCalendar.class,
            java.sql.Date.class,
            java.util.Date.class,
            Timestamp.class,
        };
        for (Class<?> cls : numberTypes) {
            _classNames.add(cls.getName());
        }
    }

    public static JsonDeserializer<?> find(Class<?> rawType, String clsName)
    {
        if (_classNames.contains(clsName)) {
            // Start with the most common type
            if (rawType == Calendar.class) {
                return new CalendarDeserializer();
            }
            if (rawType == java.util.Date.class) {
                return DateDeserializer.instance;
            }
            if (rawType == java.sql.Date.class) {
                return new SqlDateDeserializer();
            }
            if (rawType == Timestamp.class) {
                return new TimestampDeserializer();
            }
            if (rawType == GregorianCalendar.class) {
                return new CalendarDeserializer(GregorianCalendar.class);
            }
        }
        return null;
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
        
        @Override
        public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property)
           throws JsonMappingException
        {
            if (property != null) {
                JsonFormat.Value format = ctxt.getAnnotationIntrospector().findFormat((Annotated) property.getMember());
                if (format != null) {
                    TimeZone tz = format.getTimeZone();
                    // First: fully custom pattern?
                    if (format.hasPattern()) {
                        final String pattern = format.getPattern();
                        final Locale loc = format.hasLocale() ? format.getLocale() : ctxt.getLocale();
                        SimpleDateFormat df = new SimpleDateFormat(pattern, loc);
                        if (tz == null) {
                            tz = ctxt.getTimeZone();
                        }
                        df.setTimeZone(tz);
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
                            df = std;
                        } else {
                            // otherwise need to clone, re-set timezone:
                            df = (DateFormat) df.clone();
                            df.setTimeZone(tz);
                        }
                        return withDateFormat(df, _formatString);
                    }
                }
            }
            return this;
        }
        
        @Override
        protected java.util.Date _parseDate(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            if (_customFormat != null) {
                JsonToken t = jp.getCurrentToken();
                if (t == JsonToken.VALUE_STRING) {
                    String str = jp.getText().trim();
                    if (str.length() == 0) {
                        return (Date) getEmptyValue();
                    }
                    synchronized (_customFormat) {
                        try {
                            return _customFormat.parse(str);
                        } catch (ParseException e) {
                            throw new IllegalArgumentException("Failed to parse Date value '"+str
                                    +"' (format: \""+_formatString+"\"): "+e.getMessage());
                        }
                    }
                }
                // Issue#381
                if (t == JsonToken.START_ARRAY && ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
                    jp.nextToken();
                    final Date parsed = _parseDate(jp, ctxt);
                    t = jp.nextToken();
                    if (t != JsonToken.END_ARRAY) {
                        throw ctxt.wrongTokenException(jp, JsonToken.END_ARRAY, 
                                "Attempted to unwrap single value array for single 'java.util.Date' value but there was more than a single value in the array");
                    }            
                    return parsed;            
                }
            }
            return super._parseDate(jp, ctxt);
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
         */
        protected final Class<? extends Calendar> _calendarClass;
        
        public CalendarDeserializer() {
            super(Calendar.class);
            _calendarClass = null;
        }

        public CalendarDeserializer(Class<? extends Calendar> cc) {
            super(cc);
            _calendarClass = cc;
        }

        public CalendarDeserializer(CalendarDeserializer src, DateFormat df, String formatString) {
            super(src, df, formatString);
            _calendarClass = src._calendarClass;
        }

        @Override
        protected CalendarDeserializer withDateFormat(DateFormat df, String formatString) {
            return new CalendarDeserializer(this, df, formatString);
        }
        
        @Override
        public Calendar deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException
        {
            Date d = _parseDate(jp, ctxt);
            if (d == null) {
                return null;
            }
            if (_calendarClass == null) {
                return ctxt.constructCalendar(d);
            }
            try {
                Calendar c = _calendarClass.newInstance();            
                c.setTimeInMillis(d.getTime());
                TimeZone tz = ctxt.getTimeZone();
                if (tz != null) {
                    c.setTimeZone(tz);
                }
                return c;
            } catch (Exception e) {
                throw ctxt.instantiationException(_calendarClass, e);
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
        
        @Override
        public java.util.Date deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            return _parseDate(jp, ctxt);
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
        
        @Override
        public java.sql.Date deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            Date d = _parseDate(jp, ctxt);
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
    public static class TimestampDeserializer extends DateBasedDeserializer<Timestamp>
    {
        public TimestampDeserializer() { super(Timestamp.class); }
        public TimestampDeserializer(TimestampDeserializer src, DateFormat df, String formatString) {
            super(src, df, formatString);
        }

        @Override
        protected TimestampDeserializer withDateFormat(DateFormat df, String formatString) {
            return new TimestampDeserializer(this, df, formatString);
        }
        
        @Override
        public java.sql.Timestamp deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException
        {
            return new Timestamp(_parseDate(jp, ctxt).getTime());
        }
    }
}
