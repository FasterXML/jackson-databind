package com.fasterxml.jackson.databind.deser.enums;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.*;

public class EnumDeserMixin2787Test {

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

    static class Bean2787 {
        public String x;
    }

    static class BeanMixin2787 {
        @JsonProperty("x_mixin")
        public String x;
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    protected final ObjectMapper MAPPER = jsonMapperBuilder().build();

    @Test
    public void testEnumDeserSuccess() throws Exception {
        ObjectMapper mapper = mapperWithMixIn(Enum2787.class, EnumMixin2787.class);

        Enum2787 result = mapper.readValue(q("B_MIXIN_PROP"), Enum2787.class);

        assertEquals(Enum2787.ITEM_B, result);
    }

    @Test
    public void testEnumMixinRoundTripSerDeser() throws Exception {
        // ser -> deser
        ObjectMapper mapper = MAPPER.addMixIn(Enum2787.class, EnumMixin2787.class);
        // from
        String result = mapper.writeValueAsString(Enum2787.ITEM_B);
        assertEquals(q("B_MIXIN_PROP"), result);
        // to
        Enum2787 result2 = mapper.readValue(result, Enum2787.class);
        assertEquals(Enum2787.ITEM_B, result2);
    }

    @Test
    public void testEnumMixinRoundTripDeserSer() throws Exception {
        // deser -> ser
        ObjectMapper mapper = MAPPER.addMixIn(Enum2787.class, EnumMixin2787.class);
        // from
        Enum2787 result = mapper.readValue(q("B_MIXIN_PROP"), Enum2787.class);
        assertEquals(Enum2787.ITEM_B, result);
        // to
        String value = mapper.writeValueAsString(result);
        assertEquals(q("B_MIXIN_PROP"), value);
    }

    @Test
    public void testBeanMixin() throws Exception {
        ObjectMapper mapper = MAPPER.addMixIn(Bean2787.class, BeanMixin2787.class);

        Bean2787 result = mapper.readValue(a2q("{'x_mixin': 'value'}"), Bean2787.class);

        assertEquals("value", result.x);
    }

    @Test
    public void testEnumDeserSuccessCaseInsensitive() throws Exception {
        ObjectMapper mapper = builderWithMixIn(Enum2787.class, EnumMixin2787.class)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .build();

            Enum2787 result = mapper.readValue(q("B_mIxIn_pRoP"), Enum2787.class);
            assertEquals(Enum2787.ITEM_B, result);
    }

    @Test
    public void testEnumDeserSuccessMissingFromMixIn() throws Exception {
        ObjectMapper mapper = MAPPER.addMixIn(Enum2787.class, EnumMixin2787.class);

        Enum2787 result = mapper.readValue(q("ITEM_ORIGIN"), Enum2787.class);

        assertEquals(Enum2787.ITEM_ORIGIN, result);
    }

    @Test
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

    @Test
    public void testMixInItselfNonJsonProperty() throws Exception {
        ObjectMapper mapper = MAPPER.addMixIn(Enum2787.class, EnumMixin2787.class);

        EnumMixin2787 result = mapper.readValue(q("ITEM_MIXIN"), EnumMixin2787.class);

        assertEquals(EnumMixin2787.ITEM_MIXIN, result);
    }

    @Test
    public void testMixInValueForTargetClass() throws Exception {
        ObjectMapper mapper = MAPPER.addMixIn(Enum2787.class, EnumMixin2787.class);

        try {
            mapper.readValue(q("tax30"), Enum2787.class);
        } catch (InvalidFormatException e) {
            verifyException(e, " String \"tax30\": not one of the values accepted for Enum class:");
        }
    }

    @Test
    public void testMixinOnEnumValuesThrowWhenUnknown() throws Exception {
        ObjectMapper mapper = MAPPER.addMixIn(Enum2787.class, EnumMixin2787.class);

        try {
            mapper.readValue(q("should-not-exist"), Enum2787.class);
        } catch (InvalidFormatException e) {
            verifyException(e, "should-not-exist", "not one of the values accepted for Enum class");
        }
    }

    @Test
    public void testMixinForWrapper() throws Exception {
        ObjectMapper mapper = MAPPER.addMixIn(Enum2787.class, EnumMixin2787.class);

        EnumWrapper result = mapper.readValue(a2q("{'value': 'C_MIXIN_ALIAS_1'}"), EnumWrapper.class);

        assertEquals(Enum2787.ITEM_C, result.value);
    }

    private ObjectMapper mapperWithMixIn(Class<?> target, Class<?> mixin) {
        return builderWithMixIn(target, mixin).build();
    }

    private JsonMapper.Builder builderWithMixIn(Class<?> target, Class<?> mixin) {
        return JsonMapper.builder().addMixIn(target, mixin);
    }
}
