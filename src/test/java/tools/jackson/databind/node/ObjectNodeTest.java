package tools.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.exc.JsonNodeException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Additional tests for {@link ObjectNode} container class.
 */
public class ObjectNodeTest
    extends DatabindTestUtil
{
    @JsonDeserialize(as = DataImpl.class)
    public interface Data {
    }

    public static class DataImpl implements Data
    {
        protected JsonNode root;

        @JsonCreator
        public DataImpl(JsonNode n) {
            root = n;
        }

        @JsonValue
        public JsonNode value() { return root; }
    }

    static class ObNodeWrapper {
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public ObjectNode node;

        protected ObNodeWrapper() { }
        public ObNodeWrapper(ObjectNode n) {
            node = n;
        }
    }

    // [databind#941]
    static class MyValue
    {
        private final ObjectNode object;

        @JsonCreator
        public MyValue(ObjectNode object) { this.object = object; }

        @JsonValue
        public ObjectNode getObject() { return object; }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testSimpleObject() throws Exception
    {
        String JSON = "{ \"key\" : 1, \"b\" : \"x\" }";
        JsonNode root = MAPPER.readTree(JSON);

        // basic properties first:
        assertFalse(root.isValueNode());
        assertTrue(root.isContainer());
        assertFalse(root.isArray());
        assertTrue(root.isObject());
        assertEquals(2, root.size());
        assertFalse(root.isEmpty());

        assertFalse(root.isBoolean());
        assertFalse(root.isString());
        assertFalse(root.isNumber());
        assertFalse(root.canConvertToInt());
        assertFalse(root.canConvertToLong());
        assertFalse(root.canConvertToExactIntegral());

        Iterator<JsonNode> it = root.iterator();
        assertNotNull(it);
        assertTrue(it.hasNext());
        JsonNode n = it.next();
        assertNotNull(n);
        assertEquals(IntNode.valueOf(1), n);

        assertTrue(it.hasNext());
        n = it.next();
        assertNotNull(n);
        assertEquals(StringNode.valueOf("x"), n);

        assertFalse(it.hasNext());

        // Ok, then, let's traverse via extended interface
        ObjectNode obNode = (ObjectNode) root;
        Iterator<Map.Entry<String,JsonNode>> fit = obNode.properties().iterator();
        // we also know that LinkedHashMap is used, i.e. order preserved
        assertTrue(fit.hasNext());
        Map.Entry<String,JsonNode> en = fit.next();
        assertEquals("key", en.getKey());
        assertEquals(IntNode.valueOf(1), en.getValue());

        assertTrue(fit.hasNext());
        en = fit.next();
        assertEquals("b", en.getKey());
        assertEquals(StringNode.valueOf("x"), en.getValue());

        // Plus: we should be able to modify the node via iterator too:
        fit.remove();
        assertEquals(1, obNode.size());
        assertEquals(IntNode.valueOf(1), root.get("key"));
        assertNull(root.get("b"));
    }

    // for [databind#346]
    @Test
    public void testEmptyNodeAsValue() throws Exception
    {
        Data w = MAPPER.readValue("{}", Data.class);
        assertNotNull(w);
    }

    @Test
    public void testBasics()
    {
        ObjectNode n = new ObjectNode(JsonNodeFactory.instance);
        assertStandardEquals(n);
        assertTrue(n.isEmpty());

        assertTrue(n.values().isEmpty());
        assertTrue(n.properties().isEmpty());
        assertTrue(n.propertyNames().isEmpty());
        assertNull(n.get("a"));
        assertFalse(n.optional("a").isPresent());
        assertTrue(n.path("a").isMissingNode());

        StringNode text = StringNode.valueOf("x");
        assertSame(n, n.set("a", text));

        assertEquals(1, n.size());
        assertFalse(n.properties().isEmpty());
        assertFalse(n.propertyNames().isEmpty());
        assertFalse(n.values().isEmpty());
        assertSame(text, n.get("a"));
        assertSame(text, n.path("a"));
        assertNull(n.get("b"));
        assertNull(n.get(0)); // not used with objects

        assertFalse(n.has(0));
        assertFalse(n.hasNonNull(0));
        assertTrue(n.has("a"));
        assertTrue(n.hasNonNull("a"));
        assertFalse(n.has("b"));
        assertFalse(n.hasNonNull("b"));

        ObjectNode n2 = new ObjectNode(JsonNodeFactory.instance);
        n2.put("b", 13);
        assertFalse(n.equals(n2));
        n.setAll(n2);

        assertEquals(2, n.size());
        n.set("null", (JsonNode)null);
        assertEquals(3, n.size());
        // may be non-intuitive, but explicit nulls do exist in tree:
        assertTrue(n.has("null"));
        assertFalse(n.hasNonNull("null"));
        // should replace, not add
        n.put("null", "notReallNull");
        assertEquals(3, n.size());
        assertNotNull(n.remove("null"));
        assertEquals(2, n.size());

        Map<String,JsonNode> nodes = new HashMap<String,JsonNode>();
        nodes.put("d", text);
        n.setAll(nodes);
        assertEquals(3, n.size());

        n.removeAll();
        assertEquals(0, n.size());
    }

    @Test
    public void testBasicsPutSet()
    {
        final JsonNodeFactory f = JsonNodeFactory.instance;
        ObjectNode root = f.objectNode();
        JsonNode old;
        old = root.putIfAbsent("key", f.stringNode("foobar"));
        assertNull(old);
        assertEquals(1, root.size());
        old = root.putIfAbsent("key", f.numberNode(3));
        assertEquals(1, root.size());
        assertSame(old, root.get("key"));

        // but can replace with straight set
        old = root.replace("key", f.numberNode(72));
        assertNotNull(old);
        assertEquals("foobar", old.stringValue());
    }

    @Test
    public void testBigNumbers()
    {
        ObjectNode n = new ObjectNode(JsonNodeFactory.instance);
        assertStandardEquals(n);
        BigInteger I = BigInteger.valueOf(3);
        BigDecimal DEC = new BigDecimal("0.1");

        n.put("a", DEC);
        n.put("b", I);

        assertEquals(2, n.size());

        assertTrue(n.path("a").isBigDecimal());
        assertEquals(DEC, n.get("a").decimalValue());
        assertTrue(n.path("b").isBigInteger());
        assertEquals(I, n.get("b").bigIntegerValue());
    }

    /**
     * Verify null handling
     */
    @Test
    public void testNullChecking()
    {
        ObjectNode o1 = JsonNodeFactory.instance.objectNode();
        ObjectNode o2 = JsonNodeFactory.instance.objectNode();
        // used to throw NPE before fix:
        o1.setAll(o2);
        assertEquals(0, o1.size());
        assertEquals(0, o2.size());

        // also: nulls should be converted to NullNodes...
        o1.set("x", null);
        JsonNode n = o1.get("x");
        assertNotNull(n);
        assertSame(n, NullNode.instance);
        assertEquals(NullNode.instance, o1.optional("x").get());

        o1.put("str", (String) null);
        n = o1.get("str");
        assertNotNull(n);
        assertSame(n, NullNode.instance);

        o1.put("d", (BigDecimal) null);
        n = o1.get("d");
        assertNotNull(n);
        assertSame(n, NullNode.instance);

        o1.put("3", (BigInteger) null);
        n = o1.get("3");
        assertNotNull(3);
        assertSame(n, NullNode.instance);

        assertEquals(4, o1.size());
    }

    @Test
    public void testNullChecking2()
    {
        ObjectNode src = MAPPER.createObjectNode();
        ObjectNode dest = MAPPER.createObjectNode();
        src.put("a", "b");
        dest.setAll(src);
    }

    // for [databind#346]
    @Test
    public void testNullKeyChecking()
    {
        ObjectNode src = MAPPER.createObjectNode();
        assertThrows(NullPointerException.class, () -> src.put(null, "a"));
        assertThrows(NullPointerException.class, () -> src.put(null, 123));
        assertThrows(NullPointerException.class, () -> src.put(null, 123L));
        assertThrows(NullPointerException.class, () -> src.putNull(null));

        assertThrows(NullPointerException.class, () -> src.set(null, BooleanNode.TRUE));
        assertThrows(NullPointerException.class, () -> src.replace(null, BooleanNode.TRUE));

        assertThrows(NullPointerException.class, () -> src.setAll(Collections.singletonMap(null,
                MAPPER.createArrayNode())));
    }

    @Test
    public void testRemove()
    {
        ObjectNode ob = MAPPER.createObjectNode();
        ob.put("a", "a");
        ob.put("b", "b");
        ob.put("c", "c");
        assertEquals(3, ob.size());
        assertSame(ob, ob.without(Arrays.asList("a", "c")));
        assertEquals(1, ob.size());
        assertEquals("b", ob.get("b").stringValue());
    }

    @Test
    public void testRetain()
    {
        ObjectNode ob = MAPPER.createObjectNode();
        ob.put("a", "a");
        ob.put("b", "b");
        ob.put("c", "c");
        assertEquals(3, ob.size());
        assertSame(ob, ob.retain("a", "c"));
        assertEquals(2, ob.size());
        assertEquals("a", ob.get("a").stringValue());
        assertNull(ob.get("b"));
        assertEquals("c", ob.get("c").stringValue());
    }

    @Test
    public void testValidWithObject() throws Exception
    {
        ObjectNode root = MAPPER.createObjectNode();
        assertEquals("{}", MAPPER.writeValueAsString(root));
        JsonNode child = root.withObject("/prop");
        assertTrue(child instanceof ObjectNode);
        assertEquals("{\"prop\":{}}", MAPPER.writeValueAsString(root));
    }

    @Test
    public void testValidWithArray() throws Exception
    {
        JsonNode root = MAPPER.createObjectNode();
        assertEquals("{}", MAPPER.writeValueAsString(root));
        ArrayNode child = root.withArray("/arr");
        assertTrue(child instanceof ArrayNode);
        assertEquals("{\"arr\":[]}", MAPPER.writeValueAsString(root));
    }

    @Test
    public void testInvalidWithObject() throws Exception
    {
        JsonNode root = MAPPER.createArrayNode();
        try { // should not work for non-ObjectNode nodes:
            root.withObject("/prop");
            fail("Expected exception");
        } catch (JsonNodeException e) {
            verifyException(e, "`JsonNode` not of type `ObjectNode`");
            verifyException(e, "ArrayNode");
        }
        // also: should fail of we already have non-object property
        ObjectNode root2 = MAPPER.createObjectNode();
        root2.put("prop", 13);
        try { // should not work for non-ObjectNode nodes:
            root2.withObject("/prop");
            fail("Expected exception");
        } catch (JsonNodeException e) {
            verifyException(e, "Cannot replace `JsonNode` of type ");
            verifyException(e, "IntNode");
        }
    }

    @Test
    public void testInvalidWithArray() throws Exception
    {
        JsonNode root = MAPPER.createArrayNode();
        try { // should not work for non-ObjectNode nodes:
            root.withArray("/prop");
            fail("Expected exception");
        } catch (JsonNodeException e) {
            verifyException(e, "`JsonNode` not of type `ObjectNode`");
            verifyException(e, "ArrayNode");
        }
        // also: should fail of we already have non-Array property
        ObjectNode root2 = MAPPER.createObjectNode();
        root2.put("prop", 13);
        try { // should not work for non-ObjectNode nodes:
            root2.withArray("/prop");
            fail("Expected exception");
        } catch (JsonNodeException e) {
            verifyException(e, "Cannot replace `JsonNode` of type ");
            verifyException(e, "IntNode");
        }
    }

    @Test
    public void testSetAll() throws Exception
    {
        ObjectNode root = MAPPER.createObjectNode();
        assertEquals(0, root.size());
        HashMap<String,JsonNode> map = new HashMap<String,JsonNode>();
        map.put("a", root.numberNode(1));
        root.setAll(map);
        assertEquals(1, root.size());
        assertTrue(root.has("a"));
        assertFalse(root.has("b"));

        map.put("b", root.numberNode(2));
        root.setAll(map);
        assertEquals(2, root.size());
        assertTrue(root.has("a"));
        assertTrue(root.has("b"));
        assertEquals(2, root.path("b").intValue());

        // Then with ObjectNodes...
        ObjectNode root2 = MAPPER.createObjectNode();
        root2.setAll(root);
        assertEquals(2, root.size());
        assertEquals(2, root2.size());

        root2.setAll(root);
        assertEquals(2, root.size());
        assertEquals(2, root2.size());

        ObjectNode root3 = MAPPER.createObjectNode();
        root3.put("a", 2);
        root3.put("c", 3);
        assertEquals(2, root3.path("a").intValue());
        root3.setAll(root2);
        assertEquals(3, root3.size());
        assertEquals(1, root3.path("a").intValue());
    }

    // [databind#237] (databind): support DeserializationFeature#FAIL_ON_READING_DUP_TREE_KEY
    @Test
    public void testFailOnDupKeys() throws Exception
    {
        final String DUP_JSON = "{ \"a\":1, \"a\":2 }";

        // first: verify defaults:
        assertFalse(MAPPER.isEnabled(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY));
        ObjectNode root = _objNode(DUP_JSON);
        assertEquals(2, root.path("a").asInt());

        // and via ObjectReader, too:
        root = (ObjectNode) MAPPER.reader().readTree(DUP_JSON);
        assertEquals(2, root.path("a").asInt());

        // and then enable checks:
        try {
            MAPPER.reader(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY).readTree(DUP_JSON);
            fail("Should have thrown exception!");
        } catch (MismatchedInputException e) {
            verifyException(e, "duplicate property \"a\"");
        }
    }

    @Test
    public void testFailOnDupNestedKeys() throws Exception
    {
        final String DOC = a2q(
                "{'node' : { 'data' : [ 1, 2, { 'a':3 }, { 'foo' : 1, 'bar' : 2, 'foo': 3}]}}"
        );
        try {
            MAPPER.readerFor(ObNodeWrapper.class)
                .with(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
                .readValue(DOC);
            fail("Should have thrown exception!");
        } catch (MismatchedInputException e) {
            verifyException(e, "duplicate property \"foo\"");
        }
    }

    @Test
    public void testEqualityWrtOrder() throws Exception
    {
        ObjectNode ob1 = MAPPER.createObjectNode();
        ObjectNode ob2 = MAPPER.createObjectNode();

        // same contents, different insertion order; should not matter

        ob1.put("a", 1);
        ob1.put("b", 2);
        ob1.put("c", 3);

        ob2.put("b", 2);
        ob2.put("c", 3);
        ob2.put("a", 1);

        assertTrue(ob1.equals(ob2));
        assertTrue(ob2.equals(ob1));
    }

    @Test
    public void testSimplePath() throws Exception
    {
        JsonNode root = MAPPER.readTree("{ \"results\" : { \"a\" : 3 } }");
        assertTrue(root.isObject());
        JsonNode rnode = root.path("results");
        assertNotNull(rnode);
        assertTrue(rnode.isObject());
        assertEquals(3, rnode.path("a").intValue());
    }

    @Test
    public void testNonEmptySerialization() throws Exception
    {
        ObNodeWrapper w = new ObNodeWrapper(MAPPER.createObjectNode()
                .put("a", 3));
        assertEquals("{\"node\":{\"a\":3}}", MAPPER.writeValueAsString(w));
        w = new ObNodeWrapper(MAPPER.createObjectNode());
        assertEquals("{}", MAPPER.writeValueAsString(w));
    }

    @Test
    public void testIssue941() throws Exception
    {
        ObjectNode object = MAPPER.createObjectNode();

        String json = MAPPER.writeValueAsString(object);
//        System.out.println("json: "+json);

        ObjectNode de1 = MAPPER.readValue(json, ObjectNode.class);  // this works
//        System.out.println("Deserialized to ObjectNode: "+de1);
        assertNotNull(de1);

        MyValue de2 = MAPPER.readValue(json, MyValue.class);  // but this throws exception
//        System.out.println("Deserialized to MyValue: "+de2);
        assertNotNull(de2);
    }

    @Test
    public void testSimpleMismatch() throws Exception
    {
        try {
            MAPPER.readValue("[ 1, 2, 3 ]", ObjectNode.class);
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "from Array value (token `JsonToken.START_ARRAY`)");
        }
    }

    // [databind#3809]
    @Test
    public void testPropertiesTraversal() throws Exception
    {
        // Nothing to traverse for other types
        assertEquals("", _toString(MAPPER.createArrayNode()));
        assertEquals("", _toString(MAPPER.getNodeFactory().stringNode("foo")));

        // But yes for ObjectNode:
        JsonNode n = MAPPER.readTree(a2q(
                "{ 'a':1, 'b':true,'c':'stuff'}"));
        assertEquals("a/1,b/true,c/\"stuff\"", _toString(n));
    }

    // [databind#4863]: valueStream(), entryStream(), forEachEntry()
    @Test
    public void testStreamMethods()
    {
        ObjectMapper mapper = objectMapper();
        ObjectNode obj = mapper.createObjectNode();
        JsonNode n1 = obj.numberNode(42);
        JsonNode n2 = obj.stringNode("foo");

        obj.set("a", n1);
        obj.set("b", n2);

        // First, valueStream() testing
        assertEquals(2, obj.valueStream().count());
        assertEquals(Arrays.asList(n1, n2),
                obj.valueStream().collect(Collectors.toList()));

        // And then entryStream() (empty)
        assertEquals(2, obj.propertyStream().count());
        assertEquals(new ArrayList<>(obj.properties()),
                obj.propertyStream().collect(Collectors.toList()));

        // And then empty forEachEntry()
        final LinkedHashMap<String,JsonNode> map = new LinkedHashMap<>();
        obj.forEachEntry((k, v) -> { map.put(k, v); });
        assertEquals(obj.properties(), map.entrySet());
    }

    @Test
    public void testRemoveAll() throws Exception
    {
        assertEquals(_objNode("{}"),
                _objNode("{'a':1, 'b':2, 'c':3}").removeAll());
    }

    // [databind#4955]: remove methods
    @Test
    public void testRemoveIf() throws Exception
    {
        assertEquals(_objNode("{'c':3}"),
                _objNode("{'a':1, 'b':2, 'c':3}")
                .removeIf(value -> value.asInt() <= 2));
        assertEquals(_objNode("{'a':1}"),
                _objNode("{'a':1, 'b':2, 'c':3}")
                .removeIf(value -> value.asInt() > 1));
    }

    // [databind#4955]: remove methods
    @Test
    public void testRemoveNulls() throws Exception
    {
        assertEquals(_objNode("{'b':2}"),
                _objNode("{'a':null,'b':2,'c':null}")
                .removeNulls());
    }

    private ObjectNode _objNode(String json) throws Exception {
        // Use different read method for better code coverage
        try (JsonParser p = MAPPER.createParser(a2q(json))) {
            return (ObjectNode) MAPPER.reader().readTree(p);
        }
    }

    private String _toString(JsonNode n) {
        return n.properties().stream()
                .map(e -> e.getKey() + "/" + e.getValue())
                .collect(Collectors.joining(","));
    }
}
