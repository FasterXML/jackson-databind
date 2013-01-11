package com.fasterxml.jackson.databind.node;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Enumeration of all different {@link JsonNode} types
 *
 * <p>This covers all JSON types defined by <a
 * href="http://tools.ietf.org/html/rfc4627">RFC 4627</a> (array, boolean,
 * null, number, object and string) but also Jackson-specific types: binary,
 * missing and POJO.</p>
 *
 * @see BinaryNode
 * @see MissingNode
 * @see POJONode
 */
public enum JsonNodeType
{
    ARRAY,
    BINARY,
    BOOLEAN,
    MISSING,
    NULL,
    NUMBER,
    OBJECT,
    POJO,
    STRING
}
