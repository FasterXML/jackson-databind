package com.fasterxml.jackson.databind.contextual;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

public class TestContextAttributeWithSer extends BaseMapTest
{
    final static String KEY = "foobar";

    @SuppressWarnings("serial")
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
            provider.setAttribute(KEY, Integer.valueOf(i + 1));
            jgen.writeString("" +i+":"+value);
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
        final String EXP = aposToQuotes("[{'value':'0:a'},{'value':'1:b'}]");
        ObjectWriter w = MAPPER.writer();
        final TestPOJO[] INPUT = new TestPOJO[] {
                new TestPOJO("a"), new TestPOJO("b") };
        assertEquals(EXP, w.writeValueAsString(INPUT));

        // also: ensure that we don't retain per-call state accidentally:
        assertEquals(EXP, w.writeValueAsString(INPUT));
    }

    public void testSimpleDefaults() throws Exception
    {
        final String EXP = aposToQuotes("{'value':'3:xyz'}");
        final TestPOJO INPUT = new TestPOJO("xyz");
        String json = MAPPER.writer().withAttribute(KEY, Integer.valueOf(3))
                .writeValueAsString(INPUT);
        assertEquals(EXP, json);

        String json2 = MAPPER.writer().withAttribute(KEY, Integer.valueOf(3))
                .writeValueAsString(INPUT);
        assertEquals(EXP, json2);
    }

    public void testHierarchic() throws Exception
    {
        final TestPOJO[] INPUT = new TestPOJO[] { new TestPOJO("a"), new TestPOJO("b") };
        final String EXP = aposToQuotes("[{'value':'2:a'},{'value':'3:b'}]");
        ObjectWriter w = MAPPER.writer().withAttribute(KEY, Integer.valueOf(2));
        assertEquals(EXP, w.writeValueAsString(INPUT));

        // and verify state clearing:
        assertEquals(EXP, w.writeValueAsString(INPUT));
    }
}
