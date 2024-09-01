package com.fasterxml.jackson.databind.deser.impl;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.util.NameTransformer;
import com.fasterxml.jackson.databind.util.TokenBuffer;

/**
 * Object that is responsible for handling acrobatics related to
 * deserializing "unwrapped" values; sets of properties that are
 * embedded (inlined) as properties of parent JSON object.
 */
public class UnwrappedPropertyHandler
{
    protected final List<SettableBeanProperty> _creatorProperties;
    protected final List<SettableBeanProperty> _properties;

    public UnwrappedPropertyHandler()  {
        _creatorProperties = new ArrayList<>();
        _properties = new ArrayList<>();
    }

    protected UnwrappedPropertyHandler(List<SettableBeanProperty> creatorProps, List<SettableBeanProperty> props) {
        _creatorProperties = creatorProps;
        _properties = props;
    }

    public void addCreatorProperty(SettableBeanProperty property) {
        _creatorProperties.add(property);
    }

    public void addProperty(SettableBeanProperty property) {
        _properties.add(property);
    }

    public UnwrappedPropertyHandler renameAll(NameTransformer transformer) {
        return new UnwrappedPropertyHandler(
                renameProperties(_creatorProperties, transformer),
                renameProperties(_properties, transformer)
        );
    }

    private List<SettableBeanProperty> renameProperties(
            Collection<SettableBeanProperty> properties,
            NameTransformer transformer
    ) {
        List<SettableBeanProperty> newProps = new ArrayList<>(properties.size());
        for (SettableBeanProperty prop : properties) {
            if (prop == null) {
                newProps.add(null);
                continue;
            }

            newProps.add(prop.unwrapped(transformer));
        }
        return newProps;
    }

    public PropertyValueBuffer processUnwrappedCreatorProperties(
            JsonParser originalParser,
            DeserializationContext ctxt,
            PropertyValueBuffer values,
            TokenBuffer buffered
    ) throws IOException {
        for (SettableBeanProperty prop : _creatorProperties) {
            JsonParser p = buffered.asParser(originalParser.streamReadConstraints());
            p.nextToken();
            Object deserialized = prop.deserialize(p, ctxt);
            values.assignParameter(prop, deserialized);
        }

        return values;
    }

    public Object processUnwrapped(JsonParser originalParser, DeserializationContext ctxt,
            Object bean, TokenBuffer buffered)
        throws IOException
    {
        for (SettableBeanProperty prop : _properties) {
            JsonParser p = buffered.asParser(originalParser.streamReadConstraints());
            p.nextToken();
            prop.deserializeAndSet(p, ctxt, bean);
        }
        return bean;
    }

    /**
     * We need a placeholder for creator properties that don't have a name,
     * but are marked with `@JsonWrapped` annotation.
     */
    public static PropertyName creatorParamName(int index) {
        return new PropertyName("@JsonUnwrapped/" + index);
    }
}
