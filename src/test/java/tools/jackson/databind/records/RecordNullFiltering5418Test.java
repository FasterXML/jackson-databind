package tools.jackson.databind.records;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test for [databind#5418]: NON_ABSENT/NON_NULL inclusion not working
 * with Records when configured via changeDefaultPropertyInclusion()
 */
public class RecordNullFiltering5418Test extends DatabindTestUtil
{
    record TestRecord(String subject, String body) {}

    @Test
    public void testNonAbsentInclusionViaDefaultConfig() throws Exception
    {
        // Configure mapper to exclude absent values (which includes nulls)
        ObjectMapper mapper = JsonMapper.builder()
            .changeDefaultPropertyInclusion(
                incl -> JsonInclude.Value.construct(
                    JsonInclude.Include.NON_ABSENT,
                    JsonInclude.Include.NON_ABSENT))
            .build();

        // Should exclude null fields
        String json = mapper.writeValueAsString(new TestRecord("test subject", null));
        assertEquals(a2q("{'subject':'test subject'}"), json);

        // Both null
        json = mapper.writeValueAsString(new TestRecord(null, null));
        assertEquals("{}", json);

        // Both present
        json = mapper.writeValueAsString(new TestRecord("test subject", "test body"));
        assertEquals(a2q("{'subject':'test subject','body':'test body'}"), json);
    }

    @Test
    public void testNonNullInclusionViaDefaultConfig() throws Exception
    {
        // Configure mapper to exclude null values
        ObjectMapper mapper = JsonMapper.builder()
            .changeDefaultPropertyInclusion(
                incl -> JsonInclude.Value.construct(
                    JsonInclude.Include.NON_NULL,
                    JsonInclude.Include.NON_NULL))
            .build();

        // Should exclude null fields
        String json = mapper.writeValueAsString(new TestRecord("test subject", null));
        assertEquals(a2q("{'subject':'test subject'}"), json);

        // Both null
        json = mapper.writeValueAsString(new TestRecord(null, null));
        assertEquals("{}", json);

        // Both present
        json = mapper.writeValueAsString(new TestRecord("test subject", "test body"));
        assertEquals(a2q("{'subject':'test subject','body':'test body'}"), json);
    }
}
