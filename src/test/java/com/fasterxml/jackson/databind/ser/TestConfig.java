package com.fasterxml.jackson.databind.ser;

import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.SerializationConfig;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Unit tests for checking handling of SerializationConfig.
 */
public class TestConfig
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper beans
    /**********************************************************
     */

    @JsonSerialize(include=JsonSerialize.Inclusion.NON_DEFAULT,
                   typing=JsonSerialize.Typing.STATIC)
    final static class Config { }

    final static class ConfigNone { }

    static class AnnoBean {
        public int getX() { return 1; }
        @SuppressWarnings("unused") @JsonProperty("y")
        private int getY() { return 2; }
    }

    /*
    /**********************************************************
    /* Main tests
    /**********************************************************
     */

    public void testDefaults()
    {
        ObjectMapper m = new ObjectMapper();
        SerializationConfig cfg = m.getSerializationConfig();

        // First, defaults:
        assertTrue(cfg.isEnabled(MapperFeature.USE_ANNOTATIONS));
        assertTrue(cfg.isEnabled(MapperFeature.AUTO_DETECT_GETTERS));
        assertTrue(cfg.isEnabled(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS));

        assertTrue(cfg.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));

        assertFalse(cfg.isEnabled(SerializationFeature.INDENT_OUTPUT));
        assertFalse(cfg.isEnabled(MapperFeature.USE_STATIC_TYPING));

        // since 1.3:
        assertTrue(cfg.isEnabled(MapperFeature.AUTO_DETECT_IS_GETTERS));
        // since 1.4
        
        assertTrue(cfg.isEnabled(SerializationFeature.FAIL_ON_EMPTY_BEANS));
        // since 1.5
        assertTrue(cfg.isEnabled(MapperFeature.DEFAULT_VIEW_INCLUSION));

    }

    public void testOverrideIntrospectors()
    {
        ObjectMapper m = new ObjectMapper();
        SerializationConfig cfg = m.getSerializationConfig();
        // and finally, ensure we could override introspectors
        cfg = cfg.withClassIntrospector(null); // no way to verify tho
        cfg = cfg.withAnnotationIntrospector(null);
        assertNull(cfg.getAnnotationIntrospector());
    }

    public void testMisc()
    {
        ObjectMapper m = new ObjectMapper();
        m.setDateFormat(null); // just to execute the code path
        assertNotNull(m.getSerializationConfig().toString()); // ditto
    }

    public void testIndentation() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        m.configure(SerializationFeature.INDENT_OUTPUT, true);
        Map<String,Integer> map = new HashMap<String,Integer>();
        map.put("a", Integer.valueOf(2));
        String result = serializeAsString(m, map).trim();
        // 02-Jun-2009, tatu: not really a clean way but...
        String lf = System.getProperty("line.separator");
        assertEquals("{"+lf+"  \"a\" : 2"+lf+"}", result);
    }

    public void testAnnotationsDisabled() throws Exception
    {
        // first: verify that annotation introspection is enabled by default
        ObjectMapper m = new ObjectMapper();
        assertTrue(m.isEnabled(MapperFeature.USE_ANNOTATIONS));
        Map<String,Object> result = writeAndMap(m, new AnnoBean());
        assertEquals(2, result.size());

        m = new ObjectMapper();
        m.configure(MapperFeature.USE_ANNOTATIONS, false);
        result = writeAndMap(m, new AnnoBean());
        assertEquals(1, result.size());
    }

    /**
     * Test for verifying working of [JACKSON-191]
     */
    public void testProviderConfig() throws Exception   
    {
        ObjectMapper mapper = new ObjectMapper();
        assertEquals(0, mapper.getSerializerProvider().cachedSerializersCount());
        // and then should get one constructed for:
        Map<String,Object> result = this.writeAndMap(mapper, new AnnoBean());
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(1), result.get("x"));
        assertEquals(Integer.valueOf(2), result.get("y"));

        /* Note: it is 2 because we'll also get serializer for basic 'int', not
         * just AnnoBean
         */
        /* 12-Jan-2010, tatus: Actually, probably more, if and when we typing
         *   aspects are considered (depending on what is cached)
         */
        int count = mapper.getSerializerProvider().cachedSerializersCount();
        if (count < 2) {
            fail("Should have at least 2 cached serializers, got "+count);
        }
        mapper.getSerializerProvider().flushCachedSerializers();
        assertEquals(0, mapper.getSerializerProvider().cachedSerializersCount());
    }

}
