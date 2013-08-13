package com.fasterxml.jackson.databind.jsontype;

import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.jsontype.impl.TypeNameIdResolver;
import com.fasterxml.jackson.databind.type.TypeFactory;

public class TestDefaultForMaps 
    extends BaseMapTest
{
    static class MapKey {
        public String key;

        public MapKey(String k) { key = k; }

        @Override public String toString() { return key; }
    }

    static class MapKeyDeserializer extends KeyDeserializer
    {
        @Override
        public Object deserializeKey(String key, DeserializationContext ctxt) {
            return new MapKey(key);
        }
    }

    static class MapHolder
    {
        @JsonDeserialize(keyAs=MapKey.class, keyUsing=MapKeyDeserializer.class)
        public Map<MapKey,List<Object>> map;
    }

    // // For #234
    
    static class ItemList {
        public String value;
        public List<ItemList> childItems = new LinkedList<ItemList>();

        public void addChildItem(ItemList l) { childItems.add(l); }
    }

    static class ItemMap
    {
        public String value;

        public Map<String, List<ItemMap>> childItems = new HashMap<String, List<ItemMap>>();

        public void addChildItem(String key, ItemMap childItem) {
          List<ItemMap> items;
          if (childItems.containsKey(key)) {
              items = childItems.get(key);
          } else {
              items = new ArrayList<ItemMap>();
          }
          items.add(childItem);
          childItems.put(key, items);
        }
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */
    
    public void testJackson428() throws Exception
    {
        ObjectMapper serMapper = new ObjectMapper();

        TypeResolverBuilder<?> serializerTyper = new ObjectMapper.DefaultTypeResolverBuilder(ObjectMapper.DefaultTyping.NON_FINAL);
        serializerTyper = serializerTyper.init(JsonTypeInfo.Id.NAME, createTypeNameIdResolver(true));
        serializerTyper = serializerTyper.inclusion(JsonTypeInfo.As.PROPERTY);
        serMapper.setDefaultTyping(serializerTyper);

        // Let's start by constructing something to serialize first
        MapHolder holder = new MapHolder();
        holder.map = new HashMap<MapKey,List<Object>>();
        List<Object> ints = new ArrayList<Object>();
        ints.add(Integer.valueOf(3));
        holder.map.put(new MapKey("key"), ints);
        String json = serMapper.writeValueAsString(holder);

        // Then deserialize: need separate mapper to initialize type id resolver appropriately
        ObjectMapper deserMapper = new ObjectMapper();
        TypeResolverBuilder<?> deserializerTyper = new ObjectMapper.DefaultTypeResolverBuilder(ObjectMapper.DefaultTyping.NON_FINAL);
        deserializerTyper = deserializerTyper.init(JsonTypeInfo.Id.NAME, createTypeNameIdResolver(false));
        deserializerTyper = deserializerTyper.inclusion(JsonTypeInfo.As.PROPERTY);
        deserMapper.setDefaultTyping(deserializerTyper);

        MapHolder result = deserMapper.readValue(json, MapHolder.class);
        assertNotNull(result);
        Map<?,?> map = result.map;
        assertEquals(1, map.size());
        Map.Entry<?,?> entry = map.entrySet().iterator().next();
        Object key = entry.getKey();
        assertEquals(MapKey.class, key.getClass());
        Object value = entry.getValue();
        assertTrue(value instanceof List<?>);
        List<?> list = (List<?>) value;
        assertEquals(1, list.size());
        assertEquals(Integer.class, list.get(0).getClass());
        assertEquals(Integer.valueOf(3), list.get(0));
    }

    protected TypeNameIdResolver createTypeNameIdResolver(boolean forSerialization)
    {
        Collection<NamedType> subtypes = new ArrayList<NamedType>();
        subtypes.add(new NamedType(MapHolder.class, "mapHolder"));
        subtypes.add(new NamedType(ArrayList.class, "AList"));
        subtypes.add(new NamedType(HashMap.class, "HMap"));
        ObjectMapper mapper = new ObjectMapper();
        return TypeNameIdResolver.construct(mapper.getDeserializationConfig(),
                TypeFactory.defaultInstance().constructType(Object.class), subtypes, forSerialization, !forSerialization);
    }

    // // For #234:
    
    public void testList() throws Exception
    {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE, JsonTypeInfo.As.PROPERTY);
        ItemList child = new ItemList();
        child.value = "I am child";

        ItemList parent = new ItemList();
        parent.value = "I am parent";
        parent.addChildItem(child);
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(parent);

        Object o = mapper.readValue(json, ItemList.class);
        assertNotNull(o);
    }

    public void testMap() throws Exception
    {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE, JsonTypeInfo.As.PROPERTY);
        ItemMap child = new ItemMap();
        child.value = "I am child";

        ItemMap parent = new ItemMap();
        parent.value = "I am parent";
        parent.addChildItem("child", child);

        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(parent);
        Object o = mapper.readValue(json, ItemMap.class);
        assertNotNull(o);
    }

}
