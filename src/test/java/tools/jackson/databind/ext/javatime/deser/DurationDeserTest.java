package tools.jackson.databind.ext.javatime.deser;

import java.math.BigInteger;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Feature;

import tools.jackson.core.type.TypeReference;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;
import tools.jackson.databind.ext.javatime.MockObjectConfiguration;

import static org.junit.jupiter.api.Assertions.*;

public class DurationDeserTest extends DateTimeTestBase
{
    private final ObjectReader READER = newMapper().readerFor(Duration.class);

    private final TypeReference<Map<String, Duration>> MAP_TYPE_REF = new TypeReference<Map<String, Duration>>() { };

    final static class Wrapper {
        public Duration value;

        public Wrapper() { }
        public Wrapper(Duration v) { value = v; }
    }

    static class WrapperWithReadTimestampsAsNanosDisabled {
        @JsonFormat(
            without=Feature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS
        )
        public Duration value;

        public WrapperWithReadTimestampsAsNanosDisabled() { }
        public WrapperWithReadTimestampsAsNanosDisabled(Duration v) { value = v; }
    }

    static class WrapperWithReadTimestampsAsNanosEnabled {
        @JsonFormat(
            with=Feature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS
        )
        public Duration value;

        public WrapperWithReadTimestampsAsNanosEnabled() { }
        public WrapperWithReadTimestampsAsNanosEnabled(Duration v) { value = v; }
    }

