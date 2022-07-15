package tools.jackson.databind.exc;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.BaseMapTest;
import tools.jackson.databind.deser.UnresolvedForwardReference;

public class UnresolvedForwardReferenceTest extends BaseMapTest
{
    public void testWithAndWithoutStackTraces() throws Exception
    {
        try (JsonParser p = sharedMapper().createParser("{}")) {
            UnresolvedForwardReference e = new UnresolvedForwardReference(p, "test");
            StackTraceElement[] stack = e.getStackTrace();
            assertEquals(0, stack.length);

            e = e.withStackTrace();
            stack = e.getStackTrace();
            if (stack.length < 1) {
                fail("Should have filled in stack traces, only got: "+stack.length);
            }
        }
    }
}
