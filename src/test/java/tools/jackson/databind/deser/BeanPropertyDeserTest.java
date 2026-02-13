package tools.jackson.databind.deser;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.*;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

import static tools.jackson.databind.testutil.DatabindTestUtil.*;

/**
 * Tests for bean property detection and deserialization: field-backed
 * properties, getter-as-setter collections/maps, overloaded setter
 * methods, and static method exclusion.
 */
public class BeanPropertyDeserTest
{
    /*
    /**********************************************************
    /* Helper classes for field-backed property tests
    /**********************************************************
     */

    static class SimpleFieldBean
    {
        public int x, y;

        // not auto-detectable, not public
        int z;

        // ignored, not detectable either
        @JsonIgnore public int a;
    }

    static class SimpleFieldBean2
    {
        @JsonDeserialize String[] values;
    }

    @JsonAutoDetect(fieldVisibility=Visibility.NONE)
    static class NoAutoDetectBean
    {
        // not auto-detectable any more
        public int z;

        @JsonProperty("z")
        public int _z;
    }

    // Let's test invalid bean too
    static class DupFieldBean
    {
        public int z;

        @JsonProperty("z")
        public int _z;
    }

    public static class DupFieldBean2
    {
        @JsonProperty("foo")
        public int _z;

        @JsonDeserialize
        int foo;
    }

    public static class OkDupFieldBean
        extends SimpleFieldBean
    {
        @JsonProperty("x")
        protected int myX = 10;

        @SuppressWarnings("hiding")
        public int y = 11;
    }

    abstract static class Abstract { }

    static class Concrete extends Abstract
    {
        String value;

        public Concrete(String v) { value = v; }
    }

    static class AbstractWrapper {
        @JsonDeserialize(as=Concrete.class)
        public Abstract value;
    }

    /*
    /**********************************************************
    /* Helper classes for setterless property tests
    /**********************************************************
     */

    static class CollectionBean
    {
        List<String> _values = new ArrayList<>();

        public List<String> getValues() { return _values; }
    }

    static class MapBean
    {
        Map<String,Integer> _values = new HashMap<>();

        public Map<String,Integer> getValues() { return _values; }
    }

    // testing to verify that field has precedence over getter, for lists
    static class Dual
    {
        @JsonProperty("list") protected List<Integer> values = new ArrayList<>();

        public Dual() { }

        public List<Integer> getList() {
            throw new IllegalStateException("Should not get called");
        }
    }

    static class DataBean2692
    {
        final String val;

        @JsonCreator
        public DataBean2692(@JsonProperty(value = "val") String val) {
            super();
            this.val = val;
        }

        public String getVal() {
            return val;
        }

        public List<String> getList() {
            return new ArrayList<>();
        }

        @Override
        public String toString() {
            return "DataBean [val=" + val + "]";
        }
    }

    /*
    /**********************************************************
    /* Helper classes for overloaded method tests
    /**********************************************************
     */

    static class BaseListBean
    {
        List<String> list;

        BaseListBean() { }

        public void setList(List<String> l) { list = l; }
    }

    static class ArrayListBean extends BaseListBean
    {
        ArrayListBean() { }

        public void setList(ArrayList<String> l) { super.setList(l); }
    }

    static class NumberBean {
        protected Object value;

        public void setValue(Number n) { value = n; }
    }

    static class WasNumberBean extends NumberBean {
        public void setValue(String str) { value = str; }
    }

    static class Overloaded739
    {
        protected Object _value;

        @JsonProperty
        public void setValue(String str) { _value = str; }

        // no annotation, should not be chosen:
        public void setValue(Object o) { throw new UnsupportedOperationException(); }
    }

    /**
     * And then a Bean that is conflicting and should not work
     */
    static class ConflictBean {
        public void setA(ArrayList<Object> a) { }
        public void setA(LinkedList<Object> a) { }
    }

    /*
    /**********************************************************
    /* Helper classes for static method exclusion tests
    /**********************************************************
     */

    static class StaticSetterBean
    {
        int _x;

        public static void setX(int value) { throw new Error("Should NOT call static method"); }

        @JsonProperty("x") public void assignX(int x) { _x = x; }
    }

    /*
    /**********************************************************
    /* Test methods, field-backed properties
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testSimpleAutoDetect() throws Exception
    {
        SimpleFieldBean result = MAPPER.readValue("{ \"x\" : -13 }",
                SimpleFieldBean.class);
        assertEquals(-13, result.x);
        assertEquals(0, result.y);
    }

    @Test
    public void testSimpleAnnotation() throws Exception
    {
        SimpleFieldBean2 bean = MAPPER.readValue("{ \"values\" : [ \"x\", \"y\" ] }",
                SimpleFieldBean2.class);
        String[] values = bean.values;
        assertNotNull(values);
        assertEquals(2, values.length);
        assertEquals("x", values[0]);
        assertEquals("y", values[1]);
    }

    @Test
    public void testNoAutoDetect() throws Exception
    {
        NoAutoDetectBean bean = MAPPER.readValue("{ \"z\" : 7 }",
                NoAutoDetectBean.class);
        assertEquals(7, bean._z);
    }

    @Test
    public void testTypeAnnotation() throws Exception
    {
        AbstractWrapper w = MAPPER.readValue("{ \"value\" : \"abc\" }",
                AbstractWrapper.class);
        Abstract bean = w.value;
        assertNotNull(bean);
        assertEquals(Concrete.class, bean.getClass());
        assertEquals("abc", ((Concrete)bean).value);
    }

    @Test
    public void testResolvedDups1() throws Exception
    {
        DupFieldBean result = MAPPER.readValue(a2q("{'z':3}"), DupFieldBean.class);
        assertEquals(3, result._z);
        assertEquals(0, result.z);
    }

    @Test
    public void testFailingDups2() throws Exception
    {
        // Fails because both fields have explicit annotation
        try {
            DupFieldBean2 result = MAPPER.readValue(a2q("{'foo':28}"), DupFieldBean2.class);
            fail("Should not pass but got: "+result);
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Multiple fields representing property \"foo\"");
        }
    }

    @Test
    public void testOkFieldOverride() throws Exception
    {
        OkDupFieldBean result = MAPPER.readValue("{ \"x\" : 1, \"y\" : 2 }",
                OkDupFieldBean.class);
        assertEquals(1, result.myX);
        assertEquals(2, result.y);
    }

    /*
    /**********************************************************
    /* Test methods, setterless properties (getter-as-setter)
    /**********************************************************
     */

