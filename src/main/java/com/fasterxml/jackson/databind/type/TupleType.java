package com.fasterxml.jackson.databind.type;

/**
 * Tuple types are defined as list with fixed element types format.
 */
public class TupleType extends SimpleType {
    private static final long serialVersionUID = 1L;

    private SimpleType base;

    protected TupleType(SimpleType base)
    {
        super(base);
        this.base = base;
    }

    public SimpleType getSimpleType()
    {
        return base;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(40);
        sb.append("[tuple type, class ").append(buildCanonicalName()).append(']');
        return sb.toString();
    }
}
