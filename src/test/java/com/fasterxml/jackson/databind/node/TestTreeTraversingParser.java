package com.fasterxml.jackson.databind.node;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

import static org.junit.Assert.*;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.JsonParser.NumberType;
import com.fasterxml.jackson.core.exc.InputCoercionException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

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

    private final ObjectMapper MAPPER = newJsonMapper();

    public void testSimple() throws Exception
    {
        // For convenience, parse tree from JSON first
        final String JSON =
            "{ \"a\" : 123, \"list\" : [ 12.25, null, true, { }, [ ] ] }";
        JsonNode tree = MAPPER.readTree(JSON);
        JsonParser p = tree.traverse();

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
        JsonParser p = MAPPER.readTree("[]").traverse();
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();

        p = MAPPER.readTree("[[]]").traverse();
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();

        p = MAPPER.readTree("[[ 12.1 ]]").traverse();
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
        JsonNode tree = MAPPER.readTree(JSON);
        JsonParser p = tree.traverse();
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
        JsonNode tree = MAPPER.readTree(SAMPLE_DOC_JSON_SPEC);
        JsonParser p = tree.traverse();
        verifyJsonSpecSampleDoc(p, true);
        p.close();
    }

    public void testBinaryPojo() throws Exception
    {
        byte[] inputBinary = new byte[] { 1, 2, 100 };
        POJONode n = new POJONode(inputBinary);
        JsonParser p = n.traverse();

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
        JsonParser p = n.traverse();

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
        JsonParser p = n.traverse();
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
        p = n.traverse();
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
        JsonNode tree = MAPPER.readTree
            ("{ \"name\" : \"Tatu\", \n"
             +"\"magicNumber\" : 42,"
             +"\"kids\" : [ \"Leo\", \"Lila\", \"Leia\" ] \n"
             +"}");
        Person tatu = MAPPER.treeToValue(tree, Person.class);
        assertNotNull(tatu);
        assertEquals(42, tatu.magicNumber);
        assertEquals("Tatu", tatu.name);
        assertNotNull(tatu.kids);
        assertEquals(3, tatu.kids.size());
        assertEquals("Leo", tatu.kids.get(0));
        assertEquals("Lila", tatu.kids.get(1));
        assertEquals("Leia", tatu.kids.get(2));
    }

    public void testSkipChildrenWrt370() throws Exception
    {
        ObjectNode n = MAPPER.createObjectNode();
        n.putObject("inner").put("value", "test");
        n.putObject("unknown").putNull("inner");
        Jackson370Bean obj = MAPPER.readValue(n.traverse(), Jackson370Bean.class);
        assertNotNull(obj.inner);
        assertEquals("test", obj.inner.value);
    }

    // // // Numeric coercion checks, [databind#2189]

    public void testNumberOverflowInt() throws IOException
    {
        final long tooBig = 1L + Integer.MAX_VALUE;
        try (final JsonParser p = MAPPER.readTree("[ "+tooBig+" ]").traverse()) {
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(NumberType.LONG, p.getNumberType());
            try {
                p.getIntValue();
                fail("Expected failure for `int` overflow");
            } catch (InputCoercionException e) {
                verifyException(e, "Numeric value ("+tooBig+") out of range of int");
            }
        }
        try (final JsonParser p = MAPPER.readTree("{ \"value\" : "+tooBig+" }").traverse()) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(NumberType.LONG, p.getNumberType());
            try {
                p.getIntValue();
                fail("Expected failure for `int` overflow");
            } catch (InputCoercionException e) {
                verifyException(e, "Numeric value ("+tooBig+") out of range of int");
            }
        }
        // But also from floating-point
        final String tooBig2 = "1.0e10";
        try (final JsonParser p = MAPPER.readTree("[ "+tooBig2+" ]").traverse()) {
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            assertEquals(NumberType.DOUBLE, p.getNumberType());
            try {
                p.getIntValue();
                fail("Expected failure for `int` overflow");
            } catch (InputCoercionException e) {
                verifyException(e, "Numeric value ("+tooBig2+") out of range of int");
            }
        }
    }

    public void testNumberOverflowLong() throws IOException
    {
        final BigInteger tooBig = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
        try (final JsonParser p = MAPPER.readTree("[ "+tooBig+" ]").traverse()) {
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(NumberType.BIG_INTEGER, p.getNumberType());
            try {
                p.getLongValue();
                fail("Expected failure for `long` overflow");
            } catch (InputCoercionException e) {
                verifyException(e, "Numeric value ("+tooBig+") out of range of long");
            }
        }
        try (final JsonParser p = MAPPER.readTree("{ \"value\" : "+tooBig+" }").traverse()) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(NumberType.BIG_INTEGER, p.getNumberType());
            try {
                p.getLongValue();
                fail("Expected failure for `long` overflow");
            } catch (InputCoercionException e) {
                verifyException(e, "Numeric value ("+tooBig+") out of range of long");
            }
        }
        // But also from floating-point
        final String tooBig2 = "1.0e30";
        try (final JsonParser p = MAPPER.readTree("[ "+tooBig2+" ]").traverse()) {
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            assertEquals(NumberType.DOUBLE, p.getNumberType());
            try {
                p.getLongValue();
                fail("Expected failure for `long` overflow");
            } catch (InputCoercionException e) {
                verifyException(e, "Numeric value ("+tooBig2+") out of range of long");
            }
        }

        // Plus, wrt [databind#2393], NON-failing cases
        final long[] okValues = new long[] { 1L+Integer.MAX_VALUE, -1L + Integer.MIN_VALUE,
                Long.MAX_VALUE, Long.MIN_VALUE };
        for (long okValue : okValues) {
            try (final JsonParser p = MAPPER.readTree("{ \"value\" : "+okValue+" }").traverse()) {
                assertToken(JsonToken.START_OBJECT, p.nextToken());
                assertToken(JsonToken.FIELD_NAME, p.nextToken());
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertEquals(NumberType.LONG, p.getNumberType());
                assertEquals(okValue, p.getLongValue());
                assertToken(JsonToken.END_OBJECT, p.nextToken());
            }
        }
    }
}
