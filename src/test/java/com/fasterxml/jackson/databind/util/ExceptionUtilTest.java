package com.fasterxml.jackson.databind.util;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class ExceptionUtilTest extends DatabindTestUtil
{
    @Test
    void noClassDefError() {
        //next line should be a no-op
        ExceptionUtil.rethrowIfFatal(new NoClassDefFoundError("fake"));
    }

    @Test
    void exceptionInInitializerError() {
        //next line should be a no-op
        ExceptionUtil.rethrowIfFatal(new ExceptionInInitializerError("fake"));
    }

    @Test
    void outOfMemoryError() {
        try {
            ExceptionUtil.rethrowIfFatal(new OutOfMemoryError("fake"));
            fail("expected OutOfMemoryError");
        } catch (OutOfMemoryError err) {
            assertEquals("fake", err.getMessage());
        }
    }

    @Test
    void verifyError() {
        try {
            ExceptionUtil.rethrowIfFatal(new VerifyError("fake"));
            fail("expected VerifyError");
        } catch (VerifyError err) {
            assertEquals("fake", err.getMessage());
        }
    }
}
