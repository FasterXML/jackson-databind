package com.fasterxml.jackson.databind.node;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

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

        // Non-final as passed when `serialize()` is called
        protected SerializerProvider _context;

        public WrapperForSerializer(BaseJsonNode root) {
            _root = root;
        }

        @Override
        public void serialize(JsonGenerator g, SerializerProvider ctxt) throws JacksonException
        {
            _context = ctxt;
            _serializeNonRecursive(g, _root);
        }

        @Override
        public void serializeWithType(JsonGenerator gen, SerializerProvider serializers,
                TypeSerializer typeSer) throws JacksonException
        {
            // Should not really be called given usage, so
            serialize(gen, serializers);
        }

        protected void _serializeNonRecursive(JsonGenerator g, JsonNode node)
            throws JacksonException
        {
            if (node instanceof ObjectNode) {
                g.writeStartObject(this, node.size());
                _serializeNonRecursive(g, new IteratorStack(), node.fields());
            } else if (node instanceof ArrayNode) {
                g.writeStartArray(this, node.size());
                _serializeNonRecursive(g, new IteratorStack(), node.elements());
            } else {
                node.serialize(g, _context);
            }
        }

        protected void _serializeNonRecursive(JsonGenerator g, IteratorStack stack,
                final Iterator<?> rootIterator)
            throws JacksonException
        {
            Iterator<?> currIt = rootIterator;
            while (true) {
                // First: any more elements from the current iterator?
                while (currIt.hasNext()) {
                    JsonNode value;

                    // Otherwise we do have another Map or Array element to handle
                    Object elem = currIt.next();
                    if (elem instanceof Map.Entry<?,?>) {
                        @SuppressWarnings("unchecked")
                        Map.Entry<String, JsonNode> en = (Map.Entry<String, JsonNode>) elem;
                        g.writeName(en.getKey());
                        value = en.getValue();
                    } else {
                        value = (JsonNode) elem;
                    }
                    if (value instanceof ObjectNode) {
                        stack.push(currIt);
                        currIt = value.fields();
                        g.writeStartObject(value, value.size());
                    } else if (value instanceof ArrayNode) {
                        stack.push(currIt);
                        currIt = value.elements();
                        g.writeStartArray(value, value.size());
                    } else {
                        value.serialize(g, _context);
                    }
                }
                if (g.streamWriteContext().inArray()) {
                    g.writeEndArray();
                } else {
                    g.writeEndObject();
                }
                currIt = stack.popOrNull();
                if (currIt == null) {
                    return;
                }
            }
        }
    }

    /**
     * Optimized variant similar in functionality to (a subset of)
     * {@link java.util.ArrayDeque}; used to hold enclosing Array/Object
     * nodes during recursion-as-iteration.
     */
    final static class IteratorStack
    {
        private Iterator<?>[] _stack;
        private int _top, _end;

        public IteratorStack() { }

        public void push(Iterator<?> it)
        {
            if (_top < _end) {
                _stack[_top++] = it; // lgtm [java/dereferenced-value-may-be-null]
                return;
            }
            if (_stack == null) {
                _end = 10;
                _stack = new Iterator<?>[_end];
            } else {
                // grow by 50%, for most part
                _end += Math.min(4000, Math.max(20, _end>>1));
                _stack = Arrays.copyOf(_stack, _end);
            }
            _stack[_top++] = it;
        }

        public Iterator<?> popOrNull() {
            if (_top == 0) {
                return null;
            }
            // note: could clean up stack but due to usage pattern, should not make
            // much difference since the whole stack is discarded after serialization done
            return _stack[--_top];
        }
    }
}
