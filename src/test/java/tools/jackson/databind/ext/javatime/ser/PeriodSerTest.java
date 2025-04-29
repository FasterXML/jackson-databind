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

import java.time.Period;
import java.time.temporal.TemporalAmount;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;
import tools.jackson.databind.ext.javatime.MockObjectConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PeriodSerTest extends DateTimeTestBase
{
    private final ObjectMapper MAPPER = newMapper();

    @Test
    public void testSerialization01() throws Exception
    {
        assertEquals(q("P1Y6M15D"), MAPPER.writeValueAsString(Period.of(1, 6, 15)));
    }

    @Test
    public void testSerialization02() throws Exception
    {
        assertEquals(q("P21D"), MAPPER.writeValueAsString(Period.of(0, 0, 21)));
    }

    @Test
    public void testSerializationWithTypeInfo01() throws Exception
    {
        Period period = Period.of(5, 1, 12);
        final ObjectMapper mapper = mapperBuilder()
                .addMixIn(TemporalAmount.class, MockObjectConfiguration.class)
                .build();
        String value = mapper.writeValueAsString(period);
        assertEquals("[" + q(Period.class.getName()) + ",\"P5Y1M12D\"]", value);
    }
}
