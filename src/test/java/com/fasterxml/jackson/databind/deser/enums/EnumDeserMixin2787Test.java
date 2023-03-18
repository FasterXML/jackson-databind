package com.fasterxml.jackson.databind.deser.enums;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

public class EnumDeserMixin2787Test extends BaseMapTest {

    static enum Enum2787 {
        APPLE,
        @JsonAlias({"tax10-alias-A", "tax10-alias-B"})
        @JsonProperty("orig-tax10")
        tax10,
        @JsonAlias({"overridden-alias"})
        @JsonProperty("overrideen-property")
        tax20,
        taxOrig
    }

    static enum EnumMixin2787 {
        APPLE,
        @JsonProperty("mixIn-tax10")
        tax10,
        @JsonAlias({"tax20-aliasA", "tax20-aliasB"})
        @JsonProperty("PytPyt")
        tax20,
        tax30;

        @Override
        public String toString() {
            return "-----------------------------";
        }
    }

    static enum EnumSecond {
        @JsonAlias({"second-alias"})
        SECOND
    }

    static enum EnumSecondMixIn {
        @JsonAlias({"second-alias"})
        @JsonProperty("second-prop")
        SECOND
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    protected final ObjectMapper MAPPER = jsonMapperBuilder().build();

    public void testEnumDeserSuccess() throws Exception {
        ObjectMapper mapper = MAPPER.addMixIn(Enum2787.class, EnumMixin2787.class);

        Enum2787 result = mapper.readValue(q("mixIn-tax10"), Enum2787.class);

        assertEquals(Enum2787.tax10, result);
    }

    public void testEnumDeserSuccessCaseInsensitive() throws Exception {
        ObjectMapper mapper = MAPPER.addMixIn(Enum2787.class, EnumMixin2787.class)
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);

        Enum2787 result = mapper.readValue(q("MiXiN-tAx10"), Enum2787.class);
        assertEquals(Enum2787.tax10, result);
    }

    public void testEnumMixInOverloaded() throws JsonProcessingException {
        ObjectMapper mapper = MAPPER.addMixIn(EnumSecond.class, EnumSecondMixIn.class);

        EnumSecond resultA = mapper.readValue(q("second-alias"), EnumSecond.class);
        assertEquals(EnumSecond.SECOND, resultA);

        EnumSecond resultB = mapper.readValue(q("second-prop"), EnumSecond.class);
        assertEquals(EnumSecond.SECOND, resultB);
    }

    public void testEnumDeserSuccessMissingFromMixIn() throws Exception {
        ObjectMapper mapper = MAPPER.addMixIn(Enum2787.class, EnumMixin2787.class);

        Enum2787 result = mapper.readValue(q("taxOrig"), Enum2787.class);

        assertEquals(Enum2787.taxOrig, result);
    }

    public void testEnumDeserMixinFail() throws Exception {
        ObjectMapper mapper = MAPPER.addMixIn(Enum2787.class, EnumMixin2787.class);

        // fail for Bean property name
        try {
            mapper.readValue(q("tax10"), Enum2787.class);
            fail("should not reach");
        } catch (InvalidFormatException e) {
            verifyException(e, "tax10", "not one of the values accepted for Enum class");
        }

        // fail for Bean's JsonProperty because overridden
        try {
            mapper.readValue(q("orig-tax10"), Enum2787.class);
            fail("should not reach");
        } catch (InvalidFormatException e) {
            verifyException(e, "orig-tax10", "not one of the values accepted for Enum class");
        }
    }

    public void testMixInItselfNonJsonProperty() throws Exception {
        ObjectMapper mapper = MAPPER.addMixIn(Enum2787.class, EnumMixin2787.class);

        EnumMixin2787 result = mapper.readValue(q("tax30"), EnumMixin2787.class);

        assertEquals(EnumMixin2787.tax30, result);
    }

    public void testMixInValueForTargetClass() throws Exception {
        ObjectMapper mapper = MAPPER.addMixIn(Enum2787.class, EnumMixin2787.class);

        try {
            mapper.readValue(q("tax30"), Enum2787.class);
        } catch (InvalidFormatException e) {
            verifyException(e, " String \"tax30\": not one of the values accepted for Enum class:");
        }
    }

    public void testMixinOnEnumValuesThrowWhenUnknown() throws Exception {
        ObjectMapper mapper = MAPPER.addMixIn(Enum2787.class, EnumMixin2787.class);

        try {
            mapper.readValue(q("should-not-exist"), Enum2787.class);
        } catch (InvalidFormatException e) {
            verifyException(e, "should-not-exist","not one of the values accepted for Enum class");
        }
    }
}
