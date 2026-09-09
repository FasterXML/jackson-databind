package tools.jackson.databind.ser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.exc.JacksonIOException;

import tools.jackson.databind.DatabindException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SequenceWriter;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link SerializationFeature#CLOSE_CLOSEABLE} handling of values that
 * implement {@link AutoCloseable} but not {@link java.io.Closeable}: the feature
 * checks for the former, so all code paths need to handle it.
 */
public class CloseAutoCloseableTest extends DatabindTestUtil
{
    static class AutoCloseableBean implements AutoCloseable {
        public int a = 3;

        public boolean wasClosed = false;

        // Note: `AutoCloseable.close()` may throw checked `Exception`, unlike
        // `Closeable.close()` which is limited to `IOException`
        @Override
        public void close() throws Exception {
            wasClosed = true;
        }
    }

    static class FailingAutoCloseableBean implements AutoCloseable {
        public int a = 3;

        // Checked, non-`IOException` failure: only possible for `AutoCloseable`
        @Override
        public void close() throws Exception {
            throw new Exception("Fail on close()");
        }
    }

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .enable(SerializationFeature.CLOSE_CLOSEABLE)
            .build();

    @Test
    public void mapperWriteValueToGenerator() throws Exception
    {
        AutoCloseableBean bean = new AutoCloseableBean();
        StringWriter sw = new StringWriter();
        try (JsonGenerator g = MAPPER.createGenerator(sw)) {
            MAPPER.writeValue(g, bean);
        }
        assertEquals("""
                {"a":3,"wasClosed":false}""", sw.toString());
        assertTrue(bean.wasClosed);
    }

    // Same case, via `ObjectWriter`: has always worked, guard against regression
    @Test
    public void writerWriteValueToGenerator() throws Exception
    {
        AutoCloseableBean bean = new AutoCloseableBean();
        StringWriter sw = new StringWriter();
        try (JsonGenerator g = MAPPER.createGenerator(sw)) {
            MAPPER.writer().writeValue(g, bean);
        }
        assertEquals("""
                {"a":3,"wasClosed":false}""", sw.toString());
        assertTrue(bean.wasClosed);
    }

    // Failure to `close()` the value must not close (or otherwise mess with) the
    // caller-owned Generator, and must be reported as `JacksonException`
    @Test
    public void mapperCloseFailureWithGenerator() throws Exception
    {
        StringWriter sw = new StringWriter();
        try (JsonGenerator g = MAPPER.createGenerator(sw)) {
            DatabindException e = assertThrows(DatabindException.class,
                    () -> MAPPER.writeValue(g, new FailingAutoCloseableBean()));
            verifyException(e, "Failed to close value of type");
            assertFalse(g.isClosed());
            g.writeString("after");
        }
        assertEquals("""
                {"a":3} "after\"""", sw.toString());
    }

    @Test
    public void writerCloseFailureWithGenerator() throws Exception
    {
        StringWriter sw = new StringWriter();
        try (JsonGenerator g = MAPPER.createGenerator(sw)) {
            DatabindException e = assertThrows(DatabindException.class,
                    () -> MAPPER.writer().writeValue(g, new FailingAutoCloseableBean()));
            verifyException(e, "Failed to close value of type");
            assertFalse(g.isClosed());
            g.writeString("after");
        }
        assertEquals("""
                {"a":3} "after\"""", sw.toString());
    }

    @Test
    public void mapperWriteValueToStream() throws Exception
    {
        AutoCloseableBean bean = new AutoCloseableBean();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MAPPER.writeValue(out, bean);
        assertEquals("""
                {"a":3,"wasClosed":false}""", out.toString("UTF-8"));
        assertTrue(bean.wasClosed);
    }

    @Test
    public void mapperWriteValueAsString() throws Exception
    {
        AutoCloseableBean bean = new AutoCloseableBean();
        assertEquals("""
                {"a":3,"wasClosed":false}""", MAPPER.writeValueAsString(bean));
        assertTrue(bean.wasClosed);
    }

    // Mapper-owned Generator paths must not leak `close()` failure as plain
    // `RuntimeException` either
    @Test
    public void mapperOwnedGeneratorCloseFailure() throws Exception
    {
        DatabindException e = assertThrows(DatabindException.class,
                () -> MAPPER.writeValueAsString(new FailingAutoCloseableBean()));
        verifyException(e, "Failed to close value of type");

        e = assertThrows(DatabindException.class,
                () -> MAPPER.writer().writeValueAsString(new FailingAutoCloseableBean()));
        verifyException(e, "Failed to close value of type");
    }

