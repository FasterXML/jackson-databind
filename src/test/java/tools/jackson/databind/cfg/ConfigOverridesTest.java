package tools.jackson.databind.cfg;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConfigOverridesTest
{
    @Test
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
