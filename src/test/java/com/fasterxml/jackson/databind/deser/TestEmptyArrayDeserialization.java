package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * This unit test suite tries to verify that the "Native" java type
 * mapper can properly re-construct Java array objects from Json arrays.
 */
public class TestEmptyArrayDeserialization
        extends BaseMapTest {

    private static final String EMPTY_OBJECT = "{}";


    private static class ObjectArrayWrapper {
        public Object[] wrapped;
    }

    private static class StringArrayWrapper {
        public String[] wrapped;
    }

    private static class HiddenBinaryBean890 {
        @JsonDeserialize(as = byte[].class)
        public Object someBytes;
    }

    private final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        MAPPER.configure(DeserializationFeature.READ_NULL_AS_EMPTY_COLLECTION, true);
    }


    public void testNullArray() throws Exception {
        Object[] result = MAPPER.readValue("null", new TypeReference<Object[]>() {});

        assertNotNull(result);
        assertTrue(result.length == 0);
    }

    public void testNullStringArray() throws Exception {
        String[] result = MAPPER.readValue("null", new TypeReference<String[]>() {});

        assertNotNull(result);
        assertTrue(result.length == 0);
    }

    public void testEmptyBeanArray() throws Exception {
        ObjectArrayWrapper result = MAPPER.readValue(EMPTY_OBJECT, new TypeReference<ObjectArrayWrapper>() {});

        assertNotNull(result.wrapped);
        assertTrue(result.wrapped.length == 0);
    }

    public void testEmptyBeanStringArray() throws Exception {
        StringArrayWrapper result = MAPPER.readValue(EMPTY_OBJECT, new TypeReference<StringArrayWrapper>() {});

        assertNotNull(result.wrapped);
        assertTrue(result.wrapped.length == 0);
    }

    public void testNullBeanArray() throws Exception {
        String JSON = "{\"wrapped\": null}";

        ObjectArrayWrapper result = MAPPER.readValue(JSON, new TypeReference<ObjectArrayWrapper>() {
        });

        assertNotNull(result.wrapped);
        assertTrue(result.wrapped.length == 0);
    }

    /*
    /**********************************************************
    /* And special cases for byte array (base64 encoded)
    /**********************************************************
     */

    // for [databind#890]
    public void testEmptyByteArrayTypeOverride890() throws Exception {
        HiddenBinaryBean890 result = MAPPER.readValue(
                EMPTY_OBJECT, HiddenBinaryBean890.class);
        assertNotNull(result);
        assertNotNull(result.someBytes);
        assertEquals(byte[].class, result.someBytes.getClass());
        assertTrue(((byte[]) result.someBytes).length == 0);
    }

    // for [databind#890]
    public void testNullByteArrayTypeOverride890() throws Exception {
        HiddenBinaryBean890 result = MAPPER.readValue(
                "{\"someBytes\": null}", HiddenBinaryBean890.class);
        assertNotNull(result);
        assertNotNull(result.someBytes);
        assertEquals(byte[].class, result.someBytes.getClass());
        assertTrue(((byte[]) result.someBytes).length == 0);
    }
}
