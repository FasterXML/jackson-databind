package com.fasterxml.jackson.databind.deser.enums;

import java.io.IOException;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

public class EnumDefaultReadTest extends BaseMapTest
{
    enum SimpleEnum {
        ZERO,
        ONE;
    }

    enum SimpleEnumWithDefault {
        @JsonEnumDefaultValue
        ZERO,
        ONE;
    }

    enum CustomEnum {
        ZERO(0),
        ONE(1);

        private final int number;

        CustomEnum(final int number) {
            this.number = number;
        }

        @JsonValue
        int getNumber() {
            return this.number;
        }
    }

    enum CustomEnumWithDefault {
        @JsonEnumDefaultValue
        ZERO(0),
        ONE(1);

        private final int number;

        CustomEnumWithDefault(final int number) {
            this.number = number;
        }

        @JsonValue
        int getNumber() {
            return this.number;
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */
    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testWithoutCustomFeatures() throws Exception
    {
        final ObjectReader r = MAPPER.reader();

        _verifyOkDeserialization(r, "ZERO", SimpleEnum.class, SimpleEnum.ZERO);
        _verifyOkDeserialization(r, "ONE", SimpleEnum.class, SimpleEnum.ONE);
        _verifyOkDeserialization(r, "0", SimpleEnum.class, SimpleEnum.ZERO);
        _verifyOkDeserialization(r, "1", SimpleEnum.class, SimpleEnum.ONE);
        _verifyFailingDeserialization(r, "TWO", SimpleEnum.class);
        _verifyFailingDeserialization(r, "2", SimpleEnum.class);

        _verifyOkDeserialization(r, "ZERO", SimpleEnumWithDefault.class, SimpleEnumWithDefault.ZERO);
        _verifyOkDeserialization(r, "ONE", SimpleEnumWithDefault.class, SimpleEnumWithDefault.ONE);
        _verifyFailingDeserialization(r, "TWO", SimpleEnumWithDefault.class);
        _verifyOkDeserialization(r, "0", SimpleEnumWithDefault.class, SimpleEnumWithDefault.ZERO);
        _verifyOkDeserialization(r, "1", SimpleEnumWithDefault.class, SimpleEnumWithDefault.ONE);
        _verifyFailingDeserialization(r, "2", SimpleEnumWithDefault.class);

        _verifyFailingDeserialization(r, "ZERO", CustomEnum.class);
        _verifyFailingDeserialization(r, "ONE", CustomEnum.class);
        _verifyFailingDeserialization(r, "TWO", CustomEnum.class);
        _verifyOkDeserialization(r, "0", CustomEnum.class, CustomEnum.ZERO);
        _verifyOkDeserialization(r, "1", CustomEnum.class, CustomEnum.ONE);
        _verifyFailingDeserialization(r, "2", CustomEnum.class);

        _verifyFailingDeserialization(r, "ZERO", CustomEnumWithDefault.class);
        _verifyFailingDeserialization(r, "ONE", CustomEnumWithDefault.class);
        _verifyFailingDeserialization(r, "TWO", CustomEnumWithDefault.class);
        _verifyOkDeserialization(r, "0", CustomEnumWithDefault.class, CustomEnumWithDefault.ZERO);
        _verifyOkDeserialization(r, "1", CustomEnumWithDefault.class, CustomEnumWithDefault.ONE);
        _verifyFailingDeserialization(r, "2", CustomEnumWithDefault.class);
    }

    public void testWithFailOnNumbers() throws Exception
    {
        ObjectReader r = MAPPER.reader()
                .with(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS);

        _verifyOkDeserialization(r, "ZERO", SimpleEnum.class, SimpleEnum.ZERO);
        _verifyOkDeserialization(r, "ONE", SimpleEnum.class, SimpleEnum.ONE);
        _verifyFailingDeserialization(r, "TWO", SimpleEnum.class);
        _verifyFailingDeserialization(r, "0", SimpleEnum.class);
        _verifyFailingDeserialization(r, "1", SimpleEnum.class);
        _verifyFailingDeserialization(r, "2", SimpleEnum.class);

        _verifyOkDeserialization(r, "ZERO", SimpleEnumWithDefault.class, SimpleEnumWithDefault.ZERO);
        _verifyOkDeserialization(r, "ONE", SimpleEnumWithDefault.class, SimpleEnumWithDefault.ONE);
        _verifyFailingDeserialization(r, "TWO", SimpleEnumWithDefault.class);
        _verifyFailingDeserialization(r, "0", SimpleEnumWithDefault.class);
        _verifyFailingDeserialization(r, "1", SimpleEnumWithDefault.class);
        _verifyFailingDeserialization(r, "2", SimpleEnumWithDefault.class);

        _verifyFailingDeserialization(r, "ZERO", CustomEnum.class);
        _verifyFailingDeserialization(r, "ONE", CustomEnum.class);
        _verifyFailingDeserialization(r, "TWO", CustomEnum.class);
        _verifyOkDeserialization(r, "0", CustomEnum.class, CustomEnum.ZERO);
        _verifyOkDeserialization(r, "1", CustomEnum.class, CustomEnum.ONE);
        _verifyFailingDeserialization(r, "2", CustomEnum.class);

        _verifyFailingDeserialization(r, "ZERO", CustomEnumWithDefault.class);
        _verifyFailingDeserialization(r, "ONE", CustomEnumWithDefault.class);
        _verifyFailingDeserialization(r, "TWO", CustomEnumWithDefault.class);
        _verifyOkDeserialization(r, "0", CustomEnumWithDefault.class, CustomEnumWithDefault.ZERO);
        _verifyOkDeserialization(r, "1", CustomEnumWithDefault.class, CustomEnumWithDefault.ONE);
        _verifyFailingDeserialization(r, "2", CustomEnumWithDefault.class);
    }

    public void testWithReadUnknownAsDefault() throws Exception
    {
        ObjectReader r = MAPPER.reader()
                .with(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);

        _verifyOkDeserialization(r, "ZERO", SimpleEnum.class, SimpleEnum.ZERO);
        _verifyOkDeserialization(r, "ONE", SimpleEnum.class, SimpleEnum.ONE);
        _verifyFailingDeserialization(r, "TWO", SimpleEnum.class);
        _verifyOkDeserialization(r, "0", SimpleEnum.class, SimpleEnum.ZERO);
        _verifyOkDeserialization(r, "1", SimpleEnum.class, SimpleEnum.ONE);
        _verifyFailingDeserialization(r, "2", SimpleEnum.class);

        _verifyOkDeserialization(r, "ZERO", SimpleEnumWithDefault.class, SimpleEnumWithDefault.ZERO);
        _verifyOkDeserialization(r, "ONE", SimpleEnumWithDefault.class, SimpleEnumWithDefault.ONE);
        _verifyOkDeserialization(r, "TWO", SimpleEnumWithDefault.class, SimpleEnumWithDefault.ZERO);
        _verifyOkDeserialization(r, "0", SimpleEnumWithDefault.class, SimpleEnumWithDefault.ZERO);
        _verifyOkDeserialization(r, "1", SimpleEnumWithDefault.class, SimpleEnumWithDefault.ONE);
        _verifyOkDeserialization(r, "2", SimpleEnumWithDefault.class, SimpleEnumWithDefault.ZERO);

        _verifyFailingDeserialization(r, "ZERO", CustomEnum.class);
        _verifyFailingDeserialization(r, "ONE", CustomEnum.class);
        _verifyFailingDeserialization(r, "TWO", CustomEnum.class);
        _verifyOkDeserialization(r, "0", CustomEnum.class, CustomEnum.ZERO);
        _verifyOkDeserialization(r, "1", CustomEnum.class, CustomEnum.ONE);
        _verifyFailingDeserialization(r, "2", CustomEnum.class);

        _verifyOkDeserialization(r, "ZERO", CustomEnumWithDefault.class, CustomEnumWithDefault.ZERO);
        _verifyOkDeserialization(r, "ONE", CustomEnumWithDefault.class, CustomEnumWithDefault.ZERO);
        _verifyOkDeserialization(r, "TWO", CustomEnumWithDefault.class, CustomEnumWithDefault.ZERO);
        _verifyOkDeserialization(r, "0", CustomEnumWithDefault.class, CustomEnumWithDefault.ZERO);
        _verifyOkDeserialization(r, "1", CustomEnumWithDefault.class, CustomEnumWithDefault.ONE);
        _verifyOkDeserialization(r, "2", CustomEnumWithDefault.class, CustomEnumWithDefault.ZERO);
    }

    public void testWithFailOnNumbersAndReadUnknownAsDefault()
        throws Exception
    {
        ObjectReader r = MAPPER.reader()
                .with(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
                .with(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);

        _verifyOkDeserialization(r, "ZERO", SimpleEnum.class, SimpleEnum.ZERO);
        _verifyOkDeserialization(r, "ONE", SimpleEnum.class, SimpleEnum.ONE);

        _verifyFailingDeserialization(r, "TWO", SimpleEnum.class);
        _verifyFailingDeserialization(r, "0", SimpleEnum.class);
        _verifyFailingDeserialization(r, "1", SimpleEnum.class);
        _verifyFailingDeserialization(r, "2", SimpleEnum.class);

        _verifyOkDeserialization(r, "ZERO", SimpleEnumWithDefault.class, SimpleEnumWithDefault.ZERO);
        _verifyOkDeserialization(r, "ONE", SimpleEnumWithDefault.class, SimpleEnumWithDefault.ONE);
        _verifyOkDeserialization(r, "TWO", SimpleEnumWithDefault.class, SimpleEnumWithDefault.ZERO);

        _verifyFailingDeserialization(r, "ZERO", CustomEnum.class);
        _verifyFailingDeserialization(r, "ONE", CustomEnum.class);
        _verifyFailingDeserialization(r, "TWO", CustomEnum.class);

        _verifyOkDeserialization(r, "0", CustomEnum.class, CustomEnum.ZERO);
        _verifyOkDeserialization(r, "1", CustomEnum.class, CustomEnum.ONE);

        _verifyFailingDeserialization(r, "2", CustomEnum.class);

        _verifyOkDeserialization(r, "ZERO", CustomEnumWithDefault.class, CustomEnumWithDefault.ZERO);
        _verifyOkDeserialization(r, "ONE", CustomEnumWithDefault.class, CustomEnumWithDefault.ZERO);
        _verifyOkDeserialization(r, "TWO", CustomEnumWithDefault.class, CustomEnumWithDefault.ZERO);
        _verifyOkDeserialization(r, "0", CustomEnumWithDefault.class, CustomEnumWithDefault.ZERO);
        _verifyOkDeserialization(r, "1", CustomEnumWithDefault.class, CustomEnumWithDefault.ONE);

        //
        // The three tests below fail; Jackson throws an exception on the basis that
        // "FAIL_ON_NUMBERS_FOR_ENUMS" is enabled. I claim the default value should be returned instead.
        _verifyOkDeserialization(r, "0", SimpleEnumWithDefault.class, SimpleEnumWithDefault.ZERO);
        _verifyOkDeserialization(r, "1", SimpleEnumWithDefault.class, SimpleEnumWithDefault.ZERO);
        _verifyOkDeserialization(r, "2", SimpleEnumWithDefault.class, SimpleEnumWithDefault.ZERO);

        // Fails. Jackson throws an exception on the basis that "FAIL_ON_NUMBERS_FOR_ENUMS"
        // is enabled, but the default value should have been returned instead.
        _verifyOkDeserialization(r, "2", CustomEnumWithDefault.class, CustomEnumWithDefault.ZERO);
    }

    // [databind#3171]: Ensure "" gets returned as Default Value, not coerced
    public void testEmptyStringAsDefault() throws Exception
    {
        ObjectReader r = MAPPER.readerFor(SimpleEnumWithDefault.class)
                .with(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);
        assertEquals(SimpleEnumWithDefault.ONE,
                r.readValue(q("ONE")));
        assertEquals(SimpleEnumWithDefault.ZERO,
                r.readValue(q("Zero")));
        assertEquals(SimpleEnumWithDefault.ZERO,
                r.readValue(q("")));
        assertEquals(SimpleEnumWithDefault.ZERO,
                r.readValue(q("    ")));

        // Also check with `null` coercion as well
        ObjectReader r2 = MAPPER.readerFor(SimpleEnumWithDefault.class)
                .with(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
        assertNull(r2.readValue(q("")));
        assertNull(r2.readValue(q("    ")));
    }

    private <T> void _verifyOkDeserialization(ObjectReader reader, String fromValue,
            Class<T> toValueType, T expValue)
        throws IOException
    {
        assertEquals(expValue, reader.forType(toValueType).readValue(q(fromValue)));
    }

    private <T> void _verifyFailingDeserialization(final ObjectReader reader,
            final String fromValue, final Class<T> toValueType)
        throws IOException
    {
        try {
            reader.forType(toValueType).readValue(q(fromValue));
            fail("Deserialization should have failed");
        } catch (InvalidFormatException e) {
            verifyException(e, "Cannot deserialize value of type");
            /* Expected. */
        }
    }
}