    // [databind#6197]: `SequenceWriter` also needs to handle `AutoCloseable`,
    // not just `Closeable` (before fix: value simply never closed)
    @Test
    public void sequenceWriterWriteValue() throws Exception
    {
        AutoCloseableBean bean = new AutoCloseableBean();
        StringWriter sw = new StringWriter();
        try (SequenceWriter seq = MAPPER.writer().writeValues(sw)) {
            seq.write(bean);
        }
        assertEquals("""
                {"a":3,"wasClosed":false}""", sw.toString());
        assertTrue(bean.wasClosed);
    }

    @Test
    public void sequenceWriterWriteValueWithType() throws Exception
    {
        AutoCloseableBean bean = new AutoCloseableBean();
        StringWriter sw = new StringWriter();
        try (SequenceWriter seq = MAPPER.writer().writeValues(sw)) {
            seq.write(bean, MAPPER.constructType(AutoCloseableBean.class));
        }
        assertEquals("""
                {"a":3,"wasClosed":false}""", sw.toString());
        assertTrue(bean.wasClosed);
    }

    @Test
    public void sequenceWriterCloseFailure() throws Exception
    {
        StringWriter sw = new StringWriter();
        try (SequenceWriter seq = MAPPER.writer().writeValues(sw)) {
            DatabindException e = assertThrows(DatabindException.class,
                    () -> seq.write(new FailingAutoCloseableBean()));
            verifyException(e, "Failed to close value of type");
        }
    }

    // If both serialization and the recovery `close()` fail, the original failure
    // must survive, with close failure merely added as "suppressed"
    static class FailOnSerializeAndCloseBean implements AutoCloseable {
        public int getA() {
            throw new IllegalArgumentException("Fail on serialize");
        }

        @Override
        public void close() {
            throw new IllegalStateException("Fail on close");
        }
    }

    @Test
    public void sequenceWriterSerializationFailureKeepsCloseFailure() throws Exception
    {
        SequenceWriter seq = MAPPER.writer().writeValues(new StringWriter());
        Exception e = assertThrows(Exception.class,
                () -> seq.write(new FailOnSerializeAndCloseBean()));
        verifyException(e, "Fail on serialize");
        Throwable[] suppressed = e.getSuppressed();
        assertEquals(1, suppressed.length);
        verifyException(suppressed[0], "Fail on close");

        // and same for the `JavaType`-taking variant
        SequenceWriter seq2 = MAPPER.writer().writeValues(new StringWriter());
        e = assertThrows(Exception.class,
                () -> seq2.write(new FailOnSerializeAndCloseBean(),
                        MAPPER.constructType(FailOnSerializeAndCloseBean.class)));
        verifyException(e, "Fail on serialize");
        suppressed = e.getSuppressed();
        assertEquals(1, suppressed.length);
        verifyException(suppressed[0], "Fail on close");
    }

    // `JacksonException` from `close()` must be passed through as-is, not re-wrapped
    static class JacksonFailOnCloseBean implements AutoCloseable {
        public final static JacksonException FAILURE
            = JacksonIOException.construct(new IOException("Fail on close()"));

        public int a = 3;

        @Override
        public void close() {
            throw FAILURE;
        }
    }

    @Test
    public void jacksonExceptionFromCloseNotRewrapped() throws Exception
    {
        StringWriter sw = new StringWriter();
        try (JsonGenerator g = MAPPER.createGenerator(sw)) {
            assertSame(JacksonFailOnCloseBean.FAILURE,
                    assertThrows(JacksonException.class,
                            () -> MAPPER.writeValue(g, new JacksonFailOnCloseBean())));
            assertSame(JacksonFailOnCloseBean.FAILURE,
                    assertThrows(JacksonException.class,
                            () -> MAPPER.writer().writeValue(g, new JacksonFailOnCloseBean())));
        }
        assertSame(JacksonFailOnCloseBean.FAILURE,
                assertThrows(JacksonException.class,
                        () -> MAPPER.writeValueAsString(new JacksonFailOnCloseBean())));
        assertSame(JacksonFailOnCloseBean.FAILURE,
                assertThrows(JacksonException.class,
                        () -> MAPPER.writer().writeValueAsString(new JacksonFailOnCloseBean())));
        try (SequenceWriter seq = MAPPER.writer().writeValues(new StringWriter())) {
            assertSame(JacksonFailOnCloseBean.FAILURE,
                    assertThrows(JacksonException.class,
                            () -> seq.write(new JacksonFailOnCloseBean())));
            assertSame(JacksonFailOnCloseBean.FAILURE,
                    assertThrows(JacksonException.class,
                            () -> seq.write(new JacksonFailOnCloseBean(),
                                    MAPPER.constructType(JacksonFailOnCloseBean.class))));
        }
    }
}
