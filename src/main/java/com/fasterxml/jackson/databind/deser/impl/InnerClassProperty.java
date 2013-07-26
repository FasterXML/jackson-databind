package com.fasterxml.jackson.databind.deser.impl;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * This sub-class is used to handle special case of value being a
 * non-static inner class. If so, we will have to use a special
 * alternative for default constructor; but otherwise can delegate
 * to regular implementation.
 */
public final class InnerClassProperty
    extends SettableBeanProperty
{
    private static final long serialVersionUID = 1L;

    /**
     * Actual property that we use after value construction.
     */
    protected final SettableBeanProperty _delegate;

    /**
     * Single-arg constructor we use for value instantiation.
     */
    protected final Constructor<?> _creator;
    
    public InnerClassProperty(SettableBeanProperty delegate,
            Constructor<?> ctor)
    {
        super(delegate);
        _delegate = delegate;
        _creator = ctor;
    }

    protected InnerClassProperty(InnerClassProperty src, JsonDeserializer<?> deser)
    {
        super(src, deser);
        _delegate = src._delegate.withValueDeserializer(deser);
        _creator = src._creator;
    }

    protected InnerClassProperty(InnerClassProperty src, PropertyName newName) {
        super(src, newName);
        _delegate = src._delegate.withName(newName);
        _creator = src._creator;
    }

    @Override
    public InnerClassProperty withName(PropertyName newName) {
        return new InnerClassProperty(this, newName);
    }

    @Override
    public InnerClassProperty withValueDeserializer(JsonDeserializer<?> deser) {
        return new InnerClassProperty(this, deser);
    }
    
    // // // BeanProperty impl
    
    @Override
    public <A extends Annotation> A getAnnotation(Class<A> acls) {
        return _delegate.getAnnotation(acls);
    }

    @Override public AnnotatedMember getMember() {  return _delegate.getMember(); }

    /*
    /**********************************************************
    /* Deserialization methods
    /**********************************************************
     */

    @Override
    public void deserializeAndSet(JsonParser jp, DeserializationContext ctxt,
            Object bean)
        throws IOException, JsonProcessingException
    {
        JsonToken t = jp.getCurrentToken();
        Object value;
        if (t == JsonToken.VALUE_NULL) {
            value = (_nullProvider == null) ? null : _nullProvider.nullValue(ctxt);
        } else if (_valueTypeDeserializer != null) {
            value = _valueDeserializer.deserializeWithType(jp, ctxt, _valueTypeDeserializer);
        } else  { // the usual case
            try {
                value = _creator.newInstance(bean);
            } catch (Exception e) {
                ClassUtil.unwrapAndThrowAsIAE(e, "Failed to instantiate class "+_creator.getDeclaringClass().getName()+", problem: "+e.getMessage());
                value = null;
            }
            _valueDeserializer.deserialize(jp, ctxt, value);
        }
        set(bean, value);
    }

    @Override
    public Object deserializeSetAndReturn(JsonParser jp,
    		DeserializationContext ctxt, Object instance)
        throws IOException, JsonProcessingException
    {
        return setAndReturn(instance, deserialize(jp, ctxt));
    }
    
    @Override
    public final void set(Object instance, Object value) throws IOException
    {
        _delegate.set(instance, value);
    }

    @Override
    public Object setAndReturn(Object instance, Object value)
            throws IOException
    {
    	return _delegate.setAndReturn(instance, value);
    }
}