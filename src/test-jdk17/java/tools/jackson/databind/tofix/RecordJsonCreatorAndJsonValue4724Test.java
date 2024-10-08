package tools.jackson.databind.tofix;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

// [databind#4724] Deserialization behavior change with Java Records, JsonCreator and JsonValue between 2.17.2 => 2.18.0
public class RecordJsonCreatorAndJsonValue4724Test
    extends DatabindTestUtil
{

    public record Something(String value) {
        public Something {
            if (value == null || value.isEmpty()) {
                throw new IllegalArgumentException("Value cannot be null or empty");
            }
        }

        @JsonCreator
        public static Something of(String value) {
            if (value.isEmpty()) {
                return null;
            }
            return new Something(value);
        }

        @Override
        @JsonValue
        public String toString() {
            return value;
        }
    }

    @JacksonTestFailureExpected
    @Test
    void deserialization() throws Exception {
        newJsonMapper().readValue("\"\"", Something.class);
    }

}
