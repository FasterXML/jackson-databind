package com.fasterxml.jackson.databind.interop;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.util.TokenBuffer;

// [databind#2683]: add fallback handling for Java 8 date/time types, to
// prevent accidental serialization as POJOs, as well as give more information
// on deserialization attempts
//
// @since 2.12
public class DateJava8FallbacksTest extends BaseMapTest
{
    private final ObjectMapper MAPPER = newJsonMapper();

    private final OffsetDateTime DATETIME_EPOCH = OffsetDateTime.ofInstant(Instant.ofEpochSecond(0L),
            ZoneOffset.of("Z"));

    // Test to prevent serialization as POJO, without Java 8 date/time module:
    public void testPreventSerialization() throws Exception
    {
        try {
            String json = MAPPER.writerWithDefaultPrettyPrinter()
                .writeValueAsString(DATETIME_EPOCH);
            fail("Should not pass, wrote out as\n: "+json);
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Java 8 date/time type `java.time.OffsetDateTime` not supported by default");
            verifyException(e, "add Module \"com.fasterxml.jackson.datatype:jackson-datatype-jsr310\"");
        }
    }

    public void testBetterDeserializationError() throws Exception
    {
        try {
            OffsetDateTime result = MAPPER.readValue(" 0 ", OffsetDateTime.class);
            fail("Not expecting to pass, resulted in: "+result);
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Java 8 date/time type `java.time.OffsetDateTime` not supported by default");
            verifyException(e, "add Module \"com.fasterxml.jackson.datatype:jackson-datatype-jsr310\"");
        }
    }

    // But, [databind#3091], allow deser from JsonToken.VALUE_EMBEDDED_OBJECT
    public void testAllowAsEmbedded() throws Exception
    {
        OffsetDateTime time = OffsetDateTime.ofInstant(Instant.now(),
                ZoneId.of("Z"));
        try (TokenBuffer tb = new TokenBuffer((ObjectCodec) null, false)) {
            tb.writeEmbeddedObject(time);

            try (JsonParser p = tb.asParser()) {
                OffsetDateTime result = MAPPER.readValue(p, OffsetDateTime.class);
                assertSame(time, result);
            }
        }

        // but also try deser into an array
        try (TokenBuffer tb = new TokenBuffer((ObjectCodec) null, false)) {
            tb.writeStartArray();
            tb.writeEmbeddedObject(time);
            tb.writeEndArray();

            try (JsonParser p = tb.asParser()) {
                Object[] result = MAPPER.readValue(p, Object[].class);
                assertNotNull(result);
                assertEquals(1, result.length);
                assertSame(time, result[0]);
            }
        }
    }
}
