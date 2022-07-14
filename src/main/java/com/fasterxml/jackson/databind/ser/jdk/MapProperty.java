package com.fasterxml.jackson.databind.ser.jdk;

import java.lang.annotation.Annotation;

import tools.jackson.core.JsonGenerator;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.PropertyWriter;

/**
 * Helper class needed to support flexible filtering of Map properties
 * with generic JSON Filter functionality. Since {@link java.util.Map}s
 * are not handled as a collection of properties by Jackson (unlike POJOs),
 * bit more wrapping is required.
 */
public class MapProperty extends PropertyWriter
{
    private static final long serialVersionUID = 1L;

    private final static BeanProperty BOGUS_PROP = new BeanProperty.Bogus();
    
    protected final TypeSerializer _typeSerializer;

    protected final BeanProperty _property;

    protected Object _key, _value;

    protected ValueSerializer<Object> _keySerializer, _valueSerializer;

    public MapProperty(TypeSerializer typeSer, BeanProperty prop)
    {
        super((prop == null) ? PropertyMetadata.STD_REQUIRED_OR_OPTIONAL : prop.getMetadata());
        _typeSerializer = typeSer;
        _property = (prop == null) ? BOGUS_PROP : prop;
    }

    /**
     * Initialization method that needs to be called before passing
     * property to filter.
     */
    public void reset(Object key, Object value,
            ValueSerializer<Object> keySer, ValueSerializer<Object> valueSer)
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

    public Object getValue() {
        return _value;
    }

    public void setValue(Object v) {
        _value = v;
    }

    @Override
    public PropertyName getFullName() {
        return new PropertyName(getName());
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> acls) {
        return _property.getAnnotation(acls);
    }

    @Override
    public <A extends Annotation> A getContextAnnotation(Class<A> acls) {
        return _property.getContextAnnotation(acls);
    }
    
    @Override
    public void serializeAsProperty(Object map, JsonGenerator gen,
            SerializerProvider provider)
    {
        _keySerializer.serialize(_key, gen, provider);
        if (_typeSerializer == null) {
            _valueSerializer.serialize(_value, gen, provider);
        } else {
            _valueSerializer.serializeWithType(_value, gen, provider, _typeSerializer);
        }
    }

    @Override
    public void serializeAsOmittedProperty(Object map, JsonGenerator gen,
            SerializerProvider provider)
    {
        if (!gen.canOmitProperties()) {
            gen.writeOmittedProperty(getName());
        }
    }

    @Override
    public void serializeAsElement(Object map, JsonGenerator gen,
            SerializerProvider provider)
    {
        if (_typeSerializer == null) {
            _valueSerializer.serialize(_value, gen, provider);
        } else {
            _valueSerializer.serializeWithType(_value, gen, provider, _typeSerializer);
        }
    }
    
    @Override
    public void serializeAsOmittedElement(Object value, JsonGenerator gen,
            SerializerProvider provider)
    {
        gen.writeNull();
    }

    /*
    /**********************************************************************
    /* Rest of BeanProperty, nop
    /**********************************************************************
     */
    
    @Override
    public void depositSchemaProperty(JsonObjectFormatVisitor objectVisitor,
            SerializerProvider provider)
    {
        _property.depositSchemaProperty(objectVisitor, provider);
    }

    @Override
    public JavaType getType() {
        return _property.getType();
    }

    @Override
    public PropertyName getWrapperName() {
        return _property.getWrapperName();
    }

    @Override
    public AnnotatedMember getMember() {
        return _property.getMember();
    }
}
