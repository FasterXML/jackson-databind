package tools.jackson.databind.records;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.BaseMapTest;
import tools.jackson.databind.ObjectMapper;

public class RecordJsonValue3063Test extends BaseMapTest
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
    public void testRecordWithJsonValue3063() throws Exception
    {
        Map<String, String> locations = Collections.singletonMap("a", "locationA");
        String json = MAPPER.writeValueAsString(new GetLocations3063(locations));

        assertNotNull(json);
    }
}
