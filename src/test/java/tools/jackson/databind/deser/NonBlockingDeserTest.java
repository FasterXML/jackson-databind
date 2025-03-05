package tools.jackson.databind.deser;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.async.ByteBufferFeeder;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for checking that test deserialization with non-blocking parsers
 */
public class NonBlockingDeserTest
{
    record Foo(String bar) {}

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    @Test
    public void testNonBlockingParser()
    {
        final ObjectMapper m = JsonMapper.builder()
            //.disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();
        final int len = 10;
        Foo[] foos = new Foo[len];
        for (int i = 0; i < len; ++i) {
            foos[i] = new Foo("bar-" + i);
        }
        final Foo[] result;
        try (final JsonParser parser =
                    m.tokenStreamFactory().createNonBlockingByteBufferParser(ObjectReadContext.empty())) {
              ((ByteBufferFeeder) parser).feedInput(ByteBuffer.wrap(m.writeValueAsBytes(foos)));
              ((ByteBufferFeeder) parser).endOfInput();
              result = m.readValue(parser, Foo[].class);
        }
        assertEquals(len, result.length);
    }
}
