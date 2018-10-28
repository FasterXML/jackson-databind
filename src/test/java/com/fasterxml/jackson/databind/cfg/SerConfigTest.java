package com.fasterxml.jackson.databind.cfg;

import java.util.Collections;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
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

//        assertNotNull(config.introspectDirectClassAnnotations(getClass()));
    }

    public void testStreamWriteFeatures() throws Exception
    {
        SerializationConfig config = MAPPER.serializationConfig();
        assertFalse(config.hasFormatFeature(JsonWriteFeature.ESCAPE_NON_ASCII));
        assertNotSame(config, config.with(JsonWriteFeature.ESCAPE_NON_ASCII));
        SerializationConfig newConfig = config.withFeatures(JsonGenerator.Feature.IGNORE_UNKNOWN);
        assertNotSame(config, newConfig);
        assertTrue(newConfig.isEnabled(JsonGenerator.Feature.IGNORE_UNKNOWN));

        // no change to settings, same object:
        assertSame(config, config.without(JsonWriteFeature.ESCAPE_NON_ASCII));
        assertSame(config, config.withoutFeatures(JsonGenerator.Feature.IGNORE_UNKNOWN));
    }

    public void testFormatFeatures() throws Exception
    {
        final JsonWriteFeature DUSABLED_BY_DEFAULT = JsonWriteFeature.ESCAPE_NON_ASCII;
        final JsonWriteFeature ENABLED_BY_DEFAULT = JsonWriteFeature.QUOTE_FIELD_NAMES;

        SerializationConfig config = MAPPER.serializationConfig();
        // feature that is NOT enabled by default
        SerializationConfig config2 = config.with(DUSABLED_BY_DEFAULT);
        assertNotSame(config, config2);
        // and then with one that IS enabled by default:
        SerializationConfig config3 = config.withFeatures(DUSABLED_BY_DEFAULT, ENABLED_BY_DEFAULT);
        assertNotSame(config, config3);

        assertNotSame(config3, config3.without(ENABLED_BY_DEFAULT));
        assertNotSame(config3, config3.withoutFeatures(DUSABLED_BY_DEFAULT,
                ENABLED_BY_DEFAULT));
    }
}
