package com.fasterxml.jackson.databind;

import java.io.*;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonNode;

import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.databind.type.TypeFactory;

public class TestObjectMapper extends BaseMapTest
{
    static class Bean {
        int value = 3;
        
        public void setX(int v) { value = v; }
    }
    
    public void testProps()
    {
        ObjectMapper m = new ObjectMapper();
        // should have default factory
        assertNotNull(m.getNodeFactory());
        JsonNodeFactory nf = JsonNodeFactory.instance;
        m.setNodeFactory(nf);
        assertSame(nf, m.getNodeFactory());
    }

    public void testSupport()
    {
        ObjectMapper m = new ObjectMapper();
        assertTrue(m.canSerialize(String.class));

        assertTrue(m.canDeserialize(TypeFactory.defaultInstance().constructType(String.class)));
    }

    public void testTreeRead() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        String JSON = "{ }";
        JsonNode n = m.readTree(JSON);
        assertTrue(n instanceof ObjectNode);

        n = m.readTree(new StringReader(JSON));
        assertTrue(n instanceof ObjectNode);

        n = m.readTree(new ByteArrayInputStream(JSON.getBytes("UTF-8")));
        assertTrue(n instanceof ObjectNode);
    }

    // Test to ensure that we can check property ordering defaults...
    public void testConfigForPropertySorting() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        
        // sort-alphabetically is disabled by default:
        assertFalse(m.isEnabled(MapperConfig.Feature.SORT_PROPERTIES_ALPHABETICALLY));
        SerializationConfig sc = m.getSerializationConfig();
        assertFalse(sc.isEnabled(MapperConfig.Feature.SORT_PROPERTIES_ALPHABETICALLY));
        assertFalse(sc.shouldSortPropertiesAlphabetically());
        DeserializationConfig dc = m.getDeserializationConfig();
        assertFalse(dc.shouldSortPropertiesAlphabetically());

        // but when enabled, should be visible:
        m.enable(MapperConfig.Feature.SORT_PROPERTIES_ALPHABETICALLY);
        sc = m.getSerializationConfig();
        assertTrue(sc.isEnabled(MapperConfig.Feature.SORT_PROPERTIES_ALPHABETICALLY));
        assertTrue(sc.shouldSortPropertiesAlphabetically());
        dc = m.getDeserializationConfig();
        // and not just via SerializationConfig, but also via DeserializationConfig
        assertTrue(dc.isEnabled(MapperConfig.Feature.SORT_PROPERTIES_ALPHABETICALLY));
        assertTrue(dc.shouldSortPropertiesAlphabetically());
    }


    public void testJsonFactoryLinkage()
    {
        // first, implicit factory, giving implicit linkage
        ObjectMapper m = new ObjectMapper();
        assertSame(m, m.getJsonFactory().getCodec());

        // and then explicit factory, which should also be implicitly linked
        JsonFactory f = new JsonFactory();
        m = new ObjectMapper(f);
        assertSame(f, m.getJsonFactory());
        assertSame(m, f.getCodec());
    }
    
    /**
     * Test for verifying working of [JACKSON-191]
     */
    public void testProviderConfig() throws Exception   
    {
        ObjectMapper mapper = new ObjectMapper();
        assertEquals(0, mapper._deserializerCache.cachedDeserializersCount());
        // and then should get one constructed for:
        Bean bean = mapper.readValue("{ \"x\" : 3 }", Bean.class);
        assertNotNull(bean);
        assertEquals(1, mapper._deserializerCache.cachedDeserializersCount());
        mapper._deserializerCache.flushCachedDeserializers();
        assertEquals(0, mapper._deserializerCache.cachedDeserializersCount());
    }
}
