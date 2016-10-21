package com.fasterxml.jackson.databind.cfg;

import java.util.Collections;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;

public class DeserConfigTest extends BaseMapTest
{
    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testBasicFeatures() throws Exception
    {
        DeserializationConfig config = MAPPER.getDeserializationConfig();
        assertTrue(config.hasDeserializationFeatures(DeserializationFeature.EAGER_DESERIALIZER_FETCH.getMask()));
        assertFalse(config.hasDeserializationFeatures(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY.getMask()));
        assertTrue(config.hasSomeOfFeatures(DeserializationFeature.EAGER_DESERIALIZER_FETCH.getMask()
                + DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY.getMask()));
        // if no changes then same config object
        assertSame(config, config.without());
        assertSame(config, config.with());
        assertSame(config, config.with(MAPPER.getSubtypeResolver()));

        // and then change
        DeserializationConfig newConfig = config.with(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        assertNotSame(config, newConfig);
        config = newConfig;
        
        // but another attempt with no real change returns same
        assertSame(config, config.with(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES));
        assertNotSame(config, config.with(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, false));

        assertNotSame(config, config.with(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT,
                DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES));
    }

    public void testParserFeatures() throws Exception
    {
        DeserializationConfig config = MAPPER.getDeserializationConfig();
        assertNotSame(config, config.with(JsonParser.Feature.ALLOW_COMMENTS));
        assertNotSame(config, config.withFeatures(JsonParser.Feature.ALLOW_COMMENTS,
                JsonParser.Feature.ALLOW_MISSING_VALUES));

        assertNotSame(config, config.without(JsonParser.Feature.ALLOW_COMMENTS));
        assertNotSame(config, config.withoutFeatures(JsonParser.Feature.ALLOW_COMMENTS,
                JsonParser.Feature.ALLOW_MISSING_VALUES));
    }

    public void testFormatFeatures() throws Exception
    {
        DeserializationConfig config = MAPPER.getDeserializationConfig();
        assertNotSame(config, config.with(BogusFormatFeature.FF_DISABLED_BY_DEFAULT));
        assertNotSame(config, config.withFeatures(BogusFormatFeature.FF_DISABLED_BY_DEFAULT,
                BogusFormatFeature.FF_ENABLED_BY_DEFAULT));
        assertNotSame(config, config.without(BogusFormatFeature.FF_ENABLED_BY_DEFAULT));
        assertNotSame(config, config.withoutFeatures(BogusFormatFeature.FF_DISABLED_BY_DEFAULT,
                BogusFormatFeature.FF_ENABLED_BY_DEFAULT));
    }
    
    public void testMisc() throws Exception
    {
        DeserializationConfig config = MAPPER.getDeserializationConfig();
        assertEquals(JsonInclude.Value.empty(), config.getDefaultPropertyInclusion());
        assertEquals(JsonInclude.Value.empty(), config.getDefaultPropertyInclusion(String.class));

        
        assertSame(config, config.withRootName((PropertyName) null)); // defaults to 'none'

        DeserializationConfig newConfig = config.withRootName(PropertyName.construct("foobar"));
        assertNotSame(config, newConfig);
        config = newConfig;
        assertSame(config, config.withRootName(PropertyName.construct("foobar")));

        assertSame(config, config.with(config.getAttributes()));
        assertNotSame(config, config.with(new ContextAttributes.Impl(Collections.singletonMap("a", "b"))));

        // should also be able to introspect:
        assertNotNull(config.introspectDirectClassAnnotations(getClass()));
    }
}
