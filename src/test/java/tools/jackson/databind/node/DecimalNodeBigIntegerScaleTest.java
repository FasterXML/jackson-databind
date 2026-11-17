package tools.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.exc.StreamConstraintsException;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify that conversion of {@link DecimalNode} to {@link BigInteger}
 * is guarded against excessive {@link BigDecimal} scale magnitude (which would
 * make conversion extremely expensive), same as streaming parsers are.
 *
 * @see <a href="https://github.com/FasterXML/jackson-databind/issues/6214">[databind#6214]</a>
 */
public class DecimalNodeBigIntegerScaleTest extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .build();

    // Short text, but scale magnitude way beyond limit (100k)
    private final static String HUGE_NEG_SCALE = "1e2000000000"; // scale -2_000_000_000
    private final static String HUGE_POS_SCALE = "1e-2000000000"; // scale +2_000_000_000

    @Test
    public void testAccessorsGuarded() throws Exception
    {
        // Huge negative scale (integral value): strict accessors must fail...
        DecimalNode neg = DecimalNode.valueOf(new BigDecimal(HUGE_NEG_SCALE));
        _verifyGuarded(HUGE_NEG_SCALE, () -> neg.bigIntegerValue());
        _verifyGuarded(HUGE_NEG_SCALE, () -> neg.asBigInteger());

        // ... but default/Optional variants must return default/empty instead
        // of throwing, same as for other conversion failures (NaN, fraction)
        assertEquals(BigInteger.ONE, neg.bigIntegerValue(BigInteger.ONE));
        assertFalse(neg.bigIntegerValueOpt().isPresent());
        assertEquals(BigInteger.ONE, neg.asBigInteger(BigInteger.ONE));
        assertFalse(neg.asBigIntegerOpt().isPresent());

        // Huge positive scale (has fractional part): strict `bigIntegerValue()`
        // variants reject it earlier (fraction check, cheap); coercing
        // `asBigInteger()` variants would truncate and so must be guarded
        DecimalNode pos = DecimalNode.valueOf(new BigDecimal(HUGE_POS_SCALE));
        assertTrue(pos.hasFractionalPart());
        _verifyGuarded(HUGE_POS_SCALE, () -> pos.asBigInteger());
        assertEquals(BigInteger.ONE, pos.asBigInteger(BigInteger.ONE));
        assertFalse(pos.asBigIntegerOpt().isPresent());
    }

    // POJONode wrapping a BigDecimal has its own conversion path
    @Test
    public void testPOJONodeAccessorsGuarded() throws Exception
    {
        for (String num : new String[] { HUGE_NEG_SCALE, HUGE_POS_SCALE }) {
            POJONode n = new POJONode(new BigDecimal(num));
            // Strict accessor must fail...
            _verifyGuarded(num, () -> n.asBigInteger());
            // ... but default/Optional variants return default/empty, as with
            // `DecimalNode` (and other conversion failures)
            assertEquals(BigInteger.ONE, n.asBigInteger(BigInteger.ONE));
            assertFalse(n.asBigIntegerOpt().isPresent());
        }
        assertEquals(new BigInteger("1000"), new POJONode(new BigDecimal("1e3")).asBigInteger());
    }

    @Test
    public void testViaTreeTraversingParser() throws Exception
    {
        JsonNode root = MAPPER.readTree("{\"v\": " + HUGE_NEG_SCALE + "}");
        assertTrue(root.get("v").isBigDecimal());
        try (JsonParser p = MAPPER.treeAsTokens(root)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            _verifyGuarded(HUGE_NEG_SCALE, () -> p.getBigIntegerValue());
        }
    }

    @Test
    public void testViaConversion() throws Exception
    {
        JsonNode n = MAPPER.readTree(HUGE_NEG_SCALE);
        _verifyGuarded(HUGE_NEG_SCALE, () -> MAPPER.treeToValue(n, BigInteger.class));
    }

    // Values within limits must still convert
    @Test
    public void testWithinLimits() throws Exception
    {
        assertEquals(new BigInteger("1000"), DecimalNode.valueOf(new BigDecimal("1e3")).bigIntegerValue());
        assertEquals(BigInteger.ZERO, DecimalNode.valueOf(new BigDecimal("1e-3")).asBigInteger());
        // scale magnitude exactly at limit (100_000) is fine
        BigDecimal atLimit = new BigDecimal("1e-100000");
        assertEquals(BigInteger.ZERO, DecimalNode.valueOf(atLimit).asBigInteger());
        assertEquals(BigInteger.ZERO, DecimalNode.valueOf(atLimit).asBigInteger(BigInteger.TEN));

        // ... and so is the other (expensive but legal) direction: negative scale
        // at limit yields an actual 100k-digit integer, and must NOT be rejected
        DecimalNode atNegLimit = DecimalNode.valueOf(new BigDecimal("1e100000"));
        assertEquals(-100_000, atNegLimit.decimalValue().scale());
        assertEquals(332193, atNegLimit.bigIntegerValue().bitLength());
        assertEquals(332193, atNegLimit.asBigInteger(BigInteger.ONE).bitLength());
        assertTrue(atNegLimit.bigIntegerValueOpt().isPresent());
    }

    private interface Conversion { Object convert() throws Exception; }

    private void _verifyGuarded(String num, Conversion c) throws Exception
    {
        try {
            c.convert();
            fail("Should not pass for: " + num);
        } catch (StreamConstraintsException e) {
            verifyException(e, "BigDecimal scale");
            verifyException(e, "magnitude exceeds the maximum allowed");
        }
    }
}
