package com.fasterxml.jackson.databind.ser.std;

import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.type.ReferenceType;
import com.fasterxml.jackson.databind.util.NameTransformer;

public class AtomicReferenceSerializer
    extends ReferenceTypeSerializer<AtomicReference<?>>
{
    private static final long serialVersionUID = 1L;

    /*
    /**********************************************************
    /* Constructors, factory methods
    /**********************************************************
     */

    public AtomicReferenceSerializer(ReferenceType fullType, boolean staticTyping,
            TypeSerializer vts, JsonSerializer<Object> ser)
    {
        super(fullType, staticTyping, vts, ser);
    }

    protected AtomicReferenceSerializer(AtomicReferenceSerializer base, BeanProperty property,
            TypeSerializer vts, JsonSerializer<?> valueSer,
            NameTransformer unwrapper,
            Object suppressableValue, boolean suppressNulls)
    {
        super(base, property, vts, valueSer, unwrapper,
                suppressableValue, suppressNulls);
    }

    @Override
    protected ReferenceTypeSerializer<AtomicReference<?>> withResolved(BeanProperty prop,
            TypeSerializer vts, JsonSerializer<?> valueSer,
            NameTransformer unwrapper)
    {
        return new AtomicReferenceSerializer(this, prop, vts, valueSer, unwrapper,
                _suppressableValue, _suppressNulls);
    }

    @Override
    public ReferenceTypeSerializer<AtomicReference<?>> withContentInclusion(Object suppressableValue,
            boolean suppressNulls)
    {
        return new AtomicReferenceSerializer(this, _property, _valueTypeSerializer,
                _valueSerializer, _unwrapper,
                suppressableValue, suppressNulls);
    }

    /*
    /**********************************************************
    /* Abstract method impls
    /**********************************************************
     */

    @Override
    protected boolean _isValuePresent(AtomicReference<?> value) {
        return value.get() != null;
    }

    @Override
    protected Object _getReferenced(AtomicReference<?> value) {
        return value.get();
    }

    @Override
    protected Object _getReferencedIfPresent(AtomicReference<?> value) {
        return value.get();
    }
}
