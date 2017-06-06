package com.fasterxml.jackson.databind.filter;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Unit tests for checking that alternative settings for
 * {@link JsonSerialize#include} annotation property work
 * as expected.
 */
public class JsonIncludeTest
    extends BaseMapTest
{
    static class SimpleBean
    {
        public String getA() { return "a"; }
        public String getB() { return null; }
    }
    
    @JsonInclude(JsonInclude.Include.ALWAYS) // just to ensure default
    static class NoNullsBean
    {
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String getA() { return null; }

        public String getB() { return null; }
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    static class NonDefaultBean
    {
        String _a = "a", _b = "b";

        NonDefaultBean() { }

        public String getA() { return _a; }
        public String getB() { return _b; }
    }

    // [databind#998]: Do not require no-arg constructor; but if not, defaults check
    //    has weaker interpretation
    @JsonPropertyOrder({ "x", "y", "z" })
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    static class NonDefaultBeanXYZ
    {
        public int x;
        public int y = 3;
        public int z = 7;

        NonDefaultBeanXYZ(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
    
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    static class MixedBean
    {
        String _a = "a", _b = "b";

        MixedBean() { }

        public String getA() { return _a; }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String getB() { return _b; }
    }

    // to ensure that default values work for collections as well
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    static class ListBean {
        public List<String> strings = new ArrayList<String>();
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    static class ArrayBean {
        public int[] ints = new int[] { 1, 2 };
    }

    // Test to ensure that default exclusion works for fields too
    @JsonPropertyOrder({ "i1", "i2" })
    static class DefaultIntBean {
        @JsonInclude(JsonInclude.Include.NON_DEFAULT)
        public int i1;

        @JsonInclude(JsonInclude.Include.NON_DEFAULT)
        public Integer i2;

        public DefaultIntBean(int i1, Integer i2) {
            this.i1 = i1;
            this.i2 = i2;
        }
    }

    static class NonEmptyString {
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public String value;

        public NonEmptyString(String v) { value = v; }
    }

    static class NonEmptyInt {
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public int value;

        public NonEmptyInt(int v) { value = v; }
    }

    static class NonEmptyDouble {
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public double value;

        public NonEmptyDouble(double v) { value = v; }
    }

    @JsonPropertyOrder({"list", "map"})
    static class EmptyListMapBean
    {
        public List<String> list = Collections.emptyList();

        public Map<String,String> map = Collections.emptyMap();
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonPropertyOrder({"num", "annotated", "plain"})
    static class MixedTypeBean
    {
        @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
        public Integer num = null;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String annotated = null;

        public String plain = null;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"num", "annotated", "plain"})
    static class MixedTypeNonNullBean
    {
        @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
        public Integer num = null;

        @JsonInclude(JsonInclude.Include.ALWAYS)
        public String annotated = null;

        public String plain = null;
    }

    // [databind#1351]

    static class Issue1351Bean
    {
        public final String first;
        public final double second;

        public Issue1351Bean(String first, double second) {
            this.first = first;
            this.second = second;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    static abstract class Issue1351NonBeanParent
    {
        protected final int num;

        protected Issue1351NonBeanParent(int num) {
            this.num = num;
        }

        @JsonProperty("num")
        public int getNum() {
            return num;
        }
    }

    static class Issue1351NonBean extends Issue1351NonBeanParent {
        private String str;

        @JsonCreator
        public Issue1351NonBean(@JsonProperty("num") int num) {
            super(num);
        }

        public String getStr() {
            return str;
        }

        public void setStr(String str) {
            this.str = str;
        }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    final private ObjectMapper MAPPER = new ObjectMapper();

    public void testGlobal() throws IOException
    {
        Map<String,Object> result = writeAndMap(MAPPER, new SimpleBean());
        assertEquals(2, result.size());
        assertEquals("a", result.get("a"));
        assertNull(result.get("b"));
        assertTrue(result.containsKey("b"));
    }

    public void testNonNullByClass() throws IOException
    {
        Map<String,Object> result = writeAndMap(MAPPER, new NoNullsBean());
        assertEquals(1, result.size());
        assertFalse(result.containsKey("a"));
        assertNull(result.get("a"));
        assertTrue(result.containsKey("b"));
        assertNull(result.get("b"));
    }

    public void testNonDefaultByClass() throws IOException
    {
        NonDefaultBean bean = new NonDefaultBean();
        // need to change one of defaults
        bean._a = "notA";
        Map<String,Object> result = writeAndMap(MAPPER, bean);
        assertEquals(1, result.size());
        assertTrue(result.containsKey("a"));
        assertEquals("notA", result.get("a"));
        assertFalse(result.containsKey("b"));
        assertNull(result.get("b"));
    }

    // [databind#998]
    public void testNonDefaultByClassNoCtor() throws IOException
    {
        NonDefaultBeanXYZ bean = new NonDefaultBeanXYZ(1, 2, 0);
        String json = MAPPER.writeValueAsString(bean);
        assertEquals(aposToQuotes("{'x':1,'y':2}"), json);
    }

    public void testMixedMethod() throws IOException
    {
        MixedBean bean = new MixedBean();
        bean._a = "xyz";
        bean._b = null;
        Map<String,Object> result = writeAndMap(MAPPER, bean);
        assertEquals(1, result.size());
        assertEquals("xyz", result.get("a"));
        assertFalse(result.containsKey("b"));

        bean._a = "a";
        bean._b = "b";
        result = writeAndMap(MAPPER, bean);
        assertEquals(1, result.size());
        assertEquals("b", result.get("b"));
        assertFalse(result.containsKey("a"));
    }

    public void testDefaultForEmptyList() throws IOException
    {
        assertEquals("{}", MAPPER.writeValueAsString(new ListBean()));
    }

    // NON_DEFAULT shoud work for arrays too
    public void testNonEmptyDefaultArray() throws IOException
    {
        assertEquals("{}", MAPPER.writeValueAsString(new ArrayBean()));
    }

    public void testDefaultForIntegers() throws IOException
    {
        assertEquals("{}", MAPPER.writeValueAsString(new DefaultIntBean(0, Integer.valueOf(0))));
        assertEquals("{\"i2\":1}", MAPPER.writeValueAsString(new DefaultIntBean(0, Integer.valueOf(1))));
        assertEquals("{\"i1\":3}", MAPPER.writeValueAsString(new DefaultIntBean(3, Integer.valueOf(0))));
    }

    public void testEmptyInclusionScalars() throws IOException
    {
        ObjectMapper defMapper = MAPPER;
        ObjectMapper inclMapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        // First, Strings
        StringWrapper str = new StringWrapper("");
        assertEquals("{\"str\":\"\"}", defMapper.writeValueAsString(str));
        assertEquals("{}", inclMapper.writeValueAsString(str));
        assertEquals("{}", inclMapper.writeValueAsString(new StringWrapper()));

        assertEquals("{\"value\":\"x\"}", defMapper.writeValueAsString(new NonEmptyString("x")));
        assertEquals("{}", defMapper.writeValueAsString(new NonEmptyString("")));

        // Then numbers
        // 11-Nov-2015, tatu: As of Jackson 2.7, scalars should NOT be considered empty,
        //   except for wrappers if they are `null`
        assertEquals("{\"value\":12}", defMapper.writeValueAsString(new NonEmptyInt(12)));
        assertEquals("{\"value\":0}", defMapper.writeValueAsString(new NonEmptyInt(0)));

        assertEquals("{\"value\":1.25}", defMapper.writeValueAsString(new NonEmptyDouble(1.25)));
        assertEquals("{\"value\":0.0}", defMapper.writeValueAsString(new NonEmptyDouble(0.0)));

        
        IntWrapper zero = new IntWrapper(0);
        assertEquals("{\"i\":0}", defMapper.writeValueAsString(zero));
        assertEquals("{\"i\":0}", inclMapper.writeValueAsString(zero));
    }

    public void testPropConfigOverridesForInclude() throws IOException
    {
        // First, with defaults, both included:
        EmptyListMapBean empty = new EmptyListMapBean();
        assertEquals(aposToQuotes("{'list':[],'map':{}}"),
                MAPPER.writeValueAsString(empty));
        ObjectMapper mapper;

        // and then change inclusion criteria for either
        mapper = new ObjectMapper();
        mapper.configOverride(Map.class)
            .setInclude(JsonInclude.Value.construct(JsonInclude.Include.NON_EMPTY, null));
        assertEquals(aposToQuotes("{'list':[]}"),
                mapper.writeValueAsString(empty));

        mapper = new ObjectMapper();
        mapper.configOverride(List.class)
            .setInclude(JsonInclude.Value.construct(JsonInclude.Include.NON_EMPTY, null));
        assertEquals(aposToQuotes("{'map':{}}"),
                mapper.writeValueAsString(empty));
    }

    public void testPropConfigOverrideForIncludeAsPropertyNonNull() throws Exception
    {
        // First, with defaults, all but NON_NULL annotated included
        MixedTypeBean nullValues = new MixedTypeBean();
        assertEquals(aposToQuotes("{'num':null,'plain':null}"),
                MAPPER.writeValueAsString(nullValues));
        ObjectMapper mapper;

        // and then change inclusion as property criteria for either
        mapper = new ObjectMapper();
        mapper.configOverride(String.class)
                .setIncludeAsProperty(JsonInclude.Value
                        .construct(JsonInclude.Include.NON_NULL, null));
        assertEquals("{\"num\":null}",
                mapper.writeValueAsString(nullValues));

        mapper = new ObjectMapper();
        mapper.configOverride(Integer.class)
                .setIncludeAsProperty(JsonInclude.Value
                        .construct(JsonInclude.Include.NON_NULL, null));
        assertEquals("{\"plain\":null}",
                mapper.writeValueAsString(nullValues));
    }

    public void testPropConfigOverrideForIncludeAsPropertyAlways() throws Exception
    {
        // First, with defaults, only ALWAYS annotated included
        MixedTypeNonNullBean nullValues = new MixedTypeNonNullBean();
        assertEquals("{\"annotated\":null}",
                MAPPER.writeValueAsString(nullValues));
        ObjectMapper mapper;

        // and then change inclusion as property criteria for either
        mapper = new ObjectMapper();
        mapper.configOverride(String.class)
                .setIncludeAsProperty(JsonInclude.Value
                        .construct(JsonInclude.Include.ALWAYS, null));
        assertEquals(aposToQuotes("{'annotated':null,'plain':null}"),
                mapper.writeValueAsString(nullValues));

        mapper = new ObjectMapper();
        mapper.configOverride(Integer.class)
                .setIncludeAsProperty(JsonInclude.Value
                        .construct(JsonInclude.Include.ALWAYS, null));
        assertEquals(aposToQuotes("{'num':null,'annotated':null}"),
                mapper.writeValueAsString(nullValues));
    }

    // [databind#1351], [databind#1417]
    public void testIssue1351() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);
        assertEquals(aposToQuotes("{}"),
                mapper.writeValueAsString(new Issue1351Bean(null, (double) 0)));
        // [databind#1417]
        assertEquals(aposToQuotes("{}"),
                mapper.writeValueAsString(new Issue1351NonBean(0)));
    }
}
