package com.fasterxml.jackson.databind.contextual;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

public class TestContextAttributeWithSer extends BaseMapTest
{
    final static String KEY = "foobar";
    
    static class PrefixStringSerializer extends StdScalarSerializer<String>
    {
        protected PrefixStringSerializer() {
            super(String.class);
        }

        @Override
        public void serialize(String value, JsonGenerator jgen,
                SerializerProvider provider)
            throws IOException
        {
            Integer I = (Integer) provider.getAttribute(KEY);
            if (I == null) {
                I = Integer.valueOf(0);
            }
            int i = I.intValue();
            jgen.writeString("" +i+":"+value);
            provider.setAttribute(KEY, Integer.valueOf(i + 1));
        }
    }

    static class TestPOJO
    {
        @JsonSerialize(using=PrefixStringSerializer.class)
        public String value;

        public TestPOJO(String str) { value = str; }
    }
    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    final ObjectMapper MAPPER = objectMapper();
    
    public void testSimplePerCall() throws Exception
    {
        ObjectWriter w = MAPPER.writer();
        String json = w.writeValueAsString(new TestPOJO[] {
                new TestPOJO("a"),
                new TestPOJO("b") });
        assertEquals(aposToQuotes("[{'value':'0:a'},{'value':'1:b'}]"), json);
    }

    public void testSimpleDefaults() throws Exception
    {
        String json = MAPPER.writer().withAttribute(KEY, Integer.valueOf(3))
                .writeValueAsString(new TestPOJO("xyz"));
        assertEquals(aposToQuotes("{'value':'3:xyz'}"), json);
    }

    public void testHierarchic() throws Exception
    {
        ObjectWriter w = MAPPER.writer().withAttribute(KEY, Integer.valueOf(2));
        String json = w.writeValueAsString(new TestPOJO[] {
                new TestPOJO("a"),
                new TestPOJO("b") });
        assertEquals(aposToQuotes("[{'value':'2:a'},{'value':'3:b'}]"), json);
    }
}
