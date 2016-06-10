package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

@SuppressWarnings("serial")
public class TestEmptyCollectionDeserialization
        extends BaseMapTest {

    private static final String EMPTY_OBJECT = "{}";

    static class XBean {
        public int x;

        public XBean() {
        }

        public XBean(int x) {
            this.x = x;
        }
    }

    // [Issue#199]
    static class ListAsIterable {
        public Iterable<String> values;
    }

    static class ListAsIterableX {
        public Iterable<XBean> nums;
    }

    // [Issue#828]
    @JsonDeserialize(using = SomeObjectDeserializer.class)
    static class SomeObject {
    }

    static class SomeObjectDeserializer extends StdDeserializer<SomeObject> {
        public SomeObjectDeserializer() {
            super(SomeObject.class);
        }

        @Override
        public SomeObject deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException {
            throw new RuntimeException("I want to catch this exception");
        }
    }

    private static class SetWrapper<T> {
        public Set<T> set;

        public SetWrapper() {
        }

        public SetWrapper(@SuppressWarnings("unchecked") T... values) {
            set = new HashSet<T>();
            Collections.addAll(set, values);
        }
    }

    private static class ArrayBlockingQueueWrapper<T> {
        public ArrayBlockingQueue<T> queue;

        public ArrayBlockingQueueWrapper() {

        }

        public ArrayBlockingQueueWrapper(@SuppressWarnings("unchecked") T... values) {
            queue = new ArrayBlockingQueue<T>(values.length + 1);
            Collections.addAll(queue, values);
        }


    }


    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final static ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void setUp() throws Exception {
        super.setUp();
        MAPPER.configure(DeserializationFeature.READ_NULL_OR_MISSING_CONTAINER_AS_EMPTY, true);
    }

    public void testListFromNull() throws Exception {
        ObjectReader r = MAPPER.reader(DeserializationFeature.READ_NULL_OR_MISSING_CONTAINER_AS_EMPTY);
        List<?> result = r.forType(List.class).readValue("null");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    public void testListFromEmpty() throws Exception {
        ObjectReader r = MAPPER.reader(DeserializationFeature.READ_NULL_OR_MISSING_CONTAINER_AS_EMPTY);
        List<?> result = r.forType(List.class).readValue("");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    public void testEmptyListWrapper() throws Exception {
        String JSON = EMPTY_OBJECT;

        ListWrapper<Object> result = MAPPER.readValue(JSON, new TypeReference<ListWrapper<Object>>() {});
        assertNotNull(result);

        assertNotNull(result.list);

        assertTrue(result.list.isEmpty());
    }

    public void testNullListWrapper() throws Exception {
        String JSON = "{\"list\": null}";

        ListWrapper<Object> result = MAPPER.readValue(JSON, new TypeReference<ListWrapper<Object>>() {});
        assertNotNull(result);

        assertNotNull(result.list);

        assertTrue(result.list.isEmpty());
    }

    public void testSetFromNull() throws Exception {
        ObjectReader r = MAPPER.reader(DeserializationFeature.READ_NULL_OR_MISSING_CONTAINER_AS_EMPTY);
        Set<?> result = r.forType(Set.class).readValue("null");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    public void testSetFromEmpty() throws Exception {
        ObjectReader r = MAPPER.reader(DeserializationFeature.READ_NULL_OR_MISSING_CONTAINER_AS_EMPTY);
        Set<?> result = r.forType(Set.class).readValue("");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    public void testEmptySetWrapper() throws Exception {
        String JSON = EMPTY_OBJECT;

        SetWrapper<Object> result = MAPPER.readValue(JSON, new TypeReference<SetWrapper<Object>>() {});
        assertNotNull(result);

        assertNotNull(result.set);
        assertTrue(result.set.isEmpty());
    }

    public void testNullSetWrapper() throws Exception {
        String JSON = "{\"set\": null}";

        SetWrapper<Object> result = MAPPER.readValue(JSON, new TypeReference<SetWrapper<Object>>() {});
        assertNotNull(result);

        assertNotNull(result.set);
        assertTrue(result.set.isEmpty());
    }

    public void testMapFromNull() throws Exception {
        ObjectReader r = MAPPER.reader(DeserializationFeature.READ_NULL_OR_MISSING_CONTAINER_AS_EMPTY);
        Map<?, ?> result = r.forType(Map.class).readValue("null");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    public void testMapFromEmpty() throws Exception {
        ObjectReader r = MAPPER.reader(DeserializationFeature.READ_NULL_OR_MISSING_CONTAINER_AS_EMPTY);
        Map<?, ?> result = r.forType(Map.class).readValue("");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    public void testEmptyMapWrapper() throws Exception {
        String JSON = EMPTY_OBJECT;

        MapWrapper<Integer, Object> result = MAPPER.readValue(JSON, new TypeReference<MapWrapper<Integer, Object>>() {});
        assertNotNull(result);

        assertNotNull(result.map);
        assertTrue(result.map.isEmpty());
    }

    public void testNullMapWrapper() throws Exception {
        String JSON = "{\"map\": null}";

        MapWrapper<Integer, Object> result = MAPPER.readValue(JSON, new TypeReference<MapWrapper<Integer, Object>>() {});
        assertNotNull(result);

        assertNotNull(result.map);
        assertTrue(result.map.isEmpty());
    }

    // [databind#161]
    public void testArrayBlockingQueueWrapper() throws Exception {
        ArrayBlockingQueueWrapper<?> q = MAPPER.readValue(EMPTY_OBJECT, ArrayBlockingQueueWrapper.class);
        assertNotNull(q);
        assertNotNull(q.queue);
        assertTrue(q.queue.isEmpty());
    }

    public void testIterableFromNull() throws Exception {
        ObjectReader r = MAPPER.reader(DeserializationFeature.READ_NULL_OR_MISSING_CONTAINER_AS_EMPTY);
        Iterable<?> result = r.forType(Iterable.class).readValue("null");
        assertNotNull(result);
        assertFalse(result.iterator().hasNext());
    }

    public void testIterableFromEmpty() throws Exception {
        ObjectReader r = MAPPER.reader(DeserializationFeature.READ_NULL_OR_MISSING_CONTAINER_AS_EMPTY);
        Iterable<?> result = r.forType(Iterable.class).readValue("");
        assertNotNull(result);
        assertFalse(result.iterator().hasNext());
    }

    // [databind#199]
    public void testIterableWrapperWithStrings() throws Exception {
        String JSON = EMPTY_OBJECT;
        ListAsIterable w = MAPPER.readValue(JSON, ListAsIterable.class);
        assertNotNull(w);
        assertNotNull(w.values);
        Iterator<String> it = w.values.iterator();
        assertFalse(it.hasNext());
    }

    // [databind#199]
    public void testIterableWrapperWithNullString() throws Exception {
        String JSON = "{\"values\": null}";
        ListAsIterable w = MAPPER.readValue(JSON, ListAsIterable.class);
        assertNotNull(w);
        assertNotNull(w.values);
        Iterator<String> it = w.values.iterator();
        assertFalse(it.hasNext());
    }

    public void testEmptyIterableWithBeans() throws Exception {
        String JSON = EMPTY_OBJECT;
        ListAsIterableX w = MAPPER.readValue(JSON, ListAsIterableX.class);
        assertNotNull(w);
        assertNotNull(w.nums);
        Iterator<XBean> it = w.nums.iterator();
        assertFalse(it.hasNext());
    }

    public void testNullIterableWithBeans() throws Exception {
        String JSON = "{\"nums\": null}";
        ListAsIterableX w = MAPPER.readValue(JSON, ListAsIterableX.class);
        assertNotNull(w);
        assertNotNull(w.nums);
        Iterator<XBean> it = w.nums.iterator();
        assertFalse(it.hasNext());
    }

    // And then a round-trip test for empty collections
    public void testRoundtrippingCollections() throws Exception {
        final TypeReference<?> listWrapperType = new TypeReference<ListWrapper<Object>>() {};

        String json = MAPPER.writeValueAsString(new ListWrapper());
        ListWrapper<Object> result = MAPPER.readValue(json, listWrapperType);
        assertNotNull(result);
        assertNotNull(result.list);
        assertTrue(result.list.isEmpty());
    }
}
