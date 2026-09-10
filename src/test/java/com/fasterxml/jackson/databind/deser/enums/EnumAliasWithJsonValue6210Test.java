package com.fasterxml.jackson.databind.deser.enums;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonValue;

import com.fasterxml.jackson.databind.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;
import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.q;

// [databind#6210]: `@JsonAlias` ignored on Enum values if `@JsonValue` is present
public class EnumAliasWithJsonValue6210Test
{
    enum Versus6210 {
        @JsonAlias({ "A", "B" })
        BUY("B"),
        @JsonAlias({ "V", "S" })
        SELL("S");

        private final String value;

        Versus6210(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }
    }

    enum IntValued6210 {
        @JsonAlias({ "one" })
        FIRST(1),
        @JsonAlias({ "two", "second" })
        SECOND(2);

        private final int value;

        IntValued6210(int value) {
            this.value = value;
        }

        @JsonValue
        public int getValue() {
            return value;
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#6210]
    @Test
    public void testAliasesWithJsonValue() throws Exception {
        ObjectReader r = MAPPER.readerFor(Versus6210.class);

        // First: values from `@JsonValue` accessor still work
        assertEquals(Versus6210.BUY, r.readValue(q("B")));
        assertEquals(Versus6210.SELL, r.readValue(q("S")));

        // and then aliases, which used to fail
        assertEquals(Versus6210.BUY, r.readValue(q("A")));
        assertEquals(Versus6210.SELL, r.readValue(q("V")));
    }

    // [databind#6210]: aliases must not override `@JsonValue`-provided ids
    @Test
    public void testJsonValueWinsOverAlias() throws Exception {
        // "B" is both `@JsonValue` of BUY and an alias of BUY itself, "S"
        // similarly for SELL: primary mapping must be retained
        assertEquals(Versus6210.BUY, MAPPER.readValue(q("B"), Versus6210.class));
        assertEquals(Versus6210.SELL, MAPPER.readValue(q("S"), Versus6210.class));
    }

    // [databind#6210]: aliases are Strings even if `@JsonValue` is of int type
    @Test
    public void testAliasesWithIntJsonValue() throws Exception {
        ObjectReader r = MAPPER.readerFor(IntValued6210.class);

        assertEquals(IntValued6210.FIRST, r.readValue("1"));
        assertEquals(IntValued6210.SECOND, r.readValue("2"));

        assertEquals(IntValued6210.FIRST, r.readValue(q("one")));
        assertEquals(IntValued6210.SECOND, r.readValue(q("two")));
        assertEquals(IntValued6210.SECOND, r.readValue(q("second")));
    }

    // [databind#6210]: also verify Enum-as-Map-key path
    @Test
    public void testAliasesWithJsonValueAsMapKey() throws Exception {
        java.util.Map<Versus6210, String> map = MAPPER.readValue(
                "{\"V\":\"x\", \"B\":\"y\"}",
                MAPPER.getTypeFactory().constructMapType(java.util.LinkedHashMap.class,
                        Versus6210.class, String.class));
        assertEquals(2, map.size());
        assertEquals("x", map.get(Versus6210.SELL));
        assertEquals("y", map.get(Versus6210.BUY));
    }
}
