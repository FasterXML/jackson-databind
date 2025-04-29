package tools.jackson.databind.ext.javatime.deser;

import java.time.*;
import java.time.temporal.Temporal;
import java.util.Map;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.fasterxml.jackson.annotation.OptBoolean;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Feature;

import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.core.type.TypeReference;

import tools.jackson.databind.*;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;
import tools.jackson.databind.ext.javatime.MockObjectConfiguration;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;

public class LocalDateDeserTest extends DateTimeTestBase
{
    private final ObjectMapper MAPPER = newMapper();
    private final ObjectReader READER = MAPPER.readerFor(LocalDate.class);
    private final ObjectReader READER_USING_TIME_ZONE = JsonMapper.builder()
        .enable(DateTimeFeature.USE_TIME_ZONE_FOR_LENIENT_DATE_PARSING)
        .build()
        .readerFor(LocalDate.class);

    private final TypeReference<Map<String, LocalDate>> MAP_TYPE_REF = new TypeReference<Map<String, LocalDate>>() { };

    final static class Wrapper {
        @JsonFormat(pattern="yyyy_MM_dd'T'HH:mmZ",
                shape=JsonFormat.Shape.STRING)
        public LocalDate value;

        public Wrapper() { }
        public Wrapper(LocalDate v) { value = v; }
    }

    final static class ShapeWrapper {
        @JsonFormat(shape=JsonFormat.Shape.NUMBER_INT)
        public LocalDate date;

        public ShapeWrapper() { }
        public ShapeWrapper(LocalDate v) { date = v; }
    }

    static class StrictWrapperWithFormat {
        @JsonFormat(pattern="yyyy-MM-dd",
                lenient = OptBoolean.FALSE)
        public LocalDate value;

        public StrictWrapperWithFormat() { }
        public StrictWrapperWithFormat(LocalDate v) { value = v; }
    }

    final static class StrictWrapperWithYearOfEra {
        @JsonFormat(pattern="yyyy-MM-dd G",
                lenient = OptBoolean.FALSE)
        public LocalDate value;

        public StrictWrapperWithYearOfEra() { }
        public StrictWrapperWithYearOfEra(LocalDate v) { value = v; }
    }

    final static class StrictWrapperWithYearWithoutEra {
        @JsonFormat(pattern="uuuu-MM-dd",
                lenient = OptBoolean.FALSE)
        public LocalDate value;

        public StrictWrapperWithYearWithoutEra() { }
        public StrictWrapperWithYearWithoutEra(LocalDate v) { value = v; }
    }

    /*
    /**********************************************************
    /* Deserialization from Int array representation
    /**********************************************************
     */
    
    @Test
    public void testDeserializationAsTimestamp01()
    {
        assertEquals(LocalDate.of(1986, Month.JANUARY, 17),
                READER.readValue("[1986,1,17]"));
    }

    @Test
    public void testDeserializationAsTimestamp02()
    {
        assertEquals(LocalDate.of(2013, Month.AUGUST, 21),
                READER.readValue("[2013,8,21]"));
    }

    /*
    /**********************************************************
    /* Deserialization from String representation
    /**********************************************************
     */
    
    @Test
    public void testDeserializationAsString01()
    {
        assertEquals(LocalDate.of(2000, Month.JANUARY, 1), READER.readValue(q("2000-01-01")));

        LocalDate date = LocalDate.of(1986, Month.JANUARY, 17);
        assertEquals(date, READER.readValue('"' + date.toString() + '"'));

        date = LocalDate.of(2013, Month.AUGUST, 21);
        assertEquals(date, READER.readValue('"' + date.toString() + '"'));
    }

    @Test
    public void testDeserializationAsString02()
    {
        LocalDateTime date = LocalDateTime.now();
        assertEquals(date.toLocalDate(), READER.readValue('"' + date.toString() + '"'));
    }

    @Test
    public void testLenientDeserializationAsString01() throws Exception
    {
        Instant instant = Instant.now();
        LocalDate value = READER.readValue(q(instant.toString()));
        assertEquals(LocalDateTime.ofInstant(instant, ZoneOffset.UTC).toLocalDate(), value);
    }

