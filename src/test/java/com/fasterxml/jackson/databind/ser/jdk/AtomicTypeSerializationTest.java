package com.fasterxml.jackson.databind.ser.jdk;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.atomic.*;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Unit tests for verifying serialization of simple basic non-structured
 * types; primitives (and/or their wrappers), Strings.
 */
public class AtomicTypeSerializationTest
    extends BaseMapTest
{
    static class UCStringWrapper {
        @JsonSerialize(contentUsing=UpperCasingSerializer.class)
        public AtomicReference<String> value;

        public UCStringWrapper(String s) { value = new AtomicReference<String>(s); }
    }

    // [datatypes-java8#17]
    @JsonPropertyOrder({ "date1", "date2", "date" })
    static class ContextualOptionals
    {
        public AtomicReference<Date> date;

        @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy+MM+dd")
        public AtomicReference<Date> date1;

        @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy*MM*dd")
        public AtomicReference<Date> date2;
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = objectMapper();
    
    public void testAtomicBoolean() throws Exception
    {
        assertEquals("true", MAPPER.writeValueAsString(new AtomicBoolean(true)));
        assertEquals("false", MAPPER.writeValueAsString(new AtomicBoolean(false)));
    }

    public void testAtomicInteger() throws Exception
    {
        assertEquals("1", MAPPER.writeValueAsString(new AtomicInteger(1)));
        assertEquals("-9", MAPPER.writeValueAsString(new AtomicInteger(-9)));
    }

    public void testAtomicLong() throws Exception
    {
        assertEquals("0", MAPPER.writeValueAsString(new AtomicLong(0)));
    }

    public void testAtomicReference() throws Exception
    {
        String[] strs = new String[] { "abc" };
        assertEquals("[\"abc\"]", MAPPER.writeValueAsString(new AtomicReference<String[]>(strs)));
    }

    public void testCustomSerializer() throws Exception
    {
        final String VALUE = "fooBAR";
        String json = MAPPER.writeValueAsString(new UCStringWrapper(VALUE));
        assertEquals(json, aposToQuotes("{'value':'FOOBAR'}"));
    }

    public void testContextualAtomicReference() throws Exception
    {
        SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        final ObjectMapper mapper = objectMapper();
        mapper.setDateFormat(df);
        ContextualOptionals input = new ContextualOptionals();
        input.date = new AtomicReference<>(new Date(0L));
        input.date1 = new AtomicReference<>(new Date(0L));
        input.date2 = new AtomicReference<>(new Date(0L));
        final String json = mapper.writeValueAsString(input);
        assertEquals(aposToQuotes(
                "{'date1':'1970+01+01','date2':'1970*01*01','date':'1970/01/01'}"),
                json);
    }
}
