package com.fasterxml.jackson.databind.deser.enums;

import java.io.IOException;
import java.util.EnumSet;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonFormat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.*;

public class EnumAltIdTest
{
    // [databind#1313]

    enum TestEnum { JACKSON, RULES, OK; }
    protected enum LowerCaseEnum {
        A, B, C;
        private LowerCaseEnum() { }
        @Override
        public String toString() { return name().toLowerCase(); }
    }

    protected static class EnumBean {
        @JsonFormat(with={ JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES })
        public TestEnum value;
    }

    protected static class StrictCaseBean {
        @JsonFormat(without={ JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES })
        public TestEnum value;
    }

    protected static class DefaultEnumBean {
        @JsonFormat(with={ JsonFormat.Feature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE })
        public MyEnum2352_3 value;
    }

    protected static class DefaultEnumSetBean {
        @JsonFormat(with={ JsonFormat.Feature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE })
        public EnumSet<MyEnum2352_3> value;
    }

    protected static class NullValueEnumBean {
        @JsonFormat(with={ JsonFormat.Feature.READ_UNKNOWN_ENUM_VALUES_AS_NULL })
        public MyEnum2352_3 value;
    }

    protected static class NullEnumSetBean {
        @JsonFormat(with={ JsonFormat.Feature.READ_UNKNOWN_ENUM_VALUES_AS_NULL })
        public EnumSet<MyEnum2352_3> value;
    }

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
        C;
    }

    // [databind#4481]: override for "unknown as null"
    enum Color {
        RED, BLUE
    }

    static class Book4481 {
        @JsonFormat(without = JsonFormat.Feature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
        public Color color;
    }

    enum Types {
        @JsonEnumDefaultValue
        DEFAULT_TYPE,
        FAST, SLOW
    }

    static class SpeedWithoutDefaultOverride {
        @JsonFormat(without = JsonFormat.Feature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
        public Types type;
    }

    static class SpeedWithDefaultOverride {
        @JsonFormat(with = JsonFormat.Feature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
        public Types type;
    }

    /*
    /**********************************************************
    /* Test methods, basic
    /**********************************************************
     */

    protected final ObjectMapper MAPPER = new ObjectMapper();
    protected final ObjectMapper MAPPER_IGNORE_CASE = jsonMapperBuilder()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .build();

    protected final ObjectReader READER_DEFAULT = MAPPER.reader();
    protected final ObjectReader READER_IGNORE_CASE = MAPPER_IGNORE_CASE.reader();

    // Tests for [databind#1313], case-insensitive

    @Test
    public void testFailWhenCaseSensitiveAndNameIsNotUpperCase() throws IOException {
        try {
            READER_DEFAULT.forType(TestEnum.class).readValue("\"Jackson\"");
            fail("InvalidFormatException expected");
        } catch (InvalidFormatException e) {
            verifyException(e, "not one of the values accepted for Enum class");
            verifyException(e, "[JACKSON, OK, RULES]");
        }
    }

    @Test
    public void testFailWhenCaseSensitiveAndToStringIsUpperCase() throws IOException {
        ObjectReader r = READER_DEFAULT.forType(LowerCaseEnum.class)
                .with(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
        try {
            r.readValue("\"A\"");
            fail("InvalidFormatException expected");
        } catch (InvalidFormatException e) {
            verifyException(e, "not one of the values accepted for Enum class");
            verifyException(e,"[a, b, c]");
        }
    }

    @Test
    public void testEnumDesIgnoringCaseWithLowerCaseContent() throws IOException {
        assertEquals(TestEnum.JACKSON,
                READER_IGNORE_CASE.forType(TestEnum.class).readValue(q("jackson")));
    }

    @Test
    public void testEnumDesIgnoringCaseWithUpperCaseToString() throws IOException {
        ObjectReader r = MAPPER_IGNORE_CASE.readerFor(LowerCaseEnum.class)
                .with(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
        assertEquals(LowerCaseEnum.A, r.readValue("\"A\""));
    }

    /*
    /**********************************************************
    /* Test methods, containers
    /**********************************************************
     */

    @Test
    public void testIgnoreCaseInEnumList() throws Exception {
        TestEnum[] enums = READER_IGNORE_CASE.forType(TestEnum[].class)
            .readValue("[\"jacksON\", \"ruLes\"]");

        assertEquals(2, enums.length);
        assertEquals(TestEnum.JACKSON, enums[0]);
        assertEquals(TestEnum.RULES, enums[1]);
    }

    @Test
    public void testIgnoreCaseInEnumSet() throws IOException {
        ObjectReader r = READER_IGNORE_CASE.forType(new TypeReference<EnumSet<TestEnum>>() { });
        EnumSet<TestEnum> set = r.readValue("[\"jackson\"]");
        assertEquals(1, set.size());
        assertTrue(set.contains(TestEnum.JACKSON));
    }

    /*
    /**********************************************************
    /* Test methods, property overrides
    /**********************************************************
     */

    @Test
    public void testIgnoreCaseViaFormat() throws Exception
    {
        final String JSON = a2q("{'value':'ok'}");

        // should be able to allow on per-case basis:
        EnumBean pojo = READER_DEFAULT.forType(EnumBean.class)
            .readValue(JSON);
        assertEquals(TestEnum.OK, pojo.value);

        // including disabling acceptance
        try {
            READER_DEFAULT.forType(StrictCaseBean.class)
                    .readValue(JSON);
            fail("Should not pass");
        } catch (InvalidFormatException e) {
            verifyException(e, "not one of the values accepted for Enum class");
            verifyException(e, "[JACKSON, OK, RULES]");
        }
    }

    /*
    /**********************************************************
    /* Test methods, Enum Aliases [databind#2352]
    /**********************************************************
     */

    // for [databind#2352]
    @Test
    public void testEnumWithAlias() throws Exception {
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
    public void testEnumWithAliasAndToStringSupported() throws Exception {
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
    public void testEnumWithAliasAndDefaultForUnknownValueEnabled() throws Exception {
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

    @Test
    public void testEnumWithDefaultForUnknownValueEnabled() throws Exception {
        final String JSON = a2q("{'value':'ok'}");

        DefaultEnumBean pojo = READER_DEFAULT.forType(DefaultEnumBean.class)
          .readValue(JSON);
        assertEquals(MyEnum2352_3.B, pojo.value);
        // including disabling acceptance
        try {
            READER_DEFAULT.forType(StrictCaseBean.class)
              .readValue(JSON);
            fail("Should not pass");
        } catch (InvalidFormatException e) {
            verifyException(e, "not one of the values accepted for Enum class");
            verifyException(e, "[JACKSON, OK, RULES]");
        }
    }

    @Test
    public void testEnumWithNullForUnknownValueEnabled() throws Exception {
        final String JSON = a2q("{'value':'ok'}");

        NullValueEnumBean pojo = READER_DEFAULT.forType(NullValueEnumBean.class)
          .readValue(JSON);
        assertNull(pojo.value);
        // including disabling acceptance
        try {
            READER_DEFAULT.forType(StrictCaseBean.class)
              .readValue(JSON);
            fail("Should not pass");
        } catch (InvalidFormatException e) {
            verifyException(e, "not one of the values accepted for Enum class");
            verifyException(e, "[JACKSON, OK, RULES]");
        }
    }

    @Test
    public void testEnumWithDefaultForUnknownValueEnumSet() throws Exception {
        final String JSON = a2q("{'value':['ok']}");

        DefaultEnumSetBean pojo = READER_DEFAULT.forType(DefaultEnumSetBean.class)
          .readValue(JSON);
        assertEquals(1, pojo.value.size());
        assertTrue(pojo.value.contains(MyEnum2352_3.B));
    }

    @Test
    public void testEnumWithNullForUnknownValueEnumSet() throws Exception {
        final String JSON = a2q("{'value':['ok','B']}");

        NullEnumSetBean pojo = READER_DEFAULT.forType(NullEnumSetBean.class)
          .readValue(JSON);
        assertEquals(1, pojo.value.size());
        assertTrue(pojo.value.contains(MyEnum2352_3.B));
    }

    /**
     * Test to verify that configuration via
     * {@link com.fasterxml.jackson.annotation.JsonFormat.Feature#READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE}
     * takes precedence over global configuration.
     */
    @Test
    public void testJsonEnumDefaultValueOverrideOverGlobalConfig() throws Exception {
        final String UNKNOWN_JSON = a2q("{'type':'OOPS!'}");

        // First, global configuration is ENABLED and JsonFeature configuration is DISABLED
        // So the test should fail
        try {
            JsonMapper.builder()
                .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
                .build()
                .readValue(UNKNOWN_JSON, SpeedWithoutDefaultOverride.class);
            fail();
        } catch (InvalidFormatException e) {
            verifyException(e, "Cannot deserialize value of type");
            verifyException(e, "not one of the values accepted for Enum class");
        }

        // Second, global configuration is DISABLED and JsonFeature configuration is ENABLED
        // So the test should pass
        SpeedWithDefaultOverride pojo = JsonMapper.builder()
            .disable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
            .build()
            .readValue(UNKNOWN_JSON, SpeedWithDefaultOverride.class);

        assertEquals(Types.DEFAULT_TYPE, pojo.type);
    }

    /*
    /**********************************************************
    /* Test methods, other
    /**********************************************************
     */

    // [databind#4481]
    @Test
    public void testDefaultFromNullOverride4481() throws Exception
    {
        try {
            Book4481 book = MAPPER.readerFor(Book4481.class)
                    .with(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
                    .readValue("{\"color\":\"WHITE\"}");
            fail("Should have failed; got: "+book.color);
        } catch (InvalidFormatException e) {
            verifyException(e, "Cannot deserialize value of type ");
            verifyException(e, "not one of the values accepted for Enum class");
        }
    }
}
