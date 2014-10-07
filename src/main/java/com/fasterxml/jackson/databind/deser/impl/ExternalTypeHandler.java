package com.fasterxml.jackson.databind.deser.impl;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.util.TokenBuffer;

/**
 * Helper class that is used to flatten JSON structure when using
 * "external type id" (see {@link com.fasterxml.jackson.annotation.JsonTypeInfo.As#EXTERNAL_PROPERTY}).
 * This is needed to store temporary state and buffer tokens, as the structure is
 * rearranged a bit so that actual type deserializer can resolve type and 
 * finalize deserialization.
 */
public class ExternalTypeHandler
{
    private final ExtTypedProperty[] _properties;
    private final HashMap<String, Integer> _nameToPropertyIndex;

    private final String[] _typeIds;
    private final TokenBuffer[] _tokens;
    
    protected ExternalTypeHandler(ExtTypedProperty[] properties,
            HashMap<String, Integer> nameToPropertyIndex,
            String[] typeIds, TokenBuffer[] tokens)
    {
        _properties = properties;        
        _nameToPropertyIndex = nameToPropertyIndex;
        _typeIds = typeIds;
        _tokens = tokens;
    }

    protected ExternalTypeHandler(ExternalTypeHandler h)
    {
        _properties = h._properties;
        _nameToPropertyIndex = h._nameToPropertyIndex;
        int len = _properties.length;
        _typeIds = new String[len];
        _tokens = new TokenBuffer[len];
    }
    
    public ExternalTypeHandler start() {
        return new ExternalTypeHandler(this);
    }

    /**
     * Method called to see if given property/value pair is an external type
     * id; and if so handle it. This is <b>only</b> to be called in case
     * containing POJO has similarly named property as the external type id;
     * otherwise {@link #handlePropertyValue} should be called instead.
     */
    public boolean handleTypePropertyValue(JsonParser jp, DeserializationContext ctxt,
            String propName, Object bean)
        throws IOException
    {
        Integer I = _nameToPropertyIndex.get(propName);
        if (I == null) {
            return false;
        }
        int index = I.intValue();
        ExtTypedProperty prop = _properties[index];
        if (!prop.hasTypePropertyName(propName)) {
            return false;
        }
        String typeId = jp.getText();
        // note: can NOT skip child values (should always be String anyway)
        boolean canDeserialize = (bean != null) && (_tokens[index] != null);
        // Minor optimization: deserialize properties as soon as we have all we need:
        if (canDeserialize) {
            _deserializeAndSet(jp, ctxt, bean, index, typeId);
            // clear stored data, to avoid deserializing+setting twice:
            _tokens[index] = null;
        } else {
            _typeIds[index] = typeId;
        }
        return true;
    }
    
    /**
     * Method called to ask handler to handle value of given property,
     * at point where parser points to the first token of the value.
     * Handling can mean either resolving type id it contains (if it matches type
     * property name), or by buffering the value for further use.
     * 
     * @return True, if the given property was properly handled
     */
    public boolean handlePropertyValue(JsonParser jp, DeserializationContext ctxt,
            String propName, Object bean) throws IOException
    {
        Integer I = _nameToPropertyIndex.get(propName);
        if (I == null) {
            return false;
        }
        int index = I.intValue();
        ExtTypedProperty prop = _properties[index];
        boolean canDeserialize;
        if (prop.hasTypePropertyName(propName)) {
            _typeIds[index] = jp.getText();
            jp.skipChildren();
            canDeserialize = (bean != null) && (_tokens[index] != null);
        } else {
            @SuppressWarnings("resource")
            TokenBuffer tokens = new TokenBuffer(jp);
            tokens.copyCurrentStructure(jp);
            _tokens[index] = tokens;
            canDeserialize = (bean != null) && (_typeIds[index] != null);
        }
        /* Minor optimization: let's deserialize properties as soon as
         * we have all pertinent information:
         */
        if (canDeserialize) {
            String typeId = _typeIds[index];
            // clear stored data, to avoid deserializing+setting twice:
            _typeIds[index] = null;
            _deserializeAndSet(jp, ctxt, bean, index, typeId);
            _tokens[index] = null;
        }
        return true;
    }
    
    @SuppressWarnings("resource")
    public Object complete(JsonParser jp, DeserializationContext ctxt, Object bean)
        throws IOException
    {
        for (int i = 0, len = _properties.length; i < len; ++i) {
            String typeId = _typeIds[i];
            if (typeId == null) {
                TokenBuffer tokens = _tokens[i];
                // let's allow missing both type and property (may already have been set, too)
                // but not just one
                if (tokens == null) {
                    continue;
                }
                /* [Issue#118]: Need to mind natural types, for which no type id
                 *   will be included.
                 */
                JsonToken t = tokens.firstToken();
                if (t != null && t.isScalarValue()) {
                    JsonParser buffered = tokens.asParser(jp);
                    buffered.nextToken();
                    SettableBeanProperty extProp = _properties[i].getProperty();
                    Object result = TypeDeserializer.deserializeIfNatural(buffered, ctxt, extProp.getType());
                    if (result != null) {
                        extProp.set(bean, result);
                        continue;
                    }
                    // 26-Oct-2012, tatu: As per [Issue#94], must allow use of 'defaultImpl'
                    if (!_properties[i].hasDefaultType()) {
                        throw ctxt.mappingException("Missing external type id property '"+_properties[i].getTypePropertyName()+"'");
                    }
                    typeId = _properties[i].getDefaultTypeId();
                }
            } else if (_tokens[i] == null) {
                SettableBeanProperty prop = _properties[i].getProperty();
                throw ctxt.mappingException("Missing property '"+prop.getName()+"' for external type id '"+_properties[i].getTypePropertyName());
            }
            _deserializeAndSet(jp, ctxt, bean, i, typeId);
        }
        return bean;
    }

