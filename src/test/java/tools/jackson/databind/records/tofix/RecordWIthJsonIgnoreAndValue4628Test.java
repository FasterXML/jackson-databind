package tools.jackson.databind.records.tofix;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#4628] @JsonIgnore is ignored with read access
public class RecordWIthJsonIgnoreAndValue4628Test
    extends DatabindTestUtil
{
    record RecordWithIgnoreJsonProperty(int id, @JsonIgnore @JsonProperty("name") String name) {
    }

    record RecordWithIgnoreJsonPropertyDifferentName(int id, @JsonIgnore @JsonProperty("name2") String name) {
    }

    private final ObjectMapper MAPPER = newJsonMapper();


    // passing normally given different name is used
    @Test
    public void testDeserializeJsonIgnoreRecordWithDifferentName() throws Exception {
        RecordWithIgnoreJsonPropertyDifferentName value = MAPPER.readValue("{\"id\":123,\"name\":\"Bob\"}", RecordWithIgnoreJsonPropertyDifferentName.class);
        assertEquals(new RecordWithIgnoreJsonPropertyDifferentName(123, null), value);
    }

    @Test
    @JacksonTestFailureExpected
    public void testDeserializeJsonIgnoreAndJsonPropertyRecord() throws Exception {
        RecordWithIgnoreJsonProperty value = MAPPER.readValue("{\"id\":123,\"name\":\"Bob\"}", RecordWithIgnoreJsonProperty.class);
        assertEquals(new RecordWithIgnoreJsonProperty(123, null), value); // should be null, actual "bob"
    }
}
