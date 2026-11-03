package tools.jackson.databind.records;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.NoCheckSubTypeValidator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// [databind#3786]: Deserialization of generic container (Record type) using
// EXTERNAL_PROPERTY fails for boxed built-ins because type information is missing
public class RecordExternalPropertyGeneric3786Test extends DatabindTestUtil
{
    record Container3786<T>(
        int id,
        @JsonTypeInfo(
            use = JsonTypeInfo.Id.CLASS,
            include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
            property = "type"
        )
        T value
    ) { }

    record MyObject3786(
        String foo,
        String bar
    ) { }

    // Need to allow Object as base type for CLASS-based type id
    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .polymorphicTypeValidator(NoCheckSubTypeValidator.instance)
            .build();

    // This case works: custom object has type info written
    @Test
    public void testCustomObjectRoundTrip() {
        Container3786<MyObject3786> myContainer = new Container3786<>(1, new MyObject3786("foo", "bar"));
        String json = MAPPER.writeValueAsString(myContainer);

        // Should include type property for custom object
        Container3786<?> result = MAPPER.readValue(json, new TypeReference<Container3786<?>>() { });
        assertNotNull(result);
        assertEquals(1, result.id());
        assertNotNull(result.value());
    }

    // This case fails: String value has no type info written,
    // but deserialization requires it
    @Test
    public void testStringValueRoundTrip() {
        Container3786<String> strContainer = new Container3786<>(1, "Hello");
        String json = MAPPER.writeValueAsString(strContainer);

        // JSON will be {"id":1,"value":"Hello"} -- no "type" property
        Container3786<?> result = MAPPER.readValue(json, new TypeReference<Container3786<?>>() { });
        assertNotNull(result);
        assertEquals(1, result.id());
        assertEquals("Hello", result.value());
    }

    // Also test with Integer value
    @Test
    public void testIntegerValueRoundTrip() {
        Container3786<Integer> intContainer = new Container3786<>(1, 42);
        String json = MAPPER.writeValueAsString(intContainer);

        Container3786<?> result = MAPPER.readValue(json, new TypeReference<Container3786<?>>() { });
        assertNotNull(result);
        assertEquals(1, result.id());
        assertEquals(42, result.value());
    }

    // Also test with Boolean value
    @Test
    public void testBooleanValueRoundTrip() {
        Container3786<Boolean> boolContainer = new Container3786<>(1, true);
        String json = MAPPER.writeValueAsString(boolContainer);

        Container3786<?> result = MAPPER.readValue(json, new TypeReference<Container3786<?>>() { });
        assertNotNull(result);
        assertEquals(1, result.id());
        assertEquals(true, result.value());
    }
}
