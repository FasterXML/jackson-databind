package com.fasterxml.jackson.databind.util;

/**
 * Enumeration used to indicate required access pattern for providers:
 * this can sometimes be used to optimize out dynamic calls.
 * The main difference is between constant values (which can be resolved once)
 * and dynamic ones (which must be resolved anew every time).
 */
public enum AccessPattern {
    /**
     * Value that indicates that provider never returns anything other than
     * Java `null`.
     */
    ALWAYS_NULL,

    /**
     * Value that indicates that provider will always return a constant
     * value, regardless of when it is called; and also that it never
     * uses `context` argument (which may then be passed as `null`)
     */
    CONSTANT,

    /**
     * Value that indicates that provider may return different values
     * at different times (often a freshly constructed empty container),
     * and thus must be called every time "null replacement" value is
     * needed.
     */
    DYNAMIC
    ;
}