    @Test
    public void testLenientDeserializationAsString02() throws Exception
    {
        ObjectReader reader = READER.with(TimeZone.getTimeZone(Z_BUDAPEST));
        Instant instant = Instant.now();
        LocalDate value = reader.readValue(q(instant.toString()));
        assertEquals(LocalDateTime.ofInstant(instant, ZoneOffset.UTC).toLocalDate(), value);
    }

    @Test
    public void testLenientDeserializationAsString03() throws Exception
    {
        Instant instant = Instant.now();
        LocalDate value = READER_USING_TIME_ZONE.readValue(q(instant.toString()));
        assertEquals(LocalDateTime.ofInstant(instant, ZoneOffset.UTC).toLocalDate(), value);
    }

    @ParameterizedTest
    @CsvSource({
        "Europe/Budapest, 2024-07-21T21:59:59Z, 2024-07-21",
        "Europe/Budapest, 2024-07-21T22:00:00Z, 2024-07-22",
        "America/Chicago, 2024-07-22T04:59:59Z, 2024-07-21",
        "America/Chicago, 2024-07-22T05:00:00Z, 2024-07-22"
    })
    public void testLenientDeserializationAsString04(TimeZone zone, String string, LocalDate expected) throws Exception
    {
        ObjectReader reader = READER_USING_TIME_ZONE.with(zone);
        LocalDate value = reader.readValue(q(string));
        assertEquals(expected, value);
    }

    @Test
    public void testBadDeserializationAsString01()
    {
        try {
            READER.readValue(q("notalocaldate"));
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot deserialize value of type");
            verifyException(e, "from String \"");
        }
    }

    @Test
    public void testBadDeserializationAsString02()
    {
        try {
            READER.readValue(q("2015-06-19TShouldNotParse"));
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot deserialize value of type");
            verifyException(e, "from String \"");
        }
    }

    @Test
    public void testDeserializationWithTypeInfo01()
    {
        ObjectMapper mapper = mapperBuilder()
               .addMixIn(Temporal.class, MockObjectConfiguration.class)
               .build();
        LocalDate date = LocalDate.of(2005, Month.NOVEMBER, 5);
        Temporal value = mapper.readValue(
                "[\"" + LocalDate.class.getName() + "\",\"" + date.toString() + "\"]", Temporal.class
                );
        assertEquals(date, value);
    }

    /*
    /**********************************************************
    /* Deserialization from alternate representation: int (number
    /* of days since Epoch)
    /**********************************************************
     */

    // By default, lenient handling on so we can do this:
    @Test
    public void testLenientDeserializeFromInt()
    {
        assertEquals(LocalDate.of(1970, Month.JANUARY, 3), READER.readValue("2"));

        assertEquals(LocalDate.of(1970, Month.FEBRUARY, 10), READER.readValue("40"));
    }

    // But with alternate setting, not so
    @Test
    public void testStricDeserializeFromInt()
    {
        ObjectMapper mapper = mapperBuilder()
                .withConfigOverride(LocalDate.class,
                        c -> c.setFormat(JsonFormat.Value.forLeniency(false))
                )
                .build();
        try {
            mapper.readValue("2", LocalDate.class);
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot deserialize instance of");
            verifyException(e, "not allowed because 'strict' mode set for property or type");
        }

        // 17-Aug-2019, tatu: Should possibly test other mechanism too, but for now let's
        //    be content with just one...
    }

    /*
    /**********************************************************
    /* Tests for empty string handling
    /**********************************************************
     */

