package com.fasterxml.jackson.databind.records;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

public class RecordCreatorsTest extends BaseMapTest
{
    record RecordWithCanonicalCtorOverride(int id, String name) {
        public RecordWithCanonicalCtorOverride(int id, String name) {
            this.id = id;
            this.name = "name";
        }
    }

    record RecordWithAltCtor(int id, String name) {
        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public RecordWithAltCtor(@JsonProperty("id") int id) {
            this(id, "name2");
        }
    }

    // [databind#2980]
    record RecordWithDelegation(String value) {
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public RecordWithDelegation(String value) {
            this.value = "del:"+value;
        }

        @JsonValue()
        public String getValue() {
            return "val:"+value;
        }

        public String accessValueForTest() { return value; }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    /*
    /**********************************************************************
    /* Test methods, alternate constructors
    /**********************************************************************
     */

    public void testDeserializeWithCanonicalCtorOverride() throws Exception {
        RecordWithCanonicalCtorOverride value = MAPPER.readValue("{\"id\":123,\"name\":\"Bob\"}",
                RecordWithCanonicalCtorOverride.class);
        assertEquals(123, value.id());
        assertEquals("name", value.name());
    }

    public void testDeserializeWithAltCtor() throws Exception {
        RecordWithAltCtor value = MAPPER.readValue("{\"id\":2812}",
                RecordWithAltCtor.class);
        assertEquals(2812, value.id());
        assertEquals("name2", value.name());

        // "Implicit" canonical constructor can no longer be used when there's explicit constructor
        try {
            MAPPER.readValue("{\"id\":2812,\"name\":\"Bob\"}",
                    RecordWithAltCtor.class);
            fail("should not pass");
        } catch (UnrecognizedPropertyException e) {
            verifyException(e, "Unrecognized");
            verifyException(e, "\"name\"");
            verifyException(e, "RecordWithAltCtor");
        }
    }

    // [databind#2980]
    public void testDeserializeWithDelegatingCtor() throws Exception {
        RecordWithDelegation value = MAPPER.readValue(q("foobar"),
                RecordWithDelegation.class);
        assertEquals("del:foobar", value.accessValueForTest());

        assertEquals(q("val:del:foobar"), MAPPER.writeValueAsString(value));
    }
}
