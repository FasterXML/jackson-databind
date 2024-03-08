package com.fasterxml.jackson.databind.deser.enums;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;
import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.q;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class EnumDefaultRead4403Test
{
    // [databind#4403]
    enum Brand4403 {
        @JsonProperty("005")
        SEAT,
        @JsonProperty("006")
        HYUNDAI,
        @JsonEnumDefaultValue
        OTHER
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#4403]
    @Test
    public void readFromDefault4403() throws Exception
    {
        ObjectReader r = MAPPER.readerFor(Brand4403.class)
                .with(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);
        assertEquals(Brand4403.SEAT, r.readValue(q("005")));
        assertEquals(Brand4403.HYUNDAI, r.readValue(q("006")));
        assertEquals(Brand4403.OTHER, r.readValue(q("x")));

        // Problem here: "001" taken as "Stringified" index 1
        assertEquals(Brand4403.OTHER, r.readValue(q("001")));
    }

}
