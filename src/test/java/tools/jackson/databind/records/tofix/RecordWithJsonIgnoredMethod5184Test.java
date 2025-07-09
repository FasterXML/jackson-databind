package tools.jackson.databind.records.tofix;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

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

    private static final ObjectMapper MAPPER = newJsonMapper();

    @JacksonTestFailureExpected
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
    void should_not_deserialize_wrong_json_model_to_test_data() throws Exception {
        String json = """
                {"value":"test value"}
                """;

        TestData5184 testData = MAPPER.readValue(json, TestData5184.class);

        assertThat(testData.value()).isNull();
    }
}
