package tools.jackson.databind.ser.filter;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.databind.*;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for checking that alternative settings for
 * <code>JsonInclude</code> annotation property work
 * as expected.
 */
public class JsonIncludeTest
    extends DatabindTestUtil
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

    // [databind#5570]: Test for Boolean wrapper default behavior
    @JsonPropertyOrder({ "b1", "b2" })
    static class DefaultBooleanBean {
        @JsonInclude(JsonInclude.Include.NON_DEFAULT)
        public boolean b1;  // primitive

        @JsonInclude(JsonInclude.Include.NON_DEFAULT)
        public Boolean b2;  // wrapper

        public DefaultBooleanBean(boolean b1, Boolean b2) {
            this.b1 = b1;
            this.b2 = b2;
        }
    }

    // [databind#5570]: Bean for reproducing the exact issue scenario
    static class Issue5570Bean {
        private Boolean value;

        public Boolean getValue() { return value; }
        public void setValue(Boolean value) { this.value = value; }
    }

    // [databind#4741]
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    static class NonDefaultBean4741 {
        public String value = null;
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

    static class NonEmpty<T> {
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public T value;

        public NonEmpty(T v) { value = v; }
    }

    static class NonEmptyDate extends NonEmpty<Date> {
        public NonEmptyDate(Date v) { super(v); }
    }
    static class NonEmptyCalendar extends NonEmpty<Calendar> {
        public NonEmptyCalendar(Calendar v) { super(v); }
    }

    static class NonDefault<T> {
        @JsonInclude(JsonInclude.Include.NON_DEFAULT)
        public T value;

        public NonDefault(T v) { value = v; }
    }

    static class NonDefaultDate extends NonDefault<Date> {
        public NonDefaultDate(Date v) { super(v); }
    }
    static class NonDefaultCalendar extends NonDefault<Calendar> {
        public NonDefaultCalendar(Calendar v) { super(v); }
    }

    // [databind#1327]
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    static class Issue1327BeanEmpty {
        public List<String> myList = new ArrayList<String>();
    }

    // [databind#1327]
    static class Issue1327BeanAlways {
        @JsonInclude(JsonInclude.Include.ALWAYS)
        public List<String> myList = new ArrayList<String>();
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
    /* Test methods
    /**********************************************************
     */

    final private ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testGlobal() throws Exception
    {
        Map<String,Object> result = writeAndMap(MAPPER, new SimpleBean());
        assertEquals(2, result.size());
        assertEquals("a", result.get("a"));
        assertNull(result.get("b"));
        assertTrue(result.containsKey("b"));
    }

    @Test
    public void testNonNullByClass() throws Exception
    {
        Map<String,Object> result = writeAndMap(MAPPER, new NoNullsBean());
        assertEquals(1, result.size());
        assertFalse(result.containsKey("a"));
        assertNull(result.get("a"));
        assertTrue(result.containsKey("b"));
        assertNull(result.get("b"));
    }

    @Test
    public void testNonDefaultByClass() throws Exception
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
    @Test
    public void testNonDefaultByClassNoCtor() throws Exception
    {
        NonDefaultBeanXYZ bean = new NonDefaultBeanXYZ(1, 2, 0);
        String json = MAPPER.writeValueAsString(bean);
        assertEquals(a2q("{'x':1,'y':2}"), json);
    }

    @Test
    public void testMixedMethod() throws Exception
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

    @Test
    public void testDefaultForEmptyList() throws Exception
    {
        assertEquals("{}", MAPPER.writeValueAsString(new ListBean()));
    }

    // NON_DEFAULT should work for arrays too
    @Test
    public void testNonEmptyDefaultArray() throws Exception
    {
        assertEquals("{}", MAPPER.writeValueAsString(new ArrayBean()));
    }

    @Test
    public void testDefaultForIntegers() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(MapperFeature.WRAPPERS_DEFAULT_TO_NULL)
                .build();
        // [databind#5570]: Integer wrapper default is null, not 0, so Integer.valueOf(0) should be included
        assertEquals("{\"i2\":0}", mapper.writeValueAsString(new DefaultIntBean(0, Integer.valueOf(0))));
        assertEquals("{\"i2\":1}", mapper.writeValueAsString(new DefaultIntBean(0, Integer.valueOf(1))));
        assertEquals("{}", mapper.writeValueAsString(new DefaultIntBean(0, null)));
        assertEquals("{\"i1\":3,\"i2\":0}", mapper.writeValueAsString(new DefaultIntBean(3, Integer.valueOf(0))));
        assertEquals("{\"i1\":3}", mapper.writeValueAsString(new DefaultIntBean(3, null)));

        // but with different settings
        mapper = jsonMapperBuilder()
                .disable(MapperFeature.WRAPPERS_DEFAULT_TO_NULL)
                .build();

        assertEquals("{}", mapper.writeValueAsString(new DefaultIntBean(0, Integer.valueOf(0))));
        assertEquals("{\"i2\":1}", mapper.writeValueAsString(new DefaultIntBean(0, Integer.valueOf(1))));
        assertEquals("{}", mapper.writeValueAsString(new DefaultIntBean(0, null)));
        assertEquals("{\"i1\":3}", mapper.writeValueAsString(new DefaultIntBean(3, Integer.valueOf(0))));
        assertEquals("{\"i1\":3}", mapper.writeValueAsString(new DefaultIntBean(3, null)));
}

    // [databind#5570]: Test for Boolean wrapper with NON_DEFAULT
    @Test
    public void testDefaultForBooleans() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(MapperFeature.WRAPPERS_DEFAULT_TO_NULL)
                .build();

        // Both false: primitive suppressed (false is default for boolean),
        // wrapper included (Boolean default is null, not false)
        assertEquals("{\"b2\":false}", mapper.writeValueAsString(
            new DefaultBooleanBean(false, Boolean.FALSE)));

        // Both true: both included (neither matches their defaults)
        assertEquals("{\"b1\":true,\"b2\":true}", mapper.writeValueAsString(
            new DefaultBooleanBean(true, Boolean.TRUE)));

        // Wrapper null: both suppressed (primitive=false matches default, wrapper=null matches default)
        assertEquals("{}", mapper.writeValueAsString(
            new DefaultBooleanBean(false, null)));

        // Primitive true, wrapper false: both included (neither matches defaults)
        assertEquals("{\"b1\":true,\"b2\":false}", mapper.writeValueAsString(
            new DefaultBooleanBean(true, Boolean.FALSE)));
    }

    // [databind#5570]: Reproduce exact scenario from issue report
    @Test
    public void testIssue5570BooleanWrapper() throws Exception
    {
        // Test case from issue #5570 - global NON_DEFAULT configuration
        ObjectMapper mapper = jsonMapperBuilder()
            .enable(MapperFeature.WRAPPERS_DEFAULT_TO_NULL)
            .changeDefaultPropertyInclusion(incl ->
                incl.withValueInclusion(JsonInclude.Include.NON_DEFAULT))
            .build();

        Issue5570Bean pojo = new Issue5570Bean();
        pojo.setValue(Boolean.FALSE);

        // Should include "value":false because Boolean default is null, not false
        assertEquals("{\"value\":false}", mapper.writeValueAsString(pojo));

        // null should be excluded (matches default for Boolean wrapper)
        pojo.setValue(null);
        assertEquals("{}", mapper.writeValueAsString(pojo));

        // true should be included (doesn't match null default)
        pojo.setValue(Boolean.TRUE);
        assertEquals("{\"value\":true}", mapper.writeValueAsString(pojo));
    }

    @Test
    public void testEmptyInclusionScalars() throws Exception
    {
        ObjectMapper defMapper = MAPPER;
        ObjectMapper inclMapper = jsonMapperBuilder()
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_EMPTY))
                .build();

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

    // for [databind#1327]
    @Test
    public void test1327ClassDefaultsForEmpty() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
                .build();

        final String jsonString = mapper.writeValueAsString(new Issue1327BeanEmpty());

        if (jsonString.contains("myList")) {
            fail("Should not contain `myList`: "+jsonString);
        }
    }

    @Test
    public void test1327ClassDefaultsForAlways() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_EMPTY))
                .build();

        final String jsonString = mapper.writeValueAsString(new Issue1327BeanAlways());

        if (!jsonString.contains("myList")) {
            fail("Should contain `myList` with Include.ALWAYS: "+jsonString);
        }
    }

    // [databind#1351], [databind#1417]
    @Test
    public void testIssue1351() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_DEFAULT))
                .build();
        assertEquals(a2q("{}"),
                mapper.writeValueAsString(new Issue1351Bean(null, (double) 0)));
        // [databind#1417]
        assertEquals(a2q("{}"),
                mapper.writeValueAsString(new Issue1351NonBean(0)));
    }

    // [databind#4741]
    @Test
    public void testSerialization4741() throws Exception
    {
        NonDefaultBean4741 bean = new NonDefaultBean4741();
        bean.value = "";
        assertEquals(a2q("{'value':''}"), MAPPER.writeValueAsString(bean));
    }

    // [databind#1550]
    @Test
    public void testInclusionOfDate() throws Exception
    {
        ObjectWriter writerWith = MAPPER.writer().with(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS);

        final Date input = new Date(0L);
        assertEquals(a2q("{'value':0}"),
                writerWith.writeValueAsString(new NonEmptyDate(input)));
        assertEquals("{}",
                writerWith.writeValueAsString(new NonDefaultDate(input)));


    }

    // [databind#1550]
    @Test
    public void testInclusionOfCalendar() throws Exception
    {
        ObjectWriter writerWith = MAPPER.writer().with(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS);

        final Calendar input = new GregorianCalendar();
        input.setTimeInMillis(0L);
        assertEquals(a2q("{'value':0}"),
                writerWith.writeValueAsString(new NonEmptyCalendar(input)));
        assertEquals("{}",
                writerWith.writeValueAsString(new NonDefaultCalendar(input)));
    }
}
