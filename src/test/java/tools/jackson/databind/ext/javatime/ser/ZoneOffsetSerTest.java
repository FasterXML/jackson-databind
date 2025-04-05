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

import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;
import tools.jackson.databind.ext.javatime.MockObjectConfiguration;

import static org.junit.jupiter.api.Assertions.*;

public class ZoneOffsetSerTest extends DateTimeTestBase
{
    private final ObjectMapper MAPPER = newMapper();

    @Test
    public void testSerialization01() throws Exception
    {
        ZoneOffset offset = ZoneOffset.of("Z");
        String value = MAPPER.writeValueAsString(offset);
        assertEquals("\"Z\"", value);
    }

    @Test
    public void testSerialization02() throws Exception
    {
        ZoneOffset offset = ZoneOffset.of("+0300");
        String value = MAPPER.writeValueAsString(offset);
        assertEquals("\"+03:00\"", value);
    }

    @Test
    public void testSerialization03() throws Exception
    {
        ZoneOffset offset = ZoneOffset.of("-0630");
        String value = MAPPER.writeValueAsString(offset);
        assertEquals("\"-06:30\"", value);
    }

    @Test
    public void testSerializationWithTypeInfo03() throws Exception
    {
        ObjectMapper mapper = newMapperBuilder()
                .addMixIn(ZoneId.class, MockObjectConfiguration.class)
                .build();
        ZoneOffset offset = ZoneOffset.of("+0415");
        String value = mapper.writeValueAsString(offset);
        assertEquals("[\"" + ZoneOffset.class.getName() + "\",\"+04:15\"]", value);
    }
}
