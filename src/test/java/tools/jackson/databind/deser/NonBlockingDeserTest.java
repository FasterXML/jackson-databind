package tools.jackson.databind.deser;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.async.ByteArrayFeeder;
import tools.jackson.core.async.ByteBufferFeeder;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for checking that test deserialization with non-blocking parsers
 */
public class NonBlockingDeserTest extends DatabindTestUtil
{
    record Foo(String bar) {}

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final static int TEST_ITEM_COUNT = 10;

    private final ObjectMapper MAPPER = newJsonMapper();

    private final byte[] TEST_DOC = _testDoc(TEST_ITEM_COUNT);
    
    @Test
    public void testNonBlockingByteArrayParserViaMapper()
    {
        try (final JsonParser parser =
                MAPPER.createNonBlockingByteArrayParser()) {
              ((ByteArrayFeeder) parser).feedInput(TEST_DOC, 0, TEST_DOC.length);
              ((ByteArrayFeeder) parser).endOfInput();
              Foo[] result = MAPPER.readValue(parser, Foo[].class);
              assertEquals(TEST_ITEM_COUNT, result.length);
        }
    }

    @Test
    public void testNonBlockingByteArrayParserViaReader()
    {
        try (final JsonParser parser =
                MAPPER.reader().createNonBlockingByteArrayParser()) {
            ((ByteArrayFeeder) parser).feedInput(TEST_DOC, 0, TEST_DOC.length);
            ((ByteArrayFeeder) parser).endOfInput();
              Foo[] result = MAPPER.readValue(parser, Foo[].class);
              assertEquals(TEST_ITEM_COUNT, result.length);
        }
    }

    @Test
    public void testNonBlockingByteBufferParserViaMapper()
    {
        try (final JsonParser parser =
                MAPPER.createNonBlockingByteBufferParser()) {
              ((ByteBufferFeeder) parser).feedInput(ByteBuffer.wrap(TEST_DOC));
              ((ByteBufferFeeder) parser).endOfInput();
              Foo[] result = MAPPER.readValue(parser, Foo[].class);
              assertEquals(TEST_ITEM_COUNT, result.length);
        }
    }

    @Test
    public void testNonBlockingByteBufferParserViaReader()
    {
        try (final JsonParser parser =
                MAPPER.reader().createNonBlockingByteBufferParser()) {
              ((ByteBufferFeeder) parser).feedInput(ByteBuffer.wrap(TEST_DOC));
              ((ByteBufferFeeder) parser).endOfInput();
              Foo[] result = MAPPER.readValue(parser, Foo[].class);
              assertEquals(TEST_ITEM_COUNT, result.length);
        }
    }
    
    private byte[] _testDoc(int count) {
        Foo[] foos = new Foo[count];
        for (int i = 0; i < count; ++i) {
            foos[i] = new Foo("bar-" + i);
        }
        return MAPPER.writeValueAsBytes(foos);
    }

}