    /**
     * Variant called when creation of the POJO involves buffering of creator properties
     * as well as property-based creator.
     */
    public Object complete(JsonParser jp, DeserializationContext ctxt,
            PropertyValueBuffer buffer, PropertyBasedCreator creator)
        throws IOException
    {
        // first things first: deserialize all data buffered:
        final int len = _properties.length;
        Object[] values = new Object[len];
        for (int i = 0; i < len; ++i) {
            String typeId = _typeIds[i];
            if (typeId == null) {
                // let's allow missing both type and property (may already have been set, too)
                if (_tokens[i] == null) {
                    continue;
                }
                // but not just one
                // 26-Oct-2012, tatu: As per [Issue#94], must allow use of 'defaultImpl'
                if (!_properties[i].hasDefaultType()) {
                    throw ctxt.mappingException("Missing external type id property '"+_properties[i].getTypePropertyName()+"'");
                }
                typeId = _properties[i].getDefaultTypeId();
            } else if (_tokens[i] == null) {
                SettableBeanProperty prop = _properties[i].getProperty();
                throw ctxt.mappingException("Missing property '"+prop.getName()+"' for external type id '"+_properties[i].getTypePropertyName());
            }
            values[i] = _deserialize(jp, ctxt, i, typeId);
        }
        // second: fill in creator properties:
        for (int i = 0; i < len; ++i) {
            SettableBeanProperty prop = _properties[i].getProperty();
            if (creator.findCreatorProperty(prop.getName()) != null) {
                buffer.assignParameter(prop.getCreatorIndex(), values[i]);
            }
        }
        Object bean = creator.build(ctxt, buffer);
        // third: assign non-creator properties
        for (int i = 0; i < len; ++i) {
            SettableBeanProperty prop = _properties[i].getProperty();
            if (creator.findCreatorProperty(prop.getName()) == null) {
                prop.set(bean, values[i]);
            }
        }
        return bean;
    }

    @SuppressWarnings("resource")
    protected final Object _deserialize(JsonParser jp, DeserializationContext ctxt,
            int index, String typeId) throws IOException
    {
        TokenBuffer merged = new TokenBuffer(jp);
        merged.writeStartArray();
        merged.writeString(typeId);
        JsonParser p2 = _tokens[index].asParser(jp);
        p2.nextToken();
        merged.copyCurrentStructure(p2);
        merged.writeEndArray();

        // needs to point to START_OBJECT (or whatever first token is)
        p2 = merged.asParser(jp);
        p2.nextToken();
        return _properties[index].getProperty().deserialize(p2, ctxt);
    }

    @SuppressWarnings("resource")
    protected final void _deserializeAndSet(JsonParser jp, DeserializationContext ctxt,
            Object bean, int index, String typeId) throws IOException
    {
        /* Ok: time to mix type id, value; and we will actually use "wrapper-array"
         * style to ensure we can handle all kinds of JSON constructs.
         */
        TokenBuffer merged = new TokenBuffer(jp);
        merged.writeStartArray();
        merged.writeString(typeId);
        JsonParser p2 = _tokens[index].asParser(jp);
        p2.nextToken();
        merged.copyCurrentStructure(p2);
        merged.writeEndArray();
        // needs to point to START_OBJECT (or whatever first token is)
        p2 = merged.asParser(jp);
        p2.nextToken();
        _properties[index].getProperty().deserializeAndSet(p2, ctxt, bean);
    }
    
    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */
    
    public static class Builder
    {
        private final ArrayList<ExtTypedProperty> _properties = new ArrayList<ExtTypedProperty>();
        private final HashMap<String, Integer> _nameToPropertyIndex = new HashMap<String, Integer>();

        public void addExternal(SettableBeanProperty property, TypeDeserializer typeDeser)
        {
            Integer index = _properties.size();
            _properties.add(new ExtTypedProperty(property, typeDeser));
            _nameToPropertyIndex.put(property.getName(), index);
            _nameToPropertyIndex.put(typeDeser.getPropertyName(), index);
        }
        
        public ExternalTypeHandler build() {
            return new ExternalTypeHandler(_properties.toArray(new ExtTypedProperty[_properties.size()]),
                    _nameToPropertyIndex, null, null);
        }
    }

    private final static class ExtTypedProperty
    {
        private final SettableBeanProperty _property;
        private final TypeDeserializer _typeDeserializer;
        private final String _typePropertyName;
        
        public ExtTypedProperty(SettableBeanProperty property, TypeDeserializer typeDeser)
        {
            _property = property;
            _typeDeserializer = typeDeser;
            _typePropertyName = typeDeser.getPropertyName();
        }

        public boolean hasTypePropertyName(String n) {
            return n.equals(_typePropertyName);
        }

        public boolean hasDefaultType() {
            return _typeDeserializer.getDefaultImpl() != null;
        }

        public String getDefaultTypeId() {
            Class<?> defaultType = _typeDeserializer.getDefaultImpl();
            if (defaultType == null) {
                return null;
            }
            return _typeDeserializer.getTypeIdResolver().idFromValueAndType(null, defaultType);
        }
        
        public String getTypePropertyName() { return _typePropertyName; }
        
        public SettableBeanProperty getProperty() {
            return _property;
        }
    }
}
