package com.fasterxml.jackson.databind.records;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RecordWithJsonIgnoreTest extends BaseMapTest
{
    record RecordWithIgnore(int id, @JsonIgnore String name) {
    }

    record RecordWithIgnoreJsonProperty(int id, @JsonIgnore @JsonProperty("name") String name) {
    }

    record RecordWithIgnoreAccessor(int id, String name) {

        @JsonIgnore
        @Override
        public String name() {
            return name;
        }
    }

    record RecordWithIgnorePrimitiveType(@JsonIgnore int id, String name) {
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    /*
    /**********************************************************************
    /* Test methods, JsonIgnore
    /**********************************************************************
     */

    public void testSerializeJsonIgnoreRecord() throws Exception {
        String json = MAPPER.writeValueAsString(new RecordWithIgnore(123, "Bob"));
        assertEquals("{\"id\":123}", json);
    }

    public void testDeserializeJsonIgnoreRecord() throws Exception {
        RecordWithIgnore value = MAPPER.readValue("{\"id\":123,\"name\":\"Bob\"}", RecordWithIgnore.class);
        assertEquals(new RecordWithIgnore(123, null), value);
    }

    /*
    /**********************************************************************
    /* Test methods, JsonIgnore + JsonProperty
    /**********************************************************************
     */

    public void testSerializeJsonIgnoreAndJsonPropertyRecord() throws Exception {
        String json = MAPPER.writeValueAsString(new RecordWithIgnoreJsonProperty(123, "Bob"));
        assertEquals("{\"id\":123}", json);
    }

    public void testDeserializeJsonIgnoreAndJsonPropertyRecord() throws Exception {
        RecordWithIgnoreJsonProperty value = MAPPER.readValue("{\"id\":123,\"name\":\"Bob\"}", RecordWithIgnoreJsonProperty.class);
        assertEquals(new RecordWithIgnoreJsonProperty(123, "Bob"), value);
    }

    /*
    /**********************************************************************
    /* Test methods, JsonIgnore accessor
    /**********************************************************************
     */

    public void testSerializeJsonIgnoreAccessorRecord() throws Exception {
        String json = MAPPER.writeValueAsString(new RecordWithIgnoreAccessor(123, "Bob"));
        assertEquals("{\"id\":123}", json);
    }

    public void testDeserializeJsonIgnoreAccessorRecord() throws Exception {
        RecordWithIgnoreAccessor value = MAPPER.readValue("{\"id\":123,\"name\":\"Bob\"}", RecordWithIgnoreAccessor.class);
        assertEquals(new RecordWithIgnoreAccessor(123, null), value);
    }

    /*
    /**********************************************************************
    /* Test methods, JsonIgnore parameter of primitive type
    /**********************************************************************
     */

    public void testSerializeJsonIgnorePrimitiveTypeRecord() throws Exception {
        String json = MAPPER.writeValueAsString(new RecordWithIgnorePrimitiveType(123, "Bob"));
        assertEquals("{\"name\":\"Bob\"}", json);
    }

    public void testDeserializeJsonIgnorePrimitiveTypeRecord() throws Exception {
        RecordWithIgnorePrimitiveType value = MAPPER.readValue("{\"id\":123,\"name\":\"Bob\"}", RecordWithIgnorePrimitiveType.class);
        assertEquals(new RecordWithIgnorePrimitiveType(0, "Bob"), value);
    }
}
