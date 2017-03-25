package com.fasterxml.jackson.databind.deser.impl;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.introspect.*;
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
     * Constructor used when deserializing this property.
     * Transient since there is no need to persist; only needed during
     * construction of objects.
     */
    final protected transient Constructor<?> _creator;
    
    /**
     * Serializable version of single-arg constructor we use for value instantiation.
     */
    protected AnnotatedConstructor _annotated;

    public InnerClassProperty(SettableBeanProperty delegate,
            Constructor<?> ctor)
    {
        super(delegate);
        _delegate = delegate;
        _creator = ctor;
    }

    /**
     * Constructor used with JDK Serialization; needed to handle transient
     * Constructor, wrap/unwrap in/out-of Annotated variant.
     */
    protected InnerClassProperty(InnerClassProperty src, AnnotatedConstructor ann)
    {
        super(src);
        _delegate = src._delegate;
        _annotated = ann;
        _creator = (_annotated == null) ? null : _annotated.getAnnotated();
        if (_creator == null) {
            throw new IllegalArgumentException("Missing constructor (broken JDK (de)serialization?)");
        }
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
        if (_valueDeserializer == deser) {
            return this;
        }
        return new InnerClassProperty(this, deser);
    }

    @Override
    public void assignIndex(int index) { _delegate.assignIndex(index); }

    @Override
    public int getPropertyIndex() { return _delegate.getPropertyIndex(); }

    @Override
    public int getCreatorIndex() { return _delegate.getCreatorIndex(); }
    
    @Override
    public void fixAccess(DeserializationConfig config) {
        _delegate.fixAccess(config);
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
    public void deserializeAndSet(JsonParser jp, DeserializationContext ctxt, Object bean)
        throws IOException
    {
        JsonToken t = jp.getCurrentToken();
        Object value;
        if (t == JsonToken.VALUE_NULL) {
            value = _valueDeserializer.getNullValue(ctxt);
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
        throws IOException
    {
        return setAndReturn(instance, deserialize(jp, ctxt));
    }
    
    @Override
    public final void set(Object instance, Object value) throws IOException {
        _delegate.set(instance, value);
    }

    @Override
    public Object setAndReturn(Object instance, Object value) throws IOException {
        return _delegate.setAndReturn(instance, value);
    }

    /*
    /**********************************************************
    /* JDK serialization handling
    /**********************************************************
     */

    // When reading things back, 
    Object readResolve() {
        return new InnerClassProperty(this, _annotated);
    }

    Object writeReplace() {
        // need to construct a fake instance to support serialization
        if (_annotated != null) {
            return this;
        }
        return new InnerClassProperty(this, new AnnotatedConstructor(null, _creator, null, null));
    }
}