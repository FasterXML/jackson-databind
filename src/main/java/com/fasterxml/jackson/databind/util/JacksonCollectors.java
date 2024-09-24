package com.fasterxml.jackson.databind.util;

import java.util.stream.Collector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeCreator;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

/**
 * Utility class that provides custom {@link Collector} implementations to support Stream operations.
 * <p>
 * This class is not meant to be instantiated and serves only as a utility class.
 * </p>
 *
 * @since 2.18
 */
public abstract class JacksonCollectors {
    /**
     * Creates a {@link Collector} that collects {@link JsonNode} elements into an {@link ArrayNode}.
     * <p>
     * This method uses a {@link JsonNodeFactory} to create an empty {@link ArrayNode} and then adds each
     * {@link JsonNode} to it.
     * </p>
     *<p>
     * Short-cut to
     *{@code toArrayNode(JsonNodeFactory.instance}}
     *
     * @return a {@link Collector} that collects {@link JsonNode} elements into an {@link ArrayNode}
     */
    public static Collector<JsonNode, ArrayNode, ArrayNode> toArrayNode() {
        return toArrayNode(JsonNodeFactory.instance);
    }

    /**
     * Creates a {@link Collector} that collects {@link JsonNode} elements into an {@link ArrayNode}.
     * <p>
     * This method uses a {@link JsonNodeFactory} to create an empty {@link ArrayNode} and then adds each
     * {@link JsonNode} to it.
     * </p>
     *
     * @param nodeCreator Factory for constructing {@link ArrayNode} to contain nodes in
     *
     * @return a {@link Collector} that collects {@link JsonNode} elements into an {@link ArrayNode}
     */
    public static Collector<JsonNode, ArrayNode, ArrayNode> toArrayNode(JsonNodeCreator nodeCreator) {
        return Collector.of(
                nodeCreator::arrayNode, // supplier
                ArrayNode::add, // accumulator
                ArrayNode::addAll // combiner
            );
    }
}
