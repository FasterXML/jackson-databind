package tools.jackson.databind.struct;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UnwrappedWithIgnore1075Test extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#1075]
    static class Outer {
        @JsonUnwrapped
        private Inner inner;

        @JsonIgnore
        public Long getId() {
            return inner.id;
        }
    }

    static class Inner {
        @JsonProperty
        Long id;

        @JsonProperty
        String name;
    }

    // [databind#1075]: same as `Outer`, but with a property-based `@JsonCreator`,
    // which is handled by a separate code path
    static class OuterWithCreator {
        @JsonUnwrapped
        private Inner inner;

        private final int tag;

        @JsonCreator
        public OuterWithCreator(@JsonProperty("tag") int tag) {
            this.tag = tag;
        }

        @JsonIgnore
        public Long getId() {
            return inner.id;
        }

        public int getTag() {
            return tag;
        }
    }

    // [databind#1075]: builder-based equivalent, with a property-based Creator on the
    // builder, so that the outer type declares "id" ignorable while the unwrapped
    // `Inner` is the one that actually provides it
    @JsonDeserialize(builder = OuterBuilder.class)
    static class OuterFromBuilder {
        final Inner inner;
        final int tag;

        OuterFromBuilder(Inner inner, int tag) {
            this.inner = inner;
            this.tag = tag;
        }

        public Long getId() {
            return inner.id;
        }
    }

    @JsonPOJOBuilder(withPrefix = "with")
    @JsonIgnoreProperties({"id"})
    static class OuterBuilder {
        @JsonUnwrapped
        Inner inner;

        final int tag;

        @JsonCreator
        OuterBuilder(@JsonProperty("tag") int tag) {
            this.tag = tag;
        }

        public OuterFromBuilder build() {
            return new OuterFromBuilder(inner, tag);
        }
    }

    // [databind#1075]
    @Test
    public void jsonUnwrappedShouldDeserializeFieldsWithGetterInOuterClass() throws Exception
    {
        final String JSON = "{\"id\": 1, \"name\": \"John\"}";
        Outer outer = MAPPER.readValue(JSON, Outer.class);
        assertEquals(Long.valueOf(1), outer.getId());
    }

    // [databind#1075]: as above but via property-based Creator -- goes through
    // `BeanDeserializer.deserializeUsingPropertyBasedWithUnwrapped()` instead of
    // `deserializeWithUnwrapped()`, so needs its own coverage
    @Test
    public void jsonUnwrappedShouldDeserializeFieldsWithGetterInOuterClassViaCreator() throws Exception
    {
        OuterWithCreator outer = MAPPER.readValue("""
                {"tag": 7, "id": 1, "name": "John"}
                """, OuterWithCreator.class);
        assertEquals(Long.valueOf(1), outer.getId());
        assertEquals("John", outer.inner.name);
        assertEquals(7, outer.getTag());
    }

    // [databind#1075]: as above but builder-based -- goes through
    // `BuilderBasedDeserializer.deserializeUsingPropertyBasedWithUnwrapped()`.
    //
    // NOTE: property ORDER is significant here. That method delegates to
    // `deserializeWithUnwrapped()` as soon as the last Creator property is seen, so
    // the Creator property ("tag") must come LAST for the unwrapped/ignorable names
    // to be handled by the method under test. Both orders are asserted, since the
    // result must not depend on JSON property order.
    @Test
    public void jsonUnwrappedShouldDeserializeIgnoredNameViaBuilderCreator() throws Exception
    {
        // Creator property last: handled by deserializeUsingPropertyBasedWithUnwrapped()
        OuterFromBuilder outer = MAPPER.readValue("""
                {"id": 1, "name": "John", "tag": 7}
                """, OuterFromBuilder.class);
        assertEquals(Long.valueOf(1), outer.getId());
        assertEquals("John", outer.inner.name);
        assertEquals(7, outer.tag);

        // Creator property first: delegated to deserializeWithUnwrapped()
        OuterFromBuilder outer2 = MAPPER.readValue("""
                {"tag": 7, "id": 1, "name": "John"}
                """, OuterFromBuilder.class);
        assertEquals(Long.valueOf(1), outer2.getId());
        assertEquals("John", outer2.inner.name);
        assertEquals(7, outer2.tag);
    }
}
