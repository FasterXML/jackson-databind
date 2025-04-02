package com.fasterxml.jackson.databind.tofix;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class RecordWithReadOnly5049Test extends DatabindTestUtil
{
    record ReadOnly5049Record(
            @JsonProperty(value = "a", access = JsonProperty.Access.READ_ONLY) String a,
            @JsonProperty(value = "b", access = JsonProperty.Access.READ_ONLY) String b) {
    }

    static class ReadOnly5049Pojo
    {
        protected String a, b;

        ReadOnly5049Pojo(
            @JsonProperty(value = "a", access = JsonProperty.Access.READ_ONLY) String a,
            @JsonProperty(value = "b", access = JsonProperty.Access.READ_ONLY) String b) {
            this.a = a;
            this.b = b;
        }

        public String getA() { return a; }
        public String getB() { return b; }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    void testRoundtripRecord() throws Exception
    {
        // json = MAPPER.writeValueAsString(new ReadOnly5049Record("hello", "world"));
        String json = "{\"a\":\"hello\",\"b\":\"world\"}";
        ReadOnly5049Record record = MAPPER.readValue(json, ReadOnly5049Record.class);
        assertNotNull(record);
        assertNull(record.a());
        assertNull(record.b());
    } 

    @Test
    void testRoundtripPOJO() throws Exception
    {
        String json = MAPPER.writeValueAsString(new ReadOnly5049Pojo("hello", "world"));
        //System.err.println("JSON/pojo: "+json);
        ReadOnly5049Pojo pojo = MAPPER.readerFor(ReadOnly5049Pojo.class).readValue(json);
        assertNotNull(pojo);
        assertNull(pojo.a);
        assertNull(pojo.b);
    } 
}
