package com.fasterxml.jackson.databind.node;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * Helper class used to implement <code>toString()</code> method for
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
    
    public static String nodeToString(JsonNode n) {
        try {
            return STD_WRITER.writeValueAsString(n);
        } catch (IOException e) { // should never occur
            throw new RuntimeException(e);
        }
    }

    public static String nodeToPrettyString(JsonNode n) {
        try {
            return PRETTY_WRITER.writeValueAsString(n);
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
}
