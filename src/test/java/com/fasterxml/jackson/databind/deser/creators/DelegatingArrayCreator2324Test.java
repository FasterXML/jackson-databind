package com.fasterxml.jackson.databind.deser.creators;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

// for [databind#2324]
public class DelegatingArrayCreator2324Test extends BaseMapTest
{
    @JsonDeserialize(as=ImmutableBag.class)
    public interface Bag<T> extends Collection<T> { }

    public static class ImmutableBag<T> extends AbstractCollection<T> implements Bag<T>  {
        @Override
        public Iterator<T> iterator() { return elements.iterator(); }

        @Override
        public int size() { return elements.size(); }

        @JsonCreator(mode=JsonCreator.Mode.DELEGATING)
        private ImmutableBag(Collection<T> elements ) {
            this.elements = Collections.unmodifiableCollection(elements);
        }

        private final Collection<T> elements;
    }

    static class Value {
        public String value;

        public Value(String v) { value = v; }

        @Override
        public boolean equals(Object o) {
            return value.equals(((Value) o).value);
        }
    }

    static class WithBagOfStrings {
        public Bag<String> getStrings() { return this.bagOfStrings; }
        public void setStrings(Bag<String> bagOfStrings) { this.bagOfStrings = bagOfStrings; }
        private Bag<String> bagOfStrings;
    }

    static class WithBagOfValues {
        public Bag<Value> getValues() { return this.bagOfValues; }
        public void setValues(Bag<Value> bagOfValues) { this.bagOfValues = bagOfValues; }
        private Bag<Value> bagOfValues;
    }    

    private final ObjectMapper MAPPER = objectMapper();

    public void testDeserializeBagOfStrings() throws Exception {
        WithBagOfStrings result = MAPPER.readerFor(WithBagOfStrings.class)
                .readValue("{\"strings\": [ \"a\", \"b\", \"c\"]}");
        assertEquals(3, result.getStrings().size());
    }

    public void testDeserializeBagOfPOJOs() throws Exception {
        WithBagOfValues result = MAPPER.readerFor(WithBagOfValues.class)
                .readValue("{\"values\": [ \"a\", \"b\", \"c\"]}");
        assertEquals(3, result.getValues().size());
        assertEquals(new Value("a"),  result.getValues().iterator().next());
    }
}
