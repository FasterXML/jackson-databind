package com.fasterxml.jackson.databind.node;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.*;

import static com.fasterxml.jackson.core.JsonToken.*;

class NonRecursiveSerializer {
    
    private static class ContainerNodeWrapper {
        private final ContainerNode<?> node;
        private List<Object> events;

        // does this container have nested objects or arrays?
        private boolean nested = true;

        ContainerNodeWrapper(final ContainerNode<?> node) {
            this.node = node;   
        }

        ContainerNode<?> getNode() {
            return node;
        }

        List<Object> getEvents() {
            return events;
        }

        void setEvents(List<Object> events) {
            this.events = events;
        }

        public boolean isNested() {
            return nested;
        }

        public void setNested(boolean nested) {
            this.nested = nested;
        }
    }

    static void serialize(final JsonNode node, final JsonGenerator gen, final SerializerProvider provider)
            throws IOException {
        if (node instanceof ArrayNode) {
            List<Object> events = new ArrayList<>();
            events.add(START_ARRAY);
            events.addAll(getDescendentNodes(node, provider));
            events.add(END_ARRAY);
            serializeList(events, gen, provider);
        } else if (node instanceof ObjectNode) {
            List<Object> events = new ArrayList<>();
            events.add(START_OBJECT);
            Map<String, JsonNode> nodeMap = ((ObjectNode) node)._contentsToSerialize(provider);
            events.addAll(getDescendentNodes(nodeMap, provider));
            events.add(END_OBJECT);
            serializeList(events, gen, provider);
        } else {
            node.serialize(gen, provider);
        }
    }

    private static void serializeList(List<Object> events, final JsonGenerator gen, final SerializerProvider provider)
            throws IOException {
        for (Object ev : events) {
            if (ev == START_ARRAY) {
                gen.writeStartArray();
            } else if (ev == END_ARRAY) {
                gen.writeEndArray();
            } else if (ev == START_OBJECT) {
                gen.writeStartObject();
            } else if (ev == END_OBJECT) {
                gen.writeEndObject();
            } else if (ev instanceof String) {
                gen.writeFieldName((String) ev);
            } else if (ev instanceof JsonNode) {
                ((JsonNode) ev).serialize(gen, provider);
            } else if (ev instanceof ContainerNodeWrapper) {
                serializeList(((ContainerNodeWrapper) ev).getEvents(), gen, provider);
            }
        }
    }

    private static List<Object> getDescendentNodes(final JsonNode node, final SerializerProvider provider) {
        final LinkedList<ContainerNodeWrapper> containerNodeWrappers = new LinkedList<>();
        final List<Object> events = new ArrayList<>();
        final Stack<ContainerNodeWrapper> stack = new Stack<>();
        final Iterator<JsonNode> iterator = node.elements();
        while (iterator.hasNext()) {
            JsonNode child = iterator.next();
            if (child instanceof ContainerNode<?>) {
                ContainerNodeWrapper wrapper = new ContainerNodeWrapper((ContainerNode<?>) child);
                containerNodeWrappers.add(wrapper);
                stack.add(wrapper);
                events.add(wrapper);
            } else {
                events.add(child);
            }
        }

        if (!containerNodeWrappers.isEmpty()) {
            while (!stack.isEmpty()) {
                ContainerNodeWrapper containerNodeWrapper = stack.pop();
                ContainerNode<?> containerNode = containerNodeWrapper.getNode();
                List<Object> childEvents = new ArrayList<>();
                containerNodeWrapper.setEvents(childEvents);
                boolean nested = false;
                if (containerNode instanceof ArrayNode) {
                    childEvents.add(START_ARRAY);
                    nested = processChildNode(containerNode, stack, containerNodeWrappers, childEvents);
                    events.add(END_ARRAY);
                } else if (containerNode instanceof ObjectNode) {
                    childEvents.add(START_OBJECT);
                    Map<String, JsonNode> nodeMap = ((ObjectNode) containerNode)._contentsToSerialize(provider);
                    nested = processNodeMap(nodeMap, stack, containerNodeWrappers, childEvents);
                    events.add(END_OBJECT);
                }
                containerNodeWrapper.setNested(nested);
            }
            expandContainers(containerNodeWrappers);
        }
        return events;
    }

