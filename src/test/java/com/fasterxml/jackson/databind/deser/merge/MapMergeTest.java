package com.fasterxml.jackson.databind.deser.merge;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonMerge;

import com.fasterxml.jackson.databind.*;

public class MapMergeTest extends BaseMapTest
{
    static class MergedMap
    {
        @JsonMerge
        public Map<String,Object> values;

        protected MergedMap() {
            values = new LinkedHashMap<>();
            values.put("a", "x");
        }

        public MergedMap(String a, String b) {
            values = new LinkedHashMap<>();
            values.put(a, b);
        }

        public MergedMap(Map<String,Object> src) {
            values = src;
        }
    }

    /*
    /********************************************************
    /* Test methods, Map merging
    /********************************************************
     */

    private final ObjectMapper MAPPER = newObjectMapper()
            // 26-Oct-2016, tatu: Make sure we'll report merge problems by default
            .disable(MapperFeature.IGNORE_MERGE_FOR_UNMERGEABLE)
    ;

    public void testShallowMapMerging() throws Exception
    {
        MergedMap v = MAPPER.readValue(aposToQuotes("{'values':{'c':'y'}}"), MergedMap.class);
        assertEquals(2, v.values.size());
        assertEquals("y", v.values.get("c"));
        assertEquals("x", v.values.get("a"));
    }

    @SuppressWarnings("unchecked")
    public void testDeepMapMerging() throws Exception
    {
        // first, create base Map
        MergedMap base = new MergedMap("name", "foobar");
        Map<String,Object> props = new LinkedHashMap<>();
        props.put("default", "yes");
        props.put("x", "abc");
        base.values.put("props", props);

//System.err.println("BASE: "+base.values);
        
        // to be update
        MergedMap v = MAPPER.readerForUpdating(base)
                .readValue(aposToQuotes("{'values':{'props':{'x':'xyz','y' : '...'}}}"));
        assertEquals(2, v.values.size());
//System.err.println("PROPS: "+v.values);
        assertEquals("yes", v.values.get("defaults"));
        assertNotNull(v.values.get("props"));
        props = (Map<String,Object>) v.values.get("props");
        assertEquals(3, props.size());
    }
}
