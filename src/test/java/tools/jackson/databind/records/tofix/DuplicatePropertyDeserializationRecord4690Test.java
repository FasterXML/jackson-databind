package tools.jackson.databind.records.tofix;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// [databind#4690] InvalidDefinitionException "No fallback setter/field defined for creator property"
// when deserializing JSON with duplicated property to single-property Record
public class DuplicatePropertyDeserializationRecord4690Test
        extends DatabindTestUtil
{
    record MyRecord(String first) { }

    private final ObjectMapper mapper = newJsonMapper();

    @JacksonTestFailureExpected
    @Test
    void testDuplicatePropertyDeserialization() throws Exception {
        final String json = a2q("{'first':'value','first':'value2'}");

        MyRecord result = mapper.readValue(json, MyRecord.class);

        assertNotNull(result);
        assertEquals("value2", result.first());
    }

}
