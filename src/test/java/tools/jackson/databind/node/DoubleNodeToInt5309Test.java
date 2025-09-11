package tools.jackson.databind.node;

import org.junit.jupiter.api.Test;

import tools.jackson.core.exc.InputCoercionException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

// [databind#5309] Confusing exception for DoubleNode to Integer conversion in Jackson 3 #5309
public class DoubleNodeToInt5309Test
    extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void fpConversionsToIntOk()
    {
        assertEquals(0, MAPPER.treeToValue(MAPPER.valueToTree(0.00), Integer.class));
        assertEquals(0, MAPPER.treeToValue(MAPPER.valueToTree(0.00f), Integer.class));
        assertEquals(9999, MAPPER.treeToValue(MAPPER.valueToTree(9999.0), Integer.class));
        assertEquals(9999, MAPPER.treeToValue(MAPPER.valueToTree(9999.0f), Integer.class));
        assertEquals(-28, MAPPER.treeToValue(MAPPER.valueToTree(-28.0), Integer.class));
        assertEquals(-28, MAPPER.treeToValue(MAPPER.valueToTree(-28.0f), Integer.class));
    }

    @Test
    public void fpConversionsToIntFail()
    {
        try {
            MAPPER.treeToValue(MAPPER.valueToTree(2.75), Integer.class);
            fail("Should have thrown an exception");
        } catch (InputCoercionException e) {
            verifyException(e,
                    "Numeric value (2.75) of `DoubleNode` has fractional part; cannot convert to `int`");
        }

        try {
            MAPPER.treeToValue(MAPPER.valueToTree(-4.75f), Integer.class);
            fail("Should have thrown an exception");
        } catch (InputCoercionException e) {
            verifyException(e,
                    "Numeric value (-4.75) of `FloatNode` has fractional part; cannot convert to `int`");
        }
    }

    @Test
    public void fpConversionsToLongOk()
    {
        assertEquals(0, MAPPER.treeToValue(MAPPER.valueToTree(0.00), Long.class));
        assertEquals(0, MAPPER.treeToValue(MAPPER.valueToTree(0.00f), Long.class));
        assertEquals(9999, MAPPER.treeToValue(MAPPER.valueToTree(9999.0), Long.class));
        assertEquals(9999, MAPPER.treeToValue(MAPPER.valueToTree(9999.0f), Long.class));
        assertEquals(-28, MAPPER.treeToValue(MAPPER.valueToTree(-28.0), Long.class));
        assertEquals(-28, MAPPER.treeToValue(MAPPER.valueToTree(-28.0f), Long.class));
    }

    @Test
    public void fpConversionsToLongFail()
    {
        try {
            MAPPER.treeToValue(MAPPER.valueToTree(1.5), Long.class);
            fail("Should have thrown an exception");
        } catch (InputCoercionException e) {
            verifyException(e,
                    "Numeric value (1.5) of `DoubleNode` has fractional part; cannot convert to `long`");
        }

        try {
            MAPPER.treeToValue(MAPPER.valueToTree(-6.25f), Long.class);
            fail("Should have thrown an exception");
        } catch (InputCoercionException e) {
            verifyException(e,
                    "Numeric value (-6.25) of `FloatNode` has fractional part; cannot convert to `long`");
        }
    }

}
