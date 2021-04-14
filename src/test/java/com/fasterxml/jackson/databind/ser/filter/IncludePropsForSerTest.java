package com.fasterxml.jackson.databind.ser.filter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IncludePropsForSerTest extends BaseMapTest
{
    @JsonIncludeProperties({"a", "d"})
    static class IncludeSome
    {
        public int a = 3;
        public String b = "x";

        public int getC()
        {
            return -6;
        }

        public String getD()
        {
            return "abc";
        }
    }

    @SuppressWarnings("serial")
    @JsonIncludeProperties({"@class", "a"})
    static class MyMap extends HashMap<String, String> { }

    //allow use of @JsonIncludeProperties for properties
    static class WrapperWithPropInclude
    {
        @JsonIncludeProperties({"y"})
        public XY value = new XY();
    }

    static class XY
    {
        public int x = 1;
        public int y = 2;
    }

    static class WrapperWithPropInclude2
    {
        @JsonIncludeProperties("x")
        public XYZ value = new XYZ();
    }

    static class WrapperWithPropIgnore
    {
        @JsonIgnoreProperties("y")
        public XYZ value = new XYZ();
    }

    @JsonIncludeProperties({"x", "y"})
    static class XYZ
    {
        public int x = 1;
        public int y = 2;
        public int z = 3;
    }

    // also ought to work without full typing?
    static class WrapperWithPropIncludeUntyped
    {
        @JsonIncludeProperties({"x"})
        public Object value = new XYZ();
    }

    static class MapWrapper
    {
        @JsonIncludeProperties({"a"})
        public final HashMap<String, Integer> value = new HashMap<String, Integer>();

        {
            value.put("a", 1);
            value.put("b", 2);
        }
    }

    // for [databind#1060]
    static class IncludeForListValuesXY
    {
        @JsonIncludeProperties({"x"})
        public List<XY> coordinates;

        public IncludeForListValuesXY()
        {
            coordinates = Arrays.asList(new XY());
        }
    }

    static class IncludeForListValuesXYZ
    {
        @JsonIncludeProperties({"x"})
        public List<XYZ> coordinates;

        public IncludeForListValuesXYZ()
        {
            coordinates = Arrays.asList(new XYZ());
        }
    }

    /*
    /****************************************************************
    /* Unit tests
    /****************************************************************
     */

    private final ObjectMapper MAPPER = objectMapper();

    public void testExplicitIncludeWithBean() throws Exception
    {
        IncludeSome value = new IncludeSome();
        Map<String, Object> result = writeAndMap(MAPPER, value);
        assertEquals(2, result.size());
        // verify that specified fields are ignored
        assertFalse(result.containsKey("b"));
        assertFalse(result.containsKey("c"));
        // and that others are not
        assertEquals(Integer.valueOf(value.a), result.get("a"));
        assertEquals(value.getD(), result.get("d"));
    }

    public void testExplicitIncludeWithMap() throws Exception
    {
        // test simulating need to filter out metadata like class name
        MyMap value = new MyMap();
        value.put("a", "b");
        value.put("c", "d");
        value.put("@class", MyMap.class.getName());
        Map<String, Object> result = writeAndMap(MAPPER, value);
        assertEquals(2, result.size());
        assertEquals(MyMap.class.getName(), result.get("@class"));
        assertEquals(value.get("a"), result.get("a"));
    }

    public void testIncludeViaOnlyProps() throws Exception
    {
        assertEquals("{\"value\":{\"y\":2}}",
                MAPPER.writeValueAsString(new WrapperWithPropInclude()));
    }

    // Also: should be fine even if nominal type is `java.lang.Object`
    public void testIncludeViaPropForUntyped() throws Exception
    {
        assertEquals("{\"value\":{\"x\":1}}",
                MAPPER.writeValueAsString(new WrapperWithPropIncludeUntyped()));
    }

    public void testIncludeWithMapProperty() throws Exception
    {
        assertEquals("{\"value\":{\"a\":1}}", MAPPER.writeValueAsString(new MapWrapper()));
    }

    public void testIncludeViaPropsAndClass() throws Exception
    {
        assertEquals("{\"value\":{\"x\":1}}",
                MAPPER.writeValueAsString(new WrapperWithPropInclude2()));
    }

    // for [databind#1060]
    // Ensure that `@JsonIncludeProperties` applies to POJOs within lists, too
    public void testIncludeForListValues() throws Exception
    {
        // should apply to elements
        assertEquals(a2q("{'coordinates':[{'x':1}]}"),
                MAPPER.writeValueAsString(new IncludeForListValuesXY()));

        // and combine values too
        assertEquals(a2q("{'coordinates':[{'x':1}]}"),
                MAPPER.writeValueAsString(new IncludeForListValuesXYZ()));
    }

    public void testIgnoreWithInclude() throws Exception
    {
        assertEquals("{\"value\":{\"x\":1}}", MAPPER.writeValueAsString(new WrapperWithPropIgnore()));
    }
}
