package tools.jackson.databind.jsontype.vld;

import tools.jackson.databind.BaseMapTest;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.InvalidTypeIdException;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

// for [databind#2539]
public class BasicPTVKnownTypesTest extends BaseMapTest
{
    private final ObjectMapper DEFAULTING_MAPPER = jsonMapperBuilder()
            .activateDefaultTyping(BasicPolymorphicTypeValidator.builder()
                    .allowSubTypesWithExplicitDeserializer()
                    .build(),
                    DefaultTyping.EVERYTHING)
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
        json = DEFAULTING_MAPPER.writeValueAsString(new Object[] {
                new Dangerous()
        });
        try {
            DEFAULTING_MAPPER.readValue(json, Object.class);
            fail("Should not pass");
        } catch (InvalidTypeIdException e) {
            verifyException(e, "Could not resolve type id 'tools.jackson.");
            verifyException(e, "as a subtype of");
        }

        // and another one within array
        json = DEFAULTING_MAPPER.writeValueAsString(new Object[] {
                new Dangerous[] { new Dangerous() }
        });
        try {
            DEFAULTING_MAPPER.readValue(json, Object.class);
            fail("Should not pass");
        } catch (InvalidTypeIdException e) {
            verifyException(e, "Could not resolve type id '[Ltools.jackson.");
            verifyException(e, "as a subtype of");
        }
    }
}
