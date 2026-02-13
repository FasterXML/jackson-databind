package tools.jackson.databind.deser.jdk;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import tools.jackson.core.JsonParser;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.exc.InvalidNullException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// For [databind#5139] and [databind#5165]:
// Tests for null content handling across Collection, Map, Object array,
// String array, and String collection deserializers.
public class NullContentHandling5165Test
    extends DatabindTestUtil
{
    // Custom deserializer that converts empty strings to null
    static class EmptyStringToNullDeserializer extends StdDeserializer<String> {
        public EmptyStringToNullDeserializer() {
            super(String.class);
        }

        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) {
            String value = p.getValueAsString();
            if (value != null && value.isEmpty()) {
                return null;
            }
            return value;
        }
    }

    // [databind#5139]
    static class CollectionDst {
        private List<Integer> list;

        public List<Integer> getList() {
            return list;
        }

        public void setList(List<Integer> list) {
            this.list = list;
        }
    }

    // [databind#5165]
    static class MapDst {
        private Map<String, Integer> map;

        public Map<String, Integer> getMap() {
            return map;
        }

        public void setMap(Map<String, Integer> map) {
            this.map = map;
        }
    }

    // [databind#5165]
    static class ObjectArrayDst {
        public Integer[] array;
    }

    // [databind#5165]
    static class StringArrayDst {
        public String[] array;
    }

    // [databind#5165]
    static class StringCollectionDst {
        public List<String> list;
    }

    private ObjectMapper createMapperWithCustomDeserializer() {
        SimpleModule module = new SimpleModule()
            .addDeserializer(String.class, new EmptyStringToNullDeserializer());

        return JsonMapper.builder()
                .addModule(module)
                .changeDefaultNullHandling(n -> JsonSetter.Value.forContentNulls(Nulls.FAIL))
                .build();
    }

    /*
    /**********************************************************************
    /* Test methods, Collection [databind#5139]
    /**********************************************************************
     */

    @Test
    public void testNullsFail_Collection() {
        ObjectMapper mapper = JsonMapper.builder()
                .changeDefaultNullHandling(n -> JsonSetter.Value.forContentNulls(Nulls.FAIL))
                .build();

        assertThrows(
                InvalidNullException.class,
                () -> mapper.readValue("{\"list\":[\"\"]}", new TypeReference<CollectionDst>(){})
        );
    }

    @Test
    public void testNullsSkip_Collection() throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .changeDefaultNullHandling(n -> JsonSetter.Value.forContentNulls(Nulls.SKIP))
                .build();

        CollectionDst dst = mapper.readValue("{\"list\":[\"\"]}", new TypeReference<CollectionDst>() {});

        assertTrue(dst.getList().isEmpty());
    }

    /*
    /**********************************************************************
    /* Test methods, Map [databind#5165]
    /**********************************************************************
     */

    @Test
    public void testNullsFail_Map() {
        ObjectMapper mapper = JsonMapper.builder()
                .changeDefaultNullHandling(n -> JsonSetter.Value.forContentNulls(Nulls.FAIL))
                .build();
        assertThrows(
                InvalidNullException.class,
                () -> mapper.readValue("{\"map\":{\"key\":\"\"}}", new TypeReference<MapDst>(){})
        );
    }

    @Test
    public void testNullsSkip_Map() throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .changeDefaultNullHandling(n -> JsonSetter.Value.forContentNulls(Nulls.SKIP))
                .build();
        MapDst dst = mapper.readValue("{\"map\":{\"key\":\"\"}}", new TypeReference<MapDst>() {});
        assertTrue(dst.getMap().isEmpty());
    }

    /*
    /**********************************************************************
    /* Test methods, Object array [databind#5165]
    /**********************************************************************
     */

    @Test
    public void testNullsFail_ObjectArray() {
        ObjectMapper mapper = JsonMapper.builder()
                .changeDefaultNullHandling(n -> JsonSetter.Value.forContentNulls(Nulls.FAIL))
                .build();

        // NOTE! Relies on default coercion of "" into `null` for `Integer`s...
        assertThrows(
                InvalidNullException.class,
                () -> mapper.readValue("{\"array\":[\"\"]}", ObjectArrayDst.class)
        );
    }

    @Test
    public void testNullsSkip_ObjectArray() throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .changeDefaultNullHandling(n -> JsonSetter.Value.forContentNulls(Nulls.SKIP))
                .build();

        ObjectArrayDst dst = mapper.readValue("{\"array\":[\"\"]}", ObjectArrayDst.class);
        // NOTE! Relies on default coercion of "" into `null` for `Integer`s...
        assertEquals(0, dst.array.length, "Null values should be skipped");
    }

    /*
    /**********************************************************************
    /* Test methods, String array [databind#5165]
    /**********************************************************************
     */

    @Test
    public void testNullsVanilla_StringArray() {
        ObjectMapper mapper = sharedMapper();
        String[] arr = mapper.readValue("[ ]", String[].class);
        assertEquals(0, arr.length);

        arr = mapper.readValue("[null ]", String[].class);
        assertEquals(1, arr.length);
        assertNull(arr[0]);

        arr = mapper.readValue("[ \"abc\", null ]", String[].class);
        assertEquals(2, arr.length);
        assertEquals("abc", arr[0]);
        assertNull(arr[1]);
    }

    @Test
    public void testNullsFail_StringArray() {
        ObjectMapper mapper = createMapperWithCustomDeserializer();

        assertThrows(
                InvalidNullException.class,
                () -> mapper.readValue("{\"array\":[\"\"]}", StringArrayDst.class)
        );
    }

    @Test
    public void testNullsSkip_StringArray() throws Exception {
        SimpleModule module = new SimpleModule()
                .addDeserializer(String.class, new EmptyStringToNullDeserializer());

        ObjectMapper mapper = JsonMapper.builder()
                .addModule(module)
                .changeDefaultNullHandling(n -> JsonSetter.Value.forContentNulls(Nulls.SKIP))
                .build();

        StringArrayDst dst = mapper.readValue("{\"array\":[\"\"]}", StringArrayDst.class);
        assertEquals(0, dst.array.length, "Null values should be skipped");
    }

    /*
    /**********************************************************************
    /* Test methods, String collection [databind#5165]
    /**********************************************************************
     */

    @Test
    public void testNullsFail_StringCollection() {
        ObjectMapper mapper = createMapperWithCustomDeserializer();

        assertThrows(
                InvalidNullException.class,
                () -> mapper.readValue("{\"list\":[\"\"]}", StringCollectionDst.class)
        );
    }

    @Test
    public void testNullsSkip_StringCollection() throws Exception {
        SimpleModule module = new SimpleModule()
                .addDeserializer(String.class, new EmptyStringToNullDeserializer());

        ObjectMapper mapper = JsonMapper.builder()
                .addModule(module)
                .changeDefaultNullHandling(n -> JsonSetter.Value.forContentNulls(Nulls.SKIP))
                .build();

        StringCollectionDst dst = mapper.readValue("{\"list\":[\"\"]}", StringCollectionDst.class);

        assertTrue(dst.list.isEmpty(), "Null values should be skipped");
    }
}
