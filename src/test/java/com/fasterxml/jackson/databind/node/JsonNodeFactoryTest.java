package com.fasterxml.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;

public class JsonNodeFactoryTest extends NodeTestBase
{
    private final ObjectMapper MAPPER = objectMapper();

    static class SortingNodeFactory extends JsonNodeFactory {
        private static final long serialVersionUID = 1L;

        @Override
        public ObjectNode objectNode() {
            return new ObjectNode(this, new TreeMap<String, JsonNode>());
        }
    }

    public void testSimpleCreation()
    {
        JsonNodeFactory f = MAPPER.getNodeFactory();
        JsonNode n;

        n = f.numberNode((byte) 4);
        assertTrue(n.isInt());
        assertEquals(4, n.intValue());

        assertTrue(f.numberNode((Byte) null).isNull());

        assertTrue(f.numberNode((Short) null).isNull());

        assertTrue(f.numberNode((Integer) null).isNull());

        assertTrue(f.numberNode((Long) null).isNull());

        assertTrue(f.numberNode((Float) null).isNull());

        assertTrue(f.numberNode((Double) null).isNull());

        assertTrue(f.numberNode((BigDecimal) null).isNull());

        assertTrue(f.numberNode((BigInteger) null).isNull());

        assertTrue(f.missingNode().isMissingNode());
    }

    // 09-Sep-2020, tatu: Let's try something more useful: auto-sorting
    //    Tree Model!

   public void testSortingObjectNode() throws Exception
   {
       final String SIMPLE_INPUT = "{\"b\":2,\"a\":1}";
       
       // First, by default, ordering retained:
       assertEquals(SIMPLE_INPUT,
               MAPPER.writeValueAsString(MAPPER.readTree(SIMPLE_INPUT)));

       // But can change easily
       ObjectMapper mapper = JsonMapper.builder()
               .nodeFactory(new SortingNodeFactory())
               .build();
       JsonNode sort = mapper.readTree(SIMPLE_INPUT);
       assertEquals("{\"a\":1,\"b\":2}", MAPPER.writeValueAsString(sort));

       final String BIGGER_INPUT = a2q("['x',{'b':1,'c':true,'a':3},false]");
       final String BIGGER_OUTPUT = a2q("['x',{'a':3,'b':1,'c':true},false]");

       assertEquals(BIGGER_OUTPUT,
               MAPPER.writeValueAsString(mapper.readTree(BIGGER_INPUT)));
   }
}
