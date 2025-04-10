package tools.jackson.databind.ext.javatime.deser;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.fasterxml.jackson.annotation.JsonFormat;

import tools.jackson.core.type.TypeReference;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;
import tools.jackson.databind.ext.javatime.MockObjectConfiguration;
import tools.jackson.databind.ext.javatime.util.DecimalUtils;

import static org.junit.jupiter.api.Assertions.*;
import static tools.jackson.databind.ext.javatime.deser.InstantDeserializer.ISO8601_COLONLESS_OFFSET_REGEX;

public class InstantDeserTest extends DateTimeTestBase
{
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_INSTANT;
    private static final String CUSTOM_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private final TypeReference<Map<String, Instant>> MAP_TYPE_REF = new TypeReference<Map<String, Instant>>() { };

    static class Wrapper {
        @JsonFormat(
                // 22-Jun-2015, tatu: I'll be damned if I understand why pattern does not
                //    work here... but it doesn't. Someone with better date-fu has to come
                //   and fix this; until then I will only verify that we can force textual
                //    representation here
                //pattern="YYYY-mm-dd",
                shape=JsonFormat.Shape.STRING
        )
        public Instant value;

        public Wrapper() { }
        public Wrapper(Instant v) { value = v; }
    }

    static class WrapperWithCustomPattern {
        @JsonFormat(
                pattern = CUSTOM_PATTERN,
                shape=JsonFormat.Shape.STRING,
                timezone = "UTC"
        )
        public Instant valueInUTC;

        public WrapperWithCustomPattern() { }
        public WrapperWithCustomPattern(Instant v) {
            valueInUTC = v;
        }
    }

    static class WrapperWithReadTimestampsAsNanosDisabled {
        @JsonFormat(
            without=JsonFormat.Feature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS
        )
        public Instant value;

        public WrapperWithReadTimestampsAsNanosDisabled() { }
        public WrapperWithReadTimestampsAsNanosDisabled(Instant v) { value = v; }
    }

    static class WrapperWithReadTimestampsAsNanosEnabled {
        @JsonFormat(
            with=JsonFormat.Feature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS
        )
        public Instant value;

        public WrapperWithReadTimestampsAsNanosEnabled() { }
        public WrapperWithReadTimestampsAsNanosEnabled(Instant v) { value = v; }
    }

    private final ObjectMapper MAPPER = newMapper();
    private final ObjectReader READER = MAPPER.readerFor(Instant.class);

    /*
    /**********************************************************************
    /* Basic deserialization from floating point value (seconds with fractions)
    /**********************************************************************
     */
    
    @Test
    public void testDeserializationAsFloat01() throws Exception {
        assertEquals(Instant.ofEpochSecond(0L),
                READER.readValue("0.000000000"));
    }

    @Test
    public void testDeserializationAsFloat02() throws Exception {
        assertEquals(Instant.ofEpochSecond(123456789L, 183917322),
                READER.readValue("123456789.183917322"));
    }

    @Test
    public void testDeserializationAsFloat03() throws Exception
    {
        Instant date = Instant.now();
        Instant value = READER.readValue(
                DecimalUtils.toDecimal(date.getEpochSecond(), date.getNano()));
        assertEquals(date, value);
    }

    /**
     * Test the upper-bound of Instant.
     */
    @Test
    public void testDeserializationAsFloatEdgeCase01() throws Exception
    {
        String input = Instant.MAX.getEpochSecond() + ".999999999";
        Instant value = READER.readValue(input);
        assertEquals(value, Instant.MAX);
        assertEquals(Instant.MAX.getEpochSecond(), value.getEpochSecond());
        assertEquals(999999999, value.getNano());
    }

    /**
     * Test the lower-bound of Instant.
     */
    @Test
    public void testDeserializationAsFloatEdgeCase02() throws Exception
    {
        String input = Instant.MIN.getEpochSecond() + ".0";
        Instant value = READER.readValue(input);
        assertEquals(value, Instant.MIN);
        assertEquals(Instant.MIN.getEpochSecond(), value.getEpochSecond());
        assertEquals(0, value.getNano());
    }

    @Test
    public void testDeserializationAsFloatEdgeCase03() throws Exception
    {
        // Instant can't go this low
        String input = Instant.MIN.getEpochSecond() + ".1";
        assertThrows(DateTimeException.class, () -> READER.readValue(input));
    }

