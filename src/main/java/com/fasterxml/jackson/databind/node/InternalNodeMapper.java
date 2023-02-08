package com.fasterxml.jackson.databind.node;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

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
     * {@link BaseJsonNode}.
     *<p>
     * NOTE: not designed as thread-safe; instances must NOT be shared or reused.
     *
     * @since 2.14
     */
    protected static class WrapperForSerializer
        extends JsonSerializable.Base
    {
        protected final BaseJsonNode _root;

        // Non-final as passed when `serialize()` is called
        protected SerializerProvider _context;

        public WrapperForSerializer(BaseJsonNode root) {
            _root = root;
        }

        @Override
        public void serialize(JsonGenerator g, SerializerProvider ctxt) throws IOException {
            _context = ctxt;
            _serializeNonRecursive(g, _root);
        }

        @Override
        public void serializeWithType(JsonGenerator g, SerializerProvider ctxt, TypeSerializer typeSer)
            throws IOException
        {
            // Should not really be called given usage, so
            serialize(g, ctxt);
        }

        protected void _serializeNonRecursive(JsonGenerator g, JsonNode node) throws IOException
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
            throws IOException
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
                        g.writeFieldName(en.getKey());
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
                    } else if (value instanceof POJONode) {
                        // [databind#3262] Problematic case, try to handle
                        try {
                            value.serialize(g, _context);
                        } catch (IOException | RuntimeException e) {
                            g.writeString(String.format("[ERROR: (%s) %s]",
                                    e.getClass().getName(), e.getMessage()));
                        }
                    } else {
                        value.serialize(g, _context);
                    }
                }
                if (g.getOutputContext().inArray()) {
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
