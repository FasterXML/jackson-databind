package com.fasterxml.jackson.databind.deser.impl;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
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
    extends SettableBeanProperty.Delegating
{
    private static final long serialVersionUID = 1L;

    /**
     * Member (field, method) used for accessing existing value.
     */
    protected final AnnotatedMember _accessor;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    protected MergingSettableBeanProperty(SettableBeanProperty delegate,
            AnnotatedMember accessor)
    {
        super(delegate);
        _accessor = accessor;
    }

    protected MergingSettableBeanProperty(MergingSettableBeanProperty src,
            SettableBeanProperty delegate)
    {
        super(delegate);
        _accessor = src._accessor;
    }

    public static MergingSettableBeanProperty construct(SettableBeanProperty delegate,
            AnnotatedMember accessor)
    {
        return new MergingSettableBeanProperty(delegate, accessor);
    }

    @Override
    protected SettableBeanProperty withDelegate(SettableBeanProperty d) {
        return new MergingSettableBeanProperty(d, _accessor);
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
        delegate.set(instance, _deserialize(p, ctxt, instance));
    }

    @Override
    public Object deserializeSetAndReturn(JsonParser p,
            DeserializationContext ctxt, Object instance) throws IOException {
        return delegate.setAndReturn(instance, _deserialize(p, ctxt, instance));
    }

    @Override
    public void set(Object instance, Object value) throws IOException {
        delegate.set(instance, value);
    }

    @Override
    public Object setAndReturn(Object instance, Object value)
            throws IOException
    {
        return delegate.setAndReturn(instance, value);
    }

    protected Object _deserialize(JsonParser p, DeserializationContext ctxt,
            Object instance) throws IOException
    {
        Object value = _accessor.getValue(instance);
        // 20-Oct-2016, tatu: Couple of possibilities of how to proceed; for
        //    now, default to "normal" handling without merging
        if (value == null) {
            return delegate.deserialize(p, ctxt);
        }
        Object result = delegate.deserializeWith(p,  ctxt,  value);
        // 20-Oct-2016, tatu: Similarly, we may get same object or different one;
        //   whether to return original or new is an open question.
        return result;
    }
}
