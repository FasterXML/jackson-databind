package com.fasterxml.jackson.databind.records;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#5923] Record with @JsonCreator(mode=PROPERTIES) factory + @JsonValue
//   fails to deserialize
public class RecordJsonCreatorAndJsonValue5923Test extends DatabindTestUtil
{
    public record Inner(@JsonProperty(required = true, value = "innerValue") boolean innerValue) {}

    public record Outer(Inner bools) {
        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public static Outer fromJson(@JsonProperty(required = true, value = "renamed") boolean booleanValue) {
            return new Outer(new Inner(booleanValue));
        }

        @JsonValue
        public Map<String, Boolean> toJson() {
            return Map.of("renamed", bools.innerValue());
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testDeserializationTrue() throws Exception {
        Outer bw = MAPPER.readValue("{ \"renamed\": true }", Outer.class);
        assertEquals(true, bw.bools.innerValue);
    }

    @Test
    public void testDeserializationFalse() throws Exception {
        Outer bw = MAPPER.readValue("{ \"renamed\": false }", Outer.class);
        assertEquals(false, bw.bools.innerValue);
    }

    @Test
    public void testSerializationViaJsonValue() throws Exception {
        assertEquals("{\"renamed\":true}",
                MAPPER.writeValueAsString(new Outer(new Inner(true))));
        assertEquals("{\"renamed\":false}",
                MAPPER.writeValueAsString(new Outer(new Inner(false))));
    }

    @Test
    public void testRoundTrip() throws Exception {
        Outer original = new Outer(new Inner(true));
        Outer roundTripped = MAPPER.readValue(MAPPER.writeValueAsString(original), Outer.class);
        assertEquals(original.bools.innerValue, roundTripped.bools.innerValue);
    }
}
