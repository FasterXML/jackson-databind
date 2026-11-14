package tools.jackson.databind.node;

import java.io.*;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class NodeJDKSerializationTest extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = newJsonMapper();

    /*
    /**********************************************************
    /* Then something bit different; serialize `JsonNode`(s)
    /**********************************************************
     */

    // [databind#18]: Allow JDK serialization of `ObjectNode`
    @Test
    public void testObjectNodeSerialization() throws Exception
    {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("answer", 42);
        ArrayNode arr = root.withArray("/matrix");
        arr.add(1).add(12345678901L).add(true).add("...");
        ObjectNode misc = root.withObject("/misc");
        misc.put("value", 0.25);

        testNodeRoundtrip(root);
    }

    // [databind#18]: Allow JDK serialization of `ArrayNode`
    @Test
    public void testArrayNodeSerialization() throws Exception
    {
        ArrayNode root = MAPPER.createArrayNode();
        root.add(false);
        ObjectNode props = root.addObject();
        props.put("answer", 42);
        root.add(137);

        testNodeRoundtrip(root);
    }

    // [databind#3328]
    @Test
    public void testBigArrayNodeSerialization() throws Exception
    {
        // Try couple of variations just to tease out possible edge cases
        _testBigArrayNodeSerialization(NodeSerialization.LONGEST_EAGER_ALLOC - 39);
        _testBigArrayNodeSerialization(NodeSerialization.LONGEST_EAGER_ALLOC + 1);
        _testBigArrayNodeSerialization(3 * NodeSerialization.LONGEST_EAGER_ALLOC - 1);
        _testBigArrayNodeSerialization(9 * NodeSerialization.LONGEST_EAGER_ALLOC);
    }

    private void _testBigArrayNodeSerialization(int expSize) throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int ix = 0;
        try (JsonGenerator g = MAPPER.createGenerator(out)) {
            g.writeStartArray();
            do {
                g.writeStartObject();
                g.writeNumberProperty("index", ix++);
                g.writeStringProperty("extra", "none#"+ix);
                g.writeEndObject();
            } while (out.size() < expSize);
            g.writeEndArray();
        }

        JsonNode root = MAPPER.readTree(out.toByteArray());
        testNodeRoundtrip(root);
    }

    // and then also some scalar types
    @Test
    public void testScalarSerialization() throws Exception
    {
        testNodeRoundtrip(MAPPER.getNodeFactory().nullNode());

        testNodeRoundtrip(MAPPER.getNodeFactory().stringNode("Foobar"));

        testNodeRoundtrip(MAPPER.getNodeFactory().booleanNode(true));
        testNodeRoundtrip(MAPPER.getNodeFactory().booleanNode(false));

        testNodeRoundtrip(MAPPER.getNodeFactory().numberNode(123));
        testNodeRoundtrip(MAPPER.getNodeFactory().numberNode(-12345678901234L));
    }

    // [databind#6443]: negative length prefix must fail with an IOException
    @Test
    public void testNegativeContentLength() throws Exception
    {
        byte[] ser = jdkSerialize(MAPPER.createObjectNode().put("a", 1));
        byte[] json = "{\"a\":1}".getBytes("UTF-8");
        int idx = _indexOf(ser, json);
        // 4-byte length written by NodeSerialization.writeExternal() precedes the JSON
        ser[idx - 4] = ser[idx - 3] = ser[idx - 2] = ser[idx - 1] = (byte) 0xFF;
        assertThrows(StreamCorruptedException.class, () -> jdkDeserialize(ser));
    }

    // [databind#6443]: corrupt (unparseable) content must also fail with an IOException
    @Test
    public void testCorruptContent() throws Exception
    {
        byte[] ser = jdkSerialize(MAPPER.createObjectNode().put("a", 1));
        byte[] json = "{\"a\":1}".getBytes("UTF-8");
        int idx = _indexOf(ser, json);
        // Replace ':' with 'x', keeping the length unchanged: `{"a"x1}`
        ser[idx + 4] = (byte) 'x';
        InvalidObjectException e = assertThrows(InvalidObjectException.class,
                () -> jdkDeserialize(ser));
        assertInstanceOf(JacksonException.class, e.getCause());
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    protected void testNodeRoundtrip(JsonNode input) throws Exception
    {
        byte[] ser = jdkSerialize(input);
        JsonNode result = jdkDeserialize(ser);
        assertEquals(input, result);
    }

    private static int _indexOf(byte[] b, byte[] sub)
    {
        outer:
        for (int i = 0; i + sub.length <= b.length; i++) {
            for (int j = 0; j < sub.length; j++) {
                if (b[i + j] != sub[j]) continue outer;
            }
            return i;
        }
        throw new IllegalStateException("Content not found");
    }
}
