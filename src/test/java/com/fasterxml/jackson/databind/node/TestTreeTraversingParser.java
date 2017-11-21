package com.fasterxml.jackson.databind.node;

import java.util.*;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.node.BinaryNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.POJONode;
import com.fasterxml.jackson.databind.node.TextNode;

public class TestTreeTraversingParser
    extends BaseMapTest
{
    static class Person {
        public String name;
        public int magicNumber;
        public List<String> kids;
    }

    @JsonIgnoreProperties(ignoreUnknown=true)
    public static class Jackson370Bean {
        public Inner inner;
    }

    public static class Inner {
        public String value;
    }
    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    public void testSimple() throws Exception
    {
        // For convenience, parse tree from JSON first
        final String JSON =
            "{ \"a\" : 123, \"list\" : [ 12.25, null, true, { }, [ ] ] }";
        ObjectMapper m = new ObjectMapper();
        JsonNode tree = m.readTree(JSON);
        JsonParser p = tree.traverse(ObjectReadContext.empty());

        assertNull(p.currentToken());
        assertNull(p.currentName());

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertNull(p.currentName());
        assertEquals("Expected START_OBJECT", JsonToken.START_OBJECT.asString(), p.getText());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("a", p.currentName());
        assertEquals("a", p.getText());

        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals("a", p.currentName());
        assertEquals(123, p.getIntValue());
        assertEquals("123", p.getText());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("list", p.currentName());
        assertEquals("list", p.getText());

        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertEquals("list", p.currentName());
        assertEquals(JsonToken.START_ARRAY.asString(), p.getText());

        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertNull(p.currentName());
        assertEquals(12.25, p.getDoubleValue(), 0);
        assertEquals("12.25", p.getText());

        assertToken(JsonToken.VALUE_NULL, p.nextToken());
        assertNull(p.currentName());
        assertEquals(JsonToken.VALUE_NULL.asString(), p.getText());

        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertNull(p.currentName());
        assertTrue(p.getBooleanValue());
        assertEquals(JsonToken.VALUE_TRUE.asString(), p.getText());

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertNull(p.currentName());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertNull(p.currentName());

        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertNull(p.currentName());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertNull(p.currentName());

        assertToken(JsonToken.END_ARRAY, p.nextToken());

        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertNull(p.currentName());

        assertNull(p.nextToken());

        p.close();
        assertTrue(p.isClosed());
    }

    public void testArray() throws Exception
    {
        // For convenience, parse tree from JSON first
        ObjectMapper m = new ObjectMapper();

        JsonParser p = m.readTree("[]").traverse(ObjectReadContext.empty());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();

        p = m.readTree("[[]]").traverse(ObjectReadContext.empty());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();

        p = m.readTree("[[ 12.1 ]]").traverse(ObjectReadContext.empty());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();
    }
    
    public void testNested() throws Exception
    {
        // For convenience, parse tree from JSON first
        final String JSON =
            "{\"coordinates\":[[[-3,\n1],[179.859681,51.175092]]]}"
            ;
        ObjectMapper m = new ObjectMapper();
        JsonNode tree = m.readTree(JSON);
        JsonParser p = tree.traverse(ObjectReadContext.empty());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());

        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.START_ARRAY, p.nextToken());

        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());

        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());

        assertToken(JsonToken.END_OBJECT, p.nextToken());
        p.close();
    }
    
    /**
     * Unit test that verifies that we can (re)parse sample document
     * from JSON specification.
     */
    public void testSpecDoc() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        JsonNode tree = m.readTree(SAMPLE_DOC_JSON_SPEC);
        JsonParser p = tree.traverse(ObjectReadContext.empty());
        verifyJsonSpecSampleDoc(p, true);
        p.close();
    }

    public void testBinaryPojo() throws Exception
    {
        byte[] inputBinary = new byte[] { 1, 2, 100 };
        POJONode n = new POJONode(inputBinary);
        JsonParser p = n.traverse(ObjectReadContext.empty());

        assertNull(p.currentToken());
        assertToken(JsonToken.VALUE_EMBEDDED_OBJECT, p.nextToken());
        byte[] data = p.getBinaryValue();
        assertNotNull(data);
        assertArrayEquals(inputBinary, data);
        Object pojo = p.getEmbeddedObject();
        assertSame(data, pojo);
        p.close();
    }

    public void testBinaryNode() throws Exception
    {
        byte[] inputBinary = new byte[] { 0, -5 };
        BinaryNode n = new BinaryNode(inputBinary);
        JsonParser p = n.traverse(ObjectReadContext.empty());

        assertNull(p.currentToken());
        // exposed as POJO... not as VALUE_STRING
        assertToken(JsonToken.VALUE_EMBEDDED_OBJECT, p.nextToken());
        byte[] data = p.getBinaryValue();
        assertNotNull(data);
        assertArrayEquals(inputBinary, data);

        // but as importantly, can be viewed as base64 encoded thing:
        assertEquals("APs=", p.getText());

        assertNull(p.nextToken());
        p.close();
    }

    public void testTextAsBinary() throws Exception
    {
        TextNode n = new TextNode("   APs=\n");
        JsonParser p = n.traverse(ObjectReadContext.empty());
        assertNull(p.currentToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        byte[] data = p.getBinaryValue();
        assertNotNull(data);
        assertArrayEquals(new byte[] { 0, -5 }, data);

        assertNull(p.nextToken());
        p.close();
        assertTrue(p.isClosed());

        // Also: let's verify we get an exception for garbage...
        n = new TextNode("?!??");
        p = n.traverse(ObjectReadContext.empty());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        try {
            p.getBinaryValue();
        } catch (InvalidFormatException e) {
            verifyException(e, "Illegal character");
        }
        p.close();
    }

    /**
     * Very simple test case to verify that tree-to-POJO
     * conversion works ok
     */
    public void testDataBind() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        JsonNode tree = m.readTree
            ("{ \"name\" : \"Tatu\", \n"
             +"\"magicNumber\" : 42,"
             +"\"kids\" : [ \"Leo\", \"Lila\", \"Leia\" ] \n"
             +"}");
        Person tatu = m.treeToValue(tree, Person.class);
        assertNotNull(tatu);
        assertEquals(42, tatu.magicNumber);
        assertEquals("Tatu", tatu.name);
        assertNotNull(tatu.kids);
        assertEquals(3, tatu.kids.size());
        assertEquals("Leo", tatu.kids.get(0));
        assertEquals("Lila", tatu.kids.get(1));
        assertEquals("Leia", tatu.kids.get(2));
    }

    // [JACKSON-370]
    public void testSkipChildrenWrt370() throws Exception
    {
        ObjectMapper o = new ObjectMapper();
        ObjectNode n = o.createObjectNode();
        n.putObject("inner").put("value", "test");
        n.putObject("unknown").putNull("inner");
        Jackson370Bean obj = o.readValue(n.traverse(ObjectReadContext.empty()), Jackson370Bean.class);
        assertNotNull(obj.inner);
        assertEquals("test", obj.inner.value);        
    }
}

