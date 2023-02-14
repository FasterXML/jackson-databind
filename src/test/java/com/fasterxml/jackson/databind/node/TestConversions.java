package com.fasterxml.jackson.databind.node;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

import org.junit.Assert;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.util.TokenBuffer;

/**
 * Unit tests for verifying functionality of {@link JsonNode} methods that
 * convert values to other types
 */
public class TestConversions extends BaseMapTest
{
    static class Root {
        public Leaf leaf;
    }

    static class Leaf {
        public int value;

        public Leaf() { }
        public Leaf(int v) { value = v; }
    }

    @JsonDeserialize(using = LeafDeserializer.class)
    public static class LeafMixIn { }

    // for [databind#467]
    @JsonSerialize(using=Issue467Serializer.class)
    static class Issue467Bean  {
        public int i;

        public Issue467Bean(int i0) { i = i0; }
        public Issue467Bean() { this(0); }
    }

    @JsonSerialize(using=Issue467TreeSerializer.class)
    static class Issue467Tree  {
    }

    static class Issue467Serializer extends JsonSerializer<Issue467Bean> {
        @Override
        public void serialize(Issue467Bean value, JsonGenerator jgen,
                SerializerProvider provider) throws IOException {
            jgen.writeObject(new Issue467TmpBean(value.i));
        }
    }

    static class Issue467TreeSerializer extends JsonSerializer<Issue467Tree> {
        @Override
        public void serialize(Issue467Tree value, JsonGenerator jgen,
                SerializerProvider provider) throws IOException {
            jgen.writeTree(BooleanNode.TRUE);
        }
    }

    static class Issue467TmpBean  {
        public int x;

        public Issue467TmpBean(int i) { x = i; }
    }

    static class Issue709Bean {
        public byte[] data;
    }

