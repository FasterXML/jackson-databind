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

    // No length limit by default
    @Test
    public void defaultPatternLengthUnlimited() throws Exception
    {
        String longPattern = "a".repeat(50_000);
        assertEquals(longPattern, MAPPER.readValue(q(longPattern), Pattern.class).pattern());
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
