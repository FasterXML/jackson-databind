package tools.jackson.databind.deser;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.exc.IgnoredPropertyException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

import static org.junit.jupiter.api.Assertions.*;

import static tools.jackson.databind.testutil.DatabindTestUtil.*;

/**
 * Tests for property inclusion/exclusion during deserialization,
 * using {@link JsonIgnore}, {@link JsonIgnoreProperties} and
 * {@link com.fasterxml.jackson.annotation.JsonIncludeProperties}.
 */
public class PropertyFilteringDeserTest
{
    /*
    /**********************************************************
    /* Helper classes for @JsonIgnore tests
    /**********************************************************
     */

    // Class for testing {@link JsonIgnore} annotations with setters
    final static class SizeClassIgnore
    {
        int _x = 0;
        int _y = 0;

        public void setX(int value) { _x = value; }
        @JsonIgnore public void setY(int value) { _y = value; }

        // Just igoring won't help a lot here; let's define a replacement
        // so that we won't get an exception for "unknown field"
        @JsonProperty("y") void foobar(int value) {
            ; // nop
        }
    }

    @JsonIgnoreProperties({ "z" })
    final static class NoYOrZ
    {
        public int x;

        @JsonIgnore
        public int y = 1;
    }

    /*
    /**********************************************************
    /* Helper classes for @JsonIncludeProperties tests
    /**********************************************************
     */

    @JsonIncludeProperties({"y", "z"})
    static class OnlyYAndZ
    {
        int _x = 0;
        int _y = 0;
        int _z = 0;

        public void setX(int value)
        {
            _x = value;
        }

        public void setY(int value)
        {
            _y = value;
        }

        public void setZ(int value)
        {
            _z = value;
        }

        @JsonProperty("y")
        void replacementForY(int value)
        {
            _y = value * 2;
        }
    }

    @JsonIncludeProperties({"y", "z"})
    static class OnlyY
    {
        public int x;

        public int y = 1;
    }

    static class OnlyYWrapperForOnlyYAndZ
    {
        @JsonIncludeProperties("y")
        public OnlyYAndZ onlyY;
    }

    // for [databind#1060]
    static class IncludeForListValuesY
    {
        @JsonIncludeProperties({"y"})
        //@JsonIgnoreProperties({"z"})
        public List<OnlyYAndZ> onlyYs;

        public IncludeForListValuesY()
        {
            onlyYs = Arrays.asList(new OnlyYAndZ());
        }
    }

    @SuppressWarnings("serial")
    @JsonIncludeProperties({"@class", "a"})
    static class MyMap extends HashMap<String, String>
    {
    }

    static class MapWrapper
    {
        @JsonIncludeProperties({"a"})
        public HashMap<String, Integer> value;
    }

