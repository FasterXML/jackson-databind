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

import java.time.LocalTime;
import java.time.temporal.Temporal;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Feature;
import com.fasterxml.jackson.annotation.OptBoolean;

import tools.jackson.core.type.TypeReference;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;
import tools.jackson.databind.ext.javatime.MockObjectConfiguration;

import static org.junit.jupiter.api.Assertions.*;

public class LocalTimeDeserTest extends DateTimeTestBase
{
    private final ObjectMapper MAPPER = newMapper();
    private final ObjectReader READER = MAPPER.readerFor(LocalTime.class);

    private final TypeReference<Map<String, LocalTime>> MAP_TYPE_REF = new TypeReference<Map<String, LocalTime>>() { };

    final static class StrictWrapper {
        @JsonFormat(pattern="HH:mm", lenient = OptBoolean.FALSE)
        public LocalTime value;

        public StrictWrapper() { }
        public StrictWrapper(LocalTime v) { value = v; }
    }

    static class WrapperWithReadTimestampsAsNanosDisabled {
        @JsonFormat(
            without=Feature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS
        )
        public LocalTime value;

        public WrapperWithReadTimestampsAsNanosDisabled() { }
        public WrapperWithReadTimestampsAsNanosDisabled(LocalTime v) { value = v; }
    }

    static class WrapperWithReadTimestampsAsNanosEnabled {
        @JsonFormat(
            with=Feature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS
        )
        public LocalTime value;

        public WrapperWithReadTimestampsAsNanosEnabled() { }
        public WrapperWithReadTimestampsAsNanosEnabled(LocalTime v) { value = v; }
    }

    @Test
    public void testDeserializationAsTimestamp01() throws Exception
    {
        LocalTime time = LocalTime.of(15, 43);
        LocalTime value = READER.readValue("[15,43]");
        assertEquals(time, value, "The value is not correct.");
    }

    @Test
    public void testDeserializationAsTimestamp02() throws Exception
    {
        LocalTime time = LocalTime.of(9, 22, 57);
        LocalTime value = READER.readValue("[9,22,57]");
        assertEquals(time, value, "The value is not correct.");
    }

