package com.fasterxml.jackson.databind.cfg;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.databind.*;

public class ConfigOverridesTest extends BaseMapTest
{
    public void testSnapshot() throws Exception
    {
        ConfigOverrides co = new ConfigOverrides();
        co.findOrCreateOverride(String.class)
            .setVisibility(JsonAutoDetect.Value.construct(PropertyAccessor.SETTER,
                    Visibility.NONE));
        // simplest verification of snapshot(): check that string repr matches
        assertEquals(co.toString(),
                co.snapshot().toString());
    }
}
