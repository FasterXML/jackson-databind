package com.fasterxml.jackson.databind.util;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class ExceptionUtilTest extends DatabindTestUtil
{
    @Test
    public void testNoClassDefError() {
        //next line should be a no-op
        ExceptionUtil.rethrowIfFatal(new NoClassDefFoundError("fake"));
    }

    @Test
    public void testExceptionInInitializerError() {
        //next line should be a no-op
        ExceptionUtil.rethrowIfFatal(new ExceptionInInitializerError("fake"));
    }

    @Test
    public void testOutOfMemoryError() {
        try {
            ExceptionUtil.rethrowIfFatal(new OutOfMemoryError("fake"));
            fail("expected OutOfMemoryError");
        } catch (OutOfMemoryError err) {
            assertEquals("fake", err.getMessage());
        }
    }

    @Test
    public void testVerifyError() {
        try {
            ExceptionUtil.rethrowIfFatal(new VerifyError("fake"));
            fail("expected VerifyError");
        } catch (VerifyError err) {
            assertEquals("fake", err.getMessage());
        }
    }
}
