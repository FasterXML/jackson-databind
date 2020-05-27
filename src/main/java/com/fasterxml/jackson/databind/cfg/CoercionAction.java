package com.fasterxml.jackson.databind.cfg;

/**
 * Set of possible actions for requested coercion from an
 * input shape {@link CoercionInputShape}
 * that does not directly or naturally match target type
 * ({@link CoercionTargetType}).
 * This action is suggestion for deserializers to use in cases
 * where alternate actions could be appropriate: it is up to deserializer
 * to check configured action and take it into consideration.
 *
 * @since 2.12
 */
public enum CoercionAction
{

}
