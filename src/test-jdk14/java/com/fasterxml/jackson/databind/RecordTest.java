package com.fasterxml.jackson.databind;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.IOException;

public class RecordTest extends BaseMapTest {

    private JsonMapper jsonMapper;

    public void setUp() {
        jsonMapper = new JsonMapper();
    }

    record SimpleRecord(int id, String name) {
    }

    public void testSerializeSimpleRecord() throws JsonProcessingException {
        SimpleRecord record = new SimpleRecord(123, "Bob");

        String json = jsonMapper.writeValueAsString(record);

        assertEquals("{\"id\":123,\"name\":\"Bob\"}", json);
    }

    public void testDeserializeSimpleRecord() throws IOException {
        SimpleRecord value = jsonMapper.readValue("{\"id\":123,\"name\":\"Bob\"}", SimpleRecord.class);

        assertEquals(new SimpleRecord(123, "Bob"), value);
    }

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

    record RecordOfRecord(SimpleRecord record) {
    }

    public void testSerializeRecordOfRecord() throws JsonProcessingException {
        RecordOfRecord record = new RecordOfRecord(new SimpleRecord(123, "Bob"));

        String json = jsonMapper.writeValueAsString(record);

        assertEquals("{\"record\":{\"id\":123,\"name\":\"Bob\"}}", json);
    }

    record JsonIgnoreRecord(int id, @JsonIgnore String name) {
    }

    public void testSerializeJsonIgnoreRecord() throws JsonProcessingException {
        JsonIgnoreRecord record = new JsonIgnoreRecord(123, "Bob");

        String json = jsonMapper.writeValueAsString(record);

        assertEquals("{\"id\":123}", json);
    }

    record RecordWithConstructor(int id, String name) {
        public RecordWithConstructor(int id) {
            this(id, "name");
        }
    }

    public void testDeserializeRecordWithConstructor() throws IOException {
        RecordWithConstructor value = jsonMapper.readValue("{\"id\":123,\"name\":\"Bob\"}", RecordWithConstructor.class);

        assertEquals(new RecordWithConstructor(123, "Bob"), value);
    }

    record JsonPropertyRenameRecord(int id, @JsonProperty("rename")String name) {
    }

    public void testSerializeJsonRenameRecord() throws JsonProcessingException {
        JsonPropertyRenameRecord record = new JsonPropertyRenameRecord(123, "Bob");

        String json = jsonMapper.writeValueAsString(record);

        assertEquals("{\"id\":123,\"rename\":\"Bob\"}", json);
    }

    public void testDeserializeJsonRenameRecord() throws IOException {
        JsonPropertyRenameRecord value = jsonMapper.readValue("{\"id\":123,\"rename\":\"Bob\"}", JsonPropertyRenameRecord.class);

        assertEquals(new JsonPropertyRenameRecord(123, "Bob"), value);
    }
}
