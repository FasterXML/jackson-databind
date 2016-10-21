package com.fasterxml.jackson.databind.deser.impl;

import java.io.IOException;
import java.lang.annotation.Annotation;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.deser.*;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;

/**
 * {@link SettableBeanProperty} implementation that will try to access value of
 * the property first, and if non-null value found, pass that for update
 * (using {@link com.fasterxml.jackson.databind.JsonDeserializer#deserialize(com.fasterxml.jackson.core.JsonParser, com.fasterxml.jackson.databind.DeserializationContext, Object)}
 * ) instead of constructing a new value. This is necessary to support "merging" properties.
 *
 * @since 2.9
 */
public class MergingSettableBeanProperty
    extends SettableBeanProperty
{
    private static final long serialVersionUID = 1L;

    /**
     * Underlying actual property (field- or member-backed).
     */
    protected final SettableBeanProperty _delegate;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    public MergingSettableBeanProperty(SettableBeanProperty delegate)
    {
        super(delegate);
        _delegate = delegate;
    }

    protected MergingSettableBeanProperty(MergingSettableBeanProperty src,
            SettableBeanProperty delegate)
    {
        super(src);
        _delegate = src._delegate;
    }

    @Override
    public SettableBeanProperty withValueDeserializer(JsonDeserializer<?> deser) {
        return _new(_delegate.withValueDeserializer(deser));
    }

    @Override
    public SettableBeanProperty withName(PropertyName newName) {
        return _new(_delegate.withName(newName));
    }

    protected MergingSettableBeanProperty _new(SettableBeanProperty newDelegate) {
        if (newDelegate == _delegate) {
            return this;
        }
        return new MergingSettableBeanProperty(this, newDelegate);
    }

    /*
    /**********************************************************
    /* BeanProperty impl
    /**********************************************************
     */

    @Override
    public AnnotatedMember getMember() {
        return _delegate.getMember();
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> acls) {
        return _delegate.getAnnotation(acls);
    }

    /*
    /**********************************************************
    /* Deserialization methods
    /**********************************************************
     */

    @Override
    public void deserializeAndSet(JsonParser p, DeserializationContext ctxt,
            Object instance) throws IOException
    {
        // TODO Auto-generated method stub
    }

    @Override
    public Object deserializeSetAndReturn(JsonParser p,
            DeserializationContext ctxt, Object instance) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void set(Object instance, Object value) throws IOException {
        // TODO Auto-generated method stub
    }

    @Override
    public Object setAndReturn(Object instance, Object value)
            throws IOException {
        // TODO Auto-generated method stub
        return null;
    }
}
