package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.*;

public class SkipInjectableIntrospection962Test extends BaseMapTest
{
    static class InjectMe
    {
        private String a;

        public void setA(String a) {
            this.a = a;
        }

        public void setA(Integer a) {
            this.a = a.toString();
        }

        public void setA(InjectMe a) {
            this.a = String.valueOf(a);
        }
        
        public String getA() {
            return a;
        }
    }

    static class Injectee
    {
        private String b;

        @JsonCreator
        public Injectee(@JacksonInject InjectMe injectMe, @JsonProperty("b") String b) {
            this.b = b;
        }

        public String getB() {
            return b;
        }
    }

    public void testInjected()
    {
        InjectMe im = new InjectMe();
        ObjectMapper sut = new ObjectMapper()
            .setInjectableValues(new InjectableValues.Std().addValue(InjectMe.class, im));
        String test = "{\"b\":\"bbb\"}";

        Injectee actual = null;
        try {
            actual = sut.readValue(test, Injectee.class);
        }
        catch (Exception e) {
            fail("failed to deserialize: "+e);
        }
        assertEquals("bbb", actual.getB());
    }
}
