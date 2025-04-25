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

import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.temporal.Temporal;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.*;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;
import tools.jackson.databind.ext.javatime.MockObjectConfiguration;

import static org.junit.jupiter.api.Assertions.*;

public class OffsetTimeSerTest extends DateTimeTestBase
{
    private final ObjectMapper MAPPER = newMapper();

    @Test
    public void testSerializationAsTimestamp01() throws Exception
    {
        OffsetTime time = OffsetTime.of(15, 43, 0, 0, ZoneOffset.of("+0300"));
        String value = MAPPER.writer()
                .with(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .writeValueAsString(time);
        assertEquals("[15,43,\"+03:00\"]", value);
    }

    @Test
    public void testSerializationAsTimestamp02() throws Exception
    {
        OffsetTime time = OffsetTime.of(9, 22, 57, 0, ZoneOffset.of("-0630"));
        String value = MAPPER.writer()
                .with(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .writeValueAsString(time);
        assertEquals("[9,22,57,\"-06:30\"]", value);
    }

    @Test
    public void testSerializationAsTimestamp03Nanoseconds() throws Exception
    {
        OffsetTime time = OffsetTime.of(9, 22, 0, 57, ZoneOffset.of("-0630"));
        String value = MAPPER.writer()
                .with(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .with(DateTimeFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .writeValueAsString(time);
        assertEquals("[9,22,0,57,\"-06:30\"]", value);
    }

    @Test
    public void testSerializationAsTimestamp03Milliseconds() throws Exception
    {
        OffsetTime time = OffsetTime.of(9, 22, 0, 57, ZoneOffset.of("-0630"));
        String value = MAPPER.writer()
                .with(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .without(DateTimeFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .writeValueAsString(time);
        assertEquals("[9,22,0,0,\"-06:30\"]", value);
    }

    @Test
    public void testSerializationAsTimestamp04Nanoseconds() throws Exception
    {
        OffsetTime time = OffsetTime.of(22, 31, 5, 829837, ZoneOffset.of("+1100"));
        String value = MAPPER.writer()
                .with(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .with(DateTimeFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .writeValueAsString(time);
        assertEquals("[22,31,5,829837,\"+11:00\"]", value);
    }

    @Test
    public void testSerializationAsTimestamp04Milliseconds() throws Exception
    {
        OffsetTime time = OffsetTime.of(22, 31, 5, 422829837, ZoneOffset.of("+1100"));
        String value = MAPPER.writer()
                .with(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .without(DateTimeFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .writeValueAsString(time);
        assertEquals("[22,31,5,422,\"+11:00\"]", value);
    }

    @Test
    public void testSerializationAsString01() throws Exception
    {
        OffsetTime time = OffsetTime.of(15, 43, 0, 0, ZoneOffset.of("+0300"));
        String value = MAPPER.writer()
                .without(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .writeValueAsString(time);
        assertEquals('"' + time.toString() + '"', value);
    }

    @Test
    public void testSerializationAsString02() throws Exception
    {
        OffsetTime time = OffsetTime.of(9, 22, 57, 0, ZoneOffset.of("-0630"));
        String value = MAPPER.writer()
                .without(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .writeValueAsString(time);
        assertEquals('"' + time.toString() + '"', value);
    }

    @Test
    public void testSerializationAsString03() throws Exception
    {
        OffsetTime time = OffsetTime.of(22, 31, 5, 829837, ZoneOffset.of("+1100"));
        String value = MAPPER.writer()
                .without(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .writeValueAsString(time);
        assertEquals('"' + time.toString() + '"', value);
    }

    @Test
    public void testSerializationWithTypeInfo01() throws Exception
    {
        OffsetTime time = OffsetTime.of(22, 31, 5, 829837, ZoneOffset.of("+1100"));

        final ObjectMapper mapper = newMapperBuilder()
                .configure(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, true)
                .configure(DateTimeFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, true)
                .addMixIn(Temporal.class, MockObjectConfiguration.class)
                .build();
        assertEquals("[\"" + OffsetTime.class.getName() + "\",[22,31,5,829837,\"+11:00\"]]",
                mapper.writeValueAsString(time));
    }

    @Test
    public void testSerializationWithTypeInfo02() throws Exception
    {
        OffsetTime time = OffsetTime.of(22, 31, 5, 422829837, ZoneOffset.of("+1100"));

        final ObjectMapper mapper = newMapperBuilder()
            .configure(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, true)
            .configure(DateTimeFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false)
            .addMixIn(Temporal.class, MockObjectConfiguration.class)
            .build();

        assertEquals("[\"" + OffsetTime.class.getName() + "\",[22,31,5,422,\"+11:00\"]]",
                mapper.writeValueAsString(time));
    }

    @Test
    public void testSerializationWithTypeInfo03() throws Exception
    {
        OffsetTime time = OffsetTime.of(22, 31, 5, 829837, ZoneOffset.of("+1100"));

        final ObjectMapper mapper = newMapperBuilder()
                .configure(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .addMixIn(Temporal.class, MockObjectConfiguration.class)
                .build();

        assertEquals("[\"" + OffsetTime.class.getName() + "\",\"" + time.toString() + "\"]",
                mapper.writeValueAsString(time));
    }
}
