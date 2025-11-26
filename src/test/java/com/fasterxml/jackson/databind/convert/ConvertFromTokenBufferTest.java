package com.fasterxml.jackson.databind.convert;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import com.fasterxml.jackson.databind.util.TokenBuffer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ObjectMapper#convertValue(Object, Class)} optimization
 * when the source value is already a {@link TokenBuffer}.
 * See [databind#5368]
 */
public class ConvertFromTokenBufferTest extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = newJsonMapper();

    static class SimpleBean {
        public int x;
        public String name;

        public SimpleBean() { }

        public SimpleBean(int x, String name) {
            this.x = x;
            this.name = name;
        }
    }

    // [databind#5368]: Should reuse TokenBuffer directly without re-serializing
    @Test
    public void testConvertTokenBufferToBean() throws Exception
    {
        TokenBuffer buf = _beanToBuffer(42, "test");
        SimpleBean result = MAPPER.convertValue(buf, SimpleBean.class);
        assertEquals(42, result.x);
        assertEquals("test", result.name);
    }

    // [databind#5368]: Test with JavaType
    @Test
    public void testConvertTokenBufferToJavaType() throws Exception
    {
        TokenBuffer buf = _beanToBuffer(123, "javatype");
        JavaType type = MAPPER.getTypeFactory().constructType(SimpleBean.class);
        SimpleBean result = MAPPER.convertValue(buf, type);
        assertEquals(123, result.x);
        assertEquals("javatype", result.name);
    }

    // [databind#5368]: Test with TypeReference
    @Test
    public void testConvertTokenBufferToTypeReference() throws Exception
    {
        TokenBuffer buf = _beanToBuffer(456, "foobar");
        SimpleBean result = MAPPER.convertValue(buf,
                new TypeReference<SimpleBean>() {});
        assertEquals(456, result.x);
        assertEquals("foobar", result.name);
    }

    private TokenBuffer _beanToBuffer(int x, String name) throws Exception {
        TokenBuffer buf = new TokenBuffer(MAPPER, false);
        buf.writeStartObject();
        buf.writeNumberField("x", x);
        buf.writeStringField("name", name);
        buf.writeEndObject();
        buf.close();
        return buf;
    }
    
    // [databind#5368]: Test with Map
    @Test
    public void testConvertTokenBufferToMap() throws Exception
    {
        TokenBuffer buf = new TokenBuffer(MAPPER, false);
        buf.writeStartObject();
        buf.writeStringField("key1", "value1");
        buf.writeNumberField("key2", 42);
        buf.writeEndObject();
        buf.close();

        @SuppressWarnings("unchecked")
        Map<String, Object> result = MAPPER.convertValue(buf, Map.class);

        assertNotNull(result);
        assertEquals("value1", result.get("key1"));
        assertEquals(42, result.get("key2"));
    }

    // [databind#5368]: Test with null TokenBuffer value
    @Test
    public void testConvertNullTokenBuffer() throws Exception
    {
        TokenBuffer buf = new TokenBuffer(MAPPER, false);
        buf.writeNull();
        buf.close();

        SimpleBean result = MAPPER.convertValue(buf, SimpleBean.class);
        assertNull(result);
    }

    // [databind#5368]: Test with array in TokenBuffer
    @Test
    public void testConvertTokenBufferArray() throws Exception
    {
        TokenBuffer buf = new TokenBuffer(MAPPER, false);
        buf.writeStartArray();

        buf.writeStartObject();
        buf.writeNumberField("x", 1);
        buf.writeStringField("name", "first");
        buf.writeEndObject();

        buf.writeStartObject();
        buf.writeNumberField("x", 2);
        buf.writeStringField("name", "second");
        buf.writeEndObject();

        buf.writeEndArray();
        buf.close();

        SimpleBean[] result = MAPPER.convertValue(buf, SimpleBean[].class);

        assertNotNull(result);
        assertEquals(2, result.length);
        assertEquals(1, result[0].x);
        assertEquals("first", result[0].name);
        assertEquals(2, result[1].x);
        assertEquals("second", result[1].name);
    }

    // [databind#5368]: Verify TokenBuffer can still be used after conversion
    // (asParser() creates new stateful instance)
    @Test
    public void testTokenBufferReusableAfterConvert() throws Exception
    {
        TokenBuffer buf = new TokenBuffer(MAPPER, false);
        buf.writeStartObject();
        buf.writeNumberField("x", 99);
        buf.writeStringField("name", "reusable");
        buf.writeEndObject();
        buf.close();

        // First conversion
        SimpleBean result1 = MAPPER.convertValue(buf, SimpleBean.class);
        assertNotNull(result1);
        assertEquals(99, result1.x);

        // Second conversion - should work because asParser() creates new state
        SimpleBean result2 = MAPPER.convertValue(buf, SimpleBean.class);
        assertNotNull(result2);
        assertEquals(99, result2.x);
        assertEquals("reusable", result2.name);
    }

    // [databind#5368]: Test with USE_BIG_DECIMAL_FOR_FLOATS feature
    @Test
    public void testConvertTokenBufferWithBigDecimalFeature() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .build();

        TokenBuffer buf = new TokenBuffer(mapper, false);
        buf.writeStartObject();
        buf.writeStringField("value", "test");
        buf.writeEndObject();
        buf.close();

        @SuppressWarnings("unchecked")
        Map<String, Object> result = mapper.convertValue(buf, Map.class);

        assertNotNull(result);
        assertEquals("test", result.get("value"));
    }

    // [databind#5368]: Ensure regular (non-TokenBuffer) conversion still works
    @Test
    public void testConvertFromRegularObjectStillWorks() throws Exception
    {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("x", 77);
        map.put("name", "regular");

        SimpleBean result = MAPPER.convertValue(map, SimpleBean.class);

        assertNotNull(result);
        assertEquals(77, result.x);
        assertEquals("regular", result.name);
    }

    // [databind#5368]: Test with empty TokenBuffer
    @Test
    public void testConvertEmptyTokenBuffer() throws Exception
    {
        TokenBuffer buf = new TokenBuffer(MAPPER, false);
        buf.writeStartObject();
        buf.writeEndObject();
        buf.close();

        SimpleBean result = MAPPER.convertValue(buf, SimpleBean.class);

        assertNotNull(result);
        assertEquals(0, result.x);
        assertNull(result.name);
    }
}
