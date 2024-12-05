package tools.jackson.databind.records;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RecordWithJsonNaming3102Test extends DatabindTestUtil
{
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
    /* Test methods, Record type introspection
    /**********************************************************************
     */

    // [databind#3102]
    @Test
    public void testDeserializeWithJsonNaming() throws Exception
    {
        final ObjectReader r = MAPPER.readerFor(SnakeRecord.class);
        // First, regular case
        SnakeRecord value = r.readValue(a2q(
 "{'id':123,'to_snake_case':'snakey'}"));
        assertEquals(123, value.id);
        assertEquals("snakey", value.toSnakeCase);
    }
}
