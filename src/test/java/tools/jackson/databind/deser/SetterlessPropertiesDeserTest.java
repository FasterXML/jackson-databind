package tools.jackson.databind.deser;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.exc.UnrecognizedPropertyException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for verifying that "setterless collections" work as
 * expected, similar to how Collections and Maps work
 * with JAXB.
 */
public class SetterlessPropertiesDeserTest
    extends DatabindTestUtil
{
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
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

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
        // System.out.println("this is ko" + out);
        assertNotNull(out);
    }
}
