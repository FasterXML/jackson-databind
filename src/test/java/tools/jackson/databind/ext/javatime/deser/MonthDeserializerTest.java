package tools.jackson.databind.ext.javatime.deser;

import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.OptBoolean;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;
import tools.jackson.databind.ext.javatime.MockObjectConfiguration;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;

public class MonthDeserializerTest extends DateTimeTestBase
{
    static class Wrapper {
        public Month value;

        public Wrapper(Month v) { value = v; }
        public Wrapper() { }
    }

    static class WrapperWithFormat {
        @JsonFormat(pattern = "MMM", locale = "en")
        public Month value;
    }

    @ParameterizedTest
    @EnumSource(Month.class)
    public void testDeserializationAsString01_oneBased(Month expectedMonth) throws Exception
    {
        int monthNum = expectedMonth.getValue();
        assertEquals(expectedMonth, readerForOneBased().readValue("\"" + monthNum + '"'));
    }

    @ParameterizedTest
    @EnumSource(Month.class)
    public void testDeserializationAsString01_zeroBased(Month expectedMonth) throws Exception
    {
        int monthNum = expectedMonth.ordinal();
        assertEquals(expectedMonth, readerForZeroBased().readValue("\"" + monthNum + '"'));
    }


    @ParameterizedTest
    @EnumSource(Month.class)
    public void testDeserializationAsString02_oneBased(Month month) throws Exception
    {
        assertEquals(month, readerForOneBased().readValue("\"" + month.name() + '"'));
    }

    @ParameterizedTest
    @EnumSource(Month.class)
    public void testDeserializationAsString02_zeroBased(Month month) throws Exception
    {
        assertEquals(month, readerForOneBased().readValue("\"" + month.name() + '"'));
    }

    @ParameterizedTest
    @CsvSource({
            "notamonth , 'Cannot deserialize value of type `java.time.Month` from String \"notamonth\": not one of known `Month` values:'",
            "JANUAR    , 'Cannot deserialize value of type `java.time.Month` from String \"JANUAR\": not one of known `Month` values:'",
            "march     , 'Cannot deserialize value of type `java.time.Month` from String \"march\": not one of known `Month` values:'",
            "0         , 'month number outside 1-12'",
            "13        , 'month number outside 1-12'",
    })
    public void testBadDeserializationAsString01_oneBased(String monthSpec, String expectedMessage) {
        String value = "\"" + monthSpec + '"';
        assertError(
            () -> readerForOneBased().readValue(value),
            InvalidFormatException.class,
            expectedMessage
        );
    }

    static void assertError(Executable codeToRun, Class<? extends Throwable> expectedException, String expectedMessage) {
        try {
            codeToRun.execute();
            fail(String.format("Expecting %s, but nothing was thrown!", expectedException.getName()));
        } catch (Throwable actualException) {
            if (!expectedException.isInstance(actualException)) {
                fail(String.format("Expecting exception of type %s, but %s was thrown instead", expectedException.getName(), actualException.getClass().getName()));
            }
            if (actualException.getMessage() == null || !actualException.getMessage().contains(expectedMessage)) {
                fail(String.format("Expecting exception with message containing: '%s', but the actual error message was:'%s'", expectedMessage, actualException.getMessage()));
            }
        }
    }

    private final ObjectMapper MAPPER = newMapper();

    @Test
    public void testDeserialization01_zeroBased() throws Exception
    {
        assertEquals(Month.FEBRUARY, readerForZeroBased().readValue("1"));
    }

    @Test
    public void testDeserialization01_oneBased() throws Exception
    {
        assertEquals(Month.JANUARY, readerForOneBased().readValue("1"));
    }

    @Test
    public void testDeserialization02_zeroBased() throws Exception
    {
        assertEquals(Month.SEPTEMBER, readerForZeroBased().readValue("\"8\""));
    }

    @Test
    public void testDeserialization02_oneBased() throws Exception
    {
        assertEquals(Month.AUGUST, readerForOneBased().readValue("\"8\""));
    }

