package com.fasterxml.jackson.databind.deser.impl;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.util.Annotations;

/**
 * Wrapper property that is used to handle managed (forward) properties
 * Basically just needs to delegate first to actual forward property, and
 * then to back property.
 */
public final class ManagedReferenceProperty
    extends SettableBeanProperty
{
    private static final long serialVersionUID = 1L;

    protected final String _referenceName;
    
    /**
     * Flag that indicates whether property to handle is a container type
     * (array, Collection, Map) or not.
     */
    protected final boolean _isContainer;
    
    protected final SettableBeanProperty _managedProperty;

    protected final SettableBeanProperty _backProperty;
    
    public ManagedReferenceProperty(SettableBeanProperty forward, String refName,
            SettableBeanProperty backward, Annotations contextAnnotations, boolean isContainer)
    {
        super(forward.getFullName(), forward.getType(), forward.getWrapperName(),
                forward.getValueTypeDeserializer(), contextAnnotations,
                forward.getMetadata());
        _referenceName = refName;
        _managedProperty = forward;
        _backProperty = backward;
        _isContainer = isContainer;
    }

    protected ManagedReferenceProperty(ManagedReferenceProperty src, JsonDeserializer<?> deser)
    {
        super(src, deser);
        _referenceName = src._referenceName;
        _isContainer = src._isContainer;
        _managedProperty = src._managedProperty;
        _backProperty = src._backProperty;
    }

    protected ManagedReferenceProperty(ManagedReferenceProperty src, PropertyName newName) {
        super(src, newName);
        _referenceName = src._referenceName;
        _isContainer = src._isContainer;
        _managedProperty = src._managedProperty;
        _backProperty = src._backProperty;
    }

    @Override
    public ManagedReferenceProperty withName(PropertyName newName) {
        return new ManagedReferenceProperty(this, newName);
    }
    
    @Override
    public ManagedReferenceProperty withValueDeserializer(JsonDeserializer<?> deser) {
        return new ManagedReferenceProperty(this, deser);
    }
    
    /*
    /**********************************************************
    /* BeanProperty impl
    /**********************************************************
     */
    
    @Override
    public <A extends Annotation> A getAnnotation(Class<A> acls) {
        return _managedProperty.getAnnotation(acls);
    }

    @Override public AnnotatedMember getMember() {  return _managedProperty.getMember(); }

    /*
    /**********************************************************
    /* Overridden methods
    /**********************************************************
     */

    @Override
    public void deserializeAndSet(JsonParser jp, DeserializationContext ctxt, Object instance)
            throws IOException, JsonProcessingException {
        set(instance, _managedProperty.deserialize(jp, ctxt));
    }

    @Override
    public Object deserializeSetAndReturn(JsonParser jp, DeserializationContext ctxt, Object instance)
            throws IOException, JsonProcessingException {
        return setAndReturn(instance, deserialize(jp, ctxt));
    }
    
    @Override
    public final void set(Object instance, Object value) throws IOException {
    	setAndReturn(instance, value);
    }

    @Override
    public Object setAndReturn(Object instance, Object value) throws IOException
    {
        /* 04-Feb-2014, tatu: As per [#390], it may be necessary to switch the
         *   ordering of forward/backward references, and start with back ref.
         */
        if (value != null) {
            if (_isContainer) { // ok, this gets ugly... but has to do for now
                if (value instanceof Object[]) {
                    for (Object ob : (Object[]) value) {
                        if (ob != null) { _backProperty.set(ob, instance); }
                    }
                } else if (value instanceof Collection<?>) {
                    for (Object ob : (Collection<?>) value) {
                        if (ob != null) { _backProperty.set(ob, instance); }
                    }
                } else if (value instanceof Map<?,?>) {
                    for (Object ob : ((Map<?,?>) value).values()) {
                        if (ob != null) { _backProperty.set(ob, instance); }
                    }
                } else {
                    throw new IllegalStateException("Unsupported container type ("+value.getClass().getName()
                            +") when resolving reference '"+_referenceName+"'");
                }
            } else {
                _backProperty.set(value, instance);
            }
        }
        // and then the forward reference itself
        return _managedProperty.setAndReturn(instance, value);
	}
}
