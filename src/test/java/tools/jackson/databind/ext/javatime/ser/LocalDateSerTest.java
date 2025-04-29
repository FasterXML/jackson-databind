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

import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.Temporal;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;
import tools.jackson.databind.ext.javatime.MockObjectConfiguration;

import static org.junit.jupiter.api.Assertions.*;

public class LocalDateSerTest
	extends DateTimeTestBase
{
    final static class EpochDayWrapper {
        @JsonFormat(shape=JsonFormat.Shape.NUMBER_INT)
        public LocalDate value;

        public EpochDayWrapper() { }
        public EpochDayWrapper(LocalDate v) { value = v; }
    }

    static class VanillaWrapper {
        public LocalDate value;

        public VanillaWrapper() { }
        public VanillaWrapper(LocalDate v) { value = v; }
    }

    // [modules-java8#46]
    static class Holder46 {
        public LocalDate localDate;

        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.WRAPPER_OBJECT)
        public Object object;

        public Holder46(LocalDate localDate, Object object) {
            this.localDate = localDate;
            this.object = object;
        }
    }    
    
    private final ObjectMapper MAPPER = newMapper();

    @Test
    public void testSerializationAsTimestamp01() throws Exception
    {
        LocalDate date = LocalDate.of(1986, Month.JANUARY, 17);
        String value = MAPPER.writer()
        		.with(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        		.writeValueAsString(date);

        assertNotNull(value);
        assertEquals("[1986,1,17]", value);
    }

    @Test
    public void testSerializationAsTimestamp02() throws Exception
    {
        LocalDate date = LocalDate.of(2013, Month.AUGUST, 21);
        String value = MAPPER.writer()
        		.with(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        		.writeValueAsString(date);

        assertNotNull(value);
        assertEquals("[2013,8,21]", value);
    }

    @Test
    public void testSerializationAsString01() throws Exception
    {
        LocalDate date = LocalDate.of(1986, Month.JANUARY, 17);
        String value = MAPPER.writer()
        		.without(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        		.writeValueAsString(date);

        assertNotNull(value);
        assertEquals('"' + date.toString() + '"', value);
    }

    @Test
    public void testSerializationAsString02() throws Exception
    {
        LocalDate date = LocalDate.of(2013, Month.AUGUST, 21);
        String value = MAPPER.writer()
        		.without(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        		.writeValueAsString(date);
        assertNotNull(value);
        assertEquals('"' + date.toString() + '"', value);
    }

    @Test
    public void testSerializationWithTypeInfo01() throws Exception
    {
        ObjectMapper mapper = newMapperBuilder()
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .addMixIn(Temporal.class, MockObjectConfiguration.class)
                .build();
        LocalDate date = LocalDate.of(2005, Month.NOVEMBER, 5);
        String value = mapper.writeValueAsString(date);

        assertNotNull(value);
        assertEquals("[\"" + LocalDate.class.getName() + "\",\"" + date.toString() + "\"]", value);
    }

    // [modules-java8#46]
    @Test
    public void testSerializationWithTypeInfo02() throws Exception
    {
        final LocalDate localDate = LocalDate.of(2017, 12, 5);
        String json = MAPPER.writer()
                .with(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .writeValueAsString(new Holder46(localDate, localDate));
        assertEquals(a2q("{\"localDate\":[2017,12,5],\"object\":{\"java.time.LocalDate\":[2017,12,5]}}"),
                json);
    }
    
    @Test
    public void testConfigOverrides() throws Exception
    {
        ObjectMapper mapper = newMapperBuilder()
                .withConfigOverride(LocalDate.class,
                        o -> o.setFormat(JsonFormat.Value.forPattern("yyyy_MM_dd")))
                .build();
        LocalDate date = LocalDate.of(2005, Month.NOVEMBER, 5);
        VanillaWrapper input = new VanillaWrapper(date);
        final String EXP_DATE = "\"2005_11_05\"";
        String json = mapper.writeValueAsString(input);
        assertEquals("{\"value\":"+EXP_DATE+"}", json);
        assertEquals(EXP_DATE, mapper.writeValueAsString(date));

        // and read back, too
        VanillaWrapper output = mapper.readValue(json, VanillaWrapper.class);
        assertEquals(input.value, output.value);
        LocalDate date2 = mapper.readValue(EXP_DATE, LocalDate.class);
        assertEquals(date, date2);
    }

    @Test
    public void testConfigOverridesToEpochDay() throws Exception
    {
        ObjectMapper mapper = newMapperBuilder()
                .withConfigOverride(LocalDate.class,
                        o -> o.setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.NUMBER_INT)))
                .build();
        LocalDate date = LocalDate.ofEpochDay(1000);
        VanillaWrapper input = new VanillaWrapper(date);
        final String EXP_DATE = "1000";
        String json = mapper.writeValueAsString(input);
        assertEquals("{\"value\":"+EXP_DATE+"}", json);
        assertEquals(EXP_DATE, mapper.writeValueAsString(date));

        // and read back, too
        VanillaWrapper output = mapper.readValue(json, VanillaWrapper.class);
        assertEquals(input.value, output.value);
        LocalDate date2 = mapper.readValue(EXP_DATE, LocalDate.class);
        assertEquals(date, date2);
    }

    @Test
    public void testCustomFormatToEpochDay() throws Exception
    {
        EpochDayWrapper w = MAPPER.readValue("{\"value\": 1000}", EpochDayWrapper.class);
        LocalDate date = w.value;
        assertNotNull(date);
        assertEquals(LocalDate.ofEpochDay(1000), date);
    }
}
