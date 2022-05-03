package com.fasterxml.jackson.databind.node;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

/**
 * Helper class used to implement {@code toString()} method for
 * {@link BaseJsonNode}, by embedding a private instance of
 * {@link JsonMapper}, only to be used for node serialization.
 *
 * @since 2.10 (but not to be included in 3.0)
 */
final class InternalNodeMapper {
    private final static JsonMapper JSON_MAPPER = new JsonMapper();

    private final static ObjectWriter STD_WRITER = JSON_MAPPER.writer();
    private final static ObjectWriter PRETTY_WRITER = JSON_MAPPER.writer()
            .withDefaultPrettyPrinter();

    private final static ObjectReader NODE_READER = JSON_MAPPER.readerFor(JsonNode.class);

    // // // Methods for `JsonNode.toString()` and `JsonNode.toPrettyString()`
    
    public static String nodeToString(BaseJsonNode n) {
        try {
            return STD_WRITER.writeValueAsString(_wrapper(n));
        } catch (IOException e) { // should never occur
            throw new RuntimeException(e);
        }
    }

    public static String nodeToPrettyString(BaseJsonNode n) {
        try {
            return PRETTY_WRITER.writeValueAsString(_wrapper(n));
        } catch (IOException e) { // should never occur
            throw new RuntimeException(e);
        }
    }

    // // // Methods for JDK serialization support of JsonNodes
    
    public static byte[] valueToBytes(Object value) throws IOException {
        return JSON_MAPPER.writeValueAsBytes(value);
    }

    public static JsonNode bytesToNode(byte[] json) throws IOException {
        return NODE_READER.readValue(json);
    }

    private static JsonSerializable _wrapper(BaseJsonNode root) {
        return new WrapperForSerializer(root);
    }

    /**
     * Intermediate serializer we need to implement non-recursive serialization of
     * {@link BaseJsonNode}
     *
     * @since 2.14
     */
    protected static class WrapperForSerializer
        extends JsonSerializable.Base
    {
        protected final BaseJsonNode _root;

        public WrapperForSerializer(BaseJsonNode root) {
            _root = root;
        }

        @Override
        public void serialize(JsonGenerator gen, SerializerProvider serializers) throws IOException {
            // !!! TODO: placeholder
            _root.serialize(gen, serializers);
        }

        @Override
        public void serializeWithType(JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer)
            throws IOException
        {
            // Should not really be called given usage, so
            serialize(gen, serializers);
        }
    }
}
