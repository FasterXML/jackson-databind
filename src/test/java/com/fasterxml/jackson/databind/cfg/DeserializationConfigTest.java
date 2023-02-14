package com.fasterxml.jackson.databind.cfg;

import java.util.Collections;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.ClassIntrospector;

public class DeserializationConfigTest extends BaseMapTest
{
    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testFeatureDefaults()
    {
        ObjectMapper m = new ObjectMapper();
        DeserializationConfig cfg = m.getDeserializationConfig();

        // Expected defaults:
        assertTrue(cfg.isEnabled(MapperFeature.USE_ANNOTATIONS));
        assertTrue(cfg.isEnabled(MapperFeature.AUTO_DETECT_SETTERS));
        assertTrue(cfg.isEnabled(MapperFeature.AUTO_DETECT_CREATORS));
        assertTrue(cfg.isEnabled(MapperFeature.USE_GETTERS_AS_SETTERS));
        assertTrue(cfg.isEnabled(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS));

        assertFalse(cfg.isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS));
        assertFalse(cfg.isEnabled(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS));

        assertTrue(cfg.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
    }

    public void testBasicFeatures() throws Exception
    {
        DeserializationConfig config = MAPPER.getDeserializationConfig();
        assertTrue(config.hasDeserializationFeatures(DeserializationFeature.EAGER_DESERIALIZER_FETCH.getMask()));
        assertFalse(config.hasDeserializationFeatures(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY.getMask()));
        assertTrue(config.hasSomeOfFeatures(DeserializationFeature.EAGER_DESERIALIZER_FETCH.getMask()
                + DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY.getMask()));
        assertFalse(config.hasSomeOfFeatures(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY.getMask()));

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
        assertNotSame(config, config.with(JsonReadFeature.ALLOW_JAVA_COMMENTS));
        assertNotSame(config, config.withFeatures(JsonReadFeature.ALLOW_JAVA_COMMENTS,
                JsonReadFeature.ALLOW_MISSING_VALUES));

        assertNotSame(config, config.without(JsonReadFeature.ALLOW_JAVA_COMMENTS));
        assertNotSame(config, config.withoutFeatures(JsonReadFeature.ALLOW_JAVA_COMMENTS,
                JsonReadFeature.ALLOW_MISSING_VALUES));
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

    /* Test to verify that we don't overflow number of features; if we
     * hit the limit, need to change implementation -- this test just
     * gives low-water mark
     */
    public void testEnumIndexes()
    {
        int max = 0;

        for (DeserializationFeature f : DeserializationFeature.values()) {
            max = Math.max(max, f.ordinal());
        }
        if (max >= 31) { // 31 is actually ok; 32 not
            fail("Max number of DeserializationFeature enums reached: "+max);
        }
    }

    public void testOverrideIntrospectors()
    {
        ObjectMapper m = new ObjectMapper();
        DeserializationConfig cfg = m.getDeserializationConfig();
        // and finally, ensure we could override introspectors
        cfg = cfg.with((ClassIntrospector) null); // no way to verify tho
        cfg = cfg.with((AnnotationIntrospector) null);
        assertNull(cfg.getAnnotationIntrospector());
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