    @Test
    public void testDeserializationAsFloat01() throws Exception
    {
        Duration value = READER.with(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .readValue("60.0");
        assertEquals(Duration.ofSeconds(60L, 0), value, "The value is not correct.");
    }

    @Test
    public void testDeserializationAsFloat02() throws Exception
    {
        Duration value = READER.without(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .readValue("60.0");
        assertEquals(Duration.ofSeconds(60L, 0), value, "The value is not correct.");
    }

    @Test
    public void testDeserializationAsFloat03() throws Exception
    {
        Duration value = READER.with(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .readValue("13498.000008374");
        assertEquals(Duration.ofSeconds(13498L, 8374), value, "The value is not correct.");
    }

    @Test
    public void testDeserializationAsFloat04() throws Exception
    {
        Duration value = READER.without(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .readValue("13498.000008374");
        assertEquals(Duration.ofSeconds(13498L, 8374), value, "The value is not correct.");
    }

    /**
     * Test the upper-bound of Duration.
     */
    @Test
    public void testDeserializationAsFloatEdgeCase01() throws Exception
    {
        String input = Long.MAX_VALUE + ".999999999";
        Duration value = READER.without(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                                 .readValue(input);
        assertEquals(Long.MAX_VALUE, value.getSeconds());
        assertEquals(999999999, value.getNano());
    }

    /**
     * Test the lower-bound of Duration.
     */
    @Test
    public void testDeserializationAsFloatEdgeCase02() throws Exception
    {
        String input = Long.MIN_VALUE + ".0";
        Duration value = READER.without(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                                 .readValue(input);
        assertEquals(Long.MIN_VALUE, value.getSeconds());
        assertEquals(0, value.getNano());
    }

    @Test
    public void testDeserializationAsFloatEdgeCase03() throws Exception
    {
        // Duration can't go this low
        assertThrows(ArithmeticException.class, () -> {
            READER.without(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                    .readValue(Long.MIN_VALUE + ".1");
        });
    }

    /*
     * DurationDeserializer currently uses BigDecimal.longValue() which has surprising behavior
     * for numbers outside the range of Long.  Numbers less than 1e64 will result in the lower 64 bits.
     * Numbers at or above 1e64 will always result in zero.
     */

    @Test
    public void testDeserializationAsFloatEdgeCase04() throws Exception
    {
        // Just beyond the upper-bound of Duration.
        String input = new BigInteger(Long.toString(Long.MAX_VALUE)).add(BigInteger.ONE) + ".0";
        Duration value = READER.without(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                                 .readValue(input);
        assertEquals(Long.MIN_VALUE, value.getSeconds());  // We've turned a positive number into negative duration!
    }

    @Test
    public void testDeserializationAsFloatEdgeCase05() throws Exception
    {
        // Just beyond the lower-bound of Duration.
        String input = new BigInteger(Long.toString(Long.MIN_VALUE)).subtract(BigInteger.ONE) + ".0";
        Duration value = READER.without(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                                 .readValue(input);
        assertEquals(Long.MAX_VALUE, value.getSeconds());  // We've turned a negative number into positive duration!
    }

    @Test
    public void testDeserializationAsFloatEdgeCase06() throws Exception
    {
        // Into the positive zone where everything becomes zero.
        String input = "1e64";
        Duration value = READER.without(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .readValue(input);
        assertEquals(0, value.getSeconds());
    }

    @Test
    public void testDeserializationAsFloatEdgeCase07() throws Exception
    {
        // Into the negative zone where everything becomes zero.
        String input = "-1e64";
        Duration value = READER.without(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .readValue(input);
        assertEquals(0, value.getSeconds());
    }

    /**
     * Numbers with very large exponents can take a long time, but still result in zero.
     * https://github.com/FasterXML/jackson-databind/issues/2141
     */
    @Timeout(value = 100, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testDeserializationAsFloatEdgeCase08() throws Exception
    {
        String input = "1e10000000";
        Duration value = READER.without(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                                 .readValue(input);
        assertEquals(0, value.getSeconds());
    }

    @Timeout(value = 100, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testDeserializationAsFloatEdgeCase09() throws Exception
    {
        String input = "-1e10000000";
        Duration value = READER.without(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .readValue(input);
        assertEquals(0, value.getSeconds());
    }

    /**
     * Same for large negative exponents.
     */
    @Timeout(value = 100, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testDeserializationAsFloatEdgeCase10() throws Exception
    {
        String input = "1e-10000000";
        Duration value = READER.without(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .readValue(input);
        assertEquals(0, value.getSeconds());
    }

    @Timeout(value = 100, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testDeserializationAsFloatEdgeCase11() throws Exception
    {
        String input = "-1e-10000000";
        Duration value = READER.without(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .readValue(input);
        assertEquals(0, value.getSeconds());
    }

    @Test
    public void testDeserializationAsInt01() throws Exception
    {
        Duration value = READER.with(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .readValue("60");
        assertEquals(Duration.ofSeconds(60L, 0), value, "The value is not correct.");
    }

    @Test
    public void testDeserializationAsInt02() throws Exception
    {
        Duration value = READER.without(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .readValue("60000");
        assertEquals(Duration.ofSeconds(60L, 0), value, "The value is not correct.");
    }

    @Test
    public void testDeserializationAsInt03() throws Exception
    {
        Duration value = READER.with(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .readValue("13498");
        assertEquals(Duration.ofSeconds(13498L, 0), value, "The value is not correct.");
    }

    @Test
    public void testDeserializationAsInt04() throws Exception
    {
        Duration value = READER.without(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .readValue("13498000");
        assertEquals(Duration.ofSeconds(13498L, 0), value, "The value is not correct.");
    }

    @Test
    public void testDeserializationAsInt05() throws Exception
    {
        ObjectReader reader = newMapper().readerFor(WrapperWithReadTimestampsAsNanosEnabled.class);
        WrapperWithReadTimestampsAsNanosEnabled expected =
            new WrapperWithReadTimestampsAsNanosEnabled(Duration.ofSeconds(13498L, 0));
        WrapperWithReadTimestampsAsNanosEnabled actual =
            reader.readValue(wrapperPayload(13498));
        assertEquals(expected.value, actual.value, "The value is not correct.");
    }

    @Test
    public void testDeserializationAsInt06() throws Exception
    {
        ObjectReader reader = newMapper().readerFor(WrapperWithReadTimestampsAsNanosDisabled.class);
        WrapperWithReadTimestampsAsNanosDisabled expected =
            new WrapperWithReadTimestampsAsNanosDisabled(Duration.ofSeconds(13498L, 0));
        WrapperWithReadTimestampsAsNanosDisabled actual =
            reader.readValue(wrapperPayload(13498000));
        assertEquals(expected.value, actual.value, "The value is not correct.");
    }

    @Test
    public void testDeserializationAsString01() throws Exception
    {
        Duration exp = Duration.ofSeconds(60L, 0);
        Duration value = READER.readValue('"' + exp.toString() + '"');
        assertEquals(exp, value, "The value is not correct.");
    }

    @Test
    public void testDeserializationAsString02() throws Exception
    {
        Duration exp = Duration.ofSeconds(13498L, 8374);
        Duration value = READER.readValue('"' + exp.toString() + '"');
        assertEquals(exp, value, "The value is not correct.");
    }

    @Test
    public void testDeserializationAsString03() throws Exception
    {
        assertNull(READER.readValue("\"   \""), "The value should be null.");
    }

    @Test
    public void testDeserializationWithTypeInfo01() throws Exception
    {
        Duration duration = Duration.ofSeconds(13498L, 8374);

        String prefix = "[\"" + Duration.class.getName() + "\",";

        ObjectMapper mapper = newMapperBuilder()
                .addMixIn(TemporalAmount.class, MockObjectConfiguration.class)
                .build();
        TemporalAmount value = mapper.readerFor(TemporalAmount.class)
                .without(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .readValue(prefix + "13498.000008374]");

        assertTrue(value instanceof Duration, "The value should be a Duration.");
        assertEquals(duration, value, "The value is not correct.");
    }

    @Test
    public void testDeserializationWithTypeInfo02() throws Exception
    {
        String prefix = "[\"" + Duration.class.getName() + "\",";
        ObjectMapper mapper = newMapperBuilder()
                .addMixIn(TemporalAmount.class, MockObjectConfiguration.class)
                .build();
        TemporalAmount value = mapper.readerFor(TemporalAmount.class)
                .with(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .readValue(prefix + "13498]");
        assertTrue(value instanceof Duration, "The value should be a Duration.");
        assertEquals(Duration.ofSeconds(13498L), value, "The value is not correct.");
    }

    @Test
    public void testDeserializationWithTypeInfo03() throws Exception
    {
        String prefix = "[\"" + Duration.class.getName() + "\",";
        ObjectMapper mapper = newMapperBuilder()
                .addMixIn(TemporalAmount.class, MockObjectConfiguration.class)
                .build();
        TemporalAmount value = mapper
                .readerFor(TemporalAmount.class)
                .without(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .readValue(prefix + "13498837]");
        assertTrue(value instanceof Duration, "The value should be a Duration.");
        assertEquals(Duration.ofSeconds(13498L, 837000000), value, "The value is not correct.");
    }

    @Test
    public void testDeserializationWithTypeInfo04() throws Exception
    {
        Duration duration = Duration.ofSeconds(13498L, 8374);
        String prefix = "[\"" + Duration.class.getName() + "\",";
        ObjectMapper mapper = newMapperBuilder()
            .addMixIn(TemporalAmount.class, MockObjectConfiguration.class)
            .build();
        TemporalAmount value = mapper.readerFor(TemporalAmount.class)
                .readValue(prefix + '"' + duration.toString() + "\"]");
        assertTrue(value instanceof Duration, "The value should be a Duration.");
        assertEquals(duration, value, "The value is not correct.");
    }
    
    @Test
    public void testDeserializationAsArrayDisabled() throws Exception {
    	Duration exp = Duration.ofSeconds(13498L, 8374);
    	try {
	        READER.readValue("[\"" + exp.toString() + "\"]");
	        fail("expected MismatchedInputException");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot deserialize value of type `java.time.Duration` from Array value");
        }
    }

    @Test
    public void testDeserializationAsEmptyArrayDisabled() throws Throwable
    {
        try {
            READER.readValue("[]");
            fail("expected MismatchedInputException");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot deserialize value of type `java.time.Duration` from Array value");
        }
        try {
            READER.with(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
        	        .readValue("[]");
            fail("expected MismatchedInputException");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot deserialize value of type `java.time.Duration` from Array value");
        }
    }

    @Test
    public void testDeserializationAsArrayEnabled() throws Exception {
        Duration exp = Duration.ofSeconds(13498L, 8374);
        Duration value = newMapper().readerFor(Duration.class)
                .with(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
                .readValue("[\"" + exp.toString() + "\"]");
          assertEquals(exp, value, "The value is not correct.");
    }
   
    @Test
    public void testDeserializationAsEmptyArrayEnabled() throws Throwable
    {
        Duration value = newMapper().readerFor(Duration.class)
                .with(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS,
                        DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT)
                .readValue("[]");
        assertNull(value);
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

        final String key = "duration";
        final ObjectMapper mapper = mapperBuilder()
                .withConfigOverride(Duration.class,
                        o -> o.setFormat(JsonFormat.Value.forLeniency(false)))
                .build();

        final ObjectReader objectReader = mapper.readerFor(MAP_TYPE_REF);
        final String dateValAsNullStr = null;

        // even with strict, null value should be deserialized without throwing an exception
        String valueFromNullStr = mapper.writeValueAsString(asMap(key, dateValAsNullStr));
        Map<String, Duration> actualMapFromNullStr = objectReader.readValue(valueFromNullStr);
        assertNull(actualMapFromNullStr.get(key));

        String dateValAsEmptyStr = "";
        String valueFromEmptyStr = mapper.writeValueAsString(asMap(key, dateValAsEmptyStr));
        assertThrows(MismatchedInputException.class, () -> objectReader.readValue(valueFromEmptyStr));
    }

    /*
    /**********************************************************
    /* Tests for custom patterns (modules-java8#184)
    /**********************************************************
     */

    @Test
    public void shouldDeserializeInNanos_whenNanosUnitAsPattern_andValueIsInteger() throws Exception {
        ObjectMapper mapper = _mapperForPatternOverride("NANOS");
        ObjectReader reader = mapper.readerFor(Wrapper.class);
        Wrapper wrapper = reader.readValue(wrapperPayload(25));
        assertEquals(Duration.ofNanos(25),  wrapper.value);
    }

    @Test
    public void shouldDeserializeInMicros_whenMicrosUnitAsPattern_andValueIsInteger() throws Exception {
        ObjectMapper mapper = _mapperForPatternOverride("MICROS");
        ObjectReader reader = mapper.readerFor(Wrapper.class);

        Wrapper wrapper = reader.readValue(wrapperPayload(25));

        assertEquals(Duration.of(25, ChronoUnit.MICROS),  wrapper.value);
    }

    @Test
    public void shouldDeserializeInMillis_whenMillisUnitAsPattern_andValueIsInteger() throws Exception {
        ObjectMapper mapper = _mapperForPatternOverride("MILLIS");
        ObjectReader reader = mapper.readerFor(Wrapper.class);

        Wrapper wrapper = reader.readValue(wrapperPayload(25));

        assertEquals(Duration.ofMillis(25),  wrapper.value);
    }

    @Test
    public void shouldDeserializeInSeconds_whenSecondsUnitAsPattern_andValueIsInteger() throws Exception {
        ObjectMapper mapper = _mapperForPatternOverride("SECONDS");
        ObjectReader reader = mapper.readerFor(Wrapper.class);

        Wrapper wrapper = reader.readValue(wrapperPayload(25));

        assertEquals(Duration.ofSeconds(25), wrapper.value);
    }

    @Test
    public void shouldDeserializeInMinutes_whenMinutesUnitAsPattern_andValueIsInteger() throws Exception {
        ObjectMapper mapper = _mapperForPatternOverride("MINUTES");
        ObjectReader reader = mapper.readerFor(Wrapper.class);

        Wrapper wrapper = reader.readValue(wrapperPayload(25));

        assertEquals(Duration.ofMinutes(25), wrapper.value);
    }

    @Test
    public void shouldDeserializeInHours_whenHoursUnitAsPattern_andValueIsInteger() throws Exception {
        ObjectMapper mapper = _mapperForPatternOverride("HOURS");
        ObjectReader reader = mapper.readerFor(Wrapper.class);

        Wrapper wrapper = reader.readValue(wrapperPayload(25));

        assertEquals(Duration.ofHours(25),  wrapper.value);
    }

    @Test
    public void shouldDeserializeInHalfDays_whenHalfDaysUnitAsPattern_andValueIsInteger() throws Exception {
        ObjectMapper mapper = _mapperForPatternOverride("HALF_DAYS");
        ObjectReader reader = mapper.readerFor(Wrapper.class);

        Wrapper wrapper = reader.readValue(wrapperPayload(25));

        assertEquals(Duration.of(25, ChronoUnit.HALF_DAYS),  wrapper.value);
    }

    @Test
    public void shouldDeserializeInDays_whenDaysUnitAsPattern_andValueIsInteger() throws Exception {
        ObjectMapper mapper = _mapperForPatternOverride("DAYS");
        ObjectReader reader = mapper.readerFor(Wrapper.class);

        Wrapper wrapper = reader.readValue(wrapperPayload(25));

        assertEquals(Duration.ofDays(25),  wrapper.value);
    }

    @Test
    public void shouldIgnoreUnitPattern_whenValueIsFloat() throws Exception {
        ObjectMapper mapper = _mapperForPatternOverride("MINUTES");
        ObjectReader reader = mapper.readerFor(Wrapper.class);

        Wrapper wrapper = reader.readValue(wrapperPayload(25.5));

        assertEquals(Duration.parse("PT25.5S"),  wrapper.value);
    }

    @Test
    public void shouldIgnoreUnitPattern_whenValueIsString() throws Exception {
        ObjectMapper mapper = _mapperForPatternOverride("MINUTES");
        ObjectReader reader = mapper.readerFor(Wrapper.class);

        Wrapper wrapper = reader.readValue("{\"value\":\"PT25S\"}");

        assertEquals(Duration.parse("PT25S"),  wrapper.value);
    }

    @Test
    public void shouldFailForInvalidPattern() throws Exception {
        ObjectMapper mapper = _mapperForPatternOverride("Nanos");
        ObjectReader reader = mapper.readerFor(Wrapper.class);

        try {
            /*Wrapper wrapper =*/ reader.readValue(wrapperPayload(25));
            fail("Should not allow invalid 'pattern'");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Bad 'pattern' definition (\"Nanos\")");
            verifyException(e, "expected one of [");
        }
    }

    private String wrapperPayload(Number number) {
        return "{\"value\":" + number + "}";
    }

    private ObjectMapper _mapperForPatternOverride(String patternStr) {
        return mapperBuilder()
                .withConfigOverride(Duration.class,
                        o -> o.setFormat(JsonFormat.Value.forPattern(patternStr)))
                .build();
    }
}
