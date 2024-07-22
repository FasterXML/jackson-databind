package com.fasterxml.jackson.databind.records;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#562] Allow @JsonAnySetter on Creator constructors
public class RecordCreatorWithAnySetter562Test extends DatabindTestUtil
{
    record RecordWithAnySetterCtorParamCanonCtor(int id, Map<String, Integer> additionalProperties) {
        @JsonCreator
        public RecordWithAnySetterCtorParamCanonCtor(@JsonProperty("regular") int id,
                                                     @JsonAnySetter Map<String, Integer> additionalProperties) {
            this.id = id;
            this.additionalProperties = additionalProperties;
        }
    }

    record RecordWithAnySetterComponentCanonCtor(int id, @JsonAnySetter Map<String, Object> any) {
    }

    // Workaround mentioned in [databind#3439]
    record RecordWorkaroundWithAnySetterMethodWithAltCtor(int id, Map<String, Object> any) {
        @JsonCreator
        public RecordWorkaroundWithAnySetterMethodWithAltCtor(@JsonProperty("id") int id) {
            this(id, new LinkedHashMap<>());
        }

        @JsonAnySetter
        private void updateProperty(String name, Object value) {
            any.put(name, value);
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testRecordWithAnySetterCtor() throws Exception
    {
        // First, only regular property mapped
        RecordWithAnySetterCtorParamCanonCtor result = MAPPER.readValue(a2q("{'regular':13}"),
                RecordWithAnySetterCtorParamCanonCtor.class);
        assertEquals(13, result.id);
        assertEquals(0, result.additionalProperties.size());

        // Then with unknown properties
        result = MAPPER.readValue(a2q("{'regular':13, 'unknown':99, 'extra':-1}"),
                RecordWithAnySetterCtorParamCanonCtor.class);
        assertEquals(13, result.id);
        assertEquals(Integer.valueOf(99), result.additionalProperties.get("unknown"));
        assertEquals(Integer.valueOf(-1), result.additionalProperties.get("extra"));
        assertEquals(2, result.additionalProperties.size());
    }

    @Test
    public void testDeserialize_WithAnySetterComponent_WithCanonicalConstructor() throws Exception
    {
        RecordWithAnySetterComponentCanonCtor value = MAPPER.readValue(
                a2q("{'id':123,'any1':'Any 1','any2':'Any 2'}"), RecordWithAnySetterComponentCanonCtor.class);

        Map<String, Object> expectedAny = new LinkedHashMap<>();
        expectedAny.put("any1", "Any 1");
        expectedAny.put("any2", "Any 2");
        assertEquals(new RecordWithAnySetterComponentCanonCtor(123, expectedAny), value);
    }

    @Test
    public void testDeserializeWorkaround_WithAnySetterMethod_WithAltConstructor() throws Exception
    {
        RecordWorkaroundWithAnySetterMethodWithAltCtor value = MAPPER.readValue(
                a2q("{'id':123,'any1':'Any 1','any2':'Any 2'}"), RecordWorkaroundWithAnySetterMethodWithAltCtor.class);

        Map<String, Object> expectedAny = new LinkedHashMap<>();
        expectedAny.put("any1", "Any 1");
        expectedAny.put("any2", "Any 2");
        assertEquals(new RecordWorkaroundWithAnySetterMethodWithAltCtor(123, expectedAny), value);
    }
}
