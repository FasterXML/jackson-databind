package tools.jackson.databind.deser.std;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import tools.jackson.core.type.TypeReference;

import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.type.LogicalType;

import static org.junit.jupiter.api.Assertions.*;
import static tools.jackson.databind.testutil.DatabindTestUtil.*;

// [databind#4004]: Add FunctionalScalarDeserializer for functional-style deserialization
public class FunctionalScalarDeserializer4004Test
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

    // Wrapper POJO for testing deserialization as a field
    static class BarWrapper {
        public Bar bar;
    }

    @Test
    public void testClassWithFunction() throws Exception
    {
        SimpleModule module = new SimpleModule("test");
        module.addDeserializer(Bar.class,
                new FunctionalScalarDeserializer<>(Bar.class, Bar::of));

        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();

        Bar result = mapper.readValue("\"hello\"", Bar.class);
        assertEquals("hello", result.getValue());
    }

    @Test
    public void testJavaTypeWithFunction() throws Exception
    {
        ObjectMapper baseMapper = jsonMapperBuilder().build();
        JavaType barType = baseMapper.constructType(Bar.class);

        SimpleModule module = new SimpleModule("test");
        module.addDeserializer(Bar.class,
                new FunctionalScalarDeserializer<>(barType, Bar::of));

        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();

        Bar result = mapper.readValue("\"javatype\"", Bar.class);
        assertEquals("javatype", result.getValue());
    }

    @Test
    public void testClassWithBiFunction() throws Exception
    {
        SimpleModule module = new SimpleModule("test");
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
    public void testJavaTypeWithBiFunction() throws Exception
    {
        ObjectMapper baseMapper = jsonMapperBuilder().build();
        JavaType barType = baseMapper.constructType(Bar.class);

        SimpleModule module = new SimpleModule("test");
        module.addDeserializer(Bar.class,
                new FunctionalScalarDeserializer<>(barType,
                        (p, ctx) -> Bar.of("bi:" + p.getValueAsString())));

        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();

        Bar result = mapper.readValue("\"test\"", Bar.class);
        assertEquals("bi:test", result.getValue());
    }

    @Test
    public void testFromIntegerNumber() throws Exception
    {
        SimpleModule module = new SimpleModule("test");
        module.addDeserializer(Bar.class,
                new FunctionalScalarDeserializer<>(Bar.class, Bar::of));

        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();

        Bar result = mapper.readValue("123", Bar.class);
        assertEquals("123", result.getValue());
    }

    @Test
    public void testFromDecimalNumber() throws Exception
    {
        SimpleModule module = new SimpleModule("test");
        module.addDeserializer(Bar.class,
                new FunctionalScalarDeserializer<>(Bar.class, Bar::of));

        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();

        Bar result = mapper.readValue("3.14159", Bar.class);
        assertEquals("3.14159", result.getValue());
    }

    @Test
    public void testFromNegativeNumber() throws Exception
    {
        SimpleModule module = new SimpleModule("test");
        module.addDeserializer(Bar.class,
                new FunctionalScalarDeserializer<>(Bar.class, Bar::of));

        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();

        Bar result = mapper.readValue("-42", Bar.class);
        assertEquals("-42", result.getValue());
    }

    @Test
    public void testFromBooleanTrue() throws Exception
    {
        SimpleModule module = new SimpleModule("test");
        module.addDeserializer(Bar.class,
                new FunctionalScalarDeserializer<>(Bar.class, Bar::of));

        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();

        Bar result = mapper.readValue("true", Bar.class);
        assertEquals("true", result.getValue());
    }

    @Test
    public void testFromBooleanFalse() throws Exception
    {
        SimpleModule module = new SimpleModule("test");
        module.addDeserializer(Bar.class,
                new FunctionalScalarDeserializer<>(Bar.class, Bar::of));

        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();

        Bar result = mapper.readValue("false", Bar.class);
        assertEquals("false", result.getValue());
    }

    @Test
    public void testNullValue() throws Exception
    {
        SimpleModule module = new SimpleModule("test");
        module.addDeserializer(Bar.class,
                new FunctionalScalarDeserializer<>(Bar.class, Bar::of));

        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();

        Bar result = mapper.readValue("null", Bar.class);
        assertNull(result);
    }

    @Test
    public void testEmptyStringDefault() throws Exception
    {
        SimpleModule module = new SimpleModule("test");
        module.addDeserializer(Bar.class,
                new FunctionalScalarDeserializer<>(Bar.class, Bar::of));

        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();

        // By default, empty string returns null for OtherScalar type
        Bar result = mapper.readValue("\"\"", Bar.class);
        assertNull(result);
    }

    @Test
    public void testEmptyStringCoercionFail() throws Exception
    {
        SimpleModule module = new SimpleModule("test");
        module.addDeserializer(Bar.class,
                new FunctionalScalarDeserializer<>(Bar.class, Bar::of));

        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .withCoercionConfigDefaults(cfg -> cfg.setCoercion(
                        CoercionInputShape.EmptyString, CoercionAction.Fail))
                .build();

        try {
            mapper.readValue("\"\"", Bar.class);
            fail("Should throw exception for empty string with Fail action");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot coerce empty String");
        }
    }

    @Test
    public void testEmptyStringCoercionAsNull() throws Exception
    {
        SimpleModule module = new SimpleModule("test");
        module.addDeserializer(Bar.class,
                new FunctionalScalarDeserializer<>(Bar.class, Bar::of));

        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .withCoercionConfig(LogicalType.OtherScalar, cfg -> cfg.setCoercion(
                        CoercionInputShape.EmptyString, CoercionAction.AsNull))
                .build();

        Bar result = mapper.readValue("\"\"", Bar.class);
        assertNull(result);
    }

    @Test
    public void testEmptyStringCoercionAsEmpty() throws Exception
    {
        SimpleModule module = new SimpleModule("test");
        module.addDeserializer(Bar.class,
                new FunctionalScalarDeserializer<>(Bar.class, Bar::of));

        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .withCoercionConfig(LogicalType.OtherScalar, cfg -> cfg.setCoercion(
                        CoercionInputShape.EmptyString, CoercionAction.AsEmpty))
                .build();

        // AsEmpty typically returns null for types without defined empty value
        Bar result = mapper.readValue("\"\"", Bar.class);
        assertNull(result);
    }

    @Test
    public void testAsPojoField() throws Exception
    {
        SimpleModule module = new SimpleModule("test");
        module.addDeserializer(Bar.class,
                new FunctionalScalarDeserializer<>(Bar.class, Bar::of));

        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();

        BarWrapper result = mapper.readValue("{\"bar\":\"fieldValue\"}", BarWrapper.class);
        assertNotNull(result.bar);
        assertEquals("fieldValue", result.bar.getValue());
    }

    @Test
    public void testInList() throws Exception
    {
        SimpleModule module = new SimpleModule("test");
        module.addDeserializer(Bar.class,
                new FunctionalScalarDeserializer<>(Bar.class, Bar::of));

        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();

        List<Bar> result = mapper.readValue("[\"a\", \"b\", \"c\"]",
                new TypeReference<List<Bar>>() {});
        assertEquals(3, result.size());
        assertEquals("a", result.get(0).getValue());
        assertEquals("b", result.get(1).getValue());
        assertEquals("c", result.get(2).getValue());
    }

    @Test
    public void testNullFieldInPojo() throws Exception
    {
        SimpleModule module = new SimpleModule("test");
        module.addDeserializer(Bar.class,
                new FunctionalScalarDeserializer<>(Bar.class, Bar::of));

        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();

        BarWrapper result = mapper.readValue("{\"bar\":null}", BarWrapper.class);
        assertNull(result.bar);
    }

    @Test
    public void testRejectsJsonArray() throws Exception
    {
        SimpleModule module = new SimpleModule("test");
        module.addDeserializer(Bar.class,
                new FunctionalScalarDeserializer<>(Bar.class, Bar::of));

        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();

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
        SimpleModule module = new SimpleModule("test");
        module.addDeserializer(Bar.class,
                new FunctionalScalarDeserializer<>(Bar.class, Bar::of));

        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();

        try {
            mapper.readValue("{\"value\":\"hello\"}", Bar.class);
            fail("Should not accept JSON object");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot deserialize");
        }
    }

    @Test
    public void testFunctionThrowsIllegalArgumentException() throws Exception
    {
        SimpleModule module = new SimpleModule("test");
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
    public void testBiFunctionThrowsIllegalArgumentException() throws Exception
    {
        SimpleModule module = new SimpleModule("test");
        module.addDeserializer(Bar.class,
                new FunctionalScalarDeserializer<>(Bar.class, (p, ctx) -> {
                    throw new IllegalArgumentException("BiFunction error: " + p.getValueAsString());
                }));

        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();

        try {
            mapper.readValue("\"invalid\"", Bar.class);
            fail("Should throw exception");
        } catch (MismatchedInputException e) {
            verifyException(e, "not a valid textual representation");
            verifyException(e, "BiFunction error");
        }
    }

    @Test
    public void testStringFunctionReceivesExtractedText() throws Exception
    {
        final AtomicReference<String> receivedValue = new AtomicReference<>();

        SimpleModule module = new SimpleModule("test");
        module.addDeserializer(Bar.class,
                new FunctionalScalarDeserializer<>(Bar.class, text -> {
                    receivedValue.set(text);
                    return Bar.of(text);
                }));

        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();

        Bar result = mapper.readValue("\"expected-value\"", Bar.class);

        assertEquals("expected-value", receivedValue.get());
        assertEquals("expected-value", result.getValue());
    }

    @Test
    public void testBiFunctionReceivesParserDirectly() throws Exception
    {
        final AtomicReference<String> parserState = new AtomicReference<>();

        SimpleModule module = new SimpleModule("test");
        module.addDeserializer(Bar.class,
                new FunctionalScalarDeserializer<>(Bar.class, (p, ctx) -> {
                    parserState.set(p.currentToken().toString());
                    return Bar.of(p.getValueAsString());
                }));

        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();

        Bar result = mapper.readValue("\"test-value\"", Bar.class);

        assertEquals("VALUE_STRING", parserState.get());
        assertEquals("test-value", result.getValue());
    }

    @Test
    public void testStringFunctionReceivesCoercedNumericText() throws Exception
    {
        final AtomicReference<String> receivedValue = new AtomicReference<>();

        SimpleModule module = new SimpleModule("test");
        module.addDeserializer(Bar.class,
                new FunctionalScalarDeserializer<>(Bar.class, text -> {
                    receivedValue.set(text);
                    return Bar.of(text);
                }));

        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();

        Bar result = mapper.readValue("12345", Bar.class);

        assertEquals("12345", receivedValue.get());
        assertEquals("12345", result.getValue());
    }

    @Test
    public void testFunctionThrowsCustomException() throws Exception
    {
        SimpleModule module = new SimpleModule("test");
        module.addDeserializer(Bar.class,
                new FunctionalScalarDeserializer<>(Bar.class, s -> {
                    throw new RuntimeException("Custom error: " + s);
                }));

        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();

        try {
            mapper.readValue("\"bad\"", Bar.class);
            fail("Should throw exception");
        } catch (MismatchedInputException e) {
            verifyException(e, "not a valid textual representation");
            verifyException(e, "Custom error");
        }
    }

    @Test
    public void testBiFunctionThrowsCustomException() throws Exception
    {
        SimpleModule module = new SimpleModule("test");
        module.addDeserializer(Bar.class,
                new FunctionalScalarDeserializer<>(Bar.class, (p, ctx) -> {
                    throw new RuntimeException("BiFunction custom error: " + p.getValueAsString());
                }));

        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();

        try {
            mapper.readValue("\"invalid\"", Bar.class);
            fail("Should throw exception");
        } catch (MismatchedInputException e) {
            verifyException(e, "not a valid textual representation");
            verifyException(e, "BiFunction custom error");
        }
    }
}
