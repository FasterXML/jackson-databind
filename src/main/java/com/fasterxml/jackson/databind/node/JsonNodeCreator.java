package com.fasterxml.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;

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

    // Textual nodes, other value (non-structured) nodes

    public ValueNode textNode(String text);
    public ValueNode binaryNode(byte[] data);
    public ValueNode binaryNode(byte[] data, int offset, int length);
    public ValueNode pojoNode(Object pojo);

    // Structured nodes:
    // (bit unkosher, due to forward references... but has to do for now)

    public ArrayNode arrayNode();
    public ObjectNode objectNode();
}
