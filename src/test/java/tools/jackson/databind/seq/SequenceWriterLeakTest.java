package tools.jackson.databind.seq;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonGenerator;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.cfg.GeneratorInitializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify that a generator we constructed ourselves -- and which owns the
 * output target -- gets closed if initialization fails before any content is written.
 */
public class SequenceWriterLeakTest extends DatabindTestUtil
{
    static class FailingInitializer implements GeneratorInitializer {
        @Override
        public void initialize(SerializationConfig config, JsonGenerator g) {
            throw new IllegalStateException("Initialization failed");
        }
    }

    static class CloseTrackingOutputStream extends ByteArrayOutputStream {
        public boolean closed = false;

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }

    static class CloseTrackingWriter extends StringWriter {
        public boolean closed = false;

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }

    private final ObjectMapper FAILING_MAPPER = JsonMapper.builder()
            .generatorInitializer(new FailingInitializer())
            .build();

    private final Object VALUE = Map.of("a", 1);

    @Test
    public void mapperWriteValueClosesOnInitFailure() throws Exception
    {
        CloseTrackingOutputStream out = new CloseTrackingOutputStream();
        assertThrows(IllegalStateException.class,
                () -> FAILING_MAPPER.writeValue(out, VALUE));
        assertTrue(out.closed, "OutputStream should have been closed by failed writeValue()");
    }

    @Test
    public void writerWriteValueClosesOnInitFailure() throws Exception
    {
        CloseTrackingWriter w = new CloseTrackingWriter();
        assertThrows(IllegalStateException.class,
                () -> FAILING_MAPPER.writer().writeValue(w, VALUE));
        assertTrue(w.closed, "Writer should have been closed by failed writeValue()");
    }

    @Test
    public void writeValuesClosesOnInitFailure() throws Exception
    {
        CloseTrackingOutputStream out = new CloseTrackingOutputStream();
        assertThrows(IllegalStateException.class,
                () -> FAILING_MAPPER.writer().writeValues(out));
        assertTrue(out.closed, "OutputStream should have been closed by failed writeValues()");
    }

    @Test
    public void writeValuesAsArrayClosesOnInitFailure() throws Exception
    {
        CloseTrackingOutputStream out = new CloseTrackingOutputStream();
        assertThrows(IllegalStateException.class,
                () -> FAILING_MAPPER.writer().writeValuesAsArray(out));
        assertTrue(out.closed, "OutputStream should have been closed by failed writeValuesAsArray()");
    }

    @Test
    public void writeValuesAsArrayToWriterClosesOnInitFailure() throws Exception
    {
        CloseTrackingWriter w = new CloseTrackingWriter();
        assertThrows(IllegalStateException.class,
                () -> FAILING_MAPPER.writer().writeValuesAsArray(w));
        assertTrue(w.closed, "Writer should have been closed by failed writeValuesAsArray()");
    }

    // But a caller-supplied generator is neither initialized nor closed by us
    @Test
    public void callerSuppliedGeneratorNotTouched() throws Exception
    {
        CloseTrackingOutputStream out = new CloseTrackingOutputStream();
        ObjectMapper plain = newJsonMapper();
        try (JsonGenerator g = plain.createGenerator(out)) {
            FAILING_MAPPER.writer().writeValuesAsArray(g);
            assertFalse(out.closed);
        }
    }
}
