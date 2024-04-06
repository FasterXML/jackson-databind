package com.fasterxml.jackson.databind.exc;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.deser.UnresolvedForwardReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;

public class UnresolvedForwardReferenceTest
{
    private final JsonFactory JSON_F = newJsonMapper().getFactory();

    @Test
    public void testWithAndWithoutStackTraces() throws Exception
    {
        try (JsonParser p = JSON_F.createParser("{}")) {
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
