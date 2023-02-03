package tools.jackson.databind.cfg;

import java.util.Collections;

import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.json.JsonReadFeature;

import tools.jackson.databind.*;

public class DeserializationConfigTest extends BaseMapTest
{
    private final ObjectMapper MAPPER = newJsonMapper();

    public void testFeatureDefaults()
    {
        ObjectMapper m = new ObjectMapper();
        DeserializationConfig cfg = m.deserializationConfig();

        // Expected defaults:
        assertTrue(cfg.isEnabled(MapperFeature.USE_ANNOTATIONS));
        assertFalse(cfg.isEnabled(MapperFeature.USE_GETTERS_AS_SETTERS)); // 3.0
        assertTrue(cfg.isEnabled(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS));

        assertFalse(cfg.isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS));
        assertFalse(cfg.isEnabled(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS));

        assertTrue(cfg.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
    }

    public void testBasicFeatures() throws Exception
    {
        DeserializationConfig config = MAPPER.deserializationConfig();
        assertTrue(config.hasDeserializationFeatures(DeserializationFeature.EAGER_DESERIALIZER_FETCH.getMask()));
        assertFalse(config.hasDeserializationFeatures(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY.getMask()));
        assertTrue(config.hasSomeOfFeatures(DeserializationFeature.EAGER_DESERIALIZER_FETCH.getMask()
                + DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY.getMask()));
        assertFalse(config.hasSomeOfFeatures(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY.getMask()));

        assertNotSame(config, config.with(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT,
                DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES));
    }

    public void testStreamReadFeatures() throws Exception
    {
        DeserializationConfig config = MAPPER.deserializationConfig();

        assertNotSame(config, config.with(StreamReadFeature.IGNORE_UNDEFINED));
        assertNotSame(config, config.withFeatures(StreamReadFeature.IGNORE_UNDEFINED,
                StreamReadFeature.STRICT_DUPLICATE_DETECTION));

        assertSame(config, config.without(StreamReadFeature.IGNORE_UNDEFINED));
        assertSame(config, config.withoutFeatures(StreamReadFeature.IGNORE_UNDEFINED,
                StreamReadFeature.STRICT_DUPLICATE_DETECTION));
    }

    public void testJsonReadFeatures() throws Exception
    {
        final JsonReadFeature DISABLED_BY_DEFAULT = JsonReadFeature.ALLOW_JAVA_COMMENTS;
        final JsonReadFeature DISABLED_BY_DEFAULT2 = JsonReadFeature.ALLOW_MISSING_VALUES;
        DeserializationConfig config = MAPPER.deserializationConfig();
        DeserializationConfig config2 = config.with(DISABLED_BY_DEFAULT);
        assertNotSame(config, config2);
        DeserializationConfig config3 = config.withFeatures(DISABLED_BY_DEFAULT2,
                DISABLED_BY_DEFAULT);
        assertNotSame(config, config3);

        assertNotSame(config3, config3.without(DISABLED_BY_DEFAULT));
        assertNotSame(config3, config3.withoutFeatures(DISABLED_BY_DEFAULT2,
                DISABLED_BY_DEFAULT));
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

    public void testMisc() throws Exception
    {
        DeserializationConfig config = MAPPER.deserializationConfig();
        assertEquals(ConfigOverrides.INCLUDE_DEFAULT, config.getDefaultPropertyInclusion());
        assertEquals(ConfigOverrides.INCLUDE_DEFAULT, config.getDefaultPropertyInclusion(String.class));

        assertSame(config, config.withRootName((PropertyName) null)); // defaults to 'none'

        DeserializationConfig newConfig = config.withRootName(PropertyName.construct("foobar"));
        assertNotSame(config, newConfig);
        config = newConfig;
        assertSame(config, config.withRootName(PropertyName.construct("foobar")));

        assertSame(config, config.with(config.getAttributes()));
        assertNotSame(config, config.with(new ContextAttributes.Impl(Collections.singletonMap("a", "b"))));

        // should also be able to introspect:
//        assertNotNull(config.introspectDirectClassAnnotations(getClass()));
    }
}
