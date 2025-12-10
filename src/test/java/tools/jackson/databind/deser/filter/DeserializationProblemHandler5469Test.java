package tools.jackson.databind.deser.filter;

import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.*;
import tools.jackson.databind.deser.DeserializationProblemHandler;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// For [databind#5469] Add callback to signal null for primitive in DeserializationProblemHandler
public class DeserializationProblemHandler5469Test
    extends DatabindTestUtil
{
    private static int hitCount = 0;
    static class Person5469 {
        public String id;
        public String name;
        public long age;
    }

    static class ProblemHandler5469 extends DeserializationProblemHandler
    {
        @Override
        public Object handleNullForPrimitives(DeserializationContext ctxt, ValueDeserializer<?> deser, String failureMsg) throws JacksonException {
            hitCount++;
            return 5469L;
        }
    }

    @Test
    public void testIssue5469()
            throws Exception
    {
        // Given
        assertEquals(0, hitCount);
        ObjectMapper mapper = JsonMapper.builder()
                .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .addHandler(new ProblemHandler5469())
                .build();

        // When
        Person5469 person = mapper.readValue(
            "{\"id\":  \"12ab\", \"name\": \"Bob\", " +
            // Input is NULL, but....
            "\"age\": null}", Person5469.class);

        // Then
        assertNotNull(person);
        assertEquals("12ab", person.id);
        assertEquals("Bob", person.name);
        // We get the MAGIC NUMBER as age
        assertEquals(5469L, person.age);
        // Sanity check, we hit the code path as we wanted
        assertEquals(1, hitCount);
    }
}
