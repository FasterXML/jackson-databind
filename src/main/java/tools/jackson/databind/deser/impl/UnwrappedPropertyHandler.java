package tools.jackson.databind.deser.impl;

import java.util.*;

import tools.jackson.core.*;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.PropertyName;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.SettableBeanProperty;
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
     * Set of all unwrapped property names from unwrapped deserializers.
     *
     * @since 3.1
     */
    protected final Set<String> _unwrappedPropertyNames;

    /**
     * Flag that indicates that we cannot tell from property names alone whether an
     * incoming property is "unwrapped": either some unwrapped deserializer has an
     * "any setter" (see {@link com.fasterxml.jackson.annotation.JsonAnySetter}), or
     * is "opaque" -- declares no property names (via
     * {@link ValueDeserializer#collectAllPropertyNamesTo}), as is the case for custom
     * unwrapping deserializers that capture arbitrary fields. When set, all
     * otherwise-unrecognized properties are routed to the unwrapped deserializers --
     * as was the case before [databind#650].
     *
     * @since 3.1
     */
    protected final boolean _acceptsAllUnwrapped;

    public UnwrappedPropertyHandler() {
        _creatorProperties = new ArrayList<>();
        _properties = new ArrayList<>();
        // placeholder: won't be modified in-place
        _unwrappedPropertyNames = Collections.emptySet();
        _acceptsAllUnwrapped = false;
    }

    protected UnwrappedPropertyHandler(List<SettableBeanProperty> creatorProps,
            List<SettableBeanProperty> props,
            Set<String> unwrappedPropertyNames,
            boolean acceptsAllUnwrapped) {
        _creatorProperties = creatorProps;
        _properties = props;
        _unwrappedPropertyNames = unwrappedPropertyNames;
        _acceptsAllUnwrapped = acceptsAllUnwrapped;
    }

    /**
     * Creates a new UnwrappedPropertyHandler with initialized unwrapped property names cache.
     *
     * @since 3.1
     */
    public UnwrappedPropertyHandler initializeUnwrappedPropertyNames() {
        Set<String> unwrappedNames = new HashSet<>();
        boolean acceptsAll = _collectUnwrappedPropertyNames(_properties, _creatorProperties, unwrappedNames);
        return new UnwrappedPropertyHandler(_creatorProperties, _properties, unwrappedNames, acceptsAll);
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

        // Collect unwrapped property names and check whether we must accept all unknowns
        Set<String> names = new HashSet<>();
        boolean acceptsAll = _collectUnwrappedPropertyNames(renamedProps, renamedCreatorProps, names);

        return new UnwrappedPropertyHandler(renamedCreatorProps, renamedProps, names, acceptsAll);
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
        // [databind#5971]: honor active @JsonView -- skip creator properties not
        // visible in the active view rather than populating them from buffered input.
        final Class<?> activeView = ctxt.getActiveView();
        for (SettableBeanProperty prop : _creatorProperties) {
            if ((activeView != null) && !prop.visibleInView(activeView)) {
                continue;
            }
            JsonParser p = buffered.asParserOnFirstToken(ctxt);
            values.assignParameter(prop, prop.deserialize(p, ctxt));
        }

        return values;
    }

    /**
     * Processes unwrapped properties from the buffered token stream.
     *
     * @param originalParser Parser from which input was originally read
     * @param ctxt Deserialization context
     * @param bean the target value object
     * @param buffered the token buffer containing the JSON tokens to deserialize
     *
     * @return the bean with unwrapped properties set
     *
     * @since 3.1
     */
    public Object processUnwrapped(JsonParser originalParser, DeserializationContext ctxt,
            Object bean, TokenBuffer buffered, boolean hasUnwrappedContent)
    {
        if (hasUnwrappedContent
                || _unwrappedPropertyNames.isEmpty()
                // [databind#1709]: Skip deserialization if no unwrapped content.
                || !ctxt.isEnabled(DeserializationFeature.USE_NULL_FOR_EMPTY_UNWRAPPED)) {
            // [databind#6060]: honor active @JsonView -- skip Field/Setter properties not
            // visible in the active view rather than populating them from buffered input.
            final Class<?> activeView = ctxt.getActiveView();
            for (SettableBeanProperty prop : _properties) {
                if ((activeView != null) && !prop.visibleInView(activeView)) {
                    continue;
                }
                try (JsonParser p = buffered.asParserOnFirstToken(ctxt)) {
                    prop.deserializeAndSet(p, ctxt, bean);
                }
            }
        }
        return bean;
    }

    // !!! TODO: remove from 3.2 or later (internal API)
    /**
     * @deprecated Since 3.1 use {@link #processUnwrapped(JsonParser, DeserializationContext, Object, TokenBuffer, boolean)}
     */
    @Deprecated // @since 3.1
    public Object processUnwrapped(JsonParser originalParser, DeserializationContext ctxt,
            Object bean, TokenBuffer buffered) {
        return processUnwrapped(originalParser, ctxt, bean, buffered, true);
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
     * @param propName Property name to check
     * @return {@code true} if name is recognized by an unwrapped deserializer
     *    (or if any of them has "any setter")
     *
     * @since 3.1
     */
    public boolean hasUnwrappedProperty(String propName) {
        // [databind#6001]: an "any setter" or opaque (non-introspectable) unwrapper
        //   means we cannot tell which properties are unwrapped, so accept all.
        if (_acceptsAllUnwrapped) {
            return true;
        }
        return _unwrappedPropertyNames.contains(propName);
    }

    /**
     * Method for collecting property names recognized by unwrapped deserializers.
     *
     * @since 3.1
     */
    public void collectUnwrappedPropertyNamesTo(Set<String> names) {
        _collectUnwrappedPropertyNames(_properties, _creatorProperties, names);
    }

    /**
     * Helper method to collect unwrapped property names.
     *
     * @return {@code true} if all otherwise-unrecognized properties must be accepted
     *    as unwrapped (some deserializer has an "any setter" or is "opaque").
     *
     * @since 3.1
     */
    private boolean _collectUnwrappedPropertyNames(List<SettableBeanProperty> properties,
            List<SettableBeanProperty> creatorProperties,
            Set<String> names) {
        boolean acceptsAll = false;
        for (SettableBeanProperty prop : properties) {
            acceptsAll |= _collectDeserializerPropertyNames(prop, names);
        }
        for (SettableBeanProperty prop : creatorProperties) {
            acceptsAll |= _collectDeserializerPropertyNames(prop, names);
        }
        return acceptsAll;
    }

    /**
     * Helper method to collect property names from a property's deserializer.
     *
     * @return {@code true} if the deserializer has an "any setter" or is "opaque"
     *    (declares no property names), in which case all otherwise-unrecognized
     *    properties must be routed to it (pre-#650 behavior).
     *
     * @since 3.1
     */
    private boolean _collectDeserializerPropertyNames(SettableBeanProperty prop,
            Set<String> names)
    {
        if (prop == null) {
            return false;
        }
        ValueDeserializer<?> deser = prop.getValueDeserializer();
        if (deser == null) {
            return false;
        }
        if (deser.hasAnySetter()) {
            return true;
        }
        // [databind#6001]: collect into a temp set so we can tell whether this
        //   deserializer contributed any names of its own; if not, it is "opaque"
        //   (typically a custom unwrapping deserializer capturing arbitrary fields).
        Set<String> propNames = new HashSet<>();
        deser.collectAllPropertyNamesTo(propNames);
        names.addAll(propNames);
        return propNames.isEmpty();
    }
}
