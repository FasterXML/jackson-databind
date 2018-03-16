package com.fasterxml.jackson.databind.ser;

import com.fasterxml.jackson.core.json.JsonFactory;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.GeneratorSettings;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;

public class TestSerializerProvider
    extends com.fasterxml.jackson.databind.BaseMapTest
{
    static class MyBean {
        public int getX() { return 3; }
    }

    static class NoPropsBean {
    }
    
    public void testFindExplicit() throws JsonMappingException
    {
        ObjectMapper mapper = new ObjectMapper();
        SerializationConfig config = mapper.serializationConfig();
        SerializerFactory f = new BeanSerializerFactory(null);
        GeneratorSettings genSettings =  GeneratorSettings.empty();
        DefaultSerializerProvider prov = new DefaultSerializerProvider.Impl(new JsonFactory())
                .createInstance(config, genSettings, f);

        // Should have working default key and null key serializers
        assertNotNull(prov.findKeySerializer(mapper.constructType(String.class), null));
//        assertNotNull(prov.getDefaultNullKeySerializer());
        assertNotNull(prov.getDefaultNullValueSerializer());
        // as well as 'unknown type' one (throws exception)
        assertNotNull(prov.getUnknownTypeSerializer(getClass()));
    }
}
