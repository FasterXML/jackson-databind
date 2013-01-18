package com.fasterxml.jackson.databind.node;

import java.math.BigDecimal;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * This intermediate base class is used for all container nodes,
 * specifically, array and object nodes.
 */
public abstract class ContainerNode<T extends ContainerNode<T>>
    extends BaseJsonNode
{
    /**
     * We will keep a reference to the Object (usually TreeMapper)
     * that can construct instances of nodes to add to this container
     * node.
     */
    protected final JsonNodeFactory _nodeFactory;

    protected ContainerNode(JsonNodeFactory nc)
    {
        _nodeFactory = nc;
    }

    // all containers are mutable: can't define:
//    @Override public abstract <T extends JsonNode> T deepCopy();

    @Override
    public abstract JsonToken asToken();
    
    @Override
    public String asText() { return ""; }

    /*
    /**********************************************************
    /* Methods reset as abstract to force real implementation
    /**********************************************************
     */

    @Override
    public abstract int size();

    @Override
    public abstract JsonNode get(int index);

    @Override
    public abstract JsonNode get(String fieldName);

    /*
    /**********************************************************
    /* NodeCreator implementation, just dispatch to
    /* the real creator
    /**********************************************************
     */

    /**
     * Factory method that constructs and returns an empty {@link ArrayNode}
     * Construction is done using registered {@link JsonNodeFactory}.
     */
    public final ArrayNode arrayNode() { return _nodeFactory.arrayNode(); }

    /**
     * Factory method that constructs and returns an empty {@link ObjectNode}
     * Construction is done using registered {@link JsonNodeFactory}.
     */
    public final ObjectNode objectNode() { return _nodeFactory.objectNode(); }

    public final NullNode nullNode() { return _nodeFactory.nullNode(); }

    public final BooleanNode booleanNode(boolean v) { return _nodeFactory.booleanNode(v); }

    public final NumericNode numberNode(byte v) { return _nodeFactory.numberNode(v); }
    public final NumericNode numberNode(short v) { return _nodeFactory.numberNode(v); }
    public final NumericNode numberNode(int v) { return _nodeFactory.numberNode(v); }
    public final NumericNode numberNode(long v) { return _nodeFactory.numberNode(v); }
    public final NumericNode numberNode(float v) { return _nodeFactory.numberNode(v); }
    public final NumericNode numberNode(double v) { return _nodeFactory.numberNode(v); }
    public final NumericNode numberNode(BigDecimal v) { return (_nodeFactory.numberNode(v)); }

    public final TextNode textNode(String text) { return _nodeFactory.textNode(text); }

    public final BinaryNode binaryNode(byte[] data) { return _nodeFactory.binaryNode(data); }
    public final BinaryNode binaryNode(byte[] data, int offset, int length) { return _nodeFactory.binaryNode(data, offset, length); }

    public final POJONode POJONode(Object pojo) { return _nodeFactory.POJONode(pojo); }

    /*
    /**********************************************************
    /* Common mutators
    /**********************************************************
     */

    /**
     * Method for removing all children container has (if any)
     *
     * @return Container node itself (to allow method call chaining)
     */
    public abstract T removeAll();

    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */
}
