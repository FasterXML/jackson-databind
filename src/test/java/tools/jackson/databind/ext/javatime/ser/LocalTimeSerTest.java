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

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;
import tools.jackson.databind.ext.javatime.MockObjectConfiguration;

import static org.junit.jupiter.api.Assertions.*;

public class LocalTimeSerTest extends DateTimeTestBase
{
    private final ObjectMapper MAPPER = newMapper();
    private final ObjectWriter writer = MAPPER.writer();

    // [modules-java8#115]
    static class CustomLocalTimeSerializer extends LocalTimeSerializer {
        public CustomLocalTimeSerializer() {
             // Default doesn't cut it for us.
             super(DateTimeFormatter.ofPattern("HH/mm"));
        }
    }

    static class CustomWrapper {
        @JsonSerialize(using = CustomLocalTimeSerializer.class)
        public LocalTime value;

        public CustomWrapper(LocalTime v) { value = v; }
    }

    @Test
    public void testSerializationAsTimestamp01() throws Exception
    {
        String json = writer.with(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .writeValueAsString(LocalTime.of(15, 43));
        assertEquals("[15,43]", json, "The value is not correct.");
    }

    @Test
    public void testSerializationAsTimestamp02() throws Exception
    {
        String json = writer.with(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .writeValueAsString(LocalTime.of(9, 22, 57));
        assertEquals("[9,22,57]", json, "The value is not correct.");
    }

    @Test
    public void testSerializationAsTimestamp03Nanoseconds() throws Exception
    {
        String json = writer.with(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .with(DateTimeFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .writeValueAsString(LocalTime.of(9, 22, 0, 57));
        assertEquals("[9,22,0,57]", json, "The value is not correct.");
    }

    @Test
    public void testSerializationAsTimestamp03Milliseconds() throws Exception
    {
        LocalTime time = LocalTime.of(9, 22, 0, 57);
        ObjectMapper mapper = newMapperBuilder()
                .configure(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, true)
                .configure(DateTimeFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false)
                .build();
        String value = mapper.writeValueAsString(time);

        assertEquals("[9,22,0,0]", value, "The value is not correct.");
    }

    @Test
    public void testSerializationAsTimestamp04Nanoseconds() throws Exception
    {
        LocalTime time = LocalTime.of(22, 31, 5, 829837);
        ObjectMapper mapper = newMapperBuilder()
                .configure(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, true)
                .configure(DateTimeFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, true)
                .build();
        String value = mapper.writeValueAsString(time);
        assertEquals("[22,31,5,829837]", value, "The value is not correct.");
    }

    @Test
    public void testSerializationAsTimestamp04Milliseconds() throws Exception
    {
        LocalTime time = LocalTime.of(22, 31, 5, 422829837);
        ObjectMapper mapper = newMapperBuilder()
                .enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DateTimeFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .build();
        String value = mapper.writeValueAsString(time);
        assertEquals("[22,31,5,422]", value, "The value is not correct.");
    }

    @Test
    public void testSerializationAsString01() throws Exception
    {
        LocalTime time = LocalTime.of(15, 43, 20);
        ObjectMapper mapper = newMapperBuilder()
                .configure(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .build();
        assertEquals("\"15:43:20\"", mapper.writeValueAsString(time));
    }

    @Test
    public void testSerializationAsString02() throws Exception
    {
        LocalTime time = LocalTime.of(9, 22, 57);
        ObjectMapper mapper = newMapperBuilder()
                    .configure(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                    .build();
        String value = mapper.writeValueAsString(time);
        assertEquals('"' + time.toString() + '"', value, "The value is not correct.");
    }

    @Test
    public void testSerializationAsString03() throws Exception
    {
        LocalTime time = LocalTime.of(22, 31, 5, 829837);
        ObjectMapper m = newMapperBuilder()
                .configure(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .build();
        String value = m.writeValueAsString(time);
        assertEquals('"' + time.toString() + '"', value, "The value is not correct.");
    }

    // [modules-java8#115]
    @Test
    public void testWithCustomSerializer() throws Exception
    {
        String json = MAPPER.writeValueAsString(new CustomWrapper(LocalTime.of(15, 43)));
        assertEquals("{\"value\":\"15/43\"}", json, "The value is not correct.");
    }

    @Test
    public void testSerializationWithTypeInfo01() throws Exception
    {
        LocalTime time = LocalTime.of(22, 31, 5, 829837);
        ObjectMapper m = newMapperBuilder()
                .enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(DateTimeFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .addMixIn(Temporal.class, MockObjectConfiguration.class)
                .build();
        String json = m.writeValueAsString(time);

        assertEquals("[\"" + LocalTime.class.getName() + "\",[22,31,5,829837]]", json,
                "The value is not correct.");
    }

    @Test
    public void testSerializationWithTypeInfo02() throws Exception
    {
        LocalTime time = LocalTime.of(22, 31, 5, 422829837);

        ObjectMapper m = newMapperBuilder()
                .enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DateTimeFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .addMixIn(Temporal.class, MockObjectConfiguration.class)
                .build();
        String json = m.writeValueAsString(time);
        assertEquals("[\"" + LocalTime.class.getName() + "\",[22,31,5,422]]", json,
                "The value is not correct.");
    }

    @Test
    public void testSerializationWithTypeInfo03() throws Exception
    {
        LocalTime time = LocalTime.of(22, 31, 5, 829837);
        ObjectMapper m = newMapperBuilder()
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .addMixIn(Temporal.class, MockObjectConfiguration.class)
                .build();
        String value = m.writeValueAsString(time);

        assertEquals("[\"" + LocalTime.class.getName() + "\",\"" + time.toString() + "\"]", value,
                "The value is not correct.");
    }
}