    /*
     * InstantDeserializer currently uses BigDecimal.longValue() which has surprising behavior
     * for numbers outside the range of Long.  Numbers less than 1e64 will result in the lower 64 bits.
     * Numbers at or above 1e64 will always result in zero.
     */
    @Test
    public void testDeserializationAsFloatEdgeCase04() throws Exception
    {
        // 1ns beyond the upper-bound of Instant.
        String input = (Instant.MAX.getEpochSecond() + 1) + ".0";
        assertThrows(DateTimeException.class, () -> READER.readValue(input));
    }

    @Test
    public void testDeserializationAsFloatEdgeCase05() throws Exception
    {
        // 1ns beyond the lower-bound of Instant.
        String input = (Instant.MIN.getEpochSecond() - 1) + ".0";
        assertThrows(DateTimeException.class, () -> READER.readValue(input));
    }

    @Test
    public void testDeserializationAsFloatEdgeCase06() throws Exception
    {
        // Into the positive zone where everything becomes zero.
        Instant value = READER.readValue("1e64");
        assertEquals(0, value.getEpochSecond());
    }

    @Test
    public void testDeserializationAsFloatEdgeCase07() throws Exception
    {
        // Into the negative zone where everything becomes zero.
        Instant value = READER.readValue("-1e64");
        assertEquals(0, value.getEpochSecond());
    }

