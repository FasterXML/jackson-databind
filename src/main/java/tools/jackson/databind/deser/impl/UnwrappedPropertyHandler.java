package tools.jackson.databind.deser.impl;

import java.util.*;

import tools.jackson.core.*;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.PropertyName;
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

    public UnwrappedPropertyHandler()  {
        _creatorProperties = new ArrayList<>();
        _properties = new ArrayList<>();
    }

    protected UnwrappedPropertyHandler(List<SettableBeanProperty> creatorProps,
            List<SettableBeanProperty> props) {
        _creatorProperties = creatorProps;
        _properties = props;
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
        return new UnwrappedPropertyHandler(
                renameProperties(ctxt,_creatorProperties, transformer),
                renameProperties(ctxt, _properties, transformer)
        );
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
     * but are marked with `@JsonWrapped` annotation.
     *
     * @since 2.19
     */
    public static PropertyName creatorParamName(int index) {
        return new PropertyName(JSON_UNWRAPPED_NAME_PREFIX + index);
    }
}
