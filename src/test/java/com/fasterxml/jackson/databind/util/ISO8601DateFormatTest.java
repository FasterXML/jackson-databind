package com.fasterxml.jackson.databind.util;

import java.text.DateFormat;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("deprecation")
public class ISO8601DateFormatTest extends DatabindTestUtil
{
    private ISO8601DateFormat df;
    private Date date;

    @BeforeEach
    public void setUp()
    {
        Calendar cal = new GregorianCalendar(2007, 8 - 1, 13, 19, 51, 23);
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        cal.set(Calendar.MILLISECOND, 0);
        date = cal.getTime();
        df = new ISO8601DateFormat();
    }

    @Test
    public void testFormat() {
        String result = df.format(date);
        assertEquals("2007-08-13T19:51:23Z", result);
    }

    @Test
    public void testParse() throws Exception {
        Date result = df.parse("2007-08-13T19:51:23Z");
        assertEquals(date, result);

        // Test parsing date-only values with and without a timezone designation
        Date dateOnly = df.parse("2007-08-14");
        Calendar cal = new GregorianCalendar(2007, 8-1, 14);
        assertEquals(cal.getTime(), dateOnly);

        dateOnly = df.parse("2007-08-14Z");
        cal = new GregorianCalendar(2007, 8-1, 14);
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        assertEquals(cal.getTime(), dateOnly);
    }

    @Test
    public void testPartialParse() throws Exception {
        java.text.ParsePosition pos = new java.text.ParsePosition(0);
        String timestamp = "2007-08-13T19:51:23Z";
        Date result = df.parse(timestamp + "hello", pos);

        assertEquals(date, result);
        assertEquals(timestamp.length(), pos.getIndex());
    }

    @Test
    public void testCloneObject() throws Exception {
        DateFormat clone = (DateFormat)df.clone();
        assertSame(df, clone);
    }

    @Test
    public void testHashCodeEquals() throws Exception {
        // for [databind#1130]
        DateFormat defaultDF = StdDateFormat.instance;
        defaultDF.hashCode();
        assertTrue(defaultDF.equals(defaultDF));
    }
}