    /**
     * Numbers with very large exponents can take a long time, but still result in zero.
     * https://github.com/FasterXML/jackson-databind/issues/2141
     */
    @Timeout(value = 100, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testDeserializationAsFloatEdgeCase08() throws Exception
    {
        Instant value = READER.readValue("1e10000000");
        assertEquals(0, value.getEpochSecond());
    }

    @Timeout(value = 100, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testDeserializationAsFloatEdgeCase09() throws Exception
    {
        Instant value = READER.readValue("-1e10000000");
        assertEquals(0, value.getEpochSecond());
    }

    /**
     * Same for large negative exponents.
     */
    @Timeout(value = 100, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testDeserializationAsFloatEdgeCase10() throws Exception
    {
        Instant value = READER.readValue("1e-10000000");
        assertEquals(0, value.getEpochSecond());
    }

    @Timeout(value = 100, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testDeserializationAsFloatEdgeCase11() throws Exception
    {
        Instant value = READER.readValue("-1e-10000000");
        assertEquals(0, value.getEpochSecond());
    }

    /*
    /**********************************************************************
    /* Basic deserialization from Integer (long) value, as nanos
    /**********************************************************************
     */
    
    @Test
    public void testDeserializationAsInt01Nanoseconds() throws Exception
    {
        Instant date = Instant.ofEpochSecond(0L);
        Instant value = READER
                .with(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .readValue("0");
        assertEquals(date, value);
    }

    @Test
    public void testDeserializationAsInt02Nanoseconds() throws Exception
    {
        final long ts = 123456789L;
        Instant date = Instant.ofEpochSecond(ts);
        Instant value = READER
                .with(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .readValue(String.valueOf(ts));
        assertEquals(date, value);
    }

    @Test
    public void testDeserializationAsInt03Nanoseconds() throws Exception
    {
        Instant date = Instant.now();
        date = date.minus(date.getNano(), ChronoUnit.NANOS);

        Instant value = READER
                .with(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .readValue(Long.toString(date.getEpochSecond()));
        assertEquals(date, value);
    }

    @Test
    public void testDeserializationAsInt04Nanoseconds() throws Exception
    {
        ObjectReader reader = MAPPER.readerFor(WrapperWithReadTimestampsAsNanosEnabled.class);
        Instant date = Instant.now();
        date = date.minus(date.getNano(), ChronoUnit.NANOS);
        WrapperWithReadTimestampsAsNanosEnabled expected =
            new WrapperWithReadTimestampsAsNanosEnabled(date);
        WrapperWithReadTimestampsAsNanosEnabled actual = reader.readValue(
            a2q("{'value':" + date.getEpochSecond() + "}"));
        assertEquals(expected.value, actual.value);
    }

    /*
    /**********************************************************************
    /* Basic deserialization from Integer (long) value, as milliseconds
    /**********************************************************************
     */

    @Test
    public void testDeserializationAsInt01Milliseconds() throws Exception
    {
        Instant date = Instant.ofEpochSecond(0L);
        Instant value = READER
                .without(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .readValue("0");
        assertEquals(date, value);
    }

    @Test
    public void testDeserializationAsInt02Milliseconds() throws Exception
    {
        Instant date = Instant.ofEpochSecond(123456789L, 422000000);
        Instant value = READER
                .without(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .readValue("123456789422");
        assertEquals(date, value);
    }

    @Test
    public void testDeserializationAsInt03Milliseconds() throws Exception
    {
        Instant date = Instant.now();
        date = date.minus(date.getNano(), ChronoUnit.NANOS);

        Instant value = READER
                .without(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .readValue(Long.toString(date.toEpochMilli()));
        assertEquals(date, value);
    }

    @Test
    public void testDeserializationAsInt04Milliseconds() throws Exception
    {
        ObjectReader reader = MAPPER.readerFor(WrapperWithReadTimestampsAsNanosDisabled.class);
        Instant date = Instant.now();
        date = date.minus(date.getNano(), ChronoUnit.NANOS);
        WrapperWithReadTimestampsAsNanosDisabled expected =
            new WrapperWithReadTimestampsAsNanosDisabled(date);
        WrapperWithReadTimestampsAsNanosDisabled actual = reader.readValue(
            a2q("{'value':" + date.toEpochMilli() + "}"));
        assertEquals(expected.value, actual.value);
    }

    /*
    /**********************************************************************
    /* Basic deserialization from String (ISO-8601 timestamps)
    /**********************************************************************
     */
    
    @Test
    public void testDeserializationAsString01() throws Exception
    {
        Instant date = Instant.ofEpochSecond(0L);
        Instant value = READER.readValue('"' + FORMATTER.format(date) + '"');
        assertEquals(date, value);
    }

    @Test
    public void testDeserializationAsString02() throws Exception
    {
        Instant date = Instant.ofEpochSecond(123456789L, 183917322);
        Instant value = READER.readValue('"' + FORMATTER.format(date) + '"');
        assertEquals(date, value);
    }

    @Test
    public void testDeserializationAsString03() throws Exception
    {
        Instant date = Instant.now();

        Instant value = READER.readValue('"' + FORMATTER.format(date) + '"');
        assertEquals(date, value);
    }

    /*
    /**********************************************************************
    /* Polymorphic deserialization, numeric timestamps
    /**********************************************************************
     */
    
    @Test
    public void testDeserializationWithTypeInfo01() throws Exception
    {
        Instant date = Instant.ofEpochSecond(123456789L, 183917322);
        ObjectMapper m = newMapperBuilder()
                .addMixIn(Temporal.class, MockObjectConfiguration.class)
                .build();
        Temporal value = m.readValue(
                "[\"" + Instant.class.getName() + "\",123456789.183917322]", Temporal.class
                );
        assertTrue(value instanceof Instant, "The value should be an Instant.");
        assertEquals(date, value);
    }

    @Test
    public void testDeserializationWithTypeInfo02() throws Exception
    {
        Instant date = Instant.ofEpochSecond(123456789L, 0);
        ObjectMapper m = newMapperBuilder()
                .enable(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .addMixIn(Temporal.class, MockObjectConfiguration.class)
                .build();
        Temporal value = m.readValue(
                "[\"" + Instant.class.getName() + "\",123456789]", Temporal.class
                );
        assertTrue(value instanceof Instant, "The value should be an Instant.");
        assertEquals(date, value);
    }

    @Test
    public void testDeserializationWithTypeInfo03() throws Exception
    {
        Instant date = Instant.ofEpochSecond(123456789L, 422000000);
        ObjectMapper m = newMapperBuilder()
                .disable(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .addMixIn(Temporal.class, MockObjectConfiguration.class)
                .build();
        Temporal value = m.readValue(
                "[\"" + Instant.class.getName() + "\",123456789422]", Temporal.class
                );

        assertTrue(value instanceof Instant, "The value should be an Instant.");
        assertEquals(date, value);
    }

    @Test
    public void testDeserializationWithTypeInfo04() throws Exception
    {
        Instant date = Instant.now();
        ObjectMapper m = newMapperBuilder()
                .addMixIn(Temporal.class, MockObjectConfiguration.class)
                .build();
        Temporal value = m.readValue(
                "[\"" + Instant.class.getName() + "\",\"" + FORMATTER.format(date) + "\"]", Temporal.class
                );
        assertTrue(value instanceof Instant, "The value should be an Instant.");
        assertEquals(date, value);
    }

    /*
    /**********************************************************************
    /* Deserialization with custom pattern overrides (for String values)
    /**********************************************************************
     */
    
    @Test
    public void testCustomPatternWithAnnotations01() throws Exception
    {
        final Wrapper input = new Wrapper(Instant.ofEpochMilli(0));
        String json = MAPPER.writeValueAsString(input);
        assertEquals(a2q("{'value':'1970-01-01T00:00:00Z'}"), json);

        Wrapper result = MAPPER.readValue(json, Wrapper.class);
        assertEquals(input.value, result.value);
    }

    // [datatype-jsr310#69]
    @Test
    public void testCustomPatternWithAnnotations02() throws Exception
    {
        //Test date is pushed one year after start of the epoch just to avoid possible issues with UTC-X TZs which could
        //push the instant before tha start of the epoch
        final Instant instant = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneOffset.UTC).plusYears(1).toInstant();
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(CUSTOM_PATTERN);
        final String valueInUTC = formatter.withZone(ZoneOffset.UTC).format(instant);

        final WrapperWithCustomPattern input = new WrapperWithCustomPattern(instant);
        String json = MAPPER.writeValueAsString(input);

        assertTrue(json.contains(a2q("'valueInUTC':'" + valueInUTC + "'")),
            "Instant in UTC timezone was not serialized as expected.");

        WrapperWithCustomPattern result = MAPPER.readValue(json, WrapperWithCustomPattern.class);
        assertEquals(input.valueInUTC, result.valueInUTC,
            "Instant in UTC timezone was not deserialized as expected.");
    }

    /*
    /**********************************************************************
    /* Deserialization, timezone overrides
    /**********************************************************************
     */

    // [jackson-modules-java8#18]
    @Test
    public void testDeserializationFromStringWithZeroZoneOffset01() throws Exception {
        Instant date = Instant.now();
        String json = formatWithZeroZoneOffset(date, "+00:00");
        Instant result = READER.readValue(json);
        assertEquals(date, result);
    }

    @Test
    public void testDeserializationFromStringWithZeroZoneOffset02() throws Exception {
        Instant date = Instant.now();
        String json = formatWithZeroZoneOffset(date, "+0000");
        Instant result = READER.readValue(json);
        assertEquals(date, result);
    }

    @Test
    public void testDeserializationFromStringWithZeroZoneOffset03() throws Exception {
        Instant date = Instant.now();
        String json = formatWithZeroZoneOffset(date, "+00");
        Instant result = READER.readValue(json);
        assertEquals(date, result);
    }

    @Test
    public void testDeserializationFromStringWithZeroZoneOffset04() throws Exception {
        assumeInstantCanParseOffsets();
        Instant date = Instant.now();
        String json = formatWithZeroZoneOffset(date, "+00:30");
        Instant result = READER.readValue(json);
        assertNotEquals(date, result);
    }

    @Test
    public void testDeserializationFromStringWithZeroZoneOffset05() throws Exception {
        assumeInstantCanParseOffsets();
        Instant date = Instant.now();
        String json = formatWithZeroZoneOffset(date, "+01:30");
        Instant result = READER.readValue(json);
        assertNotEquals(date, result);
    }

    @Test
    public void testDeserializationFromStringWithZeroZoneOffset06() throws Exception {
        assumeInstantCanParseOffsets();
        Instant date = Instant.now();
        String json = formatWithZeroZoneOffset(date, "-00:00");
        Instant result = READER.readValue(json);
        assertEquals(date, result);
    }

    private String formatWithZeroZoneOffset(Instant date, String offset){
        return '"' + FORMATTER.format(date).replaceFirst("Z$", offset) + '"';
    }

    private static void assumeInstantCanParseOffsets() {
        // 27-Jan-2025, tatu: We are on JDK 17 so true always
        // DateTimeFormatter.ISO_INSTANT didn't handle offsets until JDK 12+.
        ;
    }

    /*
    /**********************************************************************
    /* Deserialization, misc other
    /**********************************************************************
     */
    
    // [datatype-jsr310#16]
    @Test
    public void testDeserializationFromStringAsNumber() throws Exception
    {
        // First, baseline test with floating-point numbers
        Instant inst = Instant.now();
        String json = MAPPER.writer()
                .with(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .with(DateTimeFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .writeValueAsString(inst);
        Instant result = READER.readValue(json);
        assertNotNull(result);
        assertEquals(result, inst);

        // but then quoted as JSON String
        result = READER.readValue(String.format("\"%s\"", json));
        assertNotNull(result);
        assertEquals(result, inst);
    }

    // [datatype-jsr310#79]
    @Test
    public void testRoundTripOfInstantAndJavaUtilDate() throws Exception
    {
        ObjectMapper mapper = newMapperBuilder()
                .configure(DateTimeFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false)
                .configure(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false)
                .build();

        Instant givenInstant = LocalDate.of(2016, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant();
        String json = mapper.writeValueAsString(java.util.Date.from(givenInstant));
        Instant actual = mapper.readValue(json, Instant.class);

        assertEquals(givenInstant, actual);
    }

        /*
    /**********************************************************
    /* Tests for empty string handling
    /**********************************************************
     */

    @Test
    public void testLenientDeserializeFromEmptyString() throws Exception {

        String key = "duration";
        ObjectMapper mapper = newMapper();
        ObjectReader objectReader = mapper.readerFor(MAP_TYPE_REF);

        String dateValAsNullStr = null;
        String dateValAsEmptyStr = "";

        String valueFromNullStr = mapper.writeValueAsString(asMap(key, dateValAsNullStr));
        Map<String, Duration> actualMapFromNullStr = objectReader.readValue(valueFromNullStr);
        Duration actualDateFromNullStr = actualMapFromNullStr.get(key);
        assertNull(actualDateFromNullStr);

        String valueFromEmptyStr = mapper.writeValueAsString(asMap(key, dateValAsEmptyStr));
        Map<String, Duration> actualMapFromEmptyStr = objectReader.readValue(valueFromEmptyStr);
        Duration actualDateFromEmptyStr = actualMapFromEmptyStr.get(key);
        assertEquals(null, actualDateFromEmptyStr, "empty string failed to deserialize to null with lenient setting");
    }

    @Test
    public void testStrictDeserializeFromEmptyString() throws Exception {

        final String key = "instant";
        final ObjectMapper mapper = mapperBuilder()
                .withConfigOverride(Instant.class,
                        o -> o.setFormat(JsonFormat.Value.forLeniency(false)))
                .build();

        final ObjectReader objectReader = mapper.readerFor(MAP_TYPE_REF);

        String valueFromNullStr = mapper.writeValueAsString(asMap(key, null));
        Map<String, Instant> actualMapFromNullStr = objectReader.readValue(valueFromNullStr);
        assertNull(actualMapFromNullStr.get(key));

        String valueFromEmptyStr = mapper.writeValueAsString(asMap(key, ""));
        assertThrows(MismatchedInputException.class, () -> objectReader.readValue(valueFromEmptyStr));
    }
    
    /*
    /************************************************************************
    /* Tests for InstantDeserializer.ISO8601_COLONLESS_OFFSET_REGEX
    /************************************************************************
    */
    @Test
    public void testISO8601ColonlessRegexFindsOffset() {
        Matcher matcher = ISO8601_COLONLESS_OFFSET_REGEX.matcher("2000-01-01T12:00+0100");

        assertTrue(matcher.find(), "Matcher finds +0100 as an colonless offset");
        assertEquals(matcher.group(), "+0100", "Matcher groups +0100 as an colonless offset");
    }

    @Test
    public void testISO8601ColonlessRegexFindsOffsetWithTZ() {
        Matcher matcher = ISO8601_COLONLESS_OFFSET_REGEX.matcher("2000-01-01T12:00+0100[Europe/Paris]");

        assertTrue(matcher.find(), "Matcher finds +0100 as an colonless offset");
        assertEquals(matcher.group(), "+0100", "Matcher groups +0100 as an colonless offset");
    }

    @Test
    public void testISO8601ColonlessRegexDoesNotAffectNegativeYears() {
        Matcher matcher = ISO8601_COLONLESS_OFFSET_REGEX.matcher("-2000-01-01T12:00+01:00[Europe/Paris]");

        assertFalse(matcher.find(), "Matcher does not find -2000 (years) as an offset without colon");
    }

    @Test
    public void testISO8601ColonlessRegexDoesNotAffectNegativeYearsWithColonless() {
        Matcher matcher = ISO8601_COLONLESS_OFFSET_REGEX.matcher("-2000-01-01T12:00+0100[Europe/Paris]");

        assertTrue(matcher.find(), "Matcher finds +0100 as an colonless offset");
        assertEquals(matcher.group(), "+0100", "Matcher groups +0100 as an colonless offset");
    }
}
