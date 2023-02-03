package com.fasterxml.jackson.databind.node;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.WritableTypeId;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.JsonNodeFeature;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.util.RawValue;

/**
 * Node that maps to JSON Object structures in JSON content.
 *<p>
 * Note: class was <code>final</code> temporarily for Jackson 2.2.
 */
public class ObjectNode
    extends ContainerNode<ObjectNode>
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L; // since 2.10

    // Note: LinkedHashMap for backwards compatibility
    protected final Map<String, JsonNode> _children;

    public ObjectNode(JsonNodeFactory nc) {
        super(nc);
        _children = new LinkedHashMap<String, JsonNode>();
    }

    /**
     * @since 2.4
     */
    public ObjectNode(JsonNodeFactory nc, Map<String, JsonNode> kids) {
        super(nc);
        _children = kids;
    }

    @Override
    protected JsonNode _at(JsonPointer ptr) {
        return get(ptr.getMatchingProperty());
    }

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
        JsonNode n = _children.get(exprOrProperty);
        if (n != null) {
            if (n instanceof ObjectNode) {
                return (ObjectNode) n;
            }
            throw new UnsupportedOperationException("Property '" + exprOrProperty
                + "' has value that is not of type `ObjectNode` (but `" + n
                .getClass().getName() + "`)");
        }
        ObjectNode result = objectNode();
        _children.put(exprOrProperty, result);
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ArrayNode withArray(String exprOrProperty)
    {
        JsonPointer ptr = _jsonPointerIfValid(exprOrProperty);
        if (ptr != null) {
            return withArray(ptr);
        }
        JsonNode n = _children.get(exprOrProperty);
        if (n != null) {
            if (n instanceof ArrayNode) {
                return (ArrayNode) n;
            }
            throw new UnsupportedOperationException("Property '" + exprOrProperty
                + "' has value that is not of type `ArrayNode` (but `" + n
                .getClass().getName() + "`)");
        }
        ArrayNode result = arrayNode();
        _children.put(exprOrProperty, result);
        return result;
    }

    @Override
    protected ObjectNode _withObject(JsonPointer origPtr,
            JsonPointer currentPtr,
            OverwriteMode overwriteMode, boolean preferIndex)
    {
        if (currentPtr.matches()) {
            return this;
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
        return _withObjectAddTailProperty(currentPtr, preferIndex);
    }

    @Override
    protected ArrayNode _withArray(JsonPointer origPtr,
            JsonPointer currentPtr,
            OverwriteMode overwriteMode, boolean preferIndex)
    {
        if (currentPtr.matches()) {
            // Cannot return, not an ArrayNode so:
            return null;
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
        return _withArrayAddTailProperty(currentPtr, preferIndex);
    }

    protected ObjectNode _withObjectAddTailProperty(JsonPointer tail, boolean preferIndex)
    {
        final String propName = tail.getMatchingProperty();
        tail = tail.tail();

        // First: did we complete traversal? If so, easy, we got our result
        if (tail.matches()) {
            return putObject(propName);
        }

        // Otherwise, do we want Array or Object
        if (preferIndex && tail.mayMatchElement()) { // array!
            return putArray(propName)._withObjectAddTailElement(tail, preferIndex);
        }
        return putObject(propName)._withObjectAddTailProperty(tail, preferIndex);
    }

    protected ArrayNode _withArrayAddTailProperty(JsonPointer tail, boolean preferIndex)
    {
        final String propName = tail.getMatchingProperty();
        tail = tail.tail();

        // First: did we complete traversal? If so, easy, we got our result
        if (tail.matches()) {
            return putArray(propName);
        }

        // Otherwise, do we want Array or Object
        if (preferIndex && tail.mayMatchElement()) { // array!
            return putArray(propName)._withArrayAddTailElement(tail, preferIndex);
        }
        return putObject(propName)._withArrayAddTailProperty(tail, preferIndex);
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
        return JsonNodeType.OBJECT;
    }

    @Override
    public final boolean isObject() {
        return true;
    }

    @Override public JsonToken asToken() { return JsonToken.START_OBJECT; }

    @Override
    public int size() {
        return _children.size();
    }

    @Override // since 2.10
    public boolean isEmpty() { return _children.isEmpty(); }

    @Override
    public Iterator<JsonNode> elements() {
        return _children.values().iterator();
    }

    @Override
    public JsonNode get(int index) { return null; }

    @Override
    public JsonNode get(String propertyName) {
        return _children.get(propertyName);
    }

    @Override
    public Iterator<String> fieldNames() {
        return _children.keySet().iterator();
    }

    @Override
    public JsonNode path(int index) {
        return MissingNode.getInstance();
    }

    @Override
    public JsonNode path(String propertyName)
    {
        JsonNode n = _children.get(propertyName);
        if (n != null) {
            return n;
        }
        return MissingNode.getInstance();
    }

    @Override
    public JsonNode required(String propertyName) {
        JsonNode n = _children.get(propertyName);
        if (n != null) {
            return n;
        }
        return _reportRequiredViolation("No value for property '%s' of `ObjectNode`", propertyName);
    }

    /**
     * Method to use for accessing all properties (with both names
     * and values) of this JSON Object.
     */
    @Override
    public Iterator<Map.Entry<String, JsonNode>> fields() {
        return _children.entrySet().iterator();
    }

    @Override
    public boolean equals(Comparator<JsonNode> comparator, JsonNode o)
    {
        if (!(o instanceof ObjectNode)) {
            return false;
        }
        ObjectNode other = (ObjectNode) o;
        Map<String, JsonNode> m1 = _children;
        Map<String, JsonNode> m2 = other._children;

        final int len = m1.size();
        if (m2.size() != len) {
            return false;
        }

        for (Map.Entry<String, JsonNode> entry : m1.entrySet()) {
            JsonNode v2 = m2.get(entry.getKey());
            if ((v2 == null) || !entry.getValue().equals(comparator, v2)) {
                return false;
            }
        }
        return true;
    }

    /*
    /**********************************************************
    /* Public API, finding value nodes
    /**********************************************************
     */

    @Override
    public JsonNode findValue(String propertyName)
    {
        for (Map.Entry<String, JsonNode> entry : _children.entrySet()) {
            if (propertyName.equals(entry.getKey())) {
                return entry.getValue();
            }
            JsonNode value = entry.getValue().findValue(propertyName);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    @Override
    public List<JsonNode> findValues(String propertyName, List<JsonNode> foundSoFar)
    {
        for (Map.Entry<String, JsonNode> entry : _children.entrySet()) {
            if (propertyName.equals(entry.getKey())) {
                if (foundSoFar == null) {
                    foundSoFar = new ArrayList<JsonNode>();
                }
                foundSoFar.add(entry.getValue());
            } else { // only add children if parent not added
                foundSoFar = entry.getValue().findValues(propertyName, foundSoFar);
            }
        }
        return foundSoFar;
    }

    @Override
    public List<String> findValuesAsText(String propertyName, List<String> foundSoFar)
    {
        for (Map.Entry<String, JsonNode> entry : _children.entrySet()) {
            if (propertyName.equals(entry.getKey())) {
                if (foundSoFar == null) {
                    foundSoFar = new ArrayList<String>();
                }
                foundSoFar.add(entry.getValue().asText());
            } else { // only add children if parent not added
                foundSoFar = entry.getValue().findValuesAsText(propertyName,
                    foundSoFar);
            }
        }
        return foundSoFar;
    }

    @Override
    public ObjectNode findParent(String propertyName)
    {
        for (Map.Entry<String, JsonNode> entry : _children.entrySet()) {
            if (propertyName.equals(entry.getKey())) {
                return this;
            }
            JsonNode value = entry.getValue().findParent(propertyName);
            if (value != null) {
                return (ObjectNode) value;
            }
        }
        return null;
    }

    @Override
    public List<JsonNode> findParents(String propertyName, List<JsonNode> foundSoFar)
    {
        for (Map.Entry<String, JsonNode> entry : _children.entrySet()) {
            if (propertyName.equals(entry.getKey())) {
                if (foundSoFar == null) {
                    foundSoFar = new ArrayList<JsonNode>();
                }
                foundSoFar.add(this);
            } else { // only add children if parent not added
                foundSoFar = entry.getValue()
                    .findParents(propertyName, foundSoFar);
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
    @SuppressWarnings("deprecation")
    @Override
    public void serialize(JsonGenerator g, SerializerProvider provider)
        throws IOException
    {
        if (provider != null) {
            boolean trimEmptyArray = !provider.isEnabled(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);
            boolean skipNulls = !provider.isEnabled(JsonNodeFeature.WRITE_NULL_PROPERTIES);
            if (trimEmptyArray || skipNulls) {
                g.writeStartObject(this);
                serializeFilteredContents(g, provider, trimEmptyArray, skipNulls);
                g.writeEndObject();
                return;
            }
        }
        g.writeStartObject(this);
        for (Map.Entry<String, JsonNode> en : _children.entrySet()) {
            JsonNode value = en.getValue();
            g.writeFieldName(en.getKey());
            value.serialize(g, provider);
        }
        g.writeEndObject();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void serializeWithType(JsonGenerator g, SerializerProvider provider,
            TypeSerializer typeSer)
        throws IOException
    {
        boolean trimEmptyArray = false;
        boolean skipNulls = false;
        if (provider != null) {
            trimEmptyArray = !provider.isEnabled(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);
            skipNulls = !provider.isEnabled(JsonNodeFeature.WRITE_NULL_PROPERTIES);
        }

        WritableTypeId typeIdDef = typeSer.writeTypePrefix(g,
                typeSer.typeId(this, JsonToken.START_OBJECT));

        if (trimEmptyArray || skipNulls) {
            serializeFilteredContents(g, provider, trimEmptyArray, skipNulls);
        } else {
            for (Map.Entry<String, JsonNode> en : _children.entrySet()) {
                JsonNode value = en.getValue();
                g.writeFieldName(en.getKey());
                value.serialize(g, provider);
            }
        }
        typeSer.writeTypeSuffix(g, typeIdDef);
    }

    /**
     * Helper method shared and called by {@link #serialize} and {@link #serializeWithType}
     * in cases where actual filtering is needed based on configuration.
     *
     * @since 2.14
     */
    protected void serializeFilteredContents(final JsonGenerator g, final SerializerProvider provider,
            final boolean trimEmptyArray, final boolean skipNulls)
        throws IOException
    {
        for (Map.Entry<String, JsonNode> en : _children.entrySet()) {
            // 17-Feb-2009, tatu: Can we trust that all nodes will always
            //   extend BaseJsonNode? Or if not, at least implement
            //   JsonSerializable? Let's start with former, change if
            //   we must.
            BaseJsonNode value = (BaseJsonNode) en.getValue();

            // as per [databind#867], see if WRITE_EMPTY_JSON_ARRAYS feature is disabled,
            // if the feature is disabled, then should not write an empty array
            // to the output, so continue to the next element in the iteration
            if (trimEmptyArray && value.isArray() && value.isEmpty(provider)) {
               continue;
            }
            if (skipNulls && value.isNull()) {
                continue;
            }
            g.writeFieldName(en.getKey());
            value.serialize(g, provider);
        }
    }

    /*
    /**********************************************************
    /* Extended ObjectNode API, mutators, since 2.1
    /**********************************************************
     */

    /**
     * Method that will set specified property, replacing old value, if any.
     * Note that this is identical to {@link #replace(String, JsonNode)},
     * except for return value.
     *<p>
     * NOTE: added to replace those uses of {@link #put(String, JsonNode)}
     * where chaining with 'this' is desired.
     *<p>
     * NOTE: co-variant return type since 2.10
     *
     * @param propertyName Name of property to set
     * @param value Value to set property to; if null, will be converted
     *   to a {@link NullNode} first  (to remove a property, call
     *   {@link #remove} instead)
     *
     * @return This node after adding/replacing property value (to allow chaining)
     *
     * @since 2.1
     */
    @SuppressWarnings("unchecked")
    public <T extends JsonNode> T set(String propertyName, JsonNode value)
    {
        if (value == null) {
            value = nullNode();
        }
        _children.put(propertyName, value);
        return (T) this;
    }

    /**
     * Method for adding given properties to this object node, overriding
     * any existing values for those properties.
     *<p>
     * NOTE: co-variant return type since 2.10
     *
     * @param properties Properties to add
     *
     * @return This node after adding/replacing property values (to allow chaining)
     *
     * @since 2.1
     */
    @SuppressWarnings("unchecked")
    public <T extends JsonNode> T setAll(Map<String,? extends JsonNode> properties)
    {
        for (Map.Entry<String,? extends JsonNode> en : properties.entrySet()) {
            JsonNode n = en.getValue();
            if (n == null) {
                n = nullNode();
            }
            _children.put(en.getKey(), n);
        }
        return (T) this;
    }

    /**
     * Method for adding all properties of the given Object, overriding
     * any existing values for those properties.
     *<p>
     * NOTE: co-variant return type since 2.10
     *
     * @param other Object of which properties to add to this object
     *
     * @return This node after addition (to allow chaining)
     *
     * @since 2.1
     */
    @SuppressWarnings("unchecked")
    public <T extends JsonNode> T setAll(ObjectNode other)
    {
        _children.putAll(other._children);
        return (T) this;
    }

    /**
     * Method for replacing value of specific property with passed
     * value, and returning value (or null if none).
     *
     * @param propertyName Property of which value to replace
     * @param value Value to set property to, replacing old value if any
     *
     * @return Old value of the property; null if there was no such property
     *   with value
     *
     * @since 2.1
     */
    public JsonNode replace(String propertyName, JsonNode value)
    {
        if (value == null) { // let's not store 'raw' nulls but nodes
            value = nullNode();
        }
        return _children.put(propertyName, value);
    }

    /**
     * Method for removing property from this ObjectNode, and
     * returning instance after removal.
     *<p>
     * NOTE: co-variant return type since 2.10
     *
     * @return This node after removing property (if any)
     *
     * @since 2.1
     */
    @SuppressWarnings("unchecked")
    public <T extends JsonNode> T without(String propertyName)
    {
        _children.remove(propertyName);
        return (T) this;
    }

    /**
     * Method for removing specified field properties out of
     * this ObjectNode.
     *<p>
     * NOTE: co-variant return type since 2.10
     *
     * @param propertyNames Names of properties to remove
     *
     * @return This node after removing entries
     *
     * @since 2.1
     */
    @SuppressWarnings("unchecked")
    public <T extends JsonNode> T without(Collection<String> propertyNames)
    {
        _children.keySet().removeAll(propertyNames);
        return (T) this;
    }

    /*
    /**********************************************************
    /* Extended ObjectNode API, mutators, generic
    /**********************************************************
     */

    /**
     * Method that will set specified property, replacing old value, if any.
     *
     * @param propertyName Name of property to set
     * @param value Value to set to property; if null, will be converted
     *   to a {@link NullNode} first  (to remove a property, call
     *   {@link #remove} instead).
     *
     * @return Old value of the property, if any; {@code null} if there was no
     *   old value.
     *
     * @deprecated Since 2.4 use either {@link #set(String,JsonNode)} or {@link #replace(String,JsonNode)},
     */
    @Deprecated
    public JsonNode put(String propertyName, JsonNode value)
    {
        if (value == null) { // let's not store 'raw' nulls but nodes
            value = nullNode();
        }
        return _children.put(propertyName, value);
    }

    /**
     * Method that will set value of specified property if (and only if)
     * it had no set value previously.
     * Note that explicitly set {@code null} is a value.
     * Functionally equivalent to:
     *<code>
     *  if (get(propertyName) == null) {
     *      set(propertyName, value);
     *      return null;
     *  } else {
     *      return get(propertyName);
     *  }
     *</code>
     *
     * @param propertyName Name of property to set
     * @param value Value to set to property (if and only if it had no value previously);
     *  if null, will be converted to a {@link NullNode} first.
     *
     * @return Old value of the property, if any (in which case value was not changed);
     *     null if there was no old value (in which case value is now set)
     *
     * @since 2.13
     */
    public JsonNode putIfAbsent(String propertyName, JsonNode value)
    {
        if (value == null) { // let's not store 'raw' nulls but nodes
            value = nullNode();
        }
        return _children.putIfAbsent(propertyName, value);
    }

    /**
     * Method for removing a property from this {@code ObjectNode}.
     * Will return previous value of the property, if such property existed;
     * null if not.
     *
     * @return Value of specified property, if it existed; null if not
     */
    public JsonNode remove(String propertyName) {
        return _children.remove(propertyName);
    }

    /**
     * Method for removing specified field properties out of
     * this ObjectNode.
     *
     * @param propertyNames Names of fields to remove
     *
     * @return This node after removing entries
     */
    public ObjectNode remove(Collection<String> propertyNames)
    {
        _children.keySet().removeAll(propertyNames);
        return this;
    }

    /**
     * Method for removing all properties, such that this
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
     *
     * @param properties Properties to add
     *
     * @return This node after adding/replacing property values (to allow chaining)
     *
     * @deprecated Since 2.4 use {@link #setAll(Map)},
     */
    @Deprecated
    public JsonNode putAll(Map<String,? extends JsonNode> properties) {
        return setAll(properties);
    }

    /**
     * Method for adding all properties of the given Object, overriding
     * any existing values for those properties.
     *
     * @param other Object of which properties to add to this object
     *
     * @return This node (to allow chaining)
     *
     * @deprecated Since 2.4 use {@link #setAll(ObjectNode)},
     */
    @Deprecated
    public JsonNode putAll(ObjectNode other) {
        return setAll(other);
    }

    /**
     * Method for removing all properties out of this ObjectNode
     * <b>except</b> for ones specified in argument.
     *
     * @param propertyNames Fields to <b>retain</b> in this ObjectNode
     *
     * @return This node (to allow call chaining)
     */
    public ObjectNode retain(Collection<String> propertyNames)
    {
        _children.keySet().retainAll(propertyNames);
        return this;
    }

    /**
     * Method for removing all properties out of this ObjectNode
     * <b>except</b> for ones specified in argument.
     *
     * @param propertyNames Fields to <b>retain</b> in this ObjectNode
     *
     * @return This node (to allow call chaining)
     */
    public ObjectNode retain(String... propertyNames) {
        return retain(Arrays.asList(propertyNames));
    }

    /*
    /**********************************************************
    /* Extended ObjectNode API, mutators, typed
    /**********************************************************
     */

    /**
     * Method that will construct an ArrayNode and add it as a
     * property of this {@code ObjectNode}, replacing old value, if any.
     *<p>
     * <b>NOTE</b>: Unlike all <b>put(...)</b> methods, return value
     * is <b>NOT</b> this <code>ObjectNode</code>, but the
     * <b>newly created</b> <code>ArrayNode</code> instance.
     *
     * @return Newly constructed ArrayNode (NOT the old value,
     *   which could be of any type)
     */
    public ArrayNode putArray(String propertyName)
    {
        ArrayNode n  = arrayNode();
        _put(propertyName, n);
        return n;
    }

    /**
     * Method that will construct an ObjectNode and add it as a
     * property of this {@code ObjectNode}, replacing old value, if any.
     *<p>
     * <b>NOTE</b>: Unlike all <b>put(...)</b> methods, return value
     * is <b>NOT</b> this <code>ObjectNode</code>, but the
     * <b>newly created</b> <code>ObjectNode</code> instance.
     *
     * @return Newly constructed ObjectNode (NOT the old value,
     *   which could be of any type)
     */
    public ObjectNode putObject(String propertyName)
    {
        ObjectNode n = objectNode();
        _put(propertyName, n);
        return n;
    }

    /**
     * Method for adding an opaque Java value as the value of specified property.
     * Value can be serialized like any other property, as long as Jackson can
     * serialize it. Despite term "POJO" this allows use of about any Java type, including
     * {@link java.util.Map}s, {@link java.util.Collection}s, as well as Beans (POJOs),
     * primitives/wrappers and even {@link JsonNode}s.
     * Method is most commonly useful when composing content to serialize from heterogenous
     * sources.
     *<p>
     * NOTE: if using {@link JsonNode#toString()} (or {@link JsonNode#toPrettyString()}
     * support for serialization may be more limited, compared to serializing node
     * with specifically configured {@link ObjectMapper}.
     *
     * @param propertyName Name of property to set.
     * @param pojo Java value to set as the property value
     *
     * @return This {@code ObjectNode} (to allow chaining)
     */
    public ObjectNode putPOJO(String propertyName, Object pojo) {
        return _put(propertyName, pojoNode(pojo));
    }

    /**
     * @since 2.6
     */
    public ObjectNode putRawValue(String propertyName, RawValue raw) {
        return _put(propertyName, rawValueNode(raw));
    }

    /**
     * Method for setting value of a property to explicit {@code null} value.
     *
     * @param propertyName Name of property to set.
     *
     * @return This {@code ObjectNode} (to allow chaining)
     */
    public ObjectNode putNull(String propertyName)
    {
        _children.put(propertyName, nullNode());
        return this;
    }

    /**
     * Method for setting value of a property to specified numeric value.
     *
     * @return This node (to allow chaining)
     */
    public ObjectNode put(String propertyName, short v) {
        return _put(propertyName, numberNode(v));
    }

    /**
     * Alternative method that we need to avoid bumping into NPE issues
     * with auto-unboxing.
     *
     * @return This node (to allow chaining)
     */
    public ObjectNode put(String fieldName, Short v) {
        return _put(fieldName, (v == null) ? nullNode()
                : numberNode(v.shortValue()));
    }

    /**
     * Method for setting value of a field to specified numeric value.
     * The underlying {@link JsonNode} that will be added is constructed
     * using {@link JsonNodeFactory#numberNode(int)}, and may be
     *  "smaller" (like {@link ShortNode}) in cases where value fits within
     *  range of a smaller integral numeric value.
     *
     * @return This node (to allow chaining)
     */
    public ObjectNode put(String fieldName, int v) {
        return _put(fieldName, numberNode(v));
    }

    /**
     * Alternative method that we need to avoid bumping into NPE issues
     * with auto-unboxing.
     *
     * @return This node (to allow chaining)
     */
    public ObjectNode put(String fieldName, Integer v) {
        return _put(fieldName, (v == null) ? nullNode()
                : numberNode(v.intValue()));
    }

    /**
     * Method for setting value of a field to specified numeric value.
     * The underlying {@link JsonNode} that will be added is constructed
     * using {@link JsonNodeFactory#numberNode(long)}, and may be
     *  "smaller" (like {@link IntNode}) in cases where value fits within
     *  range of a smaller integral numeric value.
     *
     * @return This node (to allow chaining)
     */
    public ObjectNode put(String fieldName, long v) {
        return _put(fieldName, numberNode(v));
    }

    /**
     * Method for setting value of a field to specified numeric value.
     * The underlying {@link JsonNode} that will be added is constructed
     * using {@link JsonNodeFactory#numberNode(Long)}, and may be
     *  "smaller" (like {@link IntNode}) in cases where value fits within
     *  range of a smaller integral numeric value.
     * <p>
     * Note that this is alternative to {@link #put(String, long)} needed to avoid
     * bumping into NPE issues with auto-unboxing.
     *
     * @return This node (to allow chaining)
     */
    public ObjectNode put(String fieldName, Long v) {
        return _put(fieldName, (v == null) ? nullNode()
                : numberNode(v.longValue()));
    }

    /**
     * Method for setting value of a field to specified numeric value.
     *
     * @return This node (to allow chaining)
     */
    public ObjectNode put(String fieldName, float v) {
        return _put(fieldName, numberNode(v));
    }

    /**
     * Alternative method that we need to avoid bumping into NPE issues
     * with auto-unboxing.
     *
     * @return This node (to allow chaining)
     */
    public ObjectNode put(String fieldName, Float v) {
        return _put(fieldName, (v == null) ? nullNode()
                : numberNode(v.floatValue()));
    }

    /**
     * Method for setting value of a field to specified numeric value.
     *
     * @return This node (to allow chaining)
     */
    public ObjectNode put(String fieldName, double v) {
        return _put(fieldName, numberNode(v));
    }

    /**
     * Alternative method that we need to avoid bumping into NPE issues
     * with auto-unboxing.
     *
     * @return This node (to allow chaining)
     */
    public ObjectNode put(String fieldName, Double v) {
        return _put(fieldName, (v == null) ? nullNode()
                : numberNode(v.doubleValue()));
    }

    /**
     * Method for setting value of a field to specified numeric value.
     *
     * @return This node (to allow chaining)
     */
    public ObjectNode put(String fieldName, BigDecimal v) {
        return _put(fieldName, (v == null) ? nullNode()
                : numberNode(v));
    }

    /**
     * Method for setting value of a field to specified numeric value.
     *
     * @return This node (to allow chaining)
     *
     * @since 2.9
     */
    public ObjectNode put(String fieldName, BigInteger v) {
        return _put(fieldName, (v == null) ? nullNode()
                : numberNode(v));
    }

    /**
     * Method for setting value of a field to specified String value.
     *
     * @return This node (to allow chaining)
     */
    public ObjectNode put(String fieldName, String v) {
        return _put(fieldName, (v == null) ? nullNode()
                : textNode(v));
    }

    /**
     * Method for setting value of a field to specified String value.
     *
     * @return This node (to allow chaining)
     */
    public ObjectNode put(String fieldName, boolean v) {
        return _put(fieldName, booleanNode(v));
    }

    /**
     * Alternative method that we need to avoid bumping into NPE issues
     * with auto-unboxing.
     *
     * @return This node (to allow chaining)
     */
    public ObjectNode put(String fieldName, Boolean v) {
        return _put(fieldName, (v == null) ? nullNode()
                : booleanNode(v.booleanValue()));
    }

    /**
     * Method for setting value of a field to specified binary value
     *
     * @return This node (to allow chaining)
     */
    public ObjectNode put(String fieldName, byte[] v) {
        return _put(fieldName, (v == null) ? nullNode()
                : binaryNode(v));
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
        if (o instanceof ObjectNode) {
            return _childrenEqual((ObjectNode) o);
        }
        return false;
    }

    /**
     * @since 2.3
     */
    protected boolean _childrenEqual(ObjectNode other)
    {
        return _children.equals(other._children);
    }

    @Override
    public int hashCode()
    {
        return _children.hashCode();
    }

    /*
    /**********************************************************
    /* Internal methods (overridable)
    /**********************************************************
     */

    protected ObjectNode _put(String fieldName, JsonNode value)
    {
        _children.put(fieldName, value);
        return this;
    }
}
