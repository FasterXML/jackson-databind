package tools.jackson.databind.introspect;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;
import tools.jackson.databind.exc.UnrecognizedPropertyException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class AccessorNamingForBuilderTest extends DatabindTestUtil
{
    @JsonDeserialize(builder=NoPrexixBuilderXY.class)
    static class ValueClassXY
    {
        final int _x, _y;

        protected ValueClassXY(int x, int y) {
            _x = x+1;
            _y = y+1;
        }
    }

    static class NoPrexixBuilderXY
    {
        // non-public so as not to discover without prefix-match
        protected int x, y;

        public NoPrexixBuilderXY x(int x0) {
              this.x = x0;
              return this;
        }

        public NoPrexixBuilderXY y(int y0) {
              this.y = y0;
              return this;
        }

        public ValueClassXY build() {
              return new ValueClassXY(x, y);
        }
    }

    // [databind#2624]: Test with builderPrefix="" on @JsonDeserialize (Lombok-style)
    @JsonDeserialize(builder=NoPrefixBuilderViaAnnotation.NoPrefixBuilder.class, builderPrefix="")
    static class NoPrefixBuilderViaAnnotation
    {
        final int a, b;

        protected NoPrefixBuilderViaAnnotation(int a, int b) {
            this.a = a;
            this.b = b;
        }

        static class NoPrefixBuilder
        {
            protected int a, b;

            public NoPrefixBuilder a(int a0) {
                this.a = a0;
                return this;
            }

            public NoPrefixBuilder b(int b0) {
                this.b = b0;
                return this;
            }

            public NoPrefixBuilderViaAnnotation build() {
                return new NoPrefixBuilderViaAnnotation(a, b);
            }
        }
    }

    // [databind#2624]: Test with custom builderPrefix ("set") on @JsonDeserialize
    @JsonDeserialize(builder=SetPrefixBuilderViaAnnotation.SetPrefixBuilder.class, builderPrefix="set")
    static class SetPrefixBuilderViaAnnotation
    {
        final String name;
        final int value;

        protected SetPrefixBuilderViaAnnotation(String name, int value) {
            this.name = name;
            this.value = value;
        }

        static class SetPrefixBuilder
        {
            protected String name;
            protected int value;

            public SetPrefixBuilder setName(String n) {
                this.name = n;
                return this;
            }

            public SetPrefixBuilder setValue(int v) {
                this.value = v;
                return this;
            }

            public SetPrefixBuilderViaAnnotation build() {
                return new SetPrefixBuilderViaAnnotation(name, value);
            }
        }
    }

    // [databind#2624]: Test that @JsonDeserialize.builderPrefix overrides @JsonPOJOBuilder.withPrefix
    @JsonDeserialize(builder=AnnotationOverrideTest.OverriddenBuilder.class, builderPrefix="")
    static class AnnotationOverrideTest
    {
        final int x;

        protected AnnotationOverrideTest(int x) {
            this.x = x;
        }

        // Builder has @JsonPOJOBuilder(withPrefix="with"), but @JsonDeserialize(builderPrefix="") should win
        @JsonPOJOBuilder(withPrefix="with")
        static class OverriddenBuilder
        {
            protected int x;

            // Using no-prefix method name, not "withX"
            public OverriddenBuilder x(int x0) {
                this.x = x0;
                return this;
            }

            public AnnotationOverrideTest build() {
                return new AnnotationOverrideTest(x);
            }
        }
    }

    // [databind#2624]: Test that @JsonPOJOBuilder.withPrefix still works when builderPrefix not specified
    @JsonDeserialize(builder=FallbackToPojoBuilderTest.PojoBuilder.class)
    static class FallbackToPojoBuilderTest
    {
        final int y;

        protected FallbackToPojoBuilderTest(int y) {
            this.y = y;
        }

        @JsonPOJOBuilder(withPrefix="set")
        static class PojoBuilder
        {
            protected int y;

            public PojoBuilder setY(int y0) {
                this.y = y0;
                return this;
            }

            public FallbackToPojoBuilderTest build() {
                return new FallbackToPojoBuilderTest(y);
            }
        }
    }

    // For [databind#2624]
    @Test
    public void testAccessorCustomWithMethod() throws Exception
    {
        final String json = a2q("{'x':28,'y':72}");
        final ObjectMapper vanillaMapper = jsonMapperBuilder()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

        // First: without custom strategy, will fail:
        try {
            ValueClassXY xy = vanillaMapper.readValue(json, ValueClassXY.class);
            fail("Should not pass, got instance with x="+xy._x+", y="+xy._y);
        } catch (UnrecognizedPropertyException e) {
            verifyException(e, "Unrecognized ");
        }

        // But fine with "no-prefix"
        final ObjectMapper customMapper = jsonMapperBuilder()
                .accessorNaming(new DefaultAccessorNamingStrategy.Provider()
                        .withBuilderPrefix("")
                )
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        ValueClassXY xy = customMapper.readValue(json, ValueClassXY.class);
        assertEquals(29, xy._x);
        assertEquals(73, xy._y);
    }

    // [databind#2624]: Test @JsonDeserialize.builderPrefix with empty string (Lombok-style)
    @Test
    public void testBuilderPrefixEmptyViaAnnotation() throws Exception
    {
        final ObjectMapper mapper = newJsonMapper();
        final String json = a2q("{'a':10,'b':20}");
        NoPrefixBuilderViaAnnotation result = mapper.readValue(json, NoPrefixBuilderViaAnnotation.class);
        assertEquals(10, result.a);
        assertEquals(20, result.b);
    }

    // [databind#2624]: Test @JsonDeserialize.builderPrefix with custom prefix ("set")
    @Test
    public void testBuilderPrefixCustomViaAnnotation() throws Exception
    {
        final ObjectMapper mapper = newJsonMapper();
        final String json = a2q("{'name':'test','value':42}");
        SetPrefixBuilderViaAnnotation result = mapper.readValue(json, SetPrefixBuilderViaAnnotation.class);
        assertEquals("test", result.name);
        assertEquals(42, result.value);
    }

    // [databind#2624]: Test that @JsonDeserialize.builderPrefix overrides @JsonPOJOBuilder.withPrefix
    @Test
    public void testBuilderPrefixOverridesJsonPOJOBuilder() throws Exception
    {
        final ObjectMapper mapper = newJsonMapper();
        final String json = a2q("{'x':99}");
        AnnotationOverrideTest result = mapper.readValue(json, AnnotationOverrideTest.class);
        assertEquals(99, result.x);
    }

    // [databind#2624]: Test fallback to @JsonPOJOBuilder when builderPrefix not specified
    @Test
    public void testFallbackToJsonPOJOBuilderPrefix() throws Exception
    {
        final ObjectMapper mapper = newJsonMapper();
        final String json = a2q("{'y':55}");
        FallbackToPojoBuilderTest result = mapper.readValue(json, FallbackToPojoBuilderTest.class);
        assertEquals(55, result.y);
    }
}
