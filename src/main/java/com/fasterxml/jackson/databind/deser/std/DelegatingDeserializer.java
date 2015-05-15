package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.util.Collection;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.*;
import com.fasterxml.jackson.databind.deser.impl.ObjectIdReader;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;

/**
 * Base class that simplifies implementations of {@link JsonDeserializer}s
 * that mostly delegate functionality to another deserializer implementation
 * (possibly forming a chaing of deserializers delegating functionality
 * in some cases)
 * 
 * @since 2.1
 */
public abstract class DelegatingDeserializer
    extends StdDeserializer<Object>
    implements ContextualDeserializer, ResolvableDeserializer
{
    private static final long serialVersionUID = 1L;

    protected final JsonDeserializer<?> _delegatee;
    
    /*
    /**********************************************************************
    /* Construction
    /**********************************************************************
     */

    public DelegatingDeserializer(JsonDeserializer<?> delegatee)
    {
        super(_figureType(delegatee));
        _delegatee = delegatee;
    }

    protected abstract JsonDeserializer<?> newDelegatingInstance(JsonDeserializer<?> newDelegatee);
    
    private static Class<?> _figureType(JsonDeserializer<?> deser)
    {
        Class<?> cls = deser.handledType();
        if (cls != null) {
            return cls;
        }
        return Object.class;
    }
    
    /*
    /**********************************************************************
    /* Overridden methods for contextualization, resolving
    /**********************************************************************
     */

    @Override
    public void resolve(DeserializationContext ctxt) throws JsonMappingException {
        if (_delegatee instanceof ResolvableDeserializer) {
            ((ResolvableDeserializer) _delegatee).resolve(ctxt);
        }
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt,
            BeanProperty property)
        throws JsonMappingException
    {
        JavaType vt = ctxt.constructType(_delegatee.handledType());
        JsonDeserializer<?> del = ctxt.handleSecondaryContextualization(_delegatee,
                property, vt);
        if (del == _delegatee) {
            return this;
        }
        return newDelegatingInstance(del);
    }

    /**
     * @deprecated Since 2.3, use {@link #newDelegatingInstance} instead
     */
    @Deprecated
    protected JsonDeserializer<?> _createContextual(DeserializationContext ctxt,
            BeanProperty property, JsonDeserializer<?> newDelegatee)
    {
        if (newDelegatee == _delegatee) {
            return this;
        }
        return newDelegatingInstance(newDelegatee);
    }

    @Override
    public SettableBeanProperty findBackReference(String logicalName) {
        // [Issue#253]: Hope this works....
        return _delegatee.findBackReference(logicalName);
    }

    /*
    /**********************************************************************
    /* Overridden deserialization methods
    /**********************************************************************
     */

    @Override
    public Object deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        return _delegatee.deserialize(jp,  ctxt);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object deserialize(JsonParser jp, DeserializationContext ctxt,
            Object intoValue)
        throws IOException, JsonProcessingException
    {
        return ((JsonDeserializer<Object>)_delegatee).deserialize(jp, ctxt, intoValue);
    }

    @Override
    public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer)
        throws IOException, JsonProcessingException
    {
        return _delegatee.deserializeWithType(jp, ctxt, typeDeserializer);
    }

    /*
    /**********************************************************************
    /* Overridden other methods
    /**********************************************************************
     */

    @Override
    public JsonDeserializer<?> replaceDelegatee(JsonDeserializer<?> delegatee)
    {
        if (delegatee == _delegatee) {
            return this;
        }
        return newDelegatingInstance(delegatee);
    }

    @Override
    public Object getNullValue(DeserializationContext ctxt) throws JsonMappingException {
        return _delegatee.getNullValue(ctxt);
    }

    @Override
    public Object getEmptyValue(DeserializationContext ctxt) throws JsonMappingException {
        return _delegatee.getEmptyValue(ctxt);
    }

    @Override
    @Deprecated // remove in 2.7
    public Object getNullValue() { return _delegatee.getNullValue(); }

    // Remove in 2.7
    @Override
    @Deprecated // remove in 2.7
    public Object getEmptyValue() { return _delegatee.getEmptyValue(); }

    
    @Override
    public Collection<Object> getKnownPropertyNames() { return _delegatee.getKnownPropertyNames(); }
    
    @Override
    public boolean isCachable() { return _delegatee.isCachable(); }

    @Override
    public ObjectIdReader getObjectIdReader() { return _delegatee.getObjectIdReader(); }

    @Override
    public JsonDeserializer<?> getDelegatee() {
        return _delegatee;
    }
}
