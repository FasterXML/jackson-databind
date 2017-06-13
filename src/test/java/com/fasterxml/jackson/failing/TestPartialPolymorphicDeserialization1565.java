package com.fasterxml.jackson.failing;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestPartialPolymorphicDeserialization1565 extends BaseMapTest {

    public void testDeserializeScenario1ToDefaultImpl() throws JsonProcessingException, IOException {
        ObjectMapper om = objectMapper();
        om.readerFor(Scenario1.Sub2.class).readValue("{\"type\":\"sub2\"}"); // no exception should happen
    }

    public void testDeserializeScenario3ToDefaultImpl() throws JsonProcessingException, IOException {
        ObjectMapper om = objectMapper();
        om.readerFor(Scenario3.Sub2.class).readValue("{\"type\":\"sub2\"}"); // no exception should happen
    }

    public void testDeserializeScenario4ToDefaultImpl() throws JsonProcessingException, IOException {
        ObjectMapper om = objectMapper();
        om.readerFor(Scenario4.Sub2.class).readValue("{\"type\":\"sub2\"}"); // no exception should happen
    }

    public void testDeserializeToUnrelatedDefaultImpl() throws JsonProcessingException, IOException {
        try {
            ObjectMapper om = objectMapper();
            om.readerFor(Scenario2.BaseWithUnrelated.class).readValue("{}");
            fail("JsonProcessingException was not thrown.");
        }
        catch (IllegalArgumentException e) { // should this throw another type of exception?
        }
    }

    /**
     * A base class with a subs and one of the is default.
     */
    static class Scenario1 {
        @JsonTypeInfo(include=As.PROPERTY, property="type", use=Id.NAME, defaultImpl=Sub1.class)
        @JsonSubTypes({@Type(name="sub1", value=Sub1.class), @Type(name="sub2", value=Sub2.class)})
        static abstract class Base {}
    
        static class Sub1 extends Base {}
    
        static class Sub2 extends Base {}
    }

    /**
     * A base class with an unrelated default class. This is incorrect and should throw errors.
     */
    static class Scenario2 {
        @JsonTypeInfo(include=As.PROPERTY, property="type", use=Id.NAME, defaultImpl=Unrelated.class)
        static abstract class BaseWithUnrelated {}
    
        static class Unrelated {}
    }

    /**
     * Multiple levels of inheritance. 2 Base classes 3 Subs. Annotations on the higher base class.
     */
    static class Scenario3 {
        @JsonTypeInfo(include=As.PROPERTY, property="type", use=Id.NAME, defaultImpl=Sub1.class)
        @JsonSubTypes({@Type(name="sub1", value=Sub1.class), @Type(name="sub2", value=Sub2.class), @Type(name="sub3", value=Sub3.class)})
        static abstract class BaseHigh {}
        static abstract class BaseLow extends BaseHigh {}
        static class Sub1 extends BaseLow {}
        static class Sub2 extends BaseLow {}
        static class Sub3 extends BaseHigh {}
    }

    /**
     * Multiple levels of inheritance. 2 Base classes 3 Subs. Annotations on the lower base class
     */
    static class Scenario4 {
        static abstract class BaseHigh {}
        @JsonTypeInfo(include=As.PROPERTY, property="type", use=Id.NAME, defaultImpl=Sub1.class)
        @JsonSubTypes({@Type(name="sub1", value=Sub1.class), @Type(name="sub2", value=Sub2.class)})
        static abstract class BaseLow extends BaseHigh {}
        static class Sub1 extends BaseLow {}
        static class Sub2 extends BaseLow {}
        static class Sub3 extends BaseHigh {}
    }
}
