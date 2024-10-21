package com.fasterxml.jackson.databind.deser.enums;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

import com.fasterxml.jackson.databind.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;
import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.q;

class EnumAliasDeser2352Test
{
    // for [databind#2352]: Support aliases on enum values
    enum MyEnum2352_1 {
        A,
        @JsonAlias({"singleAlias"})
        B,
        @JsonAlias({"multipleAliases1", "multipleAliases2"})
        C
    }

    // for [databind#2352]: Support aliases on enum values
    enum MyEnum2352_2 {
        A,
        @JsonAlias({"singleAlias"})
        B,
        @JsonAlias({"multipleAliases1", "multipleAliases2"})
        C;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    // for [databind#2352]: Support aliases on enum values
    enum MyEnum2352_3 {
        A,
        @JsonEnumDefaultValue
        @JsonAlias({"singleAlias"})
        B,
        @JsonAlias({"multipleAliases1", "multipleAliases2"})
        C
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    protected final ObjectMapper MAPPER = newJsonMapper();

    // for [databind#2352]
    @Test
    void enumWithAlias() throws Exception {
        ObjectReader reader = MAPPER.readerFor(MyEnum2352_1.class);
        MyEnum2352_1 nonAliased = reader.readValue(q("A"));
        assertEquals(MyEnum2352_1.A, nonAliased);
        MyEnum2352_1 singleAlias = reader.readValue(q("singleAlias"));
        assertEquals(MyEnum2352_1.B, singleAlias);
        MyEnum2352_1 multipleAliases1 = reader.readValue(q("multipleAliases1"));
        assertEquals(MyEnum2352_1.C, multipleAliases1);
        MyEnum2352_1 multipleAliases2 = reader.readValue(q("multipleAliases2"));
        assertEquals(MyEnum2352_1.C, multipleAliases2);
    }

    // for [databind#2352]
    @Test
    void enumWithAliasAndToStringSupported() throws Exception {
        ObjectReader reader = MAPPER.readerFor(MyEnum2352_2.class)
                .with(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
        MyEnum2352_2 nonAliased = reader.readValue(q("a"));
        assertEquals(MyEnum2352_2.A, nonAliased);
        MyEnum2352_2 singleAlias = reader.readValue(q("singleAlias"));
        assertEquals(MyEnum2352_2.B, singleAlias);
        MyEnum2352_2 multipleAliases1 = reader.readValue(q("multipleAliases1"));
        assertEquals(MyEnum2352_2.C, multipleAliases1);
        MyEnum2352_2 multipleAliases2 = reader.readValue(q("multipleAliases2"));
        assertEquals(MyEnum2352_2.C, multipleAliases2);
    }

    // for [databind#2352]
    @Test
    void enumWithAliasAndDefaultForUnknownValueEnabled() throws Exception {
        ObjectReader reader = MAPPER.readerFor(MyEnum2352_3.class)
                .with(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);
        MyEnum2352_3 nonAliased = reader.readValue(q("A"));
        assertEquals(MyEnum2352_3.A, nonAliased);
        MyEnum2352_3 singleAlias = reader.readValue(q("singleAlias"));
        assertEquals(MyEnum2352_3.B, singleAlias);
        MyEnum2352_3 defaulted = reader.readValue(q("unknownValue"));
        assertEquals(MyEnum2352_3.B, defaulted);
        MyEnum2352_3 multipleAliases1 = reader.readValue(q("multipleAliases1"));
        assertEquals(MyEnum2352_3.C, multipleAliases1);
        MyEnum2352_3 multipleAliases2 = reader.readValue(q("multipleAliases2"));
        assertEquals(MyEnum2352_3.C, multipleAliases2);
    }
}
