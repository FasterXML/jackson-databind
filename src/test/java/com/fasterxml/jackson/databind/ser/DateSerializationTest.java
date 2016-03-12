package com.fasterxml.jackson.databind.ser;

import java.io.*;
import java.text.*;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.*;

public class DateSerializationTest
    extends BaseMapTest
{
    static class TimeZoneBean {
        private TimeZone tz;
        
        public TimeZoneBean(String name) {
            tz = TimeZone.getTimeZone(name);
        }

        public TimeZone getTz() { return tz; }
    }

    static class DateAsNumberBean {
        @JsonFormat(shape=JsonFormat.Shape.NUMBER)
        public Date date;
        public DateAsNumberBean(long l) { date = new java.util.Date(l); }
    }

    static class SqlDateAsDefaultBean {
        public java.sql.Date date;
        public SqlDateAsDefaultBean(long l) { date = new java.sql.Date(l); }
    }

    static class SqlDateAsNumberBean {
        @JsonFormat(shape=JsonFormat.Shape.NUMBER)
        public java.sql.Date date;
        public SqlDateAsNumberBean(long l) { date = new java.sql.Date(l); }
    }

    static class DateAsStringBean {
        @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")
        public Date date;
        public DateAsStringBean(long l) { date = new java.util.Date(l); }
    }

    static class DateAsDefaultStringBean {
        @JsonFormat(shape=JsonFormat.Shape.STRING)
        public Date date;
        public DateAsDefaultStringBean(long l) { date = new java.util.Date(l); }
    }

    static class DateInCETBean {
        @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy-MM-dd,HH:00", timezone="CET")
        public Date date;
        public DateInCETBean(long l) { date = new java.util.Date(l); }
    }

    static class CalendarAsStringBean {
        @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")
        public Calendar value;
        public CalendarAsStringBean(long l) {
            value = new GregorianCalendar();
            value.setTimeInMillis(l);
        }
    }

    static class DateAsDefaultBean {
        public Date date;
        public DateAsDefaultBean(long l) { date = new java.util.Date(l); }
    }
    
    static class DateAsDefaultBeanWithEmptyJsonFormat {
        @JsonFormat
        public Date date;
        public DateAsDefaultBeanWithEmptyJsonFormat(long l) { date = new java.util.Date(l); }
    }

    static class DateAsDefaultBeanWithPattern {
        @JsonFormat(pattern="yyyy-MM-dd")
        public Date date;
        public DateAsDefaultBeanWithPattern(long l) { date = new java.util.Date(l); }
    }

    static class DateAsDefaultBeanWithLocale {
        @JsonFormat(locale = "fr")
        public Date date;
        public DateAsDefaultBeanWithLocale(long l) { date = new java.util.Date(l); }
    }

    static class DateAsDefaultBeanWithTimezone {
        @JsonFormat(timezone="CET")
        public Date date;
        public DateAsDefaultBeanWithTimezone(long l) { date = new java.util.Date(l); }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testDateNumeric() throws IOException
    {
        // default is to output time stamps...
        assertTrue(MAPPER.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));
        // shouldn't matter which offset we give...
        String json = MAPPER.writeValueAsString(new Date(199L));
        assertEquals("199", json);
    }

    public void testDateISO8601() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        // let's hit epoch start
        String json = mapper.writeValueAsString(new Date(0L));
        assertEquals("\"1970-01-01T00:00:00.000+0000\"", json);
    }

    public void testDateOther() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'X'HH:mm:ss");
        mapper.setDateFormat(df);
        mapper.setTimeZone(TimeZone.getTimeZone("PST"));
        // let's hit epoch start, offset by a bit
        assertEquals(quote("1969-12-31X16:00:00"), mapper.writeValueAsString(new Date(0L)));
    }

    @SuppressWarnings("deprecation")
    public void testSqlDate() throws IOException
    {
        // use date 1999-04-01 (note: months are 0-based, use constant)
        java.sql.Date date = new java.sql.Date(99, Calendar.APRIL, 1);
        assertEquals(quote("1999-04-01"), MAPPER.writeValueAsString(date));

        java.sql.Date date0 = new java.sql.Date(0L);
        assertEquals(aposToQuotes("{'date':'"+date0.toString()+"'}"),
                MAPPER.writeValueAsString(new SqlDateAsDefaultBean(0L)));

        // but may explicitly force timestamp too
        assertEquals(aposToQuotes("{'date':0}"), MAPPER.writeValueAsString(new SqlDateAsNumberBean(0L)));
    }

    public void testSqlTime() throws IOException
    {
        java.sql.Time time = new java.sql.Time(0L);
        // not 100% sure what we should expect wrt timezone, but what serializes
        // does use is quite simple:
        assertEquals(quote(time.toString()), MAPPER.writeValueAsString(time));
    }

    public void testSqlTimestamp() throws IOException
    {
        java.sql.Timestamp input = new java.sql.Timestamp(0L);
        // just should produce same output as standard `java.util.Date`:
        Date altTnput = new Date(0L);
        assertEquals(MAPPER.writeValueAsString(altTnput),
                MAPPER.writeValueAsString(input));
    }

    public void testTimeZone() throws IOException
    {
        TimeZone input = TimeZone.getTimeZone("PST");
        String json = MAPPER.writeValueAsString(input);
        assertEquals(quote("PST"), json);
    }

    public void testTimeZoneInBean() throws IOException
    {
        String json = MAPPER.writeValueAsString(new TimeZoneBean("PST"));
        assertEquals("{\"tz\":\"PST\"}", json);
    }

    public void testDateUsingObjectWriter() throws IOException
    {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'X'HH:mm:ss");
        TimeZone tz = TimeZone.getTimeZone("PST");
        assertEquals(quote("1969-12-31X16:00:00"),
                MAPPER.writer(df)
                    .with(tz)
                    .writeValueAsString(new Date(0L)));
        ObjectWriter w = MAPPER.writer((DateFormat)null);
        assertEquals("0", w.writeValueAsString(new Date(0L)));

        w = w.with(df).with(tz);
        assertEquals(quote("1969-12-31X16:00:00"), w.writeValueAsString(new Date(0L)));
        w = w.with((DateFormat) null);
        assertEquals("0", w.writeValueAsString(new Date(0L)));
    }

    public void testDatesAsMapKeys() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        Map<Date,Integer> map = new HashMap<Date,Integer>();
        assertFalse(mapper.isEnabled(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS));
        map.put(new Date(0L), Integer.valueOf(1));
        // by default will serialize as ISO-8601 values...
        assertEquals("{\"1970-01-01T00:00:00.000+0000\":1}", mapper.writeValueAsString(map));
        
        // but can change to use timestamps too
        mapper.configure(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS, true);
        assertEquals("{\"0\":1}", mapper.writeValueAsString(map));
    }

    public void testDateWithJsonFormat() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        String json;

        // first: test overriding writing as timestamp
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        json = mapper.writeValueAsString(new DateAsNumberBean(0L));
        assertEquals(aposToQuotes("{'date':0}"), json);

        // then reverse
        mapper.enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        json = mapper.writer().with(getUTCTimeZone()).writeValueAsString(new DateAsStringBean(0L));
        assertEquals("{\"date\":\"1970-01-01\"}", json);

        // and with different DateFormat; CET is one hour ahead of GMT
        json = mapper.writeValueAsString(new DateInCETBean(0L));
        assertEquals("{\"date\":\"1970-01-01,01:00\"}", json);
        
        // and for [Issue#423] as well:
        json = mapper.writer().with(getUTCTimeZone()).writeValueAsString(new CalendarAsStringBean(0L));
        assertEquals("{\"value\":\"1970-01-01\"}", json);

        // and with default (ISO8601) format (databind#1109)
        json = mapper.writeValueAsString(new DateAsDefaultStringBean(0L));
        assertEquals("{\"date\":\"1970-01-01T00:00:00.000+0000\"}", json);
    }

    /**
     * Test to ensure that setting a TimeZone _after_ dateformat should enforce
     * that timezone on format, regardless of TimeZone format had.
     */
    public void testWithTimeZoneOverride() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd/HH:mm z"));
        mapper.setTimeZone(TimeZone.getTimeZone("PST"));
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        String json = mapper.writeValueAsString(new Date(0));
        // pacific time is GMT-8; so midnight becomes 16:00 previous day:
        assertEquals(quote("1969-12-31/16:00 PST"), json);

        // Let's also verify that Locale won't matter too much...
        mapper.setLocale(Locale.FRANCE);
        json = mapper.writeValueAsString(new Date(0));
        assertEquals(quote("1969-12-31/16:00 PST"), json);

        // Also: should be able to dynamically change timezone:
        ObjectWriter w = mapper.writer();
        w = w.with(TimeZone.getTimeZone("EST"));
        json = w.writeValueAsString(new Date(0));
        assertEquals(quote("1969-12-31/19:00 EST"), json);
    }

    /**
     * Test to ensure that the default shape is correctly inferred as string or numeric,
     * when this shape is not explicitly set with a <code>@JsonFormat</code> annotation
     */
    public void testDateDefaultShape() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        // No @JsonFormat => default to user config
        mapper.enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        String json = mapper.writeValueAsString(new DateAsDefaultBean(0L));
        assertEquals(aposToQuotes("{'date':0}"), json);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        json = mapper.writeValueAsString(new DateAsDefaultBean(0L));
        assertEquals(aposToQuotes("{'date':'1970-01-01T00:00:00.000+0000'}"), json);

        // Empty @JsonFormat => default to user config
        mapper.enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        json = mapper.writeValueAsString(new DateAsDefaultBeanWithEmptyJsonFormat(0L));
        assertEquals(aposToQuotes("{'date':0}"), json);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        json = mapper.writeValueAsString(new DateAsDefaultBeanWithEmptyJsonFormat(0L));
        assertEquals(aposToQuotes("{'date':'1970-01-01T00:00:00.000+0000'}"), json);

        // @JsonFormat with Shape.ANY and pattern => STRING shape, regardless of user config
        mapper.enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        json = mapper.writeValueAsString(new DateAsDefaultBeanWithPattern(0L));
        assertEquals(aposToQuotes("{'date':'1970-01-01'}"), json);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        json = mapper.writeValueAsString(new DateAsDefaultBeanWithPattern(0L));
        assertEquals(aposToQuotes("{'date':'1970-01-01'}"), json);

        // @JsonFormat with Shape.ANY and locale => STRING shape, regardless of user config
        mapper.enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        json = mapper.writeValueAsString(new DateAsDefaultBeanWithLocale(0L));
        assertEquals(aposToQuotes("{'date':'1970-01-01T00:00:00.000+0000'}"), json);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        json = mapper.writeValueAsString(new DateAsDefaultBeanWithLocale(0L));
        assertEquals(aposToQuotes("{'date':'1970-01-01T00:00:00.000+0000'}"), json);

        // @JsonFormat with Shape.ANY and timezone => STRING shape, regardless of user config
        mapper.enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        json = mapper.writeValueAsString(new DateAsDefaultBeanWithTimezone(0L));
        assertEquals(aposToQuotes("{'date':'1970-01-01T01:00:00.000+0100'}"), json);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        json = mapper.writeValueAsString(new DateAsDefaultBeanWithTimezone(0L));
        assertEquals(aposToQuotes("{'date':'1970-01-01T01:00:00.000+0100'}"), json);
    }
}

