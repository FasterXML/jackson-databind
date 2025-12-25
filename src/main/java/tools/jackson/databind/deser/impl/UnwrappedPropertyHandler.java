package tools.jackson.databind.deser.impl;

import java.util.*;

import tools.jackson.core.*;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.PropertyName;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.SettableBeanProperty;
import tools.jackson.databind.deser.bean.BeanDeserializerBase;
import tools.jackson.databind.deser.bean.PropertyValueBuffer;
import tools.jackson.databind.util.NameTransformer;
import tools.jackson.databind.util.TokenBuffer;

/**
 * Object that is responsible for handling acrobatics related to
 * deserializing "unwrapped" values; sets of properties that are
 * embedded (inlined) as properties of parent JSON object.
 */
public class UnwrappedPropertyHandler
{
    /**
     * @since 2.19
     */
    public static final String JSON_UNWRAPPED_NAME_PREFIX = "@JsonUnwrapped/";

    /**
     * @since 2.19
     */
    protected final List<SettableBeanProperty> _creatorProperties;
    protected final List<SettableBeanProperty> _properties;

    /**
     * Set of all nested property names from unwrapped deserializers.
     */
    protected final Set<String> _nestedPropertyNames;

    /**
     * Flag indicating whether any unwrapped deserializer has an AnySetter,
     * which means it can handle any property name.
     */
    protected final boolean _hasNestedAnySetter;

    public UnwrappedPropertyHandler() {
        _creatorProperties = new ArrayList<>();
        _properties = new ArrayList<>();
        // placeholder: won't be modified in-place
        _nestedPropertyNames = Collections.emptySet();
        _hasNestedAnySetter = false;
    }

    protected UnwrappedPropertyHandler(List<SettableBeanProperty> creatorProps,
            List<SettableBeanProperty> props,
            Set<String> nestedPropertyNames,
            boolean hasNestedAnySetter) {
        _creatorProperties = creatorProps;
        _properties = props;
        _nestedPropertyNames = nestedPropertyNames;
        _hasNestedAnySetter = hasNestedAnySetter;
    }

    /**
     * Creates a new UnwrappedPropertyHandler with initialized nested property names cache.
     *
     * @since 3.1
     */
    public UnwrappedPropertyHandler initializedNestedPropertyNames() {
        Set<String> nestedNames = new HashSet<>();
        boolean hasAnySetter = _collectNestedPropertyNames(_properties, _creatorProperties, nestedNames);
        return new UnwrappedPropertyHandler(_creatorProperties, _properties, nestedNames, hasAnySetter);
    }

    /**
     * @since 2.19
     */
    public void addCreatorProperty(SettableBeanProperty property) {
        _creatorProperties.add(property);
    }

    public void addProperty(SettableBeanProperty property) {
        _properties.add(property);
    }

    public UnwrappedPropertyHandler renameAll(DeserializationContext ctxt,
            NameTransformer transformer)
    {
        List<SettableBeanProperty> renamedCreatorProps = renameProperties(ctxt, _creatorProperties, transformer);
        List<SettableBeanProperty> renamedProps = renameProperties(ctxt, _properties, transformer);

        // Collect nested property names and check for AnySetter
        Set<String> nestedNames = new HashSet<>();
        boolean hasAnySetter = _collectNestedPropertyNames(renamedProps, renamedCreatorProps, nestedNames);

        return new UnwrappedPropertyHandler(renamedCreatorProps, renamedProps, nestedNames, hasAnySetter);
    }

    private List<SettableBeanProperty> renameProperties(DeserializationContext ctxt,
            Collection<SettableBeanProperty> properties,
            NameTransformer transformer
    ) {
        List<SettableBeanProperty> newProps = new ArrayList<>(properties.size());
        for (SettableBeanProperty prop : properties) {
            if (prop == null) {
                newProps.add(null);
                continue;
            }

            newProps.add(prop.unwrapped(ctxt, transformer));
        }
        return newProps;
    }

    /**
     * @since 2.19
     */
    public PropertyValueBuffer processUnwrappedCreatorProperties(JsonParser originalParser,
            DeserializationContext ctxt, PropertyValueBuffer values, TokenBuffer buffered)
    {
        for (SettableBeanProperty prop : _creatorProperties) {
            JsonParser p = buffered.asParserOnFirstToken(ctxt);
            values.assignParameter(prop, prop.deserialize(p, ctxt));
        }

        return values;
    }

    @SuppressWarnings("resource")
    public Object processUnwrapped(JsonParser originalParser, DeserializationContext ctxt,
            Object bean, TokenBuffer buffered)
    {
        for (SettableBeanProperty prop : _properties) {
            JsonParser p = buffered.asParserOnFirstToken(ctxt);
            prop.deserializeAndSet(p, ctxt, bean);
        }
        return bean;
    }

    /**
     * Generates a placeholder name for creator properties that don't have a name,
     * but are marked with `@JsonUnwrapped` annotation.
     *
     * @since 2.19
     */
    public static PropertyName creatorParamName(int index) {
        return new PropertyName(JSON_UNWRAPPED_NAME_PREFIX + index);
    }

    /**
     * Method that checks if the given property name belongs to any unwrapped property.
     *
     * @return {@code true} if any nested deserializers has an "any-setter".
     *
     * @since 3.1
     */
    public boolean hasUnwrappedProperty(String propName) {
        // If any nested deserializer has AnySetter, it can handle any property
        if (_hasNestedAnySetter) {
            return true;
        }
        return _nestedPropertyNames.contains(propName);
    }

    /**
     * Collects all nested property names from unwrapped deserializers.
     *
     * @since 3.1
     */
    public void collectNestedPropertyNamesTo(Set<String> names) {
        _collectNestedPropertyNames(_properties, _creatorProperties, names);
    }

    /**
     * Helper method to collect nested property names.
     *
     * @return {@code true} if any property deserializer has AnySetter.
     *
     * @since 3.1
     */
    private boolean _collectNestedPropertyNames(List<SettableBeanProperty> properties,
            List<SettableBeanProperty> creatorProperties,
            Set<String> names) {
        boolean hasAnySetter = false;
        for (SettableBeanProperty prop : properties) {
            if (_collectDeserializerPropertyNames(prop, names)) {
                hasAnySetter = true;
            }
        }
        for (SettableBeanProperty prop : creatorProperties) {
            if (_collectDeserializerPropertyNames(prop, names)) {
                hasAnySetter = true;
            }
        }
        return hasAnySetter;
    }

    /**
     * Helper method to collect property names from a property's deserializer.
     *
     * @return {@code true} if the property deserializer has AnySetter.
     */
    private boolean _collectDeserializerPropertyNames(SettableBeanProperty prop, Set<String> names) {
        if (prop != null) {
            ValueDeserializer<?> deser = prop.getValueDeserializer();
            if (deser instanceof BeanDeserializerBase bd) {
                // Recursively collect property names
                bd.collectAllPropertyNamesTo(names);
                return bd.hasAnySetter();
            }
        }
        return false;
    }
}
