package com.fasterxml.jackson.databind.cfg;

import com.fasterxml.jackson.databind.type.LogicalType;

/**
 * Set of possible actions for requested coercion from an
 * input shape {@link CoercionInputShape}
 * that does not directly or naturally match target type
 * ({@link LogicalType}).
 * This action is suggestion for deserializers to use in cases
 * where alternate actions could be appropriate: it is up to deserializer
 * to check configured action and take it into consideration.
 *
 * @since 2.12
 */
public enum CoercionAction
{
    /**
     * Action to fail coercion attempt with exceptipn
     */
    Fail,

    /**
     * Action to attempt coercion (which may lead to failure)
     */
    TryConvert,

    /**
     * Action to convert to {@code null} value
     */
    AsNull,

    /**
     * Action to convert to "empty" value for type, whatever that is: for
     * primitive types and their wrappers this is "default" value (for example,
     * for {@code int} that would be {@code 0}); for {@link java.util.Collection}s
     * empty collection; for POJOs instance configured with default constructor
     * and so on.
     */
    AsEmpty;
    ;
}