    @Test
    public void testDeserializationWithTypeInfo01_oneBased() throws Exception
    {
        ObjectMapper MAPPER = JsonMapper.builder()
            .addMixIn(TemporalAccessor.class, MockObjectConfiguration.class)
            .enable(DateTimeFeature.ONE_BASED_MONTHS)
            .build();

        TemporalAccessor value = MAPPER.readValue("[\"java.time.Month\",11]", TemporalAccessor.class);
        assertEquals(Month.NOVEMBER, value);
    }

    @Test
    public void testDeserializationWithTypeInfo01_zeroBased() throws Exception
    {
        ObjectMapper MAPPER = JsonMapper.builder()
                .addMixIn(TemporalAccessor.class, MockObjectConfiguration.class)
                .disable(DateTimeFeature.ONE_BASED_MONTHS)
                .build();

        TemporalAccessor value = MAPPER.readValue("[\"java.time.Month\",\"11\"]", TemporalAccessor.class);
        assertEquals(Month.DECEMBER, value);
    }

    @Test
    public void testFormatAnnotation_zeroBased() throws Exception
    {
        Wrapper output = readerForZeroBased()
                .forType(Wrapper.class)
                .readValue("{\"value\":\"11\"}");
        assertEquals(new Wrapper(Month.DECEMBER).value, output.value);
    }

    @Test
    public void testFormatAnnotation_oneBased() throws Exception
    {
        Wrapper output = readerForOneBased()
                .forType(Wrapper.class)
                .readValue("{\"value\":\"11\"}");
        assertEquals(new Wrapper(Month.NOVEMBER).value, output.value);
    }

    /*
    /**********************************************************************
    /* Tests for empty string handling
    /**********************************************************************
     */

