package tools.jackson.databind.records;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RecordNamingStrategyTest extends DatabindTestUtil
{
    // [databind#2992]
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record Record2992(String myId, String myValue) {}

    // [databind#3102]
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SnakeRecord(int id, String toSnakeCase) {
        @JsonCreator
        public SnakeRecord(int id, String toSnakeCase) {
            this.id = id;
            this.toSnakeCase = toSnakeCase;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    /*
    /**********************************************************************
    /* Test methods, @JsonNaming on records [databind#2992]
    /**********************************************************************
     */

    // [databind#2992]
    @Test
    public void testRecordNaming2992() throws Exception
    {
        Record2992 src = new Record2992("id", "value");
        String json = MAPPER.writeValueAsString(src);
        assertEquals(a2q("{'my_id':'id','my_value':'value'}"), json);
        Record2992 after = MAPPER.readValue(json, Record2992.class);
        assertEquals(src.myId(), after.myId());
        assertEquals(src.myValue(), after.myValue());
    }

    /*
    /**********************************************************************
    /* Test methods, @JsonNaming with @JsonCreator [databind#3102]
    /**********************************************************************
     */

    // [databind#3102]
    @Test
    public void testDeserializeWithJsonNaming3102() throws Exception
    {
        final ObjectReader r = MAPPER.readerFor(SnakeRecord.class);
        SnakeRecord value = r.readValue(a2q(
"{'id':123,'to_snake_case':'snakey'}"));
        assertEquals(123, value.id);
        assertEquals("snakey", value.toSnakeCase);
    }
}
