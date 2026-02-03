package tools.jackson.databind.deser.jdk;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ByteBufferDeserializer}.
 */
public class ByteBufferDeserializerTest extends DatabindTestUtil
{
    static class ByteBufferBean {
        public ByteBuffer data;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testSimpleByteBufferDeser() throws Exception
    {
        // Base64 for "hello" is "aGVsbG8="
        ByteBuffer result = MAPPER.readValue(q("aGVsbG8="), ByteBuffer.class);
        assertNotNull(result);
        byte[] bytes = new byte[result.remaining()];
        result.get(bytes);
        assertEquals("hello", new String(bytes, StandardCharsets.UTF_8));
    }

    @Test
    public void testEmptyByteBuffer() throws Exception
    {
        // Base64 for empty byte array is ""
        ByteBuffer result = MAPPER.readValue(q(""), ByteBuffer.class);
        assertNotNull(result);
        assertEquals(0, result.remaining());
    }

    @Test
    public void testByteBufferRoundTrip() throws Exception
    {
        byte[] original = "ByteBuffer round-trip test data".getBytes(StandardCharsets.UTF_8);
        ByteBuffer input = ByteBuffer.wrap(original);

        String json = MAPPER.writeValueAsString(input);
        ByteBuffer result = MAPPER.readValue(json, ByteBuffer.class);

        assertNotNull(result);
        byte[] resultBytes = new byte[result.remaining()];
        result.get(resultBytes);
        assertArrayEquals(original, resultBytes);
    }

    @Test
    public void testByteBufferAsProperty() throws Exception
    {
        // Base64 for [1,2,3] is "AQID"
        ByteBufferBean bean = MAPPER.readValue(
                a2q("{'data':'AQID'}"), ByteBufferBean.class);
        assertNotNull(bean);
        assertNotNull(bean.data);
        assertEquals(3, bean.data.remaining());
        assertEquals(1, bean.data.get());
        assertEquals(2, bean.data.get());
        assertEquals(3, bean.data.get());
    }

    @Test
    public void testByteBufferPropertyRoundTrip() throws Exception
    {
        ByteBufferBean input = new ByteBufferBean();
        input.data = ByteBuffer.wrap(new byte[] { 10, 20, 30, 40 });

        String json = MAPPER.writeValueAsString(input);
        ByteBufferBean result = MAPPER.readValue(json, ByteBufferBean.class);

        assertNotNull(result);
        assertNotNull(result.data);
        assertEquals(4, result.data.remaining());
        assertEquals(10, result.data.get());
        assertEquals(20, result.data.get());
    }

    @Test
    public void testLargeByteBuffer() throws Exception
    {
        byte[] largeData = new byte[10000];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i & 0xFF);
        }
        ByteBuffer input = ByteBuffer.wrap(largeData);

        String json = MAPPER.writeValueAsString(input);
        ByteBuffer result = MAPPER.readValue(json, ByteBuffer.class);

        assertNotNull(result);
        byte[] resultBytes = new byte[result.remaining()];
        result.get(resultBytes);
        assertArrayEquals(largeData, resultBytes);
    }
}
