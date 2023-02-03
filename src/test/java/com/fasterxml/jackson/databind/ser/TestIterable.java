package com.fasterxml.jackson.databind.ser;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class TestIterable extends BaseMapTest
{
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
    static class A {
        public String unexpected = "Bye.";
    }

    static class B {
        @JsonSerialize(as = Iterable.class,
                contentUsing = ASerializer.class)
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

    // [databind#2390]
    @JsonFilter("default")
    static class IntIterable2390 extends IntIterable { }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    private final ObjectMapper STATIC_MAPPER = jsonMapperBuilder()
            .enable(MapperFeature.USE_STATIC_TYPING)
            .build();

    public void testIterator() throws IOException
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

    public void testIterable() throws IOException
    {
        assertEquals("[1,2,3]",
                MAPPER.writeValueAsString(new IterableWrapper(new int[] { 1, 2, 3 })));
    }

    public void testWithIterable() throws IOException
    {
        assertEquals("{\"values\":[\"value\"]}",
                STATIC_MAPPER.writeValueAsString(new BeanWithIterable()));
        assertEquals("[1,2,3]",
                STATIC_MAPPER.writeValueAsString(new IntIterable()));
    }

    public void testWithIterator() throws IOException
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
    public void testIterable358() throws Exception {
        String json = MAPPER.writeValueAsString(new B());
        assertEquals("{\"list\":[[\"Hello world.\"]]}", json);
    }

    // [databind#2390]
    public void testIterableWithAnnotation() throws Exception
    {
        assertEquals("[1,2,3]",
                STATIC_MAPPER.writeValueAsString(new IntIterable2390()));
    }
}
