package com.fasterxml.jackson.databind.node;

import java.io.*;

import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class NodeJDKSerializationTest extends BaseMapTest
{
    private final ObjectMapper MAPPER = newJsonMapper();

    /*
    /**********************************************************
    /* Then something bit different; serialize `JsonNode`(s)
    /**********************************************************
     */

    // [databind#18]: Allow JDK serialization of `ObjectNode`
    public void testObjectNodeSerialization() throws Exception
    {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("answer", 42);
        ArrayNode arr = root.withArray("matrix");
        arr.add(1).add(12345678901L).add(true).add("...");
        ObjectNode misc = root.withObject("/misc");
        misc.put("value", 0.25);

        testNodeRoundtrip(root);
    }

    // [databind#18]: Allow JDK serialization of `ArrayNode`
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
                g.writeNumberField("index", ix++);
                g.writeStringField("extra", "none#"+ix);
                g.writeEndObject();
            } while (out.size() < expSize);

            g.writeEndArray();
        }

        JsonNode root = MAPPER.readTree(out.toByteArray());

        testNodeRoundtrip(root);
    }

    // and then also some scalar types
    public void testScalarSerialization() throws Exception
    {
        testNodeRoundtrip(MAPPER.getNodeFactory().nullNode());

        testNodeRoundtrip(MAPPER.getNodeFactory().textNode("Foobar"));

        testNodeRoundtrip(MAPPER.getNodeFactory().booleanNode(true));
        testNodeRoundtrip(MAPPER.getNodeFactory().booleanNode(false));

        testNodeRoundtrip(MAPPER.getNodeFactory().numberNode(123));
        testNodeRoundtrip(MAPPER.getNodeFactory().numberNode(-12345678901234L));
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
}
