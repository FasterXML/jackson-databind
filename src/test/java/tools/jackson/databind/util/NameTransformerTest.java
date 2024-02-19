package tools.jackson.databind.util;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.Assert.assertEquals;

public class NameTransformerTest extends DatabindTestUtil
{
    @Test
    public void testSimpleTransformer() throws Exception
    {
        NameTransformer xfer;

        xfer = NameTransformer.simpleTransformer("a", null);
        assertEquals("aFoo", xfer.transform("Foo"));
        assertEquals("Foo", xfer.reverse("aFoo"));

        xfer = NameTransformer.simpleTransformer(null, "++");
        assertEquals("foo++", xfer.transform("foo"));
        assertEquals("foo", xfer.reverse("foo++"));

        xfer = NameTransformer.simpleTransformer("(", ")");
        assertEquals("(foo)", xfer.transform("foo"));
        assertEquals("foo", xfer.reverse("(foo)"));
    }
}
