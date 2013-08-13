package com.fasterxml.jackson.failing;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.*;

public class TestIssue234 extends BaseMapTest
{
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
    public void testList() throws Exception {
        String json = getMapper().writerWithDefaultPrettyPrinter().writeValueAsString(generateItemList());
System.out.println("ItemList as JSON:\n" +json);
        Object o = getMapper().readValue(json, ItemList.class);
        assertNotNull(o);
    }
    */

    public void testMap() throws Exception {
        String json = getMapper().writerWithDefaultPrettyPrinter().writeValueAsString(generateItemMap());
System.out.println("ItemMap as JSON:\n"+json);
        Object o = getMapper().readValue(json, ItemMap.class);
        assertNotNull(o);
    }
    /*
    */

    static ItemList generateItemList() {
        ItemList child = new ItemList();
        child.value = "I am child";

        ItemList parent = new ItemList();
        parent.value = "I am parent";
        parent.addChildItem(child);
        return parent;
    }

    static ItemMap generateItemMap() {
        ItemMap child = new ItemMap();
        child.value = "I am child";

        ItemMap parent = new ItemMap();
        parent.value = "I am parent";
        parent.addChildItem("child", child);
        return parent;
    }

    static ObjectMapper getMapper() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enableDefaultTyping(
//                ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
//                JsonTypeInfo.As.WRAPPER_ARRAY);
        return objectMapper;
     }
}
