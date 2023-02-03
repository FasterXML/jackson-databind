package com.fasterxml.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.util.RawValue;

/**
 * Base class that specifies methods for getting access to
 * Node instances (newly constructed, or shared, depending
 * on type), as well as basic implementation of the methods.
 * Designed to be sub-classed if extended functionality (additions
 * to behavior of node types, mostly) is needed.
 *<p>
 * One configuration option in the default implementation is that
 * of whether
 * {@link DecimalNode} instances must be built with exact representations of
 * {@link BigDecimal} instances; or to use "normalized" instance.
 * This has quite an influence since, for instance, a {@link BigDecimal} (and,
 * therefore, a {@code DecimalNode}) constructed from input string {@code "1.0"} and
 * another constructed with input string {@code "1.00"} <b>will not</b> be
 * equal unless normalized since their scale differs (1 in the first case,
 * 2 in the second case).
 * Normalization, if enabled, means simply calling {@link BigDecimal#stripTrailingZeros()}.
 *<p>
 * Note that configuration of "normalization" changed in 2.15:
 * while {@code JsonNodeFactory} still has a default setting,
 * the intent is to deprecate and remove this, to be replaced by
 * new {@link com.fasterxml.jackson.databind.cfg.JsonNodeFeature#STRIP_TRAILING_BIGDECIMAL_ZEROES}
 * setting.
 * Default setting in 2.15 is to ENABLE normalization: this will likely
 * change at latest in Jackson 3.0 (to leave {@code BigDecimal} values as
 * they are).
 * Note, too, that this factory will no longer handle this normalization
 * (if enabled): caller (like {@link com.fasterxml.jackson.databind.deser.std.JsonNodeDeserializer})
 * is expected to handle it.
 */
public class JsonNodeFactory
    implements java.io.Serializable, // since 2.1
        JsonNodeCreator // since 2.3
{
    // with 2.2
    private static final long serialVersionUID = 1L;

    /**
     * Constant that defines maximum {@code JsonPointer} element index we
     * use for inserts.
     */
    protected final static int MAX_ELEMENT_INDEX_FOR_INSERT = 9999;

    @Deprecated // as of 2.15
    private final boolean _cfgBigDecimalExact;

    /**
     * Default singleton instance that construct "standard" node instances:
     * given that this class is stateless, a globally shared singleton
     * can be used.
     */
    public final static JsonNodeFactory instance = new JsonNodeFactory();

    /**
     * @param bigDecimalExact see Class description on "BigDecimal normalization"
     *
     * @see BigDecimal
     */
    public JsonNodeFactory(boolean bigDecimalExact)
    {
        _cfgBigDecimalExact = bigDecimalExact;
    }

    /**
     * Default constructor.
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
     * @param bigDecimalExact If {code true} DISABLE normalization of {@link BigDecimal} values;
     *    if {code false} ENABLE normalization
     * @return a factory instance with specified configuration
     *
     * @deprecated Use {@link com.fasterxml.jackson.databind.cfg.JsonNodeFeature#STRIP_TRAILING_BIGDECIMAL_ZEROES}
     *   instead for configuring behavior.
     */
    @Deprecated
    public static JsonNodeFactory withExactBigDecimals(boolean bigDecimalExact)
    {
        return new JsonNodeFactory(bigDecimalExact);
    }

    /*
    /**********************************************************
    /* Metadata/config access
    /**********************************************************
     */

    /**
     * @since 2.14
     */
    public int getMaxElementIndexForInsert() {
        return MAX_ELEMENT_INDEX_FOR_INSERT;
    }

    /**
     * Accessor needed by {@code JsonNodeDeserializer}.
     *
     * @since 2.15
     */
    public boolean willStripTrailingBigDecimalZeroes() {
        return !_cfgBigDecimalExact;
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

    public JsonNode missingNode() {
        return MissingNode.getInstance();
    }

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
        return LongNode.valueOf(v);
    }

    /**
     * Alternate factory method that will handle wrapper value, which may be null.
     * Due to possibility of null, returning type is not guaranteed to be
     * {@link NumericNode}, but just {@link ValueNode}.
     */
    @Override
    public ValueNode numberNode(Long v) {
        if (v == null) {
            return nullNode();
        }
        return LongNode.valueOf(v.longValue());
    }

    /**
     * Factory method for getting an instance of JSON numeric value
     * that expresses given unlimited range integer value
     */
    @Override
    public ValueNode numberNode(BigInteger v) {
        if (v == null) {
            return nullNode();
        }
        return BigIntegerNode.valueOf(v);
    }

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
     * <p>
     * Note that regardless whether the factory has been built to normalize decimal
     * values (see class JavaDoc), the {@link BigDecimal} argument will NOT be
     * modified by this method -- caller will need to handle normalization, if any.
     *
     * @see #JsonNodeFactory(boolean)
     */
    @Override
    public ValueNode numberNode(BigDecimal v)
    {
        if (v == null) {
            return nullNode();
        }
        // 23-Jan-2023, tatu: As per [databind#3651] it's now up to caller
        //   to do normalization, if any; we will construct node with given value
        return DecimalNode.valueOf(v);
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
     * Factory method for constructing a JSON Array node with an initial capacity
     *
     * @since 2.8
     */
    @Override
    public ArrayNode arrayNode(int capacity) { return new ArrayNode(this, capacity); }

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

    @Override
    public ValueNode rawValueNode(RawValue value) {
        return new POJONode(value);
    }

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

