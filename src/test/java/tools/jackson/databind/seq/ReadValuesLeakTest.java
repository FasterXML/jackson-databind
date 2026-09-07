package tools.jackson.databind.seq;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JacksonException;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify that {@code ObjectReader.readValues()} does not leak the
 * "managed" (created by {@code ObjectReader}, owning the underlying input source)
 * parser if initialization of the {@code MappingIterator} fails.
 */
public class ReadValuesLeakTest extends DatabindTestUtil
{
    // Content whose very first `nextToken()` fails, so that no `MappingIterator`
    // is ever constructed
    private final static String INVALID_JSON = "@@@";

    static class CloseTrackingInputStream extends ByteArrayInputStream {
        public boolean closed = false;

        public CloseTrackingInputStream(String src) {
            super(utf8Bytes(src));
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }

    static class CloseTrackingReader extends StringReader {
        public boolean closed = false;

        public CloseTrackingReader(String src) { super(src); }

        @Override
        public void close() {
            closed = true;
            super.close();
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void inputStreamClosedOnInitFailure() throws Exception
    {
        CloseTrackingInputStream in = new CloseTrackingInputStream(INVALID_JSON);
        assertThrows(JacksonException.class,
                () -> MAPPER.readerFor(Object.class).readValues(in));
        assertTrue(in.closed, "InputStream should have been closed by failed readValues()");
    }

    @Test
    public void readerClosedOnInitFailure() throws Exception
    {
        CloseTrackingReader r = new CloseTrackingReader(INVALID_JSON);
        assertThrows(JacksonException.class,
                () -> MAPPER.readerFor(Object.class).readValues(r));
        assertTrue(r.closed, "Reader should have been closed by failed readValues()");
    }

    // And for comparison: single-value read has always closed correctly
    @Test
    public void singleValueReadClosesToo() throws Exception
    {
        CloseTrackingInputStream in = new CloseTrackingInputStream(INVALID_JSON);
        assertThrows(JacksonException.class,
                () -> MAPPER.readValue(in, Object.class));
        assertTrue(in.closed, "InputStream should have been closed by failed readValue()");
    }

    // Caller-provided parser, however, must NOT be closed by us
    @Test
    public void callerSuppliedParserNotClosed() throws Exception
    {
        CloseTrackingInputStream in = new CloseTrackingInputStream(INVALID_JSON);
        try (var p = MAPPER.createParser(in)) {
            // does not fail here: no read attempted by `readValues(JsonParser)`
            MAPPER.readerFor(Object.class).readValues(p);
            assertFalse(in.closed);
        }
    }
}