    @Test
    public void testLenientDeserializeFromEmptyString()
    {

        String key = "date";
        ObjectMapper mapper = newMapper();
        ObjectReader objectReader = mapper.readerFor(MAP_TYPE_REF);

        String dateValAsNullStr = null;
        String dateValAsEmptyStr = "";

        String valueFromNullStr = mapper.writeValueAsString(asMap(key, dateValAsNullStr));
        Map<String, LocalDate> actualMapFromNullStr = objectReader.readValue(valueFromNullStr);
        LocalDate actualDateFromNullStr = actualMapFromNullStr.get(key);
        assertNull(actualDateFromNullStr);

        String valueFromEmptyStr = mapper.writeValueAsString(asMap(key, dateValAsEmptyStr));
        Map<String, LocalDate> actualMapFromEmptyStr = objectReader.readValue(valueFromEmptyStr);
        LocalDate actualDateFromEmptyStr = actualMapFromEmptyStr.get(key);
        assertEquals(actualDateFromNullStr, actualDateFromEmptyStr, "empty string failed to deserialize to null with lenient setting");
    }

    @Test
    // ( expected =  MismatchedInputException.class)
    public void testStrictDeserializeFromEmptyString() throws Exception {

        final String key = "date";
        final ObjectMapper mapper = mapperBuilder()
                .withConfigOverride(LocalDate.class,
                        c -> c.setFormat(JsonFormat.Value.forLeniency(false))
                )
                .build();
        final ObjectReader objectReader = mapper.readerFor(MAP_TYPE_REF);
        final String dateValAsNullStr = null;

        // even with strict, null value should be deserialized without throwing an exception
        String valueFromNullStr = mapper.writeValueAsString(asMap(key, dateValAsNullStr));
        Map<String, LocalDate> actualMapFromNullStr = objectReader.readValue(valueFromNullStr);
        assertNull(actualMapFromNullStr.get(key));

        String dateValAsEmptyStr = "";
        // TODO: nothing stops us from writing an empty string, maybe there should be a check there too?
        String valueFromEmptyStr = mapper.writeValueAsString(asMap("date", dateValAsEmptyStr));
        // with strict, deserializing an empty string is not permitted
        assertThrows(MismatchedInputException.class, () -> objectReader.readValue(valueFromEmptyStr));
    }

    /*
    /**********************************************************
    /* Tests for alternate array handling
    /**********************************************************
     */
    
    @Test
    public void testDeserializationAsArrayDisabled()
    {
        try {
            READER.readValue("[\"2000-01-01\"]");
            fail("expected MismatchedInputException");
        } catch (MismatchedInputException e) {
            verifyException(e, "Unexpected token (VALUE_STRING) within Array");
        }
    }

    @Test
    public void testDeserializationAsEmptyArrayDisabled()
    {
        // works even without the feature enabled
        assertNull(READER.readValue("[]"));
    }

