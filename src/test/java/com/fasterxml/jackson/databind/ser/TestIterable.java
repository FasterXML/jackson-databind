package com.fasterxml.jackson.databind.ser;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

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

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final static ObjectMapper MAPPER = new ObjectMapper();
    
    public void testIterator() throws IOException
    {
        StringWriter sw = new StringWriter();
        ArrayList<Integer> l = new ArrayList<Integer>();
        l.add(1);
        l.add(-9);
        l.add(0);
        MAPPER.writeValue(sw, l.iterator());
        assertEquals("[1,-9,0]", sw.toString().trim());
    }

    public void testIterable() throws IOException
    {
        StringWriter sw = new StringWriter();
        MAPPER.writeValue(sw, new IterableWrapper(new int[] { 1, 2, 3 }));
        assertEquals("[1,2,3]", sw.toString().trim());
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
    
    // [databind#358]
    public void testIterable358() throws Exception {
        String json = MAPPER.writeValueAsString(new B());
        assertEquals("{\"list\":[[\"Hello world.\"]]}", json);
    }
}
