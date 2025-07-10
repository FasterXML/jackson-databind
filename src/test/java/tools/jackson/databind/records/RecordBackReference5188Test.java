package tools.jackson.databind.records;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.fail;

// [databind#5188] JsonManagedReference/JsonBackReference exception for records #5188
// (cannot workd 
public class RecordBackReference5188Test
    extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testRecordDeserializationFail() throws Exception {
        final String json = "{\"children\":[{}]}";

        try {
            MAPPER.readValue(json, Parent.class);
            fail("Should not pass");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Cannot add back-reference to a `java.lang.Record` type");
            verifyException(e, "Invalid type definition for ");
            verifyException(e, "(property 'parent')");
        }
    }

    record Child(@JsonBackReference Parent parent) {}

    record Parent(@JsonManagedReference List<Child> children) {}

}
