package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.util.Arrays;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.fasterxml.jackson.databind.util.RawValue;

/**
 * Deserializer that can build instances of {@link JsonNode} from any
 * JSON content, using appropriate {@link JsonNode} type.
 */
@SuppressWarnings("serial")
public class JsonNodeDeserializer
    extends BaseNodeDeserializer<JsonNode>
{
    /**
     * Singleton instance of generic deserializer for {@link JsonNode}.
     * Only used for types other than JSON Object and Array.
     */
    private final static JsonNodeDeserializer instance = new JsonNodeDeserializer();

    protected JsonNodeDeserializer() {
        // `null` means that explicit "merge" is honored and may or may not work, but
        // that per-type and global defaults do not enable merging. This because
        // some node types (Object, Array) do support, others don't.
        super(JsonNode.class, null);
    }

    /**
     * Factory method for accessing deserializer for specific node type
     */
    public static JsonDeserializer<? extends JsonNode> getDeserializer(Class<?> nodeClass)
    {
        if (nodeClass == ObjectNode.class) {
            return ObjectDeserializer.getInstance();
        }
        if (nodeClass == ArrayNode.class) {
            return ArrayDeserializer.getInstance();
        }
        // For others, generic one works fine
        return instance;
    }

    /*
    /**********************************************************
    /* Actual deserializer implementations
    /**********************************************************
     */

    @Override
    public JsonNode getNullValue(DeserializationContext ctxt) {
        return ctxt.getNodeFactory().nullNode();
    }

    /**
     * Implementation that will produce types of any JSON nodes; not just one
     * deserializer is registered to handle (in case of more specialized handler).
     * Overridden by typed sub-classes for more thorough checking
     */
    @Override
    public JsonNode deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        final ContainerStack stack = new ContainerStack();
        final JsonNodeFactory nodeF = ctxt.getNodeFactory();
        switch (p.currentTokenId()) {
        case JsonTokenId.ID_START_OBJECT:
            return deserializeObject(p, ctxt, nodeF, stack);
        case JsonTokenId.ID_END_OBJECT:
            return nodeF.objectNode();
        case JsonTokenId.ID_START_ARRAY:
            return deserializeArray(p, ctxt, nodeF, stack);
        case JsonTokenId.ID_FIELD_NAME:
            return deserializeObjectAtName(p, ctxt, nodeF, stack);
        default:
        }
        return deserializeAnyScalar(p, ctxt);
    }

    /*
    /**********************************************************************
    /* Specific instances for more accurate types
    /**********************************************************************
     */

    final static class ObjectDeserializer
        extends BaseNodeDeserializer<ObjectNode>
    {
        private static final long serialVersionUID = 1L;

        protected final static ObjectDeserializer _instance = new ObjectDeserializer();

        protected ObjectDeserializer() { super(ObjectNode.class, true); }

        public static ObjectDeserializer getInstance() { return _instance; }

        @Override
        public ObjectNode deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
        {
            if (p.isExpectedStartObjectToken()) {
                return deserializeObject(p, ctxt, ctxt.getNodeFactory(), new ContainerStack());
            }
            if (p.hasToken(JsonToken.FIELD_NAME)) {
                return deserializeObjectAtName(p, ctxt, ctxt.getNodeFactory(), new ContainerStack());
            }
            // 23-Sep-2015, tatu: Ugh. We may also be given END_OBJECT (similar to FIELD_NAME),
            //    if caller has advanced to the first token of Object, but for empty Object
            if (p.hasToken(JsonToken.END_OBJECT)) {
                return ctxt.getNodeFactory().objectNode();
            }
            return (ObjectNode) ctxt.handleUnexpectedToken(ObjectNode.class, p);
         }

        /**
         * Variant needed to support both root-level `updateValue()` and merging.
         *
         * @since 2.9
         */
        @Override
        public ObjectNode deserialize(JsonParser p, DeserializationContext ctxt,
                ObjectNode node) throws IOException
        {
            if (p.isExpectedStartObjectToken() || p.hasToken(JsonToken.FIELD_NAME)) {
                return (ObjectNode) updateObject(p, ctxt, (ObjectNode) node);
            }
            return (ObjectNode) ctxt.handleUnexpectedToken(ObjectNode.class, p);
        }
    }

    final static class ArrayDeserializer
        extends BaseNodeDeserializer<ArrayNode>
    {
        private static final long serialVersionUID = 1L;

        protected final static ArrayDeserializer _instance = new ArrayDeserializer();

        protected ArrayDeserializer() { super(ArrayNode.class, true); }

        public static ArrayDeserializer getInstance() { return _instance; }

        @Override
        public ArrayNode deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
        {
            if (p.isExpectedStartArrayToken()) {
                return deserializeArray(p, ctxt, ctxt.getNodeFactory(),
                        new ContainerStack());
            }
            return (ArrayNode) ctxt.handleUnexpectedToken(ArrayNode.class, p);
        }

        /**
         * Variant needed to support both root-level `updateValue()` and merging.
         *
         * @since 2.9
         */
        @Override
        public ArrayNode deserialize(JsonParser p, DeserializationContext ctxt,
                ArrayNode node) throws IOException
        {
            if (p.isExpectedStartArrayToken()) {
                return (ArrayNode) updateArray(p, ctxt, (ArrayNode) node);
            }
            return (ArrayNode) ctxt.handleUnexpectedToken(ArrayNode.class, p);
        }
    }
}

