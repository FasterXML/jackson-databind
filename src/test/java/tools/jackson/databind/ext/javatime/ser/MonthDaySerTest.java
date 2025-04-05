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

import static org.junit.jupiter.api.Assertions.*;

public class MonthDaySerTest
    extends DateTimeTestBase
{
    private ObjectMapper MAPPER = newMapper();

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
}
