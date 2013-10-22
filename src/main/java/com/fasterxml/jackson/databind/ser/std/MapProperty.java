package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.PropertyWriter;

/**
 * Helper class needed to support flexible filtering of Map properties
 * with generic JSON Filter functionality. Since {@link java.util.Map}s
 * are not handled as a collection of properties by Jackson (unlike POJOs),
 * bit more wrapping is required.
 */
public class MapProperty extends PropertyWriter
{
    protected TypeSerializer _typeSerializer;
    
    protected Object _key, _value;

    protected JsonSerializer<Object> _keySerializer, _valueSerializer;

    public MapProperty(TypeSerializer typeSer)
    {
        _typeSerializer = typeSer;
    }
    
    /**
     * Initialization method that needs to be called before passing
     * property to filter.
     */
    public void reset(Object key, Object value,
            JsonSerializer<Object> keySer, JsonSerializer<Object> valueSer)
    {
        _key = key;
        _value = value;
        _keySerializer = keySer;
        _valueSerializer = valueSer;
    }
    
    @Override
    public String getName() {
        if (_key instanceof String) {
            return (String) _key;
        }
        return String.valueOf(_key);
    }

    @Override
    public PropertyName getFullName() {
        return new PropertyName(getName());
    }

    @Override
    public void serializeAsField(Object pojo, JsonGenerator jgen,
            SerializerProvider provider) throws IOException
    {
        _keySerializer.serialize(_key, jgen, provider);
        if (_typeSerializer == null) {
            _valueSerializer.serialize(_value, jgen, provider);
        } else {
            _valueSerializer.serializeWithType(_value, jgen, provider, _typeSerializer);
        }
    }

    @Override
    public void serializeAsOmittedField(Object pojo, JsonGenerator jgen,
            SerializerProvider provider) throws Exception
    {
        if (!jgen.canOmitFields()) {
            jgen.writeOmittedField(getName());
        }
    }

    @Override
    public void serializeAsElement(Object pojo, JsonGenerator jgen,
            SerializerProvider provider) throws Exception
    {
        if (_typeSerializer == null) {
            _valueSerializer.serialize(_value, jgen, provider);
        } else {
            _valueSerializer.serializeWithType(_value, jgen, provider, _typeSerializer);
        }
    }
    
    @Override
    public void serializeAsPlaceholder(Object pojo, JsonGenerator jgen,
            SerializerProvider provider) throws Exception
    {
        jgen.writeNull();
    }

    @Override
    public void depositSchemaProperty(JsonObjectFormatVisitor objectVisitor)
        throws JsonMappingException
    {
        // !!! TODO
    }

    @Override
    @Deprecated
    public void depositSchemaProperty(ObjectNode propertiesNode,
            SerializerProvider provider) throws JsonMappingException {
        // !!! TODO
    }
}