package tools.jackson.databind.records;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class RecordJsonValue3063Test extends DatabindTestUtil
{
    // [databind#3063]
    record GetLocations3063(@JsonValue Map<String, String> nameToLocation)
    {
         @JsonCreator
         public GetLocations3063(Map<String, String> nameToLocation)
         {
              this.nameToLocation = nameToLocation;
         }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#3063]
    @Test
    public void testRecordWithJsonValue3063() throws Exception
    {
        Map<String, String> locations = Collections.singletonMap("a", "locationA");
        String json = MAPPER.writeValueAsString(new GetLocations3063(locations));

        assertNotNull(json);
    }
}
