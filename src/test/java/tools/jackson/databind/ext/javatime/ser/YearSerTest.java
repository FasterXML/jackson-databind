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

import java.time.Year;
import java.time.temporal.Temporal;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFormat;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;
import tools.jackson.databind.ext.javatime.MockObjectConfiguration;

import static org.junit.jupiter.api.Assertions.*;

public class YearSerTest extends DateTimeTestBase
{
    final static class YearAsStringWrapper {
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        public Year value;

        public YearAsStringWrapper(Year value) {
            this.value = value;
        }
    }

    // Defaults fine: year only serialized as String with explicit
    // overrides
    private final ObjectMapper MAPPER = newMapper();

    @Test
    public void testDefaultSerialization() throws Exception
    {
        assertEquals("1986",
                MAPPER.writeValueAsString(Year.of(1986)));
        assertEquals("2013",
                MAPPER.writeValueAsString(Year.of(2013)));
    }

    @Test
    public void testAsStringSerializationViaAnnotation() throws Exception
    {
        assertEquals(a2q("{'value':'1972'}"),
                MAPPER.writeValueAsString(new YearAsStringWrapper(Year.of(1972))));
    }

    @Test
    public void testAsStringSerializationViaFormatConfig() throws Exception
    {
        final ObjectMapper asStringMapper = mapperBuilder()
                .withConfigOverride(Year.class, o -> o.setFormat(
                        JsonFormat.Value.forShape(JsonFormat.Shape.STRING)))
                .build();

        assertEquals(q("2025"),
                asStringMapper.writeValueAsString(Year.of(2025)));
    }

    @Test
    public void testSerializationWithTypeInfo() throws Exception
    {
        ObjectMapper mapper = newMapperBuilder()
                .addMixIn(Temporal.class, MockObjectConfiguration.class)
                .build();
        String value = mapper.writeValueAsString(Year.of(2005));
        assertEquals("[\"" + Year.class.getName() + "\",2005]", value);
    }
}
