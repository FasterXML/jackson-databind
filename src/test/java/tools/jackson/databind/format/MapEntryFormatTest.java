package tools.jackson.databind.format;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.databind.*;
import tools.jackson.databind.exc.UnrecognizedPropertyException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class MapEntryFormatTest extends DatabindTestUtil
{
    static class BeanWithMapEntry {
        // would work with any other shape than OBJECT, or without annotation:
        @JsonFormat(shape=JsonFormat.Shape.NATURAL)
        public Map.Entry<String,String> entry;

        protected BeanWithMapEntry() { }
        public BeanWithMapEntry(String key, String value) {
            Map<String,String> map = new LinkedHashMap<>();
            map.put(key, value);
            entry = map.entrySet().iterator().next();
        }
    }

    @JsonFormat(shape=JsonFormat.Shape.POJO)
    @JsonPropertyOrder({ "key", "value" })
    static class MapEntryAsObject implements Map.Entry<String,String> {
        protected String key, value;

        protected MapEntryAsObject() { }
        public MapEntryAsObject(String k, String v) {
            key = k;
            value = v;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public String setValue(String v) {
            value = v;
            return v; // wrong, whatever
        }
    }

    static class EntryWithNullWrapper {
        @JsonInclude(value=JsonInclude.Include.NON_EMPTY,
                content=JsonInclude.Include.NON_NULL)
        public Map.Entry<String,String> entry;

        public EntryWithNullWrapper(String key, String value) {
            HashMap<String,String> map = new HashMap<>();
            map.put(key, value);
            entry = map.entrySet().iterator().next();
        }
    }

    static class EntryWithDefaultWrapper {
        @JsonInclude(value=JsonInclude.Include.NON_EMPTY,
                content=JsonInclude.Include.NON_DEFAULT)
        public Map.Entry<String,String> entry;

        public EntryWithDefaultWrapper(String key, String value) {
            HashMap<String,String> map = new HashMap<>();
            map.put(key, value);
            entry = map.entrySet().iterator().next();
        }
    }

    static class EntryWithNonAbsentWrapper {
        @JsonInclude(value=JsonInclude.Include.NON_EMPTY,
                content=JsonInclude.Include.NON_ABSENT)
        public Map.Entry<String,AtomicReference<String>> entry;

        public EntryWithNonAbsentWrapper(String key, String value) {
            HashMap<String,AtomicReference<String>> map = new HashMap<>();
            map.put(key, new AtomicReference<String>(value));
            entry = map.entrySet().iterator().next();
        }
    }

    static class EmptyEntryWrapper {
        @JsonInclude(value=JsonInclude.Include.NON_EMPTY,
                content=JsonInclude.Include.NON_EMPTY)
        public Map.Entry<String,String> entry;

        public EmptyEntryWrapper(String key, String value) {
            HashMap<String,String> map = new HashMap<>();
            map.put(key, value);
            entry = map.entrySet().iterator().next();
        }
    }

    // [databind#1419]
    static class BeanWithMapEntryAsPOJO {
        @JsonFormat(shape = JsonFormat.Shape.POJO)
        public Map.Entry<String, String> entry;

        protected BeanWithMapEntryAsPOJO() { }

        protected BeanWithMapEntryAsPOJO(String key, String value) {
            Map<String, String> map = new HashMap<>();
            map.put(key, value);
            entry = map.entrySet().iterator().next();
        }

        @Override
        public String toString() {
            return "[POJO: entry = "+entry+"]";
        }
    }

    // [databind#1419]
    static class BeanWithComplexMapEntryAsPOJO {
        @JsonFormat(shape = JsonFormat.Shape.POJO)
        public Map.Entry<List<Integer>, String[]> entry;

        protected BeanWithComplexMapEntryAsPOJO() { }

        protected BeanWithComplexMapEntryAsPOJO(int key, String value) {
            Map<List<Integer>, String[]> map = new HashMap<>();
            map.put(Arrays.asList(42), new String[] { value });
            entry = map.entrySet().iterator().next();
        }

        @Override
        public String toString() {
            return "[POJO: entry = "+entry+"]";
        }
    }

    /*
    /**********************************************************
    /* Test methods, basic
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testInclusion() throws Exception
    {
        assertEquals(a2q("{'entry':{'a':'b'}}"),
                MAPPER.writeValueAsString(new EmptyEntryWrapper("a", "b")));
        assertEquals(a2q("{'entry':{'a':'b'}}"),
                MAPPER.writeValueAsString(new EntryWithDefaultWrapper("a", "b")));
        assertEquals(a2q("{'entry':{'a':'b'}}"),
                MAPPER.writeValueAsString(new EntryWithNullWrapper("a", "b")));

        assertEquals(a2q("{}"),
                MAPPER.writeValueAsString(new EmptyEntryWrapper("a", "")));
        assertEquals(a2q("{}"),
                MAPPER.writeValueAsString(new EntryWithDefaultWrapper("a", "")));
        assertEquals(a2q("{'entry':{'a':''}}"),
                MAPPER.writeValueAsString(new EntryWithNullWrapper("a", "")));
        assertEquals(a2q("{}"),
                MAPPER.writeValueAsString(new EntryWithNullWrapper("a", null)));
    }

    @Test
    public void testInclusionWithReference() throws Exception
    {
        assertEquals(a2q("{'entry':{'a':'b'}}"),
                MAPPER.writeValueAsString(new EntryWithNonAbsentWrapper("a", "b")));
        // empty String not excluded since reference is not absent, just points to empty
        // (so would need 3rd level inclusion definition)
        assertEquals(a2q("{'entry':{'a':''}}"),
                MAPPER.writeValueAsString(new EntryWithNonAbsentWrapper("a", "")));
        assertEquals(a2q("{}"),
                MAPPER.writeValueAsString(new EntryWithNonAbsentWrapper("a", null)));
    }

    /*
    /**********************************************************
    /* Test methods, as-Object (Shape)
    /**********************************************************
     */

    @Test
    public void testAsNaturalRoundtrip() throws Exception
    {
        BeanWithMapEntry input = new BeanWithMapEntry("foo" ,"bar");
        String json = MAPPER.writeValueAsString(input);
        assertEquals(a2q("{'entry':{'foo':'bar'}}"), json);
        BeanWithMapEntry result = MAPPER.readValue(json, BeanWithMapEntry.class);
        assertEquals("foo", result.entry.getKey());
        assertEquals("bar", result.entry.getValue());
    }
    // should work via class annotation
    @Test
    public void testAsObjectRoundtrip() throws Exception
    {
        MapEntryAsObject input = new MapEntryAsObject("foo" ,"bar");
        String json = MAPPER.writeValueAsString(input);
        assertEquals(a2q("{'key':'foo','value':'bar'}"), json);

        // 16-Oct-2016, tatu: Happens to work by default because it's NOT basic
        //   `Map.Entry` but subtype.

        MapEntryAsObject result = MAPPER.readValue(json, MapEntryAsObject.class);
        assertEquals("foo", result.getKey());
        assertEquals("bar", result.getValue());
    }

    // [databind#1895]
    @Test
    public void testDefaultShapeOverride() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .withConfigOverride(Map.Entry.class,
                        o -> o.setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.POJO)))
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                .build();
        Map.Entry<String,String> input = new BeanWithMapEntry("foo", "bar").entry;
        assertTrue(mapper.writeValueAsString(input).equals(a2q("{'key':'foo','value':'bar'}"))
                || mapper.writeValueAsString(input).equals(a2q("{'value':'bar','key':'foo'}")));
    }

    /*
    /**********************************************************
    /* Test methods, as-POJO (Shape) [databind#1419]
    /**********************************************************
     */

    // [databind#1419]
    @Test
    public void testWrappedAsObjectRoundtrip1419() throws Exception
    {
        BeanWithMapEntryAsPOJO input = new BeanWithMapEntryAsPOJO("foo", "bar");
        String json = MAPPER.writeValueAsString(input);
        assertEquals(a2q("{'entry':{'key':'foo','value':'bar'}}"), json);
        BeanWithMapEntryAsPOJO result = MAPPER.readValue(json, BeanWithMapEntryAsPOJO.class);
        assertEquals("foo", result.entry.getKey());
        assertEquals("bar", result.entry.getValue());
    }

    // [databind#1419]
    @Test
    public void testWrappedAsComplexRoundtrip1419() throws Exception
    {
        BeanWithComplexMapEntryAsPOJO input = new BeanWithComplexMapEntryAsPOJO(42, "answer");
        String json = MAPPER.writeValueAsString(input);
        assertEquals(a2q("{'entry':{'key':[42],'value':['answer']}}"), json);
        BeanWithComplexMapEntryAsPOJO result = MAPPER.readValue(json,
                BeanWithComplexMapEntryAsPOJO.class);
        assertEquals(Arrays.asList(42), result.entry.getKey());
        assertArrayEquals(new String[] { "answer" }, result.entry.getValue());
    }

    // [databind#1419]
    @Test
    public void testDeserFailWithStructureMismatch1419() throws Exception
    {
        final ObjectMapper strictMapper = jsonMapperBuilder()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        UnrecognizedPropertyException e = assertThrows(UnrecognizedPropertyException.class,
                () -> strictMapper.readValue(a2q("{'entry':{'notKey': 'value'}}"),
                        BeanWithMapEntryAsPOJO.class));
        assertEquals("notKey", e.getPropertyName());
    }
}
