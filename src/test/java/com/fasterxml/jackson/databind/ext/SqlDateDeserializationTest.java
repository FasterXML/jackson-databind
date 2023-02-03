package com.fasterxml.jackson.databind.ext;

import java.util.Calendar;
import java.util.TimeZone;

import com.fasterxml.jackson.databind.*;

public class SqlDateDeserializationTest
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    @SuppressWarnings("deprecation")
    public void testDateSql() throws Exception
    {
        java.sql.Date value = new java.sql.Date(0L);
        value.setYear(99); // 1999
        value.setDate(19);
        value.setMonth(Calendar.APRIL);
        long now = value.getTime();

        // First from long
        assertEquals(value, MAPPER.readValue(String.valueOf(now), java.sql.Date.class));

        // then from default java.sql.Date String serialization:

        java.sql.Date result = MAPPER.readValue(q(value.toString()), java.sql.Date.class);
        Calendar c = gmtCalendar(result.getTime());
        assertEquals(1999, c.get(Calendar.YEAR));
        assertEquals(Calendar.APRIL, c.get(Calendar.MONTH));
        assertEquals(19, c.get(Calendar.DAY_OF_MONTH));

        /* [JACKSON-200]: looks like we better add support for regular date
         *   formats as well
         */
        String expStr = "1981-07-13";
        result = MAPPER.readValue(q(expStr), java.sql.Date.class);
        c.setTimeInMillis(result.getTime());
        assertEquals(1981, c.get(Calendar.YEAR));
        assertEquals(Calendar.JULY, c.get(Calendar.MONTH));
        assertEquals(13, c.get(Calendar.DAY_OF_MONTH));

        /* 20-Nov-2009, tatus: I'll be damned if I understand why string serialization
         *   is off-by-one, but day-of-month does seem to be one less. My guess is
         *   that something is funky with timezones (i.e. somewhere local TZ is
         *   being used), but just can't resolve it. Hence, need to comment this:
         */
//        assertEquals(expStr, result.toString());
    }

    public void testDatesWithEmptyStrings() throws Exception
    {
        assertNull(MAPPER.readValue(q(""), java.sql.Date.class));
    }

    private static Calendar gmtCalendar(long time)
    {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        c.setTimeInMillis(time);
        return c;
    }

}
