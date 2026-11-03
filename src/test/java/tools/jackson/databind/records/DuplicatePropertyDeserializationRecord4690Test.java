package tools.jackson.databind.records;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// [databind#4690] InvalidDefinitionException "No fallback setter/field defined for creator property"
// when deserializing JSON with duplicated property to single-property Record
public class DuplicatePropertyDeserializationRecord4690Test
        extends DatabindTestUtil
{
    record MyRecord(String first) { }

    static class MyPojo {
        private String first;

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        MyPojo(String first) { this.first = first; }

        public void setFirst(String first) {
            this.first = first;
        }

        public String getFirst() {
            return first;
        }
    }

    private final ObjectMapper mapper = newJsonMapper();

    @Test
    void testDuplicatePropertyDeserialization() throws Exception {
        final String json = a2q("{'first':'value','first':'value2'}");

        MyRecord result = mapper.readValue(json, MyRecord.class);

        assertNotNull(result);
        assertEquals("value2", result.first());
    }

    @Test
    void testDuplicatePropertyDeserialization2() throws Exception {
        final String json = a2q("{'first':'value','second':'test1','first':'value2'}");

        MyRecord result = mapper.readValue(json, MyRecord.class);
        assertEquals("value2", result.first());
    }

    @Test
    void testDuplicatePropertyClassDeserialization() throws Exception {
        final String json = a2q("{'first':'value','second':'test1','first':'value2'}");

        MyPojo result = mapper.readValue(json, MyPojo.class);
        assertEquals("value2", result.getFirst());
    }
}
