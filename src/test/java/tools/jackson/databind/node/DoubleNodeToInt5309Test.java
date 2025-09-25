package tools.jackson.databind.node;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

// [databind#5309] Confusing exception for DoubleNode to Integer conversion in Jackson 3
public class DoubleNodeToInt5309Test
    extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .enable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .build();

    private final ObjectMapper STRICT_MAPPER = jsonMapperBuilder()
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .build();

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
            STRICT_MAPPER.treeToValue(STRICT_MAPPER.valueToTree(2.75), Integer.class);
            fail("Should have thrown an exception");
        } catch (InvalidFormatException e) {
            verifyException(e,
                    "Cannot coerce Floating-point value (2.75) to `java.lang.Integer`");
        }

        try {
            STRICT_MAPPER.treeToValue(STRICT_MAPPER.valueToTree(-4.75f), Integer.class);
            fail("Should have thrown an exception");
        } catch (InvalidFormatException e) {
            verifyException(e,
                    "Cannot coerce Floating-point value (-4.75) to `java.lang.Integer`");
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
            STRICT_MAPPER.treeToValue(STRICT_MAPPER.valueToTree(1.5), Long.class);
            fail("Should have thrown an exception");
        } catch (InvalidFormatException e) {
            verifyException(e,
                    "Cannot coerce Floating-point value (1.5) to `java.lang.Long`");
        }

        try {
            STRICT_MAPPER.treeToValue(STRICT_MAPPER.valueToTree(-6.25f), Long.class);
            fail("Should have thrown an exception");
        } catch (InvalidFormatException e) {
            verifyException(e,
                    "Cannot coerce Floating-point value (-6.25) to `java.lang.Long`");
        }
    }

}
