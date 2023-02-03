package com.fasterxml.jackson.databind.deser.jdk;

import java.beans.ConstructorProperties;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.OptBoolean;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

public class DateDeserializationTest
    extends BaseMapTest
{
    static class DateAsStringBean
    {
        @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="/yyyy/MM/dd/")
        public Date date;
    }

    static class DateAsStringBeanGermany
    {
        @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="/yyyy/MM/dd/", locale="fr_FR")
        public Date date;
    }

    static class CalendarAsStringBean
    {
        @JsonFormat(shape=JsonFormat.Shape.STRING, pattern=";yyyy/MM/dd;")
        public Calendar cal;
    }

    static class DateInCETBean {
        @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy-MM-dd,HH", timezone="CET")
        public Date date;
    }

    static class CalendarBean {
        Calendar _v;
        void setV(Calendar v) { _v = v; }
    }

    static class LenientCalendarBean {
        @JsonFormat(lenient=OptBoolean.TRUE)
        public Calendar value;
    }

    static class StrictCalendarBean {
        @JsonFormat(lenient=OptBoolean.FALSE)
        public Calendar value;
    }

    // [databind#1722]
    public static class Date1722 {
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
        private Date date;

        @JsonIgnore
        private String foo;

        @ConstructorProperties({"date", "foo"})
        public Date1722(Date date, String foo) {
            this.date = date;
            this.foo = foo;
        }

        public Date getDate() {
            return this.date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public String getFoo() {
            return this.foo;
        }

        public void setFoo(String foo) {
            this.foo = foo;
        }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    public void testDateUtil() throws Exception
    {
        long now = 123456789L;
        java.util.Date value = new java.util.Date(now);

        // First from long
        assertEquals(value, MAPPER.readValue(""+now, java.util.Date.class));

        // then from String
        String dateStr = dateToString(value);
        java.util.Date result = MAPPER.readValue("\""+dateStr+"\"", java.util.Date.class);

        assertEquals("Date: expect "+value+" ("+value.getTime()+"), got "+result+" ("+result.getTime()+")",
                value.getTime(), result.getTime());
    }

    public void testDateUtilWithStringTimestamp() throws Exception
    {
        long now = 1321992375446L;
        /* Should be ok to pass as JSON String, as long
         * as it is plain timestamp (all numbers, 64-bit)
         */
        String json = q(String.valueOf(now));
        java.util.Date value = MAPPER.readValue(json, java.util.Date.class);
        assertEquals(now, value.getTime());

        // #267: should handle negative timestamps too; like 12 hours before 1.1.1970
        long before = - (24 * 3600 * 1000L);
        json = q(String.valueOf(before));
        value = MAPPER.readValue(json, java.util.Date.class);
        assertEquals(before, value.getTime());
    }

    public void testDateUtilRFC1123() throws Exception
    {
        DateFormat fmt = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
        // let's use an arbitrary value...
        String inputStr = "Sat, 17 Jan 2009 06:13:58 +0000";
        java.util.Date inputDate = fmt.parse(inputStr);
        assertEquals(inputDate, MAPPER.readValue("\""+inputStr+"\"", java.util.Date.class));
    }

    public void testDateUtilRFC1123OnNonUSLocales() throws Exception
    {
        Locale old = Locale.getDefault();
        Locale.setDefault(Locale.GERMAN);
        DateFormat fmt = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
        // let's use an arbitrary value...
        String inputStr = "Sat, 17 Jan 2009 06:13:58 +0000";
        java.util.Date inputDate = fmt.parse(inputStr);
        assertEquals(inputDate, MAPPER.readValue("\""+inputStr+"\"", java.util.Date.class));
        Locale.setDefault(old);
    }

    /**
     * ISO8601 is supported as well
     */
    public void testDateUtilISO8601() throws Exception
    {
        /* let's use simple baseline value, arbitrary date in GMT,
         * using the standard notation
         */
        String inputStr = "1972-12-28T00:00:00.000+0000";
        Date inputDate = MAPPER.readValue("\""+inputStr+"\"", java.util.Date.class);
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        c.setTime(inputDate);
        assertEquals(1972, c.get(Calendar.YEAR));
        assertEquals(Calendar.DECEMBER, c.get(Calendar.MONTH));
        assertEquals(28, c.get(Calendar.DAY_OF_MONTH));

        // And then the same, but using 'Z' as alias for +0000 (very common)
        inputStr = "1972-12-28T00:00:00.000Z";
        inputDate = MAPPER.readValue(q(inputStr), java.util.Date.class);
        c.setTime(inputDate);
        assertEquals(1972, c.get(Calendar.YEAR));
        assertEquals(Calendar.DECEMBER, c.get(Calendar.MONTH));
        assertEquals(28, c.get(Calendar.DAY_OF_MONTH));

        // Same but using colon in timezone
        inputStr = "1972-12-28T00:00:00.000+00:00";
        inputDate = MAPPER.readValue(q(inputStr), java.util.Date.class);
        c.setTime(inputDate);
        assertEquals(1972, c.get(Calendar.YEAR));
        assertEquals(Calendar.DECEMBER, c.get(Calendar.MONTH));
        assertEquals(28, c.get(Calendar.DAY_OF_MONTH));

        // Same but only passing hour difference as timezone
        inputStr = "1972-12-28T00:00:00.000+00";
        inputDate = MAPPER.readValue(q(inputStr), java.util.Date.class);
        c.setTime(inputDate);
        assertEquals(1972, c.get(Calendar.YEAR));
        assertEquals(Calendar.DECEMBER, c.get(Calendar.MONTH));
        assertEquals(28, c.get(Calendar.DAY_OF_MONTH));

        inputStr = "1984-11-30T00:00:00.000Z";
        inputDate = MAPPER.readValue(q(inputStr), java.util.Date.class);
        c.setTime(inputDate);
        assertEquals(1984, c.get(Calendar.YEAR));
        assertEquals(Calendar.NOVEMBER, c.get(Calendar.MONTH));
        assertEquals(30, c.get(Calendar.DAY_OF_MONTH));
    }

    // [Databind#570]
    public void testISO8601PartialMilliseconds() throws Exception
    {
        String inputStr;
        Date inputDate;
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

        inputStr = "2014-10-03T18:00:00.6-05:00";
        inputDate = MAPPER.readValue(q(inputStr), java.util.Date.class);
        c.setTime(inputDate);
        assertEquals(2014, c.get(Calendar.YEAR));
        assertEquals(Calendar.OCTOBER, c.get(Calendar.MONTH));
        assertEquals(3, c.get(Calendar.DAY_OF_MONTH));
        assertEquals(600, c.get(Calendar.MILLISECOND));

        inputStr = "2014-10-03T18:00:00.61-05:00";
        inputDate = MAPPER.readValue(q(inputStr), java.util.Date.class);
        c.setTime(inputDate);
        assertEquals(2014, c.get(Calendar.YEAR));
        assertEquals(Calendar.OCTOBER, c.get(Calendar.MONTH));
        assertEquals(3, c.get(Calendar.DAY_OF_MONTH));
        assertEquals(18 + 5, c.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, c.get(Calendar.MINUTE));
        assertEquals(0, c.get(Calendar.SECOND));
        assertEquals(610, c.get(Calendar.MILLISECOND));

        inputStr = "1997-07-16T19:20:30.45+01:00";
        inputDate = MAPPER.readValue(q(inputStr), java.util.Date.class);
        c.setTime(inputDate);
        assertEquals(1997, c.get(Calendar.YEAR));
        assertEquals(Calendar.JULY, c.get(Calendar.MONTH));
        assertEquals(16, c.get(Calendar.DAY_OF_MONTH));
        assertEquals(19 - 1, c.get(Calendar.HOUR_OF_DAY));
        assertEquals(20, c.get(Calendar.MINUTE));
        assertEquals(30, c.get(Calendar.SECOND));
        assertEquals(450, c.get(Calendar.MILLISECOND));

        // 14-Sep-2015, tatu: Colon for timezone offset is optional, verify
        inputStr = "1997-07-16T19:20:30.45+0100";
        inputDate = MAPPER.readValue(q(inputStr), java.util.Date.class);
        c.setTime(inputDate);
        assertEquals(1997, c.get(Calendar.YEAR));
        assertEquals(Calendar.JULY, c.get(Calendar.MONTH));
        assertEquals(16, c.get(Calendar.DAY_OF_MONTH));
        assertEquals(19 - 1, c.get(Calendar.HOUR_OF_DAY));
        assertEquals(20, c.get(Calendar.MINUTE));
        assertEquals(30, c.get(Calendar.SECOND));
        assertEquals(450, c.get(Calendar.MILLISECOND));

        // plus may also just have hour part
        inputStr = "1997-07-16T19:20:30.45+01";
        inputDate = MAPPER.readValue(q(inputStr), java.util.Date.class);
        c.setTime(inputDate);
        assertEquals(1997, c.get(Calendar.YEAR));
        assertEquals(Calendar.JULY, c.get(Calendar.MONTH));
        assertEquals(16, c.get(Calendar.DAY_OF_MONTH));
        assertEquals(19 - 1, c.get(Calendar.HOUR_OF_DAY));
        assertEquals(20, c.get(Calendar.MINUTE));
        assertEquals(30, c.get(Calendar.SECOND));
        assertEquals(450, c.get(Calendar.MILLISECOND));
    }

    // Also: minutes-part of offset need not be all zeroes: [databind#1788]
    public void testISO8601FractionalTimezoneOffset() throws Exception
    {
        String inputStr = "1997-07-16T19:20:30.45+01:30";
        java.util.Date inputDate = MAPPER.readValue(q(inputStr), java.util.Date.class);
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        c.setTime(inputDate);
        assertEquals(1997, c.get(Calendar.YEAR));
        assertEquals(Calendar.JULY, c.get(Calendar.MONTH));
        assertEquals(16, c.get(Calendar.DAY_OF_MONTH));
        assertEquals(19 - 2, c.get(Calendar.HOUR_OF_DAY));
        assertEquals(50, c.get(Calendar.MINUTE));
        assertEquals(30, c.get(Calendar.SECOND));
        assertEquals(450, c.get(Calendar.MILLISECOND));
    }

    // [databind#1745]
    public void testISO8601FractSecondsLong() throws Exception
    {
        String inputStr;
        Date inputDate;
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

        inputStr = "2014-10-03T18:00:00.3456-05:00";
        inputDate = MAPPER.readValue(q(inputStr), java.util.Date.class);
        c.setTime(inputDate);
        assertEquals(2014, c.get(Calendar.YEAR));
        assertEquals(Calendar.OCTOBER, c.get(Calendar.MONTH));
        assertEquals(3, c.get(Calendar.DAY_OF_MONTH));
        // should truncate; not error or round
        assertEquals(345, c.get(Calendar.MILLISECOND));

        // But! Still limit to 9 digits (nanoseconds)
        try {
            MAPPER.readValue(q("2014-10-03T18:00:00.1234567890-05:00"), java.util.Date.class);
        } catch (InvalidFormatException e) {
            verifyException(e, "invalid fractional seconds");
            verifyException(e, "can use at most 9 digits");
        }
    }

    public void testISO8601MissingSeconds() throws Exception
    {
        String inputStr;
        Date inputDate;

        // 23-Jun-2017, tatu: Shouldn't this be UTC?
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

        inputStr = "1997-07-16T19:20+01:00";
        inputDate = MAPPER.readValue(q(inputStr), java.util.Date.class);
        c.setTime(inputDate);
        assertEquals(1997, c.get(Calendar.YEAR));
        assertEquals(Calendar.JULY, c.get(Calendar.MONTH));
        assertEquals(16, c.get(Calendar.DAY_OF_MONTH));
        assertEquals(19 - 1, c.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, c.get(Calendar.SECOND));
        assertEquals(0, c.get(Calendar.MILLISECOND));

        // 14-Sep-2015, tatu: Colon for timezone offset is optional, verify
        inputStr = "1997-07-16T19:20+0200";
        inputDate = MAPPER.readValue(q(inputStr), java.util.Date.class);
        c.setTime(inputDate);
        assertEquals(1997, c.get(Calendar.YEAR));
        assertEquals(Calendar.JULY, c.get(Calendar.MONTH));
        assertEquals(16, c.get(Calendar.DAY_OF_MONTH));
        assertEquals(19 - 2, c.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, c.get(Calendar.SECOND));
        assertEquals(0, c.get(Calendar.MILLISECOND));

        // or just hour
        inputStr = "1997-07-16T19:20+04";
        inputDate = MAPPER.readValue(q(inputStr), java.util.Date.class);
        c.setTime(inputDate);
        assertEquals(1997, c.get(Calendar.YEAR));
        assertEquals(Calendar.JULY, c.get(Calendar.MONTH));
        assertEquals(16, c.get(Calendar.DAY_OF_MONTH));
        assertEquals(19 - 4, c.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, c.get(Calendar.SECOND));
        assertEquals(0, c.get(Calendar.MILLISECOND));
    }

    public void testDateUtilISO8601NoTimezone() throws Exception
    {
        // Timezone itself is optional as well...
        String inputStr = "1984-11-13T00:00:09";
        Date inputDate = MAPPER.readValue(q(inputStr), java.util.Date.class);
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        c.setTime(inputDate);
        assertEquals(1984, c.get(Calendar.YEAR));
        assertEquals(Calendar.NOVEMBER, c.get(Calendar.MONTH));
        assertEquals(13, c.get(Calendar.DAY_OF_MONTH));
        assertEquals(0, c.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, c.get(Calendar.MINUTE));
        assertEquals(9, c.get(Calendar.SECOND));
        assertEquals(0, c.get(Calendar.MILLISECOND));
    }

    // [databind#1657]: no timezone should use configured default
    public void testDateUtilISO8601NoTimezoneNonDefault() throws Exception
    {
        // In first case, no timezone -> SHOULD use configured timezone
        ObjectReader r = MAPPER.readerFor(Date.class);
        TimeZone tz = TimeZone.getTimeZone("GMT-2");
        Date date1 = r.with(tz)
                .readValue(q("1970-01-01T00:00:00.000"));
        // Second case, should use specified timezone, not configured
        Date date2 = r.with(TimeZone.getTimeZone("GMT+5"))
                .readValue(q("1970-01-01T00:00:00.000-02:00"));
        assertEquals(date1, date2);

        // also verify actual value, in GMT
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        c.setTime(date1);
        assertEquals(1970, c.get(Calendar.YEAR));
        assertEquals(Calendar.JANUARY, c.get(Calendar.MONTH));
        assertEquals(1, c.get(Calendar.DAY_OF_MONTH));
        assertEquals(2, c.get(Calendar.HOUR_OF_DAY));
    }

    // [databind#1722]: combination of `@ConstructorProperties` and `@JsonIgnore`
    //  should work fine.
    public void testFormatAndCtors1722() throws Exception
    {
        Date1722 input = new Date1722(new Date(0L), "bogus");
        String json = MAPPER.writeValueAsString(input);
        Date1722 result = MAPPER.readValue(json, Date1722.class);
        assertNotNull(result);
    }

    // [databind#338]
    public void testDateUtilISO8601NoMilliseconds() throws Exception
    {
        final String INPUT_STR = "2013-10-31T17:27:00";
        Date inputDate;
        Calendar c;

        inputDate = MAPPER.readValue(q(INPUT_STR), java.util.Date.class);
        c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        c.setTime(inputDate);
        assertEquals(2013, c.get(Calendar.YEAR));
        assertEquals(Calendar.OCTOBER, c.get(Calendar.MONTH));
        assertEquals(31, c.get(Calendar.DAY_OF_MONTH));
        assertEquals(17, c.get(Calendar.HOUR_OF_DAY));
        assertEquals(27, c.get(Calendar.MINUTE));
        assertEquals(0, c.get(Calendar.SECOND));
        assertEquals(0, c.get(Calendar.MILLISECOND));

        // 03-Nov-2013, tatu: This wouldn't work, and is the nominal reason
        //    for #338 I think
        /*
        inputDate =  ISO8601Utils.parse(INPUT_STR, new ParsePosition(0));
        c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        c.setTime(inputDate);
        assertEquals(2013, c.get(Calendar.YEAR));
        assertEquals(Calendar.OCTOBER, c.get(Calendar.MONTH));
        assertEquals(31, c.get(Calendar.DAY_OF_MONTH));
        assertEquals(17, c.get(Calendar.HOUR_OF_DAY));
        assertEquals(27, c.get(Calendar.MINUTE));
        assertEquals(0, c.get(Calendar.SECOND));
        assertEquals(0, c.get(Calendar.MILLISECOND));
        */
    }

    public void testDateUtilISO8601JustDate() throws Exception
    {
        // Plain date (no time)
        String inputStr = "1972-12-28";
        Date inputDate = MAPPER.readValue(q(inputStr), java.util.Date.class);
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        c.setTime(inputDate);
        assertEquals(1972, c.get(Calendar.YEAR));
        assertEquals(Calendar.DECEMBER, c.get(Calendar.MONTH));
        assertEquals(28, c.get(Calendar.DAY_OF_MONTH));
    }

    public void testCalendar() throws Exception
    {
        // not ideal, to use (ever-changing) current date, but...
        java.util.Calendar value = Calendar.getInstance();
        long l = 12345678L;
        value.setTimeInMillis(l);

        // First from long
        Calendar result = MAPPER.readValue(""+l, Calendar.class);
        assertEquals(l, result.getTimeInMillis());

        // Then from serialized String
        String dateStr = dateToString(new Date(l));
        result = MAPPER.readValue(q(dateStr), Calendar.class);

        // note: representation may differ (wrt timezone etc), but underlying value must remain the same:
        if (l != result.getTimeInMillis()) {
            fail(String.format("Expected timestamp %d, got %d, for '%s'",
                    l, result.getTimeInMillis(), dateStr));
        }
    }

    public void testCustom() throws Exception
    {
        final ObjectMapper mapper = new ObjectMapper();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'X'HH:mm:ss");
        df.setTimeZone(TimeZone.getTimeZone("PST"));
        mapper.setDateFormat(df);

        String dateStr = "1972-12-28X15:45:00";
        java.util.Date exp = df.parse(dateStr);
        java.util.Date result = mapper.readValue("\""+dateStr+"\"", java.util.Date.class);
        assertEquals(exp, result);
    }

    /**
     * Test for [JACKSON-203]: make empty Strings deserialize as nulls by default,
     * without need to turn on feature (which may be added in future)
     */
    public void testDatesWithEmptyStrings() throws Exception
    {
        assertNull(MAPPER.readValue(q(""), java.util.Date.class));
        assertNull(MAPPER.readValue(q(""), java.util.Calendar.class));
    }

    public void test8601DateTimeNoMilliSecs() throws Exception
    {
        // ok, Zebra, no milliseconds
        for (String inputStr : new String[] {
               "2010-06-28T23:34:22Z",
               "2010-06-28T23:34:22+0000",
               "2010-06-28T23:34:22+00:00",
               "2010-06-28T23:34:22+00",
        }) {
            Date inputDate = MAPPER.readValue(q(inputStr), java.util.Date.class);
            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            c.setTime(inputDate);
            assertEquals(2010, c.get(Calendar.YEAR));
            assertEquals(Calendar.JUNE, c.get(Calendar.MONTH));
            assertEquals(28, c.get(Calendar.DAY_OF_MONTH));
            assertEquals(23, c.get(Calendar.HOUR_OF_DAY));
            assertEquals(34, c.get(Calendar.MINUTE));
            assertEquals(22, c.get(Calendar.SECOND));
            assertEquals(0, c.get(Calendar.MILLISECOND));
        }
    }

    public void testTimeZone() throws Exception
    {
        TimeZone result = MAPPER.readValue(q("PST"), TimeZone.class);
        assertEquals("PST", result.getID());
    }

    public void testCustomDateWithAnnotation() throws Exception
    {
        final String INPUT = "{\"date\":\"/2005/05/25/\"}";
        DateAsStringBean result = MAPPER.readValue(INPUT, DateAsStringBean.class);
        assertNotNull(result);
        assertNotNull(result.date);
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        long l = result.date.getTime();
        if (l == 0L) {
            fail("Should not get null date");
        }
        c.setTimeInMillis(l);
        assertEquals(2005, c.get(Calendar.YEAR));
        assertEquals(Calendar.MAY, c.get(Calendar.MONTH));
        assertEquals(25, c.get(Calendar.DAY_OF_MONTH));

        // 27-Mar-2014, tatu: Let's verify that changing Locale won't break it;
        //   either via context Locale
        result = MAPPER.readerFor(DateAsStringBean.class)
                .with(Locale.GERMANY)
                .readValue(INPUT);
        assertNotNull(result);
        assertNotNull(result.date);
        l = result.date.getTime();
        if (l == 0L) {
            fail("Should not get null date");
        }
        c.setTimeInMillis(l);
        assertEquals(2005, c.get(Calendar.YEAR));
        assertEquals(Calendar.MAY, c.get(Calendar.MONTH));
        assertEquals(25, c.get(Calendar.DAY_OF_MONTH));

        // or, via annotations
        DateAsStringBeanGermany result2 = MAPPER.readerFor(DateAsStringBeanGermany.class).readValue(INPUT);
        assertNotNull(result2);
        assertNotNull(result2.date);
        l = result2.date.getTime();
        if (l == 0L) {
            fail("Should not get null date");
        }
        c.setTimeInMillis(l);
        assertEquals(2005, c.get(Calendar.YEAR));
        assertEquals(Calendar.MAY, c.get(Calendar.MONTH));
        assertEquals(25, c.get(Calendar.DAY_OF_MONTH));
    }

    public void testCustomCalendarWithAnnotation() throws Exception
    {
        CalendarAsStringBean cbean = MAPPER.readValue("{\"cal\":\";2007/07/13;\"}",
                CalendarAsStringBean.class);
        assertNotNull(cbean);
        assertNotNull(cbean.cal);
        Calendar c = cbean.cal;
        assertEquals(2007, c.get(Calendar.YEAR));
        assertEquals(Calendar.JULY, c.get(Calendar.MONTH));
        assertEquals(13, c.get(Calendar.DAY_OF_MONTH));
    }

    public void testCustomCalendarWithTimeZone() throws Exception
    {
        // And then with different TimeZone: CET is +01:00 from GMT -- read as CET
        DateInCETBean cet = MAPPER.readValue("{\"date\":\"2001-01-01,10\"}",
                DateInCETBean.class);
        Calendar c = Calendar.getInstance(getUTCTimeZone());
        c.setTimeInMillis(cet.date.getTime());
        // so, going to UTC/GMT should reduce hour by one
        assertEquals(2001, c.get(Calendar.YEAR));
        assertEquals(Calendar.JANUARY, c.get(Calendar.MONTH));
        assertEquals(1, c.get(Calendar.DAY_OF_MONTH));
        assertEquals(9, c.get(Calendar.HOUR_OF_DAY));
    }

    // [databind#1651]
    public void testDateEndingWithZNonDefTZ1651() throws Exception
    {
        String json = q("1970-01-01T00:00:00.000Z");

        // Standard mapper with timezone UTC: shared instance should be ok.
        // ... but, Travis manages to have fails, so insist on newly created
        ObjectMapper mapper = newJsonMapper();
        Date dateUTC = mapper.readValue(json, Date.class);  // 1970-01-01T00:00:00.000+00:00

        // Mapper with timezone GMT-2
        // note: must construct new one, not share
        mapper = new ObjectMapper();
        mapper.setTimeZone(TimeZone.getTimeZone("GMT-2"));
        Date dateGMT1 = mapper.readValue(json, Date.class);  // 1970-01-01T00:00:00.000-02:00

        // Underlying timestamps should be the same
        assertEquals(dateUTC.getTime(), dateGMT1.getTime());
    }

    /*
    /**********************************************************
    /* Context timezone use (or not)
    /**********************************************************
     */

    // for [databind#204]
    public void testContextTimezone() throws Exception
    {
        String inputStr = "1997-07-16T19:20:30.45+0100";
        final String tzId = "PST";

        // this is enabled by default:
        assertTrue(MAPPER.isEnabled(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE));
        final ObjectReader r = MAPPER
                .readerFor(Calendar.class)
                .with(TimeZone.getTimeZone(tzId));

        // by default use contextual timezone:
        Calendar cal = r.readValue(q(inputStr));
        TimeZone tz = cal.getTimeZone();
        assertEquals(tzId, tz.getID());

        assertEquals(1997, cal.get(Calendar.YEAR));
        assertEquals(Calendar.JULY, cal.get(Calendar.MONTH));
        assertEquals(16, cal.get(Calendar.DAY_OF_MONTH));

        // Translated from original into PST differs:
        assertEquals(20, cal.get(Calendar.MINUTE));
        assertEquals(30, cal.get(Calendar.SECOND));
        assertEquals(11, cal.get(Calendar.HOUR_OF_DAY));

        // but if disabled, should use what's been sent in:
        cal = r.without(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                .readValue(q(inputStr));

        // 23-Jun-2017, tatu: Actually turns out to be hard if not impossible to do ...
        //    problem being SimpleDateFormat does not really retain timezone offset.
        //    But if we match fields... we perhaps could use it?

        // !!! TODO: would not yet pass
/*
        System.err.println("CAL/2 == "+cal);

        System.err.println("tz == "+cal.getTimeZone());
        */
    }

    /*
    /**********************************************************
    /* Test(s) for array unwrapping
    /**********************************************************
     */

    public void testCalendarArrayUnwrap() throws Exception
    {
        ObjectReader reader = new ObjectMapper()
                .readerFor(CalendarBean.class)
                .without(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS);
        final String inputDate = "1972-12-28T00:00:00.000+0000";
        final String input = a2q("{'v':['"+inputDate+"']}");
        try {
            reader.readValue(input);
            fail("Did not throw exception when reading a value from a single value array with the UNWRAP_SINGLE_VALUE_ARRAYS feature disabled");
        } catch (MismatchedInputException exp) {
            verifyException(exp, "Cannot deserialize");
            verifyException(exp, "from Array value (token `JsonToken.START_ARRAY`)");
        }

        reader = reader.with(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS);
        CalendarBean bean = reader.readValue(input);
        assertNotNull(bean._v);
        assertEquals(1972, bean._v.get(Calendar.YEAR));

        // and finally, a fail due to multiple values:
        try {
            reader.readValue(a2q("{'v':['"+inputDate+"','"+inputDate+"']}"));
            fail("Did not throw exception while reading a value from a multi value array with UNWRAP_SINGLE_VALUE_ARRAY feature enabled");
        } catch (MismatchedInputException exp) {
            verifyException(exp, "Attempted to unwrap");
        }
    }

    /*
    /**********************************************************
    /* Tests for leniency
    /**********************************************************
     */

    public void testLenientJDKDateTypes() throws Exception
    {
        final String JSON = a2q("{'value':'2015-11-32'}");

        // with lenient, can parse fine
        LenientCalendarBean lenBean = MAPPER.readValue(JSON, LenientCalendarBean.class);
        assertEquals(Calendar.DECEMBER, lenBean.value.get(Calendar.MONTH));
        assertEquals(2, lenBean.value.get(Calendar.DAY_OF_MONTH));

        // with strict, ought to produce exception
        try {
            MAPPER.readValue(JSON, StrictCalendarBean.class);
            fail("Should not pass with invalid (with strict) date value");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot deserialize value of type `java.util.Calendar`");
            verifyException(e, "from String \"2015-11-32\"");
            verifyException(e, "expected format");
        }
    }

    public void testLenientJDKDateTypesViaTypeOverride() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configOverride(java.util.Date.class)
            .setFormat(JsonFormat.Value.forLeniency(Boolean.FALSE));
        try {
            mapper.readValue(q("2015-11-32"), java.util.Date.class);
            fail("Should not pass with invalid (with strict) date value");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot deserialize value of type `java.util.Date`");
            verifyException(e, "from String \"2015-11-32\"");
            verifyException(e, "expected format");
        }
    }

    public void testLenientJDKDateTypesViaGlobal() throws Exception
    {
        final String JSON = q("2015-11-32");

        // with lenient, can parse fine
        Calendar value = MAPPER.readValue(JSON, Calendar.class);
        assertEquals(Calendar.DECEMBER, value.get(Calendar.MONTH));
        assertEquals(2, value.get(Calendar.DAY_OF_MONTH));

        // but not so if default leniency disabled
        ObjectMapper mapper = new ObjectMapper();
        mapper.setDefaultLeniency(false);
        try {
            mapper.readValue(JSON, java.util.Date.class);
            fail("Should not pass with invalid (with strict) date value");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot deserialize value of type `java.util.Date`");
            verifyException(e, "from String \"2015-11-32\"");
            verifyException(e, "expected format");
        }

        // Unless we actually had per-type override too
        mapper = new ObjectMapper();
        mapper.configOverride(Calendar.class)
            .setFormat(JsonFormat.Value.forLeniency(Boolean.TRUE));
        mapper.setDefaultLeniency(false);

        value = mapper.readValue(JSON, Calendar.class);
        assertEquals(Calendar.DECEMBER, value.get(Calendar.MONTH));
        assertEquals(2, value.get(Calendar.DAY_OF_MONTH));
    }

    /*
    /**********************************************************
    /* Tests to verify failing cases
    /**********************************************************
     */

    public void testInvalidFormat() throws Exception
    {
        try {
            MAPPER.readValue(q("foobar"), Date.class);
            fail("Should have failed with an exception");
        } catch (InvalidFormatException e) {
            verifyException(e, "Cannot deserialize value of type `java.util.Date` from String");
            assertEquals("foobar", e.getValue());
            assertEquals(Date.class, e.getTargetType());
        } catch (Exception e) {
            fail("Wrong type of exception ("+e.getClass().getName()+"), should get "
                    +InvalidFormatException.class.getName());
        }
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private String dateToString(java.util.Date value)
    {
        // Then from String. This is bit tricky, since JDK does not really
        // suggest a 'standard' format. So let's try using something...
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        return df.format(value);
    }
}
