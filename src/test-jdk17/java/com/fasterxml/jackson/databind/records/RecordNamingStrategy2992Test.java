package com.fasterxml.jackson.databind.records;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import org.junit.jupiter.api.Test;

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
