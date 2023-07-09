package com.fasterxml.jackson.databind.node;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

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

    static void serialize(final JsonNode node, final JsonGenerator gen, final SerializerProvider provider,
                          final TypeSerializer typeSer, final boolean trimEmptyArray, final boolean skipNulls)
            throws IOException {
        if (node.isArray()) {
            final List<Object> events = new ArrayList<>();
            events.add(START_ARRAY);
            events.addAll(getDescendentNodes(node, provider, trimEmptyArray, skipNulls));
            events.add(END_ARRAY);
            serializeList(node, events, gen, provider, typeSer);
        } else if (node.isObject()) {
            final List<Object> events = new ArrayList<>();
            events.add(START_OBJECT);
            Map<String, JsonNode> nodeMap = ((ObjectNode) node)._contentsToSerialize(provider);
            events.addAll(getDescendentNodes(nodeMap, provider, trimEmptyArray, skipNulls));
            events.add(END_OBJECT);
            serializeList(node, events, gen, provider, typeSer);
        } else {
            node.serialize(gen, provider);
        }
    }

    private static void serializeList(final JsonNode rootNode, final List<Object> events,
                                      final JsonGenerator gen, final SerializerProvider provider,
                                      final TypeSerializer typeSer)
            throws IOException {
        final int size = events.size();
        WritableTypeId typeIdDef = null;
        Object ev;
        for (int i = 0; i < size; i++) {
            ev = events.get(i);
            if (ev == START_ARRAY) {
                if (typeSer != null && i == 0) {
                    typeIdDef = typeSer.writeTypePrefix(gen, typeSer.typeId(rootNode, START_ARRAY));
                } else {
                    gen.writeStartArray();
                }
            } else if (ev == END_ARRAY) {
                if (typeIdDef != null && i == (size - 1)) {
                    typeSer.writeTypeSuffix(gen, typeIdDef);
                } else {
                    gen.writeEndArray();
                }
            } else if (ev == START_OBJECT) {
                if (typeSer != null && i == 0) {
                    typeIdDef = typeSer.writeTypePrefix(gen, typeSer.typeId(rootNode, START_OBJECT));
                } else {
                    gen.writeStartObject();
                }
            } else if (ev == END_OBJECT) {
                if (typeIdDef != null && i == (size - 1)) {
                    typeSer.writeTypeSuffix(gen, typeIdDef);
                } else {
                    gen.writeEndObject();
                }
            } else if (ev instanceof String) {
                gen.writeFieldName((String) ev);
            } else if (ev instanceof JsonNode) {
                ((JsonNode) ev).serialize(gen, provider);
            } else if (ev instanceof ContainerNodeWrapper) {
                serializeList(rootNode, ((ContainerNodeWrapper) ev).getEvents(), gen, provider, null);
            }
        }
    }

    private static List<Object> getDescendentNodes(final JsonNode node, final SerializerProvider provider,
                                                   final boolean trimEmptyArray, final boolean skipNulls) {
        final LinkedList<ContainerNodeWrapper> containerNodeWrappers = new LinkedList<>();
        final List<Object> events = new ArrayList<>();
        final Stack<ContainerNodeWrapper> stack = new Stack<>();
        final Iterator<JsonNode> iterator = node.elements();
        while (iterator.hasNext()) {
            JsonNode child = iterator.next();
            if (child.isObject() || child.isArray()) {
                if (!(trimEmptyArray && child.isArray() && child.isEmpty(provider))) {
                    ContainerNodeWrapper wrapper = new ContainerNodeWrapper((ContainerNode<?>) child);
                    containerNodeWrappers.add(wrapper);
                    stack.add(wrapper);
                    events.add(wrapper);
                }
            } else {
                events.add(child);
            }
        }
        expandChildNodes(provider, trimEmptyArray, skipNulls, containerNodeWrappers, stack);
        return events;
    }

    private static List<Object> getDescendentNodes(final Map<String, JsonNode> nodeMap,
                                                   final SerializerProvider provider,
                                                   final boolean trimEmptyArray, final boolean skipNulls) {
        final LinkedList<ContainerNodeWrapper> containerNodeWrappers = new LinkedList<>();
        final List<Object> events = new ArrayList<>();
        final Stack<ContainerNodeWrapper> stack = new Stack<>();
        final Iterator<Map.Entry<String, JsonNode>> iterator = nodeMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> childEntry = iterator.next();
            JsonNode child = childEntry.getValue();
            if (child.isObject() || child.isArray()) {
                if (!(trimEmptyArray && child.isArray() && child.isEmpty(provider))) {
                    events.add(childEntry.getKey());
                    ContainerNodeWrapper wrapper = new ContainerNodeWrapper((ContainerNode<?>) child);
                    containerNodeWrappers.add(wrapper);
                    stack.add(wrapper);
                    events.add(wrapper);
                }
            } else if (!(skipNulls && child.isNull())) {
                events.add(childEntry.getKey());
                events.add(child);
            }
        }
        expandChildNodes(provider, trimEmptyArray, skipNulls, containerNodeWrappers, stack);
        return events;
    }

    private static void expandChildNodes(final SerializerProvider provider,
                                         final boolean trimEmptyArray, final boolean skipNulls,
                                         final LinkedList<ContainerNodeWrapper> containerNodeWrappers,
                                         final Stack<ContainerNodeWrapper> stack) {
        if (!containerNodeWrappers.isEmpty()) {
            ContainerNodeWrapper containerNodeWrapper;
            ContainerNode<?> containerNode;
            while (!stack.isEmpty()) {
                containerNodeWrapper = stack.pop();
                containerNode = containerNodeWrapper.getNode();
                final List<Object> childEvents = new ArrayList<>();
                containerNodeWrapper.setEvents(childEvents);
                boolean nested = false;
                if (containerNode.isArray()) {
                    if (!(trimEmptyArray && containerNode.isEmpty(provider))) {
                        childEvents.add(START_ARRAY);
                        nested = processChildNode(containerNode, stack, containerNodeWrappers, childEvents,
                                provider, trimEmptyArray, skipNulls);
                        childEvents.add(END_ARRAY);
                    }
                } else if (containerNode.isObject()) {
                    childEvents.add(START_OBJECT);
                    Map<String, JsonNode> nodeMap2 = ((ObjectNode) containerNode)._contentsToSerialize(provider);
                    nested = processNodeMap(nodeMap2, stack, containerNodeWrappers, childEvents,
                            provider, trimEmptyArray, skipNulls);
                    childEvents.add(END_OBJECT);
                }
                containerNodeWrapper.setNested(nested);
            }
            expandContainers(containerNodeWrappers);
        }
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

    private static boolean processChildNode(final JsonNode node,
                                            final Stack<ContainerNodeWrapper> stack,
                                            final LinkedList<ContainerNodeWrapper> wrappers,
                                            final List<Object> childEvents,
                                            final SerializerProvider provider,
                                            final boolean trimEmptyArray, final boolean skipNulls) {
        boolean nested = false;
        Iterator<JsonNode> childIterator = node.elements();
        while (childIterator.hasNext()) {
            JsonNode child = childIterator.next();
            if (child.isObject() || child.isArray()) {
                if (!(trimEmptyArray && child.isArray() && child.isEmpty(provider))) {
                    ContainerNodeWrapper wrapper = new ContainerNodeWrapper((ContainerNode<?>) child);
                    wrappers.add(wrapper);
                    childEvents.add(wrapper);
                    stack.add(wrapper);
                    nested = true;
                }
            } else if (!(skipNulls && child.isNull())) {
                childEvents.add(child);
            }
        }
        return nested;
    }

    private static boolean processNodeMap(final Map<String, JsonNode> nodeMap,
                                          final Stack<ContainerNodeWrapper> stack,
                                          final LinkedList<ContainerNodeWrapper> wrappers,
                                          final List<Object> childEvents,
                                          final SerializerProvider provider,
                                          final boolean trimEmptyArray, final boolean skipNulls) {
        boolean nested = false;
        Iterator<Map.Entry<String, JsonNode>> childIterator = nodeMap.entrySet().iterator();
        while (childIterator.hasNext()) {
            Map.Entry<String, JsonNode> childEntry = childIterator.next();
            JsonNode child = childEntry.getValue();
            if (child.isObject() || child.isArray()) {
                if (!(trimEmptyArray && child.isArray() && child.isEmpty(provider))) {
                    childEvents.add(childEntry.getKey());
                    ContainerNodeWrapper wrapper = new ContainerNodeWrapper((ContainerNode<?>) child);
                    wrappers.add(wrapper);
                    childEvents.add(wrapper);
                    stack.add(wrapper);
                    nested = true;
                }
            } else if (!(skipNulls && child.isNull())) {
                childEvents.add(childEntry.getKey());
                childEvents.add(child);
            }
        }
        return nested;
    }
}
