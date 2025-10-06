package tools.jackson.databind.records;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#5335] When deserializing records, java.util.Optional parameters should be not be
// deserialized as null (Optional.empty() should be used instead)
public class RecordWithOptionalParamTest
    extends DatabindTestUtil
{
    record RecordWithOptionalParam(String name, Optional<String> optional) { }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    void serialize() throws Exception
    {
        RecordWithOptionalParam input = new RecordWithOptionalParam("v1", Optional.of("v2"));
        String json = MAPPER.writeValueAsString(input);
        String expected = a2q("{'name':'v1','optional':'v2'}");
        assertEquals(expected, json);
    }

    @Test
    void serializeEmpty() throws Exception
    {
        RecordWithOptionalParam input = new RecordWithOptionalParam("v1", Optional.empty());
        String json = MAPPER.writeValueAsString(input);
        String expected = a2q("{'name':'v1','optional':null}");
        assertEquals(expected, json);
    }

    @Test
    void deserializeNonEmpty() throws Exception
    {
        String json = a2q("{'name':'v1','optional':'v2'}");
        RecordWithOptionalParam output = MAPPER.readValue(json, RecordWithOptionalParam.class);
        assertEquals("v1", output.name());
        assertEquals(Optional.of("v2"), output.optional());
    }

    @Test
    void deserializeExplicitNull() throws Exception
    {
        String json = a2q("{'name':'v1','optional':null}");
        RecordWithOptionalParam output = MAPPER.readValue(json, RecordWithOptionalParam.class);
        assertEquals("v1", output.name());
        assertNotNull(output.optional());
        assertEquals(Optional.empty(), output.optional());
    }

    @JacksonTestFailureExpected // [databind#5335]
    @Test
    void deserializeMissing() throws Exception
    {
        String json = a2q("{'name':'v1'}");
        RecordWithOptionalParam output = MAPPER.readValue(json, RecordWithOptionalParam.class);
        assertEquals("v1", output.name());
        assertNotNull(output.optional());
        assertEquals(Optional.empty(), output.optional());
    }

    @JacksonTestFailureExpected // [databind#5335]
    @Test
    void deserializeIssue5335Config() throws Exception
    {
        String json = a2q("{'name':'v1'}");
        JsonMapper mapper = JsonMapper.builder()
            .changeDefaultPropertyInclusion(
                    v -> JsonInclude.Value.construct(JsonInclude.Include.NON_ABSENT, JsonInclude.Include.ALWAYS))
            .build();
        RecordWithOptionalParam output = mapper.readValue(json, RecordWithOptionalParam.class);
        assertEquals("v1", output.name());
        assertNotNull(output.optional());
        assertEquals(Optional.empty(), output.optional());
    }

}
