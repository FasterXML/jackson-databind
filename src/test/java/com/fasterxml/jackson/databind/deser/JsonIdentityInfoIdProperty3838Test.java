package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

// [databind#3838]: Difference in the handling of ObjectId-property in JsonIdentityInfo depending
// on the deserialization route.
public class JsonIdentityInfoIdProperty3838Test extends BaseMapTest {

    /*
    /**********************************************************
    /* Set Up
    /**********************************************************
    */

    @JsonIdentityInfo(property = "id", generator = ObjectIdGenerators.PropertyGenerator.class)
    static class SetterBased {
        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    @JsonIdentityInfo(property = "id", generator = ObjectIdGenerators.PropertyGenerator.class)
    static class CreatorBased {
        private final String id;

        @JsonCreator
        CreatorBased(@JsonProperty(value = "id") String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    /*
    /**********************************************************
    /* Test
    /**********************************************************
    */

    private final ObjectMapper MAPPER = newJsonMapper();

    public void testUniformHandlingForMissingObjectId() throws Exception {
        Class<?>[] classes = {SetterBased.class, CreatorBased.class};
        for (Class<?> cls : classes) {
            try {
                MAPPER.readValue("{}", cls);
                fail("should not pass");
            } catch (MismatchedInputException e) {
                verifyException(e,
                    "No Object Id found for an instance of", "to",
                    "to assign to property 'id'");
            }
        }
    }
}