    @Test
    public void testDeserializationAsArrayEnabled()
    {
        LocalDate actual = READER
                .with(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
                .readValue("[\"2000-01-01\"]");
        assertEquals(LocalDate.of(2000, 1, 1), actual);
    }

    @Test
    public void testDeserializationAsEmptyArrayEnabled()
    {
        LocalDate value = READER
                .with(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS,
                        DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT)
                .readValue("[]");
        assertNull(value);
    }

    /*
    /**********************************************************
    /* Custom format
    /**********************************************************
     */

    // for [datatype-jsr310#37]
    @Test
    public void testCustomFormat()
    {
        Wrapper w = MAPPER.readValue("{\"value\":\"2015_07_28T13:53+0300\"}", Wrapper.class);
        LocalDate date = w.value; 
        assertEquals(28, date.getDayOfMonth());
    }

    /*
    /**********************************************************
    /* Strict Custom format
    /**********************************************************
     */

    // for [modules-java8#148]
    @Test
    public void testStrictWithCustomFormat()
    {
        try {
            /*StrictWrapperWithFormat w =*/ MAPPER.readValue(
                    "{\"value\":\"2019-11-31\"}",
                    StrictWrapperWithFormat.class);
            fail("Should not pass");
        } catch (InvalidFormatException e) {
            verifyException(e, "Cannot deserialize value of type `java.time.LocalDate` from String");
            verifyException(e, "\"2019-11-31\"");
        }
    }

    @Test
    public void testStrictCustomFormatForInvalidFormat() throws Exception
    {
        try {
            /*StrictWrapperWithFormat w = */ MAPPER.readValue(
                "{\"value\":\"2019-11-30\"}",
                StrictWrapperWithFormat.class);
            fail("Should not pass");
        } catch (InvalidFormatException e) {
            // 25-Mar-2021, tatu: Really bad exception message we got... but
            //   it is what it is
            verifyException(e, "Cannot deserialize value of type `java.time.LocalDate` from String");
            verifyException(e, "\"2019-11-30\"");
        }
    }

    @Test
    public void testStrictCustomFormatForInvalidFormatWithEra() throws Exception
    {
        assertThrows(InvalidFormatException.class, () -> {
            /*StrictWrapperWithYearOfEra w =*/ MAPPER.readValue("{\"value\":\"2019-11-30\"}", StrictWrapperWithYearOfEra.class);
        });
    }

    @Test
    public void testStrictCustomFormatForInvalidDateWithEra() throws Exception
    {
        assertThrows(InvalidFormatException.class, () -> {
            /*StrictWrapperWithYearOfEra w =*/ MAPPER.readValue("{\"value\":\"2019-11-31 AD\"}", StrictWrapperWithYearOfEra.class);
        });
    }

    @Test
    public void testStrictCustomFormatForValidDateWithEra() throws Exception
    {
        StrictWrapperWithYearOfEra w = MAPPER.readValue("{\"value\":\"2019-11-30 AD\"}", StrictWrapperWithYearOfEra.class);

        assertEquals(w.value, LocalDate.of(2019, 11, 30));
    }

    @Test
    public void testStrictCustomFormatForInvalidFormatWithoutEra() throws Exception
    {
        assertThrows(InvalidFormatException.class, () -> {
            /*StrictWrapperWithYearWithoutEra w =*/ MAPPER.readValue("{\"value\":\"2019-11-30 AD\"}", StrictWrapperWithYearWithoutEra.class);
        });
    }

    @Test
    public void testStrictCustomFormatForInvalidDateWithoutEra() throws Exception
    {
        assertThrows(InvalidFormatException.class, () -> {
            /*StrictWrapperWithYearWithoutEra w =*/ MAPPER.readValue("{\"value\":\"2019-11-31\"}", StrictWrapperWithYearWithoutEra.class);
        });
    }

    @Test
    public void testStrictCustomFormatForValidDateWithoutEra() throws Exception
    {
        StrictWrapperWithYearWithoutEra w = MAPPER.readValue("{\"value\":\"2019-11-30\"}", StrictWrapperWithYearWithoutEra.class);

        assertEquals(w.value, LocalDate.of(2019, 11, 30));
    }

    /*
    /**********************************************************************
    /* Case-insensitive tests
    /**********************************************************************
     */

    @Test
    public void testDeserializationCaseInsensitiveEnabledOnValue()
    {
        ObjectMapper mapper = newMapperBuilder()
                .withConfigOverride(LocalDate.class, o -> o.setFormat(JsonFormat.Value
                        .forPattern("dd-MMM-yyyy")
                        .withFeature(Feature.ACCEPT_CASE_INSENSITIVE_VALUES))
                )
                .build();
        ObjectReader reader = mapper.readerFor(LocalDate.class);
        String[] jsons = new String[] { q("01-Jan-2000"), q("01-JAN-2000"),
                q("01-jan-2000")};
        for (String json : jsons) {
            expectSuccess(reader, LocalDate.of(2000, Month.JANUARY, 1), json);
        }
    }
    
    @Test
    public void testDeserializationCaseInsensitiveEnabled()
    {
        final ObjectMapper mapper = mapperBuilder()
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_VALUES)
                .withConfigOverride(LocalDate.class, o -> o.setFormat(
                        JsonFormat.Value.forPattern("dd-MMM-yyyy")))
                .build();
        ObjectReader reader = mapper.readerFor(LocalDate.class);
        String[] jsons = new String[] { q("01-Jan-2000"), q("01-JAN-2000"),
                q("01-jan-2000")};
        for(String json : jsons) {
            expectSuccess(reader, LocalDate.of(2000, Month.JANUARY, 1), json);
        }
    }
    
