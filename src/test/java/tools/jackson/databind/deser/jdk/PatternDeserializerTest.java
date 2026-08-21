package tools.jackson.databind.deser.jdk;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.module.SimpleModule;

import static org.junit.jupiter.api.Assertions.*;

import static tools.jackson.databind.testutil.DatabindTestUtil.*;

/**
 * Tests for {@link PatternDeserializer}.
 */
public class PatternDeserializerTest
{
    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void patternDeser() throws Exception
    {
        Pattern exp = Pattern.compile("abc:\\s?(\\d+)");
        // Ok: easiest way is to just serialize first; problem
        // is the backslash
        String json = MAPPER.writeValueAsString(exp);
        Pattern result = MAPPER.readValue(json, Pattern.class);
        assertEquals(exp.pattern(), result.pattern());

        // [databind#3290]: actually need to retain at least trailing space
        // (and since we do that, just retain all...)
        exp = Pattern.compile("^WIN\\ ");
        json = MAPPER.writeValueAsString(exp);
        result = MAPPER.readValue(json, Pattern.class);
        assertEquals(exp.pattern(), result.pattern());

        // [databind#3598]: should also handle invalid pattern serialization
        // somewhat gracefully
        try {
            MAPPER.readValue(q("[abc"), Pattern.class);
            fail("Should not pass");
        } catch (InvalidFormatException e) {
            verifyException(e, "Cannot deserialize value of type `java.util.regex.Pattern` from String \"[abc\"");
            verifyException(e, "Invalid pattern, problem");
        }
    }

    // Reject excessively long regex patterns to prevent catastrophic
    // backtracking and excessive resource usage
    @Test
    public void patternLengthLimit() throws Exception
    {
        // A normal-length pattern should still work
        Pattern result = MAPPER.readValue(q("abc"), Pattern.class);
        assertEquals("abc", result.pattern());

        // A pattern at exactly the limit (1000 chars) should work
        String limitPattern = "a".repeat(PatternDeserializer.DEFAULT_MAX_PATTERN_LENGTH);
        result = MAPPER.readValue(q(limitPattern), Pattern.class);
        assertEquals(limitPattern, result.pattern());

        // A pattern exceeding the limit (1001 chars) should be rejected
        String overLimitPattern = "a".repeat(PatternDeserializer.DEFAULT_MAX_PATTERN_LENGTH + 1);
        try {
            MAPPER.readValue(q(overLimitPattern), Pattern.class);
            fail("Should not pass");
        } catch (InvalidFormatException e) {
            verifyException(e, "regex pattern length");
            verifyException(e, "exceeds maximum");
        }
    }

    // Maximum length is configurable by explicitly registering the deserializer
    @Test
    public void customPatternLengthLimit() throws Exception
    {
        ObjectMapper mapper = _mapperWithMaxLength(10);

        assertEquals("a".repeat(10),
                mapper.readValue(q("a".repeat(10)), Pattern.class).pattern());

        try {
            mapper.readValue(q("a".repeat(11)), Pattern.class);
            fail("Should not pass");
        } catch (InvalidFormatException e) {
            verifyException(e, "regex pattern length (11) exceeds maximum (10)");
        }
    }

    // Registered deserializer must take precedence over the default one, even
    // when it is more permissive than the default
    @Test
    public void customLimitOverridesDefault() throws Exception
    {
        ObjectMapper mapper = _mapperWithMaxLength(2000);

        String longPattern = "a".repeat(1500);
        // Would be rejected by the default deserializer...
        try {
            MAPPER.readValue(q(longPattern), Pattern.class);
            fail("Should not pass");
        } catch (InvalidFormatException e) {
            verifyException(e, "regex pattern length");
        }
        // ... but accepted by the registered one
        assertEquals(longPattern, mapper.readValue(q(longPattern), Pattern.class).pattern());
    }

    // Length checking can be disabled altogether with UNLIMITED_PATTERN_LENGTH
    @Test
    public void unlimitedPatternLength() throws Exception
    {
        ObjectMapper mapper = _mapperWithMaxLength(PatternDeserializer.UNLIMITED_PATTERN_LENGTH);

        String hugePattern = "a".repeat(50_000);
        assertEquals(hugePattern, mapper.readValue(q(hugePattern), Pattern.class).pattern());
    }

    @Test
    public void invalidMaxLength() throws Exception
    {
        assertThrows(IllegalArgumentException.class, () -> new PatternDeserializer(0));
        assertThrows(IllegalArgumentException.class, () -> new PatternDeserializer(-2));
        // but -1 means "unlimited", and is accepted
        assertNotNull(new PatternDeserializer(PatternDeserializer.UNLIMITED_PATTERN_LENGTH));
    }

    private ObjectMapper _mapperWithMaxLength(int maxPatternLength) {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Pattern.class, new PatternDeserializer(maxPatternLength));
        return jsonMapperBuilder().addModule(module).build();
    }
}
