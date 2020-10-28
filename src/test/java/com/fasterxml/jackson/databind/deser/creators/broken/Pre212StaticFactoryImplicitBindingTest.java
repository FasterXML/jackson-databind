package com.fasterxml.jackson.databind.deser.creators.broken;

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

public class Pre212StaticFactoryImplicitBindingTest extends BaseMapTest
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

    private final ObjectMapper MAPPER = newJsonMapper();

    public void testIssue2894() throws Exception
    {
        Wrapper<Value> src = new Wrapper<>(Arrays.asList(new Value(1), new Value(2)));
        final String json = MAPPER.writeValueAsString(src);
        Wrapper<Value> output = MAPPER.readValue(json,
                new TypeReference<Wrapper<Value>>() {});
        assertEquals(src.values, output.values);
    }
}