/**
 * Base class for all actual {@link JsonNode} deserializer
 * implementations
 */
@SuppressWarnings("serial")
abstract class BaseNodeDeserializer<T extends JsonNode>
    extends StdDeserializer<T>
{
    protected final Boolean _supportsUpdates;

    public BaseNodeDeserializer(Class<T> vc, Boolean supportsUpdates) {
        super(vc);
        _supportsUpdates = supportsUpdates;
    }

    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer)
        throws IOException
    {
        // Output can be as JSON Object, Array or scalar: no way to know a priori:
        return typeDeserializer.deserializeTypedFromAny(p, ctxt);
    }

    @Override // since 2.12
    public LogicalType logicalType() {
        return LogicalType.Untyped;
    }

    // 07-Nov-2014, tatu: When investigating [databind#604], realized that it makes
    //   sense to also mark this is cachable, since lookup not exactly free, and
    //   since it's not uncommon to "read anything"
    @Override
    public boolean isCachable() { return true; }

    @Override // since 2.9
    public Boolean supportsUpdate(DeserializationConfig config) {
        return _supportsUpdates;
    }

    /*
    /**********************************************************
    /* Overridable methods
    /**********************************************************
     */

    /**
     * Method called when there is a duplicate value for a field.
     * By default we don't care, and the last value is used.
     * Can be overridden to provide alternate handling, such as throwing
     * an exception, or choosing different strategy for combining values
     * or choosing which one to keep.
     *
     * @param fieldName Name of the field for which duplicate value was found
     * @param objectNode Object node that contains values
     * @param oldValue Value that existed for the object node before newValue
     *   was added
     * @param newValue Newly added value just added to the object node
     */
    protected void _handleDuplicateField(JsonParser p, DeserializationContext ctxt,
            JsonNodeFactory nodeFactory,
            String fieldName, ObjectNode objectNode,
            JsonNode oldValue, JsonNode newValue)
        throws IOException
    {
        // [databind#237]: Report an error if asked to do so:
        if (ctxt.isEnabled(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)) {
            // 11-Sep-2019, tatu: Can not pass "property name" because we may be
            //    missing enclosing JSON content context...
// ctxt.reportPropertyInputMismatch(JsonNode.class, fieldName,
            ctxt.reportInputMismatch(JsonNode.class,
"Duplicate field '%s' for `ObjectNode`: not allowed when `DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY` enabled",
                    fieldName);
        }
        // [databind#2732]: Special case for XML; automatically coerce into `ArrayNode`
        if (ctxt.isEnabled(StreamReadCapability.DUPLICATE_PROPERTIES)) {
            // Note that ideally we wouldn't have to shuffle things but... Map.putIfAbsent()
            // only added in JDK 8, to efficiently check for add. So...
            if (oldValue.isArray()) { // already was array, to append
                ((ArrayNode) oldValue).add(newValue);
                objectNode.replace(fieldName, oldValue);
            } else { // was not array, convert
                ArrayNode arr = nodeFactory.arrayNode();
                arr.add(oldValue);
                arr.add(newValue);
                objectNode.replace(fieldName, arr);
            }
        }
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    /**
     * Method called to deserialize Object node instance when there is no existing
     * node to modify.
     */
    protected final ObjectNode deserializeObject(JsonParser p, DeserializationContext ctxt,
            final JsonNodeFactory nodeFactory, final ContainerStack stack) throws IOException
    {
        final ObjectNode node = nodeFactory.objectNode();
        String key = p.nextFieldName();
        for (; key != null; key = p.nextFieldName()) {
            JsonNode value;
            JsonToken t = p.nextToken();
            if (t == null) { // can this ever occur?
                t = JsonToken.NOT_AVAILABLE; // can this ever occur?
            }
            switch (t.id()) {
            case JsonTokenId.ID_START_OBJECT:
                // Need to avoid deep recursion, so:
                value = deserializeContainerNonRecursive(p, ctxt, nodeFactory,
                        stack, nodeFactory.objectNode());
                break;
            case JsonTokenId.ID_START_ARRAY:
                // Ok to do one level of recursion:
                value = deserializeArray(p, ctxt, nodeFactory, stack);
                break;
            case JsonTokenId.ID_EMBEDDED_OBJECT:
                value = _fromEmbedded(p, ctxt);
                break;
            case JsonTokenId.ID_STRING:
                value = nodeFactory.textNode(p.getText());
                break;
            case JsonTokenId.ID_NUMBER_INT:
                value = _fromInt(p, ctxt, nodeFactory);
                break;
            case JsonTokenId.ID_TRUE:
                value = nodeFactory.booleanNode(true);
                break;
            case JsonTokenId.ID_FALSE:
                value = nodeFactory.booleanNode(false);
                break;
            case JsonTokenId.ID_NULL:
                value = nodeFactory.nullNode();
                break;
            default:
                value = deserializeRareScalar(p, ctxt);
            }
            JsonNode old = node.replace(key, value);
            if (old != null) {
                _handleDuplicateField(p, ctxt, nodeFactory,
                        key, node, old, value);
            }
        }
        return node;
    }

    /**
     * Alternate deserialization method used when parser already points to first
     * FIELD_NAME and not START_OBJECT.
     *
     * @since 2.9
     */
    protected final ObjectNode deserializeObjectAtName(JsonParser p, DeserializationContext ctxt,
            final JsonNodeFactory nodeFactory, final ContainerStack stack) throws IOException
    {
        final ObjectNode node = nodeFactory.objectNode();
        String key = p.currentName();
        for (; key != null; key = p.nextFieldName()) {
            JsonNode value;
            JsonToken t = p.nextToken();
            if (t == null) { // can this ever occur?
                t = JsonToken.NOT_AVAILABLE; // can this ever occur?
            }
            switch (t.id()) {
            case JsonTokenId.ID_START_OBJECT:
                // Need to avoid deep recursion, so:
                value = deserializeContainerNonRecursive(p, ctxt, nodeFactory,
                        stack, nodeFactory.objectNode());
                break;
            case JsonTokenId.ID_START_ARRAY:
                // Ok to do one level of recursion:
                value = deserializeArray(p, ctxt, nodeFactory, stack);
                break;
            case JsonTokenId.ID_STRING:
                value = nodeFactory.textNode(p.getText());
                break;
            case JsonTokenId.ID_NUMBER_INT:
                value = _fromInt(p, ctxt, nodeFactory);
                break;
            case JsonTokenId.ID_TRUE:
                value = nodeFactory.booleanNode(true);
                break;
            case JsonTokenId.ID_FALSE:
                value = nodeFactory.booleanNode(false);
                break;
            case JsonTokenId.ID_NULL:
                value = nodeFactory.nullNode();
                break;
            default:
                value = deserializeRareScalar(p, ctxt);
            }
            JsonNode old = node.replace(key, value);
            if (old != null) {
                _handleDuplicateField(p, ctxt, nodeFactory,
                        key, node, old, value);
            }
        }
        return node;
    }
    
    /**
     * Alternate deserialization method that is to update existing {@link ObjectNode}
     * if possible.
     *
     * @since 2.9
     */
    protected final JsonNode updateObject(JsonParser p, DeserializationContext ctxt,
        final ObjectNode node) throws IOException
    {
        String key;
        if (p.isExpectedStartObjectToken()) {
            key = p.nextFieldName();
        } else {
            if (!p.hasToken(JsonToken.FIELD_NAME)) {
                return deserialize(p, ctxt);
            }
            key = p.currentName();
        }
        final ContainerStack stack = new ContainerStack();
        for (; key != null; key = p.nextFieldName()) {
            // If not, fall through to regular handling
            JsonToken t = p.nextToken();

            // First: see if we can merge things:
            JsonNode old = node.get(key);
            if (old != null) {
                if (old instanceof ObjectNode) {
                    // [databind#3056]: merging only if had Object and
                    // getting an Object
                    if (t == JsonToken.START_OBJECT) {
                        JsonNode newValue = updateObject(p, ctxt, (ObjectNode) old);
                        if (newValue != old) {
                            node.set(key, newValue);
                        }
                        continue;
                    }
                } else if (old instanceof ArrayNode) {
                    // [databind#3056]: related to Object handling, ensure
                    // Array values also match for mergeability
                    if (t == JsonToken.START_ARRAY) {
                        JsonNode newValue = updateArray(p, ctxt, (ArrayNode) old);
                        if (newValue != old) {
                            node.set(key, newValue);
                        }
                        continue;
                    }
                }
            }
            if (t == null) { // can this ever occur?
                t = JsonToken.NOT_AVAILABLE;
            }
            JsonNode value;
            JsonNodeFactory nodeFactory = ctxt.getNodeFactory();
            switch (t.id()) {
            case JsonTokenId.ID_START_OBJECT:
                value = deserializeObject(p, ctxt, nodeFactory, stack);
                break;
            case JsonTokenId.ID_START_ARRAY:
                value = deserializeArray(p, ctxt, nodeFactory, stack);
                break;
            case JsonTokenId.ID_STRING:
                value = nodeFactory.textNode(p.getText());
                break;
            case JsonTokenId.ID_NUMBER_INT:
                value = _fromInt(p, ctxt, nodeFactory);
                break;
            case JsonTokenId.ID_TRUE:
                value = nodeFactory.booleanNode(true);
                break;
            case JsonTokenId.ID_FALSE:
                value = nodeFactory.booleanNode(false);
                break;
            case JsonTokenId.ID_NULL:
                value = nodeFactory.nullNode();
                break;
            default:
                value = deserializeRareScalar(p, ctxt);
            }
            // 15-Feb-2021, tatu: I don't think this should have been called
            //   on update case (was until 2.12.2) and was simply result of
            //   copy-paste.
            /*
            if (old != null) {
                _handleDuplicateField(p, ctxt, nodeFactory,
                        key, node, old, value);
            }
            */
            node.set(key, value);
        }
        return node;
    }

    protected final ArrayNode deserializeArray(JsonParser p, DeserializationContext ctxt,
            final JsonNodeFactory nodeFactory, final ContainerStack stack) throws IOException
    {
        ArrayNode node = nodeFactory.arrayNode();
        JsonToken t;

        while ((t = p.nextToken()) != null) {
            switch (t.id()) {
            case JsonTokenId.ID_START_OBJECT:
                // Need to avoid deep recursion, so:
                node.add(deserializeContainerNonRecursive(p, ctxt, nodeFactory,
                        stack, nodeFactory.objectNode()));
                break;
            case JsonTokenId.ID_START_ARRAY:
                // Need to avoid deep recursion, so:
                node.add(deserializeContainerNonRecursive(p, ctxt, nodeFactory,
                        stack, nodeFactory.arrayNode()));
                break;
            case JsonTokenId.ID_END_ARRAY:
                return node;
            case JsonTokenId.ID_STRING:
                node.add(nodeFactory.textNode(p.getText()));
                break;
            case JsonTokenId.ID_NUMBER_INT:
                node.add(_fromInt(p, ctxt, nodeFactory));
                break;
            case JsonTokenId.ID_TRUE:
                node.add(nodeFactory.booleanNode(true));
                break;
            case JsonTokenId.ID_FALSE:
                node.add(nodeFactory.booleanNode(false));
                break;
            case JsonTokenId.ID_NULL:
                node.add(nodeFactory.nullNode());
                break;
            default:
                node.add(deserializeRareScalar(p, ctxt));
                break;
            }
        }
        // should never really get here
        return node;
    }

    /**
     * Alternate deserialization method that is to update existing {@link ObjectNode}
     * if possible.
     */
    protected final JsonNode updateArray(JsonParser p, DeserializationContext ctxt,
        final ArrayNode node) throws IOException
    {
        final JsonNodeFactory nodeFactory = ctxt.getNodeFactory();
        final ContainerStack stack = new ContainerStack();
        while (true) {
            JsonToken t = p.nextToken();
            switch (t.id()) {
            case JsonTokenId.ID_START_OBJECT:
                node.add(deserializeObject(p, ctxt, nodeFactory, stack));
                break;
            case JsonTokenId.ID_START_ARRAY:
                node.add(deserializeArray(p, ctxt, nodeFactory, stack));
                break;
            case JsonTokenId.ID_END_ARRAY:
                return node;
            case JsonTokenId.ID_STRING:
                node.add(nodeFactory.textNode(p.getText()));
                break;
            case JsonTokenId.ID_NUMBER_INT:
                node.add(_fromInt(p, ctxt, nodeFactory));
                break;
            case JsonTokenId.ID_TRUE:
                node.add(nodeFactory.booleanNode(true));
                break;
            case JsonTokenId.ID_FALSE:
                node.add(nodeFactory.booleanNode(false));
                break;
            case JsonTokenId.ID_NULL:
                node.add(nodeFactory.nullNode());
                break;
            default:
                node.add(deserializeRareScalar(p, ctxt));
                break;
            }
        }
    }

    // Was called "deserializeAny()" in 2.12 and prior
    protected final JsonNode deserializeAnyScalar(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        final JsonNodeFactory nodeF = ctxt.getNodeFactory();
        switch (p.currentTokenId()) {
        case JsonTokenId.ID_END_OBJECT:
            return nodeF.objectNode();
        case JsonTokenId.ID_STRING:
            return nodeF.textNode(p.getText());
        case JsonTokenId.ID_NUMBER_INT:
            return _fromInt(p, ctxt, nodeF);
        case JsonTokenId.ID_NUMBER_FLOAT:
            return _fromFloat(p, ctxt);
        case JsonTokenId.ID_TRUE:
            return nodeF.booleanNode(true);
        case JsonTokenId.ID_FALSE:
            return nodeF.booleanNode(false);
        case JsonTokenId.ID_NULL:
            return nodeF.nullNode();
        case JsonTokenId.ID_EMBEDDED_OBJECT:
            return _fromEmbedded(p, ctxt);

        // Caller should check for anything else
        default:
        }
        return (JsonNode) ctxt.handleUnexpectedToken(handledType(), p);
    }

    protected final JsonNode deserializeRareScalar(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        // 28-Mar-2021, tatu: Only things that caller does not check
        switch (p.currentTokenId()) {
        case JsonTokenId.ID_END_OBJECT: // for empty JSON Objects we may point to this?
            return ctxt.getNodeFactory().objectNode();
        case JsonTokenId.ID_NUMBER_FLOAT:
            return _fromFloat(p, ctxt);
        case JsonTokenId.ID_EMBEDDED_OBJECT:
            return _fromEmbedded(p, ctxt);

        // Caller should check for anything else
        default:
        }
        return (JsonNode) ctxt.handleUnexpectedToken(handledType(), p);
    }

    // Non-recursive alternative, used beyond certain nesting level
    // @since 2.13.0
    protected final ContainerNode<?> deserializeContainerNonRecursive(JsonParser p, DeserializationContext ctxt,
            JsonNodeFactory nodeFactory, ContainerStack stack, ContainerNode<?> root)
        throws IOException
    {
        ContainerNode<?> curr = root;

        outer_loop:
        while (true) {
            if (curr.isObject()) {
                final ObjectNode currObject = (ObjectNode) curr;
                String propName = p.nextFieldName();

                for (; propName != null; propName = p.nextFieldName()) {
                    JsonNode value;
                    JsonToken t = p.nextToken();
                    if (t == null) { // can this ever occur?
                        t = JsonToken.NOT_AVAILABLE; // to trigger an exception
                    }
                    ContainerNode<?> newContainer = null;
                    switch (t.id()) {
                    case JsonTokenId.ID_START_OBJECT:
                        value = newContainer = nodeFactory.objectNode();
                        break;
                    case JsonTokenId.ID_START_ARRAY:
                        value = newContainer = nodeFactory.arrayNode();
                        break;
                    case JsonTokenId.ID_STRING:
                        value = nodeFactory.textNode(p.getText());
                        break;
                    case JsonTokenId.ID_NUMBER_INT:
                        value = _fromInt(p, ctxt, nodeFactory);
                        break;
                    case JsonTokenId.ID_TRUE:
                        value = nodeFactory.booleanNode(true);
                        break;
                    case JsonTokenId.ID_FALSE:
                        value = nodeFactory.booleanNode(false);
                        break;
                    case JsonTokenId.ID_NULL:
                        value = nodeFactory.nullNode();
                        break;
                    default:
                        value = deserializeRareScalar(p, ctxt);
                    }
                    JsonNode old = currObject.replace(propName, value);
                    if (old != null) {
                        _handleDuplicateField(p, ctxt, nodeFactory,
                                propName, currObject, old, value);
                    }
                    // But for Arrays/Objects, need to iterate over contents
                    if (newContainer != null) {
                        stack.push(curr);
                        curr = newContainer;
                        continue outer_loop;
                    }
                }
                // reached not-property-name, should be END_OBJECT
            } else {
                final ArrayNode currArray = (ArrayNode) curr;
                JsonToken t;

                arrayLoop:
                while ((t = p.nextToken()) != null) {
                    switch (t.id()) {
                    case JsonTokenId.ID_START_OBJECT:
                        stack.push(curr);
                        curr = nodeFactory.objectNode();
                        currArray.add(curr);
                        continue outer_loop;
                    case JsonTokenId.ID_START_ARRAY:
                        stack.push(curr);
                        curr = nodeFactory.arrayNode();
                        currArray.add(curr);
                        continue outer_loop;
                    case JsonTokenId.ID_END_ARRAY:
                        break arrayLoop;
                    case JsonTokenId.ID_STRING:
                        currArray.add(nodeFactory.textNode(p.getText()));
                        break;
                    case JsonTokenId.ID_NUMBER_INT:
                        currArray.add(_fromInt(p, ctxt, nodeFactory));
                        break;
                    case JsonTokenId.ID_TRUE:
                        currArray.add(nodeFactory.booleanNode(true));
                        break;
                    case JsonTokenId.ID_FALSE:
                        currArray.add(nodeFactory.booleanNode(false));
                        break;
                    case JsonTokenId.ID_NULL:
                        currArray.add(nodeFactory.nullNode());
                        break;
                    default:
                        currArray.add(deserializeRareScalar(p, ctxt));
                        break;
                    }
                }
                // Reached end of array, so...
            }
            if (stack.isEmpty()) {
                return root;
            }
            curr = stack.pop();
        }
    }

    protected final JsonNode _fromInt(JsonParser p, DeserializationContext ctxt,
            JsonNodeFactory nodeFactory) throws IOException
    {
        JsonParser.NumberType nt;
        int feats = ctxt.getDeserializationFeatures();
        if ((feats & F_MASK_INT_COERCIONS) != 0) {
            if (DeserializationFeature.USE_BIG_INTEGER_FOR_INTS.enabledIn(feats)) {
                nt = JsonParser.NumberType.BIG_INTEGER;
            } else if (DeserializationFeature.USE_LONG_FOR_INTS.enabledIn(feats)) {
                nt = JsonParser.NumberType.LONG;
            } else {
                nt = p.getNumberType();
            }
        } else {
            nt = p.getNumberType();
        }
        if (nt == JsonParser.NumberType.INT) {
            return nodeFactory.numberNode(p.getIntValue());
        }
        if (nt == JsonParser.NumberType.LONG) {
            return nodeFactory.numberNode(p.getLongValue());
        }
        return nodeFactory.numberNode(p.getBigIntegerValue());
    }

    protected final JsonNode _fromFloat(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        final JsonNodeFactory nodeFactory = ctxt.getNodeFactory();
        JsonParser.NumberType nt = p.getNumberType();
        if (nt == JsonParser.NumberType.BIG_DECIMAL) {
            return nodeFactory.numberNode(p.getDecimalValue());
        }
        if (ctxt.isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)) {
            // 20-May-2016, tatu: As per [databind#1028], need to be careful
            //   (note: JDK 1.8 would have `Double.isFinite()`)
            if (p.isNaN()) {
                return nodeFactory.numberNode(p.getDoubleValue());
            }
            return nodeFactory.numberNode(p.getDecimalValue());
        }
        if (nt == JsonParser.NumberType.FLOAT) {
            return nodeFactory.numberNode(p.getFloatValue());
        }
        return nodeFactory.numberNode(p.getDoubleValue());
    }

    protected final JsonNode _fromEmbedded(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        final JsonNodeFactory nodeF = ctxt.getNodeFactory();
        final Object ob = p.getEmbeddedObject();

        if (ob == null) { // should this occur?
            return nodeF.nullNode();
        }
        Class<?> type = ob.getClass();
        if (type == byte[].class) { // most common special case
            return nodeF.binaryNode((byte[]) ob);
        }
        // [databind#743]: Don't forget RawValue
        if (ob instanceof RawValue) {
            return nodeF.rawValueNode((RawValue) ob);
        }
        if (ob instanceof JsonNode) {
            // [databind#433]: but could also be a JsonNode hiding in there!
            return (JsonNode) ob;
        }
        // any other special handling needed?
        return nodeF.pojoNode(ob);
    }

    /*
    /**********************************************************************
    /* Helper classes
    /**********************************************************************
     */

    /**
     * Optimized variant similar in functionality to (a subset of)
     * {@link java.util.ArrayDeque}; used to hold enclosing Array/Object
     * nodes during recursion-as-iteration.
     */
    @SuppressWarnings("rawtypes")
    final static class ContainerStack
    {
        private ContainerNode[] _stack;
        private int _top;

        public ContainerStack() { }

        public boolean isEmpty() { return _top == 0; }

        public void push(ContainerNode node) {
            if (_stack == null) {
                _stack = new ContainerNode[10];
            } else if (_stack.length == _top) {
                // grow by 50%, for most part
                final int newSize = _top + Math.min(512, Math.max(10, _top>>1));
                _stack = Arrays.copyOf(_stack, newSize);
            }
            _stack[_top++] = node;
        }

        public ContainerNode pop() {
            if (_top == 0) {
                throw new IllegalStateException("ContainerStack empty");
            }
            // note: could clean up stack but due to usage pattern, should not make
            // any difference -- all nodes joined during and after construction and
            // after construction the whole stack is discarded
            return _stack[--_top];
        }
    }
}
