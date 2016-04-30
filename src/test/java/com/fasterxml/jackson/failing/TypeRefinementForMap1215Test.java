package com.fasterxml.jackson.failing;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class TypeRefinementForMap1215Test extends BaseMapTest
{
    interface HasUniqueId<K> {
        K getId();
        String getIdFieldName();
    }
    interface AnotherMap<K, V extends HasUniqueId<K>> extends Map<K, V> {
        public void add(V v);
    }

    static class Item implements HasUniqueId<String>
    {
        public String id;
        public String property;

        @Override
        public String getId() { return id; }
        
        @Override
        public String getIdFieldName() {
            return "id";
        }
    }

    static class Data
    {
        public String id;

        @JsonProperty
        private Map<String, Item> items;

        public Data() { }

        public Map<String, Item> getItems() {
            return items;
        }

        @JsonDeserialize(as = MyHashMap.class)
        public void setItems(Map<String, Item> items) {
            this.items = items;
        }
    }

    @SuppressWarnings("serial")
    static class MyHashMap<K, V extends HasUniqueId<K>>
        extends LinkedHashMap<K, V>
        implements AnotherMap<K, V>
    {
        @JsonCreator
        public static <K, V extends HasUniqueId<K>> MyHashMap<K, V> fromArray(V[] values) {
            MyHashMap<K, V> map = new MyHashMap<K, V>();
            for (int i = 0; i < values.length; i++) {
                V v = values[i];
                if (v.getId() == null) {
                    throw new RuntimeException("Failed to get id");
                }
                if (map.containsKey(v.getId())) {
                    throw new RuntimeException("Conflict on id");
                }
                map.put(v.getId(), v);
            }
            return map;
        }
        
        public MyHashMap() { }
        
        @Override
        public void add(V v) {
            if (containsKey(v.getId())) {
                throw new RuntimeException("Conflict on add of id");
            }
            put(v.getId(), v);
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
        assertEquals(ID2, data.items.get(0).id);
    }
}
