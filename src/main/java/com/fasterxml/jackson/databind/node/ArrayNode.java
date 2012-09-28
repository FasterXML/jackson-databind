package com.fasterxml.jackson.databind.node;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

/**
 * Node class that represents Arrays mapped from Json content.
 */
public class ArrayNode
    extends ContainerNode<ArrayNode>
{
    // before 2.1, was explicitly `ArrayList`
    protected List<JsonNode> _children;

    public ArrayNode(JsonNodeFactory nc) { super(nc); }

    protected ArrayNode(JsonNodeFactory nc, List<JsonNode> children) {
        super(nc);
        _children = children;
    }
    
    // note: co-variant to allow caller-side type safety
    @SuppressWarnings("unchecked")
    @Override
    public ArrayNode deepCopy()
    {
        /* 28-Sep-2012, tatu: Sub-classes really should override this method to
         *   produce compliant copies.
         */
        if (getClass() != ArrayNode.class) {
            throw new IllegalStateException("ArrayNode subtype ("+getClass().getName()+" does not override deepCopy(), needs to");
        }
        return _defaultDeepCopy();
    }

    /**
     * Default implementation for 'deepCopy()': can be delegated to by sub-classes
     * if necessary; but usually isn't.
     */
    protected ArrayNode _defaultDeepCopy()
    {
        if (_children == null) {
            return new ArrayNode(_nodeFactory);
        }
        final int len = _children.size();
        List<JsonNode> newKids = _createList(Math.max(4, len));
        for (int i = 0; i < len; ++i) {
            newKids.add(_children.get(i).deepCopy());
        }
        return new ArrayNode(_nodeFactory, newKids);
    }
    
    /*
    /**********************************************************
    /* Implementation of core JsonNode API
    /**********************************************************
     */

    @Override public JsonToken asToken() { return JsonToken.START_ARRAY; }

    @Override
    public boolean isArray() { return true; }

    @Override
    public int size()
    {
        return (_children == null) ? 0 : _children.size();
    }

    @Override
    public Iterator<JsonNode> elements()
    {
        return (_children == null) ? NoNodesIterator.instance() : _children.iterator();
    }

    @Override
    public JsonNode get(int index)
    {
        if (index >= 0 && (_children != null) && index < _children.size()) {
            return _children.get(index);
        }
        return null;
    }

    @Override
    public JsonNode get(String fieldName) { return null; }

    @Override
    public JsonNode path(String fieldName) { return MissingNode.getInstance(); }

    @Override
    public JsonNode path(int index)
    {
        if (index >= 0 && (_children != null) && index < _children.size()) {
            return _children.get(index);
        }
        return MissingNode.getInstance();
    }
    
    /*
    /**********************************************************
    /* Public API, serialization
    /**********************************************************
     */

    @Override
    public final void serialize(JsonGenerator jg, SerializerProvider provider)
        throws IOException, JsonProcessingException
    {
        jg.writeStartArray();
        if (_children != null) {
            for (JsonNode n : _children) {
                /* 17-Feb-2009, tatu: Can we trust that all nodes will always
                 *   extend BaseJsonNode? Or if not, at least implement
                 *   JsonSerializable? Let's start with former, change if
                 *   we must.
                 */
                ((BaseJsonNode)n).serialize(jg, provider);
            }
        }
        jg.writeEndArray();
    }

    @Override
    public void serializeWithType(JsonGenerator jg, SerializerProvider provider,
            TypeSerializer typeSer)
        throws IOException, JsonProcessingException
    {
        typeSer.writeTypePrefixForArray(this, jg);
        if (_children != null) {
            for (JsonNode n : _children) {
                ((BaseJsonNode)n).serialize(jg, provider);
            }
        }
        typeSer.writeTypeSuffixForArray(this, jg);
    }
    
    /*
    /**********************************************************
    /* Public API, finding value nodes
    /**********************************************************
     */
    
    @Override
    public JsonNode findValue(String fieldName)
    {
        if (_children != null) {
            for (JsonNode node : _children) {
                JsonNode value = node.findValue(fieldName);
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }
    
    @Override
    public List<JsonNode> findValues(String fieldName, List<JsonNode> foundSoFar)
    {
        if (_children != null) {
            for (JsonNode node : _children) {
                foundSoFar = node.findValues(fieldName, foundSoFar);
            }
        }
        return foundSoFar;
    }

    @Override
    public List<String> findValuesAsText(String fieldName, List<String> foundSoFar)
    {
        if (_children != null) {
            for (JsonNode node : _children) {
                foundSoFar = node.findValuesAsText(fieldName, foundSoFar);
            }
        }
        return foundSoFar;
    }
    
    @Override
    public ObjectNode findParent(String fieldName)
    {
        if (_children != null) {
            for (JsonNode node : _children) {
                JsonNode parent = node.findParent(fieldName);
                if (parent != null) {
                    return (ObjectNode) parent;
                }
            }
        }
        return null;        
    }

    @Override
    public List<JsonNode> findParents(String fieldName, List<JsonNode> foundSoFar)
    {
        if (_children != null) {
            for (JsonNode node : _children) {
                foundSoFar = node.findParents(fieldName, foundSoFar);
            }
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
        return _set(index, value);
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
        int len = other.size();
        if (len > 0) {
            if (_children == null) {
                _children = _createList(len+2);
            }
            other.addContentsTo(_children);
        }
        return this;
    }

    /**
     * Method for adding given nodes as child nodes of this array node.
     * 
     * @param nodes Nodes to add
     * 
     * @return This node (to allow chaining)
     */
    public ArrayNode addAll(Collection<JsonNode> nodes)
    {
        int len = nodes.size();
        if (len > 0) {
            if (_children == null) {
                _children = _createList(nodes.size());
            }
            _children.addAll(nodes);
        }
        return this;
    }
    
    /**
     * Method for inserting specified child node as an element
     * of this Array. If index is 0 or less, it will be inserted as
     * the first element; if >= size(), appended at the end, and otherwise
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
        if (index >= 0 && (_children != null) && index < _children.size()) {
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
        _children = null;
        return this;
    }
    
    /*
    /**********************************************************
    /* Extended ObjectNode API, mutators, generic; addXxx()/insertXxx()
    /**********************************************************
     */

    /**
     * Method that will construct an ArrayNode and add it as a
     * field of this ObjectNode, replacing old value, if any.
     *
     * @return Newly constructed ArrayNode
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
     * @return Newly constructed ObjectNode
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
    public ArrayNode addPOJO(Object value)
    {
        if (value == null) {
            addNull();
        } else {
            _add(POJONode(value));
        }
        return this;
    }

    /**
     * Method that will add a null value at the end of this array node.
     * 
     * @return This array node, to allow chaining
     */
    public ArrayNode addNull()
    {
        _add(nullNode());
        return this;
    }

    /**
     * Method for adding specified number at the end of this array.
     * 
     * @return This array node, to allow chaining
     */
    public ArrayNode add(int v) {
        _add(numberNode(v));
        return this;
    }

    /**
     * Alternative method that we need to avoid bumping into NPE issues
     * with auto-unboxing.
     * 
     * @return This array node, to allow chaining
     */
    public ArrayNode add(Integer value) {
        if (value == null) {
            return addNull();
        }
        return _add(numberNode(value.intValue()));
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
    public ArrayNode add(Long value) {
        if (value == null) {
            return addNull();
        }
        return _add(numberNode(value.longValue()));
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
    public ArrayNode add(Float value) {
        if (value == null) {
            return addNull();
        }
        return _add(numberNode(value.floatValue()));
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
    public ArrayNode add(Double value) {
        if (value == null) {
            return addNull();
        }
        return _add(numberNode(value.doubleValue()));
    }
    
    /**
     * Method for adding specified number at the end of this array.
     * 
     * @return This array node, to allow chaining
     */
    public ArrayNode add(BigDecimal v) {
        if (v == null) {
            return addNull();
        }
        return _add(numberNode(v));
    }

    /**
     * Method for adding specified String value at the end of this array.
     * 
     * @return This array node, to allow chaining
     */
    public ArrayNode add(String v) {
        if (v == null) {
            return addNull();
        }
        return _add(textNode(v));
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
    public ArrayNode add(Boolean value) {
        if (value == null) {
            return addNull();
        }
        return _add(booleanNode(value.booleanValue()));
    }
    
    /**
     * Method for adding specified binary value at the end of this array
     * (note: when serializing as JSON, will be output Base64 encoded)
     * 
     * @return This array node, to allow chaining
     */
    public ArrayNode add(byte[] v) {
        if (v == null) {
            return addNull();
        }
        return _add(binaryNode(v));
    }

    /**
     * Method for creating an array node, inserting it at the
     * specified point in the array,
     * and returning the <b>newly created array</b>
     * (note: NOT 'this' array)
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
     * @return Newly constructed ObjectNode
     */
    public ObjectNode insertObject(int index)
    {
        ObjectNode n  = objectNode();
        _insert(index, n);
        return n;
    }

    /**
     * Method that will construct a POJONode and
     * insert it at specified position in this array.
     * 
     * @return This array node, to allow chaining
     */
    public ArrayNode insertPOJO(int index, Object value)
    {
        if (value == null) {
            return insertNull(index);
        }
        return _insert(index, POJONode(value));
    }

    /**
     * Method that will insert a null value
     * at specified position in this array.
     * 
     * @return This array node, to allow chaining
     */
    public ArrayNode insertNull(int index)
    {
        _insert(index, nullNode());
        return this;
    }

    /**
     * Method that will insert specified numeric value
     * at specified position in this array.
     * 
     * @return This array node, to allow chaining
     */
    public ArrayNode insert(int index, int v) {
        _insert(index, numberNode(v));
        return this;
    }

    /**
     * Alternative method that we need to avoid bumping into NPE issues
     * with auto-unboxing.
     * 
     * @return This array node, to allow chaining
     */
    public ArrayNode insert(int index, Integer value) {
        if (value == null) {
            insertNull(index);
        } else {
            _insert(index, numberNode(value.intValue()));
        }
        return this;
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
    public ArrayNode insert(int index, Long value) {
        if (value == null) {
            return insertNull(index);
        }
        return _insert(index, numberNode(value.longValue()));
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
    public ArrayNode insert(int index, Float value) {
        if (value == null) {
            return insertNull(index);
        }
        return _insert(index, numberNode(value.floatValue()));
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
    public ArrayNode insert(int index, Double value) {
        if (value == null) {
            return insertNull(index);
        }
        return _insert(index, numberNode(value.doubleValue()));
    }

    /**
     * Method that will insert specified numeric value
     * at specified position in this array.
     * 
     * @return This array node, to allow chaining
     */
    public ArrayNode insert(int index, BigDecimal v) {
        if (v == null) {
            return insertNull(index);
        }
        return _insert(index, numberNode(v));
    }

    /**
     * Method that will insert specified String
     * at specified position in this array.
     * 
     * @return This array node, to allow chaining
     */
    public ArrayNode insert(int index, String v) {
        if (v == null) {
            return insertNull(index);
        }
        return _insert(index, textNode(v));
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

    /*
    /**********************************************************
    /* Package methods (for other node classes to use)
    /**********************************************************
     */

    protected void addContentsTo(List<JsonNode> dst)
    {
        if (_children != null) {
            for (JsonNode n : _children) {
                dst.add(n);
            }
        }
    }

    /*
    /**********************************************************
    /* Overridable methods
    /**********************************************************
     */

    /**
     * Internal factory method for creating {@link Map} used for storing
     * child nodes. 
     * Overridable by sub-classes, used when caller does not know what
     * optimal size would, used for example when constructing a Map when adding
     * the first one.
     * 
     * @since 2.1
     */
    protected List<JsonNode> _createList() {
        return new ArrayList<JsonNode>();
    }
    
    /**
     * Internal factory method for creating {@link Map} used for storing
     * child nodes. 
     * Overridable by sub-classes, used when caller has an idea of what
     * optimal size should be: used when copying contents of an existing node.
     * 
     * @since 2.1
     */
    protected List<JsonNode> _createList(int defaultSize) {
        return new ArrayList<JsonNode>(defaultSize);
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
        if (o.getClass() != getClass()) { // final class, can do this
            return false;
        }
        ArrayNode other = (ArrayNode) o;
        if (_children == null || _children.size() == 0) {
            return other.size() == 0;
        }
        return other._sameChildren(_children);
    }

    @Override
    public int hashCode()
    {
        int hash;
        if (_children == null) {
            hash = 1;
        } else {
            hash = _children.size();
            for (JsonNode n : _children) {
                if (n != null) {
                    hash ^= n.hashCode();
                }
            }
        }
        return hash;
    }


    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(16 + (size() << 4));
        sb.append('[');
        if (_children != null) {
            for (int i = 0, len = _children.size(); i < len; ++i) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(_children.get(i).toString());
            }
        }
        sb.append(']');
        return sb.toString();
    }

    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */

    public JsonNode _set(int index, JsonNode value)
    {
        if (_children == null || index < 0 || index >= _children.size()) {
            throw new IndexOutOfBoundsException("Illegal index "+index+", array size "+size());
        }
        return _children.set(index, value);
    }

    private ArrayNode _add(JsonNode node)
    {
        if (_children == null) {
            _children = _createList();
        }
        _children.add(node);
        return this;
    }

    private ArrayNode _insert(int index, JsonNode node)
    {
        if (_children == null) {
            _children = _createList();
            _children.add(node);
            return this;
        }
        if (index < 0) {
            _children.add(0, node);
        } else if (index >= _children.size()) {
            _children.add(node);
        } else {
            _children.add(index, node);
        }
        return this;
    }

    /**
     * Note: this method gets called iff <code>otherChildren</code>
     * is non-empty
     */
    private boolean _sameChildren(List<JsonNode> otherChildren)
    {
        int len = otherChildren.size();
        if (this.size() != len) { // important: call size() to handle case of null list...
            return false;
        }
        for (int i = 0; i < len; ++i) {
            if (!_children.get(i).equals(otherChildren.get(i))) {
                return false;
            }
        }
        return true;
    }
}
