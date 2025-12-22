package com.fasterxml.jackson.databind.deser;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonDeserializeAs;
import com.fasterxml.jackson.databind.*;

import static org.junit.jupiter.api.Assertions.*;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;

/**
 * Unit tests for new {@link JsonDeserializeAs} annotation.
 *
 * @since 2.21
 */
public class JsonDeserializeAsTest
{
    /*
    /**********************************************************************
    /* Annotated root classes for @JsonDeserializeAs
    /**********************************************************************
     */

    @JsonDeserializeAs(RootInterfaceImpl.class)
    interface RootInterface {
        public String getA();
    }

    static class RootInterfaceImpl implements RootInterface {
        public String a;

        public RootInterfaceImpl() { }

        @Override
        public String getA() { return a; }
    }

    /*
    /**********************************************************************
    /* Annotated helper classes for @JsonDeserializeAs#value
    /**********************************************************************
     */

    // Class for testing valid {@link JsonDeserializeAs} annotation
    // with 'value' parameter to define concrete class to deserialize to
    static class CollectionHolder
    {
        Collection<String> _strings;

        /* Default for 'Collection' would probably be ArrayList or so;
         * let's try to make it a TreeSet instead.
         */
        @JsonDeserializeAs(TreeSet.class)
        public void setStrings(Collection<String> s)
        {
            _strings = s;
        }
    }

    // Another class for testing valid {@link JsonDeserializeAs} annotation
    // with 'value' parameter to define concrete class to deserialize to
    static class MapHolder
    {
        // Let's also coerce numbers into Strings here
        Map<String,String> _data;

        /* Default for 'Map' would be HashMap,
         * let's try to make it a TreeMap instead.
         */
        @JsonDeserializeAs(TreeMap.class)
        public void setStrings(Map<String,String> s)
        {
            _data = s;
        }
    }

    // Another class for testing valid {@link JsonDeserializeAs} annotation
    // with 'value' parameter, but with array
    static class ArrayHolder
    {
        String[] _strings;

        @JsonDeserializeAs(String[].class)
        public void setStrings(Object[] o)
        {
            // should be passed instances of proper type, as per annotation
            _strings = (String[]) o;
        }
    }

    /*
    /**********************************************************************
    /* Annotated helper classes for @JsonDeserializeAs#keys
    /**********************************************************************
     */

    static class StringWrapper
    {
        final String _string;

        public StringWrapper(String s) { _string = s; }
    }

    static class MapKeyHolder
    {
        Map<Object, String> _map;

        @JsonDeserializeAs(keys=StringWrapper.class)
        public void setMap(Map<Object,String> m)
        {
            // type should be ok, but no need to cast here (won't matter)
            _map = m;
        }
    }

    /*
    /**********************************************************************
    /* Annotated helper classes for @JsonDeserializeAs#content
    /**********************************************************************
     */

    static class ListContentHolder
    {
        List<?> _list;

        @JsonDeserializeAs(content=StringWrapper.class)
        public void setList(List<?> l) {
            _list = l;
        }
    }

    static class ArrayContentHolder
    {
        Object[] _data;

        @JsonDeserializeAs(content=Long.class)
        public void setData(Object[] o)
        { // should have proper type, but no need to coerce here
            _data = o;
        }
    }

    static class MapContentHolder
    {
        Map<Object,Object> _map;

        @JsonDeserializeAs(content=Integer.class)
        public void setMap(Map<Object,Object> m)
        {
            _map = m;
        }
    }

    /*
    /**********************************************************
    /* Test methods for @JsonDeserializeAs#value
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testOverrideClassValid() throws Exception
    {
        CollectionHolder result = MAPPER.readValue
            ("{ \"strings\" : [ \"test\" ] }", CollectionHolder.class);

        Collection<String> strs = result._strings;
        assertEquals(1, strs.size());
        assertEquals(TreeSet.class, strs.getClass());
        assertEquals("test", strs.iterator().next());
    }

    @Test
    public void testOverrideMapValid() throws Exception
    {
        // note: expecting conversion from number to String, as well
        MapHolder result = MAPPER.readValue
            ("{ \"strings\" :  { \"a\" : 3 } }", MapHolder.class);

        Map<String,String> strs = result._data;
        assertEquals(1, strs.size());
        assertEquals(TreeMap.class, strs.getClass());
        String value = strs.get("a");
        assertEquals("3", value);
    }

    @Test
    public void testOverrideArrayClass() throws Exception
    {
        ArrayHolder result = MAPPER.readValue
            ("{ \"strings\" : [ \"test\" ] }", ArrayHolder.class);

        String[] strs = result._strings;
        assertEquals(1, strs.length);
        assertEquals(String[].class, strs.getClass());
        assertEquals("test", strs[0]);
    }

    /*
    /**********************************************************
    /* Test methods for @JsonDeserializeAs#value used for root values
    /**********************************************************
     */

    @Test
    public void testRootInterfaceAs() throws Exception
    {
        RootInterface value = MAPPER.readValue("{\"a\":\"abc\" }", RootInterface.class);
        assertTrue(value instanceof RootInterfaceImpl);
        assertEquals("abc", value.getA());
    }

    /*
    /**********************************************************
    /* Test methods for @JsonDeserializeAs#keys
    /**********************************************************
     */

    @SuppressWarnings("unchecked")
    @Test
    public void testOverrideKeyClassValid() throws Exception
    {
        MapKeyHolder result = MAPPER.readValue("{ \"map\" : { \"xxx\" : \"yyy\" } }", MapKeyHolder.class);
        Map<StringWrapper, String> map = (Map<StringWrapper,String>)(Map<?,?>)result._map;
        assertEquals(1, map.size());
        Map.Entry<StringWrapper, String> en = map.entrySet().iterator().next();

        StringWrapper key = en.getKey();
        assertEquals(StringWrapper.class, key.getClass());
        assertEquals("xxx", key._string);
        assertEquals("yyy", en.getValue());
    }

    /*
    /**********************************************************
    /* Test methods for @JsonDeserializeAs#content
    /**********************************************************
     */

    @SuppressWarnings("unchecked")
    @Test
    public void testOverrideContentClassValid() throws Exception
    {
        ListContentHolder result = MAPPER.readValue("{ \"list\" : [ \"abc\" ] }", ListContentHolder.class);
        List<StringWrapper> list = (List<StringWrapper>)result._list;
        assertEquals(1, list.size());
        Object value = list.get(0);
        assertEquals(StringWrapper.class, value.getClass());
        assertEquals("abc", ((StringWrapper) value)._string);
    }

    @Test
    public void testOverrideArrayContents() throws Exception
    {
        ArrayContentHolder result = MAPPER.readValue("{ \"data\" : [ 1, 2, 3 ] }",
                ArrayContentHolder.class);
        Object[] data = result._data;
        assertEquals(3, data.length);
        assertEquals(Long[].class, data.getClass());
        assertEquals(1L, data[0]);
        assertEquals(2L, data[1]);
        assertEquals(3L, data[2]);
    }

    @Test
    public void testOverrideMapContents() throws Exception
    {
        MapContentHolder result = MAPPER.readValue("{ \"map\" : { \"a\" : 9 } }",
                MapContentHolder.class);
        Map<Object,Object> map = result._map;
        assertEquals(1, map.size());
        Object ob = map.values().iterator().next();
        assertEquals(Integer.class, ob.getClass());
        assertEquals(Integer.valueOf(9), ob);
    }
}
