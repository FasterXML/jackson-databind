package tools.jackson.databind.struct;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonWrapped;
import tools.jackson.databind.deser.DeserializationProblemHandler;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link JsonWrapped} annotation deserialization.
 */
public class JsonWrappedDeserializationTest extends DatabindTestUtil
{
    static class Gene {
        public String symbol;

        @JsonWrapped("chr")
        @JsonProperty("id")
        public String chrId;

        @JsonWrapped("chr")
        @JsonProperty("name")
        public String chrName;

        public Gene() { }
    }

    // Test with @JsonAnySetter
    static class BeanWithAnySetter {
        public String symbol;

        @JsonWrapped("chr")
        @JsonProperty("id")
        public String chrId;

        public java.util.Map<String, Object> extra = new java.util.HashMap<>();

        @JsonAnySetter
        public void setExtra(String key, Object value) {
            extra.put(key, value);
        }
    }

    // Bean with two independent wrapper groups
    static class MultiWrapperBean {
        @JsonWrapped("w1")
        @JsonProperty("a")
        public String w1a;

        @JsonWrapped("w1")
        @JsonProperty("b")
        public String w1b;

        @JsonWrapped("w2")
        @JsonProperty("c")
        public String w2c;

        @JsonWrapped("w2")
        @JsonProperty("d")
        public String w2d;

        public MultiWrapperBean() { }
    }

    private final ObjectMapper MAPPER = JsonMapper.builder().build();

    @Nested
    @DisplayName("basic deserialization")
    class BasicDeserializationTests {

        @Test
        @DisplayName("should populate all fields when wrapper object present")
        public void testRoundTrip() throws Exception
        {
            // setup
            String json = "{\"symbol\":\"TP53\",\"chr\":{\"id\":\"17\",\"name\":\"chr17\"}}";

            // when
            Gene gene = MAPPER.readValue(json, Gene.class);

            // then
            assertThat(gene.symbol).isEqualTo("TP53");
            assertThat(gene.chrId).isEqualTo("17");
            assertThat(gene.chrName).isEqualTo("chr17");
        }

        @Test
        @DisplayName("should leave wrapped fields null when wrapper object absent")
        public void testMissingWrapper() throws Exception
        {
            // setup
            String json = "{\"symbol\":\"TP53\"}";

            // when
            Gene gene = MAPPER.readValue(json, Gene.class);

            // then
            assertThat(gene.symbol).isEqualTo("TP53");
            assertThat(gene.chrId).isNull();
            assertThat(gene.chrName).isNull();
        }

        @Test
        @DisplayName("should leave wrapped fields null when wrapper object is null")
        public void testNullWrapper() throws Exception
        {
            // setup
            String json = "{\"symbol\":\"TP53\",\"chr\":null}";

            // when
            Gene gene = MAPPER.readValue(json, Gene.class);

            // then
            assertThat(gene.symbol).isEqualTo("TP53");
            assertThat(gene.chrId).isNull();
            assertThat(gene.chrName).isNull();
        }

        @Test
        @DisplayName("should populate all fields when multiple independent wrapper groups present")
        public void testMultipleWrappersRoundTrip() throws Exception
        {
            // setup - two independent wrapper groups w1 and w2
            String json = "{\"w1\":{\"a\":\"A\",\"b\":\"B\"},\"w2\":{\"c\":\"C\",\"d\":\"D\"}}";

            // when
            MultiWrapperBean bean = MAPPER.readValue(json, MultiWrapperBean.class);

            // then - both wrapper groups fully populated
            assertThat(bean.w1a).isEqualTo("A");
            assertThat(bean.w1b).isEqualTo("B");
            assertThat(bean.w2c).isEqualTo("C");
            assertThat(bean.w2d).isEqualTo("D");
        }
    }

    @Nested
    @DisplayName("round-trip serialization and deserialization")
    class RoundTripTests {

        @Test
        @DisplayName("should round-trip a Gene through serialize then deserialize")
        void trueRoundTrip() throws Exception {
            // setup
            Gene original = new Gene();
            original.symbol = "TP53";
            original.chrId = "17";
            original.chrName = "chr17";

            // when
            String json = MAPPER.writeValueAsString(original);
            Gene roundTripped = MAPPER.readValue(json, Gene.class);

            // then
            assertThat(roundTripped.symbol).isEqualTo(original.symbol);
            assertThat(roundTripped.chrId).isEqualTo(original.chrId);
            assertThat(roundTripped.chrName).isEqualTo(original.chrName);
        }

        @Test
        @DisplayName("should round-trip a MultiWrapperBean through serialize then deserialize")
        void trueRoundTripMultipleWrappers() throws Exception {
            // setup
            MultiWrapperBean original = new MultiWrapperBean();
            original.w1a = "A";
            original.w1b = "B";
            original.w2c = "C";
            original.w2d = "D";

            // when
            String json = MAPPER.writeValueAsString(original);
            MultiWrapperBean roundTripped = MAPPER.readValue(json, MultiWrapperBean.class);

            // then
            assertThat(roundTripped.w1a).isEqualTo(original.w1a);
            assertThat(roundTripped.w1b).isEqualTo(original.w1b);
            assertThat(roundTripped.w2c).isEqualTo(original.w2c);
            assertThat(roundTripped.w2d).isEqualTo(original.w2d);
        }
    }

