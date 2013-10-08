package com.fasterxml.jackson.databind.contextual;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;

public class TestContextAttributeWithDeser extends BaseMapTest
{
    final static String KEY = "foobar";
    
    @SuppressWarnings("serial")
    static class PrefixStringDeserializer extends StdScalarDeserializer<String>
    {
        protected PrefixStringDeserializer() {
            super(String.class);
        }

        @Override
        public String deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException
        {
            Integer I = (Integer) ctxt.getAttribute(KEY);
            if (I == null) {
                I = Integer.valueOf(0);
            }
            int i = I.intValue();
            ctxt.setAttribute(KEY, Integer.valueOf(i + 1));
            return jp.getText()+"/"+i;
        }

    }

    static class TestPOJO
    {
        @JsonDeserialize(using=PrefixStringDeserializer.class)
        public String value;
    }
    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    final ObjectMapper MAPPER = objectMapper();
    
    public void testSimplePerCall() throws Exception
    {
        TestPOJO[] pojos = MAPPER.reader(TestPOJO[].class)
            .readValue(aposToQuotes("[{'value':'a'},{'value':'b'}]"));
        assertEquals(2, pojos.length);
        assertEquals("a/0", pojos[0].value);
        assertEquals("b/1", pojos[1].value);
    }

    public void testSimpleDefaults() throws Exception
    {
        TestPOJO pojo = MAPPER.reader(TestPOJO.class)
                .withAttribute(KEY, Integer.valueOf(3))
                .readValue(aposToQuotes("{'value':'x'}"));
        assertEquals("x/3", pojo.value);
    }

    public void testHierarchic() throws Exception
    {
        ObjectReader r = MAPPER.reader(TestPOJO[].class).withAttribute(KEY, Integer.valueOf(2));
        TestPOJO[] pojos = r.readValue(aposToQuotes("[{'value':'x'},{'value':'y'}]"));
        assertEquals(2, pojos.length);
        assertEquals("x/2", pojos[0].value);
        assertEquals("y/3", pojos[1].value);
    }
}
