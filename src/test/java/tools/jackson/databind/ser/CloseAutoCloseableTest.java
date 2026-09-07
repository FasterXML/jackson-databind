package tools.jackson.databind.ser;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonGenerator;

import tools.jackson.databind.ObjectMapper;
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
}
