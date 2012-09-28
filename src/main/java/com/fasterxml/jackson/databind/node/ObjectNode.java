package com.fasterxml.jackson.databind.node;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

/**
 * Node that maps to JSON Object structures in JSON content.
 */
public class ObjectNode
    extends ContainerNode<ObjectNode>
{
    // note: until 2.1, was explicitly `LinkedHashMap`
    protected Map<String, JsonNode> _children = null;

    public ObjectNode(JsonNodeFactory nc) { super(nc); }

    protected ObjectNode(JsonNodeFactory nc, Map<String, JsonNode> children) {
        super(nc);
        _children = children;
    }
    
    /* Question: should this delegate to `JsonNodeFactory`? It does not absolutely
     * have to, as long as sub-types override the method but...
     */
    // note: co-variant for type safety
    @SuppressWarnings("unchecked")
    @Override
    public ObjectNode deepCopy()
    {
        /* 28-Sep-2012, tatu: Sub-classes really should override this method to
         *   produce compliant copies.
         */
        if (getClass() != ObjectNode.class) {
            throw new IllegalStateException("ObjectNode subtype ("+getClass().getName()+" does not override deepCopy(), needs to");
        }
        return _defaultDeepCopy();
    }
 
    /**
     * Default implementation for 'deepCopy()': can be delegated to by sub-classes
     * if necessary; but usually isn't.
     */
    protected ObjectNode _defaultDeepCopy()
    {
        if (_children == null) {
            return new ObjectNode(_nodeFactory);
        }
        final int len = _children.size();
        Map<String, JsonNode> newKids = _createMap(Math.max(4, len));
        for (Map.Entry<String, JsonNode> entry : _children.entrySet()) {
            newKids.put(entry.getKey(), entry.getValue().deepCopy());
        }
        return new ObjectNode(_nodeFactory, newKids);
    }
    
    /*
    /**********************************************************
    /* Implementation of core JsonNode API
    /**********************************************************
     */

    @Override public JsonToken asToken() { return JsonToken.START_OBJECT; }

    @Override
    public boolean isObject() { return true; }

    @Override
    public int size() {
        return (_children == null) ? 0 : _children.size();
    }

    @Override
    public Iterator<JsonNode> elements()
    {
        return (_children == null) ? NoNodesIterator.instance() : _children.values().iterator();
    }

    @Override
    public JsonNode get(int index) { return null; }

    @Override
    public JsonNode get(String fieldName)
    {
        if (_children != null) {
            return _children.get(fieldName);
        }
        return null;
    }

    @Override
    public Iterator<String> fieldNames() {
        return (_children == null) ? NoStringsIterator.instance() : _children.keySet().iterator();
    }

    @Override
    public JsonNode path(int index)
    {
        return MissingNode.getInstance();
    }

    @Override
    public JsonNode path(String fieldName)
    {
        if (_children != null) {
            JsonNode n = _children.get(fieldName);
            if (n != null) {
                return n;
            }
        }
        return MissingNode.getInstance();
    }

    /**
     * Method to use for accessing all fields (with both names
     * and values) of this JSON Object.
     */
    @Override
    public Iterator<Map.Entry<String, JsonNode>> fields()
    {
        if (_children == null) {
            return NoFieldsIterator.instance;
        }
        return _children.entrySet().iterator();
    }

    @Override
    public ObjectNode with(String propertyName)
    {
        if (_children == null) {
            _children = _createMap();
        } else {
            JsonNode n = _children.get(propertyName);
            if (n != null) {
                if (n instanceof ObjectNode) {
                    return (ObjectNode) n;
                }
                throw new UnsupportedOperationException("Property '"+propertyName
                        +"' has value that is not of type ObjectNode (but "
                        +n.getClass().getName()+")");
            }
        }
        ObjectNode result = objectNode();
        _children.put(propertyName, result);
        return result;
    }

    @Override
    public ArrayNode withArray(String propertyName)
    {
        if (_children == null) {
            _children = _createMap();
        } else {
            JsonNode n = _children.get(propertyName);
            if (n != null) {
                if (n instanceof ArrayNode) {
                    return (ArrayNode) n;
                }
                throw new UnsupportedOperationException("Property '"+propertyName
                        +"' has value that is not of type ArrayNode (but "
                        +n.getClass().getName()+")");
            }
        }
        ArrayNode result = arrayNode();
        _children.put(propertyName, result);
        return result;
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
            for (Map.Entry<String, JsonNode> entry : _children.entrySet()) {
                if (fieldName.equals(entry.getKey())) {
                    return entry.getValue();
                }
                JsonNode value = entry.getValue().findValue(fieldName);
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
            for (Map.Entry<String, JsonNode> entry : _children.entrySet()) {
                if (fieldName.equals(entry.getKey())) {
                    if (foundSoFar == null) {
                        foundSoFar = new ArrayList<JsonNode>();
                    }
                    foundSoFar.add(entry.getValue());
                } else { // only add children if parent not added
                    foundSoFar = entry.getValue().findValues(fieldName, foundSoFar);
                }
            }
        }
        return foundSoFar;
    }

    @Override
    public List<String> findValuesAsText(String fieldName, List<String> foundSoFar)
    {
        if (_children != null) {
            for (Map.Entry<String, JsonNode> entry : _children.entrySet()) {
                if (fieldName.equals(entry.getKey())) {
                    if (foundSoFar == null) {
                        foundSoFar = new ArrayList<String>();
                    }
                    foundSoFar.add(entry.getValue().asText());
                } else { // only add children if parent not added
                    foundSoFar = entry.getValue().findValuesAsText(fieldName, foundSoFar);
                }
            }
        }
        return foundSoFar;
    }
    
    @Override
    public ObjectNode findParent(String fieldName)
    {
        if (_children != null) {
            for (Map.Entry<String, JsonNode> entry : _children.entrySet()) {
                if (fieldName.equals(entry.getKey())) {
                    return this;
                }
                JsonNode value = entry.getValue().findParent(fieldName);
                if (value != null) {
                    return (ObjectNode) value;
                }
            }
        }
        return null;
    }

    @Override
    public List<JsonNode> findParents(String fieldName, List<JsonNode> foundSoFar)
    {
        if (_children != null) {
            for (Map.Entry<String, JsonNode> entry : _children.entrySet()) {
                if (fieldName.equals(entry.getKey())) {
                    if (foundSoFar == null) {
                        foundSoFar = new ArrayList<JsonNode>();
                    }
                    foundSoFar.add(this);
                } else { // only add children if parent not added
                    foundSoFar = entry.getValue().findParents(fieldName, foundSoFar);
                }
            }
        }
        return foundSoFar;
    }
    
    /*
    /**********************************************************
    /* Public API, serialization
    /**********************************************************
     */

    /**
     * Method that can be called to serialize this node and
     * all of its descendants using specified JSON generator.
     */
    @Override
    public final void serialize(JsonGenerator jg, SerializerProvider provider)
        throws IOException, JsonProcessingException
    {
        jg.writeStartObject();
        if (_children != null) {
            for (Map.Entry<String, JsonNode> en : _children.entrySet()) {
                jg.writeFieldName(en.getKey());
                /* 17-Feb-2009, tatu: Can we trust that all nodes will always
                 *   extend BaseJsonNode? Or if not, at least implement
                 *   JsonSerializable? Let's start with former, change if
                 *   we must.
                 */
                ((BaseJsonNode) en.getValue()).serialize(jg, provider);
            }
        }
        jg.writeEndObject();
    }

    @Override
    public void serializeWithType(JsonGenerator jg, SerializerProvider provider,
            TypeSerializer typeSer)
        throws IOException, JsonProcessingException
    {
        typeSer.writeTypePrefixForObject(this, jg);
        if (_children != null) {
            for (Map.Entry<String, JsonNode> en : _children.entrySet()) {
                jg.writeFieldName(en.getKey());
                ((BaseJsonNode) en.getValue()).serialize(jg, provider);
            }
        }
        typeSer.writeTypeSuffixForObject(this, jg);
    }

    /*
    /**********************************************************
    /* Extended ObjectNode API, mutators, since 2.1
    /**********************************************************
     */

    /**
     * Method that will set specified field, replacing old value, if any.
     * Note that this is identical to {@link #replace(String, JsonNode)},
     * except for return value.
     *<p>
     * NOTE: added to replace those uses of {@link #put(String, JsonNode)}
     * where chaining with 'this' is desired.
     *
     * @param value to set field to; if null, will be converted
     *   to a {@link NullNode} first  (to remove field entry, call
     *   {@link #remove} instead)
     *
     * @return This node after adding/replacing property value (to allow chaining)
     *
     * @since 2.1
     */
    public JsonNode set(String fieldName, JsonNode value)
    {
        if (value == null) {
            value = nullNode();
        }
        _put(fieldName, value);
        return this;
    }

    /**
     * Method for adding given properties to this object node, overriding
     * any existing values for those properties.
     * 
     * @param properties Properties to add
     * 
     * @return This node after adding/replacing property values (to allow chaining)
     *
     * @since 2.1
     */
    public JsonNode setAll(Map<String,JsonNode> properties)
    {
        if (_children == null) {
            _children = _createMap();
        } else {
            for (Map.Entry<String, JsonNode> en : properties.entrySet()) {
                JsonNode n = en.getValue();
                if (n == null) {
                    n = nullNode();
                }
                _children.put(en.getKey(), n);
            }
        }
        return this;
    }

    /**
     * Method for adding all properties of the given Object, overriding
     * any existing values for those properties.
     * 
     * @param other Object of which properties to add to this object
     *
     * @return This node after addition (to allow chaining)
     *
     * @since 2.1
     */
    public JsonNode setAll(ObjectNode other)
    {
        int len = other.size();
        if (len > 0) {
            if (_children == null) {
                _children = _createMap(len);
            }
            other.putContentsTo(_children);
        }
        return this;
    }
    
    /**
     * Method for replacing value of specific property with passed
     * value, and returning value (or null if none).
     *
     * @param fieldName Property of which value to replace
     * @param value Value to set property to, replacing old value if any
     * 
     * @return Old value of the property; null if there was no such property
     *   with value
     * 
     * @since 2.1
     */
    public JsonNode replace(String fieldName, JsonNode value)
    {
        if (value == null) { // let's not store 'raw' nulls but nodes
            value = nullNode();
        }
        return _put(fieldName, value);
    }

    /**
     * Method for removing field entry from this ObjectNode, and
     * returning instance after removal.
     * 
     * @return This node after removing entry (if any)
     * 
     * @since 2.1
     */
    public JsonNode without(String fieldName)
    {
        if (_children != null) {
            _children.remove(fieldName);
        }
        return this;
    }

    /**
     * Method for removing specified field properties out of
     * this ObjectNode.
     * 
     * @param fieldNames Names of fields to remove
     * 
     * @return This node after removing entries
     * 
     * @since 2.1
     */
    public ObjectNode without(Collection<String> fieldNames)
    {
        if (_children != null) {
            for (String fieldName : fieldNames) {
                _children.remove(fieldName);
            }
        }
        return this;
    }
    
    /*
    /**********************************************************
    /* Extended ObjectNode API, mutators, generic
    /**********************************************************
     */
    
    /**
     * Method that will set specified field, replacing old value, if any.
     *
     * @param value to set field to; if null, will be converted
     *   to a {@link NullNode} first  (to remove field entry, call
     *   {@link #remove} instead)
     *<p>
     * NOTE: this method will be <b>deprecated</b> in 2.2; and should
     * be replace with either
     * {@link #set(String,JsonNode)} or {@link #replace(String,JsonNode)},
     * depending on which return value is desired for possible chaining.
     *   
     * @return Old value of the field, if any; null if there was no
     *   old value.
     */
    public JsonNode put(String fieldName, JsonNode value)
    {
        if (value == null) { // let's not store 'raw' nulls but nodes
            value = nullNode();
        }
        return _put(fieldName, value);
    }
    
    /**
     * Method for removing field entry from this ObjectNode.
     * Will return value of the field, if such field existed;
     * null if not.
     * 
     * @return Value of specified field, if it existed; null if not
     */
    public JsonNode remove(String fieldName)
    {
        if (_children != null) {
            return _children.remove(fieldName);
        }
        return null;
    }

    /**
     * Method for removing specified field properties out of
     * this ObjectNode.
     * 
     * @param fieldNames Names of fields to remove
     * 
     * @return This node after removing entries
     */
    public ObjectNode remove(Collection<String> fieldNames)
    {
        if (_children != null) {
            for (String fieldName : fieldNames) {
                _children.remove(fieldName);
            }
        }
        return this;
    }
    
    /**
     * Method for removing all field properties, such that this
     * ObjectNode will contain no properties after call.
     * 
     * @return This node after removing all entries
     */
    @Override
    public ObjectNode removeAll()
    {
        _children = null;
        return this;
    }

    /**
     * Method for adding given properties to this object node, overriding
     * any existing values for those properties.
     *<p>
     * NOTE: this method will be <b>deprecated</b> in 2.2; and should
     * be replace with {@link #setAll(Map)}.
     * 
     * @param properties Properties to add
     * 
     * @return This node after adding/replacing property values (to allow chaining)
     */
    public JsonNode putAll(Map<String,JsonNode> properties) {
        return setAll(properties);
    }

    /**
     * Method for adding all properties of the given Object, overriding
     * any existing values for those properties.
     *<p>
     * NOTE: this method will be <b>deprecated</b> in 2.2; and should
     * be replace with {@link #setAll(ObjectNode)}.
     * 
     * @param other Object of which properties to add to this object
     * 
     * @return This node (to allow chaining)
     */
    public JsonNode putAll(ObjectNode other) {
        return setAll(other);
    }

    /**
     * Method for removing all field properties out of this ObjectNode
     * <b>except</b> for ones specified in argument.
     * 
     * @param fieldNames Fields to <b>retain</b> in this ObjectNode
     * 
     * @return This node (to allow call chaining)
     */
    public ObjectNode retain(Collection<String> fieldNames)
    {
        if (_children != null) {
            Iterator<Map.Entry<String,JsonNode>> entries = _children.entrySet().iterator();
            while (entries.hasNext()) {
                Map.Entry<String, JsonNode> entry = entries.next();
                if (!fieldNames.contains(entry.getKey())) {
                    entries.remove();
                }
            }
        }
        return this;
    }

    /**
     * Method for removing all field properties out of this ObjectNode
     * <b>except</b> for ones specified in argument.
     * 
     * @param fieldNames Fields to <b>retain</b> in this ObjectNode
     * 
     * @return This node (to allow call chaining)
     */
    public ObjectNode retain(String... fieldNames) {
        return retain(Arrays.asList(fieldNames));
    }
    
    /*
    /**********************************************************
    /* Extended ObjectNode API, mutators, typed
    /**********************************************************
     */

    /**
     * Method that will construct an ArrayNode and add it as a
     * field of this ObjectNode, replacing old value, if any.
     *<p>
     * <b>NOTE</b>: Unlike all <b>put(...)</b> methods, return value
     * is <b>NOT</b> this <code>ObjectNode</code>, but the
     * <b>newly created</b> <code>ArrayNode</code> instance.
     *
     * @return Newly constructed ArrayNode (NOT the old value,
     *   which could be of any type)
     */
    public ArrayNode putArray(String fieldName)
    {
        ArrayNode n  = arrayNode();
        _put(fieldName, n);
        return n;
    }

    /**
     * Method that will construct an ObjectNode and add it as a
     * field of this ObjectNode, replacing old value, if any.
     *<p>
     * <b>NOTE</b>: Unlike all <b>put(...)</b> methods, return value
     * is <b>NOT</b> this <code>ObjectNode</code>, but the
     * <b>newly created</b> <code>ObjectNode</code> instance.
     *
     * @return Newly constructed ObjectNode (NOT the old value,
     *   which could be of any type)
     */
    public ObjectNode putObject(String fieldName)
    {
        ObjectNode n  = objectNode();
        _put(fieldName, n);
        return n;
    }

    /**
     * @return This node (to allow chaining)
     */
    public ObjectNode putPOJO(String fieldName, Object pojo) {
        _put(fieldName, POJONode(pojo));
        return this;
    }

    /**
     * @return This node (to allow chaining)
     */
    public ObjectNode putNull(String fieldName)
    {
        _put(fieldName, nullNode());
        return this;
    }

    /**
     * Method for setting value of a field to specified numeric value.
     * 
     * @return This node (to allow chaining)
     */
    public ObjectNode put(String fieldName, int v) {
        _put(fieldName, numberNode(v));
        return this;
    }

    /**
     * Alternative method that we need to avoid bumping into NPE issues
     * with auto-unboxing.
     * 
     * @return This node (to allow chaining)
     */
    public ObjectNode put(String fieldName, Integer value) {
        if (value == null) {
            _put(fieldName, nullNode());
        } else {
            _put(fieldName, numberNode(value.intValue()));
        }
        return this;
    }
    
    /**
     * Method for setting value of a field to specified numeric value.
     * 
     * @return This node (to allow chaining)
     */
    public ObjectNode put(String fieldName, long v) {
        _put(fieldName, numberNode(v));
        return this;
    }

    /**
     * Alternative method that we need to avoid bumping into NPE issues
     * with auto-unboxing.
     * 
     * @return This node (to allow chaining)
     */
    public ObjectNode put(String fieldName, Long value) {
        if (value == null) {
            _put(fieldName, nullNode());
        } else {
            _put(fieldName, numberNode(value.longValue()));
        }
        return this;
    }
    
    /**
     * Method for setting value of a field to specified numeric value.
     * 
     * @return This node (to allow chaining)
     */
    public ObjectNode put(String fieldName, float v) {
        _put(fieldName, numberNode(v));
        return this;
    }

    /**
     * Alternative method that we need to avoid bumping into NPE issues
     * with auto-unboxing.
     * 
     * @return This node (to allow chaining)
     */
    public ObjectNode put(String fieldName, Float value) {
        if (value == null) {
            _put(fieldName, nullNode());
        } else {
            _put(fieldName, numberNode(value.floatValue()));
        }
        return this;
    }
    
    /**
     * Method for setting value of a field to specified numeric value.
     * 
     * @return This node (to allow chaining)
     */
    public ObjectNode put(String fieldName, double v) {
        _put(fieldName, numberNode(v));
        return this;
    }

    /**
     * Alternative method that we need to avoid bumping into NPE issues
     * with auto-unboxing.
     * 
     * @return This node (to allow chaining)
     */
    public ObjectNode put(String fieldName, Double value) {
        if (value == null) {
            _put(fieldName, nullNode());
        } else {
            _put(fieldName, numberNode(value.doubleValue()));
        }
        return this;
    }
    
    /**
     * Method for setting value of a field to specified numeric value.
     * 
     * @return This node (to allow chaining)
     */
    public ObjectNode put(String fieldName, BigDecimal v) {
        if (v == null) {
            putNull(fieldName);
        } else {
            _put(fieldName, numberNode(v));
        }
        return this;
    }

    /**
     * Method for setting value of a field to specified String value.
     * 
     * @return This node (to allow chaining)
     */
    public ObjectNode put(String fieldName, String v) {
        if (v == null) {
            putNull(fieldName);
        } else {
            _put(fieldName, textNode(v));
        }
        return this;
    }

    /**
     * Method for setting value of a field to specified String value.
     * 
     * @return This node (to allow chaining)
     */
    public ObjectNode put(String fieldName, boolean v) {
        _put(fieldName, booleanNode(v));
        return this;
    }

    /**
     * Alternative method that we need to avoid bumping into NPE issues
     * with auto-unboxing.
     * 
     * @return This node (to allow chaining)
     */
    public ObjectNode put(String fieldName, Boolean value) {
        if (value == null) {
            _put(fieldName, nullNode());
        } else {
            _put(fieldName, booleanNode(value.booleanValue()));
        }
        return this;
    }
    
    /**
     * Method for setting value of a field to specified binary value
     * 
     * @return This node (to allow chaining)
     */
    public ObjectNode put(String fieldName, byte[] v) {
        if (v == null) {
            _put(fieldName, nullNode());
        } else {
            _put(fieldName, binaryNode(v));
        }
        return this;
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
    protected Map<String, JsonNode> _createMap() {
        return new LinkedHashMap<String, JsonNode>();
    }
    
    /**
     * Internal factory method for creating {@link Map} used for storing
     * child nodes. 
     * Overridable by sub-classes, used when caller has an idea of what
     * optimal size should be: used when copying contents of an existing node.
     * 
     * @since 2.1
     */
    protected Map<String, JsonNode> _createMap(int defaultSize) {
        return new LinkedHashMap<String, JsonNode>(defaultSize);
    }
    
    /*
    /**********************************************************
    /* Package methods (for other node classes to use)
    /**********************************************************
     */

    protected void putContentsTo(Map<String,JsonNode> dst)
    {
        if (_children != null) {
            for (Map.Entry<String,JsonNode> en : _children.entrySet()) {
                dst.put(en.getKey(), en.getValue());
            }
        }
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
        if (o.getClass() != getClass()) {
            return false;
        }
        ObjectNode other = (ObjectNode) o;
        if (other.size() != size()) {
            return false;
        }
        if (_children != null) {
            for (Map.Entry<String, JsonNode> en : _children.entrySet()) {
                String key = en.getKey();
                JsonNode value = en.getValue();

                JsonNode otherValue = other.get(key);

                if (otherValue == null || !otherValue.equals(value)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        return (_children == null) ? -1 : _children.hashCode();
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(32 + (size() << 4));
        sb.append("{");
        if (_children != null) {
            int count = 0;
            for (Map.Entry<String, JsonNode> en : _children.entrySet()) {
                if (count > 0) {
                    sb.append(",");
                }
                ++count;
                TextNode.appendQuoted(sb, en.getKey());
                sb.append(':');
                sb.append(en.getValue().toString());
            }
        }
        sb.append("}");
        return sb.toString();
    }

    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */

    private final JsonNode _put(String fieldName, JsonNode value)
    {
        if (_children == null) {
            _children = _createMap();
        }
        return _children.put(fieldName, value);
    }

    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    /**
     * For efficiency, let's share the "no fields" iterator...
     */
    protected static class NoFieldsIterator
        implements Iterator<Map.Entry<String, JsonNode>>
    {
        final static NoFieldsIterator instance = new NoFieldsIterator();

        private NoFieldsIterator() { }

//      @Override
        public boolean hasNext() { return false; }
//      @Override
        public Map.Entry<String,JsonNode> next() { throw new NoSuchElementException(); }

//      @Override
        public void remove() { // or IllegalOperationException?
            throw new IllegalStateException();
        }
    }
}
