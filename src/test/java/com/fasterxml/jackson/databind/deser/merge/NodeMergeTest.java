package com.fasterxml.jackson.databind.deser.merge;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class NodeMergeTest extends BaseMapTest
{
    private final static ObjectMapper MAPPER = new ObjectMapper()
            // 26-Oct-2016, tatu: Make sure we'll report merge problems by default
            .disable(MapperFeature.IGNORE_MERGE_FOR_UNMERGEABLE)
    ;

    static class ObjectNodeWrapper {
        @JsonSetter(merge=OptBoolean.TRUE)
        public ObjectNode props = MAPPER.createObjectNode();
        {
            props.put("default", "enabled");
        }
    }

    static class ArrayNodeWrapper {
        @JsonSetter(merge=OptBoolean.TRUE)
        public ArrayNode list = MAPPER.createArrayNode();
        {
            list.add(123);
        }
    }

    /*
    /********************************************************
    /* Test methods
    /********************************************************
     */

    public void testObjectNodeUpdateValue() throws Exception
    {
        ObjectNode base = MAPPER.createObjectNode();
        base.put("first", "foo");
        assertSame(base,
                MAPPER.readerForUpdating(base)
                .readValue(aposToQuotes("{'second':'bar'}")));
        assertEquals(2, base.size());
        assertEquals("bar", base.path("second").asText());
        assertEquals("foo", base.path("first").asText());
    }

    public void testObjectNodeMerge() throws Exception
    {
        ObjectNodeWrapper w = MAPPER.readValue(aposToQuotes("{'props':{'stuff':'xyz'}}"),
                ObjectNodeWrapper.class);
        assertEquals(2, w.props.size());
        assertEquals("enabled", w.props.path("default").asText());
        assertEquals("xyz", w.props.path("stuff").asText());
    }

    public void testArrayNodeUpdateValue() throws Exception
    {
        ArrayNode base = MAPPER.createArrayNode();
        base.add("first");
        assertSame(base,
                MAPPER.readerForUpdating(base)
                .readValue(aposToQuotes("['second','third']")));
        assertEquals(3, base.size());
        assertEquals("first", base.path(0).asText());
        assertEquals("second", base.path(1).asText());
        assertEquals("third", base.path(2).asText());
    }

    public void testArrayNodeMerge() throws Exception
    {
        ArrayNodeWrapper w = MAPPER.readValue(aposToQuotes("{'list':[456]}"),
                ArrayNodeWrapper.class);
        assertEquals(2, w.list.size());
        assertEquals(123, w.list.path(0).asInt());
        assertEquals(456, w.list.path(1).asInt());
    }
}
