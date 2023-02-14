package com.fasterxml.jackson.databind.deser.builder;

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
        final int index;

        MyPOJOWithArrayCreator(int i) {
            index = i;
        }

        public int getIndex() {
            return index;
        }

        public static class Builder {
            int index;

            public Builder() {
                // Default constructor
            }

            @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
            public Builder(final List<Object> jsonArray) {
                withIndex((int) jsonArray.get(0));
            }

            // When deserialized via builder
            public Builder withIndex(int i) {
                index = i;
                return this;
            }

            // When deserialized into a builder
            public Builder setIndex(int i) {
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

        MyPOJOWithPrimitiveCreator(int i) {
            index = i;
        }

        public int getIndex() {
            return index;
        }

        public static class Builder {
            int index;

            public Builder() {
                // Default constructor
            }

            @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
            public Builder(final int i) {
                withIndex(i);
            }

            // When deserialized via builder
            public Builder withIndex(int i) {
                index = i;
                return this;
            }

            // When deserialized into a builder
            public Builder setIndex(int i) {
                index = i;
                return this;
            }

            public MyPOJOWithPrimitiveCreator build() {
                return new MyPOJOWithPrimitiveCreator(index);
            }
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // This test passes when the array based @JsonCreator is removed from the
    // MyPOJOWithArrayCreator.Builder implementation. The presence of the creator
    // in the case of arrays breaks deserialize from an object.
    //
    // Compare that to the analogous tests for MyPOJOWithPrimitiveCreator which
    // pass in both cases.
    //
    // I left some notes in BeanDeserializerBase as to behavior.
    public void testPOJOWithArrayCreatorFromObjectRepresentation() throws Exception {
        final String json = a2q("{ 'index': 123 }");
        final MyPOJOWithArrayCreator deserialized = MAPPER.readValue(json, MyPOJOWithArrayCreator.class);
        assertEquals(123, deserialized.getIndex());
    }

    public void testPOJOWithArrayCreatorFromArrayRepresentation() throws Exception {
        final String json = "[123]";
        final MyPOJOWithArrayCreator deserialized = MAPPER.readValue(json, MyPOJOWithArrayCreator.class);
        assertEquals(123, deserialized.getIndex());
    }

    public void testPOJOWithPrimitiveCreatorFromObjectRepresentation() throws Exception {
        final String json = a2q("{ 'index': 123 }");
        final MyPOJOWithPrimitiveCreator deserialized = MAPPER.readValue(json, MyPOJOWithPrimitiveCreator.class);
        assertEquals(123, deserialized.getIndex());
    }

    public void testPOJOWithPrimitiveCreatorFromPrimitiveRepresentation() throws Exception {
        final String json ="123";
        final MyPOJOWithPrimitiveCreator deserialized = MAPPER.readValue(json, MyPOJOWithPrimitiveCreator.class);
        assertEquals(123, deserialized.getIndex());
    }

    // Now let's try it without the builder by deserializing directly into an
    // instance of the POJO Builder class instead of via it into the POJO.

    // This fails the same as above. So the failure of default deserialization
    // from an object shape in the presence of a @JsonCreator accepting an array
    // is not specific to the use of Builders as an intermediary.
    public void testPOJOBuilderWithArrayCreatorFromObjectRepresentation() throws Exception {
        final String json = a2q("{ 'index': 123 }");
        final MyPOJOWithArrayCreator.Builder deserialized = MAPPER.readValue(json, MyPOJOWithArrayCreator.Builder.class);
        assertEquals(123, deserialized.index);
    }

    public void testPOJOBuilderWithArrayCreatorFromArrayRepresentation() throws Exception {
        final String json = "[123]";
        final MyPOJOWithArrayCreator.Builder deserialized = MAPPER.readValue(json, MyPOJOWithArrayCreator.Builder.class);
        assertEquals(123, deserialized.index);
    }

    public void testPOJOBuilderWithPrimitiveCreatorFromObjectRepresentation() throws Exception {
        final String json = a2q("{ 'index': 123 }");
        final MyPOJOWithPrimitiveCreator.Builder deserialized = MAPPER.readValue(json, MyPOJOWithPrimitiveCreator.Builder.class);
        assertEquals(123, deserialized.index);
    }

    public void testPOJOBuilderWithPrimitiveCreatorFromPrimitiveRepresentation() throws Exception {
        final String json = "123";
        final MyPOJOWithPrimitiveCreator.Builder deserialized = MAPPER.readValue(json, MyPOJOWithPrimitiveCreator.Builder.class);
        assertEquals(123, deserialized.index);
    }
}