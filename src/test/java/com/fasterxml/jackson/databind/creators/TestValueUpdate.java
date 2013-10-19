package com.fasterxml.jackson.databind.creators;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.test.BaseTest;

public class TestValueUpdate extends BaseTest
{
    static class Bean
    {
        private String a;
        private String b;

        @JsonCreator
        public Bean(@JsonProperty("a") String a, @JsonProperty("b") String b)
        {
            this.a = a;
            this.b = b;
        }

        String getA() {
            return a;
        }

        void setA(String a) {
            this.a = a;
        }

        String getB() {
            return b;
        }

        void setB(String b) {
            this.b = b;
        }
    }

    // [Issue#318] (and Scala module issue #83]
    public void testValueUpdateWithCreator() throws Exception
    {
        Bean bean = new Bean("abc", "def");
        new ObjectMapper().reader(Bean.class).withValueToUpdate(bean).readValue("{\"a\":\"ghi\",\"b\":\"jkl\"}");
        assertEquals("ghi", bean.getA());
        assertEquals("jkl", bean.getB());
    }

}
