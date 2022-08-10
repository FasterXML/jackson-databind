package tools.jackson.databind.deser.jackson;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.node.*;

/**
 * Deserializer that can build instances of {@link JsonNode} from any
 * JSON content, using appropriate {@link JsonNode} type.
 *<p>
 * Rewritten in Jackson 2.13 to avoid recursion and allow handling of
 * very deeply nested structures.
 */
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

    protected JsonNodeDeserializer(JsonNodeDeserializer base,
            boolean mergeArrays, boolean mergeObjects) {
        super(base, mergeArrays, mergeObjects);
    }

    @Override
    protected BaseNodeDeserializer<?> _createWithMerge(boolean mergeArrays,
            boolean mergeObjects) {
        return new JsonNodeDeserializer(this, mergeArrays, mergeObjects);
    }

    /**
     * Factory method for accessing deserializer for specific node type
     */
    public static BaseNodeDeserializer<?> getDeserializer(Class<?> nodeClass)
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
    /**********************************************************************
    /* Actual deserialization method implementations
    /**********************************************************************
     */

    @Override
    public JsonNode getNullValue(DeserializationContext ctxt) {
        return ctxt.getNodeFactory().nullNode();
    }

    /**
     * Overridden variant to ensure that absent values are NOT coerced into
     * {@code NullNode}s, unlike incoming {@code null} values.
     */
    @Override // since 2.13
    public Object getAbsentValue(DeserializationContext ctxt) {
        return null;
    }

    /**
     * Implementation that will produce types of any JSON nodes; not just one
     * deserializer is registered to handle (in case of more specialized handler).
     * Overridden by typed sub-classes for more thorough checking
     */
    @Override
    public JsonNode deserialize(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        final ContainerStack stack = new ContainerStack();
        final JsonNodeFactory nodeF = ctxt.getNodeFactory();
        JsonToken t = p.currentToken();
        if (t == JsonToken.START_OBJECT) {
            return _deserializeContainerNoRecursion(p, ctxt, nodeF, stack, nodeF.objectNode());
        }
        if (t == JsonToken.START_ARRAY) {
            return _deserializeContainerNoRecursion(p, ctxt, nodeF, stack, nodeF.arrayNode());
        }
        switch (p.currentTokenId()) {
        case JsonTokenId.ID_END_OBJECT:
            return nodeF.objectNode();
        case JsonTokenId.ID_PROPERTY_NAME:
            return _deserializeObjectAtName(p, ctxt, nodeF, stack);
        default:
        }
        return _deserializeAnyScalar(p, ctxt);
    }

    @Override
    public Boolean supportsUpdate(DeserializationConfig config) {
        return _supportsUpdates;
    }

    /*
    /**********************************************************************
    /* Specific instances for more accurate types
    /**********************************************************************
     */

    /**
     * Implementation used when declared type is specifically {@link ObjectNode}.
     */
    final static class ObjectDeserializer
        extends BaseNodeDeserializer<ObjectNode>
    {
        protected final static ObjectDeserializer _instance = new ObjectDeserializer();

        protected ObjectDeserializer() { super(ObjectNode.class, true); }

        public static ObjectDeserializer getInstance() { return _instance; }

        protected ObjectDeserializer(ObjectDeserializer base,
                boolean mergeArrays, boolean mergeObjects) {
            super(base, mergeArrays, mergeObjects);
        }

        @Override
        protected BaseNodeDeserializer<?> _createWithMerge(boolean mergeArrays,
                boolean mergeObjects) {
            return new ObjectDeserializer(this, mergeArrays, mergeObjects);
        }

        @Override
        public ObjectNode deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException
        {
            final JsonNodeFactory nodeF = ctxt.getNodeFactory();
            if (p.isExpectedStartObjectToken()) {
                final ObjectNode root = nodeF.objectNode();
                _deserializeContainerNoRecursion(p, ctxt, nodeF, new ContainerStack(), root);
                return root;
            }
            if (p.hasToken(JsonToken.PROPERTY_NAME)) {
                return _deserializeObjectAtName(p, ctxt, nodeF, new ContainerStack());
            }
            // 23-Sep-2015, tatu: Ugh. We may also be given END_OBJECT (similar to PROPERTY_NAME),
            //    if caller has advanced to the first token of Object, but for empty Object
            if (p.hasToken(JsonToken.END_OBJECT)) {
                return nodeF.objectNode();
            }
            return (ObjectNode) ctxt.handleUnexpectedToken(getValueType(ctxt), p);
         }

        /**
         * Variant needed to support both root-level `updateValue()` and merging.
         */
        @Override
        public ObjectNode deserialize(JsonParser p, DeserializationContext ctxt,
                ObjectNode node) throws JacksonException
        {
            if (p.isExpectedStartObjectToken() || p.hasToken(JsonToken.PROPERTY_NAME)) {
                return (ObjectNode) updateObject(p, ctxt, (ObjectNode) node,
                        new ContainerStack());
            }
            return (ObjectNode) ctxt.handleUnexpectedToken(getValueType(ctxt), p);
        }
    }

    /**
     * Implementation used when declared type is specifically {@link ArrayNode}.
     */
    final static class ArrayDeserializer
        extends BaseNodeDeserializer<ArrayNode>
    {
        protected final static ArrayDeserializer _instance = new ArrayDeserializer();

        protected ArrayDeserializer() { super(ArrayNode.class, true); }

        public static ArrayDeserializer getInstance() { return _instance; }

        protected ArrayDeserializer(ArrayDeserializer base,
                boolean mergeArrays, boolean mergeObjects) {
            super(base, mergeArrays, mergeObjects);
        }

        @Override
        protected BaseNodeDeserializer<?> _createWithMerge(boolean mergeArrays,
                boolean mergeObjects) {
            return new ArrayDeserializer(this, mergeArrays, mergeObjects);
        }

        @Override
        public ArrayNode deserialize(JsonParser p, DeserializationContext ctxt)
            throws JacksonException
        {
            if (p.isExpectedStartArrayToken()) {
                final JsonNodeFactory nodeF = ctxt.getNodeFactory();
                final ArrayNode arrayNode = nodeF.arrayNode();
                _deserializeContainerNoRecursion(p, ctxt, nodeF,
                        new ContainerStack(), arrayNode);
                return arrayNode;
            }
            return (ArrayNode) ctxt.handleUnexpectedToken(getValueType(ctxt), p);
        }

        /**
         * Variant needed to support both root-level {@code updateValue()} and merging.
         */
        @Override
        public ArrayNode deserialize(JsonParser p, DeserializationContext ctxt,
                ArrayNode arrayNode) throws JacksonException
        {
            if (p.isExpectedStartArrayToken()) {
                _deserializeContainerNoRecursion(p, ctxt, ctxt.getNodeFactory(),
                        new ContainerStack(), arrayNode);
                return arrayNode;
            }
            return (ArrayNode) ctxt.handleUnexpectedToken(getValueType(ctxt), p);
        }
    }
}
