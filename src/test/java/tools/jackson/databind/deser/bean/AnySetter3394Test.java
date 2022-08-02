package tools.jackson.databind.deser.bean;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import tools.jackson.databind.*;
import tools.jackson.databind.node.ObjectNode;

// for [databind#3394]
public class AnySetter3394Test extends BaseMapTest
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

    public void testAnySetterWithJsonNode() throws Exception
    {
        final String DOC = a2q("{'test':3,'nullable':null,'id':42,'value':true}");
        AnySetter3394Bean bean = MAPPER.readValue(DOC, AnySetter3394Bean.class);
        assertEquals(a2q("{'test':3,'nullable':null,'value':true}"),
                ""+bean.extraData);
        assertEquals(42, bean.id);
    }
}
