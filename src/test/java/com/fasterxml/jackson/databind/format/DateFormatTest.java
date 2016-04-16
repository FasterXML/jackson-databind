package com.fasterxml.jackson.databind.format;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.*;

public class DateFormatTest extends BaseMapTest
{
    protected static class DateWrapper {
        public Date value;

        public DateWrapper() { }
        public DateWrapper(long l) { value = new Date(l); }
        public DateWrapper(Date v) { value = v; }
    }

    public void testTypeDefaults() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configOverride(Date.class)
            .setFormat(JsonFormat.Value.forPattern("yyyy-MM-dd"));
        String json = mapper.writeValueAsString(new DateWrapper(0L));
        assertEquals(aposToQuotes("{'value':'1970-01-01'}"), json);
    }
}
