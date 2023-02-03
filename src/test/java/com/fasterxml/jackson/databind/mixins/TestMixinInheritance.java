package com.fasterxml.jackson.databind.mixins;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestMixinInheritance
    extends BaseMapTest
{
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

    static class Beano2 {
        public int getIdo() { return 13; }
        public String getNameo() { return "Bill"; }
    }

    static abstract class BeanoMixinSuper2 extends Beano2 {
        @Override
        @JsonProperty("name")
        public abstract String getNameo();
    }

    static abstract class BeanoMixinSub2 extends BeanoMixinSuper2 {
        @Override
        @JsonProperty("id")
        public abstract int getIdo();
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    public void testMixinFieldInheritance() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.addMixIn(Beano.class, BeanoMixinSub.class);
        Map<String,Object> result;
        result = writeAndMap(mapper, new Beano());
        assertEquals(2, result.size());
        if (!result.containsKey("id")
                || !result.containsKey("name")) {
            fail("Should have both 'id' and 'name', but content = "+result);
        }
    }

    public void testMixinMethodInheritance() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.addMixIn(Beano2.class, BeanoMixinSub2.class);
        Map<String,Object> result;
        result = writeAndMap(mapper, new Beano2());
        assertEquals(2, result.size());
        assertTrue(result.containsKey("id"));
        assertTrue(result.containsKey("name"));
    }
}
