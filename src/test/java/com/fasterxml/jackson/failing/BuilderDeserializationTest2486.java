package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;

public class BuilderDeserializationTest2486
        extends BaseMapTest
{
    @JsonDeserialize(builder = MyPOJOWithArrayCreator.Builder.class)
    public static class MyPOJOWithArrayCreator {
        private final int index;

        private MyPOJOWithArrayCreator(int i) {
            index = i;
        }

        public int getIndex() {
            return index;
        }

        public static class Builder {
            private int index;

            public Builder() {
                // Default constructor
            }

            @JsonCreator
            public Builder(final List<Object> jsonArray) {
                withIndex((int) jsonArray.get(0));
            }

            public Builder withIndex(int i) {
                index = i;
                return this;
            }

            public MyPOJOWithArrayCreator build() {
                return new MyPOJOWithArrayCreator(index);
            }
        }
    }

    @JsonDeserialize(builder = MyPOJOWithPrimitiveCreator.Builder.class)
    public static class MyPOJOWithPrimitiveCreator {
        private final int index;

        private MyPOJOWithPrimitiveCreator(int i) {
            index = i;
        }

        public int getIndex() {
            return index;
        }

        public static class Builder {
            private int index;

            public Builder() {
                // Default constructor
            }

            @JsonCreator
            public Builder(final int i) {
                withIndex(i);
            }

            public Builder withIndex(int i) {
                index = i;
                return this;
            }

            public MyPOJOWithPrimitiveCreator build() {
                return new MyPOJOWithPrimitiveCreator(index);
            }
        }
    }

    // This test passes when the array based @JsonCreator is removed from the
    // MyPOJOWithArrayCreator.Builder implementation. The presence of the creator
    // in the case of arrays breaks deserialize from an object.
    //
    // Compare that to the analogous tests for MyPOJOWithPrimitiveCreator which
    // pass in both cases.
    //
    // I left some notes in BeanDeserializerBase as to behavior.
    public void testPOJOWithArrayCreatorFromObjectRepresentation() throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        final String json = aposToQuotes("{ 'index': 123 }");
        final MyPOJOWithArrayCreator deserialized = mapper.readValue(json, MyPOJOWithArrayCreator.class);
        assertEquals(123, deserialized.getIndex());
    }

    public void testPOJOWithArrayCreatorFromArrayRepresentation() throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        final String json = aposToQuotes("[123]");
        final MyPOJOWithArrayCreator deserialized = mapper.readValue(json, MyPOJOWithArrayCreator.class);
        assertEquals(123, deserialized.getIndex());
    }

    public void testPOJOWithPrimitiveCreatorFromObjectRepresentation() throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        final String json = aposToQuotes("{ 'index': 123 }");
        final MyPOJOWithPrimitiveCreator deserialized = mapper.readValue(json, MyPOJOWithPrimitiveCreator.class);
        assertEquals(123, deserialized.getIndex());
    }

    public void testPOJOWithPrimitiveCreatorFromPrimitiveRepresentation() throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        final String json = aposToQuotes("123");
        final MyPOJOWithPrimitiveCreator deserialized = mapper.readValue(json, MyPOJOWithPrimitiveCreator.class);
        assertEquals(123, deserialized.getIndex());
    }
}