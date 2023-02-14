package com.fasterxml.jackson.databind.deser.impl;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
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
    protected final List<SettableBeanProperty> _properties;

    public UnwrappedPropertyHandler()  {
        _properties = new ArrayList<SettableBeanProperty>();
   }
    protected UnwrappedPropertyHandler(List<SettableBeanProperty> props)  {
        _properties = props;
    }

    public void addProperty(SettableBeanProperty property) {
        _properties.add(property);
    }

    public UnwrappedPropertyHandler renameAll(NameTransformer transformer)
    {
        ArrayList<SettableBeanProperty> newProps = new ArrayList<SettableBeanProperty>(_properties.size());
        for (SettableBeanProperty prop : _properties) {
            String newName = transformer.transform(prop.getName());
            prop = prop.withSimpleName(newName);
            JsonDeserializer<?> deser = prop.getValueDeserializer();
            if (deser != null) {
                @SuppressWarnings("unchecked")
                JsonDeserializer<Object> newDeser = (JsonDeserializer<Object>)
                    deser.unwrappingDeserializer(transformer);
                if (newDeser != deser) {
                    prop = prop.withValueDeserializer(newDeser);
                }
            }
            newProps.add(prop);
        }
        return new UnwrappedPropertyHandler(newProps);
    }

    @SuppressWarnings("resource")
    public Object processUnwrapped(JsonParser originalParser, DeserializationContext ctxt,
            Object bean, TokenBuffer buffered)
        throws IOException
    {
        for (int i = 0, len = _properties.size(); i < len; ++i) {
            SettableBeanProperty prop = _properties.get(i);
            JsonParser p = buffered.asParser(originalParser.streamReadConstraints());
            p.nextToken();
            prop.deserializeAndSet(p, ctxt, bean);
        }
        return bean;
    }
}
