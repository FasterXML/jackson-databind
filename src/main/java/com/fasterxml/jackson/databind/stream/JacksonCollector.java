package com.fasterxml.jackson.databind.stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * Class that contains collector to work with lists of {@code JsonNode}
 * in java 8 streams approach.
 */
public class JacksonCollector {

    /**
     * Builds {@code Collector} that is used to collect {@code JsonNode} objects into {@code ArrayNode}
     *
     * @return {@code Collector} implementation
     */
    public static Collector<JsonNode, ArrayNode, ArrayNode> toJsonList() {
        return new JsonListCollector();
    }

    /**
     * Default collector realization
     */
    private static class JsonListCollector implements Collector<JsonNode, ArrayNode, ArrayNode> {

        @Override
        public Supplier<ArrayNode> supplier() {
            return new ObjectMapper()::createArrayNode;
        }

        @Override
        public BiConsumer<ArrayNode, JsonNode> accumulator() {
            return ArrayNode::add;
        }

        @Override
        public BinaryOperator<ArrayNode> combiner() {
            return ArrayNode::addAll;
        }

        @Override
        public Function<ArrayNode, ArrayNode> finisher() {
            return accumulator -> accumulator;
        }

        @Override
        public Set<Characteristics> characteristics() {
            return EnumSet.of(Characteristics.UNORDERED);
        }
    }
}