package com.fasterxml.jackson.databind.records;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.exc.InputCoercionException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for issue #5184: {@code @JsonIgnore} on non-accessor methods should not
 * affect deserialization of record components.
 */
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

    // [databind#5184]: Record component should deserialize correctly even when
    // there's a non-accessor method with @JsonIgnore
    @Test
    void testRecordWithIgnoredNonAccessorMethod() throws Exception {
        String json = """
                {"test_property":"test value"}
                """;

        var testData = MAPPER.readValue(json, TestData5184.class);

        assertThat(testData.value()).isEqualTo("test value");
    }

    // Regular class behavior should be unchanged: @JsonIgnore on getter only affects serialization
    @Test
    void testRegularClassWithIgnoredGetter() throws Exception {
        String json = """
                {"test_property":"test value"}
                """;

        var testData = MAPPER.readValue(json, TestData5184Class.class);

        assertThat(testData.getValue()).contains("test value");
    }

    // Alternative naming (optionalValue vs getValue) should work without issues
    @Test
    void testRecordWithDifferentMethodName() throws Exception {
        String json = """
                {"test_property":"test value"}
                """;

        var testData = MAPPER.readValue(json, TestData5184Alternate.class);

        assertThat(testData.value()).isEqualTo("test value");
    }

    // When JSON property name doesn't match record component, should fail (not silently ignore)
    @Test
    void testUnrecognizedPropertyStillFails() throws Exception {
        String json = """
                {"value":"test value"}
                """;

        // With the fix, "value" is not recognized as a property (because getValue() is not
        // polluting the record component), so this should throw UnrecognizedPropertyException
        assertThrows(UnrecognizedPropertyException.class, () -> {
            MAPPER.readValue(json, TestData5184.class);
        });
    }
}
