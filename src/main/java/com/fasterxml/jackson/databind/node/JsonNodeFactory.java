package com.fasterxml.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Base class that specifies methods for getting access to
 * Node instances (newly constructed, or shared, depending
 * on type), as well as basic implementation of the methods. 
 * Designed to be sub-classed if extended functionality (additions
 * to behavior of node types, mostly) is needed.
 */
public class JsonNodeFactory
    implements java.io.Serializable // since 2.1
        ,JsonNodeCreator // since 2.3
{
    // with 2.2
    private static final long serialVersionUID = -3271940633258788634L;

    private final boolean _cfgBigDecimalExact;

    private static final JsonNodeFactory decimalsNormalized
        = new JsonNodeFactory(false);
    private static final JsonNodeFactory decimalsAsIs
        = new JsonNodeFactory(true);

    /**
     * Default singleton instance that construct "standard" node instances:
     * given that this class is stateless, a globally shared singleton
     * can be used.
     */
    public final static JsonNodeFactory instance = decimalsNormalized;

    /**
     * Main constructor
     *
     * <p>The only argument to this constructor is a boolean telling whether
     * {@link DecimalNode} instances must be built with exact representations of
     * {@link BigDecimal} instances.</p>
     *
     * <p>This has quite an influence since, for instance, a BigDecimal (and,
     * therefore, a DecimalNode) constructed from input string {@code "1.0"} and
     * another constructed with input string {@code "1.00"} <b>will not</b> be
     * equal, since their scale differs (1 in the first case, 2 in the second
     * case).</p>
     *
     * <p>Note that setting the argument to {@code true} does <i>not</i>
     * guarantee a strict inequality between JSON representations: input texts
     * {@code "0.1"} and {@code "1e-1"}, for instance, yield two equivalent
     * BigDecimal instances since they have the same scale (1).</p>
     *
     * <p>The no-arg constructor (and the default {@link #instance}) calls this
     * constructor with {@code false} as an argument.</p>
     *
     * @param bigDecimalExact see description
     *
     * @see BigDecimal
     */
    public JsonNodeFactory(boolean bigDecimalExact)
    {
        _cfgBigDecimalExact = bigDecimalExact;
    }

    /**
     * Default constructor
     *
     * <p>This calls {@link #JsonNodeFactory(boolean)} with {@code false}
     * as an argument.</p>
     */
    protected JsonNodeFactory()
    {
        this(false);
    }

    /**
     * Return a factory instance with the desired behavior for BigDecimals
     * <p>See {@link #JsonNodeFactory(boolean)} for a full description.</p>
     *
     * @param bigDecimalExact see description
     * @return a factory instance
     */
    public static JsonNodeFactory withExactBigDecimals(boolean bigDecimalExact)
    {
        return bigDecimalExact ? decimalsAsIs : decimalsNormalized;
    }
    
    /*
    /**********************************************************
    /* Factory methods for literal values
    /**********************************************************
     */

    /**
     * Factory method for getting an instance of JSON boolean value
     * (either literal 'true' or 'false')
     */
    @Override
    public BooleanNode booleanNode(boolean v) {
        return v ? BooleanNode.getTrue() : BooleanNode.getFalse();
    }

    /**
     * Factory method for getting an instance of JSON null node (which
     * represents literal null value)
     */
    @Override
    public NullNode nullNode() { return NullNode.getInstance(); }

    /*
    /**********************************************************
    /* Factory methods for numeric values
    /**********************************************************
     */

    /**
     * Factory method for getting an instance of JSON numeric value
     * that expresses given 8-bit value
     */
    @Override
    public NumericNode numberNode(byte v) { return IntNode.valueOf(v); }

    /**
     * Alternate factory method that will handle wrapper value, which may
     * be null.
     * Due to possibility of null, returning type is not guaranteed to be
     * {@link NumericNode}, but just {@link ValueNode}.
     */
    @Override
    public ValueNode numberNode(Byte value) {
        return (value == null) ? nullNode() : IntNode.valueOf(value.intValue());
    }
    
    /**
     * Factory method for getting an instance of JSON numeric value
     * that expresses given 16-bit integer value
     */
    @Override
    public NumericNode numberNode(short v) { return ShortNode.valueOf(v); }

    /**
     * Alternate factory method that will handle wrapper value, which may
     * be null.
     * Due to possibility of null, returning type is not guaranteed to be
     * {@link NumericNode}, but just {@link ValueNode}.
     */
    @Override
    public ValueNode numberNode(Short value) {
        return (value == null) ? nullNode() : ShortNode.valueOf(value);
    }
    
    /**
     * Factory method for getting an instance of JSON numeric value
     * that expresses given 32-bit integer value
     */
    @Override
    public NumericNode numberNode(int v) { return IntNode.valueOf(v); }

    /**
     * Alternate factory method that will handle wrapper value, which may
     * be null.
     * Due to possibility of null, returning type is not guaranteed to be
     * {@link NumericNode}, but just {@link ValueNode}.
     */
    @Override
    public ValueNode numberNode(Integer value) {
        return (value == null) ? nullNode() : IntNode.valueOf(value.intValue());
    }
    
    /**
     * Factory method for getting an instance of JSON numeric value
     * that expresses given 64-bit integer value
     */
    @Override
    public NumericNode numberNode(long v) {
        if (_inIntRange(v)) {
            return IntNode.valueOf((int) v);
        }
        return LongNode.valueOf(v);
    }
    
    /**
     * Alternate factory method that will handle wrapper value, which may be null.
     * Due to possibility of null, returning type is not guaranteed to be
     * {@link NumericNode}, but just {@link ValueNode}.
     */
    @Override
    public ValueNode numberNode(Long value) {
        if (value == null) {
            return nullNode();
        }
        long l = value.longValue();
        return _inIntRange(l)
                ? IntNode.valueOf((int) l) : LongNode.valueOf(l);
    }
    
    /**
     * Factory method for getting an instance of JSON numeric value
     * that expresses given unlimited range integer value
     */
    @Override
    public NumericNode numberNode(BigInteger v) { return BigIntegerNode.valueOf(v); }

    /**
     * Factory method for getting an instance of JSON numeric value
     * that expresses given 32-bit floating point value
     */
    @Override
    public NumericNode numberNode(float v) { return FloatNode.valueOf((float) v); }

    /**
     * Alternate factory method that will handle wrapper value, which may
     * be null.
     * Due to possibility of null, returning type is not guaranteed to be
     * {@link NumericNode}, but just {@link ValueNode}.
     */
    @Override
    public ValueNode numberNode(Float value) {
        return (value == null) ? nullNode() : FloatNode.valueOf(value.floatValue());
    }
    
    /**
     * Factory method for getting an instance of JSON numeric value
     * that expresses given 64-bit floating point value
     */
    @Override
    public NumericNode numberNode(double v) { return DoubleNode.valueOf(v); }

    /**
     * Alternate factory method that will handle wrapper value, which may
     * be null.
     * Due to possibility of null, returning type is not guaranteed to be
     * {@link NumericNode}, but just {@link ValueNode}.
     */
    @Override
    public ValueNode numberNode(Double value) {
        return (value == null) ? nullNode() : DoubleNode.valueOf(value.doubleValue());
    }
    
    /**
     * Factory method for getting an instance of JSON numeric value
     * that expresses given unlimited precision floating point value
     *
     * <p>In the event that the factory has been built to normalize decimal
     * values, the BigDecimal argument will be stripped off its trailing zeroes,
     * using {@link BigDecimal#stripTrailingZeros()}.</p>
     *
     * @see #JsonNodeFactory(boolean)
     */
    @Override
    public NumericNode numberNode(BigDecimal v)
    {
        /*
         * If the user wants the exact representation of this big decimal,
         * return the value directly
         */
        if (_cfgBigDecimalExact)
            return DecimalNode.valueOf(v);

        /*
         * If the user has asked to strip trailing zeroes, however, there is
         * this bug to account for:
         *
         * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6480539
         *
         * In short: zeroes are never stripped out of 0! We therefore _have_
         * to compare with BigDecimal.ZERO...
         */
        return v.compareTo(BigDecimal.ZERO) == 0 ? DecimalNode.ZERO
            : DecimalNode.valueOf(v.stripTrailingZeros());
    }

    /*
    /**********************************************************
    /* Factory methods for textual values
    /**********************************************************
     */

    /**
     * Factory method for constructing a node that represents JSON
     * String value
     */
    @Override
    public TextNode textNode(String text) { return TextNode.valueOf(text); }

    /**
     * Factory method for constructing a node that represents given
     * binary data, and will get serialized as equivalent base64-encoded
     * String value
     */
    @Override
    public BinaryNode binaryNode(byte[] data) { return BinaryNode.valueOf(data); }

    /**
     * Factory method for constructing a node that represents given
     * binary data, and will get serialized as equivalent base64-encoded
     * String value
     */
    @Override
    public BinaryNode binaryNode(byte[] data, int offset, int length) {
        return BinaryNode.valueOf(data, offset, length);
    }

    /*
    /**********************************************************
    /* Factory method for structured values
    /**********************************************************
     */

    /**
     * Factory method for constructing an empty JSON Array node
     */
    @Override
    public ArrayNode arrayNode() { return new ArrayNode(this); }

    /**
     * Factory method for constructing an empty JSON Object ("struct") node
     */
    @Override
    public ObjectNode objectNode() { return new ObjectNode(this); }

    /**
     * Factory method for constructing a wrapper for POJO
     * ("Plain Old Java Object") objects; these will get serialized
     * using data binding, usually as JSON Objects, but in some
     * cases as JSON Strings or other node types.
     */
    @Override
    public ValueNode pojoNode(Object pojo) { return new POJONode(pojo); }

    /**
     * @deprecated Since 2.3 Use {@link #pojoNode} instead.
     */
    @Deprecated
    public POJONode POJONode(Object pojo) { return new POJONode(pojo); }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */
    
    protected boolean _inIntRange(long l)
    {
        int i = (int) l;
        long l2 = (long) i;
        return (l2 == l);
    }
}

