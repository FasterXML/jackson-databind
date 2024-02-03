package com.fasterxml.jackson.databind.deser.enums;

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
}
