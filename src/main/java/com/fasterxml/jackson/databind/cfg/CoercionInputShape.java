package com.fasterxml.jackson.databind.cfg;

import com.fasterxml.jackson.databind.type.LogicalType;

/**
 * Set of input types (which mostly match one of
 * {@link tools.jackson.core.JsonToken} types) used for
 * configuring {@link CoercionAction}s to take when reading
 * input into target types (specific type or {@link LogicalType}).
 * Contains both physical input shapes (which match one of
 * {@link tools.jackson.core.JsonToken} types) and a few
 * logical input shapes ("empty" variants).
 *<p>
 * Note that {@code null} input shape is explicitly not included as
 * its configuration is distinct from other types.
 *
 * @since 2.12
 */
public enum CoercionInputShape
{
    // Physical types

    /**
     * Shape of Array values from input (token sequence from
     * {@link tools.jackson.core.JsonToken#START_ARRAY} to
     * {@link tools.jackson.core.JsonToken#END_ARRAY})
     */
    Array,

    /**
     * Shape of Object values from input (token sequence from
     * {@link tools.jackson.core.JsonToken#START_OBJECT} to
     * {@link tools.jackson.core.JsonToken#END_OBJECT})
     */
    Object,

    /**
     * Shape of integral (non-floating point) numeric values from input (token
     * {@link tools.jackson.core.JsonToken#VALUE_NUMBER_INT})
     */
    Integer,

    /**
     * Shape of floating point (non-integral) numeric values from input (token
     * {@link tools.jackson.core.JsonToken#VALUE_NUMBER_FLOAT})
     */
    Float,

    /**
     * Shape of boolean values from input (tokens
     * {@link tools.jackson.core.JsonToken#VALUE_TRUE} and
     * {@link tools.jackson.core.JsonToken#VALUE_FALSE})
     */
    Boolean,

    /**
     * Shape of string values from input (tokens
     * {@link tools.jackson.core.JsonToken#VALUE_STRING})
     */
    String,

    /**
     * Shape of binary data values from input, if expressed natively
     * by underlying format (many
     * textual formats, including JSON, do not have such shape); if so
     * generally seen as {@link tools.jackson.core.JsonToken#VALUE_EMBEDDED_OBJECT}.
     */
    Binary,

    // Logical types

    /**
     * Special case of Array values with no actual content (sequence of 2 tokens:
     * {@link tools.jackson.core.JsonToken#START_ARRAY},
     * {@link tools.jackson.core.JsonToken#END_ARRAY}):
     * usually used to allow special coercion into "empty" or {@code null} target type.
     */
    EmptyArray,

    /**
     * Special case of Object values with no actual content (sequence of 2 tokens:
     * {@link tools.jackson.core.JsonToken#START_OBJECT},
     * {@link tools.jackson.core.JsonToken#END_OBJECT}):
     * usually used to allow special coercion into "empty" or {@code null} target type.
     */
    EmptyObject,
    
    /**
     * Special case for String values with no content (or, if allowed by format or specific
     * configuration, also "blank" String, that is, all-whitespace content).
     * usually used to allow special coercion into "empty" or {@code null} target type.
     */
    EmptyString

    ;
}
