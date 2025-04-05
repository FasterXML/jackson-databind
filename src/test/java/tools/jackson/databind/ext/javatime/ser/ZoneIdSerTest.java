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

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;
import tools.jackson.databind.ext.javatime.MockObjectConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ZoneIdSerTest extends DateTimeTestBase
{
    private ObjectMapper MAPPER = newMapper();

    private final ObjectMapper MOCK_OBJECT_MIXIN_MAPPER = mapperBuilder()
            .addMixIn(ZoneId.class, MockObjectConfiguration.class)
            .build();

    @Test
    public void testSerialization01() throws Exception
    {
        final String value = MAPPER.writeValueAsString(ZoneId.of("America/Chicago"));
        assertEquals("\"America/Chicago\"", value);
    }

    @Test
    public void testSerialization02() throws Exception
    {
        final String value = MAPPER.writeValueAsString(ZoneId.of("America/Anchorage"));
        assertEquals("\"America/Anchorage\"", value);
    }

    @Test
    public void testSerializationWithTypeInfo01() throws Exception
    {
        String value = MOCK_OBJECT_MIXIN_MAPPER.writeValueAsString(ZoneId.of("America/Denver"));
        assertEquals("[\"java.time.ZoneId\",\"America/Denver\"]", value);
    }
}
