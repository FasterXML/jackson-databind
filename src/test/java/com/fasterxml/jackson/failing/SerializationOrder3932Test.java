package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for verifying that constraints on ordering of serialized
 * properties are held. This is a regression test for [databind#3932].
 */
public class SerializationOrder3932Test
    extends DatabindTestUtil
{
    static class NestedClassOne {
        private String id;
        private String name;

        @JsonProperty(value = "nestedProperty")
        private NestedClassTwo nestedClassTwo;

        NestedClassOne(String id,
                       String name,
                       @JsonProperty(value = "nestedProperty") NestedClassTwo nestedClassTwo) {
            this.id = id;
            this.name = name;
            this.nestedClassTwo = nestedClassTwo;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public NestedClassTwo getNestedClassTwo() {
            return nestedClassTwo;
        }

        public void setNestedClassTwo(NestedClassTwo nestedClassTwo) {
            this.nestedClassTwo = nestedClassTwo;
        }
    }

    static class NestedClassOneWithDuplicatedJsonPropertyAnnotations {
        private String id;
        private String name;

        @JsonProperty(value = "nestedProperty")
        private NestedClassTwo nestedClassTwo;

        NestedClassOneWithDuplicatedJsonPropertyAnnotations(String id,
                       String name,
                       @JsonProperty(value = "nestedProperty") NestedClassTwo nestedClassTwo) {
            this.id = id;
            this.name = name;
            this.nestedClassTwo = nestedClassTwo;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public NestedClassTwo getNestedClassTwo() {
            return nestedClassTwo;
        }

        public void setNestedClassTwo(NestedClassTwo nestedClassTwo) {
            this.nestedClassTwo = nestedClassTwo;
        }
    }

    static class NestedClassTwo {

        private String id;
        private String passport;

        NestedClassTwo(String id,
                       String passport) {
            this.id = id;
            this.passport = passport;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getPassport() {
            return passport;
        }

        public void setPassport(String passport) {
            this.passport = passport;
        }
    }

    /*
    /*********************************************
    /* Unit tests
    /*********************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testSerializationOrderWithJsonProperty() throws Exception {
        NestedClassTwo nestedTwo = new NestedClassTwo("2", "111110");
        NestedClassOne nestedOne = new NestedClassOne("1", "test@records.com", nestedTwo);
        final String output = MAPPER.writeValueAsString(nestedOne);
        final String expected = a2q(
                "{'id':'1','email':'test@records.com','nestedProperty':{'id':'2','passport':'111110'}}");
        assertEquals(expected, output);
    }

    @Test
    public void testSerializationOrderWithDuplicatedJsonProperty() throws Exception {
        NestedClassTwo nestedTwo = new NestedClassTwo("2", "111110");
        NestedClassOneWithDuplicatedJsonPropertyAnnotations nestedOne = new NestedClassOneWithDuplicatedJsonPropertyAnnotations(
            "1", "test@records.com", nestedTwo);
        final String output = MAPPER.writeValueAsString(nestedOne);
        final String expected = a2q(
                "{'id':'1','email':'test@records.com','nestedProperty':{'id':'2','passport':'111110'}}");
        assertEquals(expected, output);
    }
}
