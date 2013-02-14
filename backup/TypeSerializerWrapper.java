package com.fasterxml.jackson.databind.jsontype;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanProperty;

/**
 * Helper class used in cases where we caller has to override source
 * for type identifier, for example when serializing a value using
 * a delegate or surrogate value, in which case type id is to be based
 * on the original value, but serialization done using surrogate.
 * 
 * @since 2.2
 */
public class TypeSerializerWrapper
    extends TypeSerializer
{
    /**
     * Actual TypeSerializer to use
     */
    protected final TypeSerializer _delegate;

    protected final Object _value;
    
    public TypeSerializerWrapper(TypeSerializer delegate, Object value)
    {
        _delegate = delegate;
        _value = value;
    }
    
    /*
    /**********************************************************
    /* TypeSerializer implementation, metadata
    /**********************************************************
     */
    
    @Override
    public TypeSerializer forProperty(BeanProperty prop) {
        TypeSerializer d2 = _delegate.forProperty(prop);
        if (d2 == _delegate) {
            return this;
        }
        return new TypeSerializerWrapper(d2, _value);
    }

    @Override
    public As getTypeInclusion() {
        return _delegate.getTypeInclusion();
    }

    @Override
    public String getPropertyName() {
        return _delegate.getPropertyName();
    }

    @Override
    public TypeIdResolver getTypeIdResolver() {
        return _delegate.getTypeIdResolver();
    }

    /*
    /**********************************************************
    /* TypeSerializer implementation, actual write methods
    /**********************************************************
     */
    
    @Override
    public void writeTypePrefixForScalar(Object value, JsonGenerator jgen)
            throws IOException, JsonProcessingException {
        _delegate.writeTypePrefixForScalar(_value, jgen);
    }

    @Override
    public void writeTypePrefixForObject(Object value, JsonGenerator jgen)
            throws IOException, JsonProcessingException {
        _delegate.writeTypePrefixForObject(_value, jgen);
    }

    @Override
    public void writeTypePrefixForArray(Object value, JsonGenerator jgen)
            throws IOException, JsonProcessingException {
        _delegate.writeTypePrefixForArray(_value, jgen);
    }

    @Override
    public void writeTypeSuffixForScalar(Object value, JsonGenerator jgen)
            throws IOException, JsonProcessingException {
        _delegate.writeTypeSuffixForScalar(_value, jgen);
    }

    @Override
    public void writeTypeSuffixForObject(Object value, JsonGenerator jgen)
            throws IOException, JsonProcessingException {
        _delegate.writeTypeSuffixForObject(_value, jgen);
    }

    @Override
    public void writeTypeSuffixForArray(Object value, JsonGenerator jgen)
            throws IOException, JsonProcessingException {
        _delegate.writeTypeSuffixForArray(_value, jgen);
    }

    @Override
    public void writeCustomTypePrefixForScalar(Object value,
            JsonGenerator jgen, String typeId) throws IOException, JsonProcessingException {
        _delegate.writeCustomTypePrefixForScalar(_value, jgen, typeId);
    }

    @Override
    public void writeCustomTypePrefixForObject(Object value,
            JsonGenerator jgen, String typeId) throws IOException, JsonProcessingException {
        _delegate.writeCustomTypePrefixForObject(_value, jgen, typeId);
    }

    @Override
    public void writeCustomTypePrefixForArray(Object value, JsonGenerator jgen,
            String typeId) throws IOException, JsonProcessingException {
        _delegate.writeCustomTypePrefixForArray(_value, jgen, typeId);
    }

    @Override
    public void writeCustomTypeSuffixForScalar(Object value,
            JsonGenerator jgen, String typeId) throws IOException, JsonProcessingException {
        _delegate.writeCustomTypeSuffixForScalar(_value, jgen, typeId);
    }

    @Override
    public void writeCustomTypeSuffixForObject(Object value,
            JsonGenerator jgen, String typeId) throws IOException,
            JsonProcessingException {
        _delegate.writeCustomTypeSuffixForObject(_value, jgen, typeId);
    }

    @Override
    public void writeCustomTypeSuffixForArray(Object value, JsonGenerator jgen,
            String typeId) throws IOException, JsonProcessingException {
        _delegate.writeCustomTypeSuffixForArray(_value, jgen, typeId);
    }
}
