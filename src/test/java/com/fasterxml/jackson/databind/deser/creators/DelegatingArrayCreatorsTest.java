package com.fasterxml.jackson.databind.deser.creators;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;

public class DelegatingArrayCreatorsTest extends BaseMapTest
{
    public static class MyTypeImpl extends MyType {
        private final List<Integer> values;

        MyTypeImpl(List<Integer> values) {
            this.values = values;
        }

        @Override
        public List<Integer> getValues() {
            return values;
        }
    }

    static abstract class MyType {
        @JsonValue
        public abstract List<Integer> getValues();

        @JsonCreator(mode=JsonCreator.Mode.DELEGATING)
        public static MyType of(List<Integer> values) {
            return new MyTypeImpl(values);
        }
    }

    @JsonDeserialize(as=ImmutableBag2324.class)
    public interface Bag2324<T> extends Collection<T> { }

    public static class ImmutableBag2324<T> extends AbstractCollection<T> implements Bag2324<T>  {
        @Override
        public Iterator<T> iterator() { return elements.iterator(); }

        @Override
        public int size() { return elements.size(); }

        @JsonCreator(mode=JsonCreator.Mode.DELEGATING)
        private ImmutableBag2324(Collection<T> elements ) {
            this.elements = Collections.unmodifiableCollection(elements);
        }

        private final Collection<T> elements;
    }

    static class Value2324 {
        public String value;

        public Value2324(String v) { value = v; }

        @Override
        public boolean equals(Object o) {
            return value.equals(((Value2324) o).value);
        }
    }

    static class WithBagOfStrings2324 {
        public Bag2324<String> getStrings() { return this.bagOfStrings; }
        public void setStrings(Bag2324<String> bagOfStrings) { this.bagOfStrings = bagOfStrings; }
        private Bag2324<String> bagOfStrings;
    }

    static class WithBagOfValues2324 {
        public Bag2324<Value2324> getValues() { return this.bagOfValues; }
        public void setValues(Bag2324<Value2324> bagOfValues) { this.bagOfValues = bagOfValues; }
        private Bag2324<Value2324> bagOfValues;
    }

    static class MultipleArrayDelegators {
        @JsonCreator(mode=JsonCreator.Mode.DELEGATING)
        MultipleArrayDelegators(List<Integer> a) { }

        @JsonCreator(mode=JsonCreator.Mode.DELEGATING)
        MultipleArrayDelegators(Set<Integer> a) { }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = sharedMapper();

    // [databind#1804]
    public void testDelegatingArray1804() throws Exception {
        MyType thing = MAPPER.readValue("[]", MyType.class);
        assertNotNull(thing);
    }

    // [databind#2324]
    public void testDeserializeBagOfStrings() throws Exception {
        WithBagOfStrings2324 result = MAPPER.readerFor(WithBagOfStrings2324.class)
                .readValue("{\"strings\": [ \"a\", \"b\", \"c\"]}");
        assertEquals(3, result.getStrings().size());
    }

    // [databind#2324]
    public void testDeserializeBagOfPOJOs() throws Exception {
        WithBagOfValues2324 result = MAPPER.readerFor(WithBagOfValues2324.class)
                .readValue("{\"values\": [ \"a\", \"b\", \"c\"]}");
        assertEquals(3, result.getValues().size());
        assertEquals(new Value2324("a"),  result.getValues().iterator().next());
    }

    public void testInvalidTwoArrayDelegating() throws Exception {
        try {
            /*MultipleArrayDelegators result =*/ MAPPER.readerFor(MultipleArrayDelegators.class)
                .readValue("[ ]");
            fail("Should not pass");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Conflicting array-delegate creators");
            verifyException(e, "already had explicitly marked");
        }
    }
}
