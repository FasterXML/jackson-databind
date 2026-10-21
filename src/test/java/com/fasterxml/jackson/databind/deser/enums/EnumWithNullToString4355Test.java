package com.fasterxml.jackson.databind.deser.enums;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EnumWithNullToString4355Test extends DatabindTestUtil
{
    // [databind#4355]
    enum Enum4355 {
        ALPHA("A"),
        BETA("B"),
        UNDEFINED(null);

        private final String s;

        Enum4355(String s) {
            this.s = s;
        }

        @Override
        public String toString() {
            return s;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#4355]
    @Test
    public void testWithNullToString() throws Exception
    {
        assertEquals(q("ALPHA"), MAPPER.writeValueAsString(Enum4355.ALPHA));
        assertEquals(q("BETA"), MAPPER.writeValueAsString(Enum4355.BETA));
        assertEquals(q("UNDEFINED"), MAPPER.writeValueAsString(Enum4355.UNDEFINED));
    }

    // [databind#6309]: same with WRITE_ENUMS_USING_TO_STRING, also as Map keys
    @Test
    public void testWithNullToStringUsingToString() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
                .build();
        assertEquals("[\"A\",\"\"]", mapper.writeValueAsString(
                new Enum4355[] { Enum4355.ALPHA, Enum4355.UNDEFINED }));

        Map<Enum4355, Integer> enumMap = new EnumMap<>(Enum4355.class);
        enumMap.put(Enum4355.UNDEFINED, 1);
        assertEquals(a2q("{'':1}"), mapper.writeValueAsString(enumMap));

        Map<Enum4355, Integer> map = new HashMap<>();
        map.put(Enum4355.UNDEFINED, 1);
        assertEquals(a2q("{'':1}"), mapper.writeValueAsString(map));
    }
}
