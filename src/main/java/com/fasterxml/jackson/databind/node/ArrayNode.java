package com.fasterxml.jackson.databind.node;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.WritableTypeId;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.util.RawValue;

/**
 * Node class that represents Arrays mapped from JSON content.
 *<p>
 * Note: class was <code>final</code> temporarily for Jackson 2.2.
 */
public class ArrayNode
    extends ContainerNode<ArrayNode>
    implements java.io.Serializable // since 2.10
{
    private static final long serialVersionUID = 1L;

    private final List<JsonNode> _children;

    public ArrayNode(JsonNodeFactory nf) {
        super(nf);
        _children = new ArrayList<JsonNode>();
    }

    /**
     * @since 2.8
     */
    public ArrayNode(JsonNodeFactory nf, int capacity) {
        super(nf);
        _children = new ArrayList<JsonNode>(capacity);
    }

    /**
     * @since 2.7
     */
    public ArrayNode(JsonNodeFactory nf, List<JsonNode> children) {
        super(nf);
        _children = children;
    }

    @Override
    protected JsonNode _at(JsonPointer ptr) {
        return get(ptr.getMatchingIndex());
    }

    // note: co-variant to allow caller-side type safety
    @SuppressWarnings("unchecked")
    @Override
    public ArrayNode deepCopy()
    {
        ArrayNode ret = new ArrayNode(_nodeFactory);

        for (JsonNode element: _children)
            ret._children.add(element.deepCopy());

        return ret;
    }

    /*
    /**********************************************************
    /* Support for withArray()/withObject()
    /**********************************************************
     */

    @SuppressWarnings("unchecked")
    @Deprecated
    @Override
    public ObjectNode with(String exprOrProperty) {
        JsonPointer ptr = _jsonPointerIfValid(exprOrProperty);
        if (ptr != null) {
            return withObject(ptr);
        }
        return super.with(exprOrProperty); // to give failure
    }

    @SuppressWarnings("unchecked")
    @Override
    public ArrayNode withArray(String exprOrProperty)
    {
        JsonPointer ptr = _jsonPointerIfValid(exprOrProperty);
        if (ptr != null) {
            return withArray(ptr);
        }
        return super.withArray(exprOrProperty); // to give failure
    }

    @Override
    protected ObjectNode _withObject(JsonPointer origPtr,
            JsonPointer currentPtr,
            OverwriteMode overwriteMode, boolean preferIndex)
    {
        if (currentPtr.matches()) {
            // Cannot return, not an ObjectNode so:
            return null;
        }
        JsonNode n = _at(currentPtr);
        // If there's a path, follow it
        if ((n != null) && (n instanceof BaseJsonNode)) {
            ObjectNode found = ((BaseJsonNode) n)._withObject(origPtr, currentPtr.tail(),
                    overwriteMode, preferIndex);
            if (found != null) {
                return found;
            }
            // Ok no; must replace if allowed to
            _withXxxVerifyReplace(origPtr, currentPtr, overwriteMode, preferIndex, n);
        }
        // Either way; must replace or add a new property
        return _withObjectAddTailElement(currentPtr, preferIndex);
    }

    @Override
    protected ArrayNode _withArray(JsonPointer origPtr,
            JsonPointer currentPtr,
            OverwriteMode overwriteMode, boolean preferIndex)
    {
        if (currentPtr.matches()) {
            return this;
        }
        JsonNode n = _at(currentPtr);
        // If there's a path, follow it
        if ((n != null) && (n instanceof BaseJsonNode)) {
            ArrayNode found = ((BaseJsonNode) n)._withArray(origPtr, currentPtr.tail(),
                    overwriteMode, preferIndex);
            if (found != null) {
                return found;
            }
            // Ok no; must replace if allowed to
            _withXxxVerifyReplace(origPtr, currentPtr, overwriteMode, preferIndex, n);
        }
        // Either way; must replace or add a new property
        return _withArrayAddTailElement(currentPtr, preferIndex);
    }

    protected ObjectNode _withObjectAddTailElement(JsonPointer tail, boolean preferIndex)
    {
        final int index = tail.getMatchingIndex();
        if (index < 0) {
            return null;
        }

        tail = tail.tail();

        // First: did we complete traversal? If so, easy, we got our result
        if (tail.matches()) {
            ObjectNode result = this.objectNode();
            _withXxxSetArrayElement(index, result);
            return result;
        }

        // Otherwise, do we want Array or Object
        if (preferIndex && tail.mayMatchElement()) { // array!
            ArrayNode next = this.arrayNode();
            _withXxxSetArrayElement(index, next);
            return next._withObjectAddTailElement(tail, preferIndex);
        }
        ObjectNode next = this.objectNode();
        _withXxxSetArrayElement(index, next);
        return next._withObjectAddTailProperty(tail, preferIndex);
    }

    protected ArrayNode _withArrayAddTailElement(JsonPointer tail, boolean preferIndex)
    {
        final int index = tail.getMatchingIndex();
        if (index < 0) {
            return null;
        }
        tail = tail.tail();

        // First: did we complete traversal? If so, easy, we got our result
        if (tail.matches()) {
            ArrayNode result = this.arrayNode();
            _withXxxSetArrayElement(index, result);
            return result;
        }

        // Otherwise, do we want Array or Object
        if (preferIndex && tail.mayMatchElement()) { // array!
            ArrayNode next = this.arrayNode();
            _withXxxSetArrayElement(index, next);
            return next._withArrayAddTailElement(tail, preferIndex);
        }
        ArrayNode next = this.arrayNode();
        _withXxxSetArrayElement(index, next);
        return next._withArrayAddTailElement(tail, preferIndex);
    }

    protected void _withXxxSetArrayElement(int index, JsonNode value) {
        // 27-Jul-2022, tatu: Let's make it less likely anyone OOMs by
        //    humongous index...
        if (index >= size()) {
            final int max = _nodeFactory.getMaxElementIndexForInsert();
            if (index > max) {
                _reportWrongNodeOperation("Too big Array index (%d; max %d) to use for insert with `JsonPointer`",
                        index, max);
            }
            while (index >= this.size()) {
                addNull();
            }
        }
        set(index, value);
    }

    /*
    /**********************************************************
    /* Overrides for JsonSerializable.Base
    /**********************************************************
     */

    @Override
    public boolean isEmpty(SerializerProvider serializers) {
        return _children.isEmpty();
    }

    /*
    /**********************************************************
    /* Implementation of core JsonNode API
    /**********************************************************
     */

    @Override
    public JsonNodeType getNodeType() {
        return JsonNodeType.ARRAY;
    }

    @Override
    public boolean isArray() {
        return true;
    }

    @Override public JsonToken asToken() { return JsonToken.START_ARRAY; }

    @Override
    public int size() {
        return _children.size();
    }

    @Override // since 2.10
    public boolean isEmpty() { return _children.isEmpty(); }

    @Override
    public Iterator<JsonNode> elements() {
        return _children.iterator();
    }

    @Override
    public JsonNode get(int index) {
        if ((index >= 0) && (index < _children.size())) {
            return _children.get(index);
        }
        return null;
    }

    @Override
    public JsonNode get(String fieldName) { return null; }

    @Override
    public JsonNode path(String fieldName) { return MissingNode.getInstance(); }

    @Override
    public JsonNode path(int index) {
        if (index >= 0 && index < _children.size()) {
            return _children.get(index);
        }
        return MissingNode.getInstance();
    }

    @Override
    public JsonNode required(int index) {
        if ((index >= 0) && (index < _children.size())) {
            return _children.get(index);
        }
        return _reportRequiredViolation("No value at index #%d [0, %d) of `ArrayNode`",
                index, _children.size());
    }

    @Override
    public boolean equals(Comparator<JsonNode> comparator, JsonNode o)
    {
        if (!(o instanceof ArrayNode)) {
            return false;
        }
        ArrayNode other = (ArrayNode) o;
        final int len = _children.size();
        if (other.size() != len) {
            return false;
        }
        List<JsonNode> l1 = _children;
        List<JsonNode> l2 = other._children;
        for (int i = 0; i < len; ++i) {
            if (!l1.get(i).equals(comparator, l2.get(i))) {
                return false;
            }
        }
        return true;
    }

    /*
    /**********************************************************
    /* Public API, serialization
    /**********************************************************
     */

    @Override
    public void serialize(JsonGenerator g, SerializerProvider provider) throws IOException
    {
        final List<JsonNode> c = _children;
        final int size = c.size();
        g.writeStartArray(this, size);
        for (int i = 0; i < size; ++i) { // we'll typically have array list
            JsonNode value = c.get(i);
            value.serialize(g, provider);
        }
        g.writeEndArray();
    }

    @Override
    public void serializeWithType(JsonGenerator g, SerializerProvider provider, TypeSerializer typeSer)
        throws IOException
    {
        WritableTypeId typeIdDef = typeSer.writeTypePrefix(g,
                typeSer.typeId(this, JsonToken.START_ARRAY));
        for (JsonNode n : _children) {
            ((BaseJsonNode)n).serialize(g, provider);
        }
        typeSer.writeTypeSuffix(g, typeIdDef);
    }

    /*
    /**********************************************************
    /* Public API, finding value nodes
    /**********************************************************
     */

    @Override
    public JsonNode findValue(String fieldName)
    {
        for (JsonNode node : _children) {
            JsonNode value = node.findValue(fieldName);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    @Override
    public List<JsonNode> findValues(String fieldName, List<JsonNode> foundSoFar)
    {
        for (JsonNode node : _children) {
            foundSoFar = node.findValues(fieldName, foundSoFar);
        }
        return foundSoFar;
    }

    @Override
    public List<String> findValuesAsText(String fieldName, List<String> foundSoFar)
    {
        for (JsonNode node : _children) {
            foundSoFar = node.findValuesAsText(fieldName, foundSoFar);
        }
        return foundSoFar;
    }

    @Override
    public ObjectNode findParent(String fieldName)
    {
        for (JsonNode node : _children) {
            JsonNode parent = node.findParent(fieldName);
            if (parent != null) {
                return (ObjectNode) parent;
            }
        }
        return null;
    }

    @Override
    public List<JsonNode> findParents(String fieldName, List<JsonNode> foundSoFar)
    {
        for (JsonNode node : _children) {
            foundSoFar = node.findParents(fieldName, foundSoFar);
        }
        return foundSoFar;
    }

    /*
    /**********************************************************
    /* Extended ObjectNode API, accessors
    /**********************************************************
     */

    /**
     * Method that will set specified field, replacing old value,
     * if any.
     *
     * @param value to set field to; if null, will be converted
     *   to a {@link NullNode} first  (to remove field entry, call
     *   {@link #remove} instead)
     *
     * @return Old value of the field, if any; null if there was no
     *   old value.
     */
    public JsonNode set(int index, JsonNode value)
    {
        if (value == null) { // let's not store 'raw' nulls but nodes
            value = nullNode();
        }
        if (index < 0 || index >= _children.size()) {
            throw new IndexOutOfBoundsException("Illegal index "+ index +", array size "+size());
        }
        return _children.set(index, value);
    }

    /**
     * Method for adding specified node at the end of this array.
     *
     * @return This node, to allow chaining
     */
    public ArrayNode add(JsonNode value)
    {
        if (value == null) { // let's not store 'raw' nulls but nodes
            value = nullNode();
        }
        _add(value);
        return this;
    }

    /**
     * Method for adding all child nodes of given Array, appending to
     * child nodes this array contains
     *
     * @param other Array to add contents from
     *
     * @return This node (to allow chaining)
     */
    public ArrayNode addAll(ArrayNode other)
    {
        _children.addAll(other._children);
        return this;
    }

    /**
     * Method for adding given nodes as child nodes of this array node.
     *
     * @param nodes Nodes to add
     *
     * @return This node (to allow chaining)
     */
    public ArrayNode addAll(Collection<? extends JsonNode> nodes)
    {
        for (JsonNode node : nodes) {
            add(node);
        }
        return this;
    }

    /**
     * Method for inserting specified child node as an element
     * of this Array. If index is 0 or less, it will be inserted as
     * the first element; if {@code >= size()}, appended at the end, and otherwise
     * inserted before existing element in specified index.
     * No exceptions are thrown for any index.
     *
     * @return This node (to allow chaining)
     */
    public ArrayNode insert(int index, JsonNode value)
    {
        if (value == null) {
            value = nullNode();
        }
        _insert(index, value);
        return this;
    }

    /**
     * Method for removing an entry from this ArrayNode.
     * Will return value of the entry at specified index, if entry existed;
     * null if not.
     *
     * @return Node removed, if any; null if none
     */
    public JsonNode remove(int index)
    {
        if (index >= 0 && index < _children.size()) {
            return _children.remove(index);
        }
        return null;
    }

    /**
     * Method for removing all elements of this array, leaving the
     * array empty.
     *
     * @return This node (to allow chaining)
     */
    @Override
    public ArrayNode removeAll()
    {
        _children.clear();
        return this;
    }

    /*
    /**********************************************************
    /* Extended ObjectNode API, mutators, generic; addXxx()/insertXxx()/setXxx()
    /**********************************************************
     */

    /**
     * Method that will construct an ArrayNode and add it at the end
     * of this array node.
     *
     * @return Newly constructed ArrayNode (NOTE: NOT `this` ArrayNode)
     */
    public ArrayNode addArray()
    {
        ArrayNode n  = arrayNode();
        _add(n);
        return n;
    }

    /**
     * Method that will construct an ObjectNode and add it at the end
     * of this array node.
     *
     * @return Newly constructed ObjectNode (NOTE: NOT `this` ArrayNode)
     */
    public ObjectNode addObject()
    {
        ObjectNode n  = objectNode();
        _add(n);
        return n;
    }

    /**
     * Method that will construct a POJONode and add it at the end
     * of this array node.
     *
     * @return This array node, to allow chaining
     */
    public ArrayNode addPOJO(Object pojo) {
        return _add((pojo == null) ? nullNode() : pojoNode(pojo));
    }

    /**
     * @return This array node, to allow chaining
     *
     * @since 2.6
     */
    public ArrayNode addRawValue(RawValue raw) {
        return _add((raw == null) ? nullNode() : rawValueNode(raw));
    }

    /**
     * Method that will add a null value at the end of this array node.
     *
     * @return This array node, to allow chaining
     */
    public ArrayNode addNull() {
        return _add(nullNode());
    }

    /**
     * Method for adding specified number at the end of this array.
     *
     * @return This array node, to allow chaining
     *
     * @since 2.13
     */
    public ArrayNode add(short v) {
        return _add(numberNode(v));
    }

    /**
     * Alternative method that we need to avoid bumping into NPE issues
     * with auto-unboxing.
     *
     * @return This array node, to allow chaining
     *
     * @since 2.13
     */
    public ArrayNode add(Short v) {
        return _add((v == null) ? nullNode() : numberNode(v.shortValue()));
    }

    /**
     * Method for adding specified number at the end of this array.
     *
     * @return This array node, to allow chaining
     */
    public ArrayNode add(int v) {
        return _add(numberNode(v));
    }

    /**
     * Alternative method that we need to avoid bumping into NPE issues
     * with auto-unboxing.
     *
     * @return This array node, to allow chaining
     */
    public ArrayNode add(Integer v) {
        return _add((v == null) ? nullNode() : numberNode(v.intValue()));
    }

    /**
     * Method for adding specified number at the end of this array.
     *
     * @return This array node, to allow chaining
     */
    public ArrayNode add(long v) { return _add(numberNode(v)); }

    /**
     * Alternative method that we need to avoid bumping into NPE issues
     * with auto-unboxing.
     *
     * @return This array node, to allow chaining
     */
    public ArrayNode add(Long v) {
        return _add((v == null) ? nullNode() : numberNode(v.longValue()));
    }

    /**
     * Method for adding specified number at the end of this array.
     *
     * @return This array node, to allow chaining
     */
    public ArrayNode add(float v) {
        return _add(numberNode(v));
    }

    /**
     * Alternative method that we need to avoid bumping into NPE issues
     * with auto-unboxing.
     *
     * @return This array node, to allow chaining
     */
    public ArrayNode add(Float v) {
        return _add((v == null) ? nullNode() : numberNode(v.floatValue()));
    }

    /**
     * Method for adding specified number at the end of this array.
     *
     * @return This array node, to allow chaining
     */
    public ArrayNode add(double v) {
        return _add(numberNode(v));
    }

    /**
     * Alternative method that we need to avoid bumping into NPE issues
     * with auto-unboxing.
     *
     * @return This array node, to allow chaining
     */
    public ArrayNode add(Double v) {
        return _add((v == null) ? nullNode() : numberNode(v.doubleValue()));
    }

    /**
     * Method for adding specified number at the end of this array.
     *
     * @return This array node, to allow chaining
     */
    public ArrayNode add(BigDecimal v) {
        return _add((v == null) ? nullNode() : numberNode(v));
    }

    /**
     * Method for adding specified number at the end of this array.
     *
     * @return This array node, to allow chaining
     *
     * @since 2.9
     */
    public ArrayNode add(BigInteger v) {
        return _add((v == null) ? nullNode() : numberNode(v));
    }

    /**
     * Method for adding specified String value at the end of this array.
     *
     * @return This array node, to allow chaining
     */
    public ArrayNode add(String v) {
        return _add((v == null) ? nullNode() : textNode(v));
    }

    /**
     * Method for adding specified boolean value at the end of this array.
     *
     * @return This array node, to allow chaining
     */
    public ArrayNode add(boolean v) {
        return _add(booleanNode(v));
    }

    /**
     * Alternative method that we need to avoid bumping into NPE issues
     * with auto-unboxing.
     *
     * @return This array node, to allow chaining
     */
    public ArrayNode add(Boolean v) {
        return _add((v == null) ? nullNode() : booleanNode(v.booleanValue()));
    }

    /**
     * Method for adding specified binary value at the end of this array
     * (note: when serializing as JSON, will be output Base64 encoded)
     *
     * @return This array node, to allow chaining
     */
    public ArrayNode add(byte[] v) {
        return _add((v == null) ? nullNode() : binaryNode(v));
    }

    /**
     * Method for creating an array node, inserting it at the
     * specified point in the array,
     * and returning the <b>newly created array</b>
     * (note: NOT 'this' array)
     *
     * @return Newly constructed {@code ArrayNode} (note! NOT `this` ArrayNode)
     */
    public ArrayNode insertArray(int index)
    {
        ArrayNode n  = arrayNode();
        _insert(index, n);
        return n;
    }

    /**
     * Method for creating an {@link ObjectNode}, appending it at the end
     * of this array, and returning the <b>newly created node</b>
     * (note: NOT 'this' array)
     *
     * @return Newly constructed {@code ObjectNode} (note! NOT `this` ArrayNode)
     */
    public ObjectNode insertObject(int index)
    {
        ObjectNode n  = objectNode();
        _insert(index, n);
        return n;
    }

    /**
     * Method that will insert a null value
     * at specified position in this array.
     *
     * @return This array node, to allow chaining
     */
    public ArrayNode insertNull(int index) {
        return _insert(index, nullNode());
    }

    /**
     * Method that will construct a POJONode and
     * insert it at specified position in this array.
     *
     * @return This array node, to allow chaining
     */
    public ArrayNode insertPOJO(int index, Object pojo) {
        return _insert(index, (pojo == null) ? nullNode() : pojoNode(pojo));
    }

    /**
     * @return This array node, to allow chaining
     *
     * @since 2.13
     */
    public ArrayNode insertRawValue(int index, RawValue raw) {
        return _insert(index, (raw == null) ? nullNode() : rawValueNode(raw));
    }

    /**
     * Method that will insert specified numeric value
     * at specified position in this array.
     *
     * @return This array node, to allow chaining
     *
     * @since 2.13
     */
    public ArrayNode insert(int index, short v) {
        return _insert(index, numberNode(v));
    }

    /**
     * Alternative method that we need to avoid bumping into NPE issues
     * with auto-unboxing.
     *
     * @return This array node, to allow chaining
     *
     * @since 2.13
     */
    public ArrayNode insert(int index, Short value) {
        return _insert(index, (value == null) ? nullNode() : numberNode(value.shortValue()));
    }

    /**
     * Method that will insert specified numeric value
     * at specified position in this array.
     *
     * @return This array node, to allow chaining
     */
    public ArrayNode insert(int index, int v) {
        return _insert(index, numberNode(v));
    }

    /**
     * Alternative method that we need to avoid bumping into NPE issues
     * with auto-unboxing.
     *
     * @return This array node, to allow chaining
     */
    public ArrayNode insert(int index, Integer v) {
        return _insert(index, (v == null) ? nullNode() : numberNode(v.intValue()));
    }

    /**
     * Method that will insert specified numeric value
     * at specified position in this array.
     *
     * @return This array node, to allow chaining
     */
    public ArrayNode insert(int index, long v) {
        return _insert(index, numberNode(v));
    }

    /**
     * Alternative method that we need to avoid bumping into NPE issues
     * with auto-unboxing.
     *
     * @return This array node, to allow chaining
     */
    public ArrayNode insert(int index, Long v) {
        return _insert(index, (v == null) ? nullNode() : numberNode(v.longValue()));
    }

    /**
     * Method that will insert specified numeric value
     * at specified position in this array.
     *
     * @return This array node, to allow chaining
     */
    public ArrayNode insert(int index, float v) {
        return _insert(index, numberNode(v));
    }

    /**
     * Alternative method that we need to avoid bumping into NPE issues
     * with auto-unboxing.
     *
     * @return This array node, to allow chaining
     */
    public ArrayNode insert(int index, Float v) {
        return _insert(index, (v == null) ? nullNode() : numberNode(v.floatValue()));
    }

    /**
     * Method that will insert specified numeric value
     * at specified position in this array.
     *
     * @return This array node, to allow chaining
     */
    public ArrayNode insert(int index, double v) {
        return _insert(index, numberNode(v));
    }

    /**
     * Alternative method that we need to avoid bumping into NPE issues
     * with auto-unboxing.
     *
     * @return This array node, to allow chaining
     */
    public ArrayNode insert(int index, Double v) {
        return _insert(index, (v == null) ? nullNode() : numberNode(v.doubleValue()));
    }

    /**
     * Method that will insert specified numeric value
     * at specified position in this array.
     *
     * @return This array node, to allow chaining
     */
    public ArrayNode insert(int index, BigDecimal v) {
        return _insert(index, (v == null) ? nullNode() : numberNode(v));
    }

    /**
     * Method that will insert specified numeric value
     * at specified position in this array.
     *
     * @return This array node, to allow chaining
     *
     * @since 2.9
     */
    public ArrayNode insert(int index, BigInteger v) {
        return _insert(index, (v == null) ? nullNode() : numberNode(v));
    }

    /**
     * Method that will insert specified String
     * at specified position in this array.
     *
     * @return This array node, to allow chaining
     */
    public ArrayNode insert(int index, String v) {
        return _insert(index, (v == null) ? nullNode() : textNode(v));
    }

    /**
     * Method that will insert specified String
     * at specified position in this array.
     *
     * @return This array node, to allow chaining
     */
    public ArrayNode insert(int index, boolean v) {
        return _insert(index, booleanNode(v));
    }

    /**
     * Alternative method that we need to avoid bumping into NPE issues
     * with auto-unboxing.
     *
     * @return This array node, to allow chaining
     */
    public ArrayNode insert(int index, Boolean value) {
        if (value == null) {
            return insertNull(index);
        }
        return _insert(index, booleanNode(value.booleanValue()));
    }

    /**
     * Method that will insert specified binary value
     * at specified position in this array
     * (note: when written as JSON, will be Base64 encoded)
     *
     * @return This array node, to allow chaining
     */
    public ArrayNode insert(int index, byte[] v) {
        if (v == null) {
            return insertNull(index);
        }
        return _insert(index, binaryNode(v));
    }

    /**
     * @return This node (to allow chaining)
     *
     * @since 2.13
     */
    public ArrayNode setNull(int index) {
        return _set(index, nullNode());
    }

    /**
     * @return This array node, to allow chaining
     *
     * @since 2.13
     */
    public ArrayNode setPOJO(int index, Object pojo) {
        return _set(index, (pojo == null) ? nullNode() : pojoNode(pojo));
    }

    /**
     * @return This array node, to allow chaining
     *
     * @since 2.13
     */
    public ArrayNode setRawValue(int index, RawValue raw) {
        return _set(index, (raw == null) ? nullNode() : rawValueNode(raw));
    }

    /**
     * @return This node (to allow chaining)
     *
     * @since 2.13
     */
    public ArrayNode set(int index, short v) {
        return _set(index, numberNode(v));
    }

    /**
     * Alternative method that we need to avoid bumping into NPE issues
     * with auto-unboxing.
     *
     * @return This node (to allow chaining)
     *
     * @since 2.13
     */
    public ArrayNode set(int index, Short v) {
        return _set(index, (v == null) ? nullNode() : numberNode(v.shortValue()));
    }

    /**
     * @return This node (to allow chaining)
     *
     * @since 2.13
     */
    public ArrayNode set(int index, int v) {
        return _set(index, numberNode(v));
    }

    /**
     * Alternative method that we need to avoid bumping into NPE issues
     * with auto-unboxing.
     *
     * @return This node (to allow chaining)
     *
     * @since 2.13
     */
    public ArrayNode set(int index, Integer v) {
        return _set(index, (v == null) ? nullNode() : numberNode(v.intValue()));
    }

    /**
     * @return This node (to allow chaining)
     *
     * @since 2.13
     */
    public ArrayNode set(int index, long v) {
        return _set(index, numberNode(v));
    }

    /**
     * @return This node (to allow chaining)
     *
     * @since 2.13
     */
    public ArrayNode set(int index, Long v) {
        return _set(index, (v == null) ? nullNode() : numberNode(v.longValue()));
    }

    /**
     * Method for setting value of a field to specified numeric value.
     *
     * @return This node (to allow chaining)
     *
     * @since 2.13
     */
    public ArrayNode set(int index, float v) {
        return _set(index, numberNode(v));
    }

    /**
     * Alternative method that we need to avoid bumping into NPE issues
     * with auto-unboxing.
     *
     * @return This node (to allow chaining)
     *
     * @since 2.13
     */
    public ArrayNode set(int index, Float v) {
        return _set(index, (v == null) ? nullNode() : numberNode(v.floatValue()));
    }

    /**
     * Method for setting value of a field to specified numeric value.
     *
     * @return This node (to allow chaining)
     *
     * @since 2.13
     */
    public ArrayNode set(int index, double v) {
        return _set(index, numberNode(v));
    }

    /**
     * Alternative method that we need to avoid bumping into NPE issues
     * with auto-unboxing.
     *
     * @return This node (to allow chaining)
     *
     * @since 2.13
     */
    public ArrayNode set(int index, Double v) {
        return _set(index, (v == null) ? nullNode() : numberNode(v.doubleValue()));
    }

    /**
     * Method for setting value of a field to specified numeric value.
     *
     * @return This node (to allow chaining)
     *
     * @since 2.13
     */
    public ArrayNode set(int index, BigDecimal v) {
        return _set(index, (v == null) ? nullNode() : numberNode(v));
    }

    /**
     * Method for setting value of a field to specified numeric value.
     *
     * @return This node (to allow chaining)
     *
     * @since 2.13
     */
    public ArrayNode set(int index, BigInteger v) {
        return _set(index, (v == null) ? nullNode() : numberNode(v));
    }

    /**
     * Method for setting value of a field to specified String value.
     *
     * @return This node (to allow chaining)
     *
     * @since 2.13
     */
    public ArrayNode set(int index, String v) {
        return _set(index, (v == null) ? nullNode() : textNode(v));
    }

    /**
     * Method for setting value of a field to specified String value.
     *
     * @return This node (to allow chaining)
     *
     * @since 2.13
     */
    public ArrayNode set(int index, boolean v) {
        return _set(index, booleanNode(v));
    }

    /**
     * Alternative method that we need to avoid bumping into NPE issues
     * with auto-unboxing.
     *
     * @return This node (to allow chaining)
     *
     * @since 2.13
     */
    public ArrayNode set(int index, Boolean v) {
        return _set(index, (v == null) ? nullNode() : booleanNode(v.booleanValue()));
    }

    /**
     * Method for setting value of a field to specified binary value
     *
     * @return This node (to allow chaining)
     *
     * @since 2.13
     */
    public ArrayNode set(int index, byte[] v) {
        return _set(index, (v == null) ? nullNode() : binaryNode(v));
    }

    /*
    /**********************************************************
    /* Standard methods
    /**********************************************************
     */

    @Override
    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (o instanceof ArrayNode) {
            return _children.equals(((ArrayNode) o)._children);
        }
        return false;
    }

    /**
     * @since 2.3
     */
    protected boolean _childrenEqual(ArrayNode other) {
        return _children.equals(other._children);
    }

    @Override
    public int hashCode() {
        return _children.hashCode();
    }

    /*
    /**********************************************************
    /* Internal methods (overridable)
    /**********************************************************
     */

    protected ArrayNode _set(int index, JsonNode node) {
        if (index < 0 || index >= _children.size()) {
            throw new IndexOutOfBoundsException("Illegal index "+ index +", array size "+size());
        }
        _children.set(index, node);
        return this;
    }

    protected ArrayNode _add(JsonNode node) {
        _children.add(node);
        return this;
    }

    protected ArrayNode _insert(int index, JsonNode node)
    {
        if (index < 0) {
            _children.add(0, node);
        } else if (index >= _children.size()) {
            _children.add(node);
        } else {
            _children.add(index, node);
        }
        return this;
    }
}
