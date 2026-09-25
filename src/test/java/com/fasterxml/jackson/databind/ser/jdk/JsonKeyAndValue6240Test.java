package com.fasterxml.jackson.databind.ser.jdk;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonKey;
import com.fasterxml.jackson.annotation.JsonValue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonKeyAndValue6240Test extends DatabindTestUtil
{
    static class KeyAndValue {
        @JsonKey
        @JsonValue
        public String getId() {
            return "a";
        }
    }

    static class KeyOnly {
        public String name = "value";

        @JsonKey
        @JsonValue(false)
        public String getId() {
            return "a";
        }
    }

    static class ValueOnly {
        @JsonKey(false)
        @JsonValue
        public String getId() {
            return "a";
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    void serializeAsValue() throws Exception {
        assertEquals(q("a"), MAPPER.writeValueAsString(new KeyAndValue()));
    }

    @Test
    void serializeAsMapKey() throws Exception {
        assertEquals(a2q("{'a':1}"),
                MAPPER.writeValueAsString(Collections.singletonMap(new KeyAndValue(), 1)));
    }

    @Test
    void serializeAsMapValue() throws Exception {
        assertEquals(a2q("{'key':'a'}"),
                MAPPER.writeValueAsString(Collections.singletonMap("key", new KeyAndValue())));
    }

    @Test
    void disabledJsonValueDoesNotExposeKeyGetterAsProperty() throws Exception {
        assertEquals(a2q("{'name':'value'}"), MAPPER.writeValueAsString(new KeyOnly()));
        assertEquals(a2q("{'a':1}"),
                MAPPER.writeValueAsString(Collections.singletonMap(new KeyOnly(), 1)));
    }

    @Test
    void disabledJsonKeyDoesNotDisableJsonValue() throws Exception {
        assertEquals(q("a"), MAPPER.writeValueAsString(new ValueOnly()));
    }
}
