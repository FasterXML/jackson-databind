package com.fasterxml.jackson.databind.deser.enums;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonValue;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EnumDeserializerJsonValue5271Test extends DatabindTestUtil
{
    enum Enum5271 {
        T10("10%"), T20("20%"), T30("30%");

        private final String code;

        Enum5271(String code) {
            this.code = code;
        }

        @JsonValue
        public String getCode() {
            return code;
        }
    }

    private final ObjectReader ENUM_READER = newJsonMapper().readerFor(Enum5271.class);

    // [databind#5271]
    @Test
    void convertStringToEnum() throws Exception {
        _testConvert(ENUM_READER.without(DeserializationFeature.READ_ENUMS_USING_TO_STRING));
        _testConvert(ENUM_READER.with(DeserializationFeature.READ_ENUMS_USING_TO_STRING));
    }

    private void _testConvert(ObjectReader reader) throws Exception {
        assertEquals(Enum5271.T20, reader.readValue(q("20%")));
    }
}
