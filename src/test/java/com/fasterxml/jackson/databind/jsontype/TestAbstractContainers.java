package com.fasterxml.jackson.databind.jsontype;

import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

/**
 * For [Issue#292]
 */
@SuppressWarnings("serial")
public class TestAbstractContainers extends BaseMapTest
{
    // Polymorphic abstract Map type, wrapper
    
    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property="type")
    @JsonSubTypes({ 
        @JsonSubTypes.Type(value = MapWrapper.class, name = "wrapper"),
    })
    static class MapWrapper {
        public  IDataValueMap map = new DataValueMap();     // This does NOT work
    }

    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property="_type_")
    @JsonSubTypes({ 
        @JsonSubTypes.Type(value = DataValueMap.class,  name = "DataValueMap")
    })
    public interface IDataValueMap extends Map<String, String> { }

    static class DataValueMap extends HashMap<String, String> implements IDataValueMap { }

    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property="type")
    @JsonSubTypes({ 
        @JsonSubTypes.Type(value = ListWrapper.class, name = "wrapper"),
    })
    static class ListWrapper {
        public IDataValueList list = new DataValueList();     // This does NOT work
    }

    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property="type")
    @JsonSubTypes({ 
        @JsonSubTypes.Type(value = DataValueList.class,  name = "list")
    })
    public interface IDataValueList extends List<String> { }

    static class DataValueList extends LinkedList<String> implements IDataValueList { }
   
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    public void testAbstractLists() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        ListWrapper w = new ListWrapper();
        w.list.add("x");

        String json = mapper.writeValueAsString(w);
        Object o = mapper.readValue(json, ListWrapper.class);
        assertEquals(ListWrapper.class, o.getClass());
        ListWrapper out = (ListWrapper) o;
        assertEquals(1, out.list.size());
        assertEquals("x", out.list.get(0));
   }
    
    public void testAbstractMaps() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        MapWrapper w = new MapWrapper();
        w.map.put("key1", "name1");

        String json = mapper.writeValueAsString(w);
        Object o = mapper.readValue(json, MapWrapper.class);
        assertEquals(MapWrapper.class, o.getClass());
        MapWrapper out = (MapWrapper) o;
        assertEquals(1, out.map.size());
   }
}
