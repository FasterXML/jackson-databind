package com.fasterxml.jackson.databind.deser.merge;

import com.fasterxml.jackson.annotation.JsonMerge;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MergeWithNullTest extends BaseMapTest
{
    static class Config {
        @JsonMerge
        public AB loc = new AB(1, 2);

        protected Config() { }
        public Config(int a, int b) {
            loc = new AB(a, b);
        }
    }

    // another variant where all we got is a getter
    static class NoSetterConfig {
        AB _value = new AB(2, 3);

        @JsonMerge
        public AB getValue() { return _value; }
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

    public void testSetterlessMergingWithNull() throws Exception
    {
        NoSetterConfig input = new NoSetterConfig();
        NoSetterConfig result = MAPPER.readerForUpdating(input)
                .readValue(aposToQuotes("{'value':null}"));
        assertNotNull(result.getValue());
        assertEquals(2, result.getValue().a);
        assertEquals(3, result.getValue().b);
        assertSame(input, result);
    }
}
