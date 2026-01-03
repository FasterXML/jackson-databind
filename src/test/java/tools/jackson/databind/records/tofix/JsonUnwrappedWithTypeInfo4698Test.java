package tools.jackson.databind.records.tofix;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.*;

// [databind#4698] @JsonUnwrapped and @JsonTypeInfo do not work together
// https://github.com/FasterXML/jackson-databind/issues/4698
public class JsonUnwrappedWithTypeInfo4698Test
    extends DatabindTestUtil
{
    public record Inner4698(String foo, int bar) {}

    public record Versioned<T>(
        int version,
        @JsonUnwrapped
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
        T item
    ) {}

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .disable(SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS)
            .build();

    @JacksonTestFailureExpected
    @Test
    public void testSerializationWithTypeInfo() throws Exception {
        Versioned<Inner4698> versioned = new Versioned<>(1, new Inner4698("foo", 123));

        String json = MAPPER.writeValueAsString(versioned);

        // Expected: {"version":1,"@class":"...Inner4698","foo":"foo","bar":123}
        assertTrue(json.contains("\"@class\""));
        assertTrue(json.contains("Inner4698"));
    }
}
