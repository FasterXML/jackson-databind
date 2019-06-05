package com.fasterxml.jackson.databind;

import java.io.*;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TestNodeJDKSerialization extends BaseMapTest
{
    private final ObjectMapper MAPPER = newObjectMapper();

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
        ObjectNode misc = root.with("misc");
        misc.put("value", 0.25);

        byte[] ser = jdkSerialize(root);
        JsonNode result = jdkDeserialize(ser);
        assertEquals(root, result);
    }

    // [databind#18]: Allow JDK serialization of `ArrayNode`
    public void testArrayNodeSerialization() throws Exception
    {
        ArrayNode root = MAPPER.createArrayNode();
        root.add(false);
        ObjectNode props = root.addObject();
        props.put("answer", 42);
        root.add(137);

        byte[] ser = jdkSerialize(root);
        JsonNode result = jdkDeserialize(ser);
        assertEquals(root, result);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */
    
    protected byte[] jdkSerialize(Object o) throws IOException
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(1000);
        ObjectOutputStream obOut = new ObjectOutputStream(bytes);
        obOut.writeObject(o);
        obOut.close();
        return bytes.toByteArray();
    }

    @SuppressWarnings("unchecked")
    protected <T> T jdkDeserialize(byte[] raw) throws IOException
    {
        ObjectInputStream objIn = new ObjectInputStream(new ByteArrayInputStream(raw));
        try {
            return (T) objIn.readObject();
        } catch (ClassNotFoundException e) {
            fail("Missing class: "+e.getMessage());
            return null;
        } finally {
            objIn.close();
        }
    }
}
