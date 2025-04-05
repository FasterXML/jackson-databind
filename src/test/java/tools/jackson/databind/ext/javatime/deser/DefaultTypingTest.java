package tools.jackson.databind.ext.javatime.deser;

import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.DatabindContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultTypingTest extends DateTimeTestBase
{
    static class NoCheckSubTypeValidator
        extends PolymorphicTypeValidator.Base
    {
        private static final long serialVersionUID = 1L;

        @Override
        public Validity validateBaseType(DatabindContext ctxt, JavaType baseType) {
            return Validity.ALLOWED;
        }
    }

    private final ObjectMapper TYPING_MAPPER = newMapperBuilder()
            .activateDefaultTyping(new NoCheckSubTypeValidator())
            .build();

    // for [datatype-jsr310#24]
    @Test
    public void testZoneIdAsIs() throws Exception
    {
        ZoneId exp = ZoneId.of("America/Chicago");
        String json = TYPING_MAPPER.writeValueAsString(exp);
        ZoneId act = TYPING_MAPPER.readValue(json, ZoneId.class);
        assertEquals(exp, act);
    }

    // This one WILL add type info, since `ZoneId` is abstract type:
    @Test
    public void testZoneWithForcedBaseType() throws Exception
    {
        ZoneId exp = ZoneId.of("America/Chicago");
        String json = TYPING_MAPPER.writerFor(ZoneId.class).writeValueAsString(exp);
        ZoneId act = TYPING_MAPPER.readValue(json, ZoneId.class);
        assertEquals(exp, act);
    }
}
