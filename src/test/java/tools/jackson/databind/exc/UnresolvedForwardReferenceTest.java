package tools.jackson.databind.exc;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.deser.UnresolvedForwardReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import static tools.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;

public class UnresolvedForwardReferenceTest
{
    private final ObjectMapper MAPPER = newJsonMapper();
    
    @Test
    public void testWithAndWithoutStackTraces() throws Exception
    {
        try (JsonParser p = MAPPER.createParser("{}")) {
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
