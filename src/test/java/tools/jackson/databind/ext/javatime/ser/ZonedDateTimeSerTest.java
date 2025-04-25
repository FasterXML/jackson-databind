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

package tools.jackson.databind.ext.javatime.ser;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFormat;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;
import tools.jackson.databind.ext.javatime.MockObjectConfiguration;
import tools.jackson.databind.ext.javatime.util.DecimalUtils;

import static org.junit.jupiter.api.Assertions.*;

public class ZonedDateTimeSerTest
    extends DateTimeTestBase
{
    private static final DateTimeFormatter FORMATTER_WITHOUT_ZONEID = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private static final ZoneId Z1 = ZoneId.of("America/Chicago");

    private static final ZoneId Z2 = ZoneId.of("America/Anchorage");

    private static final ZoneId Z3 = ZoneId.of("America/Los_Angeles");

    private static final ZoneId UTC = ZoneOffset.UTC;

    private static final ZoneId DEFAULT_TZ = UTC;

    private static final ZoneId FIX_OFFSET = ZoneId.of("-08:00");

    final static class Wrapper {
        @JsonFormat(pattern="yyyy_MM_dd HH:mm:ss(Z)",
                shape=JsonFormat.Shape.STRING)
        public ZonedDateTime value;

        public Wrapper() { }
        public Wrapper(ZonedDateTime v) { value = v; }
    }

    final static class WrapperNumeric {
        @JsonFormat(pattern="yyyyMMddHHmmss",
                shape=JsonFormat.Shape.STRING,
                timezone = "UTC")
        public ZonedDateTime value;

        public WrapperNumeric() { }
        public WrapperNumeric(ZonedDateTime v) { value = v; }
    }

    private final ObjectMapper MAPPER = newMapper();
    private final ObjectReader READER = MAPPER.readerFor(ZonedDateTime.class);
    private final ObjectMapper MAPPER_WITH_DEFAULT_TZ = newMapper(TimeZone.getDefault());

    
    @Test
    public void testSerializationAsTimestamp01Nanoseconds() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(0L), Z1);
        String value = MAPPER.writer()
                .with(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .with(DateTimeFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .writeValueAsString(date);
        assertEquals("0.0", value);
    }

    @Test
    public void testSerializationAsTimestamp01NegativeSeconds() throws Exception
    {
        // test for Issue #69
        ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(-14159020000L, 183917322), UTC);
        String serialized = MAPPER.writer()
                .with(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .with(DateTimeFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .writeValueAsString(date);
        ZonedDateTime actual = MAPPER.readValue(serialized, ZonedDateTime.class);
        assertEquals(date, actual);
    }

    @Test
    public void testSerializationAsTimestamp01NegativeSecondsWithDefaults() throws Exception
    {
        // test for Issue #69 using default mapper config
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM dd yyyy HH:mm:ss.SSS zzz", Locale.ENGLISH);
        ZonedDateTime original = ZonedDateTime.parse("Apr 13 1969 05:05:38.599 UTC", dtf);
        String serialized = MAPPER.writeValueAsString(original);
        ZonedDateTime deserialized = MAPPER.readValue(serialized, ZonedDateTime.class);
        assertEquals(original.getDayOfMonth(), deserialized.getDayOfMonth(), "The day is not correct.");
        assertEquals(original.getMonthValue(), deserialized.getMonthValue(), "The month is not correct.");
        assertEquals(original.getYear(), deserialized.getYear(), "The year is not correct.");
        assertEquals(original.getHour(), deserialized.getHour(), "The hour is not correct.");
        assertEquals(original.getMinute(), deserialized.getMinute(), "The minute is not correct.");
        assertEquals(original.getSecond(), deserialized.getSecond(), "The second is not correct.");
        assertEquals(original.getNano(), deserialized.getNano(), "The nano is not correct.");
        assertEquals(ZoneId.of("UTC").getRules(), deserialized.getZone().getRules(), "The time zone is not correct.");
    }

    @Test
    public void testSerializationAsTimestamp01Milliseconds() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(0L), Z1);
        String value = MAPPER.writer()
                .with(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .without(DateTimeFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .writeValueAsString(date);
        assertEquals("0", value);
    }

    @Test
    public void testSerializationAsTimestamp02Nanoseconds() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(123456789L, 183917322), Z2);
        String value = MAPPER.writer()
                .with(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .with(DateTimeFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .writeValueAsString(date);
        assertEquals("123456789.183917322", value);
    }

    @Test
    public void testSerializationAsTimestamp02Milliseconds() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(123456789L, 183917322), Z2);
        String value = MAPPER.writer()
                .with(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .without(DateTimeFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .writeValueAsString(date);
        assertEquals("123456789183", value);
    }

    @Test
    public void testSerializationAsTimestamp03Nanoseconds() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.now(Z3);
        String value = MAPPER.writer()
                .with(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .with(DateTimeFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .writeValueAsString(date);
        assertEquals(DecimalUtils.toDecimal(date.toEpochSecond(), date.getNano()), value);
    }

    @Test
    public void testSerializationAsTimestamp03Milliseconds() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.now(Z3);
        String value = MAPPER.writer()
                .with(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .without(DateTimeFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .writeValueAsString(date);
        assertEquals(Long.toString(date.toInstant().toEpochMilli()), value);
    }

    @Test
    public void testSerializationAsString01() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(0L), Z1);
        String value = MAPPER.writer()
                .without(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .writeValueAsString(date);
        assertEquals('"'
                + FORMATTER.withZone(Z1).format(date) + '"', value);
    }

    @Test
    public void testSerializationAsString02() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(123456789L, 183917322), Z2);
        String value = MAPPER.writer()
                .without(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .writeValueAsString(date);
        assertEquals('"'
                + FORMATTER.withZone(Z2).format(date) + '"', value);
    }

    @Test
    public void testSerializationAsString03() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.now(Z3);
        String value = MAPPER.writer()
                .without(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .writeValueAsString(date);
        assertEquals('"'
                + FORMATTER.withZone(Z3).format(date) + '"', value);
    }

    @Test
    public void testSerializationAsStringWithMapperTimeZone01() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(0L), Z1);
        String value = newMapper()
                .writer()
                .with(TimeZone.getTimeZone(Z1))
                .without(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .writeValueAsString(date);
        assertEquals('"' + FORMATTER.format(date) + '"', value);
    }

    @Test
    public void testSerializationAsStringWithMapperTimeZone02() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(123456789L, 183917322), Z2);
        String value = newMapper()
                .writer()
                .with(TimeZone.getTimeZone(Z2))
                .without(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .writeValueAsString(date);
        assertEquals('"' + FORMATTER.format(date) + '"', value);
    }

    @Test
    public void testSerializationAsStringWithMapperTimeZone03() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.now(Z3);
        String value = newMapper()
                .writer()
                .with(TimeZone.getTimeZone(Z3))
                .without(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .writeValueAsString(date);
        assertEquals('"' + FORMATTER.format(date) + '"', value);
    }

    @Test
    public void testSerializationAsStringWithZoneIdOff() throws Exception {
        ZonedDateTime date = ZonedDateTime.now(Z3);
        ObjectMapper mapper = newMapperBuilder()
            .configure(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .configure(DateTimeFeature.WRITE_DATES_WITH_ZONE_ID, false)
            .build();

        assertEquals(q(FORMATTER.withZone(Z3).format(date)),
                mapper.writeValueAsString(date));
    }

    @Test
    public void testSerializationAsStringWithZoneIdOffAndMapperTimeZone() throws Exception {
        ZonedDateTime date = ZonedDateTime.now(Z3);
        String value = newMapper()
                .writer()
                .with(TimeZone.getTimeZone(Z3))
                .without(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .without(DateTimeFeature.WRITE_DATES_WITH_ZONE_ID)
                .writeValueAsString(date);
        assertEquals(q(FORMATTER.format(date)), value);
    }

    @Test
    public void testSerializationAsStringWithZoneIdOn() throws Exception {
        ZonedDateTime date = ZonedDateTime.now(Z3);
        ObjectMapper mapper = newMapperBuilder()
            .configure(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .configure(DateTimeFeature.WRITE_DATES_WITH_ZONE_ID, true)
            .build();
        String value = mapper.writeValueAsString(date);
        assertEquals("\"" + DateTimeFormatter.ISO_ZONED_DATE_TIME.format(date) + "\"", value);
    }

    @Test
    public void testSerializationAsStringWithDefaultTimeZoneAndContextTimeZoneOnAndACustomFormatter() throws Exception {
        ZonedDateTime date = ZonedDateTime.now(Z3);
        // With a custom DateTimeFormatter without a ZoneId.
        String value = newMapperBuilder().addModule(
                new SimpleModule().addSerializer(new ZonedDateTimeSerializer(FORMATTER_WITHOUT_ZONEID)))
                .build()
                .writer()
                .with(TimeZone.getTimeZone(Z2))
                .without(DateTimeFeature.WRITE_DATES_WITH_ZONE_ID)
                .without(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .with(DateTimeFeature.WRITE_DATES_WITH_CONTEXT_TIME_ZONE)
                .writeValueAsString(date);

        // We expect to have the date written with the datetime of ZoneId Z2
        assertEquals("\"" + date.withZoneSameInstant(Z2).format(FORMATTER_WITHOUT_ZONEID) + "\"", value);
    }

    @Test
    public void testSerializationAsStringWithDefaultTimeZoneAndContextTimeZoneOffAndACustomFormatter() throws Exception {
        ZonedDateTime date = ZonedDateTime.now(Z3);
        // With a custom DateTimeFormatter without a Zone.
        String value = newMapperBuilder().addModule(
                new SimpleModule().addSerializer(new ZonedDateTimeSerializer(FORMATTER_WITHOUT_ZONEID)))
                .build()
                .writer()
                .with(TimeZone.getTimeZone(Z2))
                .without(DateTimeFeature.WRITE_DATES_WITH_ZONE_ID)
                .without(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .without(DateTimeFeature.WRITE_DATES_WITH_CONTEXT_TIME_ZONE)
                .writeValueAsString(date);

        // We expect to have the date written with the datetime of ZoneId Z3
        assertEquals("\"" + date.format(FORMATTER_WITHOUT_ZONEID) + "\"", value);
    }

    @Test
    public void testSerializationAsStringWithDefaultTimeZoneAndContextTimeZoneOn() throws Exception {
        ZonedDateTime date = ZonedDateTime.now(Z3);
        String value = MAPPER.writer()
                .with(TimeZone.getTimeZone(Z2))
                .without(DateTimeFeature.WRITE_DATES_WITH_ZONE_ID)
                .without(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .with(DateTimeFeature.WRITE_DATES_WITH_CONTEXT_TIME_ZONE)
                .writeValueAsString(date);

        // We expect to have the date written with the ZoneId Z2
        assertEquals("\"" + DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(date.withZoneSameInstant(Z2)) + "\"", value);
    }

    @Test
    public void testSerializationAsStringWithDefaultTimeZoneAndContextTimeZoneOff() throws Exception {
        ZonedDateTime date = ZonedDateTime.now(Z3);
        String value = MAPPER.writer()
                .with(TimeZone.getTimeZone(Z2))
                .without(DateTimeFeature.WRITE_DATES_WITH_ZONE_ID)
                .without(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .without(DateTimeFeature.WRITE_DATES_WITH_CONTEXT_TIME_ZONE)
                .writeValueAsString(date);

        // We expect to have the date written with the ZoneId Z3
        assertEquals("\"" + DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(date) + "\"", value);
    }

    @Test
    public void testSerializationWithTypeInfo01() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(123456789L, 183917322), Z2);
        String value = newMapperBuilder()
                .addMixIn(Temporal.class, MockObjectConfiguration.class)
                .build()
                .writer()
                .with(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .with(DateTimeFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .writeValueAsString(date);
        assertEquals("[\"" + ZonedDateTime.class.getName() + "\",123456789.183917322]", value);
    }

    @Test
    public void testSerializationWithTypeInfo02() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(123456789L, 183917322), Z2);
        String value = newMapperBuilder()
                .addMixIn(Temporal.class, MockObjectConfiguration.class)
                .build()
                .writer()
                .with(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .without(DateTimeFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .writeValueAsString(date);
        assertEquals("[\"" + ZonedDateTime.class.getName() + "\",123456789183]", value);
    }

    @Test
    public void testSerializationWithTypeInfo03() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.now(Z3);
        String value = mapperBuilder()
                .addMixIn(Temporal.class, MockObjectConfiguration.class)
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build()
                .writeValueAsString(date);
        assertEquals("[\"" + ZonedDateTime.class.getName() + "\",\""
                        + FORMATTER.withZone(Z3).format(date) + "\"]", value);
    }

    @Test
    public void testSerializationWithTypeInfoAndMapperTimeZone() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.now(Z3);
        String value = newMapperBuilder()
                .addMixIn(Temporal.class, MockObjectConfiguration.class)
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build()
                .writer()
                .with(TimeZone.getTimeZone(Z3))
                .writeValueAsString(date);
        assertEquals("[\"" + ZonedDateTime.class.getName() + "\",\"" + FORMATTER.format(date) + "\"]", value);
    }

    @Test
    public void testDeserializationAsFloat01WithoutTimeZone() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(0L), Z1);
        ZonedDateTime value = MAPPER.readValue("0.000000000", ZonedDateTime.class);

        assertIsEqual(date, value);
        assertEquals(DEFAULT_TZ, value.getZone(), "The time zone is not correct.");
    }

    @Test
    public void testDeserializationAsFloat01WithTimeZone() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(0L), Z1);
        ZonedDateTime value = MAPPER
                .readerFor(ZonedDateTime.class)
                .with(TimeZone.getDefault())
                .readValue("0.000000000");
        assertIsEqual(date, value);
        assertEquals(ZoneId.systemDefault().normalized(), value.getZone(), "The time zone is not correct.");
    }

    @Test
    public void testDeserializationAsFloat02WithoutTimeZone() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(123456789L, 183917322), Z2);
        ZonedDateTime value = MAPPER.readValue("123456789.183917322", ZonedDateTime.class);

        assertIsEqual(date, value);
        assertEquals(DEFAULT_TZ, value.getZone(), "The time zone is not correct.");
    }

    @Test
    public void testDeserializationAsFloat02WithTimeZone() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(123456789L, 183917322), Z2);
        ZonedDateTime value = MAPPER
                .readerFor(ZonedDateTime.class)
                .with(TimeZone.getDefault())
                .readValue("123456789.183917322");

        assertIsEqual(date, value);
        assertEquals(ZoneId.systemDefault().normalized(), value.getZone(), "The time zone is not correct.");
    }

    @Test
    public void testDeserializationAsFloat03WithoutTimeZone() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.now(Z3);
        ObjectMapper mapper = newMapper();
        ZonedDateTime value = mapper.readValue(
                DecimalUtils.toDecimal(date.toEpochSecond(), date.getNano()), ZonedDateTime.class
        );
        assertIsEqual(date, value);
        assertEquals(DEFAULT_TZ, value.getZone(), "The time zone is not correct.");
    }

    @Test
    public void testDeserializationAsFloat03WithTimeZone() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.now(Z3);
        ObjectMapper mapper = newMapper(TimeZone.getDefault());
        ZonedDateTime value = mapper.readValue(
                DecimalUtils.toDecimal(date.toEpochSecond(), date.getNano()), ZonedDateTime.class
                );
        assertIsEqual(date, value);
        assertEquals(ZoneId.systemDefault().normalized(), value.getZone(), "The time zone is not correct.");
    }

    @Test
    public void testDeserializationAsInt01NanosecondsWithoutTimeZone() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(0L), Z1);
        ZonedDateTime value = READER
                .with(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .readValue("0");
        assertIsEqual(date, value);
        assertEquals(DEFAULT_TZ, value.getZone(), "The time zone is not correct.");
    }

    @Test
    public void testDeserializationAsInt01NanosecondsWithTimeZone() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(0L), Z1);
        ZonedDateTime value = newMapper(TimeZone.getDefault()).readerFor(ZonedDateTime.class)
                .with(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .readValue("0");
        assertIsEqual(date, value);
        assertEquals(ZoneId.systemDefault().normalized(), value.getZone(), "The time zone is not correct.");
    }

    @Test
    public void testDeserializationAsInt01MillisecondsWithoutTimeZone() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(0L), Z1);
        ZonedDateTime value = READER
                .without(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .readValue("0");
        assertIsEqual(date, value);
        assertEquals(DEFAULT_TZ, value.getZone(), "The time zone is not correct.");
    }

    @Test
    public void testDeserializationAsInt01MillisecondsWithTimeZone() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(0L), Z1);
        ZonedDateTime value = MAPPER_WITH_DEFAULT_TZ
                .readerFor(ZonedDateTime.class)
                .without(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .readValue("0");
        assertIsEqual(date, value);
        assertEquals(ZoneId.systemDefault().normalized(), value.getZone(), "The time zone is not correct.");
    }

    @Test
    public void testDeserializationAsInt02NanosecondsWithoutTimeZone() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(123456789L, 0), Z2);
        ZonedDateTime value = MAPPER.readerFor(ZonedDateTime.class)
                .with(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .readValue("123456789");
        assertIsEqual(date, value);
        assertEquals(DEFAULT_TZ, value.getZone(), "The time zone is not correct.");
    }

    @Test
    public void testDeserializationAsInt02NanosecondsWithTimeZone() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(123456789L, 0), Z2);
        ZonedDateTime value = MAPPER_WITH_DEFAULT_TZ.readerFor(ZonedDateTime.class)
                .with(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .readValue("123456789");
        assertIsEqual(date, value);
        assertEquals(ZoneId.systemDefault().normalized(), value.getZone(), "The time zone is not correct.");
    }

    @Test
    public void testDeserializationAsInt02MillisecondsWithoutTimeZone() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(123456789L, 422000000), Z2);
        ZonedDateTime value = MAPPER.readerFor(ZonedDateTime.class)
                .without(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .readValue("123456789422");
        assertIsEqual(date, value);
        assertEquals(DEFAULT_TZ, value.getZone(), "The time zone is not correct.");
    }

    @Test
    public void testDeserializationAsInt02MillisecondsWithTimeZone() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(123456789L, 422000000), Z2);
        ObjectMapper mapper = newMapper(TimeZone.getDefault());
        ZonedDateTime value = mapper.readerFor(ZonedDateTime.class)
                .without(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .readValue("123456789422");
        assertIsEqual(date, value);
        assertEquals(ZoneId.systemDefault().normalized(), value.getZone(), "The time zone is not correct.");
    }

    @Test
    public void testDeserializationAsInt03NanosecondsWithoutTimeZone() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.now(Z3);
        date = date.minus(date.getNano(), ChronoUnit.NANOS);
        ObjectMapper mapper = newMapper();
        ZonedDateTime value = mapper.readerFor(ZonedDateTime.class)
                .with(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .readValue(Long.toString(date.toEpochSecond()));
        assertIsEqual(date, value);
        assertEquals(DEFAULT_TZ, value.getZone(), "The time zone is not correct.");
    }

    @Test
    public void testDeserializationAsInt03NanosecondsWithTimeZone() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.now(Z3);
        date = date.minus(date.getNano(), ChronoUnit.NANOS);
        ObjectMapper mapper = newMapper(TimeZone.getDefault());
        ZonedDateTime value = mapper.readerFor(ZonedDateTime.class)
                .with(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .readValue(Long.toString(date.toEpochSecond()));
        assertIsEqual(date, value);
        assertEquals(ZoneId.systemDefault().normalized(), value.getZone(), "The time zone is not correct.");
    }

    @Test
    public void testDeserializationAsInt03MillisecondsWithoutTimeZone() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.now(Z3);
        date = date.minus(date.getNano() - (date.get(ChronoField.MILLI_OF_SECOND) * 1_000_000L), ChronoUnit.NANOS);
        ObjectMapper mapper = newMapper();
        ZonedDateTime value = mapper.readerFor(ZonedDateTime.class)
                .without(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .readValue(Long.toString(date.toInstant().toEpochMilli()));
        assertIsEqual(date, value);
        assertEquals(DEFAULT_TZ, value.getZone(), "The time zone is not correct.");
    }

    @Test
    public void testDeserializationAsInt03MillisecondsWithTimeZone() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.now(Z3);
        date = date.minus(date.getNano() - (date.get(ChronoField.MILLI_OF_SECOND) * 1_000_000L), ChronoUnit.NANOS);
        ObjectMapper mapper = newMapper(TimeZone.getDefault());
        ZonedDateTime value = mapper.readerFor(ZonedDateTime.class)
                .without(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .readValue(Long.toString(date.toInstant().toEpochMilli()));
        assertIsEqual(date, value);
        assertEquals(ZoneId.systemDefault().normalized(), value.getZone(), "The time zone is not correct.");
    }

    @Test
    public void testDeserializationAsString01WithoutTimeZone() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(0L), Z1);
        ObjectMapper mapper = newMapper();
        ZonedDateTime value = mapper.readerFor(ZonedDateTime.class)
                .with(DateTimeFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                .readValue('"' + FORMATTER.format(date) + '"');
        assertIsEqual(date, value);
        assertEquals(DEFAULT_TZ, value.getZone(), "The time zone is not correct.");
    }

    @Test
    public void testDeserializationAsString01WithTimeZone() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(0L), Z1);
        ObjectMapper mapper = newMapper(TimeZone.getDefault());
        ZonedDateTime value = mapper.readerFor(ZonedDateTime.class)
                .with(DateTimeFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                .readValue('"' + FORMATTER.format(date) + '"');
        assertIsEqual(date, value);
        assertEquals(ZoneId.systemDefault().normalized(), value.getZone(), "The time zone is not correct.");
    }

    @Test
    public void testDeserializationAsString01WithTimeZoneTurnedOff() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(0L), FIX_OFFSET);
        ObjectMapper mapper = newMapper(TimeZone.getDefault());
        ZonedDateTime value = mapper.readerFor(ZonedDateTime.class)
                .without(DateTimeFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                .readValue('"' + FORMATTER.format(date) + '"');
        assertIsEqual(date, value);
        assertEquals(FIX_OFFSET, value.getZone(), "The time zone is not correct.");
    }

    @Test
    public void testDeserializationAsString01WithZoneId() throws Exception {
        ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(0L), Z1);
        ZonedDateTime value = MAPPER.readerFor(ZonedDateTime.class).readValue(
                "\"" + DateTimeFormatter.ISO_ZONED_DATE_TIME.format(date) + "\"");
        assertIsEqual(date, value);
    }

    @Test
    public void testDeserializationAsString02WithoutTimeZone() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(123456789L, 183917322), Z2);
        ZonedDateTime value = READER
                .with(DateTimeFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                .readValue('"' + FORMATTER.format(date) + '"');

        assertIsEqual(date, value);
        assertEquals(DEFAULT_TZ, value.getZone(), "The time zone is not correct.");
    }

    @Test
    public void testDeserializationAsString02WithTimeZone() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(123456789L, 183917322), Z2);
        ZonedDateTime value = newMapper(TimeZone.getDefault())
                .readerFor(ZonedDateTime.class)
                .with(DateTimeFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                .readValue('"' + FORMATTER.format(date) + '"');
        assertIsEqual(date, value);
        assertEquals(ZoneId.systemDefault().normalized(), value.getZone(), "The time zone is not correct.");
    }

    @Test
    public void testDeserializationAsString02WithTimeZoneTurnedOff() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(123456789L, 183917322), FIX_OFFSET);
        ObjectMapper mapper = newMapper(TimeZone.getDefault());
        ZonedDateTime value = mapper.readerFor(ZonedDateTime.class)
                .without(DateTimeFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                .readValue('"' + FORMATTER.format(date) + '"');
        assertIsEqual(date, value);
        assertEquals(FIX_OFFSET, value.getZone(), "The time zone is not correct.");
    }

    @Test
    public void testDeserializationAsString02WithZoneId() throws Exception {
        ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(123456789L, 183917322), Z2);
        ZonedDateTime value = READER
                .readValue("\"" + DateTimeFormatter.ISO_ZONED_DATE_TIME.format(date) + "\"");
        assertIsEqual(date, value);
    }

    @Test
    public void testDeserializationAsString03WithoutTimeZone() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.now(Z3);
        ZonedDateTime value = READER
                .with(DateTimeFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                .readValue('"' + FORMATTER.format(date) + '"');
        assertIsEqual(date, value);
        assertEquals(DEFAULT_TZ, value.getZone(), "The time zone is not correct.");
    }

    @Test
    public void testDeserializationAsString03WithTimeZone() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.now(Z3);
        ZonedDateTime value = newMapper(TimeZone.getDefault())
                .readerFor(ZonedDateTime.class)
                .with(DateTimeFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                .readValue('"' + FORMATTER.format(date) + '"');
        assertIsEqual(date, value);
        assertEquals(ZoneId.systemDefault().normalized(), value.getZone(), "The time zone is not correct.");
    }

    @Test
    public void testDeserializationAsString03WithTimeZoneTurnedOff() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.now(FIX_OFFSET);
        ZonedDateTime value = newMapper(TimeZone.getDefault())
                .readerFor(ZonedDateTime.class)
                .without(DateTimeFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                .readValue('"' + FORMATTER.format(date) + '"');
        assertIsEqual(date, value);
        assertEquals(FIX_OFFSET, value.getZone(), "The time zone is not correct.");
    }

    @Test
    public void testDeserializationAsString03WithZoneId() throws Exception {
        ZonedDateTime date = ZonedDateTime.now(Z3);
        ZonedDateTime value = MAPPER.readValue("\"" + DateTimeFormatter.ISO_ZONED_DATE_TIME.format(date) + "\"", ZonedDateTime.class);
        assertIsEqual(date, value);
    }

    @Test
    public void testDeserializationWithTypeInfo01WithoutTimeZone() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(123456789L, 183917322), Z2);
        ObjectMapper mapper = newMapperBuilder()
                .addMixIn(Temporal.class, MockObjectConfiguration.class)
                .build();
        Temporal value = mapper.readValue(
                "[\"" + ZonedDateTime.class.getName() + "\",123456789.183917322]", Temporal.class
                );
        assertInstanceOf(ZonedDateTime.class, value, "The value should be an ZonedDateTime.");
        assertIsEqual(date, (ZonedDateTime) value);
        assertEquals(DEFAULT_TZ, ((ZonedDateTime) value).getZone(), "The time zone is not correct.");
    }

    @Test
    public void testDeserializationWithTypeInfo01WithTimeZone() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(123456789L, 183917322), Z2);
        ObjectMapper mapper = newMapperBuilder(TimeZone.getDefault())
                .addMixIn(Temporal.class, MockObjectConfiguration.class)
                .build();
        Temporal value = mapper.readValue(
                "[\"" + ZonedDateTime.class.getName() + "\",123456789.183917322]", Temporal.class
                );
        assertInstanceOf(ZonedDateTime.class, value, "The value should be an ZonedDateTime.");
        assertIsEqual(date, (ZonedDateTime) value);
        assertEquals(ZoneId.systemDefault().normalized(), ((ZonedDateTime) value).getZone(), "The time zone is not correct.");
    }

    @Test
    public void testDeserializationWithTypeInfo02WithoutTimeZone() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(123456789L, 0), Z2);

        ObjectMapper mapper = newMapperBuilder()
                .addMixIn(Temporal.class, MockObjectConfiguration.class)
                .build();
        Temporal value = mapper.readerFor(Temporal.class)
                .with(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .readValue("[\"" + ZonedDateTime.class.getName() + "\",123456789]");
        assertInstanceOf(ZonedDateTime.class, value, "The value should be an ZonedDateTime.");
        assertIsEqual(date, (ZonedDateTime) value);
        assertEquals(DEFAULT_TZ, ((ZonedDateTime) value).getZone(), "The time zone is not correct.");
    }

    @Test
    public void testDeserializationWithTypeInfo02WithTimeZone() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(123456789L, 0), Z2);
        ObjectMapper mapper = newMapperBuilder(TimeZone.getDefault())
                .addMixIn(Temporal.class, MockObjectConfiguration.class)
                .build();
        Temporal value = mapper.readerFor(Temporal.class)
                .with(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .readValue("[\"" + ZonedDateTime.class.getName() + "\",123456789]");
        assertInstanceOf(ZonedDateTime.class, value, "The value should be an ZonedDateTime.");
        assertIsEqual(date, (ZonedDateTime) value);
        assertEquals(ZoneId.systemDefault().normalized(), ((ZonedDateTime) value).getZone(), "The time zone is not correct.");
    }

    @Test
    public void testDeserializationWithTypeInfo03WithoutTimeZone() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(123456789L, 422000000), Z2);

        ObjectMapper mapper = newMapperBuilder()
                .addMixIn(Temporal.class, MockObjectConfiguration.class)
                .build();
        Temporal value = mapper.readerFor(Temporal.class)
                .without(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .readValue("[\"" + ZonedDateTime.class.getName() + "\",123456789422]");
        assertInstanceOf(ZonedDateTime.class, value, "The value should be an ZonedDateTime.");
        assertIsEqual(date, (ZonedDateTime) value);
        assertEquals(DEFAULT_TZ, ((ZonedDateTime) value).getZone(), "The time zone is not correct.");
    }

    @Test
    public void testDeserializationWithTypeInfo03WithTimeZone() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(123456789L, 422000000), Z2);
        ObjectMapper mapper = newMapperBuilder(TimeZone.getDefault())
                .addMixIn(Temporal.class, MockObjectConfiguration.class)
                .build();
        Temporal value = mapper
                .readerFor(Temporal.class)
                .without(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .readValue(
                "[\"" + ZonedDateTime.class.getName() + "\",123456789422]");
        assertInstanceOf(ZonedDateTime.class, value, "The value should be an ZonedDateTime.");
        assertIsEqual(date, (ZonedDateTime) value);
        assertEquals(ZoneId.systemDefault().normalized(), ((ZonedDateTime) value).getZone(), "The time zone is not correct.");
    }

    @Test
    public void testDeserializationWithTypeInfo04WithoutTimeZone() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.now(Z3);
        ObjectMapper mapper = newMapperBuilder()
                .addMixIn(Temporal.class, MockObjectConfiguration.class)
                .build();
        Temporal value = mapper
                .readerFor(Temporal.class)
                .with(DateTimeFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                .readValue(
                "[\"" + ZonedDateTime.class.getName() + "\",\"" + FORMATTER.format(date) + "\"]");
        assertInstanceOf(ZonedDateTime.class, value, "The value should be an ZonedDateTime.");
        assertIsEqual(date, (ZonedDateTime) value);
        assertEquals(DEFAULT_TZ, ((ZonedDateTime) value).getZone(), "The time zone is not correct.");
    }

    @Test
    public void testDeserializationWithTypeInfo04WithTimeZone() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.now(Z3);
        ObjectMapper mapper = newMapperBuilder(TimeZone.getDefault())
                .addMixIn(Temporal.class, MockObjectConfiguration.class)
                .build();
        Temporal value = mapper
                .readerFor(Temporal.class)
                .with(DateTimeFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                .readValue(
                "[\"" + ZonedDateTime.class.getName() + "\",\"" + FORMATTER.format(date) + "\"]");
        assertInstanceOf(ZonedDateTime.class, value, "The value should be an ZonedDateTime.");
        assertIsEqual(date, (ZonedDateTime) value);
        assertEquals(ZoneId.systemDefault().normalized(), ((ZonedDateTime) value).getZone(), "The time zone is not correct.");
    }

    @Test
    public void testDeserializationWithTypeInfo04WithTimeZoneTurnedOff() throws Exception
    {
        ZonedDateTime date = ZonedDateTime.now(FIX_OFFSET);
        Temporal value = newMapperBuilder(TimeZone.getDefault())
                .addMixIn(Temporal.class, MockObjectConfiguration.class)
                .build()
                .readerFor(Temporal.class)
                .without(DateTimeFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                .readValue(
                "[\"" + ZonedDateTime.class.getName() + "\",\"" + FORMATTER.format(date) + "\"]");
        assertInstanceOf(ZonedDateTime.class, value, "The value should be an ZonedDateTime.");
        assertIsEqual(date, (ZonedDateTime) value);
        assertEquals(FIX_OFFSET, ((ZonedDateTime) value).getZone(), "The time zone is not correct.");
    }

    @Test
    public void testCustomPatternWithAnnotations() throws Exception
    {
        ZonedDateTime inputValue = ZonedDateTime.ofInstant(Instant.ofEpochSecond(0L), UTC);
        final Wrapper input = new Wrapper(inputValue);
        String json = MAPPER.writeValueAsString(input);
        assertEquals(a2q("{'value':'1970_01_01 00:00:00(+0000)'}"), json);

        Wrapper result = MAPPER.readerFor(Wrapper.class)
                .without(DateTimeFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                .readValue(json);
        // looks like timezone gets converted (is that correct or not?); verify just offsets for now
        assertEquals(input.value.toInstant(), result.value.toInstant());
    }

    // [modules-java#269]
    @Test
    public void testCustomPatternWithNumericTimestamp() throws Exception
    {
        String input = a2q("{'value':'3.141592653'}");

        Wrapper result = JsonMapper.builder()
            .enable(DateTimeFeature.ALWAYS_ALLOW_STRINGIFIED_DATE_TIMESTAMPS)
            .build()
            .readerFor(Wrapper.class)
            .readValue(input);

        assertEquals(Instant.ofEpochSecond(3L, 141592653L), result.value.toInstant());
    }

    @Test
    public void testNumericCustomPatternWithAnnotations() throws Exception
    {
        ZonedDateTime inputValue = ZonedDateTime.ofInstant(Instant.ofEpochSecond(0L), UTC);
        final WrapperNumeric input = new WrapperNumeric(inputValue);
        ObjectMapper m = newMapper();
        String json = m.writeValueAsString(input);
        assertEquals(a2q("{'value':'19700101000000'}"), json);

        WrapperNumeric result = m.readValue(json, WrapperNumeric.class);
        assertEquals(input.value.toInstant(), result.value.toInstant());
    }

    @Test
    public void testInstantPriorToEpochIsEqual() throws Exception
    {
        //Issue #120 test
        final Instant original = Instant.ofEpochMilli(-1);
        final String serialized = MAPPER.writeValueAsString(original);
        final Instant deserialized = MAPPER.readValue(serialized, Instant.class);
        assertEquals(original, deserialized);
    }

    public static class Pojo1 {
        @JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
        public ZonedDateTime t1 = ZonedDateTime.parse("2022-04-27T12:00:00+02:00[Europe/Paris]");

        public ZonedDateTime t2 = t1;
    }

    @Test
    public void testShapeInt() throws Exception {
        String json1 = mapperBuilder()
                .enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build()
                .writeValueAsString(new Pojo1());
        assertEquals("{\"t1\":1651053600000,\"t2\":1651053600.000000000}", json1);
    }

    private static void assertIsEqual(ZonedDateTime expected, ZonedDateTime actual)
    {
        assertTrue(expected.isEqual(actual),
                "The value is not correct. Expected timezone-adjusted <" + expected + ">, actual <" + actual + ">.");
    }
}
