package com.fasterxml.jackson.databind.deser.impl;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.*;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;

/**
 * {@link SettableBeanProperty} implementation that will try to access value of
 * the property first, and if non-null value found, pass that for update
 * (using {@link com.fasterxml.jackson.databind.JsonDeserializer#deserialize(com.fasterxml.jackson.core.JsonParser, com.fasterxml.jackson.databind.DeserializationContext, Object)})
 * instead of constructing a new value. This is necessary to support "merging" properties.
 *<p>
 * Note that there are many similarities to {@link SetterlessProperty}, which predates
 * this variant; and that one is even used in cases where there is no mutator
 * available.
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
        Object oldValue = _accessor.getValue(instance);
        Object newValue;
        // 20-Oct-2016, tatu: Couple of possibilities of how to proceed; for
        //    now, default to "normal" handling without merging
        if (oldValue == null) {
            newValue = delegate.deserialize(p, ctxt);
        } else {
            newValue = delegate.deserializeWith(p, ctxt, oldValue);
        }
        if (newValue != oldValue) {
            // 18-Apr-2017, tatu: Null handling should occur within delegate, which may
            //     set/skip/transform it, or throw an exception.
            delegate.set(instance, newValue);
        }
    }

    @Override
    public Object deserializeSetAndReturn(JsonParser p,
            DeserializationContext ctxt, Object instance) throws IOException
    {
        Object oldValue = _accessor.getValue(instance);
        Object newValue;
        // 20-Oct-2016, tatu: Couple of possibilities of how to proceed; for
        //    now, default to "normal" handling without merging
        if (oldValue == null) {
            newValue = delegate.deserialize(p, ctxt);
        } else {
            newValue = delegate.deserializeWith(p, ctxt, oldValue);
        }
        // 23-Oct-2016, tatu: One possible complication here; should we always
        //    try calling setter on builder? Presumably should not be required,
        //    but may need to revise
        if (newValue != oldValue) {
            // 31-Oct-2016, tatu: Basically should just ignore as null can't really
            //    contribute to merging.
            if (newValue != null) {
                return delegate.setAndReturn(instance, newValue);
            }
        }
        return instance;
    }

    @Override
    public void set(Object instance, Object value) throws IOException {
        // 31-Oct-2016, tatu: Basically should just ignore as null can't really
        //    contribute to merging.
        if (value != null) {
            delegate.set(instance, value);
        }
    }

    @Override
    public Object setAndReturn(Object instance, Object value) throws IOException {
        // 31-Oct-2016, tatu: Basically should just ignore as null can't really
        //    contribute to merging.
        if (value != null) {
            return delegate.setAndReturn(instance, value);
        }
        return instance;
    }
}
