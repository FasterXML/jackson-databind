package com.fasterxml.jackson.databind.ser.jdk;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.junit.Assert;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;

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

        serialize(mapper, judate(1970, 1, 1,  02, 00, 00, 0, "GMT+2"),
                "1970-01-01T00:00:00.000+"+zoneOffset("0000"));
        serialize(mapper, judate(1970, 1, 1,  00, 00, 00, 0, "UTC"),
                "1970-01-01T00:00:00.000+"+zoneOffset("0000"));

        // 22-Nov-2018, tatu: Also ensure we use padding...
        serialize(mapper, judate(911, 1, 1,  00, 00, 00, 0, "UTC"),
                "0911-01-01T00:00:00.000+"+zoneOffset("0000"));
        serialize(mapper, judate(87, 1, 1,  00, 00, 00, 0, "UTC"),
                "0087-01-01T00:00:00.000+"+zoneOffset("0000"));
        serialize(mapper, judate(1, 1, 1,  00, 00, 00, 0, "UTC"),
                "0001-01-01T00:00:00.000+"+zoneOffset("0000"));
    }

    // [databind#2167]: beyond year 9999 needs special handling
    public void testDateISO8601_10k() throws IOException
    {
        ObjectWriter w = MAPPER.writer()
                .without(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        serialize(w, judate(10204, 1, 1,  00, 00, 00, 0, "UTC"),
                "+10204-01-01T00:00:00.000+"+zoneOffset("0000"));
        // and although specification lacks for beyond 5 digits (well, actually even 5...), let's do our best:
        serialize(w, judate(123456, 1, 1,  00, 00, 00, 0, "UTC"),
                "+123456-01-01T00:00:00.000+"+zoneOffset("0000"));
    }

    // [databind#2167]: dates before Common Era (CE), that is, BCE, need special care:
    public void testDateISO8601_BCE() throws IOException
    {
        ObjectWriter w = MAPPER.writer()
                .without(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // First: I _think_ BCE-1 is what you get with year 0, and should become "+0000"
        // and from further back in time, it'll be "-0001" (BCE-2) etc)

        serialize(w, judate(0, 1, 1,  00, 00, 00, 0, "UTC"),   "+0000-01-01T00:00:00.000+"+zoneOffset("0000"));
        serialize(w, judate(-1, 1, 1,  00, 00, 00, 0, "UTC"),   "-0001-01-01T00:00:00.000+"+zoneOffset("0000"));
        serialize(w, judate(-49, 1, 1,  00, 00, 00, 0, "UTC"),   "-0049-01-01T00:00:00.000+"+zoneOffset("0000")); // All hail Caesar
        serialize(w, judate(-264, 1, 1,  00, 00, 00, 0, "UTC"),   "-0264-01-01T00:00:00.000+"+zoneOffset("0000")); // Carthage FTW?
    }

    /**
     * Use a default TZ other than UTC. Dates must be serialized using that TZ.
     */
    public void testDateISO8601_customTZ() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.setTimeZone(TimeZone.getTimeZone("GMT+2"));

        serialize(mapper, judate(1970, 1, 1,  00, 00, 00, 0, "GMT+2"),
                "1970-01-01T00:00:00.000+"+zoneOffset("0200"));
        serialize(mapper, judate(1970, 1, 1,  00, 00, 00, 0, "UTC"),
                "1970-01-01T02:00:00.000+"+zoneOffset("0200"));
    }

    /**
     * Configure the StdDateFormat to serialize TZ offset with a colon between hours and minutes
     *
     * See [databind#1744], [databind#2643]
     */
    public void testDateISO8601_colonInTZ() throws IOException
    {
        // with [databind#2643], default now is to include
        StdDateFormat dateFormat = new StdDateFormat();
        assertTrue(dateFormat.isColonIncludedInTimeZone());
        // but we can disable it
        dateFormat = dateFormat.withColonInTimeZone(false);

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.setDateFormat(dateFormat);

        serialize(mapper, judate(1970, 1, 1,  02, 00, 00, 0, "GMT+2"), "1970-01-01T00:00:00.000+0000");
        serialize(mapper, judate(1970, 1, 1,  00, 00, 00, 0, "UTC"),   "1970-01-01T00:00:00.000+0000");
    }

    public void testDateOther() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'X'HH:mm:ss");
        mapper.setDateFormat(df);
        mapper.setTimeZone(TimeZone.getTimeZone("PST"));

        // let's hit epoch start, offset by a bit
        serialize( mapper, judate(1970, 1, 1,  00, 00, 00, 0, "UTC"), "1969-12-31X16:00:00");
    }

    public void testTimeZone() throws IOException
    {
        TimeZone input = TimeZone.getTimeZone("PST");
        String json = MAPPER.writeValueAsString(input);
        assertEquals(q("PST"), json);
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
        assertEquals(q("1969-12-31X16:00:00"),
                MAPPER.writer(df)
                    .with(tz)
                    .writeValueAsString(new Date(0L)));
        ObjectWriter w = MAPPER.writer((DateFormat)null);
        assertEquals("0", w.writeValueAsString(new Date(0L)));

        w = w.with(df).with(tz);
        assertEquals(q("1969-12-31X16:00:00"), w.writeValueAsString(new Date(0L)));
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
        assertEquals("{\"1970-01-01T00:00:00.000+"+zoneOffset("0000")+"\":1}", mapper.writeValueAsString(map));

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
        assertEquals(a2q("{'date':0}"), json);

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
        assertEquals("{\"date\":\"1970-01-01T00:00:00.000+"+zoneOffset("0000")+"\"}", json);
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

        // pacific time is GMT-8; so midnight becomes 16:00 previous day:
        serialize( mapper, judate(1969, 12, 31, 16, 00, 00, 00, "PST"), "1969-12-31/16:00 PST");

        // Let's also verify that Locale won't matter too much...
        mapper.setLocale(Locale.FRANCE);
        serialize( mapper, judate(1969, 12, 31, 16, 00, 00, 00, "PST"), "1969-12-31/16:00 PST");

        // Also: should be able to dynamically change timezone:
        ObjectWriter w = mapper.writer().with(TimeZone.getTimeZone("EST"));
        assertEquals(q("1969-12-31/"+zoneOffset("1900")+" EST"), w.writeValueAsString(new Date(0)));

        // wrt [databind#2643]
        w = mapper.writer().with(TimeZone.getTimeZone("Asia/Tehran"));
        assertEquals(q("1970-01-01/"+zoneOffset("0330")+" IRST"), w.writeValueAsString(new Date(0)));
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
        assertEquals(a2q("{'date':0}"), json);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        json = mapper.writeValueAsString(new DateAsDefaultBean(0L));
        assertEquals(a2q("{'date':'1970-01-01T00:00:00.000+"+zoneOffset("0000")+"'}"), json);

        // Empty @JsonFormat => default to user config
        mapper.enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        json = mapper.writeValueAsString(new DateAsDefaultBeanWithEmptyJsonFormat(0L));
        assertEquals(a2q("{'date':0}"), json);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        json = mapper.writeValueAsString(new DateAsDefaultBeanWithEmptyJsonFormat(0L));
        assertEquals(a2q("{'date':'1970-01-01T00:00:00.000+"+zoneOffset("0000")+"'}"), json);

        // @JsonFormat with Shape.ANY and pattern => STRING shape, regardless of user config
        mapper.enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        json = mapper.writeValueAsString(new DateAsDefaultBeanWithPattern(0L));
        assertEquals(a2q("{'date':'1970-01-01'}"), json);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        json = mapper.writeValueAsString(new DateAsDefaultBeanWithPattern(0L));
        assertEquals(a2q("{'date':'1970-01-01'}"), json);

        // @JsonFormat with Shape.ANY and locale => STRING shape, regardless of user config
        mapper.enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        json = mapper.writeValueAsString(new DateAsDefaultBeanWithLocale(0L));
        assertEquals(a2q("{'date':'1970-01-01T00:00:00.000+"+zoneOffset("0000")+"'}"), json);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        json = mapper.writeValueAsString(new DateAsDefaultBeanWithLocale(0L));
        assertEquals(a2q("{'date':'1970-01-01T00:00:00.000+"+zoneOffset("0000")+"'}"), json);

        // @JsonFormat with Shape.ANY and timezone => STRING shape, regardless of user config
        mapper.enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        json = mapper.writeValueAsString(new DateAsDefaultBeanWithTimezone(0L));
        assertEquals(a2q("{'date':'1970-01-01T01:00:00.000+"+zoneOffset("0100")+"'}"), json);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        json = mapper.writeValueAsString(new DateAsDefaultBeanWithTimezone(0L));
        assertEquals(a2q("{'date':'1970-01-01T01:00:00.000+"+zoneOffset("0100")+"'}"), json);
    }

    // [databind#1648]: contextual default format should be used
    public void testFormatWithoutPattern() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'X'HH:mm:ss"));
        String json = mapper.writeValueAsString(new DateAsDefaultBeanWithTimezone(0L));
        assertEquals(a2q("{'date':'1970-01-01X01:00:00'}"), json);
    }

    private static Date judate(int year, int month, int day, int hour, int minutes, int seconds, int millis, String tz) {
        Calendar cal = Calendar.getInstance();
        // 23-Nov-2018, tatu: Safer this way, even though negative appears to work too
        if (year < 0) {
            year = -year + 1;
            cal.set(Calendar.ERA, GregorianCalendar.BC);
            cal.set(year, month-1, day, hour, minutes, seconds);
        } else {
            cal.set(year, month-1, day, hour, minutes, seconds);
        }
        cal.set(Calendar.MILLISECOND, millis);
        cal.setTimeZone(TimeZone.getTimeZone(tz));

        return cal.getTime();
    }

    private void serialize(ObjectMapper mapper, Object date, String expected) throws IOException {
        Assert.assertEquals(q(expected), mapper.writeValueAsString(date));
    }

    private void serialize(ObjectWriter w, Object date, String expected) throws IOException {
        Assert.assertEquals(q(expected), w.writeValueAsString(date));
    }

    private String zoneOffset(String raw) {
        // Add colon or not -- difference between 2.10 and earlier, 2.11 and later
        return raw.substring(0, 2) + ":" + raw.substring(2); // 2.11 and later
//        return raw; // 2.10 and earlier
    }
}
