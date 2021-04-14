package com.fasterxml.jackson.failing;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.fasterxml.jackson.databind.*;

public class RecursiveIgnoreProperties1755Test extends BaseMapTest
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

    // for [databind#1755]

    private final ObjectMapper MAPPER = newJsonMapper();

    public void testRecursiveIgnore1755() throws Exception
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
}
