package tools.jackson.databind.testutil.failure;

import java.io.Serial;

/**
 * Exception used to alert that a test is passing, but should be failing.
 *
 * WARNING : This only for test code, and should never be thrown from production code.
 *
 * @since 2.19
 */
public class JacksonTestShouldFailException
    extends RuntimeException
{
    @Serial
    private static final long serialVersionUID = 1L;

    public JacksonTestShouldFailException(String msg) {
        super(msg);
    }
}
