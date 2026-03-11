package com.fasterxml.jackson.databind.records;

import java.util.Optional;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class RecordWithJsonIgnoredMethod5184Test
    extends DatabindTestUtil
{
    record TestData5184(@JsonProperty("test_property") String value) {
        @JsonIgnore
        public Optional<String> getValue() {
            return Optional.ofNullable(value);
        }
    }

    record TestData5184Alternate(@JsonProperty("test_property") String value) {
        @JsonIgnore
        public Optional<String> optionalValue() {
            return Optional.ofNullable(value);
        }
    }

    static final class TestData5184Class {
        private final String value;

        public TestData5184Class(@JsonProperty("test_property") String value) {
            this.value = value;
        }

        @JsonIgnore
        public Optional<String> getValue() {
            return Optional.ofNullable(value);
        }
    }

    // From original issue - record with @JsonProperty on parameter and @JsonIgnore on getter
    record Foo5184Record(@JsonProperty("bar") String bar) {
        @JsonIgnore
        public Object getBar() {
            return 123; // Returns different type/value
        }
    }

    private static final ObjectMapper MAPPER = newJsonMapper();

    @Test
    void should_deserialize_json_to_test_data() throws Exception {
        String json = """
                {"test_property":"test value"}
                """;

        var testData = MAPPER.readValue(json, TestData5184.class);

        assertThat(testData.value()).isEqualTo("test value");
    }

    @Test
    void should_deserialize_json_to_test_data_class() throws Exception {
        String json = """
                {"test_property":"test value"}
                """;

        var testData = MAPPER.readValue(json, TestData5184Class.class);

        assertThat(testData.getValue()).contains("test value");
    }

    @Test
    void should_deserialize_json_to_test_data_alternate() throws Exception {
        String json = """
                {"test_property":"test value"}
                """;

        var testData = MAPPER.readValue(json, TestData5184Alternate.class);

        assertThat(testData.value()).isEqualTo("test value");
    }

    @Test
    void should_deserialize_when_ignore_unknown_wrong_json_model_to_test_data() throws Exception {
        String json = """
                {"value":"test value"}
                """;

        TestData5184 testData = jsonMapperBuilder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build()
                .readValue(json, TestData5184.class);

        assertThat(testData.value()).isNull();
    }

    @Test
    void should_not_deserialize_wrong_json_model_to_test_data() throws Exception {
        String json = """
                {}
                """;

        TestData5184 testData = MAPPER.readValue(json, TestData5184.class);

        assertThat(testData.value()).isNull();
    }


    /**
     * Test round-trip serialization/deserialization consistency.
     * The @JsonIgnore on getBar() should not affect deserialization of the "bar" property.
     */
    @Test
    public void testRoundTripConsistency() throws Exception {
        final Foo5184Record obj = new Foo5184Record("foo");

        // Serialize
        final String json1 = MAPPER.writeValueAsString(obj);
        Assertions.assertThat(json1).isEqualTo("{\"bar\":\"foo\"}");

        // Deserialize
        final Foo5184Record deserialized = MAPPER.readValue(json1, Foo5184Record.class);
        assertThat(deserialized.bar()).isEqualTo("foo");

        // Serialize again - should be same as first serialization
        final String json2 = MAPPER.writeValueAsString(deserialized);
        Assertions.assertThat(json2).isEqualTo("{\"bar\":\"foo\"}");

        // Round-trip should preserve the value
        Assertions.assertThat(json1).isEqualTo(json2);
    }

    /**
     * Test that deserialization correctly populates the field
     * even though there's a @JsonIgnore on the getter.
     */
    @Test
    public void testDeserializationPopulatesField() throws Exception {
        final String json = "{\"bar\":\"test-value\"}";
        final Foo5184Record result = MAPPER.readValue(json, Foo5184Record.class);

        assertThat(result.bar()).isEqualTo("test-value");
    }

    /**
     * Test that serialization uses the field value, not the getter.
     */
    @Test
    public void testSerializationUsesFieldNotGetter() throws Exception {
        final Foo5184Record obj = new Foo5184Record("field-value");
        final String json = MAPPER.writeValueAsString(obj);

        // Should serialize the field value "field-value", not what getBar() returns (123)
        Assertions.assertThat(json).isEqualTo("{\"bar\":\"field-value\"}");
    }

}
