package com.fasterxml.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.databind.util.RawValue;

/**
 * Interface that defines common "creator" functionality implemented
 * both by {@link JsonNodeFactory} and {@link ContainerNode} (that is,
 * JSON Object and Array nodes).
 *
 * @since 2.3
 */
public interface JsonNodeCreator
{
    // Enumerated/singleton types

    public ValueNode booleanNode(boolean v);
    public ValueNode nullNode();

// Not yet in 2.10, will be added in 3.0
//    public JsonNode missingNode();

    // Numeric types

    public ValueNode numberNode(byte v);
    public ValueNode numberNode(Byte value);
    public ValueNode numberNode(short v);
    public ValueNode numberNode(Short value);
    public ValueNode numberNode(int v);
    public ValueNode numberNode(Integer value);
    public ValueNode numberNode(long v);
    public ValueNode numberNode(Long value);
    public ValueNode numberNode(BigInteger v);
    public ValueNode numberNode(float v);
    public ValueNode numberNode(Float value);
    public ValueNode numberNode(double v);
    public ValueNode numberNode(Double value);
    public ValueNode numberNode(BigDecimal v);

    // Textual nodes

    public ValueNode textNode(String text);

    // Other value (non-structured) nodes

    public ValueNode binaryNode(byte[] data);
    public ValueNode binaryNode(byte[] data, int offset, int length);
    public ValueNode pojoNode(Object pojo);

    /**
     * Factory method to use for adding "raw values"; pre-encoded values
     * that are included exactly as-is when node is serialized.
     * This may be used, for example, to include fully serialized JSON
     * sub-trees.
     * Note that the concept may not work with all backends, and since
     * no translation of any kinds is done it will not work when converting
     * between data formats.
     *
     * @since 2.6
     */
    public ValueNode rawValueNode(RawValue value);

    // Structured nodes:
    // (bit unkosher, due to forward references... but has to do for now)

    public ArrayNode arrayNode();

    /**
     * Factory method for constructing a JSON Array node with an initial capacity
     *
     * @since 2.8
     */
    public ArrayNode arrayNode(int capacity);

    public ObjectNode objectNode();
}
