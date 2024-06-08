package com.fasterxml.jackson.databind.deser.impl;

import java.io.IOException;

import com.fasterxml.jackson.databind.deser.SettableAnyProperty;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;

/**
 * Base class for property values that need to be buffered during
 * deserialization.
 */
public abstract class PropertyValue
{
    public final PropertyValue next;

    /**
     * Value to assign when POJO has been instantiated.
     */
    public final Object value;

    protected PropertyValue(PropertyValue next, Object value)
    {
        this.next = next;
        this.value = value;
    }

    /**
     * Method called to assign stored value of this property to specified
     * bean instance
     */
    public abstract void assign(Object bean)
        throws IOException;

    /**
     * Method called to assign stored value of this property to specified
     * parameter object.
     *
     * @since 2.18
     */
    public void setValue(Object parameterObject)
        throws IOException
    {
        throw new UnsupportedOperationException("Should not be called by this type " + getClass().getName());
    }

    /*
    /**********************************************************
    /* Concrete property value classes
    /**********************************************************
     */

    /**
     * Property value that used when assigning value to property using
     * a setter method or direct field access.
     */
    final static class Regular
        extends PropertyValue
    {
        final SettableBeanProperty _property;

        public Regular(PropertyValue next, Object value,
                       SettableBeanProperty prop)
        {
            super(next, value);
            _property = prop;
        }

        @Override
        public void assign(Object bean)
            throws IOException
        {
            _property.set(bean, value);
        }
    }

    /**
     * Property value type used when storing entries to be added
     * to a POJO using "any setter" (method that takes name and
     * value arguments, allowing setting multiple different
     * properties using single method).
     */
    final static class Any
        extends PropertyValue
    {
        final SettableAnyProperty _property;
        final String _propertyName;

        public Any(PropertyValue next, Object value,
                   SettableAnyProperty prop,
                   String propName)
        {
            super(next, value);
            _property = prop;
            _propertyName = propName;
        }

        @Override
        public void assign(Object bean)
            throws IOException
        {
            _property.set(bean, _propertyName, value);
        }
    }

    /**
     * Property value type used when storing entries to be added
     * to a Map.
     */
    final static class Map
        extends PropertyValue
    {
        final Object _key;

        public Map(PropertyValue next, Object value, Object key)
        {
            super(next, value);
            _key = key;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void assign(Object bean)
            throws IOException
        {
            ((java.util.Map<Object,Object>) bean).put(_key, value);
        }
    }

    /**
     * Property value type used when storing entries to be passed
     * to constructor of POJO using "any-setter".
     *
     * @since 2.18
     */
    final static class AnyParameter
            extends PropertyValue
    {
        final SettableAnyProperty _property;
        final String _propertyName;

        public AnyParameter(PropertyValue next, Object value,
                   SettableAnyProperty prop,
                   String propName)
        {
            super(next, value);
            _property = prop;
            _propertyName = propName;
        }

        @Override
        public void assign(Object bean)
            throws IOException
        {
            // do nothing, as we are not assigning to a bean
            // instead, we are assigning to a parameter object via setValue field.
        }

        @Override
        public void setValue(Object parameterObject)
            throws IOException
        {
            // AnyParameter
            _property.set(parameterObject, _propertyName, value);
        }
    }
}
