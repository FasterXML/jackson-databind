package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.node.*;
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
        return NullNode.getInstance();
    }

    /**
     * Implementation that will produce types of any JSON nodes; not just one
     * deserializer is registered to handle (in case of more specialized handler).
     * Overridden by typed sub-classes for more thorough checking
     */
    @Override
    public JsonNode deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        switch (p.getCurrentTokenId()) {
        case JsonTokenId.ID_START_OBJECT:
            return deserializeObject(p, ctxt, ctxt.getNodeFactory());
        case JsonTokenId.ID_START_ARRAY:
            return deserializeArray(p, ctxt, ctxt.getNodeFactory());
        default:
        }
        return deserializeAny(p, ctxt, ctxt.getNodeFactory());
    }

    /*
    /**********************************************************
    /* Specific instances for more accurate types
    /**********************************************************
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
                return deserializeObject(p, ctxt, ctxt.getNodeFactory());
            }
            if (p.hasToken(JsonToken.FIELD_NAME)) {
                return deserializeObjectAtName(p, ctxt, ctxt.getNodeFactory());
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
                return deserializeArray(p, ctxt, ctxt.getNodeFactory());
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

    /* 07-Nov-2014, tatu: When investigating [databind#604], realized that it makes
     *   sense to also mark this is cachable, since lookup not exactly free, and
     *   since it's not uncommon to "read anything"
     */
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
        throws JsonProcessingException
    {
        // [databind#237]: Report an error if asked to do so:
        if (ctxt.isEnabled(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)) {
            ctxt.reportInputMismatch(JsonNode.class,
                    "Duplicate field '%s' for ObjectNode: not allowed when FAIL_ON_READING_DUP_TREE_KEY enabled",
                    fieldName);
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
            final JsonNodeFactory nodeFactory) throws IOException
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
                value = deserializeObject(p, ctxt, nodeFactory);
                break;
            case JsonTokenId.ID_START_ARRAY:
                value = deserializeArray(p, ctxt, nodeFactory);
                break;
            case JsonTokenId.ID_EMBEDDED_OBJECT:
                value = _fromEmbedded(p, ctxt, nodeFactory);
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
                value = deserializeAny(p, ctxt, nodeFactory);
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
            final JsonNodeFactory nodeFactory) throws IOException
    {
        final ObjectNode node = nodeFactory.objectNode();
        String key = p.getCurrentName();
        for (; key != null; key = p.nextFieldName()) {
            JsonNode value;
            JsonToken t = p.nextToken();
            if (t == null) { // can this ever occur?
                t = JsonToken.NOT_AVAILABLE; // can this ever occur?
            }
            switch (t.id()) {
            case JsonTokenId.ID_START_OBJECT:
                value = deserializeObject(p, ctxt, nodeFactory);
                break;
            case JsonTokenId.ID_START_ARRAY:
                value = deserializeArray(p, ctxt, nodeFactory);
                break;
            case JsonTokenId.ID_EMBEDDED_OBJECT:
                value = _fromEmbedded(p, ctxt, nodeFactory);
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
                value = deserializeAny(p, ctxt, nodeFactory);
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
            key = p.getCurrentName();
        }
        for (; key != null; key = p.nextFieldName()) {
            // If not, fall through to regular handling
            JsonToken t = p.nextToken();

            // First: see if we can merge things:
            JsonNode old = node.get(key);
            if (old != null) {
                if (old instanceof ObjectNode) {
                    JsonNode newValue = updateObject(p, ctxt, (ObjectNode) old);
                    if (newValue != old) {
                        node.set(key, newValue);
                    }
                    continue;
                }
                if (old instanceof ArrayNode) {
                    JsonNode newValue = updateArray(p, ctxt, (ArrayNode) old);
                    if (newValue != old) {
                        node.set(key, newValue);
                    }
                    continue;
                }
            }
            if (t == null) { // can this ever occur?
                t = JsonToken.NOT_AVAILABLE;
            }
            JsonNode value;
            JsonNodeFactory nodeFactory = ctxt.getNodeFactory();
            switch (t.id()) {
            case JsonTokenId.ID_START_OBJECT:
                value = deserializeObject(p, ctxt, nodeFactory);
                break;
            case JsonTokenId.ID_START_ARRAY:
                value = deserializeArray(p, ctxt, nodeFactory);
                break;
            case JsonTokenId.ID_EMBEDDED_OBJECT:
                value = _fromEmbedded(p, ctxt, nodeFactory);
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
                value = deserializeAny(p, ctxt, nodeFactory);
            }
            if (old != null) {
                _handleDuplicateField(p, ctxt, nodeFactory,
                        key, node, old, value);
            }
            node.set(key, value);
        }
        return node;
    }

    protected final ArrayNode deserializeArray(JsonParser p, DeserializationContext ctxt,
            final JsonNodeFactory nodeFactory) throws IOException
    {
        ArrayNode node = nodeFactory.arrayNode();
        while (true) {
            JsonToken t = p.nextToken();
            switch (t.id()) {
            case JsonTokenId.ID_START_OBJECT:
                node.add(deserializeObject(p, ctxt, nodeFactory));
                break;
            case JsonTokenId.ID_START_ARRAY:
                node.add(deserializeArray(p, ctxt, nodeFactory));
                break;
            case JsonTokenId.ID_END_ARRAY:
                return node;
            case JsonTokenId.ID_EMBEDDED_OBJECT:
                node.add(_fromEmbedded(p, ctxt, nodeFactory));
                break;
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
                node.add(deserializeAny(p, ctxt, nodeFactory));
                break;
            }
        }
    }

    /**
     * Alternate deserialization method that is to update existing {@link ObjectNode}
     * if possible.
     *
     * @since 2.9
     */
    protected final JsonNode updateArray(JsonParser p, DeserializationContext ctxt,
        final ArrayNode node) throws IOException
    {
        final JsonNodeFactory nodeFactory = ctxt.getNodeFactory();
        while (true) {
            JsonToken t = p.nextToken();
            switch (t.id()) {
            case JsonTokenId.ID_START_OBJECT:
                node.add(deserializeObject(p, ctxt, nodeFactory));
                break;
            case JsonTokenId.ID_START_ARRAY:
                node.add(deserializeArray(p, ctxt, nodeFactory));
                break;
            case JsonTokenId.ID_END_ARRAY:
                return node;
            case JsonTokenId.ID_EMBEDDED_OBJECT:
                node.add(_fromEmbedded(p, ctxt, nodeFactory));
                break;
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
                node.add(deserializeAny(p, ctxt, nodeFactory));
                break;
            }
        }
    }
    
    protected final JsonNode deserializeAny(JsonParser p, DeserializationContext ctxt,
            final JsonNodeFactory nodeFactory) throws IOException
    {
        switch (p.getCurrentTokenId()) {
        case JsonTokenId.ID_END_OBJECT: // for empty JSON Objects we may point to this?
            return nodeFactory.objectNode();
        case JsonTokenId.ID_FIELD_NAME:
            return deserializeObjectAtName(p, ctxt, nodeFactory);
        case JsonTokenId.ID_EMBEDDED_OBJECT:
            return _fromEmbedded(p, ctxt, nodeFactory);
        case JsonTokenId.ID_STRING:
            return nodeFactory.textNode(p.getText());
        case JsonTokenId.ID_NUMBER_INT:
            return _fromInt(p, ctxt, nodeFactory);
        case JsonTokenId.ID_NUMBER_FLOAT:
            return _fromFloat(p, ctxt, nodeFactory);
        case JsonTokenId.ID_TRUE:
            return nodeFactory.booleanNode(true);
        case JsonTokenId.ID_FALSE:
            return nodeFactory.booleanNode(false);
        case JsonTokenId.ID_NULL:
            return nodeFactory.nullNode();

            /* Caller checks for these, should not get here ever
        case JsonTokenId.ID_START_OBJECT:
            return deserializeObject(p, ctxt, nodeFactory);
        case JsonTokenId.ID_START_ARRAY:
            return deserializeArray(p, ctxt, nodeFactory);
            */

            
            // These states can not be mapped; input stream is
            // off by an event or two

        //case END_OBJECT:
        //case END_ARRAY:
        default:
        }
        return (JsonNode) ctxt.handleUnexpectedToken(handledType(), p);
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

    protected final JsonNode _fromFloat(JsonParser p, DeserializationContext ctxt,
            final JsonNodeFactory nodeFactory) throws IOException
    {
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

    protected final JsonNode _fromEmbedded(JsonParser p, DeserializationContext ctxt,
            JsonNodeFactory nodeFactory) throws IOException
    {
        Object ob = p.getEmbeddedObject();
        if (ob == null) { // should this occur?
            return nodeFactory.nullNode();
        }
        Class<?> type = ob.getClass();
        if (type == byte[].class) { // most common special case
            return nodeFactory.binaryNode((byte[]) ob);
        }
        // [databind#743]: Don't forget RawValue
        if (ob instanceof RawValue) {
            return nodeFactory.rawValueNode((RawValue) ob);
        }
        if (ob instanceof JsonNode) {
            // [databind#433]: but could also be a JsonNode hiding in there!
            return (JsonNode) ob;
        }
        // any other special handling needed?
        return nodeFactory.pojoNode(ob);
    }
}
