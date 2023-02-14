package com.fasterxml.jackson.databind.struct;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

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
    @JsonPropertyOrder({ "general", "misc" })
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

    // For [Issue#226]
    static class Parent {
        @JsonUnwrapped(prefix="c1.")
        public Child c1;
        @JsonUnwrapped(prefix="c2.")
        public Child c2;
      }

    static class Child {
        @JsonUnwrapped(prefix="sc2.")
        public SubChild sc1;
      }

    static class SubChild {
        public String value;
    }

    /*
    /**********************************************************
    /* Tests, serialization
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testPrefixedUnwrappingSerialize() throws Exception
    {
        JsonMapper mapper = JsonMapper.builder().enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build();
        assertEquals("{\"_x\":1,\"_y\":2,\"name\":\"Tatu\"}",
                mapper.writeValueAsString(new PrefixUnwrap("Tatu", 1, 2)));
    }

    public void testDeepPrefixedUnwrappingSerialize() throws Exception
    {
        JsonMapper mapper = JsonMapper.builder().enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build();
        String json = mapper.writeValueAsString(new DeepPrefixUnwrap("Bubba", 1, 1));
        assertEquals("{\"u._x\":1,\"u._y\":1,\"u.name\":\"Bubba\"}", json);
    }

    public void testHierarchicConfigSerialize() throws Exception
    {
        String json = MAPPER.writeValueAsString(new ConfigRoot("Fred", 25));
        assertEquals("{\"general.names.name\":\"Fred\",\"misc.value\":25}", json);
    }

    /*
    /**********************************************************
    /* Tests, deserialization
    /**********************************************************
     */

    public void testPrefixedUnwrapping() throws Exception
    {
        PrefixUnwrap bean = MAPPER.readValue("{\"name\":\"Axel\",\"_x\":4,\"_y\":7}", PrefixUnwrap.class);
        assertNotNull(bean);
        assertEquals("Axel", bean.name);
        assertNotNull(bean.location);
        assertEquals(4, bean.location.x);
        assertEquals(7, bean.location.y);
    }

    public void testDeepPrefixedUnwrappingDeserialize() throws Exception
    {
        DeepPrefixUnwrap bean = MAPPER.readValue("{\"u.name\":\"Bubba\",\"u._x\":2,\"u._y\":3}",
                DeepPrefixUnwrap.class);
        assertNotNull(bean.unwrapped);
        assertNotNull(bean.unwrapped.location);
        assertEquals(2, bean.unwrapped.location.x);
        assertEquals(3, bean.unwrapped.location.y);
        assertEquals("Bubba", bean.unwrapped.name);
    }

    public void testHierarchicConfigDeserialize() throws Exception
    {
        ConfigRoot root = MAPPER.readValue("{\"general.names.name\":\"Bob\",\"misc.value\":3}",
                ConfigRoot.class);
        assertNotNull(root.general);
        assertNotNull(root.general.names);
        assertNotNull(root.misc);
        assertEquals(3, root.misc.value);
        assertEquals("Bob", root.general.names.name);
    }

    /*
    /**********************************************************
    /* Tests, round-trip
    /**********************************************************
     */

    public void testHierarchicConfigRoundTrip() throws Exception
    {
        ConfigAlternate input = new ConfigAlternate(123, "Joe", 42);
        String json = MAPPER.writeValueAsString(input);

        ConfigAlternate root = MAPPER.readValue(json, ConfigAlternate.class);
        assertEquals(123, root.id);
        assertNotNull(root.general);
        assertNotNull(root.general.names);
        assertNotNull(root.misc);
        assertEquals("Joe", root.general.names.name);
        assertEquals(42, root.misc.value);
    }

    public void testIssue226() throws Exception
    {
        Parent input = new Parent();
        input.c1 = new Child();
        input.c1.sc1 = new SubChild();
        input.c1.sc1.value = "a";
        input.c2 = new Child();
        input.c2.sc1 = new SubChild();
        input.c2.sc1.value = "b";

        String json = MAPPER.writeValueAsString(input);

        Parent output = MAPPER.readValue(json, Parent.class);
        assertNotNull(output.c1);
        assertNotNull(output.c2);

        assertNotNull(output.c1.sc1);
        assertNotNull(output.c2.sc1);

        assertEquals("a", output.c1.sc1.value);
        assertEquals("b", output.c2.sc1.value);
    }
}
