package com.fasterxml.jackson.databind.deser.jdk;

import java.util.Calendar;
import java.util.TimeZone;

import com.fasterxml.jackson.databind.*;

public class DateAdjustment204Test extends BaseMapTest
{
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    // for [databind#204]
    public void testContextTimezone() throws Exception
    {
        String inputStr = "1997-07-16T19:20:30.45+0100";

        // this is enabled by default:
        assertTrue(MAPPER.isEnabled(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE));
        final ObjectReader r =  MAPPER
                .readerFor(Calendar.class)
                .with(TimeZone.getTimeZone("PST"));

        // by default use contextual timezone:
        Calendar cal = r.readValue(quote(inputStr));
        TimeZone tz = cal.getTimeZone();
        assertEquals("PST", tz.getID());

        assertEquals(1997, cal.get(Calendar.YEAR));
        assertEquals(Calendar.JULY, cal.get(Calendar.MONTH));
        assertEquals(16, cal.get(Calendar.DAY_OF_MONTH));

        // Translated from original into PST differs:
        assertEquals(11, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(20, cal.get(Calendar.MINUTE));
        assertEquals(30, cal.get(Calendar.SECOND));

        // but if disabled, should use what's been sent in:
        cal = r.without(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                .readValue(quote(inputStr));

        
        // !!! TODO: would not yet pass
/*
        System.err.println("CAL/2 == "+cal);

        System.err.println("tz == "+cal.getTimeZone());
        */

    }

}
