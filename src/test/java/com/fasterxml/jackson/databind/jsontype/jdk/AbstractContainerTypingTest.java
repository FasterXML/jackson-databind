package com.fasterxml.jackson.databind.jsontype.jdk;

import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

/**
 * For [databind#292]
 */
@SuppressWarnings("serial")
public class AbstractContainerTypingTest extends BaseMapTest
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

    private final ObjectMapper MAPPER = newJsonMapper();

    public void testAbstractLists() throws Exception
    {
        ListWrapper w = new ListWrapper();
        w.list.add("x");

        String json = MAPPER.writeValueAsString(w);
        Object o = MAPPER.readValue(json, ListWrapper.class);
        assertEquals(ListWrapper.class, o.getClass());
        ListWrapper out = (ListWrapper) o;
        assertNotNull(out.list);
        assertEquals(1, out.list.size());
        assertEquals("x", out.list.get(0));
   }

    public void testAbstractMaps() throws Exception
    {
        MapWrapper w = new MapWrapper();
        w.map.put("key1", "name1");

        String json = MAPPER.writeValueAsString(w);
        Object o = MAPPER.readValue(json, MapWrapper.class);
        assertEquals(MapWrapper.class, o.getClass());
        MapWrapper out = (MapWrapper) o;
        assertEquals(1, out.map.size());
   }
}