    @Test
    public void testDeserializationCaseInsensitiveDisabled()
    {
        final ObjectMapper mapper = mapperBuilder()
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_VALUES, false)
                .withConfigOverride(LocalDate.class, o -> o.setFormat(
                        JsonFormat.Value.forPattern("dd-MMM-yyyy")))
                .build();
        ObjectReader reader = mapper.readerFor(LocalDate.class);
        expectSuccess(reader, LocalDate.of(2000, Month.JANUARY, 1), q("01-Jan-2000"));
    }
    
    @Test
    public void testDeserializationCaseInsensitiveDisabled_InvalidDate()
    {
        final ObjectMapper mapper = mapperBuilder()
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_VALUES, false)
                .withConfigOverride(LocalDate.class, o -> JsonFormat.Value.forPattern("dd-MMM-yyyy"))
                .build();
        ObjectReader reader = mapper.readerFor(LocalDate.class);
        String[] jsons = new String[] { q("01-JAN-2000"), q("01-jan-2000")};
        for(String json : jsons) {
            try {
                reader.readValue(a2q(json));
                fail("expected DateTimeParseException");
            } catch (MismatchedInputException e) {
                verifyException(e, "Cannot deserialize value of type `java.time.LocalDate` from String ");
            }
        }
    }

    /*
    /**********************************************************************
    /* Tests for issue 58 - NUMBER_INT should be specified when deserializing
    /* LocalDate as EpochDays
    /**********************************************************************
     */

    @Test
    public void testLenientDeserializeFromNumberInt() {
        ObjectMapper mapper = newMapperBuilder()
                .withConfigOverride(LocalDate.class,
                        o -> o.setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.NUMBER_INT)))
                .build();

        assertEquals(LocalDate.of(1970, Month.MAY, 4),
            mapper.readValue("123", LocalDate.class));
    }

    @Test
    public void testStrictDeserializeFromNumberInt()
    {
        ObjectMapper mapper = newMapperBuilder()
                .withConfigOverride(LocalDate.class,
                        o -> o.setFormat(JsonFormat.Value.forLeniency(false)))
                .build();

        ShapeWrapper w = mapper.readValue("{\"date\":123}", ShapeWrapper.class);
        LocalDate localDate = w.date;

        assertEquals(LocalDate.of(1970, Month.MAY, 4), localDate);
    }

    @Test
    public void testStrictDeserializeFromString()
    {
        ObjectMapper mapper = newMapperBuilder()
                .withConfigOverride(LocalDate.class,
                        o -> o.setFormat(JsonFormat.Value.forLeniency(false)))
                .build();
        try {
            mapper.readValue("{\"value\":123}", Wrapper.class);
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot deserialize instance of `java.time.LocalDate`");
        }
    }

    /**********************************************************************
     *
     * coercion config test
     *
     /**********************************************************************
     */

    @Test
    public void testDeserializeFromIntegerWithCoercionActionFail() {
        ObjectMapper mapper = newMapperBuilder()
                .withCoercionConfig(LocalDate.class, cfg ->
                    cfg.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                ).build();
        MismatchedInputException exception = assertThrows(MismatchedInputException.class,
                () -> mapper.readValue("123", LocalDate.class));

        assertTrue(exception.getMessage().contains("Cannot coerce Integer value (123) to `java.time.LocalDate`"));
    }

    @Test
    public void testDeserializeFromEmptyStringWithCoercionActionFail() {
        ObjectMapper mapper = newMapperBuilder()
                .withCoercionConfig(LocalDate.class, cfg ->
                    cfg.setCoercion(CoercionInputShape.EmptyString, CoercionAction.Fail)
                ).build();

        MismatchedInputException exception = assertThrows(MismatchedInputException.class,
                () -> mapper.readValue(a2q("{'value':''}"), Wrapper.class));

        assertTrue(exception.getMessage().contains("Cannot coerce empty String (\"\") to `java.time.LocalDate`"));
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

   private void expectSuccess(ObjectReader reader, Object exp, String json) {
        final LocalDate value = reader.readValue(a2q(json));
        assertNotNull(value);
        assertEquals(exp, value);
    }
}
