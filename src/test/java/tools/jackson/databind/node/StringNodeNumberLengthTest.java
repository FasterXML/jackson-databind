package tools.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;
import java.util.OptionalDouble;

import org.junit.jupiter.api.Test;

import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.exc.StreamConstraintsException;

import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that coercing a {@link StringNode} holding a "stringified" number to
 * {@code BigInteger}/{@code BigDecimal}/{@code double}/{@code float} enforces the
 * {@code StreamReadConstraints} number-length limit before the (super-linear) parse,
 * the same way deserializers and (for scale) {@link DecimalNode} already do.
 *
 * @see <a href="https://github.com/FasterXML/jackson-databind/issues/6214">[databind#6214]</a>
 */
public class StringNodeNumberLengthTest extends DatabindTestUtil
{
    private final static int MAX_LEN = StreamReadConstraints.defaults().getMaxNumberLength();

    // A numeric String comfortably past the default limit (arrives as a JSON String,
    // so it is bounded only by max-string-length, not max-number-length)
    private final static int OVER_LEN = MAX_LEN + 100;
    private final static String OVER_LONG_INT = "9".repeat(OVER_LEN);
    private final static String OVER_LONG_DECIMAL = "1." + "9".repeat(OVER_LEN);

    @Test
    public void strictAccessorsRejectOverLongNumber() throws Exception
    {
        StringNode intNode = StringNode.valueOf(OVER_LONG_INT);
        _verifyGuarded(() -> intNode.asBigInteger());
        _verifyGuarded(() -> intNode.asDouble());
        _verifyGuarded(() -> intNode.asFloat());
        _verifyGuarded(() -> intNode.asDecimal());

        StringNode decNode = StringNode.valueOf(OVER_LONG_DECIMAL);
        _verifyGuarded(() -> decNode.asDouble());
        _verifyGuarded(() -> decNode.asFloat());
        _verifyGuarded(() -> decNode.asDecimal());
    }

    @Test
    public void lenientAccessorsReturnDefaultForOverLongNumber() throws Exception
    {
        StringNode node = StringNode.valueOf(OVER_LONG_INT);

        // default/Optional variants must NOT throw (same contract as any other
        // non-convertible value): they return the default / empty instead
        assertEquals(BigInteger.ONE, node.asBigInteger(BigInteger.ONE));
        assertFalse(node.asBigIntegerOpt().isPresent());

        assertEquals(-1.0, node.asDouble(-1.0));
        assertFalse(node.asDoubleOpt().isPresent());

        assertEquals(-1.0f, node.asFloat(-1.0f));
        assertFalse(node.asFloatOpt().isPresent());

        assertEquals(BigDecimal.ONE, node.asDecimal(BigDecimal.ONE));
        assertFalse(node.asDecimalOpt().isPresent());
    }

    // Values within the limit must still coerce, unchanged
    @Test
    public void withinLimitStillCoerces() throws Exception
    {
        StringNode node = StringNode.valueOf("1234");
        assertEquals(new BigInteger("1234"), node.asBigInteger());
        assertEquals(new BigInteger("1234"), node.asBigInteger(BigInteger.ZERO));
        assertEquals(Optional.of(new BigInteger("1234")), node.asBigIntegerOpt());
        assertEquals(1234.0, node.asDouble());
        assertEquals(OptionalDouble.of(1234.0), node.asDoubleOpt());
        assertEquals(1234.0f, node.asFloat());
        assertEquals(new BigDecimal("1234"), node.asDecimal());

        // length exactly at the limit is still accepted
        StringNode atLimit = StringNode.valueOf("9".repeat(MAX_LEN));
        assertEquals(new BigInteger("9".repeat(MAX_LEN)), atLimit.asBigInteger());
    }

    private interface Coercion { Object convert(); }

    private void _verifyGuarded(Coercion c) throws Exception
    {
        try {
            c.convert();
            fail("Should not pass: number length exceeds the configured maximum");
        } catch (StreamConstraintsException e) {
            verifyException(e, "Number value length");
            verifyException(e, "exceeds the maximum allowed");
        }
    }
}
