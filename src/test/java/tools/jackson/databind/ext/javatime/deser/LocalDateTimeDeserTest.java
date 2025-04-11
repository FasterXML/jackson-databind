/*
 * Copyright 2013 FasterXML.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */

package tools.jackson.databind.ext.javatime.deser;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.temporal.Temporal;
import java.util.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Feature;
import com.fasterxml.jackson.annotation.OptBoolean;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.type.TypeReference;

import tools.jackson.databind.*;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.deser.DeserializationProblemHandler;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;
import tools.jackson.databind.ext.javatime.MockObjectConfiguration;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;

public class LocalDateTimeDeserTest
    extends DateTimeTestBase
{
    private final static ObjectMapper MAPPER = newMapper();
    private final static ObjectReader READER = MAPPER.readerFor(LocalDateTime.class);

    private final static ObjectMapper STRICT_MAPPER = mapperBuilder()
        .withConfigOverride(LocalDateTime.class,
                c -> c.setFormat(JsonFormat.Value.forLeniency(false)))
        .build();

    private final ObjectReader READER_USING_TIME_ZONE = JsonMapper.builder()
        .enable(DateTimeFeature.USE_TIME_ZONE_FOR_LENIENT_DATE_PARSING)
        .build()
        .readerFor(LocalDateTime.class);

    private final TypeReference<Map<String, LocalDateTime>> MAP_TYPE_REF = new TypeReference<Map<String, LocalDateTime>>() { };

    final static class StrictWrapper {
        @JsonFormat(pattern="yyyy-MM-dd HH:mm",
                lenient = OptBoolean.FALSE)
        public LocalDateTime value;

        public StrictWrapper() { }
        public StrictWrapper(LocalDateTime v) { value = v; }
    }

    final static class StrictWrapperWithYearOfEra {
        @JsonFormat(pattern="yyyy-MM-dd HH:mm G",
                lenient = OptBoolean.FALSE)
        public LocalDateTime value;

        public StrictWrapperWithYearOfEra() { }
        public StrictWrapperWithYearOfEra(LocalDateTime v) { value = v; }
    }

    final static class StrictWrapperWithYearWithoutEra {
        @JsonFormat(pattern="uuuu-MM-dd HH:mm",
                lenient = OptBoolean.FALSE)
        public LocalDateTime value;

        public StrictWrapperWithYearWithoutEra() { }
        public StrictWrapperWithYearWithoutEra(LocalDateTime v) { value = v; }
    }

    static class WrapperWithReadTimestampsAsNanosDisabled {
        @JsonFormat(
            without=Feature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS
        )
        public LocalDateTime value;

        public WrapperWithReadTimestampsAsNanosDisabled() { }
        public WrapperWithReadTimestampsAsNanosDisabled(LocalDateTime v) { value = v; }
    }

    static class WrapperWithReadTimestampsAsNanosEnabled {
        @JsonFormat(
            with=Feature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS
        )
        public LocalDateTime value;

        public WrapperWithReadTimestampsAsNanosEnabled() { }
        public WrapperWithReadTimestampsAsNanosEnabled(LocalDateTime v) { value = v; }
    }

    /*
    /**********************************************************
    /* Tests for deserializing from int array
    /**********************************************************
     */

    @Test
    public void testDeserializationAsTimestamp01()
    {
        LocalDateTime value = READER.readValue("[1986,1,17,15,43]");
        LocalDateTime time = LocalDateTime.of(1986, Month.JANUARY, 17, 15, 43);
        assertEquals(time, value, "The value is not correct.");
    }

    @Test
    public void testDeserializationAsTimestamp02()
    {
        LocalDateTime value = READER.readValue("[2013,8,21,9,22,57]");
        LocalDateTime time = LocalDateTime.of(2013, Month.AUGUST, 21, 9, 22, 57);
        assertEquals(time, value, "The value is not correct.");
    }

    @Test
    public void testDeserializationAsTimestamp03Nanoseconds()
    {
        ObjectReader r = READER
                .with(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS);
        LocalDateTime value = r.readValue("[2013,8,21,9,22,0,57]");
        LocalDateTime time = LocalDateTime.of(2013, Month.AUGUST, 21, 9, 22, 0, 57);
        assertEquals(time, value, "The value is not correct.");
    }

    @Test
    public void testDeserializationAsTimestamp03Milliseconds()
    {
        ObjectReader r = READER
                .without(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS);
        LocalDateTime value = r.readValue("[2013,8,21,9,22,0,57]");
        LocalDateTime time = LocalDateTime.of(2013, Month.AUGUST, 21, 9, 22, 0, 57000000);
        assertEquals(time, value, "The value is not correct.");
    }

    @Test
    public void testDeserializationAsTimestamp04Nanoseconds()
    {
        ObjectReader r = MAPPER.readerFor(LocalDateTime.class)
                .with(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS);
        LocalDateTime value = r.readValue("[2005,11,5,22,31,5,829837]");
        LocalDateTime time = LocalDateTime.of(2005, Month.NOVEMBER, 5, 22, 31, 5, 829837);
        assertEquals(time, value, "The value is not correct.");
    }

    @Test
    public void testDeserializationAsTimestamp04Milliseconds01()
    {
        ObjectReader r = READER
                .without(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS);
        LocalDateTime value = r.readValue("[2005,11,5,22,31,5,829837]");

        LocalDateTime time = LocalDateTime.of(2005, Month.NOVEMBER, 5, 22, 31, 5, 829837);
        assertEquals(time, value, "The value is not correct.");
    }

    @Test
    public void testDeserializationAsTimestamp04Milliseconds02()
    {
        ObjectReader r = READER
                .without(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS);
        LocalDateTime value = r.readValue("[2005,11,5,22,31,5,829]");
        LocalDateTime time = LocalDateTime.of(2005, Month.NOVEMBER, 5, 22, 31, 5, 829000000);
        assertEquals(time, value, "The value is not correct.");
    }

    @Test
    public void testDeserializationAsTimestamp05Nanoseconds() throws Exception
    {
        ObjectReader r = MAPPER.readerFor(WrapperWithReadTimestampsAsNanosEnabled.class);
        WrapperWithReadTimestampsAsNanosEnabled actual =
            r.readValue(a2q("{'value':[2013,8,21,9,22,0,57]}"));
        LocalDateTime time = LocalDateTime.of(2013, Month.AUGUST, 21, 9, 22, 0, 57);
        assertEquals(time, actual.value, "The value is not correct.");
    }

    @Test
    public void testDeserializationAsTimestamp05Milliseconds01() throws Exception
    {
        ObjectReader r = MAPPER.readerFor(WrapperWithReadTimestampsAsNanosDisabled.class);
        WrapperWithReadTimestampsAsNanosDisabled actual =
            r.readValue(a2q("{'value':[2013,8,21,9,22,0,57]}"));
        LocalDateTime time = LocalDateTime.of(2013, Month.AUGUST, 21, 9, 22, 0, 57000000);
        assertEquals(time, actual.value, "The value is not correct.");
    }

    @Test
    public void testDeserializationAsTimestamp05Milliseconds02() throws Exception
    {
        ObjectReader r = MAPPER.readerFor(WrapperWithReadTimestampsAsNanosDisabled.class);
        WrapperWithReadTimestampsAsNanosDisabled actual =
            r.readValue(a2q("{'value':[2013,8,21,9,22,0,4257]}"));
        LocalDateTime time = LocalDateTime.of(2013, Month.AUGUST, 21, 9, 22, 0, 4257);
        assertEquals(time, actual.value, "The value is not correct.");
    }

    /*
    /**********************************************************
    /* Tests for deserializing from textual representation
    /**********************************************************
     */
    
    @Test
    public void testDeserializationAsString01()
    {
        LocalDateTime exp = LocalDateTime.of(1986, Month.JANUARY, 17, 15, 43);
        LocalDateTime value = READER.readValue(q(exp.toString()));
        assertEquals(exp, value, "The value is not correct.");

        assertEquals(LocalDateTime.of(2000, Month.JANUARY, 1, 12, 0),
                READER.readValue(q("2000-01-01T12:00")),
                "The value is not correct.");
    }

    @Test
    public void testDeserializationAsString02()
    {
        LocalDateTime time = LocalDateTime.of(2013, Month.AUGUST, 21, 9, 22, 57);
        LocalDateTime value = MAPPER.readValue(q(time.toString()), LocalDateTime.class);
        assertEquals(time, value, "The value is not correct.");
    }

    @Test
    public void testDeserializationAsString03()
    {
        LocalDateTime time = LocalDateTime.of(2005, Month.NOVEMBER, 5, 22, 31, 5, 829837);
        LocalDateTime value = MAPPER.readValue(q(time.toString()), LocalDateTime.class);
        assertEquals(time, value, "The value is not correct.");
    }

    /*
    /**********************************************************
    /* Tests for deserializing from textual representation,
    /* fail cases, leniency checking
    /**********************************************************
     */

    // [modules-java#94]: "Z" offset MAY be allowed, requires leniency
    @Test
    public void testAllowZuluIfLenient()
    {
        final LocalDateTime EXP = LocalDateTime.of(2020, Month.OCTOBER, 22, 4, 16, 20, 504000000);
        final String input = q("2020-10-22T04:16:20.504Z");
        final ObjectReader r = MAPPER.readerFor(LocalDateTime.class);

        // First, defaults:
        assertEquals(EXP, r.readValue(input), "The value is not correct.");

        // but ensure that global timezone setting doesn't matter
        LocalDateTime value = r.with(TimeZone.getTimeZone(Z_CHICAGO))
                .readValue(input);
        assertEquals(EXP, value, "The value is not correct.");

        value = r.with(TimeZone.getTimeZone(Z_BUDAPEST))
                .readValue(input);
        assertEquals(EXP, value, "The value is not correct.");
    }

    @ParameterizedTest
    @CsvSource({
        "UTC,             2020-10-22T04:16:20.504Z, 2020-10-22T04:16:20.504",
        "Europe/Budapest, 2020-10-22T04:16:20.504Z, 2020-10-22T06:16:20.504",
        "Europe/Budapest, 2020-10-25T00:16:20.504Z, 2020-10-25T02:16:20.504",
        "Europe/Budapest, 2020-10-25T01:16:20.504Z, 2020-10-25T02:16:20.504",
        "America/Chicago, 2020-10-22T04:16:20.504Z, 2020-10-21T23:16:20.504",
        "America/Chicago, 2020-11-01T06:16:20.504Z, 2020-11-01T01:16:20.504",
        "America/Chicago, 2020-11-01T07:16:20.504Z, 2020-11-01T01:16:20.504"
    })
    public void testUseTimeZoneForZuluIfEnabled(TimeZone zone, String string, LocalDateTime expected) throws Exception
    {
        ObjectReader reader = READER_USING_TIME_ZONE.with(zone);
        LocalDateTime value = reader.readValue(q(string));
        assertEquals(expected, value);
    }

    // [modules-java#94]: "Z" offset not allowed if strict mode
    @Test
    public void testFailOnZuluIfStrict()
    {
        try {
            STRICT_MAPPER.readValue(q("2020-10-22T00:16:20.504Z"), LocalDateTime.class);
            fail("Should not pass");
        } catch (InvalidFormatException e) {
            verifyException(e, "Cannot deserialize value of type ");
            verifyException(e, "Should not contain offset when 'strict' mode");
        }
    }

    @Test
    public void testBadDeserializationAsString01()
    {
        try {
            READER.readValue(q("notalocaldatetime"));
            fail("expected fail");
        } catch (InvalidFormatException e) {
            verifyException(e, "Cannot deserialize value of type");
            verifyException(e, "from String \"");
        }
    }

    /*
    /**********************************************************
    /* Tests for empty string handling
    /**********************************************************
     */

    @Test
    public void testLenientDeserializeFromEmptyString()
    {
        String key = "datetime";
        ObjectMapper mapper = newMapper();
        ObjectReader objectReader = mapper.readerFor(MAP_TYPE_REF);

        String dateValAsNullStr = null;
        String dateValAsEmptyStr = "";

        String valueFromNullStr = mapper.writeValueAsString(asMap(key, dateValAsNullStr));
        Map<String, LocalDateTime> actualMapFromNullStr = objectReader.readValue(valueFromNullStr);
        LocalDateTime actualDateFromNullStr = actualMapFromNullStr.get(key);
        assertNull(actualDateFromNullStr);

        String valueFromEmptyStr = mapper.writeValueAsString(asMap(key, dateValAsEmptyStr));
        Map<String, LocalDateTime> actualMapFromEmptyStr = objectReader.readValue(valueFromEmptyStr);
        LocalDateTime actualDateFromEmptyStr = actualMapFromEmptyStr.get(key);
        assertEquals(actualDateFromNullStr, actualDateFromEmptyStr, "empty string failed to deserialize to null with lenient setting");
    }

    @Test
    public void testStrictDeserializeFromEmptyString() throws Exception {

        final String key = "datetime";
        final ObjectReader objectReader = STRICT_MAPPER.readerFor(MAP_TYPE_REF);
        final String dateValAsNullStr = null;

        // even with strict, null value should be deserialized without throwing an exception
        String valueFromNullStr = STRICT_MAPPER.writeValueAsString(asMap(key, dateValAsNullStr));
        Map<String, LocalDateTime> actualMapFromNullStr = objectReader.readValue(valueFromNullStr);
        assertNull(actualMapFromNullStr.get(key));

        String dateValAsEmptyStr = "";
        // TODO: nothing stops us from writing an empty string, maybe there should be a check there too?
        String valueFromEmptyStr = STRICT_MAPPER.writeValueAsString(asMap("date", dateValAsEmptyStr));
        // with strict, deserializing an empty string is not permitted
        try {
            objectReader.readValue(valueFromEmptyStr);
            fail("Should nae pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot deserialize instance of `java.time.LocalDateTime` out of ");
        }
    }

    /*
    /**********************************************************
    /* Tests for alternate array handling
    /**********************************************************
     */

    @Test
    public void testDeserializationAsArrayDisabled() throws Throwable
    {
        try {
            READER.readValue("[\"2000-01-01T12:00\"]");
        } catch (MismatchedInputException e) {
            verifyException(e, "Unexpected token (VALUE_STRING) within Array");
        }
    }
    
    @Test
    public void testDeserializationAsEmptyArrayDisabled() throws Throwable
    {
        // works even without the feature enabled
        assertNull(READER.readValue("[]"));
    }

    @Test
    public void testDeserializationAsArrayEnabled() throws Throwable
    {
        LocalDateTime value = READER
                .with(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
                .readValue("[\"2000-01-01T12:00\"]");
        assertEquals(LocalDateTime.of(2000, 1, 1, 12, 0, 0, 0),
                value, "The value is not correct.");
    }
    
    @Test
    public void testDeserializationAsEmptyArrayEnabled() throws Throwable
    {
        LocalDateTime value = READER
               .with(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
               .with(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT)
               .readValue("[]");
        assertNull(value);
    }

    /*
    /**********************************************************
    /* Tests for polymorphic handling
    /**********************************************************
     */
    
    @Test
    public void testDeserializationWithTypeInfo01() throws Exception
    {
        LocalDateTime time = LocalDateTime.of(2005, Month.NOVEMBER, 5, 22, 31, 5, 829837);
        final ObjectMapper m = newMapperBuilder()
                .addMixIn(Temporal.class, MockObjectConfiguration.class)
                .build();
        Temporal value = m.readerFor(Temporal.class)
                .with(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .readValue(
                "[\"" + LocalDateTime.class.getName() + "\",[2005,11,5,22,31,5,829837]]");
        assertTrue(value instanceof LocalDateTime, "The value should be a LocalDateTime.");
        assertEquals(time, value, "The value is not correct.");
    }

    @Test
    public void testDeserializationWithTypeInfo02() throws Exception
    {
        LocalDateTime time = LocalDateTime.of(2005, Month.NOVEMBER, 5, 22, 31, 5, 422000000);

        final ObjectMapper m = newMapperBuilder()
                .addMixIn(Temporal.class, MockObjectConfiguration.class)
                .build();
        Temporal value = m.readerFor(Temporal.class)
                .without(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .readValue(
                "[\"" + LocalDateTime.class.getName() + "\",[2005,11,5,22,31,5,422]]");
        assertTrue(value instanceof LocalDateTime, "The value should be a LocalDateTime.");
        assertEquals(time, value, "The value is not correct.");
    }

    @Test
    public void testDeserializationWithTypeInfo03() throws Exception
    {
        LocalDateTime time = LocalDateTime.of(2005, Month.NOVEMBER, 5, 22, 31, 5, 829837);
        final ObjectMapper m = newMapperBuilder().
                addMixIn(Temporal.class, MockObjectConfiguration.class)
                .build();
        Temporal value = m.readValue(
                "[\"" + LocalDateTime.class.getName() + "\",\"" + time.toString() + "\"]", Temporal.class
        );
        assertTrue(value instanceof LocalDateTime, "The value should be a LocalDateTime.");
        assertEquals(time, value, "The value is not correct.");
    }

    /*
    /**********************************************************
    /* Tests for `DeserialiazationProblemHandler` usage
    /**********************************************************
     */
    
    @Test
    public void testDateTimeExceptionIsHandled() throws Throwable
    {
        LocalDateTime now = LocalDateTime.now();
        DeserializationProblemHandler handler = new DeserializationProblemHandler() {
            @Override
            public Object handleWeirdStringValue(DeserializationContext ctxt, Class<?> targetType,
                   String valueToConvert, String failureMsg) {
                if (LocalDateTime.class == targetType) {
                    if ("now".equals(valueToConvert)) {
                        return now;
                    }
                }
                return NOT_HANDLED;
            }
        };
        ObjectMapper handledMapper = mapperBuilder().addHandler(handler).build();
        assertEquals(now, handledMapper.readValue(q("now"), LocalDateTime.class));
    }

    @Test
    public void testUnexpectedTokenIsHandled() throws Throwable
    {
        LocalDateTime now = LocalDateTime.now();
        DeserializationProblemHandler handler = new DeserializationProblemHandler() {
            @Override
            public Object handleUnexpectedToken(DeserializationContext ctxt, JavaType targetType,
                   JsonToken t, JsonParser p, String failureMsg) {
                if (targetType.hasRawClass(LocalDateTime.class)) {
                    if (t.isBoolean()) {
                        return now;
                    }
                }
                return NOT_HANDLED;
            }
        };
        ObjectMapper handledMapper = mapperBuilder().addHandler(handler).build();
        assertEquals(now, handledMapper.readValue("true", LocalDateTime.class));
    }
    
    /*
    /**********************************************************
    /* Tests for specific reported issues
    /**********************************************************
     */

    // [datatype-jrs310#54]
    @Test
    public void testDeserializeToDate() throws Exception
    {
        ObjectMapper m = newMapperBuilder()
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();
        String localDateTimeJson = m.writeValueAsString(LocalDateTime.of(1999,10,12,13,45,5));
        assertEquals("\"1999-10-12T13:45:05\"", localDateTimeJson);
        Date date = m.readValue(localDateTimeJson,Date.class);
        assertNotNull(date);
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(date.getTime());
        assertEquals(1999, cal.get(Calendar.YEAR));
        assertEquals(12, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(13, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(45, cal.get(Calendar.MINUTE));
        assertEquals(5, cal.get(Calendar.SECOND));
    }

    // [modules-java8#47]: should indicate why timestamp won't work
    @Test
    public void testDeserilizeFromSimpleTimestamp() throws Exception
    {
        ObjectReader r = MAPPER.readerFor(LocalDateTime.class);
        LocalDateTime value;
        try {
            value = r.readValue("1235");
            fail("Should not succeed, instead got: "+value);
        } catch (MismatchedInputException e) {
            verifyException(e, "raw timestamp (1235) not allowed for `java.time.LocalDateTime`");
        }
    }

    /*
    /**********************************************************************
    /* Case-insensitive tests
    /**********************************************************************
     */

    // [modules-java8#80]: handle case-insensitive date/time
    @Test
    public void testDeserializationCaseInsensitiveEnabledOnValue() throws Throwable
    {
        final ObjectMapper mapper = newMapperBuilder()
                .withConfigOverride(LocalDateTime.class, o -> o.setFormat(JsonFormat.Value
                        .forPattern("dd-MMM-yyyy HH:mm")
                        .withFeature(Feature.ACCEPT_CASE_INSENSITIVE_VALUES)))
                .build();
        ObjectReader reader = mapper.readerFor(LocalDateTime.class);
        String[] jsons = new String[] {"'01-Jan-2000 13:14'","'01-JAN-2000 13:14'", "'01-jan-2000 13:14'"};
        for(String json : jsons) {
            expectSuccess(reader, LocalDateTime.of(2000, Month.JANUARY, 1, 13, 14), json);
        }
    }

    @Test
    public void testDeserializationCaseInsensitiveEnabled() throws Throwable
    {
        final ObjectMapper mapper = newMapperBuilder()
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_VALUES, true)
                .withConfigOverride(LocalDateTime.class, o -> o.setFormat(
                        JsonFormat.Value.forPattern("dd-MMM-yyyy HH:mm")))
                .build();
        ObjectReader reader = mapper.readerFor(LocalDateTime.class);
        String[] jsons = new String[] {"'01-Jan-2000 13:45'","'01-JAN-2000 13:45'", "'01-jan-2000 13:45'"};
        for(String json : jsons) {
            expectSuccess(reader, LocalDateTime.of(2000, Month.JANUARY, 1, 13, 45), json);
        }
    }

    @Test
    public void testDeserializationCaseInsensitiveDisabled() throws Throwable
    {
        final ObjectMapper mapper = newMapperBuilder()
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_VALUES, false)
                .withConfigOverride(LocalDateTime.class, o -> o.setFormat(
                        JsonFormat.Value.forPattern("dd-MMM-yyyy HH:mm")))
                .build();
        ObjectReader reader = mapper.readerFor(LocalDateTime.class);
        expectSuccess(reader, LocalDateTime.of(2000, Month.JANUARY, 1, 13, 45),
                q("01-Jan-2000 13:45"));
    }

    @Test
    public void testDeserializationCaseInsensitiveDisabled_InvalidDate() throws Throwable
    {
        final ObjectMapper mapper = newMapperBuilder()
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_VALUES, false)
                .withConfigOverride(LocalDateTime.class, o -> o.setFormat(
                        JsonFormat.Value.forPattern("dd-MMM-yyyy")))
                .build();
        ObjectReader reader = mapper.readerFor(LocalDateTime.class);
        String[] jsons = new String[] {"'01-JAN-2000'", "'01-jan-2000'"};
        for(String json : jsons) {
            try {
                reader.readValue(a2q(json));
                fail("Should nae pass");
            } catch (MismatchedInputException e) {
                verifyException(e, "Failed to deserialize `java.time.LocalDateTime` (with format");
            }
        }
    }

    /*
    /**********************************************************************
    /* Strict JsonFormat tests
    /**********************************************************************
     */

    // [modules-java8#148]: handle strict deserializaiton for date/time
    @Test
    public void testStrictCustomFormatForInvalidFormat() throws Exception
    {
        assertThrows(InvalidFormatException.class,
            () -> /*StrictWrapper w =*/ MAPPER.readValue("{\"value\":\"2019-11-30 15:45\"}", StrictWrapper.class));
    }

    @Test
    public void testStrictCustomFormatForInvalidFormatWithEra() throws Exception
    {
        assertThrows(InvalidFormatException.class,
            () -> /*StrictWrapperWithYearOfEra w =*/ MAPPER.readValue("{\"value\":\"2019-11-30 15:45\"}", StrictWrapperWithYearOfEra.class));
    }

    @Test
    public void testStrictCustomFormatForInvalidDateWithEra() throws Exception
    {
        assertThrows(InvalidFormatException.class,
            () -> /*StrictWrapperWithYearOfEra w =*/ MAPPER.readValue("{\"value\":\"2019-11-31 15:45 AD\"}", StrictWrapperWithYearOfEra.class));
    }

    @Test
    public void testStrictCustomFormatForInvalidTimeWithEra() throws Exception
    {
        assertThrows(InvalidFormatException.class,
            () -> /*StrictWrapperWithYearOfEra w =*/ MAPPER.readValue("{\"value\":\"2019-11-30 25:45 AD\"}", StrictWrapperWithYearOfEra.class));
    }

    @Test
    public void testStrictCustomFormatForInvalidDateAndTimeWithEra() throws Exception
    {
        assertThrows(InvalidFormatException.class,
            () -> /*StrictWrapperWithYearOfEra w =*/ MAPPER.readValue("{\"value\":\"2019-11-31 25:45 AD\"}", StrictWrapperWithYearOfEra.class));

    }

    @Test
    public void testStrictCustomFormatValidDateAndTimeWithEra() throws Exception
    {
        StrictWrapperWithYearOfEra w = MAPPER.readValue("{\"value\":\"2019-11-30 20:45 AD\"}", StrictWrapperWithYearOfEra.class);

        assertEquals(w.value, LocalDateTime.of(2019, 11, 30, 20, 45));
    }

    @Test
    public void testStrictCustomFormatForInvalidFormatWithoutEra() throws Exception
    {
        assertThrows(InvalidFormatException.class,
            () -> /*StrictWrapperWithYearWithoutEra w =*/ MAPPER.readValue("{\"value\":\"2019-11-30 15:45 AD\"}", StrictWrapperWithYearWithoutEra.class));
    }

    @Test
    public void testStrictCustomFormatForInvalidTimeWithoutEra() throws Exception
    {
        assertThrows(InvalidFormatException.class,
            () -> /*StrictWrapperWithYearWithoutEra w =*/ MAPPER.readValue("{\"value\":\"2019-11-30 25:45\"}", StrictWrapperWithYearWithoutEra.class));
    }

    @Test
    public void testStrictCustomFormatForInvalidDateWithoutEra() throws Exception
    {
        assertThrows(InvalidFormatException.class,
            () -> /*StrictWrapperWithYearWithoutEra w =*/ MAPPER.readValue("{\"value\":\"2019-11-31 15:45\"}", StrictWrapperWithYearWithoutEra.class));
    }

    @Test
    public void testStrictCustomFormatForInvalidDateAndTimeWithoutEra() throws Exception
    {
        assertThrows(InvalidFormatException.class,
            () -> /*StrictWrapperWithYearWithoutEra w =*/ MAPPER.readValue("{\"value\":\"2019-11-31 25:45\"}", StrictWrapperWithYearWithoutEra.class));
    }

    @Test
    public void testStrictCustomFormatForValidDateAndTimeWithoutEra() throws Exception
    {
        StrictWrapperWithYearWithoutEra w = MAPPER.readValue("{\"value\":\"2019-11-30 20:45\"}",
                StrictWrapperWithYearWithoutEra.class);

        assertEquals(w.value, LocalDateTime.of(2019, 11, 30, 20, 45));
    }

    // [datatype-jsr310#124] Issue serializing and deserializing LocalDateTime.MAX and LocalDateTime.MIN
    @Test
    public void testDeserializationOfLocalDateTimeMax() throws Exception
    {
        ObjectMapper enabledMapper = mapperBuilder()
                .enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS).build();
        _testLocalDateTimeRoundTrip(enabledMapper, LocalDateTime.MAX);
        _testLocalDateTimeRoundTrip(enabledMapper, LocalDateTime.MIN);

        ObjectMapper disabledMapper = mapperBuilder()
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS).build();
        _testLocalDateTimeRoundTrip(disabledMapper, LocalDateTime.MAX);
        _testLocalDateTimeRoundTrip(disabledMapper, LocalDateTime.MIN);
    }

    private void _testLocalDateTimeRoundTrip(ObjectMapper mapper, LocalDateTime localDateTime)
        throws Exception
    {
        String ser = mapper.writeValueAsString(localDateTime);
        LocalDateTime result = mapper.readValue(ser, LocalDateTime.class);
        assertEquals(localDateTime, result);
    }

    private void expectSuccess(ObjectReader reader, Object exp, String json) throws IOException {
        final LocalDateTime value = reader.readValue(a2q(json));
        assertNotNull(value, "The value should not be null.");
        assertEquals(exp,  value, "The value is not correct.");
    }
}
