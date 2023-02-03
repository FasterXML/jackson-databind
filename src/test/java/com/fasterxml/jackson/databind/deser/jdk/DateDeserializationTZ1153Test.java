package com.fasterxml.jackson.databind.deser.jdk;

import java.util.Date;
import java.util.TimeZone;

import com.fasterxml.jackson.databind.*;

public class DateDeserializationTZ1153Test extends BaseMapTest
{
    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#1153]
    public void testWithTimezones1153() throws Exception
    {
        for (String tzStr : new String[] {
                "UTC", "CET", "America/Los_Angeles", "Australia/Melbourne"
        }) {
            _testWithTimeZone(TimeZone.getTimeZone(tzStr));
        }
    }

    void _testWithTimeZone(TimeZone tz) throws Exception
    {
        ObjectReader r = MAPPER.readerFor(Date.class)
                .with(tz);

        String time = "2016-01-01T17:00:00.000Z";
        long correctTime = 1451667600000l;
        Date dateAccordingToJackson = r.readValue(q(time));

        assertEquals("ISO8601 decoding mismatch " + tz,
                correctTime, dateAccordingToJackson.getTime());
    }
}