    private static List<Object> getDescendentNodes(final Map<String, JsonNode> nodeMap,
                                                   final SerializerProvider provider) {
        final LinkedList<ContainerNodeWrapper> containerNodeWrappers = new LinkedList<>();
        final List<Object> events = new ArrayList<>();
        final Stack<ContainerNodeWrapper> stack = new Stack<>();
        final Iterator<Map.Entry<String, JsonNode>> iterator = nodeMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> childEntry = iterator.next();
            events.add(childEntry.getKey());
            JsonNode child = childEntry.getValue();
            if (child instanceof ContainerNode<?>) {
                ContainerNodeWrapper wrapper = new ContainerNodeWrapper((ContainerNode<?>) child);
                containerNodeWrappers.add(wrapper);
                stack.add(wrapper);
                events.add(wrapper);
            } else {
                events.add(child);
            }
        }

        if (!containerNodeWrappers.isEmpty()) {
            while (!stack.isEmpty()) {
                ContainerNodeWrapper containerNodeWrapper = stack.pop();
                ContainerNode<?> containerNode = containerNodeWrapper.getNode();
                List<Object> childEvents = new ArrayList<>();
                containerNodeWrapper.setEvents(childEvents);
                boolean nested = false;
                if (containerNode instanceof ArrayNode) {
                    childEvents.add(START_ARRAY);
                    nested = processChildNode(containerNode, stack, containerNodeWrappers, childEvents);
                    childEvents.add(END_ARRAY);
                } else if (containerNode instanceof ObjectNode) {
                    childEvents.add(START_OBJECT);
                    Map<String, JsonNode> nodeMap2 = ((ObjectNode) containerNode)._contentsToSerialize(provider);
                    nested = processNodeMap(nodeMap2, stack, containerNodeWrappers, childEvents);
                    childEvents.add(END_OBJECT);
                }
                containerNodeWrapper.setNested(nested);
            }
            expandContainers(containerNodeWrappers);
        }
        return events;
    }

    private static void expandContainers(LinkedList<ContainerNodeWrapper> wrappers) {
        while (!wrappers.isEmpty()) {
            ContainerNodeWrapper wrapper = wrappers.removeLast();
            if (wrapper.isNested()) {
                boolean needsWork = false;
                List<Object> newEvents = new ArrayList<>();
                for (Object e : wrapper.getEvents()) {
                    if (e instanceof ContainerNodeWrapper) {
                        ContainerNodeWrapper childWrapper = ((ContainerNodeWrapper) e);
                        List<Object> childEvents = childWrapper.getEvents();
                        newEvents.addAll(childEvents);
                        if (childWrapper.isNested()) {
                            if (hasContainer(childEvents)) {
                                needsWork = true;
                            } else {
                                childWrapper.setNested(false);
                            }
                        }
                    } else {
                        newEvents.add(e);
                    }
                }
                wrapper.setEvents(newEvents);
                wrapper.setNested(needsWork);
                if (needsWork) wrappers.addFirst(wrapper);
            }
        }
    }

    private static boolean hasContainer(Collection<Object> containerEvents) {
        for (Object e : containerEvents) {
            if (e instanceof ContainerNodeWrapper) {
                return true;
            }
        }
        return false;
    }

    private static boolean processChildNode(JsonNode node, Stack<ContainerNodeWrapper> stack,
                                            LinkedList<ContainerNodeWrapper> wrappers,
                                            List<Object> childEvents) {
        boolean nested = false;
        Iterator<JsonNode> childIterator = node.elements();
        while (childIterator.hasNext()) {
            JsonNode child = childIterator.next();
            if (child instanceof ContainerNode<?>) {
                ContainerNodeWrapper wrapper = new ContainerNodeWrapper((ContainerNode<?>) child);
                wrappers.add(wrapper);
                childEvents.add(wrapper);
                stack.add(wrapper);
                nested = true;
            } else {
                childEvents.add(child);
            }
        }
        return nested;
    }

    private static boolean processNodeMap(Map<String, JsonNode> nodeMap, Stack<ContainerNodeWrapper> stack,
                                          LinkedList<ContainerNodeWrapper> wrappers,
                                          List<Object> childEvents) {
        boolean nested = false;
        Iterator<Map.Entry<String, JsonNode>> childIterator = nodeMap.entrySet().iterator();
        while (childIterator.hasNext()) {
            Map.Entry<String, JsonNode> childEntry = childIterator.next();
            JsonNode child = childEntry.getValue();
            childEvents.add(childEntry.getKey());
            if (child instanceof ContainerNode<?>) {
                ContainerNodeWrapper wrapper = new ContainerNodeWrapper((ContainerNode<?>) child);
                wrappers.add(wrapper);
                childEvents.add(wrapper);
                stack.add(wrapper);
                nested = true;
            } else {
                childEvents.add(child);
            }
        }
        return nested;
    }
}
