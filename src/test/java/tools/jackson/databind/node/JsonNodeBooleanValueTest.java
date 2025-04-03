package tools.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.exc.JsonNodeException;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.util.RawValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for [databind#4958], {@code JsonNode.booleanValue()}
 * and [databind#5034], {@code JsonNode.asBoolean()}
 * over all node types.
 */
public class JsonNodeBooleanValueTest
    extends DatabindTestUtil
{
    private final JsonNodeFactory NODES = newJsonMapper().getNodeFactory();

    // // // booleanValue(), variants

    @Test
    public void booleanValueOkFromBoolean()
    {
        _assertFalseFromBooleanValue(NODES.booleanNode(false));
        _assertTrueFromBooleanValue(NODES.booleanNode(true));
    }
    
    @Test
    public void booleanValueFailFromNumbersInt()
    {
        _assertFailBooleanValueForNonBoolean(NODES.numberNode((byte) 1));
        _assertFailBooleanValueForNonBoolean(NODES.numberNode((short) 2));
        _assertFailBooleanValueForNonBoolean(NODES.numberNode(3));
        _assertFailBooleanValueForNonBoolean(NODES.numberNode(4L));
        _assertFailBooleanValueForNonBoolean(NODES.numberNode(BigInteger.valueOf(5)));
    }

    @Test
    public void booleanValueFailFromNumbersFloat()
    {
        _assertFailBooleanValueForNonBoolean(NODES.numberNode(0.25f));
        _assertFailBooleanValueForNonBoolean(NODES.numberNode(-2.125d));
        _assertFailBooleanValueForNonBoolean(NODES.numberNode(new BigDecimal("0.1")));
    }

    @Test
    public void booleanValueFailFromNonNumberScalars()
    {
        _assertFailBooleanValueForNonBoolean(NODES.binaryNode(new byte[3]));
        _assertFailBooleanValueForNonBoolean(NODES.stringNode("123"));
        _assertFailBooleanValueForNonBoolean(NODES.rawValueNode(new RawValue("abc")));
        _assertFailBooleanValueForNonBoolean(NODES.pojoNode(new AtomicInteger(1)));
    }

    @Test
    public void booleanValueFailFromStructural()
    {
        _assertFailBooleanValueForNonBoolean(NODES.arrayNode(3));
        _assertFailBooleanValueForNonBoolean(NODES.objectNode());
    }

    @Test
    public void booleanValueFailFromNonNumberMisc()
    {
        _assertFailBooleanValueForNonBoolean(NODES.nullNode());
        _assertFailBooleanValueForNonBoolean(NODES.missingNode());
    }

    // // // asBoolean(), variants

    @Test
    public void asBooleanOkFromBoolean()
    {
        _assertFalseFromAsBoolean(NODES.booleanNode(false));
        _assertTrueFromAsBoolean(NODES.booleanNode(true));
    }

    @Test
    public void asBooleanOkFromNumbersInt()
    {
        _assertFalseFromAsBoolean(NODES.numberNode((byte) 0));
        _assertTrueFromAsBoolean(NODES.numberNode((byte) 1));
        _assertTrueFromAsBoolean(NODES.numberNode((byte) -1));
        _assertFalseFromAsBoolean(NODES.numberNode((short) 0));
        _assertTrueFromAsBoolean(NODES.numberNode((short) 1));
        _assertTrueFromAsBoolean(NODES.numberNode((short) 2));
        _assertFalseFromAsBoolean(NODES.numberNode(0));
        _assertTrueFromAsBoolean(NODES.numberNode(1));
        _assertTrueFromAsBoolean(NODES.numberNode(-15));
        _assertFalseFromAsBoolean(NODES.numberNode(0L));
        _assertTrueFromAsBoolean(NODES.numberNode(1L));
        _assertTrueFromAsBoolean(NODES.numberNode(2L));
        _assertFalseFromAsBoolean(NODES.numberNode(BigInteger.ZERO));
        _assertTrueFromAsBoolean(NODES.numberNode(BigInteger.ONE));
        _assertTrueFromAsBoolean(NODES.numberNode(BigInteger.TEN));
    }

    @Test
    public void asBooleanFailFromNumbersFloat()
    {
        _assertFailAsBooleanForNonBoolean(NODES.numberNode(0.25f));
        _assertFailAsBooleanForNonBoolean(NODES.numberNode(-2.125d));
        _assertFailAsBooleanForNonBoolean(NODES.numberNode(new BigDecimal("0.1")));
    }

    @Test
    public void asBooleanFromNonNumberScalars()
    {
        _assertFailAsBooleanForNonBoolean(NODES.binaryNode(new byte[3]));

        // 2 String values are ok; others not
        _assertFalseFromAsBoolean(NODES.stringNode("false"));
        _assertTrueFromAsBoolean(NODES.stringNode("true"));
        _assertFailAsBooleanForNonBoolean(NODES.stringNode("123"));

        _assertFailAsBooleanForNonBoolean(NODES.rawValueNode(new RawValue("true")));
        // POJONode can also succeed but only from `Boolean`
        _assertFailAsBooleanForNonBoolean(NODES.pojoNode(Long.valueOf(3)));
    }
    @Test
    public void asBooleanFailFromStructural()
    {
        _assertFailAsBooleanForNonBoolean(NODES.arrayNode(3));
        _assertFailAsBooleanForNonBoolean(NODES.objectNode());
    }

    @Test
    public void asBooleanFromNonNumberMisc()
    {
        // Null ok
        _assertFalseFromAsBoolean(NODES.nullNode());
        // And POJO node with Boolean:
        _assertFalseFromAsBoolean(NODES.pojoNode(Boolean.FALSE));
        _assertTrueFromAsBoolean(NODES.pojoNode(Boolean.TRUE));

        // but missing not
        _assertFailAsBooleanForNonBoolean(NODES.missingNode());
    }

    // // // Helper methods

    private void _assertFalseFromBooleanValue(JsonNode n) {
        assertEquals(false, n.booleanValue());
        assertEquals(false, n.booleanValue(true));
        assertEquals(false, n.booleanValueOpt().get());
    }

    private void _assertTrueFromBooleanValue(JsonNode n) {
        assertEquals(true, n.booleanValue());
        assertEquals(true, n.booleanValue(false));
        assertEquals(true, n.booleanValueOpt().get());
    }
    
    private void _assertFalseFromAsBoolean(JsonNode n) {
        assertEquals(false, n.asBoolean());
        assertEquals(false, n.asBoolean(true));
        assertEquals(false, n.asBooleanOpt().get());
    }

    private void _assertTrueFromAsBoolean(JsonNode n) {
        assertEquals(true, n.asBoolean());
        assertEquals(true, n.asBoolean(false));
        assertEquals(true, n.asBooleanOpt().get());
    }
    
    private void _assertFailBooleanValueForNonBoolean(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.booleanValue(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("cannot convert value")
            .contains("value type not boolean");

        // But also check defaulting
        assertFalse(node.booleanValue(false));
        assertTrue(node.booleanValue(true));
        assertFalse(node.booleanValueOpt().isPresent());
    }

    private void _assertFailAsBooleanForNonBoolean(JsonNode node) {
        Exception e = assertThrows(JsonNodeException.class,
                () ->  node.asBoolean(),
                "For ("+node.getClass().getSimpleName()+") value: "+node);
        assertThat(e.getMessage())
            .contains("cannot convert value")
            .contains("value type not coercible to `boolean`");

        // But also check defaulting
        assertFalse(node.asBoolean(false));
        assertTrue(node.asBoolean(true));
        assertFalse(node.asBooleanOpt().isPresent());
    }
}
