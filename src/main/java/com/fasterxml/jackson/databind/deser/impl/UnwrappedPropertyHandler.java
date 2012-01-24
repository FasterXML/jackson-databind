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
    protected final ArrayList<SettableBeanProperty> _properties = new ArrayList<SettableBeanProperty>();
    
    public UnwrappedPropertyHandler()  { }

    public void addProperty(SettableBeanProperty property) {
        _properties.add(property);
    }

    public void renameAll(NameTransformer transformer)
    {
        ArrayList<SettableBeanProperty> oldProps = new ArrayList<SettableBeanProperty>(_properties);
        Iterator<SettableBeanProperty> it = oldProps.iterator();
        _properties.clear();
        while (it.hasNext()) {
            SettableBeanProperty prop = it.next();
            String newName = transformer.transform(prop.getName());
            prop = prop.withName(newName);
            JsonDeserializer<?> deser = prop.getValueDeserializer();
            if (deser != null) {
                @SuppressWarnings("unchecked")
                JsonDeserializer<Object> newDeser = (JsonDeserializer<Object>)
                    deser.unwrappingDeserializer(transformer);
                if (newDeser != deser) {
                    prop = prop.withValueDeserializer(newDeser);
                }
            }
            _properties.add(prop);
        }
    }
    
    public Object processUnwrapped(JsonParser originalParser, DeserializationContext ctxt, Object bean,
            TokenBuffer buffered)
        throws IOException, JsonProcessingException
    {
        for (int i = 0, len = _properties.size(); i < len; ++i) {
            SettableBeanProperty prop = _properties.get(i);
            JsonParser jp = buffered.asParser();
            jp.nextToken();
            prop.deserializeAndSet(jp, ctxt, bean);
        }
        return bean;
    }
}
