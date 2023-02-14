package com.fasterxml.jackson.databind.cfg;

import java.util.Collections;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.*;

public class SerConfigTest extends BaseMapTest
{
    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testSerConfig() throws Exception
    {
        SerializationConfig config = MAPPER.getSerializationConfig();
        assertTrue(config.hasSerializationFeatures(SerializationFeature.FAIL_ON_EMPTY_BEANS.getMask()));
        assertFalse(config.hasSerializationFeatures(SerializationFeature.CLOSE_CLOSEABLE.getMask()));
        assertEquals(JsonInclude.Value.empty(), config.getDefaultPropertyInclusion());
        assertEquals(JsonInclude.Value.empty(), config.getDefaultPropertyInclusion(String.class));
        assertFalse(config.useRootWrapping());

        // if no changes then same config object
        assertSame(config, config.without());
        assertSame(config, config.with());
        assertSame(config, config.with(MAPPER.getSubtypeResolver()));

        // and then change
        SerializationConfig newConfig = config.with(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        assertNotSame(config, newConfig);
        config = newConfig;
        assertSame(config, config.with(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES));
        assertNotSame(config, config.with(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, false));

        assertNotSame(config, config.with(SerializationFeature.INDENT_OUTPUT,
                SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS));

        assertSame(config, config.withRootName((PropertyName) null)); // defaults to 'none'

        newConfig = config.withRootName(PropertyName.construct("foobar"));
        assertNotSame(config, newConfig);
        assertTrue(newConfig.useRootWrapping());

        assertSame(config, config.with(config.getAttributes()));
        assertNotSame(config, config.with(new ContextAttributes.Impl(Collections.singletonMap("a", "b"))));

        assertNotNull(config.introspectDirectClassAnnotations(getClass()));
    }

    public void testGeneratorFeatures() throws Exception
    {
        SerializationConfig config = MAPPER.getSerializationConfig();
        assertNotSame(config, config.with(JsonWriteFeature.ESCAPE_NON_ASCII));
        SerializationConfig newConfig = config.withFeatures(JsonGenerator.Feature.IGNORE_UNKNOWN);
        assertNotSame(config, newConfig);

        assertNotSame(config, config.without(JsonWriteFeature.ESCAPE_NON_ASCII));
        assertNotSame(config, config.withoutFeatures(JsonGenerator.Feature.IGNORE_UNKNOWN));
    }

    public void testFormatFeatures() throws Exception
    {
        SerializationConfig config = MAPPER.getSerializationConfig();
        assertNotSame(config, config.with(BogusFormatFeature.FF_DISABLED_BY_DEFAULT));
        assertNotSame(config, config.withFeatures(BogusFormatFeature.FF_DISABLED_BY_DEFAULT,
                BogusFormatFeature.FF_ENABLED_BY_DEFAULT));
        assertNotSame(config, config.without(BogusFormatFeature.FF_ENABLED_BY_DEFAULT));
        assertNotSame(config, config.withoutFeatures(BogusFormatFeature.FF_DISABLED_BY_DEFAULT,
                BogusFormatFeature.FF_ENABLED_BY_DEFAULT));
    }
}
