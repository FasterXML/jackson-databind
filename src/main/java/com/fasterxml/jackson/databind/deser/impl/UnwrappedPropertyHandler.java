package com.fasterxml.jackson.databind.deser.impl;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.util.TokenBuffer;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;


/**
 * Object that is responsible for handling acrobatics related to
 * deserializing "unwrapped" values; sets of properties that are
 * embedded (inlined) as properties of parent JSON object.
 *
 * @since 1.9
 */
public class UnwrappedPropertyHandler
{
    protected final ArrayList<SettableBeanProperty> _properties = new ArrayList<SettableBeanProperty>();
    
    public UnwrappedPropertyHandler()  { }

    public void addProperty(SettableBeanProperty property) {
        _properties.add(property);
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
