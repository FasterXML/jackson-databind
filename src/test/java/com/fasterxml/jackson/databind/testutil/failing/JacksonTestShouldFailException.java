package com.fasterxml.jackson.databind.testutil.failing;

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
    public JacksonTestShouldFailException(String msg) {
        super(msg);
    }
}