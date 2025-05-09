package tools.jackson.databind.tofix;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;

import tools.jackson.databind.*;
import tools.jackson.databind.deser.DeserializationProblemHandler;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DeserializationProblemHandler4656Test extends DatabindTestUtil
{
    // For [databind#4656]
    static class Person4656 {
        public String id;
        public String name;
        public Long age;
    }

    static class ProblemHandler4656 extends DeserializationProblemHandler
    {
        protected static final String NUMBER_LONG_KEY = "$numberLong";
    
        @Override
        public Object handleUnexpectedToken(DeserializationContext ctxt, JavaType targetType,
                JsonToken t, JsonParser p, String failureMsg)
        {
            if (targetType.getRawClass().equals(Long.class) && t == JsonToken.START_OBJECT) {
                JsonNode tree = p.readValueAsTree();
                if (tree.get(NUMBER_LONG_KEY) != null) {
                    try {
                        return Long.parseLong(tree.get(NUMBER_LONG_KEY).asString());
                    } catch (NumberFormatException e) { }
                }
            }
            return NOT_HANDLED;
        }
    }

    // For [databind#4656]
    @JacksonTestFailureExpected
    @Test
    public void testIssue4656() throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .addHandler(new ProblemHandler4656())
                .build();
        final String json = "{\"id\":  \"12ab\", \"name\": \"Bob\", \"age\": {\"$numberLong\": \"10\"}}";
        Person4656 person = mapper.readValue(json, Person4656.class);
        assertNotNull(person);
        assertEquals("12ab", person.id);
        assertEquals("Bob", person.name);
        assertEquals(10L, person.age);
    }
}
