package com.fasterxml.jackson.databind.deser.jdk;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.module.SimpleModule;

@SuppressWarnings("serial")
public class CollectionDeserTest
    extends BaseMapTest
{
    enum Key {
        KEY1, KEY2, WHATEVER;
    }

    @JsonDeserialize(using=ListDeserializer.class)
    static class CustomList extends LinkedList<String> { }

    static class ListDeserializer extends StdDeserializer<CustomList>
    {
        public ListDeserializer() { super(CustomList.class); }

        @Override
        public CustomList deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException
        {
            CustomList result = new CustomList();
            result.add(jp.getText());
            return result;
        }
    }

    static class XBean {
        public int x;

        public XBean() { }
        public XBean(int x) { this.x = x; }
    }

    // [databind#199]
    static class ListAsIterable {
        public Iterable<String> values;
    }

    // [databind#2251]
    static class ListAsAbstract {
        public AbstractList<String> values;
    }

    static class SetAsAbstract {
        public AbstractSet<String> values;
    }

    static class ListAsIterableX {
        public Iterable<XBean> nums;
    }

    static class KeyListBean {
        public List<Key> keys;
    }

    // [databind#828]
    @JsonDeserialize(using=SomeObjectDeserializer.class)
    static class SomeObject {}

    static class SomeObjectDeserializer extends StdDeserializer<SomeObject> {
        public SomeObjectDeserializer() { super(SomeObject.class); }

        @Override
        public SomeObject deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException {
            throw new RuntimeException("I want to catch this exception");
        }
    }

    // [databind#3068]: Exception wrapping (or not)
    static class MyContainerModel {
        @JsonProperty("processor-id")
        public String id = "123";
    }

    static class MyJobModel {
        public Map<String, MyContainerModel> containers = Collections.singletonMap("key",
                new MyContainerModel());
        public int maxChangeLogStreamPartitions = 13;
    }

    static class CustomException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public CustomException(String s) {
            super(s);
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final static ObjectMapper MAPPER = newJsonMapper();

    public void testUntypedList() throws Exception
    {
        // to get "untyped" default List, pass Object.class
        String JSON = "[ \"text!\", true, null, 23 ]";

        // Not a guaranteed cast theoretically, but will work:
        // (since we know that Jackson will construct an ArrayList here...)
        Object value = MAPPER.readValue(JSON, Object.class);
        assertNotNull(value);
        assertTrue(value instanceof ArrayList<?>);
        List<?> result = (List<?>) value;

        assertEquals(4, result.size());

        assertEquals("text!", result.get(0));
        assertEquals(Boolean.TRUE, result.get(1));
        assertNull(result.get(2));
        assertEquals(Integer.valueOf(23), result.get(3));
    }

    public void testExactStringCollection() throws Exception
    {
        // to get typing, must use type reference
        String JSON = "[ \"a\", \"b\" ]";
        List<String> result = MAPPER.readValue(JSON, new TypeReference<ArrayList<String>>() { });

        assertNotNull(result);
        assertEquals(ArrayList.class, result.getClass());
        assertEquals(2, result.size());

        assertEquals("a", result.get(0));
        assertEquals("b", result.get(1));
    }

    public void testHashSet() throws Exception
    {
        String JSON = "[ \"KEY1\", \"KEY2\" ]";

        EnumSet<Key> result = MAPPER.readValue(JSON, new TypeReference<EnumSet<Key>>() { });
        assertNotNull(result);
        assertTrue(EnumSet.class.isAssignableFrom(result.getClass()));
        assertEquals(2, result.size());

        assertTrue(result.contains(Key.KEY1));
        assertTrue(result.contains(Key.KEY2));
        assertFalse(result.contains(Key.WHATEVER));
    }

    /// Test to verify that @JsonDeserialize.using works as expected
    public void testCustomDeserializer() throws IOException
    {
        CustomList result = MAPPER.readValue(q("abc"), CustomList.class);
        assertEquals(1, result.size());
        assertEquals("abc", result.get(0));
    }

    // Testing "implicit JSON array" for single-element arrays,
    // mostly produced by Jettison, Badgerfish conversions (from XML)
    @SuppressWarnings("unchecked")
    public void testImplicitArrays() throws Exception
    {
        // can't share mapper, custom configs (could create ObjectWriter tho)
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

        // first with simple scalar types (numbers), with collections
        List<Integer> ints = mapper.readValue("4", List.class);
        assertEquals(1, ints.size());
        assertEquals(Integer.valueOf(4), ints.get(0));
        List<String> strings = mapper.readValue(q("abc"), new TypeReference<ArrayList<String>>() { });
        assertEquals(1, strings.size());
        assertEquals("abc", strings.get(0));
        // and arrays:
        int[] intArray = mapper.readValue("-7", int[].class);
        assertEquals(1, intArray.length);
        assertEquals(-7, intArray[0]);
        String[] stringArray = mapper.readValue(q("xyz"), String[].class);
        assertEquals(1, stringArray.length);
        assertEquals("xyz", stringArray[0]);

        // and then with Beans:
        List<XBean> xbeanList = mapper.readValue("{\"x\":4}", new TypeReference<List<XBean>>() { });
        assertEquals(1, xbeanList.size());
        assertEquals(XBean.class, xbeanList.get(0).getClass());

        Object ob = mapper.readValue("{\"x\":29}", XBean[].class);
        XBean[] xbeanArray = (XBean[]) ob;
        assertEquals(1, xbeanArray.length);
        assertEquals(XBean.class, xbeanArray[0].getClass());
    }

    // [JACKSON-620]: allow "" to mean 'null' for Maps
    public void testFromEmptyString() throws Exception
    {
        ObjectReader r = MAPPER.reader(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        List<?> result = r.forType(List.class).readValue(q(""));
        assertNull(result);
    }

    // [databind#161]
    public void testArrayBlockingQueue() throws Exception
    {
        // ok to skip polymorphic type to get Object
        ArrayBlockingQueue<?> q = MAPPER.readValue("[1, 2, 3]", ArrayBlockingQueue.class);
        assertNotNull(q);
        assertEquals(3, q.size());
        assertEquals(Integer.valueOf(1), q.take());
        assertEquals(Integer.valueOf(2), q.take());
        assertEquals(Integer.valueOf(3), q.take());
    }

    // [databind#199]
    public void testIterableWithStrings() throws Exception
    {
        String JSON = "{ \"values\":[\"a\",\"b\"]}";
        ListAsIterable w = MAPPER.readValue(JSON, ListAsIterable.class);
        assertNotNull(w);
        assertNotNull(w.values);
        Iterator<String> it = w.values.iterator();
        assertTrue(it.hasNext());
        assertEquals("a", it.next());
        assertEquals("b", it.next());
        assertFalse(it.hasNext());
    }

    public void testIterableWithBeans() throws Exception
    {
        String JSON = "{ \"nums\":[{\"x\":1},{\"x\":2}]}";
        ListAsIterableX w = MAPPER.readValue(JSON, ListAsIterableX.class);
        assertNotNull(w);
        assertNotNull(w.nums);
        Iterator<XBean> it = w.nums.iterator();
        assertTrue(it.hasNext());
        XBean xb = it.next();
        assertNotNull(xb);
        assertEquals(1, xb.x);
        xb = it.next();
        assertEquals(2, xb.x);
        assertFalse(it.hasNext());
    }

    // for [databind#506]
    public void testArrayIndexForExceptions() throws Exception
    {
        final String OBJECTS_JSON = "[ \"KEY2\", false ]";
        try {
            MAPPER.readValue(OBJECTS_JSON, Key[].class);
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot deserialize");
            assertEquals(1, e.getPath().size());
            assertEquals(1, e.getPath().get(0).getIndex());
        }

        try {
            MAPPER.readValue("[ \"xyz\", { } ]", String[].class);
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot deserialize");
            assertEquals(1, e.getPath().size());
            assertEquals(1, e.getPath().get(0).getIndex());
        }

        try {
            MAPPER.readValue("{\"keys\":"+OBJECTS_JSON+"}", KeyListBean.class);
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot deserialize");
            assertEquals(2, e.getPath().size());
            // Bean has no index, but has name:
            assertEquals(-1, e.getPath().get(0).getIndex());
            assertEquals("keys", e.getPath().get(0).getFieldName());

            // and for List, reverse:
            assertEquals(1, e.getPath().get(1).getIndex());
            assertNull(e.getPath().get(1).getFieldName());
        }
    }

    // for [databind#828]
    public void testWrapExceptions() throws Exception
    {
        final ObjectReader wrappingReader = MAPPER
                .readerFor(new TypeReference<List<SomeObject>>() {})
                .with(DeserializationFeature.WRAP_EXCEPTIONS);

        try {
            wrappingReader.readValue("[{}]");
        } catch (DatabindException exc) {
            assertEquals("I want to catch this exception", exc.getOriginalMessage());
        } catch (RuntimeException exc) {
            fail("The RuntimeException should have been wrapped with a DatabindException.");
        }

        final ObjectReader noWrapReader = MAPPER
                .readerFor(new TypeReference<List<SomeObject>>() {})
                .without(DeserializationFeature.WRAP_EXCEPTIONS);

        try {
            noWrapReader.readValue("[{}]");
        } catch (DatabindException exc) {
            fail("It should not have wrapped the RuntimeException.");
        } catch (RuntimeException exc) {
            assertEquals("I want to catch this exception", exc.getMessage());
        }
    }

    // [databind#2251]
    public void testAbstractListAndSet() throws Exception
    {
        final String JSON = "{\"values\":[\"foo\", \"bar\"]}";

        ListAsAbstract list = MAPPER.readValue(JSON, ListAsAbstract.class);
        assertEquals(2, list.values.size());
        assertEquals(ArrayList.class, list.values.getClass());

        SetAsAbstract set = MAPPER.readValue(JSON, SetAsAbstract.class);
        assertEquals(2, set.values.size());
        assertEquals(HashSet.class, set.values.getClass());
    }

    // for [databind#3068]
    public void testWrapExceptions3068() throws Exception
    {
        final SimpleModule module = new SimpleModule("SimpleModule", Version.unknownVersion())
                .addDeserializer(MyContainerModel.class,
                        new JsonDeserializer<MyContainerModel>() {
                    @Override
                    public MyContainerModel deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                        throw new CustomException("Custom message");
                    }
                });

        final ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();
        final String json = mapper.writeValueAsString(new MyJobModel());

        // First, verify NO wrapping:
        try {
            mapper.readerFor(MyJobModel.class)
                .without(DeserializationFeature.WRAP_EXCEPTIONS)
                .readValue(json);
            fail("Should not pass");
        } catch (CustomException e) {
            verifyException(e, "Custom message");
        } catch (JacksonException e) {
            fail("Should not have wrapped exception, got: "+e);
        }

        // and then wrapping
        try {
            mapper.readerFor(MyJobModel.class)
                .with(DeserializationFeature.WRAP_EXCEPTIONS)
                .readValue(json);
            fail("Should not pass");
        } catch (JacksonException e) {
            verifyException(e, "Custom message");
            assertEquals(JsonMappingException.class, e.getClass());
            Throwable rootC = e.getCause();
            assertNotNull(rootC);
            assertEquals(CustomException.class, rootC.getClass());
        }
    }
}
