package tools.jackson.databind.jsontype.deftyping;

import java.util.*;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.jsontype.NamedType;
import tools.jackson.databind.jsontype.TypeResolverBuilder;
import tools.jackson.databind.jsontype.impl.DefaultTypeResolverBuilder;
import tools.jackson.databind.jsontype.impl.TypeNameIdResolver;
import tools.jackson.databind.testutil.NoCheckSubTypeValidator;
import tools.jackson.databind.type.TypeFactory;

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
        TypeResolverBuilder<?> serializerTyper = new DefaultTypeResolverBuilder(NoCheckSubTypeValidator.instance,
                DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
//        serializerTyper = serializerTyper.init(JsonTypeInfo.Id.NAME, createTypeNameIdResolver(true));
//        serializerTyper = serializerTyper.inclusion(JsonTypeInfo.As.PROPERTY);
        ObjectMapper serMapper = jsonMapperBuilder()
                .setDefaultTyping(serializerTyper)
                .build();

        // Let's start by constructing something to serialize first
        MapHolder holder = new MapHolder();
        holder.map = new HashMap<MapKey,List<Object>>();
        List<Object> ints = new ArrayList<Object>();
        ints.add(Integer.valueOf(3));
        holder.map.put(new MapKey("key"), ints);
        String json = serMapper.writeValueAsString(holder);

        // Then deserialize: need separate mapper to initialize type id resolver appropriately
        TypeResolverBuilder<?> deserializerTyper = new DefaultTypeResolverBuilder(NoCheckSubTypeValidator.instance,
                DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
//        deserializerTyper = deserializerTyper.init(JsonTypeInfo.Id.NAME, createTypeNameIdResolver(false));
//        deserializerTyper = deserializerTyper.inclusion(JsonTypeInfo.As.PROPERTY);
        ObjectMapper deserMapper = jsonMapperBuilder()
                .setDefaultTyping(deserializerTyper)
                .build();
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
        return TypeNameIdResolver.construct(mapper.deserializationConfig(),
                TypeFactory.defaultInstance().constructType(Object.class), subtypes, forSerialization, !forSerialization);
    }

    public void testList() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(NoCheckSubTypeValidator.instance,
                        DefaultTyping.OBJECT_AND_NON_CONCRETE, JsonTypeInfo.As.PROPERTY)
                .build();
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
        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(NoCheckSubTypeValidator.instance,
                        DefaultTyping.OBJECT_AND_NON_CONCRETE, JsonTypeInfo.As.PROPERTY)
                .build();
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
