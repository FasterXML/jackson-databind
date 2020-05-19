package com.fasterxml.jackson.failing;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EnumAsMapKey2725Test extends BaseMapTest
{
    // [databind#2725]
    enum TestEnum2725 {
        FOO(1);

        private final int i;

        TestEnum2725(final int i) {
            this.i = i;
        }

        @JsonValue
        public int getI() {
            return i;
        }

        @JsonCreator
        public static TestEnum2725 getByIntegerId(final Integer id) {
            return id == FOO.i ? FOO : null;
        }

        @JsonCreator
        public static TestEnum2725 getByStringId(final String id) {
            return Integer.parseInt(id) == FOO.i ? FOO : null;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#2725]
    public void testEnumAsMapKey2725() throws Exception
    {
        final Map<TestEnum2725, String> input = new HashMap<>();
        input.put(TestEnum2725.FOO, "Hello");

        final String json = MAPPER.writeValueAsString(input);

        final Map<TestEnum2725, String> output = MAPPER.readValue(json,
                new TypeReference<Map<TestEnum2725, String>>() { });

        assertNotNull(output);
        assertEquals(1, output.size());
    }
}