    @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="_class")
    static class LongContainer1940 {
        public Long longObj;
    }

    // [databind#433]
    static class CustomSerializedPojo implements JsonSerializable
    {
        private final ObjectNode node = JsonNodeFactory.instance.objectNode();

        public void setFoo(final String foo) {
            node.put("foo", foo);
        }

        @Override
        public void serialize(final JsonGenerator jgen, final SerializerProvider provider) throws IOException
        {
            jgen.writeTree(node);
        }

        @Override
        public void serializeWithType(JsonGenerator g,
                SerializerProvider provider, TypeSerializer typeSer) throws IOException
        {
            WritableTypeId typeIdDef = new WritableTypeId(this, JsonToken.START_OBJECT);
            typeSer.writeTypePrefix(g, typeIdDef);
            serialize(g, provider);
            typeSer.writeTypePrefix(g, typeIdDef);
        }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = objectMapper();

    public void testAsInt() throws Exception
    {
        assertEquals(9, IntNode.valueOf(9).asInt());
        assertEquals(7, LongNode.valueOf(7L).asInt());
        assertEquals(13, new TextNode("13").asInt());
        assertEquals(0, new TextNode("foobar").asInt());
        assertEquals(27, new TextNode("foobar").asInt(27));
        assertEquals(1, BooleanNode.TRUE.asInt());
    }

    public void testAsBoolean() throws Exception
    {
        assertEquals(false, BooleanNode.FALSE.asBoolean());
        assertEquals(true, BooleanNode.TRUE.asBoolean());
        assertEquals(false, IntNode.valueOf(0).asBoolean());
        assertEquals(true, IntNode.valueOf(1).asBoolean());
        assertEquals(false, LongNode.valueOf(0).asBoolean());
        assertEquals(true, LongNode.valueOf(-34L).asBoolean());
        assertEquals(true, new TextNode("true").asBoolean());
        assertEquals(false, new TextNode("false").asBoolean());
        assertEquals(false, new TextNode("barf").asBoolean());
        assertEquals(true, new TextNode("barf").asBoolean(true));

        assertEquals(true, new POJONode(Boolean.TRUE).asBoolean());
    }

    // Deserializer to trigger the problem described in [JACKSON-554]
    public static class LeafDeserializer extends JsonDeserializer<Leaf>
    {
        @Override
        public Leaf deserialize(JsonParser jp, DeserializationContext ctxt)
                throws IOException
        {
            JsonNode tree = (JsonNode) jp.readValueAsTree();
            Leaf leaf = new Leaf();
            leaf.value = tree.get("value").intValue();
            return leaf;
        }
    }

    public void testTreeToValue() throws Exception
    {
        String JSON = "{\"leaf\":{\"value\":13}}";
        ObjectMapper mapper = new ObjectMapper();
        mapper.addMixIn(Leaf.class, LeafMixIn.class);
        JsonNode root = mapper.readTree(JSON);
        // Ok, try converting to bean using two mechanisms
        Root r1 = mapper.treeToValue(root, Root.class);
        assertNotNull(r1);
        assertEquals(13, r1.leaf.value);

        // ... also JavaType
        r1 = mapper.treeToValue(root, mapper.constructType(Root.class));
        assertEquals(13, r1.leaf.value);
    }

    // [databind#1208]: should coerce POJOs at least at root level
    public void testTreeToValueWithPOJO() throws Exception
    {
        Calendar c = Calendar.getInstance();
        c.setTime(new java.util.Date(0));
        final ValueNode pojoNode = MAPPER.getNodeFactory().pojoNode(c);
        Calendar result = MAPPER.treeToValue(pojoNode, Calendar.class);
        assertEquals(result.getTimeInMillis(), c.getTimeInMillis());

        // and same with JavaType
        result = MAPPER.treeToValue(pojoNode, MAPPER.constructType(Calendar.class));
        assertEquals(result.getTimeInMillis(), c.getTimeInMillis());
    }

    public void testBase64Text() throws Exception
    {
        // let's actually iterate over sets of encoding modes, lengths

        final int[] LENS = { 1, 2, 3, 4, 7, 9, 32, 33, 34, 35 };
        final Base64Variant[] VARIANTS = {
                Base64Variants.MIME,
                Base64Variants.MIME_NO_LINEFEEDS,
                Base64Variants.MODIFIED_FOR_URL,
                Base64Variants.PEM
        };

        for (int len : LENS) {
            byte[] input = new byte[len];
            for (int i = 0; i < input.length; ++i) {
                input[i] = (byte) i;
            }
            for (Base64Variant variant : VARIANTS) {
                TextNode n = new TextNode(variant.encode(input));
                byte[] data = null;
                try {
                    data = n.getBinaryValue(variant);
                } catch (Exception e) {
                    fail("Failed (variant "+variant+", data length "+len+"): "+e.getMessage());
                }
                assertNotNull(data);
                assertArrayEquals(data, input);

                // 15-Aug-2018, tatu: [databind#2096] requires another test
                JsonParser p = new TreeTraversingParser(n);
                assertEquals(JsonToken.VALUE_STRING, p.nextToken());
                try {
                    data = p.getBinaryValue(variant);
                } catch (Exception e) {
                    fail("Failed (variant "+variant+", data length "+len+"): "+e.getMessage());
                }
                assertNotNull(data);
                assertArrayEquals(data, input);
                p.close();
            }
        }
    }

    /**
     * Simple test to verify that byte[] values can be handled properly when
     * converting, as long as there is metadata (from POJO definitions).
     */
    public void testIssue709() throws Exception
    {
        byte[] inputData = new byte[] { 1, 2, 3 };
        ObjectNode node = MAPPER.createObjectNode();
        node.put("data", inputData);
        Issue709Bean result = MAPPER.treeToValue(node, Issue709Bean.class);
        String json = MAPPER.writeValueAsString(node);
        Issue709Bean resultFromString = MAPPER.readValue(json, Issue709Bean.class);
        Issue709Bean resultFromConvert = MAPPER.convertValue(node, Issue709Bean.class);

        // all methods should work equally well:
        Assert.assertArrayEquals(inputData, resultFromString.data);
        Assert.assertArrayEquals(inputData, resultFromConvert.data);
        Assert.assertArrayEquals(inputData, result.data);
    }

    public void testEmbeddedByteArray() throws Exception
    {
        TokenBuffer buf = new TokenBuffer(MAPPER, false);
        buf.writeObject(new byte[3]);
        JsonNode node = MAPPER.readTree(buf.asParser());
        buf.close();
        assertTrue(node.isBinary());
        byte[] data = node.binaryValue();
        assertNotNull(data);
        assertEquals(3, data.length);
    }

    // [databind#232]
    public void testBigDecimalAsPlainStringTreeConversion() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);
        Map<String, Object> map = new HashMap<String, Object>();
        String PI_STR = "3.00000000";
        map.put("pi", new BigDecimal(PI_STR));
        JsonNode tree = mapper.valueToTree(map);
        assertNotNull(tree);
        assertEquals(1, tree.size());
        assertTrue(tree.has("pi"));
    }

    // [databind#433]
    public void testBeanToTree() throws Exception
    {
        final CustomSerializedPojo pojo = new CustomSerializedPojo();
        pojo.setFoo("bar");
        final JsonNode node = MAPPER.valueToTree(pojo);
        assertEquals(JsonNodeType.OBJECT, node.getNodeType());
    }

    // [databind#467]
    public void testConversionOfPojos() throws Exception
    {
        final Issue467Bean input = new Issue467Bean(13);
        final String EXP = "{\"x\":13}";

        // first, sanity check
        String json = MAPPER.writeValueAsString(input);
        assertEquals(EXP, json);

        // then via conversions: should become JSON Object
        JsonNode tree = MAPPER.valueToTree(input);
        assertTrue("Expected Object, got "+tree.getNodeType(), tree.isObject());
        assertEquals(EXP, MAPPER.writeValueAsString(tree));
    }

    // [databind#467]
    public void testConversionOfTrees() throws Exception
    {
        final Issue467Tree input = new Issue467Tree();
        final String EXP = "true";

        // first, sanity check
        String json = MAPPER.writeValueAsString(input);
        assertEquals(EXP, json);

        // then via conversions: should become JSON Object
        JsonNode tree = MAPPER.valueToTree(input);
        assertTrue("Expected Object, got "+tree.getNodeType(), tree.isBoolean());
        assertEquals(EXP, MAPPER.writeValueAsString(tree));
    }

    // [databind#1940]: losing of precision due to coercion
    public void testBufferedLongViaCoercion() throws Exception {
        long EXP = 1519348261000L;
        JsonNode tree = MAPPER.readTree("{\"longObj\": "+EXP+".0, \"_class\": \""+LongContainer1940.class.getName()+"\"}");
        LongContainer1940 obj = MAPPER.treeToValue(tree, LongContainer1940.class);
        assertEquals(Long.valueOf(EXP), obj.longObj);
    }

    public void testConversionsOfNull() throws Exception
    {
        // First: `null` value should become `NullNode`
        JsonNode n = MAPPER.valueToTree(null);
        assertNotNull(n);
        assertTrue(n.isNull());

        // and vice versa
        Object pojo = MAPPER.treeToValue(n, Root.class);
        assertNull(pojo);

        pojo = MAPPER.treeToValue(n, MAPPER.constructType(Root.class));
        assertNull(pojo);

        // [databind#2972]
        AtomicReference<?> result = MAPPER.treeToValue(NullNode.instance,
                AtomicReference.class);
        assertNotNull(result);
        assertNull(result.get());

        result = MAPPER.treeToValue(NullNode.instance,
                MAPPER.constructType(AtomicReference.class));
        assertNotNull(result);
        assertNull(result.get());
    }

    // Simple cast, for Tree
    public void testNodeConvert() throws Exception
    {
        ObjectNode src = (ObjectNode) MAPPER.readTree("{}");
        TreeNode node = src;
        ObjectNode result = MAPPER.treeToValue(node, ObjectNode.class);
        // should just cast...
        assertSame(src, result);

        // ditto w/ JavaType
        result = MAPPER.treeToValue(node, MAPPER.constructType(ObjectNode.class));
        assertSame(src, result);
    }
}
