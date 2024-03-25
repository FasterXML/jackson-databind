package com.fasterxml.jackson.failing;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RecursiveIgnoreProperties1755Test extends DatabindTestUtil
{
    // for [databind#1755]
    static class JackBase1755 {
        public String id;
    }

    static class JackExt extends JackBase1755 {
        public BigDecimal quantity;
        public String ignoreMe;

        @JsonIgnoreProperties({"ignoreMe"})
        public List<JackExt> linked;

        public List<KeyValue> metadata;
    }

    static class KeyValue {
        public String key;
        public String value;
    }

    // for [databind#4417]
    static class Item4417 {
        @JsonIgnoreProperties({ "whatever" })
        public List<Item4417> items;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // for [databind#1755]
    @Test
    void recursiveIgnore1755() throws Exception
    {
        final String JSON = a2q("{\n"
                +"'id': '1',\n"
                +"'quantity': 5,\n"
                +"'ignoreMe': 'yzx',\n"
                +"'metadata': [\n"
                +"           {\n"
                +"              'key': 'position',\n"
                +"              'value': '2'\n"
                +"          }\n"
                +"       ],\n"
                +"'linked': [\n"
                +"     {\n"
                +"         'id': '1',\n"
                +"         'quantity': 5,\n"
                +"         'ignoreMe': 'yzx',\n"
                +"         'metadata': [\n"
                +"          {\n"
                +"              'key': 'position',\n"
                +"             'value': '2'\n"
                +"         }\n"
                +"     ]\n"
                +"   }\n"
                +"  ]\n"
                +"}");
        JackExt value = MAPPER.readValue(JSON, JackExt.class);
        assertNotNull(value);
    }

    // for [databind#4417]
    @Test
    void recursiveIgnore4417() throws Exception
    {
        Item4417 result = MAPPER.readValue(a2q("{'items': [{'items': []}]}"),
                Item4417.class);
        assertEquals(1, result.items.size(), 1);
    }
}