    @Nested
    @DisplayName("unknown inner properties")
    class UnknownInnerPropertyTests {

        @Test
        @DisplayName("should collect unknown inner properties when @JsonAnySetter present")
        public void testUnknownInnerWithAnySetter() throws Exception
        {
            // setup
            String json = "{\"symbol\":\"TP53\",\"chr\":{\"id\":\"17\",\"extra\":\"val\"}}";

            // when
            BeanWithAnySetter bean = MAPPER.readValue(json, BeanWithAnySetter.class);

            // then
            assertThat(bean.symbol).isEqualTo("TP53");
            assertThat(bean.chrId).isEqualTo("17");
            assertThat(bean.extra.get("extra")).isEqualTo("val");
        }

        @Test
        @DisplayName("should ignore unknown inner properties when FAIL_ON_UNKNOWN_PROPERTIES disabled")
        public void testUnknownInnerWithoutAnySetter() throws Exception
        {
            // setup - Gene has no @JsonAnySetter
            String json = "{\"symbol\":\"TP53\",\"chr\":{\"id\":\"17\",\"extra\":\"val\"}}";

            // when/then - should NOT fail by default (FAIL_ON_UNKNOWN_PROPERTIES is false)
            Gene gene = MAPPER.readValue(json, Gene.class);
            assertThat(gene.symbol).isEqualTo("TP53");
            assertThat(gene.chrId).isEqualTo("17");
        }

        @Test
        @DisplayName("should throw DatabindException when unknown inner property found and FAIL_ON_UNKNOWN_PROPERTIES enabled")
        public void testUnknownInnerFailsWhenConfigured() throws Exception
        {
            // setup
            ObjectMapper strictMapper = JsonMapper.builder()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
            String json = "{\"symbol\":\"TP53\",\"chr\":{\"id\":\"17\",\"extra\":\"val\"}}";

            // when/then - should fail with FAIL_ON_UNKNOWN_PROPERTIES enabled
            assertThatThrownBy(() -> strictMapper.readValue(json, Gene.class))
                .isInstanceOf(DatabindException.class)
                .satisfies(ex -> assertThat(ex.getMessage())
                    .matches(msg -> msg.contains("extra") || msg.contains("Unrecognized")));
        }

        @Test
        @DisplayName("should correctly parse known property after unknown scalar inner property when custom handler returns true")
        public void testKnownPropertyAfterUnknownScalarWithHandler() throws Exception
        {
            // Verifies that skipChildren() is defensive: after a custom handler returns true
            // for an unknown scalar property, the loop should still advance to the next token
            // and correctly parse the following known property.
            // setup - unknown "extra" comes before known "name"
            String json = "{\"symbol\":\"TP53\",\"chr\":{\"id\":\"17\",\"extra\":\"val\",\"name\":\"chr17\"}}";
            ObjectMapper mapper = JsonMapper.builder()
                .addHandler(new DeserializationProblemHandler() {
                    @Override
                    public boolean handleUnknownProperty(DeserializationContext ctxt,
                            JsonParser p, ValueDeserializer<?> deser,
                            Object bean, String propName) throws JacksonException {
                        // Handler returns true without calling skipChildren() —
                        // the framework must defensively skip for us.
                        // For a scalar, the value token has already been consumed by
                        // ctxt.handleUnknownProperty internal path, so skipChildren() is a no-op.
                        return true;
                    }
                })
                .build();

            // when
            Gene gene = mapper.readValue(json, Gene.class);

            // then - "name" after the unknown property must still be read
            assertThat(gene.symbol).isEqualTo("TP53");
            assertThat(gene.chrId).isEqualTo("17");
            assertThat(gene.chrName).isEqualTo("chr17");
        }

        @Test
        @DisplayName("should correctly parse known property after unknown object inner property when custom handler returns true")
        public void testKnownPropertyAfterUnknownObjectWithHandler() throws Exception
        {
            // This is the key regression test: the unknown value is a nested object.
            // If p.skipChildren() is NOT called after the handler returns true, the parser
            // will be left inside the nested object, and the outer loop's nextToken() will
            // advance to a token inside the nested object rather than the next sibling property.
            // setup - unknown "extra" is a JSON object, followed by known "name"
            String json = "{\"symbol\":\"TP53\",\"chr\":{\"id\":\"17\",\"extra\":{\"a\":1},\"name\":\"chr17\"}}";
            ObjectMapper mapper = JsonMapper.builder()
                .addHandler(new DeserializationProblemHandler() {
                    @Override
                    public boolean handleUnknownProperty(DeserializationContext ctxt,
                            JsonParser p, ValueDeserializer<?> deser,
                            Object bean, String propName) throws JacksonException {
                        // Handler claims it handled the property but does NOT call skipChildren().
                        // The framework's defensive skipChildren() must consume the nested object.
                        return true;
                    }
                })
                .build();

            // when
            Gene gene = mapper.readValue(json, Gene.class);

            // then - "name" after the unknown nested-object property must still be read
            assertThat(gene.symbol).isEqualTo("TP53");
            assertThat(gene.chrId).isEqualTo("17");
            assertThat(gene.chrName).isEqualTo("chr17");
        }
    }
}
