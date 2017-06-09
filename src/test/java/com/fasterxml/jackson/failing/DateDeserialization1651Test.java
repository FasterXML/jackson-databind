package com.fasterxml.jackson.failing;

import java.util.Date;
import java.util.TimeZone;

import com.fasterxml.jackson.databind.*;

public class DateDeserialization1651Test extends BaseMapTest
{
    private final ObjectMapper MAPPER = objectMapper();

    public void testDate1651() throws Exception
    {
        String json = quote("1970-01-01T00:00:00.000Z");

        // Standard mapper with timezone UTC
         Date dateUTC = MAPPER.readValue(json, Date.class);  // 1970-01-01T00:00:00.000+00:00
    
         // Mapper with timezone GMT-2
         ObjectMapper mapper = new ObjectMapper();
         mapper.setTimeZone(TimeZone.getTimeZone("GMT-2"));
         Date dateGMT1 = mapper.readValue(json, Date.class);  // 1970-01-01T00:00:00.000-02:00
    
         // Underlying timestamps should be the same
         assertEquals(dateUTC.getTime(), dateGMT1.getTime());
     }
}
