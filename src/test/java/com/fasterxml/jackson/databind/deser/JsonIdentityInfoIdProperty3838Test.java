package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

// [databind#3838]: Difference in the handling of ObjectId-property in JsonIdentityInfo depending on the deserialization route.
public class JsonIdentityInfoIdProperty3838Test extends BaseMapTest {

    /*
    /**********************************************************
    /* Set Up
    /**********************************************************
    */
    interface ResultGetter {
        String result();
    }

    @JsonIdentityInfo(property = "id", generator = ObjectIdGenerators.PropertyGenerator.class)
    static class SetterBased implements ResultGetter {
        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @Override
        public String result() {
            return id;
        }
    }

    @JsonIdentityInfo(property = "id", generator = ObjectIdGenerators.PropertyGenerator.class)
    static class CreatorBased implements ResultGetter {
        private final String id;

        @JsonCreator
        CreatorBased(@JsonProperty(value = "id") String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        @Override
        public String result() {
            return id;
        }
    }

    @JsonIdentityInfo(property = "id", generator = ObjectIdGenerators.PropertyGenerator.class)
    static class DefaultConstructorBased implements ResultGetter {
        public String id;

        @Override
        public String result() {
            return id;
        }
    }

    @JsonIdentityInfo(property = "id", generator = ObjectIdGenerators.PropertyGenerator.class)
    static class StaticFactoryMethodBased implements ResultGetter {
        private final String id;

        private StaticFactoryMethodBased(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        @JsonCreator
        public static StaticFactoryMethodBased create(@JsonProperty("id") String id) {
            return new StaticFactoryMethodBased(id);
        }

        @Override
        public String result() {
            return id;
        }
    }

    @JsonIdentityInfo(property = "id", generator = ObjectIdGenerators.PropertyGenerator.class)
    static class MultiArgConstructorBased implements ResultGetter {
        private final String id;
        private final int value;

        @JsonCreator
        MultiArgConstructorBased(@JsonProperty("id") String id, @JsonProperty("value") int value) {
            this.id = id;
            this.value = value;
        }

        public String getId() {
            return id;
        }

        public int getValue() {
            return value;
        }

        @Override
        public String result() {
            return id;
        }
    }

    /*
    /**********************************************************
    /* Test
    /**********************************************************
    */

    private final ObjectMapper MAPPER = newJsonMapper();

    final static Object[][] CLASS_AND_JSON_STRING = new Object[][]{
        {SetterBased.class, "{'id':'great'}"},
        {CreatorBased.class, "{'id':'great'}"},
        {DefaultConstructorBased.class, "{'id':'great'}"},
        {StaticFactoryMethodBased.class, "{'id':'great'}"},
        {MultiArgConstructorBased.class, "{'id':'great','value':42}"}
    };

    public void testUniformHandlingForMissingObjectId() throws Exception {
        for (Object[] classAndJsonStrEntry : CLASS_AND_JSON_STRING) {
            final Class<?> cls = (Class<?>) classAndJsonStrEntry[0];
            final String jsonStr = (String) classAndJsonStrEntry[1];

            // 1. throws MismatchedInputException with empty JSON object
            try {
                MAPPER.readValue("{}", cls);
                fail("should not pass");
            } catch (MismatchedInputException e) {
                verifyException(e,
                    "No Object Id found for an instance of", "to",
                    "to assign to property 'id'");
            }

            // 2. but works with non-empty JSON object
            ResultGetter resultGetter = (ResultGetter) MAPPER.readValue(a2q(jsonStr), cls);
            assertEquals("great", resultGetter.result());
        }
    }
}
