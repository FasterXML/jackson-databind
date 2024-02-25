package com.fasterxml.jackson.databind.interop;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import com.fasterxml.jackson.databind.util.TokenBuffer;

import static org.junit.jupiter.api.Assertions.*;

// [databind#4082]: add fallback handling for Java 8 Optional types, to
// prevent accidental serialization as POJOs, as well as give more information
// on deserialization attempts
//
// @since 2.16
public class OptionalJava8Fallbacks4082Test extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = newJsonMapper();

    // Test to prevent serialization as POJO, without Java 8 date/time module:
    @Test
    public void testPreventSerialization() throws Exception {
        _testPreventSerialization(Optional.empty());
        _testPreventSerialization(OptionalInt.of(13));
        _testPreventSerialization(OptionalLong.of(-1L));
        _testPreventSerialization(OptionalDouble.of(0.5));
    }

    private void _testPreventSerialization(Object value) throws Exception
    {
        try {
            String json = MAPPER.writeValueAsString(value);
            fail("Should not pass, wrote out as\n: "+json);
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Java 8 optional type `"+value.getClass().getName()
                    +"` not supported by default");
            verifyException(e, "add Module \"com.fasterxml.jackson.datatype:jackson-datatype-jdk8\"");
        }
    }

    @Test
    public void testBetterDeserializationError() throws Exception
    {
        _testBetterDeserializationError(Optional.class);
        _testBetterDeserializationError(OptionalInt.class);
        _testBetterDeserializationError(OptionalLong.class);
        _testBetterDeserializationError(OptionalDouble.class);
    }

    private void _testBetterDeserializationError(Class<?> target) throws Exception
    {
        try {
            Object result = MAPPER.readValue(" 0 ", target);
            fail("Not expecting to pass, resulted in: "+result);
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Java 8 optional type `"+target.getName()+"` not supported by default");
            verifyException(e, "add Module \"com.fasterxml.jackson.datatype:jackson-datatype-jdk8\"");
        }
    }

    // But, [databind#3091], allow deser from JsonToken.VALUE_EMBEDDED_OBJECT
    @Test
    public void testAllowAsEmbedded() throws Exception
    {
        Optional<Object> optValue = Optional.empty();
        try (TokenBuffer tb = new TokenBuffer((ObjectCodec) null, false)) {
            tb.writeEmbeddedObject(optValue);

            try (JsonParser p = tb.asParser()) {
                Optional<?>  result = MAPPER.readValue(p, Optional.class);
                assertSame(optValue, result);
            }
        }

        // but also try deser into an array
        try (TokenBuffer tb = new TokenBuffer((ObjectCodec) null, false)) {
            tb.writeStartArray();
            tb.writeEmbeddedObject(optValue);
            tb.writeEndArray();

            try (JsonParser p = tb.asParser()) {
                Object[] result = MAPPER.readValue(p, Object[].class);
                assertNotNull(result);
                assertEquals(1, result.length);
                assertSame(optValue, result[0]);
            }
        }
    }
}
