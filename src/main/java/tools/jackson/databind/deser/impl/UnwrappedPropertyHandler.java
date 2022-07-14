package tools.jackson.databind.deser.impl;

import java.util.*;

import tools.jackson.core.*;
import tools.jackson.core.util.InternCache;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.SettableBeanProperty;
import tools.jackson.databind.util.NameTransformer;
import tools.jackson.databind.util.TokenBuffer;

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

    public UnwrappedPropertyHandler renameAll(DeserializationContext ctxt,
            NameTransformer transformer)
    {
        ArrayList<SettableBeanProperty> newProps = new ArrayList<SettableBeanProperty>(_properties.size());
        for (SettableBeanProperty prop : _properties) {
            String newName = transformer.transform(prop.getName());
            newName = InternCache.instance.intern(newName);
            prop = prop.withSimpleName(newName);
            ValueDeserializer<?> deser = prop.getValueDeserializer();
            if (deser != null) {
                @SuppressWarnings("unchecked")
                ValueDeserializer<Object> newDeser = (ValueDeserializer<Object>)
                    deser.unwrappingDeserializer(ctxt, transformer);
                if (newDeser != deser) {
                    prop = prop.withValueDeserializer(newDeser);
                }
            }
            newProps.add(prop);
        }
        return new UnwrappedPropertyHandler(newProps);
    }

    /*
    public List<SettableBeanProperty> getHandledProperties() {
        return Collections.unmodifiableList(_properties);
    }
    */

    @SuppressWarnings("resource")
    public Object processUnwrapped(JsonParser originalParser, DeserializationContext ctxt,
            Object bean, TokenBuffer buffered)
        throws JacksonException
    {
        for (int i = 0, len = _properties.size(); i < len; ++i) {
            SettableBeanProperty prop = _properties.get(i);
            JsonParser p = buffered.asParser(ctxt);
            p.nextToken();
            prop.deserializeAndSet(p, ctxt, bean);
        }
        return bean;
    }
}
