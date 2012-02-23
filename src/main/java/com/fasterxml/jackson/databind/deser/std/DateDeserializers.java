package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;

/**
 * Container class for core JDK date/time type deserializers.
 */
public class DateDeserializers
{
    public static StdDeserializer<?>[] all()
    {
        return  new StdDeserializer[] {
            new CalendarDeserializer(), // for nominal type of java.util.Calendar
            new DateDeserializer(),
            /* 24-Jan-2010, tatu: When including type information, we may
             *    know that we specifically need GregorianCalendar...
             */
            new CalendarDeserializer(GregorianCalendar.class),
            new SqlDateDeserializer(),
            new TimestampDeserializer(),
            new TimeZoneDeserializer()
        };
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

        protected DateBasedDeserializer(Class<?> clz) {
            super(clz);
            _customFormat = null;
        }

        protected DateBasedDeserializer(DateBasedDeserializer<T> base,
                DateFormat format) {
            super(base._valueClass);
            _customFormat = format;
        }

        protected abstract DateBasedDeserializer<T> withDateFormat(DateFormat df);
        
        @Override
        public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property)
           throws JsonMappingException
        {
            if (property != null) {
                JsonFormat.Value format = ctxt.getAnnotationIntrospector().findFormat(property.getMember());
                if (format != null) {
                    String pattern = format.pattern;
                    if (pattern.length() > 0){
                        SimpleDateFormat df = new SimpleDateFormat(pattern, ctxt.getLocale());
                        df.setTimeZone(ctxt.getTimeZone());
                        return withDateFormat(df);
                    }
                }
            }
            return this;
        }
        
        @Override
        protected java.util.Date _parseDate(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            if (_customFormat != null && jp.getCurrentToken() == JsonToken.VALUE_STRING) {
                String str = jp.getText().trim();
                if (str.length() == 0) {
                    return (Date) getEmptyValue();
                }
                synchronized (_customFormat) {
                    try {
                        return _customFormat.parse(str);
                    } catch (ParseException e) {
                        throw new IllegalArgumentException("Failed to parse Date value '"+str+"': "+e.getMessage());
                    }
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
    public static class CalendarDeserializer
        extends DateBasedDeserializer<Calendar>
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

        public CalendarDeserializer(CalendarDeserializer src, DateFormat df) {
            super(src, df);
            _calendarClass = src._calendarClass;
        }

        @Override
        protected CalendarDeserializer withDateFormat(DateFormat df) {
            return new CalendarDeserializer(this, df);
        }
        
        @Override
        public Calendar deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
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
    public static class DateDeserializer
        extends DateBasedDeserializer<Date>
    {
        public DateDeserializer() { super(Date.class); }
        public DateDeserializer(DateDeserializer base, DateFormat df) {
            super(base, df);
        }

        @Override
        protected DateDeserializer withDateFormat(DateFormat df) {
            return new DateDeserializer(this, df);
        }
        
        @Override
        public java.util.Date deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
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
        public SqlDateDeserializer(SqlDateDeserializer src, DateFormat df) {
            super(src, df);
        }

        @Override
        protected SqlDateDeserializer withDateFormat(DateFormat df) {
            return new SqlDateDeserializer(this, df);
        }
        
        @Override
        public java.sql.Date deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
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
    public static class TimestampDeserializer
        extends DateBasedDeserializer<Timestamp>
    {
        public TimestampDeserializer() { super(Timestamp.class); }
        public TimestampDeserializer(TimestampDeserializer src, DateFormat df) {
            super(src, df);
        }

        @Override
        protected TimestampDeserializer withDateFormat(DateFormat df) {
            return new TimestampDeserializer(this, df);
        }
        
        @Override
        public java.sql.Timestamp deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            return new Timestamp(_parseDate(jp, ctxt).getTime());
        }
    }

    /*
    /**********************************************************
    /* Deserializer implementations for Date-related types
    /**********************************************************
     */
    
    /**
     * As per [JACKSON-522], also need special handling for TimeZones
     */
    protected static class TimeZoneDeserializer
        extends FromStringDeserializer<TimeZone>
    {
        public TimeZoneDeserializer() { super(TimeZone.class); }

        @Override
        protected TimeZone _deserialize(String value, DeserializationContext ctxt)
            throws IOException
        {
            return TimeZone.getTimeZone(value);
        }
    }
}
