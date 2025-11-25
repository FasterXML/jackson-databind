package tools.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.function.Predicate;
import java.util.stream.Stream;

import tools.jackson.core.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.util.RawValue;

/**
 * This intermediate base class is used for all container nodes,
 * specifically, array and object nodes.
 */
public abstract class ContainerNode<T extends ContainerNode<T>>
    extends BaseJsonNode
    implements JsonNodeCreator
{
    private static final long serialVersionUID = 3L;

    /**
     * We will keep a reference to the Object (usually TreeMapper)
     * that can construct instances of nodes to add to this container
     * node.
     */
    protected final JsonNodeFactory _nodeFactory;

    protected ContainerNode(JsonNodeFactory nc) {
        _nodeFactory = nc;
    }

    protected ContainerNode() { _nodeFactory = null; } // only for JDK ser

    @Override
    public boolean isContainer() {
        return true;
    }

    // all containers are mutable: can't define:
//    @Override public abstract <T extends JsonNode> T deepCopy();

    @Override
    public abstract JsonToken asToken();

    /*
    /**********************************************************************
    /* Methods reset as abstract to force real implementation
    /**********************************************************************
     */

    @Override
    public abstract int size();

    @Override
    public abstract JsonNode get(int index);

    @Override
    public abstract JsonNode get(String fieldName);

    // Both ArrayNode and ObjectNode must re-implement
    @Override // @since 2.19
    public abstract Stream<JsonNode> valueStream();

    @Override
    protected abstract ObjectNode _withObject(JsonPointer origPtr,
            JsonPointer currentPtr,
            OverwriteMode overwriteMode, boolean preferIndex);

    /*
    /**********************************************************************
    /* JsonNodeCreator implementation, Enumerated/singleton types
    /**********************************************************************
     */

    @Override
    public final BooleanNode booleanNode(boolean v) { return _nodeFactory.booleanNode(v); }

    @Override
    public JsonNode missingNode() {
        return _nodeFactory.missingNode();
    }

    @Override
    public final NullNode nullNode() { return _nodeFactory.nullNode(); }

    /*
    /**********************************************************************
    /* JsonNodeCreator implementation, just dispatch to real creator
    /**********************************************************************
     */

    /**
     * Factory method that constructs and returns an empty {@link ArrayNode}
     * Construction is done using registered {@link JsonNodeFactory}.
     */
    @Override
    public final ArrayNode arrayNode() { return _nodeFactory.arrayNode(); }

    /**
     * Factory method that constructs and returns an {@link ArrayNode} with an initial capacity
     * Construction is done using registered {@link JsonNodeFactory}
     * @param capacity the initial capacity of the ArrayNode
     */
    @Override
    public final ArrayNode arrayNode(int capacity) { return _nodeFactory.arrayNode(capacity); }

    /**
     * Factory method that constructs and returns an empty {@link ObjectNode}
     * Construction is done using registered {@link JsonNodeFactory}.
     */
    @Override
    public final ObjectNode objectNode() { return _nodeFactory.objectNode(); }

    @Override
    public final NumericNode numberNode(byte v) { return _nodeFactory.numberNode(v); }
    @Override
    public final NumericNode numberNode(short v) { return _nodeFactory.numberNode(v); }
    @Override
    public final NumericNode numberNode(int v) { return _nodeFactory.numberNode(v); }
    @Override
    public final NumericNode numberNode(long v) {
        return _nodeFactory.numberNode(v);
    }

    @Override
    public final NumericNode numberNode(float v) { return _nodeFactory.numberNode(v); }
    @Override
    public final NumericNode numberNode(double v) { return _nodeFactory.numberNode(v); }

    @Override
    public final ValueNode numberNode(BigInteger v) { return _nodeFactory.numberNode(v); }
    @Override
    public final ValueNode numberNode(BigDecimal v) { return (_nodeFactory.numberNode(v)); }

    @Override
    public final ValueNode numberNode(Byte v) { return _nodeFactory.numberNode(v); }
    @Override
    public final ValueNode numberNode(Short v) { return _nodeFactory.numberNode(v); }
    @Override
    public final ValueNode numberNode(Integer v) { return _nodeFactory.numberNode(v); }
    @Override
    public final ValueNode numberNode(Long v) { return _nodeFactory.numberNode(v); }

    @Override
    public final ValueNode numberNode(Float v) { return _nodeFactory.numberNode(v); }
    @Override
    public final ValueNode numberNode(Double v) { return _nodeFactory.numberNode(v); }

    @Override
    public final StringNode stringNode(String text) { return _nodeFactory.stringNode(text); }

    @Override
    public final BinaryNode binaryNode(byte[] data) { return _nodeFactory.binaryNode(data); }
    @Override
    public final BinaryNode binaryNode(byte[] data, int offset, int length) { return _nodeFactory.binaryNode(data, offset, length); }

    @Override
    public final ValueNode pojoNode(Object pojo) { return _nodeFactory.pojoNode(pojo); }

    @Override
    public final ValueNode rawValueNode(RawValue value) { return _nodeFactory.rawValueNode(value); }

    /*
    /**********************************************************************
    /* Common mutators
    /**********************************************************************
     */

    /**
     * Method for removing all children container has (if any)
     *
     * @return Container node itself (to allow method call chaining)
     */
    public abstract T removeAll();

    /**
     * Method for removing matching those children (value) nodes container has that
     * match given predicate.
     *
     * @param predicate Predicate to use for matching: anything matching will be removed
     *
     * @return Container node itself (to allow method call chaining)
     *
     * @since 2.19
     */
    public abstract T removeIf(Predicate<? super JsonNode> predicate);

    /**
     * Method for removing {@code null} children (value) nodes container has (that is,
     * children for which {@code isNull()} returns true).
     * Short-cut for:
     *<pre>
     *     removeIf(JsonNode::isNull);
     *</pre>
     *
     * @return Container node itself (to allow method call chaining)
     *
     * @since 2.19
     */
    public T removeNulls() {
        return removeIf(JsonNode::isNull);
    }

    /*
    /**********************************************************************
    /* JsonPointer-based removal (3.1)
    /**********************************************************************
     */

    /**
     * Method for removing the child node pointed to by given {@link JsonPointer},
     * if such a node exists.
     *<p>
     * For example, given JSON document:
     *<pre>
     *  {
     *    "a" : {
     *      "b" : {
     *        "c" : 13
     *      }
     *    }
     *  }
     *</pre>
     * calling {@code remove(JsonPointer.compile("/a/b/c"))} would remove the
     * numeric value {@code 13}, resulting in:
     *<pre>
     *  {
     *    "a" : {
     *      "b" : { }
     *    }
     *  }
     *</pre>
     *<p>
     * If the pointer points to a missing node, nothing is removed and {@code null}
     * is returned.
     *
     * @param ptr Pointer to the node to remove
     *
     * @return The removed node, if it existed; {@link MissingNode} if no node was found
     *   at the specified path
     *
     * @since 3.1
     */
    public JsonNode remove(JsonPointer ptr) {
        // Empty pointer would mean remove this node, but that doesn't make sense
        // as we can't remove ourselves from parent context
        if (ptr.matches()) {
            return missingNode();
        }

        // Navigate to the parent of the target node
        ContainerNode<?> parent = this;
        JsonPointer currentPtr = ptr;

        // Keep navigating until we're at the parent of the target
        while (true) {
            JsonPointer tail = currentPtr.tail();

            // If tail is empty, we're at the parent - remove from here
            if (tail.matches()) {
                return parent._removeAt(currentPtr);
            }

            // Otherwise, navigate one level deeper
            JsonNode next = parent._at(currentPtr);
            if (next instanceof ContainerNode<?> cn) {
                parent = cn;
            } else {
                return missingNode();
            }

            currentPtr = tail;
        }
    }

    /**
     * Internal helper method for removing a node at the current (single-segment)
     * pointer location.
     *
     * @param ptr Pointer with single segment to remove
     *
     * @return The removed node, if it existed; {@link #missingNode()} if not found
     */
    protected abstract JsonNode _removeAt(JsonPointer ptr);
}
