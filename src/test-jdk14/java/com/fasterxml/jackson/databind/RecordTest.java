package com.fasterxml.jackson.databind;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.util.ClassUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class RecordTest extends BaseMapTest
{
    record SimpleRecord(int id, String name) {
    }

    record RecordOfRecord(SimpleRecord record) {
    }

    record RecordWithConstructor(int id, String name) {
        public RecordWithConstructor(int id) {
            this(id, "name");
        }
    }

    record JsonIgnoreRecord(int id, @JsonIgnore String name) {
    }

    record JsonPropertyRenameRecord(int id, @JsonProperty("rename")String name) {
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    /*
    /**********************************************************************
    /* Test methods, Record type introspection
    /**********************************************************************
     */

    public void testClassUtil() {
        assertFalse(ClassUtil.isRecordType(getClass()));

        assertTrue(ClassUtil.isRecordType(SimpleRecord.class));
        assertTrue(ClassUtil.isRecordType(RecordOfRecord.class));
        assertTrue(ClassUtil.isRecordType(RecordWithConstructor.class));
        assertTrue(ClassUtil.isRecordType(JsonIgnoreRecord.class));
        assertTrue(ClassUtil.isRecordType(JsonPropertyRenameRecord.class));
    }

    public void testRecordJavaType() {
        assertFalse(MAPPER.constructType(getClass()).isRecordType());

        assertTrue(MAPPER.constructType(SimpleRecord.class).isRecordType());
        assertTrue(MAPPER.constructType(RecordOfRecord.class).isRecordType());
        assertTrue(MAPPER.constructType(RecordWithConstructor.class).isRecordType());
        assertTrue(MAPPER.constructType(JsonIgnoreRecord.class).isRecordType());
        assertTrue(MAPPER.constructType(JsonPropertyRenameRecord.class).isRecordType());
    }

    /*
    /**********************************************************************
    /* Test methods, default reading/writing Record values
    /**********************************************************************
     */

    public void testSerializeSimpleRecord() throws JsonProcessingException {
        SimpleRecord record = new SimpleRecord(123, "Bob");

        String json = MAPPER.writeValueAsString(record);
        final Object EXP = map("id", Integer.valueOf(123), "name", "Bob");
        assertEquals(EXP, MAPPER.readValue(json, Object.class));
    }

    public void testDeserializeSimpleRecord() throws IOException {
        SimpleRecord value = MAPPER.readValue("{\"id\":123,\"name\":\"Bob\"}", SimpleRecord.class);
        assertEquals(new SimpleRecord(123, "Bob"), value);
    }

    public void testSerializeRecordOfRecord() throws JsonProcessingException {
        RecordOfRecord record = new RecordOfRecord(new SimpleRecord(123, "Bob"));
        String json = MAPPER.writeValueAsString(record);
        final Object EXP = Collections.singletonMap("record",
                map("id", Integer.valueOf(123), "name", "Bob"));
        assertEquals(EXP, MAPPER.readValue(json, Object.class));
    }

    /*
    /**********************************************************************
    /* Test methods, reading/writing Record values with different config
    /**********************************************************************
     */

    public void testSerializeSimpleRecord_DisableAnnotationIntrospector() throws JsonProcessingException {
        SimpleRecord record = new SimpleRecord(123, "Bob");

        JsonMapper mapper = JsonMapper.builder()
                .configure(MapperFeature.USE_ANNOTATIONS, false)
                .build();
        String json = mapper.writeValueAsString(record);

        assertEquals("{\"id\":123,\"name\":\"Bob\"}", json);
    }

    public void testDeserializeSimpleRecord_DisableAnnotationIntrospector() throws IOException {
        JsonMapper mapper = JsonMapper.builder()
                .configure(MapperFeature.USE_ANNOTATIONS, false)
                .build();
        SimpleRecord value = mapper.readValue("{\"id\":123,\"name\":\"Bob\"}", SimpleRecord.class);

        assertEquals(new SimpleRecord(123, "Bob"), value);
    }

    /*
    /**********************************************************************
    /* Test methods, reading/writing Record values with annotations
    /**********************************************************************
     */
    
    public void testSerializeJsonIgnoreRecord() throws JsonProcessingException {
        JsonIgnoreRecord record = new JsonIgnoreRecord(123, "Bob");

        String json = MAPPER.writeValueAsString(record);

        assertEquals("{\"id\":123}", json);
    }

    public void testDeserializeRecordWithConstructor() throws IOException {
        RecordWithConstructor value = MAPPER.readValue("{\"id\":123,\"name\":\"Bob\"}",
                RecordWithConstructor.class);
        assertEquals(new RecordWithConstructor(123, "Bob"), value);
    }

    public void testSerializeJsonRenameRecord() throws JsonProcessingException {
        JsonPropertyRenameRecord record = new JsonPropertyRenameRecord(123, "Bob");

        String json = MAPPER.writeValueAsString(record);
        final Object EXP = map("id", Integer.valueOf(123), "rename", "Bob");
        assertEquals(EXP, MAPPER.readValue(json, Object.class));
    }

    public void testDeserializeJsonRenameRecord() throws IOException {
        JsonPropertyRenameRecord value = MAPPER.readValue("{\"id\":123,\"rename\":\"Bob\"}",
                JsonPropertyRenameRecord.class);
        assertEquals(new JsonPropertyRenameRecord(123, "Bob"), value);
    }

    private Map<String,Object> map(String key1, Object value1,
            String key2, Object value2) {
        final Map<String, Object> result = new LinkedHashMap<>();
        result.put(key1, value1);
        result.put(key2, value2);
        return result;
    }
}