    @JsonIncludeProperties({"foo", "bar"})
    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class)
    static class AnySetterObjectId
    {
        protected Map<String, AnySetterObjectId> values = new HashMap<String, AnySetterObjectId>();

        @JsonAnySetter
        public void anySet(String field, AnySetterObjectId value)
        {
            // Ensure that it is never called with null because of unresolved reference.
            assertNotNull(value);
            values.put(field, value);
        }
    }

    /*
    /**********************************************************
    /* Test methods, @JsonIgnore
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testSimpleIgnore() throws Exception
    {
        SizeClassIgnore result = MAPPER.readValue("{ \"x\":1, \"y\" : 2 }",
             SizeClassIgnore.class);
        // x should be set, y not
        assertEquals(1, result._x);
        assertEquals(0, result._y);
    }

    @Test
    public void testFailOnIgnore() throws Exception
    {
        ObjectReader r = MAPPER.readerFor(NoYOrZ.class);

        // First, fine to get "x":
        NoYOrZ result = r.readValue(a2q("{'x':3}"));
        assertEquals(3, result.x);
        assertEquals(1, result.y);

        // but not 'y'
        r = r.with(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);
        try {
            result = r.readValue(a2q("{'x':3, 'y':4}"));
            fail("Should fail");
        } catch (IgnoredPropertyException e) {
            verifyException(e, "Ignored field");
        }

        // or 'z'
        try {
            result = r.readValue(a2q("{'z':2 }"));
            fail("Should fail");
        } catch (IgnoredPropertyException e) {
            verifyException(e, "Ignored field");
        }
    }

    /*
    /**********************************************************
    /* Test methods, @JsonIncludeProperties
    /**********************************************************
     */

    @Test
    public void testSimpleInclude() throws Exception
    {
        OnlyYAndZ result = MAPPER.readValue(
                a2q("{ 'x':1, '_x': 1, 'y':2, 'z':3 }"),
                OnlyYAndZ.class);
        assertEquals(0, result._x);
        assertEquals(4, result._y);
        assertEquals(3, result._z);
    }

    @Test
    public void testIncludeIgnoredAndUnrecognizedField() throws Exception
    {
        ObjectReader r = MAPPER.readerFor(OnlyY.class).with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        // First, fine to get "y" only:
        OnlyY result = r.readValue(a2q("{'x':3, 'y': 4}"));
        assertEquals(0, result.x);
        assertEquals(4, result.y);

        // but fail on ignored properties.
        r = r.with(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);
        try {
            r.readValue(a2q("{'x':3, 'y': 4, 'z': 5}"));
            fail("Should fail");
        } catch (IgnoredPropertyException e) {
            verifyException(e, "Ignored field");
        }

        // or fail on unrecognized properties
        try {
            r.readValue(a2q("{'y': 3, 'z':2 }"));
            fail("Should fail");
        } catch (UnrecognizedPropertyException e) {
            verifyException(e, "Unrecognized property \"z\"");
        }

        // or success with the both settings disabled.
        r = r.without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        r = r.without(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);
        r.readValue(a2q("{'y': 3, 'z':2 }"));
        assertEquals(4, result.y);
    }

    @Test
    public void testMergeInclude() throws Exception
    {
        OnlyYWrapperForOnlyYAndZ onlyY = MAPPER.readValue(
                a2q("{'onlyY': {'x': 2, 'y':3, 'z': 4}}"),
                OnlyYWrapperForOnlyYAndZ.class
        );
        assertEquals(0, onlyY.onlyY._x);
        assertEquals(6, onlyY.onlyY._y);
        assertEquals(0, onlyY.onlyY._z);
    }

    @Test
    public void testListInclude() throws Exception
    {
        IncludeForListValuesY result = MAPPER.readValue(
                a2q("{'onlyYs':[{ 'x':1, 'y' : 2, 'z': 3 }]}"),
                IncludeForListValuesY.class);
        assertEquals(0, result.onlyYs.get(0)._x);
        assertEquals(4, result.onlyYs.get(0)._y);
        assertEquals(0, result.onlyYs.get(0)._z);
    }

    @Test
    public void testMapWrapper() throws Exception
    {
        MapWrapper result = MAPPER.readValue(a2q("{'value': {'a': 2, 'b': 3}}"), MapWrapper.class);
        assertNotNull(result.value);
        assertEquals(2, result.value.get("a").intValue());
        assertFalse(result.value.containsKey("b"));
    }

    @Test
    public void testMyMap() throws Exception
    {
        MyMap result = MAPPER.readValue(a2q("{'a': 2, 'b': 3}"), MyMap.class);
        assertEquals("2", result.get("a"));
        assertFalse(result.containsKey("b"));
    }

    @Test
    public void testForwardReferenceAnySetterComboWithInclude() throws Exception
    {
        String json = a2q("{'@id':1, 'foo':2, 'foo2':2, 'bar':{'@id':2, 'foo':1}}");
        AnySetterObjectId value = MAPPER.readValue(json, AnySetterObjectId.class);
        assertSame(value.values.get("bar"), value.values.get("foo"));
        assertNull(value.values.get("foo2"));
    }
}
