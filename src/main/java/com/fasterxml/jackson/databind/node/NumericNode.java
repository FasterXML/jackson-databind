package com.fasterxml.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.core.JsonParser;

/**
 * Intermediate value node used for numeric nodes.
 */
public abstract class NumericNode
    extends ValueNode
{
    private static final long serialVersionUID = 1L;

    protected NumericNode() { }

    @Override
    public final JsonNodeType getNodeType()
    {
        return JsonNodeType.NUMBER;
    }

    // // // Let's re-abstract so sub-classes handle them

    @Override
    public abstract JsonParser.NumberType numberType();

    @Override public abstract Number numberValue();
    @Override public abstract int intValue();
    @Override public abstract long longValue();
    @Override public abstract double doubleValue();
    @Override public abstract BigDecimal decimalValue();
    @Override public abstract BigInteger bigIntegerValue();

    @Override public abstract boolean canConvertToInt();
    @Override public abstract boolean canConvertToLong();

    /*
    /**********************************************************
    /* General type coercions
    /**********************************************************
     */

    @Override
    public abstract String asText();

    @Override
    public final int asInt() {
        return intValue();
    }

    @Override
    public final int asInt(int defaultValue) {
        return intValue();
    }

    @Override
    public final long asLong() {
        return longValue();
    }

    @Override
    public final long asLong(long defaultValue) {
        return longValue();
    }

    @Override
    public final double asDouble() {
        return doubleValue();
    }

    @Override
    public final double asDouble(double defaultValue) {
        return doubleValue();
    }

    /*
    /**********************************************************
    /* Other
    /**********************************************************
     */

    /**
     * Convenience method for checking whether this node is a
     * {@link FloatNode} or {@link DoubleNode} that contains
     * "not-a-number" (NaN) value.
     *
     * @since 2.9
     */
    public boolean isNaN() {
        return false;
    }

}
