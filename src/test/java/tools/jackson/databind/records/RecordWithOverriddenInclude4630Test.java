package tools.jackson.databind.records;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RecordWithOverriddenInclude4630Test extends DatabindTestUtil
{
    record Id2Name(int id, String name) {
    }

    record RecordWithJsonIncludeProperties(@JsonIncludeProperties("id") Id2Name child) {
        @Override
        public Id2Name child() {
            return child;
        }
    }

    record RecordWithJsonIgnoreProperties(@JsonIgnoreProperties("name") Id2Name child) {
        @Override
        public Id2Name child() {
            return child;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#4630]
    @Test
    public void testSerializeJsonIncludeProperties() throws Exception {
        String json = MAPPER.writeValueAsString(new RecordWithJsonIncludeProperties(new Id2Name(123, "Bob")));
        assertEquals(a2q("{'child':{'id':123}}"), json);
    }

    // [databind#4630]
    @Test
    public void testSerializeJsonIgnoreProperties() throws Exception {
        String json = MAPPER.writeValueAsString(new RecordWithJsonIgnoreProperties(new Id2Name(123, "Bob")));
        assertEquals(a2q("{'child':{'id':123}}"), json);
    }
}
