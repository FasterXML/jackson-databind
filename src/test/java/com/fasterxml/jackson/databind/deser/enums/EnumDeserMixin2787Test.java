package com.fasterxml.jackson.databind.deser.enums;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

public class EnumDeserMixin2787Test extends BaseMapTest {

    static enum Enum2787 {
        ITEM_A,

        @JsonAlias({"B_ORIGIN_ALIAS_1", "B_ORIGIN_ALIAS_2"})
        @JsonProperty("B_ORIGIN_PROP")
        ITEM_B,

        @JsonAlias({"C_ORIGIN_ALIAS"})
        @JsonProperty("C_ORIGIN_PROP")
        ITEM_C,

        ITEM_ORIGIN
    }

    static enum EnumMixin2787 {
        ITEM_A,

        @JsonProperty("B_MIXIN_PROP")
        ITEM_B,

        @JsonAlias({"C_MIXIN_ALIAS_1", "C_MIXIN_ALIAS_2"})
        @JsonProperty("C_MIXIN_PROP")
        ITEM_C,

        ITEM_MIXIN;

        @Override
        public String toString() {
            return "SHOULD NOT USE WITH TO STRING";
        }
    }

    static class EnumWrapper {
        public Enum2787 value;
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    protected final ObjectMapper MAPPER = jsonMapperBuilder().build();

    public void testEnumDeserSuccess() throws Exception {
        ObjectMapper mapper = MAPPER.addMixIn(Enum2787.class, EnumMixin2787.class);

        Enum2787 result = mapper.readValue(q("B_MIXIN_PROP"), Enum2787.class);

        assertEquals(Enum2787.ITEM_B, result);
    }

    public void testEnumDeserSuccessCaseInsensitive() throws Exception {
        ObjectMapper mapper = MAPPER.addMixIn(Enum2787.class, EnumMixin2787.class)
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);

        Enum2787 result = mapper.readValue(q("B_mIxIn_pRoP"), Enum2787.class);
        assertEquals(Enum2787.ITEM_B, result);
    }

    public void testEnumDeserSuccessMissingFromMixIn() throws Exception {
        ObjectMapper mapper = MAPPER.addMixIn(Enum2787.class, EnumMixin2787.class);

        Enum2787 result = mapper.readValue(q("ITEM_ORIGIN"), Enum2787.class);

        assertEquals(Enum2787.ITEM_ORIGIN, result);
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
            mapper.readValue(q("B_ORIGIN_PROP"), Enum2787.class);
            fail("should not reach");
        } catch (InvalidFormatException e) {
            verifyException(e, "B_ORIGIN_PROP", "not one of the values accepted for Enum class");
        }
    }

    public void testMixInItselfNonJsonProperty() throws Exception {
        ObjectMapper mapper = MAPPER.addMixIn(Enum2787.class, EnumMixin2787.class);

        EnumMixin2787 result = mapper.readValue(q("ITEM_MIXIN"), EnumMixin2787.class);

        assertEquals(EnumMixin2787.ITEM_MIXIN, result);
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
            verifyException(e, "should-not-exist", "not one of the values accepted for Enum class");
        }
    }

}
