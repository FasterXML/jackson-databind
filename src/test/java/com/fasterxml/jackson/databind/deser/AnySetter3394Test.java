package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.a2q;
import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;

// for [databind#3394]
public class AnySetter3394Test
{
    static class AnySetter3394Bean {
        public int id;

        @JsonAnySetter
        public JsonNode extraData = new ObjectNode(null);
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testAnySetterWithJsonNode() throws Exception
    {
        final String DOC = a2q("{'test':3,'nullable':null,'id':42,'value':true}");
        AnySetter3394Bean bean = MAPPER.readValue(DOC, AnySetter3394Bean.class);
        assertEquals(a2q("{'test':3,'nullable':null,'value':true}"),
                ""+bean.extraData);
        assertEquals(42, bean.id);
    }
}
