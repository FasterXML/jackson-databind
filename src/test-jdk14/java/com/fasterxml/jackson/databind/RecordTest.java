package com.fasterxml.jackson.databind;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.util.ClassUtil;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class RecordTest extends BaseMapTest
{
    record SimpleRecord(int id, String name) { }

    record RecordOfRecord(SimpleRecord record) { }

    record RecordWithCanonicalCtorOverride(int id, String name) {
        public RecordWithCanonicalCtorOverride(int id, String name) {
            this.id = id;
            this.name = "name";
        }
    }

    record RecordWithAltCtor(int id, String name) {
        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public RecordWithAltCtor(@JsonProperty("id") int id) {
            this(id, "name2");
        }
    }

    record RecordWithIgnore(int id, @JsonIgnore String name) { }

    record RecordWithRename(int id, @JsonProperty("rename")String name) { }

    record EmptyRecord() { }

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
        assertTrue(ClassUtil.isRecordType(RecordWithIgnore.class));
        assertTrue(ClassUtil.isRecordType(RecordWithRename.class));
    }

    public void testRecordJavaType() {
        assertFalse(MAPPER.constructType(getClass()).isRecordType());

        assertTrue(MAPPER.constructType(SimpleRecord.class).isRecordType());
        assertTrue(MAPPER.constructType(RecordOfRecord.class).isRecordType());
        assertTrue(MAPPER.constructType(RecordWithIgnore.class).isRecordType());
        assertTrue(MAPPER.constructType(RecordWithRename.class).isRecordType());
    }

    /*
    /**********************************************************************
    /* Test methods, default reading/writing Record values
    /**********************************************************************
     */

    public void testSerializeSimpleRecord() throws Exception {
        String json = MAPPER.writeValueAsString(new SimpleRecord(123, "Bob"));
        final Object EXP = map("id", Integer.valueOf(123), "name", "Bob");
        assertEquals(EXP, MAPPER.readValue(json, Object.class));
    }

    public void testDeserializeSimpleRecord() throws Exception {
        assertEquals(new SimpleRecord(123, "Bob"),
                MAPPER.readValue("{\"id\":123,\"name\":\"Bob\"}", SimpleRecord.class));
    }

    public void testSerializeEmptyRecord() throws Exception {
        assertEquals("{}", MAPPER.writeValueAsString(new EmptyRecord()));
    }

    public void testDeserializeEmptyRecord() throws Exception {
        assertEquals(new EmptyRecord(),
                MAPPER.readValue("{}", EmptyRecord.class));
    }

    public void testSerializeRecordOfRecord() throws Exception {
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

    public void testSerializeSimpleRecord_DisableAnnotationIntrospector() throws Exception {
        SimpleRecord record = new SimpleRecord(123, "Bob");

        JsonMapper mapper = JsonMapper.builder()
                .configure(MapperFeature.USE_ANNOTATIONS, false)
                .build();
        String json = mapper.writeValueAsString(record);

        assertEquals("{\"id\":123,\"name\":\"Bob\"}", json);
    }

    public void testDeserializeSimpleRecord_DisableAnnotationIntrospector() throws Exception {
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
    
    public void testSerializeJsonIgnoreRecord() throws Exception {
        String json = MAPPER.writeValueAsString(new RecordWithIgnore(123, "Bob"));
        assertEquals("{\"id\":123}", json);
    }

    public void testDeserializeWithCanonicalCtorOverride() throws Exception {
        RecordWithCanonicalCtorOverride value = MAPPER.readValue("{\"id\":123,\"name\":\"Bob\"}",
                RecordWithCanonicalCtorOverride.class);
        assertEquals(123, value.id());
        assertEquals("name", value.name());
    }

    public void testDeserializeWithAltCtor() throws Exception {
        RecordWithAltCtor value = MAPPER.readValue("{\"id\":2812}",
                RecordWithAltCtor.class);
        assertEquals(2812, value.id());
        assertEquals("name2", value.name());
    }

    public void testSerializeJsonRename() throws Exception {
        String json = MAPPER.writeValueAsString(new RecordWithRename(123, "Bob"));
        final Object EXP = map("id", Integer.valueOf(123), "rename", "Bob");
        assertEquals(EXP, MAPPER.readValue(json, Object.class));
    }

    public void testDeserializeJsonRename() throws Exception {
        RecordWithRename value = MAPPER.readValue("{\"id\":123,\"rename\":\"Bob\"}",
                RecordWithRename.class);
        assertEquals(new RecordWithRename(123, "Bob"), value);
    }

    /*
    /**********************************************************************
    /* Internal helper methods
    /**********************************************************************
     */

    private Map<String,Object> map(String key1, Object value1,
            String key2, Object value2) {
        final Map<String, Object> result = new LinkedHashMap<>();
        result.put(key1, value1);
        result.put(key2, value2);
        return result;
    }
}
