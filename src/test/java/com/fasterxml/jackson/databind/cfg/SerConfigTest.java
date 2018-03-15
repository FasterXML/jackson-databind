package com.fasterxml.jackson.databind.cfg;

import java.util.Collections;

import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.databind.*;

public class SerConfigTest extends BaseMapTest
{
    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testSerConfig() throws Exception
    {
        SerializationConfig config = MAPPER.serializationConfig();
        assertTrue(config.hasSerializationFeatures(SerializationFeature.FAIL_ON_EMPTY_BEANS.getMask()));
        assertFalse(config.hasSerializationFeatures(SerializationFeature.CLOSE_CLOSEABLE.getMask()));
        assertEquals(ConfigOverrides.INCLUDE_ALL, config.getDefaultPropertyInclusion());
        assertEquals(ConfigOverrides.INCLUDE_ALL, config.getDefaultPropertyInclusion(String.class));
        assertFalse(config.useRootWrapping());

        assertNotSame(config, config.with(SerializationFeature.INDENT_OUTPUT,
                SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS));
        
        assertSame(config, config.withRootName((PropertyName) null)); // defaults to 'none'

        SerializationConfig newConfig = config.withRootName(PropertyName.construct("foobar"));
        assertNotSame(config, newConfig);
        assertTrue(newConfig.useRootWrapping());

        assertSame(config, config.with(config.getAttributes()));
        assertNotSame(config, config.with(new ContextAttributes.Impl(Collections.singletonMap("a", "b"))));

        assertNotNull(config.introspectDirectClassAnnotations(getClass()));
    }

    public void testGeneratorFeatures() throws Exception
    {
        SerializationConfig config = MAPPER.serializationConfig();
        assertFalse(config.isEnabled(JsonGenerator.Feature.ESCAPE_NON_ASCII));
        assertNotSame(config, config.with(JsonGenerator.Feature.ESCAPE_NON_ASCII));
        SerializationConfig newConfig = config.withFeatures(JsonGenerator.Feature.ESCAPE_NON_ASCII,
                JsonGenerator.Feature.IGNORE_UNKNOWN);
        assertNotSame(config, newConfig);
        assertTrue(newConfig.isEnabled(JsonGenerator.Feature.ESCAPE_NON_ASCII));

        // no change to settings, same object:
        assertSame(config, config.without(JsonGenerator.Feature.ESCAPE_NON_ASCII));
        assertSame(config, config.withoutFeatures(JsonGenerator.Feature.ESCAPE_NON_ASCII,
                JsonGenerator.Feature.IGNORE_UNKNOWN));
    }

    public void testFormatFeatures() throws Exception
    {
        SerializationConfig config = MAPPER.serializationConfig();
        SerializationConfig config2 = config.with(BogusFormatFeature.FF_DISABLED_BY_DEFAULT);
        assertNotSame(config, config2);
        SerializationConfig config3 = config.withFeatures(BogusFormatFeature.FF_DISABLED_BY_DEFAULT,
                BogusFormatFeature.FF_ENABLED_BY_DEFAULT);
        assertNotSame(config, config3);

        assertNotSame(config3, config3.without(BogusFormatFeature.FF_ENABLED_BY_DEFAULT));
        assertNotSame(config3, config3.withoutFeatures(BogusFormatFeature.FF_DISABLED_BY_DEFAULT,
                BogusFormatFeature.FF_ENABLED_BY_DEFAULT));
    }
}
