package com.fasterxml.jackson.databind.util;

import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@SuppressWarnings("deprecation")
public class ISO8601UtilsTest extends DatabindTestUtil
{
    private Date date;
    private Date dateWithoutTime;
    private Date dateZeroMillis;
    private Date dateZeroSecondAndMillis;

    @BeforeEach
    public void setUp() {
        Calendar cal = new GregorianCalendar(2007, 8 - 1, 13, 19, 51, 23);
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        cal.set(Calendar.MILLISECOND, 789);
        date = cal.getTime();
        cal.set(Calendar.MILLISECOND, 0);
        dateZeroMillis = cal.getTime();
        cal.set(Calendar.SECOND, 0);
        dateZeroSecondAndMillis = cal.getTime();

        cal = new GregorianCalendar(2007, 8 - 1, 13, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        dateWithoutTime = cal.getTime();

    }

    @Test
    public void testFormat() {
        String result = ISO8601Utils.format(date);
        assertEquals("2007-08-13T19:51:23Z", result);
    }

    @Test
    public void testFormatMillis() {
        String result = ISO8601Utils.format(date, true);
        assertEquals("2007-08-13T19:51:23.789Z", result);

        result = ISO8601Utils.format(date, false);
        assertEquals("2007-08-13T19:51:23Z", result);
    }

    @Test
    public void testFormatTimeZone() {
        String result = ISO8601Utils.format(date, false, TimeZone.getTimeZone("GMT+02:00"));
        assertEquals("2007-08-13T21:51:23+02:00", result);
        result = ISO8601Utils.format(date, true, TimeZone.getTimeZone("GMT+02:00"));
        assertEquals("2007-08-13T21:51:23.789+02:00", result);
        result = ISO8601Utils.format(date, true, TimeZone.getTimeZone("GMT"));
        assertEquals("2007-08-13T19:51:23.789Z", result);
    }

    @Test
    public void testParse() throws java.text.ParseException {
        Date d = ISO8601Utils.parse("2007-08-13T19:51:23.789Z", new ParsePosition(0));
        assertEquals(date, d);

        d = ISO8601Utils.parse("2007-08-13T19:51:23Z", new ParsePosition(0));
        assertEquals(dateZeroMillis, d);

        d = ISO8601Utils.parse("2007-08-13T21:51:23.789+02:00", new ParsePosition(0));
        assertEquals(date, d);
    }

    @Test
    public void testParseShortDate() throws java.text.ParseException {
        Date d = ISO8601Utils.parse("20070813T19:51:23.789Z", new ParsePosition(0));
        assertEquals(date, d);

        d = ISO8601Utils.parse("20070813T19:51:23Z", new ParsePosition(0));
        assertEquals(dateZeroMillis, d);

        d = ISO8601Utils.parse("20070813T21:51:23.789+02:00", new ParsePosition(0));
        assertEquals(date, d);
    }

    @Test
    public void testParseShortTime() throws java.text.ParseException {
        Date d = ISO8601Utils.parse("2007-08-13T195123.789Z", new ParsePosition(0));
        assertEquals(date, d);

        d = ISO8601Utils.parse("2007-08-13T195123Z", new ParsePosition(0));
        assertEquals(dateZeroMillis, d);

        d = ISO8601Utils.parse("2007-08-13T215123.789+02:00", new ParsePosition(0));
        assertEquals(date, d);
    }

    @Test
    public void testParseShortDateTime() throws java.text.ParseException {
        Date d = ISO8601Utils.parse("20070813T195123.789Z", new ParsePosition(0));
        assertEquals(date, d);

        d = ISO8601Utils.parse("20070813T195123Z", new ParsePosition(0));
        assertEquals(dateZeroMillis, d);

        d = ISO8601Utils.parse("20070813T215123.789+02:00", new ParsePosition(0));
        assertEquals(date, d);
    }

    @Test
    public void testParseWithoutTime() throws ParseException {
        Date d = ISO8601Utils.parse("2007-08-13Z", new ParsePosition(0));
        assertEquals(dateWithoutTime, d);

        d = ISO8601Utils.parse("20070813Z", new ParsePosition(0));
        assertEquals(dateWithoutTime, d);

        d = ISO8601Utils.parse("2007-08-13+00:00", new ParsePosition(0));
        assertEquals(dateWithoutTime, d);

        d = ISO8601Utils.parse("20070813+00:00", new ParsePosition(0));
        assertEquals(dateWithoutTime, d);
    }

    @Test
    public void testParseOptional() throws java.text.ParseException {
        Date d = ISO8601Utils.parse("2007-08-13T19:51Z", new ParsePosition(0));
        assertEquals(dateZeroSecondAndMillis, d);

        d = ISO8601Utils.parse("2007-08-13T1951Z", new ParsePosition(0));
        assertEquals(dateZeroSecondAndMillis, d);

        d = ISO8601Utils.parse("2007-08-13T21:51+02:00", new ParsePosition(0));
        assertEquals(dateZeroSecondAndMillis, d);
    }

    @Test
    public void testParseRfc3339Examples() throws java.text.ParseException {
        // Two digit milliseconds.
        Date d = ISO8601Utils.parse("1985-04-12T23:20:50.52Z", new ParsePosition(0));
        assertEquals(newDate(1985, 4, 12, 23, 20, 50, 520, 0), d);

        d = ISO8601Utils.parse("1996-12-19T16:39:57-08:00", new ParsePosition(0));
        assertEquals(newDate(1996, 12, 19, 16, 39, 57, 0, -8 * 60), d);

        // Truncated leap second.
        d = ISO8601Utils.parse("1990-12-31T23:59:60Z", new ParsePosition(0));
        assertEquals(newDate(1990, 12, 31, 23, 59, 59, 0, 0), d);

        // Truncated leap second.
        d = ISO8601Utils.parse("1990-12-31T15:59:60-08:00", new ParsePosition(0));
        assertEquals(newDate(1990, 12, 31, 15, 59, 59, 0, -8 * 60), d);

        // Two digit milliseconds.
        d = ISO8601Utils.parse("1937-01-01T12:00:27.87+00:20", new ParsePosition(0));
        assertEquals(newDate(1937, 1, 1, 12, 0, 27, 870, 20), d);
    }

    @Test
    public void testFractionalSeconds() throws java.text.ParseException {
        Date d = ISO8601Utils.parse("1970-01-01T00:00:00.9Z", new ParsePosition(0));
        assertEquals(newDate(1970, 1, 1, 0, 0, 0, 900, 0), d);

        d = ISO8601Utils.parse("1970-01-01T00:00:00.09Z", new ParsePosition(0));
        assertEquals(newDate(1970, 1, 1, 0, 0, 0, 90, 0), d);

        d = ISO8601Utils.parse("1970-01-01T00:00:00.009Z", new ParsePosition(0));
        assertEquals(newDate(1970, 1, 1, 0, 0, 0, 9, 0), d);

        d = ISO8601Utils.parse("1970-01-01T00:00:00.0009Z", new ParsePosition(0));
        assertEquals(newDate(1970, 1, 1, 0, 0, 0, 0, 0), d);

        d = ISO8601Utils.parse("1970-01-01T00:00:00.2147483647Z", new ParsePosition(0));
        assertEquals(newDate(1970, 1, 1, 0, 0, 0, 214, 0), d);

        d = ISO8601Utils.parse("1970-01-01T00:00:00.2147483648Z", new ParsePosition(0));
        assertEquals(newDate(1970, 1, 1, 0, 0, 0, 214, 0), d);

        d = ISO8601Utils.parse("1970-01-01T00:00:00.9+02:00", new ParsePosition(0));
        assertEquals(newDate(1970, 1, 1, 0, 0, 0, 900, 2 * 60), d);

        d = ISO8601Utils.parse("1970-01-01T00:00:00.09+02:00", new ParsePosition(0));
        assertEquals(newDate(1970, 1, 1, 0, 0, 0, 90, 2 * 60), d);

        d = ISO8601Utils.parse("1970-01-01T00:00:00.009+02:00", new ParsePosition(0));
        assertEquals(newDate(1970, 1, 1, 0, 0, 0, 9, 2 * 60), d);

        d = ISO8601Utils.parse("1970-01-01T00:00:00.0009+02:00", new ParsePosition(0));
        assertEquals(newDate(1970, 1, 1, 0, 0, 0, 0, 2 * 60), d);

        d = ISO8601Utils.parse("1970-01-01T00:00:00.2147483648+02:00", new ParsePosition(0));
        assertEquals(newDate(1970, 1, 1, 0, 0, 0, 214, 2 * 60), d);
    }

    @Test
    public void testDecimalWithoutDecimalPointButNoFractionalSeconds() throws java.text.ParseException {
        try {
            ISO8601Utils.parse("1970-01-01T00:00:00.Z", new ParsePosition(0));
            fail();
        } catch (ParseException expected) {
        }
    }

    private Date newDate(int year, int month, int day, int hour,
                         int minute, int second, int millis, int timezoneOffsetMinutes) {
        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        calendar.set(year, month - 1, day, hour, minute, second);
        calendar.set(Calendar.MILLISECOND, millis);
        return new Date(calendar.getTimeInMillis() - TimeUnit.MINUTES.toMillis(timezoneOffsetMinutes));
    }
}
