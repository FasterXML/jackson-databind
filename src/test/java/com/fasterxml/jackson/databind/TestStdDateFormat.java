package com.fasterxml.jackson.databind;

import java.text.ParseException;
import java.util.*;

import com.fasterxml.jackson.databind.util.StdDateFormat;

public class TestStdDateFormat
    extends BaseMapTest
{
    public void testFactories() {
        TimeZone tz = TimeZone.getTimeZone("GMT");
        Locale loc = Locale.US;
        assertNotNull(StdDateFormat.getISO8601Format(tz, loc));
        assertNotNull(StdDateFormat.getRFC1123Format(tz, loc));
    }

    // [databind#803
    public void testLenient() throws Exception
    {
        StdDateFormat f = StdDateFormat.instance;

        // default should be lenient
        assertTrue(f.isLenient());

        StdDateFormat f2 = f.clone();
        assertTrue(f2.isLenient());

        f2.setLenient(false);
        assertFalse(f2.isLenient());

        f2.setLenient(true);
        assertTrue(f2.isLenient());

        // and for testing, finally, leave as non-lenient
        f2.setLenient(false);
        assertFalse(f2.isLenient());
        StdDateFormat f3 = f2.clone();
        assertFalse(f3.isLenient());

        // first, legal dates are... legal
        Date dt = f3.parse("2015-11-30");
        assertNotNull(dt);

        // but as importantly, when not lenient, do not allow
        try {
            f3.parse("2015-11-32");
            fail("Should not pass");
        } catch (ParseException e) {
            verifyException(e, "can not parse date");
        }

        // ... yet, with lenient, do allow
        f3.setLenient(true);
        dt = f3.parse("2015-11-32");
        assertNotNull(dt);
    }
    
    public void testInvalid() {
        StdDateFormat std = new StdDateFormat();
        try {
            std.parse("foobar");
        } catch (java.text.ParseException e) {
            verifyException(e, "Can not parse");
        }
    }
}
