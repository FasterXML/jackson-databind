package tools.jackson.databind.node;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class ToStringForNodesTest extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = objectMapper();

    @Test
    public void testObjectNode() throws Exception
    {
        _verifyToStrings(MAPPER.readTree("{ \"key\" : 1, \"b\" : \"x\", \"array\" : [ 1, false ] }"));
        final ObjectNode n = MAPPER.createObjectNode().put("msg", "hello world");
        assertEquals(MAPPER.writeValueAsString(n), n.toString());
        final String expPretty = MAPPER.writer().withDefaultPrettyPrinter()
                .writeValueAsString(n);
        assertEquals(expPretty, n.toPrettyString());
    }

    @Test
    public void testArrayNode() throws Exception
    {
        _verifyToStrings(MAPPER.readTree("[ 1, true, null, [ \"abc\",3], { } ]"));
        final ArrayNode n = MAPPER.createArrayNode().add(0.25).add(true);
        assertEquals("[0.25,true]", n.toString());
        assertEquals("[ 0.25, true ]", n.toPrettyString());
    }

    @Test
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
