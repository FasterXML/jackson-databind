package com.fasterxml.jackson.databind.tofix;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonIgnoreOnRecordMethod5184Test
    extends DatabindTestUtil
{

    private static final ObjectMapper MAPPER = newJsonMapper();

    @Test
    void should_deserialize_json_to_test_data() throws Exception {
        String json = """
                {"test_property":"test value"}
                """;

        var testData = MAPPER.readValue(json, TestData.class);

        assertThat(testData.value()).isEqualTo("test value");
    }

    @Test
    void should_deserialize_json_to_test_data_class() throws Exception {
        String json = """
                {"test_property":"test value"}
                """;

        var testData = MAPPER.readValue(json, TestDataClass.class);

        assertThat(testData.getValue()).contains("test value");
    }

    @Test
    void should_deserialize_json_to_test_data_alternate() throws Exception {
        String json = """
                {"test_property":"test value"}
                """;

        var testData = MAPPER.readValue(json, TestDataAlternate.class);

        assertThat(testData.value()).isEqualTo("test value");
    }

    @Test
    void should_not_deserialize_wrong_json_model_to_test_data() throws Exception {
        String json = """
                {"value":"test value"}
                """;

        TestData testData = MAPPER.readValue(json, TestData.class);

        assertThat(testData.value()).isNull();
    }

    public record TestData(
            @JsonProperty("test_property")
            String value) {

        @JsonIgnore
        public Optional<String> getValue() {
            return Optional.ofNullable(value);
        }
    }

    public record TestDataAlternate(
            @JsonProperty("test_property")
            String value) {

        @JsonIgnore
        public Optional<String> optionalValue() {
            return Optional.ofNullable(value);
        }
    }

    public static final class TestDataClass {
        private final String value;

        public TestDataClass(
                @JsonProperty("test_property")
                String value) {
            this.value = value;
        }

        @JsonIgnore
        public Optional<String> getValue() {
            return Optional.ofNullable(value);
        }

    }

}
