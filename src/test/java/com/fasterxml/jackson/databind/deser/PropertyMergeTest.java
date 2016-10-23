package com.fasterxml.jackson.databind.deser;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.*;

/**
 * Tests to make sure that the new "merging" property of
 * <code>JsonSetter</code> annotation works as expected.
 * 
 * @since 2.9
 */
public class PropertyMergeTest extends BaseMapTest
{
    static class Config {
        @JsonSetter(merge=OptBoolean.TRUE)
        public AB loc = new AB(1, 2);
    }

    static class NonMergeConfig {
        public AB loc = new AB(1, 2);
    }

    static class AB {
        public int a, b;

        protected AB() { }
        public AB(int a0, int b0) {
            a = a0;
            b = b0;
        }
    }

    static class CollectionWrapper {
        @JsonSetter(merge=OptBoolean.TRUE)
        public Collection<String> bag = new TreeSet<String>();
        {
            bag.add("a");
        }
    }

    /*
    /********************************************************
    /* Test methods
    /********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testBeanMergingViaProp() throws Exception
    {
        Config config = MAPPER.readValue(aposToQuotes("{'loc':{'b':3}}"), Config.class);
        assertEquals(1, config.loc.a);
        assertEquals(3, config.loc.b);
    }

    public void testBeanMergingViaType() throws Exception
    {
        // by default, no merging
        NonMergeConfig config = MAPPER.readValue(aposToQuotes("{'loc':{'a':3}}"), NonMergeConfig.class);
        assertEquals(3, config.loc.a);
        assertEquals(0, config.loc.b); // not passed, nor merge from original

        // but with type-overrides
        ObjectMapper mapper = new ObjectMapper();
        mapper.configOverride(AB.class).setSetterInfo(
                JsonSetter.Value.forMerging());
        config = mapper.readValue(aposToQuotes("{'loc':{'a':3}}"), NonMergeConfig.class);
        assertEquals(3, config.loc.a);
        assertEquals(2, config.loc.b); // original, merged
    }

    public void testBeanMergingViaGlobal() throws Exception
    {
        // but with type-overrides
        ObjectMapper mapper = new ObjectMapper()
                .setDefaultSetterInfo(JsonSetter.Value.forMerging());
            NonMergeConfig config = mapper.readValue(aposToQuotes("{'loc':{'a':3}}"), NonMergeConfig.class);
        assertEquals(3, config.loc.a);
        assertEquals(2, config.loc.b); // original, merged
    }
    
    public void testCollectionMerging() throws Exception
    {
        CollectionWrapper w = MAPPER.readValue(aposToQuotes("{'bag':['b']}"), CollectionWrapper.class);
        assertEquals(2, w.bag.size());
        assertTrue(w.bag.contains("a"));
        assertTrue(w.bag.contains("b"));
    }
}
