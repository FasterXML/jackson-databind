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
import java.time.MonthDay;
import java.time.temporal.TemporalAccessor;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;
import tools.jackson.databind.ext.javatime.MockObjectConfiguration;

import com.fasterxml.jackson.annotation.JsonFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MonthDaySerTest
    extends DateTimeTestBase
{
    private ObjectMapper MAPPER = newMapper();

    static class ShapeIntWrapper {
        @JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
        public MonthDay value;
        public ShapeIntWrapper() { }
        public ShapeIntWrapper(MonthDay v) { value = v; }
    }

    static class NoShapeIntWrapper {
        public MonthDay value;
        public NoShapeIntWrapper() { }
        public NoShapeIntWrapper(MonthDay v) { value = v; }
    }

    static class FrBean {
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MMM-dd", locale = "fr")
        public MonthDay value;
        public FrBean() { }
        public FrBean(MonthDay v) { value = v; }
    }

    static class ShapeArrayBean {
        @JsonFormat(shape = JsonFormat.Shape.ARRAY)
        public MonthDay value;
        public ShapeArrayBean() { }
        public ShapeArrayBean(MonthDay v) { value = v; }
    }

    @Test
    public void testSerialization01() throws Exception
    {
        assertEquals("\"--01-17\"",
                MAPPER.writeValueAsString(MonthDay.of(Month.JANUARY, 17)));
    }

    @Test
    public void testSerialization02() throws Exception
    {
        assertEquals("\"--08-21\"",
                MAPPER.writeValueAsString(MonthDay.of(Month.AUGUST, 21)));
    }

    @Test
    public void testSerializationWithTypeInfo01() throws Exception
    {
        final ObjectMapper mapper = mapperBuilder()
                .addMixIn(TemporalAccessor.class, MockObjectConfiguration.class)
                .build();
        MonthDay monthDay = MonthDay.of(Month.NOVEMBER, 5);
        String value = mapper.writeValueAsString(monthDay);
        assertEquals("[\"" + MonthDay.class.getName() + "\",\"--11-05\"]", value);
    }

    // ShapeInt Test
    @Test
    public void testSerializationWithShapeInt() throws Exception
    {
        // One with shape
        String json = MAPPER.writeValueAsString(new ShapeIntWrapper(MonthDay.of(Month.MARCH, 17)));
        assertEquals("{\"value\":[3,17]}", json);

        // One without shape
        json = MAPPER.writeValueAsString(new NoShapeIntWrapper(MonthDay.of(Month.MARCH, 17)));
        assertEquals("{\"value\":\"--03-17\"}", json);
    }

    @Test
    public void testSerializationWithFrLocale() throws Exception
    {
        String json = MAPPER.writeValueAsString(new FrBean(MonthDay.of(Month.MARCH, 17)));
        assertEquals("{\"value\":\"mars-17\"}", json);
    }

    @Test
    public void testSerializationWithShapeArray() throws Exception
    {
        String json = MAPPER.writeValueAsString(new ShapeArrayBean(MonthDay.of(Month.DECEMBER, 31)));
        assertEquals("{\"value\":[12,31]}", json);
    }
}
