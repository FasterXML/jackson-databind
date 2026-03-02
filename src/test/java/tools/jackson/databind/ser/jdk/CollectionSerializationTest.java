package tools.jackson.databind.ser.jdk;

import java.io.*;
import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.StdSerializer;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.NoCheckSubTypeValidator;

import static org.junit.jupiter.api.Assertions.*;


public class CollectionSerializationTest
    extends DatabindTestUtil
{
    enum Key { A, B, C };

    // Field-based simple bean with a single property, "values"
    final static class CollectionBean
    {
        @JsonProperty // not required
        public Collection<Object> values;

        public CollectionBean(Collection<Object> c) { values = c; }
    }

    static class EnumMapBean
    {
        EnumMap<Key,String> _map;

        public EnumMapBean(EnumMap<Key,String> m)
        {
            _map = m;
        }

        public EnumMap<Key,String> getMap() { return _map; }
    }

    /**
     * Class needed for testing [JACKSON-220]
     */
    @SuppressWarnings("serial")
    @JsonSerialize(using=ListSerializer.class)
    static class PseudoList extends ArrayList<String>
    {
        public PseudoList(String... values) {
            super(Arrays.asList(values));
        }
    }

    static class ListSerializer extends StdSerializer<List<String>>
    {
        public ListSerializer() { super(List.class); }

        @Override
        public void serialize(List<String> value, JsonGenerator gen, SerializationContext provider)
        {
            // just use standard List.toString(), output as JSON String
            gen.writeString(value.toString());
        }
    }

    static class EmptyListBean {
        public List<String> empty = new ArrayList<String>();
    }

    static class EmptyArrayBean {
        public String[] empty = new String[0];
    }

    static class StaticListWrapper {
        protected List<String> list;

        public StaticListWrapper(String ... v) {
            list = new ArrayList<String>(Arrays.asList(v));
        }
        protected StaticListWrapper() { }

        public List<String> getList( ) { return list; }
        public void setList(List<String> l) { list = l; }
    }

    // // // Inner types from IterableSerializationTest

    final static class IterableWrapper
        implements Iterable<Integer>
    {
        List<Integer> _ints = new ArrayList<Integer>();

        public IterableWrapper(int[] values) {
            for (int i : values) {
                _ints.add(Integer.valueOf(i));
            }
        }

        @Override
        public Iterator<Integer> iterator() {
            return _ints.iterator();
        }
    }

    @JsonSerialize(typing=JsonSerialize.Typing.STATIC)
    static class BeanWithIterable {
        private final ArrayList<String> values = new ArrayList<String>();
        {
            values.add("value");
        }

        public Iterable<String> getValues() { return values; }
    }

    static class BeanWithIterator {
        private final ArrayList<String> values = new ArrayList<String>();
        {
            values.add("itValue");
        }

        public Iterator<String> getValues() { return values.iterator(); }
    }

    static class IntIterable implements Iterable<Integer>
    {
        @Override
        public Iterator<Integer> iterator() {
            return new IntIterator(1, 3);
        }
    }

    static class IntIterator implements Iterator<Integer> {
        int i;
        final int last;

        public IntIterator(int first, int last) {
            i = first;
            this.last = last;
        }

        @Override
        public boolean hasNext() {
            return i <= last;
        }

        @Override
        public Integer next() {
            return i++;
        }

        @Override
        public void remove() { }

        public int getX() { return 13; }
    }

    // [databind#358]
    static class IterA {
        public String unexpected = "Bye.";
    }

    static class IterB {
        @JsonSerialize(as = Iterable.class,
                contentUsing = IterASerializer.class)
        public List<IterA> list = Arrays.asList(new IterA());
    }

    static class IterASerializer extends ValueSerializer<IterA> {
        @Override
        public void serialize(IterA a, JsonGenerator g, SerializationContext provider)
        {
            g.writeStartArray();
            g.writeString("Hello world.");
            g.writeEndArray();
        }
    }

    // [databind#2390]
    @JsonFilter("default")
    static class IntIterable2390 extends IntIterable { }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final static ObjectMapper MAPPER = newJsonMapper();

    private final ObjectMapper STATIC_MAPPER = jsonMapperBuilder()
        .enable(MapperFeature.USE_STATIC_TYPING)
        .build();

    @Test
    public void testCollections() throws IOException
    {
        // Let's try different collections, arrays etc
        final int entryLen = 98;

        for (int type = 0; type < 4; ++type) {
            Object value;

            if (type == 0) { // first, array
                int[] ints = new int[entryLen];
                for (int i = 0; i < entryLen; ++i) {
                    ints[i] = Integer.valueOf(i);
                }
                value = ints;
            } else {
                Collection<Integer> c;

                switch (type) {
                case 1:
                    c = new LinkedList<Integer>();
                    break;
                case 2:
                    c = new TreeSet<Integer>(); // has to be ordered
                    break;
                default:
                    c = new ArrayList<Integer>();
                    break;
                }
                for (int i = 0; i < entryLen; ++i) {
                    c.add(Integer.valueOf(i));
                }
                value = c;
            }
            String json = MAPPER.writeValueAsString(value);

            // and then need to verify:
            JsonParser p = MAPPER.createParser(json);
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            for (int i = 0; i < entryLen; ++i) {
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertEquals(i, p.getIntValue());
            }
            assertToken(JsonToken.END_ARRAY, p.nextToken());
            p.close();
        }
    }

    @SuppressWarnings("resource")
    @Test
    public void testBigCollection() throws IOException
    {
        final int COUNT = 9999;
        ArrayList<Integer> value = new ArrayList<Integer>();
        for (int i = 0; i <= COUNT; ++i) {
            value.add(i);
        }
        // Let's test using 3 main variants...
        for (int mode = 0; mode < 3; ++mode) {
            JsonParser p = null;
            switch (mode) {
            case 0:
                {
                    byte[] data = MAPPER.writeValueAsBytes(value);
                    p = MAPPER.createParser(data);
                }
                break;
            case 1:
                {
                    StringWriter sw = new StringWriter(value.size());
                    MAPPER.writeValue(sw, value);
                    p = createParserUsingReader(sw.toString());
                }
                break;
            case 2:
                {
                    String str = MAPPER.writeValueAsString(value);
                    p = createParserUsingReader(str);
                }
                break;
            }

            // and verify
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            for (int i = 0; i <= COUNT; ++i) {
                assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertEquals(i, p.getIntValue());
            }
            assertToken(JsonToken.END_ARRAY, p.nextToken());
            p.close();
        }
    }

    @Test
    public void testEnumMap() throws IOException
    {
        EnumMap<Key,String> map = new EnumMap<Key,String>(Key.class);
        map.put(Key.B, "xyz");
        map.put(Key.C, "abc");
        // assuming EnumMap uses enum entry order, which I think is true...
        String json = MAPPER.writeValueAsString(map);
        assertEquals("{\"B\":\"xyz\",\"C\":\"abc\"}",json.trim());
    }

    // Test that checks that empty collections are properly serialized
    // when they are Bean properties
    @SuppressWarnings("unchecked")
    @Test
    public void testEmptyBeanCollection() throws IOException
    {
        Collection<Object> x = new ArrayList<Object>();
        x.add("foobar");
        CollectionBean cb = new CollectionBean(x);
        Map<String,Object> result = writeAndMap(MAPPER, cb);
        assertEquals(1, result.size());
        assertTrue(result.containsKey("values"));
        Collection<Object> x2 = (Collection<Object>) result.get("values");
        assertNotNull(x2);
        assertEquals(x, x2);
    }

    @Test
    public void testNullBeanCollection()
        throws IOException
    {
        CollectionBean cb = new CollectionBean(null);
        Map<String,Object> result = writeAndMap(MAPPER, cb);
        assertEquals(1, result.size());
        assertTrue(result.containsKey("values"));
        assertNull(result.get("values"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testEmptyBeanEnumMap() throws IOException
    {
        EnumMap<Key,String> map = new EnumMap<Key,String>(Key.class);
        EnumMapBean b = new EnumMapBean(map);
        Map<String,Object> result = writeAndMap(MAPPER, b);

        assertEquals(1, result.size());
        assertTrue(result.containsKey("map"));
        // we deserialized to untyped, not back to bean, so:
        Map<Object,Object> map2 = (Map<Object,Object>) result.get("map");
        assertNotNull(map2);
        assertEquals(0, map2.size());
    }

    // Should also be able to serialize null EnumMaps as expected
    @Test
    public void testNullBeanEnumMap() throws IOException
    {
        EnumMapBean b = new EnumMapBean(null);
        Map<String,Object> result = writeAndMap(MAPPER, b);

        assertEquals(1, result.size());
        assertTrue(result.containsKey("map"));
        assertNull(result.get("map"));
    }

    @Test
    public void testListSerializer() throws IOException
    {
        assertEquals(q("[ab, cd, ef]"),
                MAPPER.writeValueAsString(new PseudoList("ab", "cd", "ef")));
        assertEquals(q("[]"),
                MAPPER.writeValueAsString(new PseudoList()));
    }

    @Test
    public void testEmptyListOrArray() throws IOException
    {
        // by default, empty lists serialized normally
        EmptyListBean list = new EmptyListBean();
        EmptyArrayBean array = new EmptyArrayBean();
        assertTrue(MAPPER.isEnabled(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS));
        assertEquals("{\"empty\":[]}", MAPPER.writeValueAsString(list));
        assertEquals("{\"empty\":[]}", MAPPER.writeValueAsString(array));

        // note: value of setting may be cached when constructing serializer, need a new instance
        ObjectMapper m = jsonMapperBuilder()
                .configure(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, false)
                .build();
        assertEquals("{}", m.writeValueAsString(list));
        assertEquals("{}", m.writeValueAsString(array));
    }

    @Test
    public void testStaticList() throws IOException
    {
        // First: au naturel
        StaticListWrapper w = new StaticListWrapper("a", "b", "c");
        String json = MAPPER.writeValueAsString(w);
        assertEquals(a2q("{'list':['a','b','c']}"), json);

        // but then with default typing
        final ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(NoCheckSubTypeValidator.instance,
                        DefaultTyping.NON_FINAL)
                .build();
        json = mapper.writeValueAsString(w);
        assertEquals(a2q(String.format("['%s',{'list':['%s',['a','b','c']]}]",
                w.getClass().getName(), w.list.getClass().getName())), json);
    }

    // // // Tests from IterableSerializationTest

    @Test
    public void testIterator()
    {
        ArrayList<Integer> l = new ArrayList<Integer>();
        l.add(1);
        l.add(null);
        l.add(-9);
        l.add(0);

        assertEquals("[1,null,-9,0]", MAPPER.writeValueAsString(l.iterator()));
        l.clear();
        assertEquals("[]", MAPPER.writeValueAsString(l.iterator()));
    }

    @Test
    public void testIterable()
    {
        assertEquals("[1,2,3]",
                MAPPER.writeValueAsString(new IterableWrapper(new int[] { 1, 2, 3 })));
    }

    @Test
    public void testWithIterable()
    {
        assertEquals("{\"values\":[\"value\"]}",
                STATIC_MAPPER.writeValueAsString(new BeanWithIterable()));
        assertEquals("[1,2,3]",
                STATIC_MAPPER.writeValueAsString(new IntIterable()));

        assertEquals("{\"values\":[\"value\"]}",
                MAPPER.writeValueAsString(new BeanWithIterable()));
        assertEquals("[1,2,3]",
                MAPPER.writeValueAsString(new IntIterable()));

        // 17-Apr-2018, tatu: Turns out we may need "fresh" mapper for some failures?
        ObjectMapper freshMapper = new ObjectMapper();
        assertEquals("{\"values\":[\"value\"]}",
                freshMapper.writeValueAsString(new BeanWithIterable()));
        assertEquals("[1,2,3]",
                freshMapper.writeValueAsString(new IntIterable()));
    }

    @Test
    public void testWithIterator()
    {
        assertEquals("{\"values\":[\"itValue\"]}",
                STATIC_MAPPER.writeValueAsString(new BeanWithIterator()));

        // [databind#1977]
        ArrayList<Number> numbersList = new ArrayList<>();
        numbersList.add(1);
        numbersList.add(0.25);
        String json = MAPPER.writeValueAsString(numbersList.iterator());
        assertEquals("[1,0.25]", json);
    }

    // [databind#358]
    @Test
    public void testIterable358() throws Exception {
        String json = MAPPER.writeValueAsString(new IterB());
        assertEquals("{\"list\":[[\"Hello world.\"]]}", json);
    }

    // [databind#2390]
    @Test
    public void testIterableWithAnnotation() throws Exception
    {
        assertEquals("[1,2,3]",
                STATIC_MAPPER.writeValueAsString(new IntIterable2390()));
    }
}
