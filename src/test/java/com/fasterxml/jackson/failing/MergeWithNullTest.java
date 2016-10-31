package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.OptBoolean;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MergeWithNullTest extends BaseMapTest
{
    static class Config {
        @JsonSetter(merge=OptBoolean.TRUE)
        public AB loc = new AB(1, 2);

        protected Config() { }
        public Config(int a, int b) {
            loc = new AB(a, b);
        }
    }

    static class AB {
        public int a;
        public int b;

        protected AB() { }
        public AB(int a0, int b0) {
            a = a0;
            b = b0;
        }
    }
    
    private final ObjectMapper MAPPER = new ObjectMapper()
            // 26-Oct-2016, tatu: Make sure we'll report merge problems by default
            .disable(MapperFeature.IGNORE_MERGE_FOR_UNMERGEABLE)
    ;

    // Also: nulls better not override
    public void testBeanMergingWithNull() throws Exception
    {
        Config config = MAPPER.readerForUpdating(new Config(5, 7))
                .readValue(aposToQuotes("{'loc':null}"));
        assertNotNull(config.loc);
        assertEquals(5, config.loc.a);
        assertEquals(7, config.loc.b);
    }
    

}