    @Test
    public void testDeserializeFromEmptyString() throws Exception
    {
        // Nulls are handled in general way, not by deserializer so they are ok
        Month m = MAPPER.readerFor(Month.class).readValue(" null ");
        assertNull(m);

        // Although coercion from empty String not enabled for Enums by default,
        // it IS for Scalars (when `MapperFeature.ALLOW_COERCION_OF_SCALARS` enabled
        // which it is by default). So need to disable it here:
        // (we no longer consider `Month` as LogicalType.Enum but LogicalType.DateTime)
        try {
            ObjectMapper strictMapper = mapperBuilder()
                    .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
                    .build();
            Month result = strictMapper.readerFor(Month.class).readValue("\"\"");
            fail("Should not pass, but got: " + result);
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot coerce empty String");
        }
        // But can allow coercion of empty String to, say, null
        ObjectMapper emptyStringMapper = mapperBuilder()
                .withCoercionConfig(Month.class,
                        h -> h.setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull))
                .build();
        m = emptyStringMapper.readerFor(Month.class).readValue("\"\"");
        assertNull(m);
    }

    /*
    /**********************************************************************
    /* Tests for numeric int input (VALUE_NUMBER_INT)
    /**********************************************************************
     */

    @ParameterizedTest
    @EnumSource(Month.class)
    public void testDeserializationAsInt_zeroBased(Month expectedMonth) throws Exception
    {
        int monthIndex = expectedMonth.ordinal();
        assertEquals(expectedMonth, readerForZeroBased().readValue(String.valueOf(monthIndex)));
    }

    @ParameterizedTest
    @EnumSource(Month.class)
    public void testDeserializationAsInt_oneBased(Month expectedMonth) throws Exception
    {
        int monthNum = expectedMonth.getValue();
        assertEquals(expectedMonth, readerForOneBased().readValue(String.valueOf(monthNum)));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 12, 13, 100})
    public void testDeserializationAsIntOutOfRange_zeroBased(int invalidValue) throws Exception
    {
        assertError(
            () -> readerForZeroBased().readValue(String.valueOf(invalidValue)),
            MismatchedInputException.class,
            "month number outside 0-11 range"
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 13, 100})
    public void testDeserializationAsIntOutOfRange_oneBased(int invalidValue) throws Exception
    {
        assertError(
            () -> readerForOneBased().readValue(String.valueOf(invalidValue)),
            MismatchedInputException.class,
            "month number outside 1-12 range"
        );
    }

    /*
    /**********************************************************************
    /* Tests for array handling
    /**********************************************************************
     */

    @Test
    public void testDeserializationAsEmptyArray() throws Exception
    {
        // Empty array returns null
        Month result = readerForOneBased().readValue("[]");
        assertNull(result);
    }

    @Test
    public void testDeserializationAsArrayWithIntValue() throws Exception
    {
        // Array with single int value (interpreted as 1-based month)
        Month result = readerForOneBased().readValue("[3]");
        assertEquals(Month.MARCH, result);
    }

    @Test
    public void testDeserializationAsArrayWithIntValue_zeroBased() throws Exception
    {
        // Array with single int value (0-based mode still uses Month.of for array)
        Month result = readerForZeroBased().readValue("[3]");
        assertEquals(Month.MARCH, result);
    }

    @Test
    public void testDeserializationAsArrayWithMoreThanOneElement() throws Exception
    {
        assertError(
            () -> readerForOneBased().readValue("[1, 2]"),
            MismatchedInputException.class,
            "Expected array to end"
        );
    }

    @Test
    public void testDeserializationAsArrayWithWrongToken() throws Exception
    {
        // Boolean in array without UNWRAP should fail with specific error
        assertError(
            () -> readerForOneBased().readValue("[true]"),
            MismatchedInputException.class,
            "Expected VALUE_NUMBER_INT"
        );
    }

    @Test
    public void testDeserializationAsArrayWithStringUnwrapDisabled() throws Exception
    {
        // String in array without UNWRAP_SINGLE_VALUE_ARRAYS should fail
        assertError(
            () -> readerForOneBased().readValue("[\"JANUARY\"]"),
            MismatchedInputException.class,
            "Expected VALUE_NUMBER_INT"
        );
    }

    @Test
    public void testDeserializationAsArrayWithFloatUnwrapDisabled() throws Exception
    {
        // Float in array without UNWRAP should fail
        assertError(
            () -> readerForOneBased().readValue("[1.5]"),
            MismatchedInputException.class,
            "Expected VALUE_NUMBER_INT"
        );
    }

    @Test
    public void testDeserializationAsArrayWithObjectUnwrapDisabled() throws Exception
    {
        // Object in array without UNWRAP should fail
        assertError(
            () -> readerForOneBased().readValue("[{}]"),
            MismatchedInputException.class,
            "Expected VALUE_NUMBER_INT"
        );
    }

    @Test
    public void testDeserializationAsArrayWithStringUnwrapEnabled() throws Exception
    {
        // String in array with UNWRAP_SINGLE_VALUE_ARRAYS should work
        Month result = MAPPER.readerFor(Month.class)
                .with(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
                .with(DateTimeFeature.ONE_BASED_MONTHS)
                .readValue("[\"JANUARY\"]");
        assertEquals(Month.JANUARY, result);
    }

    @Test
    public void testDeserializationAsArrayWithNumericStringUnwrapEnabled() throws Exception
    {
        // Numeric string in array with UNWRAP_SINGLE_VALUE_ARRAYS
        Month result = MAPPER.readerFor(Month.class)
                .with(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
                .with(DateTimeFeature.ONE_BASED_MONTHS)
                .readValue("[\"5\"]");
        assertEquals(Month.MAY, result);
    }

    @Test
    public void testDeserializationAsArrayWithMoreThanOneString() throws Exception
    {
        // More than one string with UNWRAP_SINGLE_VALUE_ARRAYS should fail
        assertError(
            () -> MAPPER.readerFor(Month.class)
                    .with(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
                    .readValue("[\"JANUARY\", \"FEBRUARY\"]"),
            MismatchedInputException.class,
            "Attempted to unwrap"
        );
    }

    /*
    /**********************************************************************
    /* Tests for zero-based string parsing edge cases
    /**********************************************************************
     */

    @ParameterizedTest
    @CsvSource({
            "12  , 'month number outside 0-11'",
            "-1  , 'month number outside 0-11'",
            "100 , 'month number outside 0-11'",
    })
    public void testBadDeserializationAsString_zeroBasedOutOfRange(String monthSpec, String expectedMessage) {
        String value = q(monthSpec);
        assertError(
            () -> readerForZeroBased().readValue(value),
            InvalidFormatException.class,
            expectedMessage
        );
    }

    /*
    /**********************************************************************
    /* Tests for whitespace handling
    /**********************************************************************
     */

    @Test
    public void testDeserializationWithWhitespace() throws Exception
    {
        // Whitespace around month name should be trimmed
        Month result = readerForOneBased().readValue("\" JANUARY \"");
        assertEquals(Month.JANUARY, result);
    }

    @Test
    public void testDeserializationWithWhitespaceNumeric() throws Exception
    {
        // Whitespace around numeric value should be trimmed
        Month result = readerForOneBased().readValue("\" 6 \"");
        assertEquals(Month.JUNE, result);
    }

    /*
    /**********************************************************************
    /* Tests for unexpected tokens
    /**********************************************************************
     */

    @Test
    public void testDeserializationFromBoolean() throws Exception
    {
        // Bare boolean should be handled as unexpected token
        assertError(
            () -> readerForOneBased().readValue("true"),
            MismatchedInputException.class,
            "Unexpected token (VALUE_TRUE)"
        );
    }

    @Test
    public void testDeserializationFromFloat() throws Exception
    {
        // Bare float should be handled as unexpected token
        assertError(
            () -> readerForOneBased().readValue("1.5"),
            MismatchedInputException.class,
            "Unexpected token (VALUE_NUMBER_FLOAT)"
        );
    }

    @Test
    public void testDeserializationFromObject() throws Exception
    {
        // Object without scalar extraction should fail
        assertError(
            () -> readerForOneBased().readValue("{}"),
            MismatchedInputException.class,
            "Unexpected token (START_OBJECT)"
        );
    }

    /*
    /**********************************************************************
    /* Tests for custom DateTimeFormatter
    /**********************************************************************
     */

    @Test
    public void testDeserializationWithCustomFormat() throws Exception
    {
        WrapperWithFormat result = MAPPER.readValue("{\"value\":\"Jan\"}", WrapperWithFormat.class);
        assertEquals(Month.JANUARY, result.value);
    }

    @Test
    public void testDeserializationWithCustomFormatMarch() throws Exception
    {
        WrapperWithFormat result = MAPPER.readValue("{\"value\":\"Mar\"}", WrapperWithFormat.class);
        assertEquals(Month.MARCH, result.value);
    }

    @Test
    public void testDeserializationWithCustomFormatInvalid() throws Exception
    {
        assertError(
            () -> MAPPER.readValue("{\"value\":\"NotAMonth\"}", WrapperWithFormat.class),
            InvalidFormatException.class,
            "could not be parsed"
        );
    }

    static class WrapperWithFullMonthFormat {
        @JsonFormat(pattern = "MMMM", locale = "en")
        public Month value;
    }

    @Test
    public void testDeserializationWithFullMonthFormat() throws Exception
    {
        WrapperWithFullMonthFormat result = MAPPER.readValue(
                "{\"value\":\"January\"}", WrapperWithFullMonthFormat.class);
        assertEquals(Month.JANUARY, result.value);
    }

    /*
    /**********************************************************************
    /* Tests for leniency settings
    /**********************************************************************
     */

    static class WrapperStrict {
        @JsonFormat(lenient = OptBoolean.FALSE)
        public Month value;
    }

    static class WrapperLenient {
        @JsonFormat(lenient = OptBoolean.TRUE)
        public Month value;
    }

    @Test
    public void testWithLeniencyCreatesNewInstance() throws Exception
    {
        MonthDeserializer original = MonthDeserializer.INSTANCE;
        MonthDeserializer strict = original.withLeniency(false);
        assertNotSame(original, strict);
        assertFalse(strict.isLenient());
    }

    @Test
    public void testWithDateFormatCreatesNewInstance() throws Exception
    {
        MonthDeserializer original = MonthDeserializer.INSTANCE;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM");
        MonthDeserializer withFormatter = original.withDateFormat(formatter);
        assertNotSame(original, withFormatter);
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    private ObjectReader readerForZeroBased() {
        return MAPPER
                .readerFor(Month.class)
                .without(DateTimeFeature.ONE_BASED_MONTHS);
    }

    private ObjectReader readerForOneBased() {
        return MAPPER
            .readerFor(Month.class)
            .with(DateTimeFeature.ONE_BASED_MONTHS);
    }
}
