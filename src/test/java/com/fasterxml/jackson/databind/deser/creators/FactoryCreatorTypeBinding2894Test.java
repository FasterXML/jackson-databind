package com.fasterxml.jackson.databind.deser.creators;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.*;

// Test(s) to check for handling of Static Factory Creator bindings
// which up until 2.11.2 used type variable bindings of the surrounding
// class for Static methods too. This is semantically wrong, but quite a
// bit of usage existed -- but no one had contributed tests to verify
// this as expected behavior.
// When this usage broken in 2.11.3 -- it was never actually supported but
// happened to sort of work -- reports came in. This test verifies
// assumed behavior so that previous broken (but useful) bindings could
// be brought back for 2.11.4.
//
// Work for 2.12 should find better solution than this.

public class FactoryCreatorTypeBinding2894Test extends BaseMapTest
{
    // [databind#2894]
    static class Wrapper<T> {
        List<T> values;

        protected Wrapper(List<T> v) {
            values = v;
        }

        public List<T> getValue() { return values; }

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        static <T> Wrapper<T> fromValues(@JsonProperty("value") List<T> values) {
          return new Wrapper<T>(values);
        }
    }

    static class Value {
        public int x;

        protected Value() { }
        protected Value(int x0) { x = x0; }

        @Override
        public boolean equals(Object o) {
            return (o instanceof Value) && ((Value) o).x == x;
        }

        @Override
        public String toString() {
            return "[Value x="+x+"]";
        }
    }

    // [databind#2895]
    static class SimpleWrapper2895<T> {
        final T value;

        SimpleWrapper2895(T value) {
            this.value = value;
        }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static <T> SimpleWrapper2895<T> fromJson(JsonSimpleWrapper2895<T> value) {
            return new SimpleWrapper2895<>(value.object);
        }
    }

    static final class JsonSimpleWrapper2895<T> {
        @JsonProperty("object")
        public T object;
    }

    static class Account2895 {
        private long id;
        private String name;

        @JsonCreator
        public Account2895(@JsonProperty("name") String name,
                @JsonProperty("id") long id) {
            this.id = id;
            this.name = name;
        }

        public String getName() { return name; }
        public long getId() { return id; }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#2894]
    public void testIssue2894() throws Exception
    {
        Wrapper<Value> src = new Wrapper<>(Arrays.asList(new Value(1), new Value(2)));
        final String json = MAPPER.writeValueAsString(src);
        Wrapper<Value> output = MAPPER.readValue(json,
                new TypeReference<Wrapper<Value>>() {});
        assertEquals(src.values, output.values);
    }

    // [databind#2895]
    public void testIssue2895() throws Exception
    {
        SimpleWrapper2895<Account2895> wrapper = MAPPER
                .readerFor(new TypeReference<SimpleWrapper2895<Account2895>>() {})
                .readValue("{\"object\":{\"id\":1,\"name\":\"name1\"}}");

        Account2895 account = wrapper.value;
        assertEquals(1, account.getId());
        assertEquals("name1", account.getName());
    }
}
