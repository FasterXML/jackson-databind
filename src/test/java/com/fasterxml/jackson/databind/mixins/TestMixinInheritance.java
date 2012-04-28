package com.fasterxml.jackson.databind.mixins;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestMixinInheritance
    extends BaseMapTest
{
    // [Issue-14]
    static class Beano {
        public int ido = 42;
        public String nameo = "Bob";
    }

    static class BeanoMixinSuper {
        @JsonProperty("name")
        public String nameo;
    }

    static class BeanoMixinSub extends BeanoMixinSuper {
        @JsonProperty("id")
        public int ido;
    }

    public void testMixinInheritance() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.addMixInAnnotations(Beano.class, BeanoMixinSub.class);
        Map<String,Object> result;
        result = writeAndMap(mapper, new Beano());
        assertEquals(2, result.size());
        assertTrue(result.containsKey("id"));
        assertTrue(result.containsKey("name"));
    }
}
