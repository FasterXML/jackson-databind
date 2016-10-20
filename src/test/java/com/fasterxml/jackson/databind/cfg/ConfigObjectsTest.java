package com.fasterxml.jackson.databind.cfg;

import java.util.Collections;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.ContextAttributes;
import com.fasterxml.jackson.databind.jsontype.SubtypeResolver;
import com.fasterxml.jackson.databind.jsontype.impl.StdSubtypeResolver;

public class ConfigObjectsTest extends BaseMapTest
{
    static class Base { }
    static class Sub extends Base { }

    private final ObjectMapper MAPPER = new ObjectMapper();
    
    public void testSubtypeResolver() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SubtypeResolver res = mapper.getSubtypeResolver();
        assertTrue(res instanceof StdSubtypeResolver);

        StdSubtypeResolver repl = new StdSubtypeResolver();
        repl.registerSubtypes(Sub.class);
        mapper.setSubtypeResolver(repl);
        assertSame(repl, mapper.getSubtypeResolver());
    }

    public void testDeserConfig() throws Exception
    {
        DeserializationConfig config = MAPPER.getDeserializationConfig();
        assertTrue(config.hasDeserializationFeatures(DeserializationFeature.EAGER_DESERIALIZER_FETCH.getMask()));
        assertFalse(config.hasDeserializationFeatures(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY.getMask()));
        assertTrue(config.hasSomeOfFeatures(DeserializationFeature.EAGER_DESERIALIZER_FETCH.getMask()
                + DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY.getMask()));
        assertEquals(JsonInclude.Value.empty(), config.getDefaultPropertyInclusion());
        assertEquals(JsonInclude.Value.empty(), config.getDefaultPropertyInclusion(String.class));

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
        
        assertSame(config, config.withRootName((PropertyName) null)); // defaults to 'none'

        newConfig = config.withRootName(PropertyName.construct("foobar"));
        assertNotSame(config, newConfig);
        config = newConfig;
        assertSame(config, config.withRootName(PropertyName.construct("foobar")));

        assertSame(config, config.with(config.getAttributes()));
        assertNotSame(config, config.with(new ContextAttributes.Impl(Collections.singletonMap("a", "b"))));

        // should also be able to introspect:
        assertNotNull(config.introspectDirectClassAnnotations(getClass()));
    }

    public void testSerConfig() throws Exception
    {
        SerializationConfig config = MAPPER.getSerializationConfig();
        assertTrue(config.hasSerializationFeatures(SerializationFeature.FAIL_ON_EMPTY_BEANS.getMask()));
        assertFalse(config.hasSerializationFeatures(SerializationFeature.CLOSE_CLOSEABLE.getMask()));
        assertEquals(JsonInclude.Value.empty(), config.getDefaultPropertyInclusion());
        assertEquals(JsonInclude.Value.empty(), config.getDefaultPropertyInclusion(String.class));

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
        assertNotSame(config, config.withRootName(PropertyName.construct("foobar")));
        assertSame(config, config.with(config.getAttributes()));
        assertNotSame(config, config.with(new ContextAttributes.Impl(Collections.singletonMap("a", "b"))));

        assertNotNull(config.introspectDirectClassAnnotations(getClass()));
    }
}
