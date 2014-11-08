package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.node.*;

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

    protected JsonNodeDeserializer() { super(JsonNode.class); }

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
    public JsonNode getNullValue() {
        return NullNode.getInstance();
    }

    /**
     * Implementation that will produce types of any JSON nodes; not just one
     * deserializer is registered to handle (in case of more specialized handler).
     * Overridden by typed sub-classes for more thorough checking
     */
    @Override
    public JsonNode deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException
    {
        switch (jp.getCurrentTokenId()) {
        case JsonTokenId.ID_START_OBJECT:
            return deserializeObject(jp, ctxt, ctxt.getNodeFactory());
        case JsonTokenId.ID_START_ARRAY:
            return deserializeArray(jp, ctxt, ctxt.getNodeFactory());
        default:
            return deserializeAny(jp, ctxt, ctxt.getNodeFactory());
        }
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

        protected ObjectDeserializer() { super(ObjectNode.class); }

        public static ObjectDeserializer getInstance() { return _instance; }
        
        @Override
        public ObjectNode deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException
        {
            if (jp.getCurrentToken() == JsonToken.START_OBJECT) {
                jp.nextToken();
                return deserializeObject(jp, ctxt, ctxt.getNodeFactory());
            }
            if (jp.getCurrentToken() == JsonToken.FIELD_NAME) {
                return deserializeObject(jp, ctxt, ctxt.getNodeFactory());
            }
            throw ctxt.mappingException(ObjectNode.class);
         }
    }
        
    final static class ArrayDeserializer
        extends BaseNodeDeserializer<ArrayNode>
    {
        private static final long serialVersionUID = 1L;

        protected final static ArrayDeserializer _instance = new ArrayDeserializer();

        protected ArrayDeserializer() { super(ArrayNode.class); }

        public static ArrayDeserializer getInstance() { return _instance; }
        
        @Override
        public ArrayNode deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            if (jp.isExpectedStartArrayToken()) {
                return deserializeArray(jp, ctxt, ctxt.getNodeFactory());
            }
            throw ctxt.mappingException(ArrayNode.class);
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
    public BaseNodeDeserializer(Class<T> vc) {
        super(vc);
    }

    @Override
    public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer)
        throws IOException
    {
        /* Output can be as JSON Object, Array or scalar: no way to know
         * a priori. So:
         */
        return typeDeserializer.deserializeTypedFromAny(jp, ctxt);
    }

    /* 07-Nov-2014, tatu: When investigating [databind#604], realized that it makes
     *   sense to also mark this is cachable, since lookup not exactly free, and
     *   since it's not uncommon to "read anything"
     */
    @Override
    public boolean isCachable() { return true; }

    /*
    /**********************************************************
    /* Overridable methods
    /**********************************************************
     */

    protected void _reportProblem(JsonParser jp, String msg) throws JsonMappingException {
        throw new JsonMappingException(msg, jp.getTokenLocation());
    }

    /**
     * 
     * @deprecated Since 2.3, use the overloaded variant
     */
    @Deprecated
    protected void _handleDuplicateField(String fieldName, ObjectNode objectNode,
                                         JsonNode oldValue, JsonNode newValue)
        throws JsonProcessingException
    {
        // By default, we don't do anything
        ;
    }

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
    protected void _handleDuplicateField(JsonParser jp, DeserializationContext ctxt,
            JsonNodeFactory nodeFactory,
            String fieldName, ObjectNode objectNode,
            JsonNode oldValue, JsonNode newValue)
        throws JsonProcessingException
    {
        // [Issue#237]: Report an error if asked to do so:
        if (ctxt.isEnabled(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)) {
            _reportProblem(jp, "Duplicate field '"+fieldName+"' for ObjectNode: not allowed when FAIL_ON_READING_DUP_TREE_KEY enabled");
        }
        // Backwards-compatibility; call in case it's overloaded
        _handleDuplicateField(fieldName, objectNode, oldValue, newValue);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    protected final ObjectNode deserializeObject(JsonParser jp, DeserializationContext ctxt,
            final JsonNodeFactory nodeFactory) throws IOException
    {
        ObjectNode node = nodeFactory.objectNode();
        JsonToken t = jp.getCurrentToken();
        if (t == JsonToken.START_OBJECT) {
            t = jp.nextToken();
        }
        for (; t == JsonToken.FIELD_NAME; t = jp.nextToken()) {
            String fieldName = jp.getCurrentName();
            JsonNode value;
            t = jp.nextToken();
            switch (t.id()) {
            case JsonTokenId.ID_START_OBJECT:
                value = deserializeObject(jp, ctxt, nodeFactory);
                break;
            case JsonTokenId.ID_START_ARRAY:
                value = deserializeArray(jp, ctxt, nodeFactory);
                break;
            case JsonTokenId.ID_STRING:
                value = nodeFactory.textNode(jp.getText());
                break;
            case JsonTokenId.ID_NUMBER_INT:
                value = _fromInt(jp, ctxt, nodeFactory);
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
                value = deserializeAny(jp, ctxt, nodeFactory);
            }
            JsonNode old = node.replace(fieldName, value);
            if (old != null) {
                _handleDuplicateField(jp, ctxt, nodeFactory,
                        fieldName, node, old, value);
            }
        }
        return node;
    }

    protected final ArrayNode deserializeArray(JsonParser jp, DeserializationContext ctxt,
            final JsonNodeFactory nodeFactory) throws IOException
    {
        ArrayNode node = nodeFactory.arrayNode();
        while (true) {
            JsonToken t = jp.nextToken();
            if (t == null) {
                throw ctxt.mappingException("Unexpected end-of-input when binding data into ArrayNode");
            }
            switch (t.id()) {
            case JsonTokenId.ID_START_OBJECT:
                node.add(deserializeObject(jp, ctxt, nodeFactory));
                break;
            case JsonTokenId.ID_START_ARRAY:
                node.add(deserializeArray(jp, ctxt, nodeFactory));
                break;
            case JsonTokenId.ID_END_ARRAY:
                return node;
            case JsonTokenId.ID_STRING:
                node.add(nodeFactory.textNode(jp.getText()));
                break;
            case JsonTokenId.ID_NUMBER_INT:
                node.add(_fromInt(jp, ctxt, nodeFactory));
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
                node.add(deserializeAny(jp, ctxt, nodeFactory));
                break;
            }
        }
    }

    protected final JsonNode deserializeAny(JsonParser jp, DeserializationContext ctxt,
            final JsonNodeFactory nodeFactory) throws IOException
    {
        switch (jp.getCurrentTokenId()) {
        case JsonTokenId.ID_START_OBJECT:
        case JsonTokenId.ID_END_OBJECT: // for empty JSON Objects we may point to this
            return deserializeObject(jp, ctxt, nodeFactory);
        case JsonTokenId.ID_START_ARRAY:
            return deserializeArray(jp, ctxt, nodeFactory);
        case JsonTokenId.ID_FIELD_NAME:
            return deserializeObject(jp, ctxt, nodeFactory);
        case JsonTokenId.ID_EMBEDDED_OBJECT:
            return _fromEmbedded(jp, ctxt, nodeFactory);
        case JsonTokenId.ID_STRING:
            return nodeFactory.textNode(jp.getText());
        case JsonTokenId.ID_NUMBER_INT:
            return _fromInt(jp, ctxt, nodeFactory);
        case JsonTokenId.ID_NUMBER_FLOAT:
            return _fromFloat(jp, ctxt, nodeFactory);
        case JsonTokenId.ID_TRUE:
            return nodeFactory.booleanNode(true);
        case JsonTokenId.ID_FALSE:
            return nodeFactory.booleanNode(false);
        case JsonTokenId.ID_NULL:
            return nodeFactory.nullNode();
            
            // These states can not be mapped; input stream is
            // off by an event or two

        //case END_OBJECT:
        //case END_ARRAY:
        default:
            throw ctxt.mappingException(handledType());
        }
    }

    protected final JsonNode _fromInt(JsonParser jp, DeserializationContext ctxt,
            JsonNodeFactory nodeFactory) throws IOException
    {
        JsonParser.NumberType nt = jp.getNumberType();
        if (nt == JsonParser.NumberType.BIG_INTEGER
            || ctxt.isEnabled(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)) {
            return nodeFactory.numberNode(jp.getBigIntegerValue());
        }
        if (nt == JsonParser.NumberType.INT) {
            return nodeFactory.numberNode(jp.getIntValue());
        }
        return nodeFactory.numberNode(jp.getLongValue());
    }

    protected final JsonNode _fromFloat(JsonParser jp, DeserializationContext ctxt,
            final JsonNodeFactory nodeFactory) throws IOException
    {
        JsonParser.NumberType nt = jp.getNumberType();
        if (nt == JsonParser.NumberType.BIG_DECIMAL
            || ctxt.isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)) {
            return nodeFactory.numberNode(jp.getDecimalValue());
        }
        return nodeFactory.numberNode(jp.getDoubleValue());
    }

    protected final JsonNode _fromEmbedded(JsonParser jp, DeserializationContext ctxt,
            JsonNodeFactory nodeFactory) throws IOException
    {
        // [JACKSON-796]
        Object ob = jp.getEmbeddedObject();
        if (ob == null) { // should this occur?
            return nodeFactory.nullNode();
        }
        Class<?> type = ob.getClass();
        if (type == byte[].class) { // most common special case
            return nodeFactory.binaryNode((byte[]) ob);
        }
        if (JsonNode.class.isAssignableFrom(type)) {
            // [Issue#433]: but could also be a JsonNode hiding in there!
            return (JsonNode) ob;
        }
        // any other special handling needed?
        return nodeFactory.pojoNode(ob);
    }
}
