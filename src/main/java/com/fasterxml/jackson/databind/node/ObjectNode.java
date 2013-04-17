package com.fasterxml.jackson.databind.node;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Node that maps to JSON Object structures in JSON content.
 */
public final class ObjectNode
    extends ContainerNode<ObjectNode>
{
    // Note: LinkedHashMap for backwards compatibility
    private final Map<String, JsonNode> _children
        = new LinkedHashMap<String, JsonNode>();

    public ObjectNode(JsonNodeFactory nc) { super(nc); }

    /* Question: should this delegate to `JsonNodeFactory`? It does not absolutely
     * have to, as long as sub-types override the method but...
     */
    // note: co-variant for type safety
    @SuppressWarnings("unchecked")
    @Override
    public ObjectNode deepCopy()
    {
        ObjectNode ret = new ObjectNode(_nodeFactory);

        for (Map.Entry<String, JsonNode> entry: _children.entrySet())
            ret._children.put(entry.getKey(), entry.getValue().deepCopy());

        return ret;
    }

    /*
    /**********************************************************
    /* Implementation of core JsonNode API
    /**********************************************************
     */

    @Override
    public JsonNodeType getNodeType()
    {
        return JsonNodeType.OBJECT;
    }

    @Override public JsonToken asToken() { return JsonToken.START_OBJECT; }

    @Override
    public int size() {
        return _children.size();
    }

    @Override
    public Iterator<JsonNode> elements()
    {
        return _children.values().iterator();
    }

    @Override
    public JsonNode get(int index) { return null; }

    @Override
    public JsonNode get(String fieldName)
    {
        return _children.get(fieldName);
    }

    @Override
    public Iterator<String> fieldNames()
    {
        return _children.keySet().iterator();
    }

    @Override
    public JsonNode path(int index)
    {
        return MissingNode.getInstance();
    }

    @Override
    public JsonNode path(String fieldName)
    {
        JsonNode n = _children.get(fieldName);
        if (n != null) {
            return n;
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
        return _children.entrySet().iterator();
    }

    @Override
    public ObjectNode with(String propertyName)
    {
        JsonNode n = _children.get(propertyName);
        if (n != null) {
            if (n instanceof ObjectNode) {
                return (ObjectNode) n;
            }
            throw new UnsupportedOperationException("Property '" + propertyName
                + "' has value that is not of type ObjectNode (but " + n
                .getClass().getName() + ")");
        }
        ObjectNode result = objectNode();
        _children.put(propertyName, result);
        return result;
    }

    @Override
    public ArrayNode withArray(String propertyName)
    {
        JsonNode n = _children.get(propertyName);
        if (n != null) {
            if (n instanceof ArrayNode) {
                return (ArrayNode) n;
            }
            throw new UnsupportedOperationException("Property '" + propertyName
                + "' has value that is not of type ArrayNode (but " + n
                .getClass().getName() + ")");
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
        for (Map.Entry<String, JsonNode> entry : _children.entrySet()) {
            if (fieldName.equals(entry.getKey())) {
                return entry.getValue();
            }
            JsonNode value = entry.getValue().findValue(fieldName);
            if (value != null) {
                return value;
            }
        }
        return null;
    }
    
    @Override
    public List<JsonNode> findValues(String fieldName, List<JsonNode> foundSoFar)
    {
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
        return foundSoFar;
    }

    @Override
    public List<String> findValuesAsText(String fieldName, List<String> foundSoFar)
    {
        for (Map.Entry<String, JsonNode> entry : _children.entrySet()) {
            if (fieldName.equals(entry.getKey())) {
                if (foundSoFar == null) {
                    foundSoFar = new ArrayList<String>();
                }
                foundSoFar.add(entry.getValue().asText());
            } else { // only add children if parent not added
                foundSoFar = entry.getValue().findValuesAsText(fieldName,
                    foundSoFar);
            }
        }
        return foundSoFar;
    }
    
    @Override
    public ObjectNode findParent(String fieldName)
    {
        for (Map.Entry<String, JsonNode> entry : _children.entrySet()) {
            if (fieldName.equals(entry.getKey())) {
                return this;
            }
            JsonNode value = entry.getValue().findParent(fieldName);
            if (value != null) {
                return (ObjectNode) value;
            }
        }
        return null;
    }

    @Override
    public List<JsonNode> findParents(String fieldName, List<JsonNode> foundSoFar)
    {
        for (Map.Entry<String, JsonNode> entry : _children.entrySet()) {
            if (fieldName.equals(entry.getKey())) {
                if (foundSoFar == null) {
                    foundSoFar = new ArrayList<JsonNode>();
                }
                foundSoFar.add(this);
            } else { // only add children if parent not added
                foundSoFar = entry.getValue()
                    .findParents(fieldName, foundSoFar);
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
    public void serialize(JsonGenerator jg, SerializerProvider provider)
        throws IOException, JsonProcessingException
    {
        jg.writeStartObject();
        for (Map.Entry<String, JsonNode> en : _children.entrySet()) {
            jg.writeFieldName(en.getKey());
                /* 17-Feb-2009, tatu: Can we trust that all nodes will always
                 *   extend BaseJsonNode? Or if not, at least implement
                 *   JsonSerializable? Let's start with former, change if
                 *   we must.
                 */
            ((BaseJsonNode) en.getValue()).serialize(jg, provider);
        }
        jg.writeEndObject();
    }

    @Override
    public void serializeWithType(JsonGenerator jg, SerializerProvider provider,
            TypeSerializer typeSer)
        throws IOException, JsonProcessingException
    {
        typeSer.writeTypePrefixForObject(this, jg);
        for (Map.Entry<String, JsonNode> en : _children.entrySet()) {
            jg.writeFieldName(en.getKey());
            ((BaseJsonNode) en.getValue()).serialize(jg, provider);
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
        _children.put(fieldName, value);
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
        for (Map.Entry<String, JsonNode> en : properties.entrySet()) {
            JsonNode n = en.getValue();
            if (n == null) {
                n = nullNode();
            }
            _children.put(en.getKey(), n);
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
        _children.putAll(other._children);
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
        return _children.put(fieldName, value);
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
        _children.remove(fieldName);
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
        _children.keySet().removeAll(fieldNames);
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
        return _children.put(fieldName, value);
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
        return _children.remove(fieldName);
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
        _children.keySet().removeAll(fieldNames);
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
        _children.clear();
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
        _children.keySet().retainAll(fieldNames);
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
        _children.put(fieldName, n);
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
        _children.put(fieldName, n);
        return n;
    }

    /**
     * @return This node (to allow chaining)
     */
    public ObjectNode putPOJO(String fieldName, Object pojo) {
        _children.put(fieldName, POJONode(pojo));
        return this;
    }

    /**
     * @return This node (to allow chaining)
     */
    public ObjectNode putNull(String fieldName)
    {
        _children.put(fieldName, nullNode());
        return this;
    }

    /**
     * Method for setting value of a field to specified numeric value.
     * 
     * @return This node (to allow chaining)
     */
    public ObjectNode put(String fieldName, short v) {
        _children.put(fieldName, numberNode(v));
        return this;
    }

    /**
     * Alternative method that we need to avoid bumping into NPE issues
     * with auto-unboxing.
     * 
     * @return This node (to allow chaining)
     */
    public ObjectNode put(String fieldName, Short value) {
        if (value == null) {
            _children.put(fieldName, nullNode());
        } else {
            _children.put(fieldName, numberNode(value.shortValue()));
        }
        return this;
    }

    /**
     * Method for setting value of a field to specified numeric value.
     * 
     * @return This node (to allow chaining)
     */
    public ObjectNode put(String fieldName, int v) {
        _children.put(fieldName, numberNode(v));
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
            _children.put(fieldName, nullNode());
        } else {
            _children.put(fieldName, numberNode(value.intValue()));
        }
        return this;
    }
    
    /**
     * Method for setting value of a field to specified numeric value.
     * 
     * @return This node (to allow chaining)
     */
    public ObjectNode put(String fieldName, long v) {
        _children.put(fieldName, numberNode(v));
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
            _children.put(fieldName, nullNode());
        } else {
            _children.put(fieldName, numberNode(value.longValue()));
        }
        return this;
    }
    
    /**
     * Method for setting value of a field to specified numeric value.
     * 
     * @return This node (to allow chaining)
     */
    public ObjectNode put(String fieldName, float v) {
        _children.put(fieldName, numberNode(v));
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
            _children.put(fieldName, nullNode());
        } else {
            _children.put(fieldName, numberNode(value.floatValue()));
        }
        return this;
    }
    
    /**
     * Method for setting value of a field to specified numeric value.
     * 
     * @return This node (to allow chaining)
     */
    public ObjectNode put(String fieldName, double v) {
        _children.put(fieldName, numberNode(v));
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
            _children.put(fieldName, nullNode());
        } else {
            _children.put(fieldName, numberNode(value.doubleValue()));
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
            _children.put(fieldName, numberNode(v));
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
            _children.put(fieldName, textNode(v));
        }
        return this;
    }

    /**
     * Method for setting value of a field to specified String value.
     * 
     * @return This node (to allow chaining)
     */
    public ObjectNode put(String fieldName, boolean v) {
        _children.put(fieldName, booleanNode(v));
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
            _children.put(fieldName, nullNode());
        } else {
            _children.put(fieldName, booleanNode(value.booleanValue()));
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
            _children.put(fieldName, nullNode());
        } else {
            _children.put(fieldName, binaryNode(v));
        }
        return this;
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
        
        // minor improvement, wrt [Issue#70]
        if (getClass() != o.getClass()) {
            return false;
        }
        return _children.equals(((ObjectNode) o)._children);
    }
    
    @Override
    public int hashCode()
    {
        return _children.hashCode();
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(32 + (size() << 4));
        sb.append("{");
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
        sb.append("}");
        return sb.toString();
    }
}
