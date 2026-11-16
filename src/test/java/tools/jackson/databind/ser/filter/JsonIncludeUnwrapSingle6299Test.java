package tools.jackson.databind.ser.filter;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#6299]: with both `APPLY_JSON_INCLUDE_FOR_CONTAINERS` and
// `WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED`, a single-element container whose only
// element is filtered out must serialize as `[]` (not as nothing at all)
public class JsonIncludeUnwrapSingle6299Test extends DatabindTestUtil
{
    enum Size { SMALL, LARGE }

    static class SuppressSmallFilter {
        @Override
        public boolean equals(Object other) {
            return other == Size.SMALL;
        }
    }

    // Genuine non-Collection Iterable, to be routed through `IterableSerializer`
    static class StringIterable implements Iterable<String> {
        private final List<String> _values;

        StringIterable(String value) { _values = List.of(value); }

        @Override
        public Iterator<String> iterator() { return _values.iterator(); }
    }

    // One property per affected container serializer
    @JsonPropertyOrder({ "stringList", "stringSet", "objectList", "objectSet", "iterable",
        "strings", "objects", "enums",
        "booleans", "shorts", "ints", "longs", "floats", "doubles", "after" })
    static class Containers {
        @JsonInclude(content = JsonInclude.Include.NON_EMPTY)
        public List<String> stringList; // IndexedStringListSerializer
        @JsonInclude(content = JsonInclude.Include.NON_EMPTY)
        public Set<String> stringSet; // StringCollectionSerializer
        @JsonInclude(content = JsonInclude.Include.NON_EMPTY)
        public List<Object> objectList; // IndexedListSerializer
        @JsonInclude(content = JsonInclude.Include.NON_EMPTY)
        public Set<Object> objectSet; // CollectionSerializer
        @JsonInclude(content = JsonInclude.Include.NON_EMPTY)
        public Iterable<String> iterable; // IterableSerializer
        @JsonInclude(content = JsonInclude.Include.NON_EMPTY)
        public String[] strings; // StringArraySerializer
        @JsonInclude(content = JsonInclude.Include.NON_EMPTY)
        public Object[] objects; // ObjectArraySerializer
        @JsonInclude(content = JsonInclude.Include.CUSTOM,
                contentFilter = SuppressSmallFilter.class)
        public EnumSet<Size> enums; // EnumSetSerializer

        // JDKArraySerializers
        @JsonInclude(content = JsonInclude.Include.NON_DEFAULT)
        public boolean[] booleans;
        @JsonInclude(content = JsonInclude.Include.NON_DEFAULT)
        public short[] shorts;
        @JsonInclude(content = JsonInclude.Include.NON_DEFAULT)
        public int[] ints;
        @JsonInclude(content = JsonInclude.Include.NON_DEFAULT)
        public long[] longs;
        @JsonInclude(content = JsonInclude.Include.NON_DEFAULT)
        public float[] floats;
        @JsonInclude(content = JsonInclude.Include.NON_DEFAULT)
        public double[] doubles;

        public int after = 1;

        static Containers filtered() {
            Containers c = new Containers();
            c.stringList = new ArrayList<>(List.of(""));
            c.stringSet = new LinkedHashSet<>(List.of(""));
            c.objectList = new ArrayList<>(List.of(""));
            c.objectSet = new LinkedHashSet<>(List.of(""));
            c.iterable = new StringIterable("");
            c.strings = new String[] { "" };
            c.objects = new Object[] { "" };
            c.enums = EnumSet.of(Size.SMALL);
            c.booleans = new boolean[] { false };
            c.shorts = new short[] { 0 };
            c.ints = new int[] { 0 };
            c.longs = new long[] { 0L };
            c.floats = new float[] { 0f };
            c.doubles = new double[] { 0d };
            return c;
        }

        static Containers retained() {
            Containers c = new Containers();
            c.stringList = new ArrayList<>(List.of("a"));
            c.stringSet = new LinkedHashSet<>(List.of("b"));
            c.objectList = new ArrayList<>(List.of("c"));
            c.objectSet = new LinkedHashSet<>(List.of("d"));
            c.iterable = new StringIterable("e");
            c.strings = new String[] { "f" };
            c.objects = new Object[] { "g" };
            c.enums = EnumSet.of(Size.LARGE);
            c.booleans = new boolean[] { true };
            c.shorts = new short[] { 1 };
            c.ints = new int[] { 2 };
            c.longs = new long[] { 3L };
            c.floats = new float[] { 0.5f };
            c.doubles = new double[] { 0.25d };
            return c;
        }
    }

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .enable(SerializationFeature.APPLY_JSON_INCLUDE_FOR_CONTAINERS,
                    SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
            .build();

    @Test
    void filteredSingletonWrittenAsEmptyArray() throws Exception
    {
        assertEquals("""
                {"stringList":[],"stringSet":[],"objectList":[],"objectSet":[],"iterable":[],\
                "strings":[],"objects":[],"enums":[],\
                "booleans":[],"shorts":[],"ints":[],"longs":[],"floats":[],"doubles":[],\
                "after":1}""",
                MAPPER.writeValueAsString(Containers.filtered()));
    }

    @Test
    void retainedSingletonStillUnwrapped() throws Exception
    {
        assertEquals("""
                {"stringList":"a","stringSet":"b","objectList":"c","objectSet":"d","iterable":"e",\
                "strings":"f","objects":"g","enums":"LARGE",\
                "booleans":true,"shorts":1,"ints":2,"longs":3,"floats":0.5,"doubles":0.25,\
                "after":1}""",
                MAPPER.writeValueAsString(Containers.retained()));
    }

    @Test
    void rootValue() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(SerializationFeature.APPLY_JSON_INCLUDE_FOR_CONTAINERS,
                        SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
                .changeDefaultPropertyInclusion(incl -> incl
                        .withContentInclusion(JsonInclude.Include.NON_EMPTY))
                .build();
        TypeReference<List<String>> type = new TypeReference<>() { };
        assertEquals("[]", mapper.writerFor(type).writeValueAsString(List.of("")));
        assertEquals("\"a\"", mapper.writerFor(type).writeValueAsString(List.of("a")));
        // Unwrapping is based on physical size: filtering down to one element
        // still produces an array
        assertEquals("[\"a\"]", mapper.writerFor(type).writeValueAsString(List.of("a", "")));
    }
}
