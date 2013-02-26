package com.fasterxml.jackson.databind.struct;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestUnwrappedWithPrefix extends BaseMapTest
{
    static class Unwrapping {
        public String name;
        @JsonUnwrapped
        public Location location;

        public Unwrapping() { }
        public Unwrapping(String str, int x, int y) {
            name = str;
            location = new Location(x, y);
        }
    }

    static class DeepUnwrapping
    {
        @JsonUnwrapped
        public Unwrapping unwrapped;

        public DeepUnwrapping() { }
        public DeepUnwrapping(String str, int x, int y) {
            unwrapped = new Unwrapping(str, x, y);
        }
    }

    static class Location {
        public int x;
        public int y;

        public Location() { }
        public Location(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    // Class with unwrapping using prefixes
    static class PrefixUnwrap
    {
        public String name;
        @JsonUnwrapped(prefix="_")
        public Location location;

        public PrefixUnwrap() { }
        public PrefixUnwrap(String str, int x, int y) {
            name = str;
            location = new Location(x, y);
        }
    }
    
    static class DeepPrefixUnwrap
    {
        @JsonUnwrapped(prefix="u.")
        public PrefixUnwrap unwrapped;

        public DeepPrefixUnwrap() { }
        public DeepPrefixUnwrap(String str, int x, int y) {
            unwrapped = new PrefixUnwrap(str, x, y);
        }
    }

    // Let's actually test hierarchic names with unwrapping bit more:
    
    static class ConfigRoot
    {
        @JsonUnwrapped(prefix="general.")
        public ConfigGeneral general = new ConfigGeneral();
        
        @JsonUnwrapped(prefix="misc.")
        public ConfigMisc misc = new ConfigMisc();

        public ConfigRoot() { }
        public ConfigRoot(String name, int value)
        {
            general = new ConfigGeneral(name);
            misc.value = value;
        }
    }

    static class ConfigAlternate
    {
        @JsonUnwrapped
        public ConfigGeneral general = new ConfigGeneral();
        
        @JsonUnwrapped(prefix="misc.")
        public ConfigMisc misc = new ConfigMisc();

        public int id;
        
        public ConfigAlternate() { }
        public ConfigAlternate(int id, String name, int value)
        {
            this.id = id;
            general = new ConfigGeneral(name);
            misc.value = value;
        }
    }

    static class ConfigGeneral
    {
        @JsonUnwrapped(prefix="names.")
        public ConfigNames names = new ConfigNames();
        
        public ConfigGeneral() { }
        public ConfigGeneral(String name) {
            names.name = name;
        }
    }

    static class ConfigNames {
        public String name = "x";
    }

    static class ConfigMisc {
        public int value;
    }

    // // // Reuse mapper to keep tests bit faster

    private final ObjectMapper mapper = new ObjectMapper();

    /*
    /**********************************************************
    /* Tests, serialization
    /**********************************************************
     */

    public void testPrefixedUnwrappingSerialize() throws Exception
    {
        assertEquals("{\"name\":\"Tatu\",\"_x\":1,\"_y\":2}",
                mapper.writeValueAsString(new PrefixUnwrap("Tatu", 1, 2)));
    }

    public void testDeepPrefixedUnwrappingSerialize() throws Exception
    {
        String json = mapper.writeValueAsString(new DeepPrefixUnwrap("Bubba", 1, 1));
        assertEquals("{\"u.name\":\"Bubba\",\"u._x\":1,\"u._y\":1}", json);
    }

    public void testHierarchicConfigSerialize() throws Exception
    {
        String json = mapper.writeValueAsString(new ConfigRoot("Fred", 25));
        assertEquals("{\"general.names.name\":\"Fred\",\"misc.value\":25}", json);
    }

    /*
    /**********************************************************
    /* Tests, deserialization
    /**********************************************************
     */

    public void testPrefixedUnwrapping() throws Exception
    {
        PrefixUnwrap bean = mapper.readValue("{\"name\":\"Axel\",\"_x\":4,\"_y\":7}", PrefixUnwrap.class);
        assertNotNull(bean);
        assertEquals("Axel", bean.name);
        assertNotNull(bean.location);
        assertEquals(4, bean.location.x);
        assertEquals(7, bean.location.y);
    }
    
    public void testDeepPrefixedUnwrappingDeserialize() throws Exception
    {
        DeepPrefixUnwrap bean = mapper.readValue("{\"u.name\":\"Bubba\",\"u._x\":2,\"u._y\":3}",
                DeepPrefixUnwrap.class);
        assertNotNull(bean.unwrapped);
        assertNotNull(bean.unwrapped.location);
        assertEquals(2, bean.unwrapped.location.x);
        assertEquals(3, bean.unwrapped.location.y);
        assertEquals("Bubba", bean.unwrapped.name);
    }
    
    public void testHierarchicConfigDeserialize() throws Exception
    {
        ConfigRoot root = mapper.readValue("{\"general.names.name\":\"Bob\",\"misc.value\":3}",
                ConfigRoot.class);
        assertNotNull(root.general);
        assertNotNull(root.general.names);
        assertNotNull(root.misc);
        assertEquals(3, root.misc.value);
        assertEquals("Bob", root.general.names.name);
    }

    /*
    /**********************************************************
    /* Tests, deserialization
    /**********************************************************
     */
    
    public void testHierarchicConfigRoundTrip() throws Exception
    {
        ConfigAlternate input = new ConfigAlternate(123, "Joe", 42);
        String json = mapper.writeValueAsString(input);

        ConfigAlternate root = mapper.readValue(json, ConfigAlternate.class);
        assertEquals(123, root.id);
        assertNotNull(root.general);
        assertNotNull(root.general.names);
        assertNotNull(root.misc);
        assertEquals("Joe", root.general.names.name);
        assertEquals(42, root.misc.value);
    }
}
