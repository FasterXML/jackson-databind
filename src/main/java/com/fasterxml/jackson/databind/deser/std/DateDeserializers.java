package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;

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
    /* Deserializer implementations
    /**********************************************************
     */
    
    @JacksonStdImpl
    public static class CalendarDeserializer
        extends StdScalarDeserializer<Calendar>
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
        extends StdScalarDeserializer<Date>
    {
        public DateDeserializer() { super(Date.class); }
       
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
        extends StdScalarDeserializer<java.sql.Date>
    {
        public SqlDateDeserializer() { super(java.sql.Date.class); }

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
        extends StdScalarDeserializer<Timestamp>
    {
        public TimestampDeserializer() { super(Timestamp.class); }

        @Override
        public java.sql.Timestamp deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            return new Timestamp(_parseDate(jp, ctxt).getTime());
        }
    }

    /**
     * As per [JACKSON-522], also need special handling for TimeZones
     * 
     * @since 1.7.4
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
