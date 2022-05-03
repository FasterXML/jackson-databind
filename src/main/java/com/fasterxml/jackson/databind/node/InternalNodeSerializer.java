package com.fasterxml.jackson.databind.node;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

/**
 * Helper class used to support non-recursive serialization for use
 * by {@link BaseJsonNode#toString} (and related) method.
 */
class InternalNodeSerializer
{
    private final static JsonMapper MAPPER = JsonMapper.shared();

    private final static ObjectWriter COMPACT_WRITER = MAPPER.writer();
    private final static ObjectWriter PRETTY_WRITER = MAPPER.writerWithDefaultPrettyPrinter();
    
    private static JacksonSerializable _wrapper(BaseJsonNode root) {
        return new WrapperForSerializer(root);
    }

    public static String toString(BaseJsonNode root) {
        return COMPACT_WRITER.writeValueAsString(_wrapper(root));
    }

    public static String toPrettyString(BaseJsonNode root) {
        return PRETTY_WRITER.writeValueAsString(_wrapper(root));
    }

    /**
     * Intermediate serializer we need to implement non-recursive serialization of
     * {@link BaseJsonNode}
     */
    protected static class WrapperForSerializer
        extends JacksonSerializable.Base
    {
        protected final BaseJsonNode _root;

        public WrapperForSerializer(BaseJsonNode root) {
            _root = root;
        }

        @Override
        public void serialize(JsonGenerator gen, SerializerProvider serializers) throws JacksonException {
            // !!! TODO: placeholder
            _root.serialize(gen, serializers);
        }

        @Override
        public void serializeWithType(JsonGenerator gen, SerializerProvider serializers,
                TypeSerializer typeSer) throws JacksonException
        {
            // Should not really be called given usage, so
            serialize(gen, serializers);
        }
    }
}
