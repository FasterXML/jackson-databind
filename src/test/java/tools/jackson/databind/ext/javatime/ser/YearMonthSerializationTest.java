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

import java.time.Month;
import java.time.YearMonth;
import java.time.temporal.Temporal;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;
import tools.jackson.databind.ext.javatime.MockObjectConfiguration;

import static org.junit.jupiter.api.Assertions.*;

public class YearMonthSerializationTest
	extends DateTimeTestBase
{
    private static class SimpleAggregate
    {
        @JsonProperty("yearMonth")
        @JsonFormat(pattern = "yyMM")
        final YearMonth yearMonth;

        @JsonCreator
        SimpleAggregate(@JsonProperty("yearMonth") YearMonth yearMonth)
        {
            this.yearMonth = yearMonth;
        }
    }

    private final ObjectMapper MAPPER = newMapper();

    @Test
    public void testSerializationAsTimestamp01() throws Exception
    {
        YearMonth yearMonth = YearMonth.of(1986, Month.JANUARY);
        String value = MAPPER.writer()
        		.with(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        		.writeValueAsString(yearMonth);

        assertNotNull(value);
        assertEquals("[1986,1]", value);
    }

    @Test
    public void testSerializationAsTmestamp02() throws Exception
    {
        YearMonth yearMonth = YearMonth.of(2013, Month.AUGUST);
        String value = MAPPER.writer()
        		.with(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        		.writeValueAsString(yearMonth);

        assertNotNull(value);
        assertEquals("[2013,8]", value);
    }

    @Test
    public void testSerializationAsString01() throws Exception
    {
        YearMonth yearMonth = YearMonth.of(1986, Month.JANUARY);
        String value = MAPPER.writer()
        		.without(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        		.writeValueAsString(yearMonth);

        assertNotNull(value);
        assertEquals('"' + yearMonth.toString() + '"', value);
    }

    @Test
    public void testSerializationAsString02() throws Exception
    {
        YearMonth yearMonth = YearMonth.of(2013, Month.AUGUST);
        String value = MAPPER.writer()
        		.without(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        		.writeValueAsString(yearMonth);
        assertEquals('"' + yearMonth.toString() + '"', value);
    }

    @Test
    public void testSerializationWithTypeInfo01() throws Exception
    {
        YearMonth yearMonth = YearMonth.of(2005, Month.NOVEMBER);
        ObjectMapper mapper = newMapperBuilder()
        		.configure(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, false)
               .addMixIn(Temporal.class, MockObjectConfiguration.class)
        		.build();
        String value = mapper.writeValueAsString(yearMonth);
        assertEquals("[\"" + YearMonth.class.getName() + "\",\"" + yearMonth.toString() + "\"]", value);
    }

    @Test
    public void testDeserializationAsTimestamp01() throws Exception
    {
        YearMonth yearMonth = YearMonth.of(1986, Month.JANUARY);
        YearMonth value = MAPPER.readValue("[1986,1]", YearMonth.class);
        assertEquals(yearMonth, value);
    }

    @Test
    public void testDeserializationAsTimestamp02() throws Exception
    {
        YearMonth yearMonth = YearMonth.of(2013, Month.AUGUST);
        YearMonth value = MAPPER.readValue("[2013,8]", YearMonth.class);
        assertEquals(yearMonth, value);
    }

    @Test
    public void testDeserializationAsString01() throws Exception
    {
        YearMonth yearMonth = YearMonth.of(1986, Month.JANUARY);
        YearMonth value = MAPPER.readValue('"' + yearMonth.toString() + '"', YearMonth.class);

        assertNotNull(value);
        assertEquals(yearMonth, value);
    }

    @Test
    public void testDeserializationAsString02() throws Exception
    {
        YearMonth yearMonth = YearMonth.of(2013, Month.AUGUST);
        YearMonth value = this.MAPPER.readValue('"' + yearMonth.toString() + '"', YearMonth.class);
        assertEquals(yearMonth, value);
    }

    @Test
    public void testDeserializationWithTypeInfo01() throws Exception
    {
        YearMonth yearMonth = YearMonth.of(2005, Month.NOVEMBER);

        ObjectMapper mapper = newMapperBuilder()
        		.addMixIn(Temporal.class, MockObjectConfiguration.class)
        		.build();
        Temporal value = mapper.readValue("[\"" + YearMonth.class.getName() + "\",\"" + yearMonth.toString() + "\"]", Temporal.class);
        assertInstanceOf(YearMonth.class, value, "The value should be a YearMonth.");
        assertEquals(yearMonth, value);
    }

    @Test
    public void testSerializationWithPattern01() throws Exception
    {
        YearMonth yearMonth = YearMonth.of(2013, Month.AUGUST);
        SimpleAggregate simpleAggregate = new SimpleAggregate(yearMonth);
        String value = MAPPER.writeValueAsString(simpleAggregate);
        assertEquals("{\"yearMonth\":\"1308\"}", value);
    }

    @Test
    public void testDeserializationWithPattern01() throws Exception
    {
        YearMonth yearMonth = YearMonth.of(2013, Month.AUGUST);
        SimpleAggregate simpleAggregate = new SimpleAggregate(yearMonth);

        SimpleAggregate value = MAPPER.readValue("{\"yearMonth\":\"1308\"}", SimpleAggregate.class);
        assertEquals(simpleAggregate.yearMonth, value.yearMonth);
    }
}
