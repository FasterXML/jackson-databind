package tools.jackson.databind.deser.impl;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonPointer;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.SettableBeanProperty;
import tools.jackson.databind.deser.bean.BeanPropertyMap;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.TreeTraversingParser;

/**
 * Helper used for properties whose input values are selected with
 * {@link com.fasterxml.jackson.annotation.JsonPointer}.
 *
 * @since 3.3
 */
public final class JsonPointerPropertyHandler
{
    private final List<PointerProperty> _properties = new ArrayList<>();

    public void addProperty(JsonPointer pointer, SettableBeanProperty property) {
        _properties.add(new PointerProperty(pointer, property));
    }

    public JsonNode prepareForBeanBinding(JsonNode source, BeanPropertyMap beanProperties) {
        if (!(source instanceof ObjectNode)) {
            return source;
        }
        Set<String> pointerRoots = new LinkedHashSet<>();
        for (PointerProperty pointerProperty : _properties) {
            JsonPointer pointer = pointerProperty.pointer;
            if (pointer.matches() || !pointer.mayMatchProperty()) {
                continue;
            }
            String rootName = pointer.getMatchingProperty();
            if (beanProperties.findDefinition(rootName) == null) {
                pointerRoots.add(rootName);
            }
        }
        if (pointerRoots.isEmpty()) {
            return source;
        }
        ObjectNode sourceObject = (ObjectNode) source;
        ObjectNode filtered = sourceObject.objectNode();
        for (var entry : sourceObject.properties()) {
            if (!pointerRoots.contains(entry.getKey())) {
                filtered.set(entry.getKey(), entry.getValue());
            }
        }
        return filtered;
    }

    public void process(DeserializationContext ctxt, Object bean, JsonNode source)
            throws JacksonException {
        for (PointerProperty pointerProperty : _properties) {
            JsonNode value = source.at(pointerProperty.pointer);
            if (value.isMissingNode()) {
                continue;
            }
            try (JsonParser p = new TreeTraversingParser(value, ctxt)) {
                p.nextToken();
                try {
                    pointerProperty.property.deserializeAndSet(p, ctxt, bean);
                } catch (Exception e) {
                    throw DatabindException.wrapWithPath(ctxt, e,
                            new JacksonException.Reference(bean,
                                    pointerProperty.property.getName()));
                }
            }
        }
    }

    private static final class PointerProperty {
        final JsonPointer pointer;
        final SettableBeanProperty property;

        PointerProperty(JsonPointer pointer, SettableBeanProperty property) {
            this.pointer = pointer;
            this.property = property;
        }
    }
}
