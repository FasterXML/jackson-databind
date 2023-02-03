package com.fasterxml.jackson.databind.node;

import com.fasterxml.jackson.databind.*;

public class ToStringForNodesTest extends BaseMapTest
{
    private final ObjectMapper MAPPER = objectMapper();

    public void testObjectNode() throws Exception
    {
        _verifyToStrings(MAPPER.readTree("{ \"key\" : 1, \"b\" : \"x\", \"array\" : [ 1, false ] }"));
        final ObjectNode n = MAPPER.createObjectNode().put("msg", "hello world");
        assertEquals(MAPPER.writeValueAsString(n), n.toString());
        final String expPretty = MAPPER.writer().withDefaultPrettyPrinter()
                .writeValueAsString(n);
        assertEquals(expPretty, n.toPrettyString());
    }

    public void testArrayNode() throws Exception
    {
        _verifyToStrings(MAPPER.readTree("[ 1, true, null, [ \"abc\",3], { } ]"));
        final ArrayNode n = MAPPER.createArrayNode().add(0.25).add(true);
        assertEquals("[0.25,true]", n.toString());
        assertEquals("[ 0.25, true ]", n.toPrettyString());
    }

    public void testBinaryNode() throws Exception
    {
        _verifyToStrings(MAPPER.getNodeFactory().binaryNode(new byte[] { 1, 2, 3, 4, 6 }));
    }

    protected void _verifyToStrings(JsonNode node) throws Exception
    {
        assertEquals(MAPPER.writeValueAsString(node), node.toString());

        assertEquals(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(node),
                node.toPrettyString());
    }
}