    @Test
    public void testSimpleSetterlessCollectionOk() throws Exception
    {
        CollectionBean result = jsonMapperBuilder()
                .enable(MapperFeature.USE_GETTERS_AS_SETTERS)
                .build()
                .readValue
            ("{\"values\":[ \"abc\", \"def\" ]}", CollectionBean.class);
        List<String> l = result._values;
        assertEquals(2, l.size());
        assertEquals("abc", l.get(0));
        assertEquals("def", l.get(1));
    }

    /**
     * Let's also verify that disabling the feature makes
     * deserialization fail for setterless bean
     */
    @Test
    public void testSimpleSetterlessCollectionFailure() throws Exception
    {
        ObjectMapper m = jsonMapperBuilder().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
        assertFalse(m.isEnabled(MapperFeature.USE_GETTERS_AS_SETTERS));

        // and now this should fail
        try {
            m.readValue
                ("{\"values\":[ \"abc\", \"def\" ]}", CollectionBean.class);
            fail("Expected an exception");
        } catch (UnrecognizedPropertyException e) {
            // Not a good exception, ideally could suggest a need for
            // a setter...?
            verifyException(e, "Unrecognized property");
        }
    }

    @Test
    public void testSimpleSetterlessMapOk() throws Exception
    {
        MapBean result = jsonMapperBuilder()
                .enable(MapperFeature.USE_GETTERS_AS_SETTERS)
                .build()
                .readValue
            ("{\"values\":{ \"a\": 15, \"b\" : -3 }}", MapBean.class);
        Map<String,Integer> m = result._values;
        assertEquals(2, m.size());
        assertEquals(Integer.valueOf(15), m.get("a"));
        assertEquals(Integer.valueOf(-3), m.get("b"));
    }

    @Test
    public void testSimpleSetterlessMapFailure() throws Exception
    {
        ObjectMapper m = jsonMapperBuilder()
                .disable(MapperFeature.USE_GETTERS_AS_SETTERS)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        // so this should fail now without a setter
        try {
            m.readValue
                ("{\"values\":{ \"a\":3 }}", MapBean.class);
            fail("Expected an exception");
        } catch (UnrecognizedPropertyException e) {
            verifyException(e, "Unrecognized property");
        }
    }

    /* Test for [JACKSON-328], precedence of "getter-as-setter" (for Lists) versus
     * field for same property.
     */
    @Test
    public void testSetterlessPrecedence() throws Exception
    {
        ObjectMapper m = jsonMapperBuilder()
                .enable(MapperFeature.USE_GETTERS_AS_SETTERS)
                .build();
        Dual value = m.readValue("{\"list\":[1,2,3]}", Dual.class);
        assertNotNull(value);
        assertEquals(3, value.values.size());
    }

    // [databind#2692]
    @Test
    void issue2692() throws Exception {
        String json = "{\"list\":[\"11\"],\"val\":\"VAL2\"}";
        DataBean2692 out = MAPPER.readerFor(DataBean2692.class).readValue(json);
        assertNotNull(out);
    }

    /*
    /**********************************************************
    /* Test methods, overloaded methods
    /**********************************************************
     */

    /**
     * It should be ok to overload with specialized
     * version; more specific method should be used.
     */
    @Test
    public void testSpecialization() throws Exception
    {
        ArrayListBean bean = MAPPER.readValue
            ("{\"list\":[\"a\",\"b\",\"c\"]}", ArrayListBean.class);
        assertNotNull(bean.list);
        assertEquals(3, bean.list.size());
        assertEquals(ArrayList.class, bean.list.getClass());
        assertEquals("a", bean.list.get(0));
        assertEquals("b", bean.list.get(1));
        assertEquals("c", bean.list.get(2));
    }

    /**
     * As per [JACKSON-255], should also allow more general overriding,
     * as long as there are no in-class conflicts.
     */
    @Test
    public void testOverride() throws Exception
    {
        WasNumberBean bean = MAPPER.readValue
            ("{\"value\" : \"abc\"}", WasNumberBean.class);
        assertNotNull(bean);
        assertEquals("abc", bean.value);
    }

    // for [JACKSON-739]
    @Test
    public void testConflictResolution() throws Exception
    {
        Overloaded739 bean = MAPPER.readValue
                ("{\"value\":\"abc\"}", Overloaded739.class);
        assertNotNull(bean);
        assertEquals("abc", bean._value);
    }

    /**
     * For genuine setter conflict, an exception is to be thrown.
     */
    @Test
    public void testSetterConflict() throws Exception
    {
    	try {
    	MAPPER.readValue("{ }", ConflictBean.class);
    	} catch (Exception e) {
    	    verifyException(e, "Conflicting setter definitions");
    	}
    }

    /*
    /**********************************************************
    /* Test methods, static method exclusion
    /**********************************************************
     */

    @Test
    public void testStaticSetterIgnored() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        // should not care about static setter...
        StaticSetterBean result = m.readValue("{ \"x\":3}", StaticSetterBean.class);
        assertEquals(3, result._x);
    }
}
