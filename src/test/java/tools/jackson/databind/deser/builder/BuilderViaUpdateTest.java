package tools.jackson.databind.deser.builder;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.exc.InvalidDefinitionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import static tools.jackson.databind.testutil.DatabindTestUtil.a2q;
import static tools.jackson.databind.testutil.DatabindTestUtil.verifyException;

/**
 * Tests for use of {@code withValueToUpdate()} with builder-based deserializers.
 * Passing a Builder instance is supported (see [databind#2100]); passing an
 * already-built value is not.
 */
public class BuilderViaUpdateTest
{
    @JsonDeserialize(builder=SimpleBuilderXY.class)
    static class ValueClassXY
    {
        public final int x, y;

        protected ValueClassXY(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    static class SimpleBuilderXY
    {
        public int x, y;

        public SimpleBuilderXY withX(int x0) {
            this.x = x0;
            return this;
        }

        public SimpleBuilderXY withY(int y0) {
            this.y = y0;
            return this;
        }

        public ValueClassXY build() {
            return new ValueClassXY(x, y);
        }
    }

    // Mirrors the reproducer from [databind#2100]
    @JsonDeserialize(builder = POJO2100.Builder.class)
    static class POJO2100
    {
        public final int id;
        public final String value;

        POJO2100(int id, String value) {
            this.id = id;
            this.value = value;
        }

        static class Builder
        {
            int id;
            String value;

            public Builder withId(int id) {
                this.id = id;
                return this;
            }

            public Builder withValue(String value) {
                this.value = value;
                return this;
            }

            public POJO2100 build() {
                return new POJO2100(id, value);
            }
        }
    }

    private final static ObjectMapper MAPPER = new ObjectMapper();

    // Passing the built value itself remains unsupported: builder-backed POJOs
    // are typically immutable and we have no way to re-populate builder state.
    @Test
    public void testBuilderUpdateWithValue() throws Exception
    {
        try {
            /*ValueClassXY value =*/ MAPPER.readerFor(ValueClassXY.class)
                    .withValueToUpdate(new ValueClassXY(6, 7))
                    .readValue(a2q("{'x':1,'y':2}"));
            fail("Should not have passed");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Deserialization of");
            verifyException(e, "by passing existing instance");
            verifyException(e, "ValueClassXY");
            // Hint should name the Builder class as the expected update-value type
            verifyException(e, "pass a Builder");
            verifyException(e, "SimpleBuilderXY");
        }
    }

    // [databind#2100]: passing a Builder instance merges JSON on top of its
    // current state and then produces the built value.
    @Test
    public void testBuilderUpdateWithBuilder() throws Exception
    {
        SimpleBuilderXY builder = new SimpleBuilderXY();
        builder.x = 10;
        builder.y = 20;
        ValueClassXY result = MAPPER.readerFor(ValueClassXY.class)
                .withValueToUpdate(builder)
                .readValue(a2q("{'x':1}"));
        // Builder state: x overridden by JSON, y preserved from pre-set state
        assertEquals(1, builder.x);
        assertEquals(20, builder.y);
        // And build() produces a value reflecting the merged builder
        assertNotNull(result);
        assertEquals(1, result.x);
        assertEquals(20, result.y);
    }

    // [databind#2100]: verbatim reproducer from the issue report.
    @Test
    public void testIssue2100Reproducer() throws Exception
    {
        POJO2100.Builder builder = new POJO2100.Builder().withId(1).withValue("1");
        POJO2100 result = MAPPER.readerFor(POJO2100.class)
                .withValueToUpdate(builder)
                .readValue(a2q("{'value':'2'}"));
        assertEquals(1, builder.id);
        assertEquals("2", builder.value);
        assertNotNull(result);
        assertEquals(1, result.id);
        assertEquals("2", result.value);
    }
}
