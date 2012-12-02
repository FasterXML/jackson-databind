package com.fasterxml.jackson.databind.ser;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;

public class TestSerializerProvider
    extends com.fasterxml.jackson.databind.BaseMapTest
{
    static class MyBean {
        public int getX() { return 3; }
    }

    public void testFindExplicit() throws JsonMappingException
    {
        ObjectMapper mapper = new ObjectMapper();
        SerializationConfig config = mapper.getSerializationConfig();
        SerializerFactory f = new BeanSerializerFactory(null);
        DefaultSerializerProvider prov = new DefaultSerializerProvider.Impl().createInstance(config, f);

        // Should have working default key and null key serializers
        assertNotNull(prov.findKeySerializer(mapper.constructType(String.class), null));
        assertNotNull(prov.getDefaultNullKeySerializer());
        assertNotNull(prov.getDefaultNullValueSerializer());
        // as well as 'unknown type' one (throws exception)
        assertNotNull(prov.getUnknownTypeSerializer(getClass()));
        
        assertTrue(prov.createInstance(config, f).hasSerializerFor(String.class));
        // call twice to verify it'll be cached (second code path)
        assertTrue(prov.createInstance(config, f).hasSerializerFor(String.class));

        assertTrue(prov.createInstance(config, f).hasSerializerFor(MyBean.class));
        assertTrue(prov.createInstance(config, f).hasSerializerFor(MyBean.class));
    }
}
