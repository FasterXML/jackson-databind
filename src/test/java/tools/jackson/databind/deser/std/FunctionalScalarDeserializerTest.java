package tools.jackson.databind.deser.std;

import org.junit.jupiter.api.Test;

import tools.jackson.core.Version;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.module.SimpleModule;

import static org.junit.jupiter.api.Assertions.*;

import static tools.jackson.databind.testutil.DatabindTestUtil.*;

/**
 * Tests for {@link FunctionalScalarDeserializer}.
 */
// [databind#4004]
public class FunctionalScalarDeserializerTest
{
    // Simple value type for testing
    static class Bar {
        private final String value;

        private Bar(String value) {
            this.value = value;
        }

        public static Bar of(String value) {
            return new Bar(value);
        }

        public String getValue() {
            return value;
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    @Test
    public void testSimpleFunctionFromString() throws Exception
    {
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addDeserializer(Bar.class,
                new FunctionalScalarDeserializer<>(Bar.class, Bar::of));

        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();

        Bar result = mapper.readValue("\"hello\"", Bar.class);
        assertEquals("hello", result.getValue());
    }

    @Test
    public void testFunctionFromNumber() throws Exception
    {
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addDeserializer(Bar.class,
                new FunctionalScalarDeserializer<>(Bar.class, Bar::of));

        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();

        // Numbers should be coerced to String via getValueAsString()
        Bar result = mapper.readValue("123", Bar.class);
        assertEquals("123", result.getValue());
    }

    @Test
    public void testFunctionFromBoolean() throws Exception
    {
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addDeserializer(Bar.class,
                new FunctionalScalarDeserializer<>(Bar.class, Bar::of));

        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();

        // Booleans should be coerced to String via getValueAsString()
        Bar result = mapper.readValue("true", Bar.class);
        assertEquals("true", result.getValue());
    }

    @Test
    public void testBiFunctionWithParserAccess() throws Exception
    {
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addDeserializer(Bar.class,
                new FunctionalScalarDeserializer<>(Bar.class,
                        (p, ctx) -> Bar.of("prefix:" + p.getValueAsString())));

        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();

        Bar result = mapper.readValue("\"test\"", Bar.class);
        assertEquals("prefix:test", result.getValue());
    }

    @Test
    public void testRejectsJsonArray() throws Exception
    {
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addDeserializer(Bar.class,
                new FunctionalScalarDeserializer<>(Bar.class, Bar::of));

        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();

        // Arrays should be rejected since this is a scalar deserializer
        try {
            mapper.readValue("[\"hello\"]", Bar.class);
            fail("Should not accept JSON array");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot deserialize");
        }
    }

    @Test
    public void testRejectsJsonObject() throws Exception
    {
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addDeserializer(Bar.class,
                new FunctionalScalarDeserializer<>(Bar.class, Bar::of));

        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();

        // Objects should be rejected (unless scalar-from-object is configured)
        try {
            mapper.readValue("{\"value\":\"hello\"}", Bar.class);
            fail("Should not accept JSON object");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot deserialize");
        }
    }

    @Test
    public void testIllegalArgumentExceptionHandling() throws Exception
    {
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addDeserializer(Bar.class,
                new FunctionalScalarDeserializer<>(Bar.class, s -> {
                    throw new IllegalArgumentException("Invalid format: " + s);
                }));

        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();

        try {
            mapper.readValue("\"bad\"", Bar.class);
            fail("Should throw exception");
        } catch (MismatchedInputException e) {
            verifyException(e, "not a valid textual representation");
            verifyException(e, "Invalid format");
        }
    }

    @Test
    public void testNullValue() throws Exception
    {
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addDeserializer(Bar.class,
                new FunctionalScalarDeserializer<>(Bar.class, Bar::of));

        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();

        Bar result = mapper.readValue("null", Bar.class);
        assertNull(result);
    }

    @Test
    public void testEmptyStringDefaultBehavior() throws Exception
    {
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addDeserializer(Bar.class,
                new FunctionalScalarDeserializer<>(Bar.class, Bar::of));

        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();

        // By default, empty string returns null for OtherScalar type
        Bar result = mapper.readValue("\"\"", Bar.class);
        assertNull(result);
    }
}
