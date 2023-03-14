package tools.jackson.databind.util;

import tools.jackson.databind.BaseTest;

public class ExceptionUtilTest extends BaseTest {
    public void testNoClassDefError() {
        //next line should be a no-op
        ExceptionUtil.rethrowIfFatal(new NoClassDefFoundError("fake"));
    }

    public void testExceptionInInitializerError() {
        //next line should be a no-op
        ExceptionUtil.rethrowIfFatal(new ExceptionInInitializerError("fake"));
    }

    public void testOutOfMemoryError() {
        try {
            ExceptionUtil.rethrowIfFatal(new OutOfMemoryError("fake"));
            fail("expected OutOfMemoryError");
        } catch (OutOfMemoryError err) {
            assertEquals("fake", err.getMessage());
        }
    }

    public void testVerifyError() {
        try {
            ExceptionUtil.rethrowIfFatal(new VerifyError("fake"));
            fail("expected VerifyError");
        } catch (VerifyError err) {
            assertEquals("fake", err.getMessage());
        }
    }
}
