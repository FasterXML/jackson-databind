package tools.jackson.databind.deser.filter;

import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.*;
import tools.jackson.databind.deser.DeserializationProblemHandler;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// For [databind#5469] Add callback to signal null for primitive in DeserializationProblemHandler
public class DeserializationProblemHandler5469Test
    extends DatabindTestUtil
{

    static class Person5469 {
        public String id;
        public String name;
        public long age;
    }

    private static int hitCountFirst = 0;
    static class ProblemHandler5469 extends DeserializationProblemHandler
    {
        @Override
        public Object handleNullForPrimitives(DeserializationContext ctxt, Class<?> targetType,
                JsonParser p, ValueDeserializer<?> deser, String failureMsg
        ) throws JacksonException {
            hitCountFirst++;
            return 5469L;
        }
    }

    private static int hitCountSecond = 0;
    static class MoreProblemHandler5469 extends DeserializationProblemHandler
    {
        @Override
        public Object handleNullForPrimitives(DeserializationContext ctxt, Class<?> targetType,
                JsonParser p, ValueDeserializer<?> deser,  String failureMsg
        ) throws JacksonException {
            hitCountSecond++;
            return "THIS  IS AN ERROR";
        }
    }

    // SUCCESS Test when problem handler was implemented as required.
    @Test
    public void testIssue5469HappyCase()
        throws Exception
    {
        // Given
        assertEquals(0, hitCountFirst);
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
        assertEquals(1, hitCountFirst);
    }

    // FAIL! Test when problem handler was implemented WRONG
    @Test
    public void testIssue5469BadImpl()
        throws Exception
    {
        // Given
        assertEquals(0, hitCountSecond);
        ObjectMapper mapper = JsonMapper.builder()
                .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .addHandler(new MoreProblemHandler5469())
                .build();

        // When
        try {
            mapper.readValue("{\"id\":  \"12ab\", \"name\": \"Bob\", " +
                    // Input is NULL, to cause problem
                    "\"age\": null}", Person5469.class);
            // Sanity check, we hit the code path as we wanted
            assertEquals(1, hitCountSecond);
            fail("Should not reach here.");
        } catch (InvalidFormatException e) {
            // Then
            verifyException(e,
                    "`DeserializationProblemHandler.handleNullForPrimitives()` for type `long` returned value of type `java.lang.String`");
        }
    }
}
