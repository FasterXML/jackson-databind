package com.fasterxml.jackson.databind.jsontype;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class TypeRefinementForMap1215Test extends BaseMapTest
{
    interface HasUniqueId<K> {
        K getId();
    }

    static class Item implements HasUniqueId<String>
    {
        public String id;
        public String property;

        @Override
        public String getId() { return id; }
    }

    static class Data
    {
        public String id;

        @JsonDeserialize(as = MyHashMap.class)
        public Map<String, Item> items;

        // Would work with straight arguments:
//        public MyHashMap<String, Item> items;
    }

    @SuppressWarnings("serial")
    static class MyHashMap<K, V extends HasUniqueId<K>>
        extends LinkedHashMap<K, V>
    {
        @JsonCreator(mode=JsonCreator.Mode.DELEGATING)
        public MyHashMap(V[] values) {
            for (int i = 0; i < values.length; i++) {
                V v = values[i];
                put(v.getId(), v);
            }
        }
    }

    /*
    /*******************************************************
    /* Test methods
    /*******************************************************
     */
    
    public void testMapRefinement() throws Exception
    {
        String ID1 = "3a6383d4-8123-4c43-8b8d-7cedf3e59404";
        String ID2 = "81c3d978-90c4-4b00-8da1-1c39ffcab02c";
        String json = aposToQuotes(
"{'id':'"+ID1+"','items':[{'id':'"+ID2+"','property':'value'}]}");

        ObjectMapper m = new ObjectMapper();
        Data data = m.readValue(json, Data.class);

        assertEquals(ID1, data.id);
        assertNotNull(data.items);
        assertEquals(1, data.items.size());
        Item value = data.items.get(ID2);
        assertNotNull(value);
        assertEquals("value", value.property);
    }
}
