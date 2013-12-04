package com.fasterxml.jackson.databind.ser;

import java.io.*;
import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class TestCollectionSerialization
    extends BaseMapTest
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

    static class ListSerializer extends JsonSerializer<List<String>>
    {
        @Override
        public void serialize(List<String> value,
                              JsonGenerator jgen,
                              SerializerProvider provider)
            throws IOException
        {
            // just use standard List.toString(), output as JSON String
            jgen.writeString(value.toString());
        }
    }

    // for [JACKSON-254], suppression of empty collections
    static class EmptyListBean {
        public List<String> empty = new ArrayList<String>();
    }

    static class EmptyArrayBean {
        public String[] empty = new String[0];
    }

    // [JACKSON-689]
    static class BeanWithIterable {
        private final ArrayList<String> values = new ArrayList<String>();
        {
            values.add("value");
        }

        public Iterable<String> getValues() { return values; }
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

    // [Issue#358]
    static class A {
        public String unexpected = "Bye.";
    }

    static class B {
        @JsonSerialize(as = Iterable.class, contentUsing = ASerializer.class)
        public List<A> list = Arrays.asList(new A());
    }
    static class ASerializer extends JsonSerializer<A> {
        @Override
        public void serialize(A a, JsonGenerator jsonGenerator, SerializerProvider provider) throws IOException {
            jsonGenerator.writeStartArray();
            jsonGenerator.writeString("Hello world.");
            jsonGenerator.writeEndArray();
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final static ObjectMapper MAPPER = new ObjectMapper();

    public void testCollections()
        throws IOException
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
            JsonParser jp = new JsonFactory().createParser(json);
            assertToken(JsonToken.START_ARRAY, jp.nextToken());
            for (int i = 0; i < entryLen; ++i) {
                assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
                assertEquals(i, jp.getIntValue());
            }
            assertToken(JsonToken.END_ARRAY, jp.nextToken());
            jp.close();
        }
    }

    @SuppressWarnings("resource")
    public void testBigCollection()
        throws IOException
    {
        final int COUNT = 9999;
        ArrayList<Integer> value = new ArrayList<Integer>();
        for (int i = 0; i <= COUNT; ++i) {
            value.add(i);
        }
        // Let's test using 3 main variants...
        for (int mode = 0; mode < 3; ++mode) {
            JsonParser jp = null;
            switch (mode) {
            case 0:
                {
                    byte[] data = MAPPER.writeValueAsBytes(value);
                    jp = new JsonFactory().createParser(data);
                }
                break;
            case 1:
                {
                    StringWriter sw = new StringWriter(value.size());
                    MAPPER.writeValue(sw, value);
                    jp = createParserUsingReader(sw.toString());
                }
                break;
            case 2:
                {
                    String str = MAPPER.writeValueAsString(value);
                    jp = createParserUsingReader(str);
                }
                break;
            }

            // and verify
            assertToken(JsonToken.START_ARRAY, jp.nextToken());
            for (int i = 0; i <= COUNT; ++i) {
                assertEquals(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
                assertEquals(i, jp.getIntValue());
            }
            assertToken(JsonToken.END_ARRAY, jp.nextToken());
            jp.close();
        }
    }

    public void testEnumMap()
        throws IOException
    {
        EnumMap<Key,String> map = new EnumMap<Key,String>(Key.class);
        map.put(Key.B, "xyz");
        map.put(Key.C, "abc");
        // assuming EnumMap uses enum entry order, which I think is true...
        String json = MAPPER.writeValueAsString(map);
        assertEquals("{\"B\":\"xyz\",\"C\":\"abc\"}",json.trim());
    }

    public void testIterator()
        throws IOException
    {
        StringWriter sw = new StringWriter();
        ArrayList<Integer> l = new ArrayList<Integer>();
        l.add(1);
        l.add(-9);
        l.add(0);
        MAPPER.writeValue(sw, l.iterator());
        assertEquals("[1,-9,0]", sw.toString().trim());
    }

    public void testIterable()
        throws IOException
    {
        StringWriter sw = new StringWriter();
        MAPPER.writeValue(sw, new IterableWrapper(new int[] { 1, 2, 3 }));
        assertEquals("[1,2,3]", sw.toString().trim());
    }

    // Test that checks that empty collections are properly serialized
    // when they are Bean properties
    @SuppressWarnings("unchecked")
    public void testEmptyBeanCollection()
        throws IOException
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
    public void testEmptyBeanEnumMap()
        throws IOException
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
    public void testNullBeanEnumMap()
        throws IOException
    {
        EnumMapBean b = new EnumMapBean(null);
        Map<String,Object> result = writeAndMap(MAPPER, b);

        assertEquals(1, result.size());
        assertTrue(result.containsKey("map"));
        assertNull(result.get("map"));
    }

    // Test [JACKSON-220]
    public void testListSerializer() throws IOException
    {
        assertEquals("\"[ab, cd, ef]\"",
                MAPPER.writeValueAsString(new PseudoList("ab", "cd", "ef")));
    }

    // [JACKSON-254]
    public void testEmptyListOrArray() throws IOException
    {
        // by default, empty lists serialized normally
        EmptyListBean list = new EmptyListBean();
        EmptyArrayBean array = new EmptyArrayBean();
        assertTrue(MAPPER.isEnabled(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS));
        assertEquals("{\"empty\":[]}", MAPPER.writeValueAsString(list));
        assertEquals("{\"empty\":[]}", MAPPER.writeValueAsString(array));

        // note: value of setting may be cached when constructing serializer, need a new instance
        ObjectMapper m = new ObjectMapper();
        m.configure(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, false);
        assertEquals("{}", m.writeValueAsString(list));
        assertEquals("{}", m.writeValueAsString(array));
    }
    
    // [JACKSON-689], [JACKSON-876]
    public void testWithIterable() throws IOException
    {
        // 689:
        assertEquals("{\"values\":[\"value\"]}",
                MAPPER.writeValueAsString(new BeanWithIterable()));
        // 876:
        assertEquals("[1,2,3]",
                MAPPER.writeValueAsString(new IntIterable()));
    }
    
    // [Issue#358]
    public void testIterable358() throws Exception {
        String json = MAPPER.writeValueAsString(new B());
        assertEquals("{\"list\":[[\"Hello world.\"]]}", json);
    }
}
