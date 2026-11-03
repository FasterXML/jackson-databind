package tools.jackson.databind.jsontype.vld;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.InvalidTypeIdException;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for [databind#6401]: malformed or conflicting generic type ids
 * must result in {@link InvalidTypeIdException}, not raw
 * {@link IllegalArgumentException}.
 */
public class InvalidGenericTypeId6401Test extends DatabindTestUtil
{
    static final class MapWrapper {
        public Map<String, Integer> map;
    }

    // Generic subtype with value type binding conflicting with that of MapWrapper.map
    static class LongValuedMap<T> extends HashMap<String, Long> {
        private static final long serialVersionUID = 1L;
    }

    private final PolymorphicTypeValidator PTV = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType("java.util.")
            .allowIfSubType(LongValuedMap.class)
            .build();

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .activateDefaultTyping(PTV, DefaultTyping.NON_FINAL)
            .build();

    // Wrong number of type parameters in generic type id
    @Test
    public void wrongTypeParameterCount() throws Exception
    {
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> MAPPER.readValue("""
                        ["java.util.HashMap<java.lang.String>", {}]
                        """, Object.class));
        verifyException(e, "Could not resolve type id 'java.util.HashMap<java.lang.String>'");
        verifyException(e, "expects 2");
    }

    // Subtype whose type parameter binding conflicts with that of the base type
    @Test
    public void conflictingTypeParameters() throws Exception
    {
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> MAPPER.readValue("""
                        {"map": ["%s", {}]}
                        """.formatted(LongValuedMap.class.getName()), MapWrapper.class));
        verifyException(e, "Could not resolve type id '"+LongValuedMap.class.getName()+"'");
        verifyException(e, "Type parameter #2/2 differs");
    }
}
