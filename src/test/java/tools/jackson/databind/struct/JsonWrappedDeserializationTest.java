package tools.jackson.databind.struct;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import com.fasterxml.jackson.annotation.JsonWrapped;
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

    static class City {
        public String name;
        public int population;
        public City() { }
    }

    static class BeanWithPojoWrapped {
        @JsonWrapped("w")
        public City city;
        public BeanWithPojoWrapped() { }
    }

    static class BeanWithListWrapped {
        @JsonWrapped("w")
        public java.util.List<String> tags;
        public BeanWithListWrapped() { }
    }

    static class BeanWithMapWrapped {
        @JsonWrapped("w")
        public java.util.Map<String, Integer> counts;
        public BeanWithMapWrapped() { }
    }

    static class BeanWithArrayWrapped {
        @JsonWrapped("w")
        public String[] items;
        public BeanWithArrayWrapped() { }
    }

    static class BeanWithMixedWrapper {
        @JsonWrapped("w")
        public String label;

        @JsonWrapped("w")
        public City city;

        public BeanWithMixedWrapper() { }
    }

    static class InnerWithWrapped {
        @JsonWrapped("sub")
        public String x;

        @JsonWrapped("sub")
        public String y;

        public InnerWithWrapped() { }
    }

    static class BeanWithNestedWrapping {
        @JsonWrapped("outer")
        public InnerWithWrapped inner;
        public BeanWithNestedWrapping() { }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

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
        @DisplayName("should NOT route unknown inner properties to outer @JsonAnySetter")
        public void testUnknownInnerWithAnySetter() throws Exception
        {
            // setup
            String json = "{\"symbol\":\"TP53\",\"chr\":{\"id\":\"17\",\"extra\":\"val\"}}";

            // when
            BeanWithAnySetter bean = MAPPER.readValue(json, BeanWithAnySetter.class);

            // then: unknown inner props are ignored (not sent to outer anySetter)
            assertThat(bean.symbol).isEqualTo("TP53");
            assertThat(bean.chrId).isEqualTo("17");
            assertThat(bean.extra).isEmpty();
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

    @Nested
    @DisplayName("non-scalar deserialization tests")
    class NonScalarDeserializationTests {

        @Test
        @DisplayName("should deserialize POJO field from wrapper object")
        void pojoInsideWrapper() throws Exception {
            String json = "{\"w\":{\"city\":{\"name\":\"NYC\",\"population\":8000000}}}";

            BeanWithPojoWrapped bean = MAPPER.readValue(json, BeanWithPojoWrapped.class);

            assertThat(bean.city).isNotNull();
            assertThat(bean.city.name).isEqualTo("NYC");
            assertThat(bean.city.population).isEqualTo(8_000_000);
        }

        @Test
        @DisplayName("should deserialize null POJO field from wrapper object")
        void nullPojoInsideWrapper() throws Exception {
            String json = "{\"w\":{\"city\":null}}";

            BeanWithPojoWrapped bean = MAPPER.readValue(json, BeanWithPojoWrapped.class);

            assertThat(bean.city).isNull();
        }

        @Test
        @DisplayName("should deserialize List field from wrapper object")
        void listInsideWrapper() throws Exception {
            String json = "{\"w\":{\"tags\":[\"java\",\"jackson\"]}}";

            BeanWithListWrapped bean = MAPPER.readValue(json, BeanWithListWrapped.class);

            assertThat(bean.tags).containsExactly("java", "jackson");
        }

        @Test
        @DisplayName("should deserialize Map field from wrapper object")
        void mapInsideWrapper() throws Exception {
            String json = "{\"w\":{\"counts\":{\"a\":1,\"b\":2}}}";

            BeanWithMapWrapped bean = MAPPER.readValue(json, BeanWithMapWrapped.class);

            assertThat(bean.counts).containsEntry("a", 1).containsEntry("b", 2);
        }

        @Test
        @DisplayName("should deserialize array field from wrapper object")
        void arrayInsideWrapper() throws Exception {
            String json = "{\"w\":{\"items\":[\"x\",\"y\"]}}";

            BeanWithArrayWrapped bean = MAPPER.readValue(json, BeanWithArrayWrapped.class);

            assertThat(bean.items).containsExactly("x", "y");
        }

        @Test
        @DisplayName("should deserialize mixed scalar and POJO from same wrapper")
        void mixedScalarAndPojoSameWrapper() throws Exception {
            String json = "{\"w\":{\"label\":\"home\",\"city\":{\"name\":\"NYC\",\"population\":8000000}}}";

            BeanWithMixedWrapper bean = MAPPER.readValue(json, BeanWithMixedWrapper.class);

            assertThat(bean.label).isEqualTo("home");
            assertThat(bean.city).isNotNull();
            assertThat(bean.city.name).isEqualTo("NYC");
        }

        @Test
        @DisplayName("should round-trip POJO inside wrapper")
        void roundTripPojoInsideWrapper() throws Exception {
            String originalJson = "{\"w\":{\"city\":{\"name\":\"NYC\",\"population\":8000000}}}";
            BeanWithPojoWrapped bean = MAPPER.readValue(originalJson, BeanWithPojoWrapped.class);

            String roundTripped = MAPPER.writeValueAsString(bean);

            assertThat(roundTripped).isEqualTo(originalJson);
        }

        @Test
        @DisplayName("should round-trip List inside wrapper")
        void roundTripListInsideWrapper() throws Exception {
            String originalJson = "{\"w\":{\"tags\":[\"java\",\"jackson\"]}}";
            BeanWithListWrapped bean = MAPPER.readValue(originalJson, BeanWithListWrapped.class);

            String roundTripped = MAPPER.writeValueAsString(bean);

            assertThat(roundTripped).isEqualTo(originalJson);
        }

        @Test
        @DisplayName("should compose nested @JsonWrapped — POJO inside wrapper that itself has @JsonWrapped fields")
        void nestedWrappingRoundTrip() throws Exception {
            String originalJson = "{\"outer\":{\"inner\":{\"sub\":{\"x\":\"a\",\"y\":\"b\"}}}}";
            BeanWithNestedWrapping bean = MAPPER.readValue(originalJson, BeanWithNestedWrapping.class);

            assertThat(bean.inner).isNotNull();
            assertThat(bean.inner.x).isEqualTo("a");
            assertThat(bean.inner.y).isEqualTo("b");

            String roundTripped = MAPPER.writeValueAsString(bean);
            assertThat(roundTripped).isEqualTo(originalJson);
        }
    }

    /*
    /**********************************************************************
    /* enabled=false tests
    /**********************************************************************
     */

    static class GeneDisableWrapMixin {
        @JsonWrapped(value = "chr", enabled = false)
        @JsonProperty("id")
        public String chrId;

        @JsonWrapped(value = "chr", enabled = false)
        @JsonProperty("name")
        public String chrName;
    }

    @Nested
    @DisplayName("enabled=false tests")
    class EnabledFalseTests {

        @Test
        @DisplayName("should deserialize flat JSON when mix-in sets enabled=false on @JsonWrapped fields")
        void enabledFalseViaFieldMixinDisablesDeserialization() throws Exception {
            ObjectMapper mapper = jsonMapperBuilder()
                    .addMixIn(Gene.class, GeneDisableWrapMixin.class)
                    .build();
            String json = "{\"symbol\":\"BRCA1\",\"id\":\"17\",\"name\":\"chr17\"}";

            Gene g = mapper.readValue(json, Gene.class);

            assertThat(g.symbol).isEqualTo("BRCA1");
            assertThat(g.chrId).isEqualTo("17");
            assertThat(g.chrName).isEqualTo("chr17");
        }
    }
}
