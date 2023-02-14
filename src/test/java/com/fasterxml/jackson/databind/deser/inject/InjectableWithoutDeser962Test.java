package com.fasterxml.jackson.databind.deser.inject;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.*;

// [databind#962]: "pure" Injectable that could not be deserialized
public class InjectableWithoutDeser962Test extends BaseMapTest
{
    // [databind#962]
    static class InjectMe
    {
        private String a;

        public InjectMe(boolean dummy) { }

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

        // Important! Prevent binding from data
        @JsonCreator
        public Injectee(@JacksonInject(useInput=OptBoolean.FALSE) InjectMe injectMe,
                @JsonProperty("b") String b) {
            this.b = b;
        }

        public String getB() {
            return b;
        }
    }

    // [databind#962]
    public void testInjected() throws Exception
    {
        InjectMe im = new InjectMe(true);
        ObjectMapper mapper = new ObjectMapper()
            .setInjectableValues(new InjectableValues.Std().addValue(InjectMe.class, im));
        String test = "{\"b\":\"bbb\"}";

        Injectee actual = mapper.readValue(test, Injectee.class);
        assertEquals("bbb", actual.getB());
    }
}
