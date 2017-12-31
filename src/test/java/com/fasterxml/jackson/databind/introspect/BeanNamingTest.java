package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.databind.*;

// Tests for [databind#653]
public class BeanNamingTest extends BaseMapTest
{
    static class URLBean {
        public String getURL() {
            return "http://foo";
        }
    }

    static class ABean {
        public int getA() {
            return 3;
        }
    }

    // 24-Sep-2017, tatu: Used to test for `MapperFeature.USE_STD_BEAN_NAMING`, but with 3.x
    //    that is always enabled.
    public void testMultipleLeadingCapitalLetters() throws Exception
    {
        ObjectMapper mapper = objectMapper();
        assertEquals(aposToQuotes("{'URL':'http://foo'}"),
                mapper.writeValueAsString(new URLBean()));
        assertEquals(aposToQuotes("{'a':3}"),
                mapper.writeValueAsString(new ABean()));
    }
}
