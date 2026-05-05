package tools.jackson.databind.objectid;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.util.TokenBuffer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for [dataformats-text#292]: native Object Ids (e.g. YAML anchors)
 * with builder-based deserialization ({@code @JsonDeserialize(builder=...)}).
 *<p>
 * Uses {@link TokenBuffer} to simulate a parser that supports native object ids.
 */
class NativeObjectIdWithBuilderTest extends DatabindTestUtil
{
    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@id")
    @JsonDeserialize(builder = SimpleBuilder.class)
    static class SimpleValue {
        public int value;

        SimpleValue(int v) { value = v; }
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@id")
    @JsonPOJOBuilder(withPrefix = "with")
    static class SimpleBuilder {
        private int value;

        public SimpleBuilder withValue(int v) { value = v; return this; }
        public SimpleValue build() { return new SimpleValue(value); }
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonDeserialize(builder = PropIdBuilder.class)
    static class PropIdValue {
        public String id;
        public String name;

        PropIdValue(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonPOJOBuilder(withPrefix = "")
    static class PropIdBuilder {
        private String id;
        private String name;

        public PropIdBuilder id(String v) { id = v; return this; }
        public PropIdBuilder name(String v) { name = v; return this; }
        public PropIdValue build() { return new PropIdValue(id, name); }
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@id")
    @JsonDeserialize(builder = RefBuilder.class)
    static class RefValue {
        public int data;
        public RefValue next;

        RefValue(int data, RefValue next) {
            this.data = data;
            this.next = next;
        }
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@id")
    @JsonPOJOBuilder(withPrefix = "")
    static class RefBuilder {
        private int data;
        private RefValue next;

        public RefBuilder data(int v) { data = v; return this; }
        public RefBuilder next(RefValue v) { next = v; return this; }
        public RefValue build() { return new RefValue(data, next); }
    }

    static class RefValueWrapper {
        public RefValue first;
        public RefValue second;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // [dataformats-text#292]: basic native object id with builder
    @Test
    void nativeObjectIdWithBuilder() throws Exception
    {
        TokenBuffer buf = new TokenBuffer(null, true);
        buf.writeStartObject();
        buf.writeObjectId(1);
        buf.writeName("value");
        buf.writeNumber(42);
        buf.writeEndObject();

        JsonParser p = buf.asParser(ObjectReadContext.empty());
        SimpleValue result = MAPPER.readValue(p, SimpleValue.class);
        p.close();
        buf.close();

        assertNotNull(result);
        assertEquals(42, result.value);
    }

    // [dataformats-text#292]: native object id with PropertyGenerator and builder
    @Test
    void nativeObjectIdPropertyGeneratorWithBuilder() throws Exception
    {
        TokenBuffer buf = new TokenBuffer(null, true);
        buf.writeStartObject();
        buf.writeObjectId("abc123");
        buf.writeName("id");
        buf.writeString("abc123");
        buf.writeName("name");
        buf.writeString("test");
        buf.writeEndObject();

        JsonParser p = buf.asParser(ObjectReadContext.empty());
        PropIdValue result = MAPPER.readValue(p, PropIdValue.class);
        p.close();
        buf.close();

        assertNotNull(result);
        assertEquals("abc123", result.id);
        assertEquals("test", result.name);
    }

    // [dataformats-text#292]: native object id with builder and back-reference
    @Test
    void nativeObjectIdWithBuilderBackReference() throws Exception
    {
        TokenBuffer buf = new TokenBuffer(null, true);
        buf.writeStartObject();
        // "first": full object with native id
        buf.writeName("first");
        buf.writeStartObject();
        buf.writeObjectId(1);
        buf.writeName("data");
        buf.writeNumber(99);
        buf.writeEndObject();
        // "second": reference by id
        buf.writeName("second");
        buf.writeNumber(1);
        buf.writeEndObject();

        JsonParser p = buf.asParser(ObjectReadContext.empty());
        RefValueWrapper result = MAPPER.readValue(p, RefValueWrapper.class);
        p.close();
        buf.close();

        assertNotNull(result);
        assertNotNull(result.first);
        assertEquals(99, result.first.data);
        // second should resolve to the same instance as first
        assertSame(result.first, result.second);
    }
}
