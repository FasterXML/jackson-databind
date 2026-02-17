package tools.jackson.databind.jsontype.vld;

import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.InvalidTypeIdException;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// for [databind#2539]
public class BasicPTVKnownTypesTest extends DatabindTestUtil
{
    private final ObjectMapper DEFAULTING_MAPPER = jsonMapperBuilder()
            .activateDefaultTyping(BasicPolymorphicTypeValidator.builder()
                    .allowSubTypesWithExplicitDeserializer()
                    .build(),
                    DefaultTyping.NON_FINAL_AND_ENUMS)
            .build();

    static class Dangerous {
        public int x;
    }

    public void testWithJDKBasicsOk() throws Exception
    {
        Object[] input = new Object[] {
                "test", 42, new java.net.URL("http://localhost"),
                java.util.UUID.nameUUIDFromBytes("abc".getBytes()),
                new Object[] { }
        };

        String json = DEFAULTING_MAPPER.writeValueAsString(input);
        Object result = DEFAULTING_MAPPER.readValue(json, Object.class);
        assertEquals(Object[].class, result.getClass());

        // but then non-ok case:
        final String json2 = DEFAULTING_MAPPER.writeValueAsString(new Object[] {
                new Dangerous()
        });
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> DEFAULTING_MAPPER.readValue(json2, Object.class));
        verifyException(e, "Could not resolve type id 'tools.jackson.");
        verifyException(e, "as a subtype of");

        // and another one within array
        final String json3 = DEFAULTING_MAPPER.writeValueAsString(new Object[] {
                new Dangerous[] { new Dangerous() }
        });
        InvalidTypeIdException e2 = assertThrows(InvalidTypeIdException.class,
                () -> DEFAULTING_MAPPER.readValue(json3, Object.class));
        verifyException(e2, "Could not resolve type id '[Ltools.jackson.");
        verifyException(e2, "as a subtype of");
    }
}
