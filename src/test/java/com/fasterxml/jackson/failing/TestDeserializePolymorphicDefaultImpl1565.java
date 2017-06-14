package com.fasterxml.jackson.failing;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestDeserializePolymorphicDefaultImpl1565 extends BaseMapTest {

    public void testDeserializeWithSameDefaultImplOnBothBaseClasses() throws JsonProcessingException, IOException {
        try {
            ObjectMapper om = objectMapper();
            om.readerFor(Scenario.Sub1.class).readValue("{\"type\":\"sub1\"}");
            fail("JsonProcessingException was not thrown.");
        }
        catch (IllegalArgumentException e) { // should this throw another type of exception?
        }
    }

    /**
     * Multiple levels of inheritance. 2 Base classes 3 Subs. TypeInfo on both higher and lower base class with same defaultImpl.
     */
    static class Scenario {
        @JsonTypeInfo(include=As.PROPERTY, property="type", use=Id.NAME, defaultImpl=Sub3.class)
        @JsonSubTypes({@Type(name="sub1", value=Sub1.class), @Type(name="sub2", value=Sub2.class), @Type(name="sub3", value=Sub3.class)})
        static abstract class BaseHigh {}
        @JsonTypeInfo(include=As.PROPERTY, property="type", use=Id.NAME, defaultImpl=Sub3.class)
        @JsonSubTypes({@Type(name="sub1", value=Sub1.class), @Type(name="sub2", value=Sub2.class)})
        static abstract class BaseLow extends BaseHigh {}
        static class Sub1 extends BaseLow {}
        static class Sub2 extends BaseLow {}
        static class Sub3 extends BaseHigh {}
    }

}
