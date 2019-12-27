package com.fasterxml.jackson.failing;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.databind.*;

// [databind#2527] Support Kotlin-style "is" properties
public class IsGetterRenaming2527Test extends BaseMapTest
{
    static class POJO2527 {
        private boolean isEnabled;

        protected POJO2527() { }
        public POJO2527(boolean b) {
            isEnabled = b;
        }

        public boolean getEnabled() { return isEnabled; }
        public void setEnabled(boolean b) { isEnabled = b; }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    public void testIsProperties() throws Exception
    {
        POJO2527 input = new POJO2527(true);
        final String json = MAPPER.writeValueAsString(input);

        Map<?, ?> props = MAPPER.readValue(json, Map.class);
        assertEquals(Collections.singletonMap("isEnabled", Boolean.TRUE),
                props);
        
        POJO2527 output = MAPPER.readValue(json, POJO2527.class);
        assertEquals(input.isEnabled, output.isEnabled);
    }
}
