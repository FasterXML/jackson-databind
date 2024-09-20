package tools.jackson.databind.util;

import java.util.stream.Collector;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeCreator;
import tools.jackson.databind.node.JsonNodeFactory;

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
     *
     * @return a {@link Collector} that collects {@link JsonNode} elements into an {@link ArrayNode}
     */
    public static Collector<JsonNode, ArrayNode, ArrayNode> toJsonNode() {
        return toJsonNode(JsonNodeFactory.instance);
    }

    public static Collector<JsonNode, ArrayNode, ArrayNode> toJsonNode(JsonNodeCreator nodeCreator) {
        return Collector.of(
                nodeCreator::arrayNode, // supplier
                ArrayNode::add, // accumulator
                ArrayNode::addAll // combiner
            );
    }
}
