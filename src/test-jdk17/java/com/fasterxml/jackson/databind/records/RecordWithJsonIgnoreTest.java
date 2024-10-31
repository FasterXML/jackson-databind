package com.fasterxml.jackson.databind.records;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RecordWithJsonIgnoreTest extends DatabindTestUtil
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

    @Test
    public void testSerializeJsonIgnoreRecord() throws Exception {
        String json = MAPPER.writeValueAsString(new RecordWithIgnore(123, "Bob"));
        assertEquals("{\"id\":123}", json);
    }

    @Test
    public void testDeserializeJsonIgnoreRecord() throws Exception {
        RecordWithIgnore value = MAPPER.readValue("{\"id\":123,\"name\":\"Bob\"}",
                RecordWithIgnore.class);
        assertEquals(new RecordWithIgnore(123, null), value);
    }

    /*
    /**********************************************************************
    /* Test methods, JsonIgnore + JsonProperty
    /**********************************************************************
     */

    @Test
    public void testSerializeJsonIgnoreAndJsonPropertyRecord() throws Exception {
        String json = MAPPER.writeValueAsString(new RecordWithIgnoreJsonProperty(123, "Bob"));
        assertEquals("{\"id\":123}", json);
    }

    @Test
    public void testDeserializeJsonIgnoreAndJsonPropertyRecord() throws Exception {
        RecordWithIgnoreJsonProperty value = MAPPER.readValue("{\"id\":123,\"name\":\"Bob\"}", RecordWithIgnoreJsonProperty.class);
        assertEquals(new RecordWithIgnoreJsonProperty(123, "Bob"), value);
    }

    /*
    /**********************************************************************
    /* Test methods, JsonIgnore accessor
    /**********************************************************************
     */

    @Test
    public void testSerializeJsonIgnoreAccessorRecord() throws Exception {
        String json = MAPPER.writeValueAsString(new RecordWithIgnoreAccessor(123, "Bob"));
        assertEquals("{\"id\":123}", json);
    }

    @Test
    public void testDeserializeJsonIgnoreAccessorRecord() throws Exception {
        RecordWithIgnoreAccessor expected = new RecordWithIgnoreAccessor(123, null);

        assertEquals(expected, MAPPER.readValue("{\"id\":123}", RecordWithIgnoreAccessor.class));
        assertEquals(expected, MAPPER.readValue("{\"id\":123,\"name\":null}", RecordWithIgnoreAccessor.class));
        assertEquals(expected, MAPPER.readValue("{\"id\":123,\"name\":\"Bob\"}", RecordWithIgnoreAccessor.class));
    }

    /*
    /**********************************************************************
    /* Test methods, JsonIgnore parameter of primitive type
    /**********************************************************************
     */

    @Test
    public void testSerializeJsonIgnorePrimitiveTypeRecord() throws Exception {
        String json = MAPPER.writeValueAsString(new RecordWithIgnorePrimitiveType(123, "Bob"));
        assertEquals("{\"name\":\"Bob\"}", json);
    }

    @Test
    public void testDeserializeJsonIgnorePrimitiveTypeRecord() throws Exception {
        RecordWithIgnorePrimitiveType value = MAPPER.readValue("{\"id\":123,\"name\":\"Bob\"}", RecordWithIgnorePrimitiveType.class);
        assertEquals(new RecordWithIgnorePrimitiveType(0, "Bob"), value);
    }
}
