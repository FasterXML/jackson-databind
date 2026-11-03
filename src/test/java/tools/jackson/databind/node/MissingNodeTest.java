package tools.jackson.databind.node;

import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonToken;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class MissingNodeTest extends NodeTestBase
{
    @Test
    public void testMissing()
    {
        MissingNode n = MissingNode.getInstance();
        assertTrue(n.isMissingNode());
        assertEquals(JsonToken.NOT_AVAILABLE, n.asToken());
        // [databind#5583]: as of 3.1, MissingNode.asString() returns "" like NullNode
        assertEquals("", n.asString());
        assertEquals("default", n.asString("default"));
        assertFalse(n.asStringOpt().isPresent());
        assertStandardEquals(n);
        // 10-Dec-2018, tatu: With 2.10, should serialize same as via ObjectMapper/ObjectWriter
        // 10-Dec-2019, tatu: Surprise! No, this is not how it worked in 2.9, nor does it make
        //    sense... see [databind#2566] for details
        assertEquals("", n.toString());

        assertNodeNumbersForNonNumeric(n);

        assertTrue(n.asBoolean(true));
        assertEquals(4, n.asInt(4));
        assertEquals(5L, n.asLong(5));
        assertEquals(0.25, n.asDouble(0.25));
    }

    // [databind#5583]: MissingNode should behave like NullNode for asXxx() methods
    @Test
    public void testMissingNodeNullLikeBehavior()
    {
        MissingNode n = MissingNode.getInstance();

        // Boolean: asBoolean() returns false (like NullNode)
        assertThat(n.asBoolean()).isFalse();
        assertThat(n.asBoolean(true)).isTrue();
        assertThat(n.asBooleanOpt()).isNotPresent();

        // String: asString() returns "" (like NullNode)
        assertThat(n.asString()).isEqualTo("");
        assertThat(n.asString("default")).isEqualTo("default");
        assertThat(n.asStringOpt()).isNotPresent();

        // Numeric types: asXxx() returns 0/ZERO (like NullNode)
        assertThat(n.asShort()).isEqualTo((short) 0);
        assertThat(n.asShort((short) 5)).isEqualTo((short) 5);
        assertThat(n.asShortOpt()).isNotPresent();

        assertThat(n.asInt()).isEqualTo(0);
        assertThat(n.asInt(5)).isEqualTo(5);
        assertThat(n.asIntOpt()).isNotPresent();

        assertThat(n.asLong()).isEqualTo(0L);
        assertThat(n.asLong(5L)).isEqualTo(5L);
        assertThat(n.asLongOpt()).isNotPresent();

        assertThat(n.asBigInteger()).isEqualTo(BigInteger.ZERO);
        assertThat(n.asBigInteger(BigInteger.TEN)).isEqualTo(BigInteger.TEN);
        assertThat(n.asBigIntegerOpt()).isNotPresent();

        assertThat(n.asFloat()).isEqualTo(0.0f);
        assertThat(n.asFloat(1.5f)).isEqualTo(1.5f);
        assertThat(n.asFloatOpt()).isNotPresent();

        assertThat(n.asDouble()).isEqualTo(0.0d);
        assertThat(n.asDouble(1.5d)).isEqualTo(1.5d);
        assertThat(n.asDoubleOpt()).isNotPresent();

        assertThat(n.asDecimal()).isEqualTo(BigDecimal.ZERO);
        assertThat(n.asDecimal(BigDecimal.TEN)).isEqualTo(BigDecimal.TEN);
        assertThat(n.asDecimalOpt()).isNotPresent();
    }

    /**
     * Let's also verify behavior of "MissingNode" -- one needs to be able
     * to traverse such bogus nodes with appropriate methods.
     */
    @SuppressWarnings("unused")
    @Test
    public void testMissingViaMapper() throws Exception
    {
        String JSON = "[ { }, [ ] ]";
        JsonNode result = objectMapper().readTree(new StringReader(JSON));

        assertTrue(result.isContainer());
        assertTrue(result.isArray());
        assertEquals(2, result.size());

        int count = 0;
        for (JsonNode node : result) {
            ++count;
        }
        assertEquals(2, count);

        Iterator<JsonNode> it = result.iterator();

        JsonNode onode = it.next();
        assertTrue(onode.isContainer());
        assertTrue(onode.isObject());
        assertEquals(0, onode.size());
        assertFalse(onode.isMissingNode()); // real node
        assertTrue(onode.asOptional().isPresent());

        // how about dereferencing?
        assertNull(onode.get(0));
        JsonNode dummyNode = onode.path(0);
        assertNotNull(dummyNode);
        assertTrue(dummyNode.isMissingNode());
        assertNull(dummyNode.get(3));
        assertNull(dummyNode.get("whatever"));
        JsonNode dummyNode2 = dummyNode.path(98);
        assertNotNull(dummyNode2);
        assertTrue(dummyNode2.isMissingNode());
        assertFalse(dummyNode2.asOptional().isPresent());
        JsonNode dummyNode3 = dummyNode.path("field");
        assertNotNull(dummyNode3);
        assertTrue(dummyNode3.isMissingNode());

        // and same for the array node

        JsonNode anode = it.next();
        assertTrue(anode.isContainer());
        assertTrue(anode.isArray());
        assertFalse(anode.isMissingNode()); // real node
        assertEquals(0, anode.size());

        assertNull(anode.get(0));
        dummyNode = anode.path(0);
        assertNotNull(dummyNode);
        assertTrue(dummyNode.isMissingNode());
        assertNull(dummyNode.get(0));
        assertNull(dummyNode.get("myfield"));
        assertFalse(dummyNode.asOptional().isPresent());
        dummyNode2 = dummyNode.path(98);
        assertNotNull(dummyNode2);
        assertTrue(dummyNode2.isMissingNode());
        dummyNode3 = dummyNode.path("f");
        assertNotNull(dummyNode3);
        assertTrue(dummyNode3.isMissingNode());
    }
}
