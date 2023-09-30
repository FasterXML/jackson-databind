package tools.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import tools.jackson.core.*;
import tools.jackson.core.tree.ObjectTreeNode;
import tools.jackson.core.type.WritableTypeId;

import tools.jackson.databind.*;
import tools.jackson.databind.cfg.JsonNodeFeature;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.util.RawValue;

/**
 * Node that maps to JSON Object structures in JSON content.
 */
public class ObjectNode
    extends ContainerNode<ObjectNode>
    implements ObjectTreeNode, // since 3.0
        java.io.Serializable
{
    private static final long serialVersionUID = 3L;

    // Note: LinkedHashMap for backwards compatibility
    protected final Map<String, JsonNode> _children;

    public ObjectNode(JsonNodeFactory nc) {
        super(nc);
        _children = new LinkedHashMap<String, JsonNode>();
    }

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
    /**********************************************************************
    /* Support for withArray()/withObject()
    /**********************************************************************
     */

    @Override
    public ObjectNode withObjectProperty(String propName) {
        JsonNode child = _children.get(propName);
        if (child == null || child.isNull()) {
            return putObject(propName);
        }
        if (child.isObject()) {
            return (ObjectNode) child;
        }
        return _reportWrongNodeType(
"Cannot replace `JsonNode` of type `%s` with `ObjectNode` for property \"%s\" (default mode `OverwriteMode.%s`)",
child.getClass().getName(), propName, OverwriteMode.NULLS);
    }

    @Override
    public ArrayNode withArrayProperty(String propName) {
        JsonNode child = _children.get(propName);
        if (child == null || child.isNull()) {
            return putArray(propName);
        }
        if (child.isArray()) {
            return (ArrayNode) child;
        }
        return _reportWrongNodeType(
"Cannot replace `JsonNode` of type `%s` with `ArrayNode` for property \"%s\" with (default mode `OverwriteMode.%s`)",
child.getClass().getName(), propName, OverwriteMode.NULLS);
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
    /**********************************************************************
    /* Overrides for JacksonSerializable.Base
    /**********************************************************************
     */

    @Override
    public boolean isEmpty(SerializerProvider serializers) {
        return _children.isEmpty();
    }

    /*
    /**********************************************************************
    /* Implementation of core JsonNode API
    /**********************************************************************
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

    @Override
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
    public Iterator<String> propertyNames() {
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

    /**
     * Method to use for accessing all properties (with both names
     * and values) of this JSON Object.
     *
     * @since 2.15
     */
    @Override
    public Set<Map.Entry<String, JsonNode>> properties() {
        return _children.entrySet();
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
    /**********************************************************************
    /* Public API, finding value nodes
    /**********************************************************************
     */

    @Override
    public JsonNode findValue(String propertyName)
    {
        JsonNode jsonNode = _children.get(propertyName);
        if (jsonNode != null) {
            return jsonNode;
        }

        for (JsonNode child : _children.values()) {
            JsonNode value = child.findValue(propertyName);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    @Override
    public List<JsonNode> findValues(String propertyName, List<JsonNode> foundSoFar)
    {
        JsonNode jsonNode = _children.get(propertyName);
        if (jsonNode != null) {
            if (foundSoFar == null) {
                foundSoFar = new ArrayList<>();
            }
            foundSoFar.add(jsonNode);
            return foundSoFar;
        }

        // only add children if parent not added
        for (JsonNode child : _children.values()) {
            foundSoFar = child.findValues(propertyName, foundSoFar);
        }
        return foundSoFar;
    }

    @Override
    public List<String> findValuesAsText(String propertyName, List<String> foundSoFar)
    {
        JsonNode jsonNode = _children.get(propertyName);
        if (jsonNode != null) {
            if (foundSoFar == null) {
                foundSoFar = new ArrayList<>();
            }
            foundSoFar.add(jsonNode.asText());
            return foundSoFar;
        }

        // only add children if parent not added
        for (JsonNode child : _children.values()) {
            foundSoFar = child.findValuesAsText(propertyName, foundSoFar);
        }
        return foundSoFar;
    }

    @Override
    public ObjectNode findParent(String propertyName)
    {
        JsonNode jsonNode = _children.get(propertyName);
        if (jsonNode != null) {
            return this;
        }
        for (JsonNode child : _children.values()) {
            JsonNode value = child.findParent(propertyName);
            if (value != null) {
                return (ObjectNode) value;
            }
        }
        return null;
    }

    @Override
    public List<JsonNode> findParents(String propertyName, List<JsonNode> foundSoFar)
    {
        JsonNode jsonNode = _children.get(propertyName);
        if (jsonNode != null) {
            if (foundSoFar == null) {
                foundSoFar = new ArrayList<>();
            }
            foundSoFar.add(this);
            return foundSoFar;
        }

        // only add children if parent not added
        for (JsonNode child : _children.values()) {
            foundSoFar = child.findParents(propertyName, foundSoFar);
        }
        return foundSoFar;
    }

    /*
    /**********************************************************************
    /* Public API, serialization
    /**********************************************************************
     */

    /**
     * Method that can be called to serialize this node and
     * all of its descendants using specified JSON generator.
     */
    @SuppressWarnings("deprecation")
    @Override
    public void serialize(JsonGenerator g, SerializerProvider ctxt)
        throws JacksonException
    {
        final int len = _children.size();

        if (len == 0) { // minor optimization
            g.writeStartObject(this, 0);
            g.writeEndObject();
            return;
        }
        if (ctxt != null) {
            boolean trimEmptyArray = !ctxt.isEnabled(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);
            boolean skipNulls = !ctxt.isEnabled(JsonNodeFeature.WRITE_NULL_PROPERTIES);
            if (trimEmptyArray || skipNulls) {
                g.writeStartObject(this, _children.size());
                serializeFilteredContents(g, ctxt, trimEmptyArray, skipNulls);
                g.writeEndObject();
                return;
            }
        }
        g.writeStartObject(this, _children.size());
        for (Map.Entry<String, JsonNode> en : _contentsToSerialize(ctxt).entrySet()) {
            g.writeName(en.getKey());
            en.getValue().serialize(g, ctxt);
        }
        g.writeEndObject();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void serializeWithType(JsonGenerator g, SerializerProvider ctxt,
            TypeSerializer typeSer)
        throws JacksonException
    {
        boolean trimEmptyArray = false;
        boolean skipNulls = false;
        if (ctxt != null) {
            trimEmptyArray = !ctxt.isEnabled(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);
            skipNulls = !ctxt.isEnabled(JsonNodeFeature.WRITE_NULL_PROPERTIES);
        }

        WritableTypeId typeIdDef = typeSer.writeTypePrefix(g, ctxt,
                typeSer.typeId(this, JsonToken.START_OBJECT));

        if (trimEmptyArray || skipNulls) {
            serializeFilteredContents(g, ctxt, trimEmptyArray, skipNulls);
        } else {
            for (Map.Entry<String, JsonNode> en : _contentsToSerialize(ctxt).entrySet()) {
                g.writeName(en.getKey());
                en.getValue().serialize(g, ctxt);
            }
        }
        typeSer.writeTypeSuffix(g, ctxt, typeIdDef);
    }

    /**
     * Helper method shared and called by {@link #serialize} and {@link #serializeWithType}
     * in cases where actual filtering is needed based on configuration.
     */
    protected void serializeFilteredContents(final JsonGenerator g, final SerializerProvider ctxt,
            final boolean trimEmptyArray, final boolean skipNulls)
        throws JacksonException
    {
        for (Map.Entry<String, JsonNode> en : _contentsToSerialize(ctxt).entrySet()) {
            // 17-Feb-2009, tatu: Can we trust that all nodes will always
            //   extend BaseJsonNode? Or if not, at least implement JsonSerializable?
            //   Let's start with former, change if we must.
            // 19-Jun-2023, tatu: Actually `JsonNode` is enough
            JsonNode value = en.getValue();

            // as per [databind#867], see if WRITE_EMPTY_JSON_ARRAYS feature is disabled,
            // if the feature is disabled, then should not write an empty array
            // to the output, so continue to the next element in the iteration
            if (trimEmptyArray && value.isArray() && value.isEmpty(ctxt)) {
                continue;
            }
            if (skipNulls && value.isNull()) {
                continue;
            }
            g.writeName(en.getKey());
            value.serialize(g, ctxt);
        }
    }

    /**
     * Helper method for encapsulating details of accessing child node entries
     * to serialize.
     *
     * @since 2.16
     */
    protected Map<String, JsonNode> _contentsToSerialize(SerializerProvider ctxt) {
        if (ctxt.isEnabled(JsonNodeFeature.WRITE_PROPERTIES_SORTED)) {
            if (!_children.isEmpty()) {
                return new TreeMap<>(_children);
            }
        }
        return _children;
    }

    /*
    /**********************************************************************
    /* Extended ObjectNode API, mutators
    /**********************************************************************
     */

    /**
     * Method that will set specified property, replacing old value, if any.
     * Note that this is identical to {@link #replace(String, JsonNode)},
     * except for return value.
     *
     * @param propertyName Name of property to set
     * @param value Value to set property to; if null, will be converted
     *   to a {@link NullNode} first  (to remove a property, call
     *   {@link #remove} instead)
     *
     * @return This node after adding/replacing property value (to allow chaining)
     */
    public ObjectNode set(String propertyName, JsonNode value)
    {
        if (value == null) {
            value = nullNode();
        }
        _children.put(propertyName, value);
        return this;
    }

    /**
     * Method for adding given properties to this object node, overriding
     * any existing values for those properties.
     *
     * @param properties Properties to add
     *
     * @return This node after adding/replacing property values (to allow chaining)
     */
    public ObjectNode setAll(Map<String,? extends JsonNode> properties)
    {
        for (Map.Entry<String,? extends JsonNode> en : properties.entrySet()) {
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
     */
    public ObjectNode setAll(ObjectNode other)
    {
        _children.putAll(other._children);
        return this;
    }

    /**
     * Method for replacing value of specific property with passed
     * value, and returning previous value (or null if none).
     *
     * @param propertyName Property of which value to replace
     * @param value Value to set property to, replacing old value if any
     *
     * @return Old value of the property; null if there was no such property
     *   with value
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
     *
     * @return This node after removing property (if any)
     */
    public ObjectNode without(String fieldName)
    {
        _children.remove(fieldName);
        return this;
    }

    /**
     * Method for removing specified field properties out of
     * this ObjectNode.
     *
     * @param propertyNames Names of properties to remove
     *
     * @return This node after removing entries
     */
    public ObjectNode without(Collection<String> propertyNames)
    {
        _children.keySet().removeAll(propertyNames);
        return this;
    }

    /*
    /**********************************************************************
    /* Extended ObjectNode API, mutators, generic
    /**********************************************************************
     */

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
     * Method for removing all field properties out of this ObjectNode
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
    /**********************************************************************
    /* Extended ObjectNode API, mutators, typed
    /**********************************************************************
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
     *   which could be of any type, nor {@code this} node)
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
     *   which could be of any type, nor {@code this} node)
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
    /**********************************************************************
    /* Standard method overrides
    /**********************************************************************
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

    protected boolean _childrenEqual(ObjectNode other) {
        return _children.equals(other._children);
    }

    @Override
    public int hashCode() {
        return _children.hashCode();
    }

    /*
    /**********************************************************************
    /* Internal methods (overridable)
    /**********************************************************************
     */

    protected ObjectNode _put(String fieldName, JsonNode value)
    {
        _children.put(fieldName, value);
        return this;
    }
}
