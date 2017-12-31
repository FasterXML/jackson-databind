package com.fasterxml.jackson.databind.ext.jdk8;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.TimeZone;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.*;

public class ContextualOptionalTest extends BaseMapTest
{
    // [datatypes-java8#17]
    @JsonPropertyOrder({ "date", "date1", "date2" })
    static class ContextualOptionals
    {
        public Optional<Date> date;

        @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy+MM+dd")
        public Optional<Date> date1;

        @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy*MM*dd")
        public Optional<Date> date2;
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    public void testContextualOptionals() throws Exception
    {
        final ObjectMapper mapper = newObjectMapper();
        SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        mapper.setDateFormat(df);
        ContextualOptionals input = new ContextualOptionals();
        input.date = Optional.ofNullable(new Date(0L));
        input.date1 = Optional.ofNullable(new Date(0L));
        input.date2 = Optional.ofNullable(new Date(0L));
        final String json = mapper.writeValueAsString(input);
//System.err.println("JSON:\n"+json);
        assertEquals(aposToQuotes(
                "{'date':'1970/01/01','date1':'1970+01+01','date2':'1970*01*01'}"),
                json);
    }        
}
