package com.fasterxml.jackson.databind.deser.impl;

import java.io.IOException;
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
    extends SettableBeanProperty.Delegating
{
    private static final long serialVersionUID = 1L;

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
        _creator = ctor;
    }

    /**
     * Constructor used with JDK Serialization; needed to handle transient
     * Constructor, wrap/unwrap in/out-of Annotated variant.
     */
    protected InnerClassProperty(SettableBeanProperty src, AnnotatedConstructor ann)
    {
        super(src);
        _annotated = ann;
        _creator = (_annotated == null) ? null : _annotated.getAnnotated();
        if (_creator == null) {
            throw new IllegalArgumentException("Missing constructor (broken JDK (de)serialization?)");
        }
    }

    @Override
    protected SettableBeanProperty withDelegate(SettableBeanProperty d) {
        if (d == this.delegate) {
            return this;
        }
        return new InnerClassProperty(d, _creator);
    }

    /*
    /**********************************************************
    /* Deserialization methods
    /**********************************************************
     */

    @Override
    public void deserializeAndSet(JsonParser p, DeserializationContext ctxt, Object bean)
        throws IOException
    {
        JsonToken t = p.currentToken();
        Object value;
        if (t == JsonToken.VALUE_NULL) {
            value = _valueDeserializer.getNullValue(ctxt);
        } else if (_valueTypeDeserializer != null) {
            value = _valueDeserializer.deserializeWithType(p, ctxt, _valueTypeDeserializer);
        } else  { // the usual case
            try {
                value = _creator.newInstance(bean);
            } catch (Exception e) {
                ClassUtil.unwrapAndThrowAsIAE(e, String.format(
"Failed to instantiate class %s, problem: %s",
_creator.getDeclaringClass().getName(), e.getMessage()));
                value = null;
            }
            _valueDeserializer.deserialize(p, ctxt, value);
        }
        set(bean, value);
    }

    @Override
    public Object deserializeSetAndReturn(JsonParser p, DeserializationContext ctxt, Object instance)
        throws IOException
    {
        return setAndReturn(instance, deserialize(p, ctxt));
    }

// these are fine with defaults
//    public final void set(Object instance, Object value) throws IOException { }
//    public Object setAndReturn(Object instance, Object value) throws IOException { }

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
        if (_annotated == null) {
            return new InnerClassProperty(this, new AnnotatedConstructor(null, _creator, null, null));
        }
        return this;
    }
}