    @Test
    public void testDeserializationAsTimestamp03Nanoseconds() throws Exception
    {
        LocalTime value = READER
                .with(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .readValue("[9,22,0,57]");
        assertEquals(LocalTime.of(9, 22, 0, 57), value, "The value is not correct.");
    }

    @Test
    public void testDeserializationAsTimestamp03Milliseconds() throws Exception
    {
        LocalTime value = READER
                .without(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .readValue("[9,22,0,57]");
        assertEquals(LocalTime.of(9, 22, 0, 57000000), value, "The value is not correct.");
    }

    @Test
    public void testDeserializationAsTimestamp04Nanoseconds() throws Exception
    {
        LocalTime value = READER
                .with(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .readValue("[22,31,5,829837]");
        assertEquals(LocalTime.of(22, 31, 5, 829837), value, "The value is not correct.");
    }

    @Test
    public void testDeserializationAsTimestamp04Milliseconds01() throws Exception
    {
        LocalTime value = READER
                .without(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .readValue("[22,31,5,829837]");
        assertEquals(LocalTime.of(22, 31, 5, 829837), value, "The value is not correct.");
    }

    @Test
    public void testDeserializationAsTimestamp04Milliseconds02() throws Exception
    {
        LocalTime value = READER
                .without(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .readValue("[22,31,5,829]");
        assertEquals(LocalTime.of(22, 31, 5, 829000000), value, "The value is not correct.");
    }

    @Test
    public void testDeserializationAsTimestamp05Nanoseconds() throws Exception
    {
        ObjectReader wrapperReader =
            newMapper().readerFor(WrapperWithReadTimestampsAsNanosEnabled.class);
        WrapperWithReadTimestampsAsNanosEnabled actual = wrapperReader
            .readValue(a2q("{'value':[9,22,0,57]}"));
        assertEquals(LocalTime.of(9, 22, 0, 57), actual.value, "The value is not correct.");
    }

    @Test
    public void testDeserializationAsTimestamp05Milliseconds01() throws Exception
    {
        ObjectReader wrapperReader =
            newMapper().readerFor(WrapperWithReadTimestampsAsNanosDisabled.class);
        WrapperWithReadTimestampsAsNanosDisabled actual = wrapperReader
            .readValue(a2q("{'value':[9,22,0,57]}"));
        assertEquals(LocalTime.of(9, 22, 0, 57000000), actual.value, "The value is not correct.");
    }

    @Test
    public void testDeserializationAsTimestamp05Milliseconds02() throws Exception
    {
        ObjectReader wrapperReader =
            newMapper().readerFor(WrapperWithReadTimestampsAsNanosDisabled.class);
        WrapperWithReadTimestampsAsNanosDisabled actual = wrapperReader
            .readValue(a2q("{'value':[9,22,0,4257]}"));
        assertEquals(LocalTime.of(9, 22, 0, 4257), actual.value, "The value is not correct.");
    }

    @Test
    public void testDeserializationFromString() throws Exception
    {
        LocalTime time = LocalTime.of(15, 43);
        LocalTime value = READER.readValue('"' + time.toString() + '"');
        assertEquals(time, value, "The value is not correct.");

        expectSuccess(LocalTime.of(12, 0), "'12:00'");

        time = LocalTime.of(9, 22, 57);
        value = READER.readValue('"' + time.toString() + '"');
        assertEquals(time, value, "The value is not correct.");

        time = LocalTime.of(22, 31, 5, 829837);
        value = READER.readValue('"' + time.toString() + '"');
        assertEquals(time, value, "The value is not correct.");
    }

    @Test
    public void testBadDeserializationFromString() throws Throwable
    {
        try {
            READER.readValue(q("notalocaltime"));
            fail("Should nae pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot deserialize value of type `java.time.LocalTime` from String");
        }
    }

    @Test
    public void testDeserializationAsArrayDisabled() throws Throwable
    {
        try {
            READER.readValue(a2q("['12:00']"));
            fail("expected MismatchedInputException");
        } catch (MismatchedInputException e) {
            verifyException(e, "Unexpected token (VALUE_STRING) within Array");
        }

        // 25-Jul-2017, tatu: Why does it work? Is it supposed to?
        // works even without the feature enabled
        assertNull(READER.readValue("[]"));
    }

    @Test
    public void testDeserializationAsArrayEnabled() throws Throwable
    {
        LocalTime value = READER
               .with(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
               .readValue(a2q("['12:00']"));
        expect(LocalTime.of(12, 0), value);
    }

    @Test
    public void testDeserializationAsEmptyArrayEnabled() throws Throwable
    {
        LocalTime value = READER
                .with(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS,
                        DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT)
                .readValue(a2q("[]"));
        assertNull(value);
    }    

    @Test
    public void testDeserializationWithTypeInfo01() throws Exception
    {
        LocalTime time = LocalTime.of(22, 31, 5, 829837);
        ObjectMapper mapper = newMapperBuilder()
                .addMixIn(Temporal.class, MockObjectConfiguration.class)
                .build();
        Temporal value = mapper.readerFor(Temporal.class)
                .with(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .readValue("[\"" + LocalTime.class.getName() + "\",[22,31,5,829837]]");

        assertNotNull(value, "The value should not be null.");
        assertTrue(value instanceof LocalTime, "The value should be a LocalTime.");
        assertEquals(time, value, "The value is not correct.");
    }

    @Test
    public void testDeserializationWithTypeInfo02() throws Exception
    {
        LocalTime time = LocalTime.of(22, 31, 5, 422000000);

        ObjectMapper mapper = newMapperBuilder()
                .addMixIn(Temporal.class, MockObjectConfiguration.class)
                .build();
        Temporal value = mapper.readerFor(Temporal.class)
                .without(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .readValue("[\"" + LocalTime.class.getName() + "\",[22,31,5,422]]");
        assertTrue(value instanceof LocalTime, "The value should be a LocalTime.");
        assertEquals(time, value, "The value is not correct.");
    }

    @Test
    public void testDeserializationWithTypeInfo03() throws Exception
    {
        LocalTime time = LocalTime.of(22, 31, 5, 829837);
        ObjectMapper mapper = newMapperBuilder()
                .addMixIn(Temporal.class, MockObjectConfiguration.class)
                .build();
        Temporal value = mapper.readValue(
                "[\"" + LocalTime.class.getName() + "\",\"" + time.toString() + "\"]", Temporal.class
                );
        assertTrue(value instanceof LocalTime, "The value should be a LocalTime.");
        assertEquals(time, value, "The value is not correct.");
    }

    /*
    /**********************************************************
    /* Tests for empty string handling
    /**********************************************************
     */

    @Test
    public void testLenientDeserializeFromEmptyString() throws Exception {

        String key = "localTime";
        ObjectMapper mapper = newMapper();
        ObjectReader objectReader = mapper.readerFor(MAP_TYPE_REF);

        String dateValAsEmptyStr = "";

        String valueFromNullStr = mapper.writeValueAsString(asMap(key, null));
        Map<String, LocalTime> actualMapFromNullStr = objectReader.readValue(valueFromNullStr);
        LocalTime actualDateFromNullStr = actualMapFromNullStr.get(key);
        assertNull(actualDateFromNullStr);

        String valueFromEmptyStr = mapper.writeValueAsString(asMap(key, dateValAsEmptyStr));
        Map<String, LocalTime> actualMapFromEmptyStr = objectReader.readValue(valueFromEmptyStr);
        LocalTime actualDateFromEmptyStr = actualMapFromEmptyStr.get(key);
        assertEquals(null, actualDateFromEmptyStr, "empty string failed to deserialize to null with lenient setting");
    }

    @Test
    public void testStrictDeserializeFromEmptyString() throws Exception {

        final String key = "localTime";
        final ObjectMapper mapper = mapperBuilder()
                .withConfigOverride(LocalTime.class,
                        c -> c.setFormat(JsonFormat.Value.forLeniency(false)))
                .build();
        final ObjectReader objectReader = mapper.readerFor(MAP_TYPE_REF);

        String valueFromNullStr = mapper.writeValueAsString(asMap(key, null));
        Map<String, LocalTime> actualMapFromNullStr = objectReader.readValue(valueFromNullStr);
        assertNull(actualMapFromNullStr.get(key));

        String valueFromEmptyStr = mapper.writeValueAsString(asMap("date", ""));
        assertThrows(MismatchedInputException.class,
                () -> objectReader.readValue(valueFromEmptyStr));
    }

    /*
    /**********************************************************************
    /* Strict JsonFormat tests
    /**********************************************************************
     */

    // [modules-java8#148]: handle strict deserializaiton for date/time

    @Test
    public void testStrictCustomFormatInvalidTime() throws Exception
    {
        assertThrows(InvalidFormatException.class,
                () -> /*StrictWrapper w =*/ MAPPER.readValue("{\"value\":\"25:45\"}", StrictWrapper.class));
    }

    private void expectSuccess(Object exp, String aposJson) throws Exception {
        final LocalTime value = READER.readValue(a2q(aposJson));
        expect(exp, value);
    }

    private static void expect(Object exp, Object value) {
        assertEquals(exp, value, "The value is not correct.");
    }
}
