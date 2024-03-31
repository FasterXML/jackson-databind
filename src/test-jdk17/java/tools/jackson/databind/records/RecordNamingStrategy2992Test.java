package tools.jackson.databind.records;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RecordNamingStrategy2992Test extends DatabindTestUtil
{
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record Record2992(String myId, String myValue) {}

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#2992]
    @Test
    public void testRecordRenaming2992() throws Exception
    {
        Record2992 src = new Record2992("id", "value");
        String json = MAPPER.writeValueAsString(src);
        assertEquals(a2q("{'my_id':'id','my_value':'value'}"), json);
        Record2992 after = MAPPER.readValue(json, Record2992.class);
        assertEquals(src.myId(), after.myId());
        assertEquals(src.myValue(), after.myValue());
    }
}
