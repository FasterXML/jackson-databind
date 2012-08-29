package com.fasterxml.jackson.databind.deser.impl;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.SettableAnyProperty;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;

/**
 * Simple container used for temporarily buffering a set of
 * <code>PropertyValue</code>s.
 * Using during construction of beans (and Maps) that use Creators, 
 * and hence need buffering before instance (that will have properties
 * to assign values to) is constructed.
 */
public final class PropertyValueBuffer
{
    protected final JsonParser _parser;
    protected final DeserializationContext _context;
    
    /**
     * Buffer used for storing creator parameters for constructing
     * instance
     */
    protected final Object[] _creatorParameters;

    protected final ObjectIdReader _objectIdReader;
    
    /**
     * Number of creator parameters we are still missing.
     *<p>
     * NOTE: assumes there are no duplicates, for now.
     */
    private int _paramsNeeded;
    
    /**
     * If we get non-creator parameters before or between
     * creator parameters, those need to be buffered. Buffer
     * is just a simple linked list
     */
    private PropertyValue _buffered;

    /**
     * In case there is an Object Id property to handle, this is the value
     * we have for it.
     */
    private Object _idValue;
    
    public PropertyValueBuffer(JsonParser jp, DeserializationContext ctxt, int paramCount,
            ObjectIdReader oir)
    {
        _parser = jp;
        _context = ctxt;
        _paramsNeeded = paramCount;
        _objectIdReader = oir;
        _creatorParameters = new Object[paramCount];
    }

    public void inject(SettableBeanProperty[] injectableProperties)
    {
        for (int i = 0, len = injectableProperties.length; i < len; ++i) {
            SettableBeanProperty prop = injectableProperties[i];
            if (prop != null) {
                // null since there is no POJO yet
                _creatorParameters[i] = _context.findInjectableValue(prop.getInjectableValueId(),
                        prop, null);
            }
        }
    }
    
    /**
     * @param defaults If any of parameters requires nulls to be replaced with a non-null
     *    object (usually primitive types), this is a non-null array that has such replacement
     *    values (and nulls for cases where nulls are ok)
     */
    protected final Object[] getParameters(Object[] defaults)
    {
        if (defaults != null) {
            for (int i = 0, len = _creatorParameters.length; i < len; ++i) {
                if (_creatorParameters[i] == null) {
                    Object value = defaults[i];
                    if (value != null) {
                        _creatorParameters[i] = value;
                    }
                }
            }
        }
        return _creatorParameters;
    }


    /**
     * Helper method called to see if given non-creator property is the "id property";
     * and if so, handle appropriately.
     * 
     * @since 2.1
     */
    public boolean readIdProperty(String propName) throws IOException
    {
        if ((_objectIdReader != null) && propName.equals(_objectIdReader.propertyName)) {
            _idValue = _objectIdReader.deserializer.deserialize(_parser, _context);
            return true;
        }
        return false;
    }

    /**
     * Helper method called to handle Object Id value collected earlier, if any
     */
    public Object handleIdValue(final DeserializationContext ctxt, Object bean)
        throws IOException
    {
        if (_objectIdReader != null) {
            if (_idValue != null) {
                ReadableObjectId roid = ctxt.findObjectId(_idValue, _objectIdReader.generator);
                roid.bindItem(bean);
                // also: may need to set a property value as well
                SettableBeanProperty idProp = _objectIdReader.idProperty;
                if (idProp != null) {
                    return idProp.setAndReturn(bean, _idValue);
                }
            } else {
                // TODO: is this an error case?
            }
        }
        return bean;
    }
    
    protected PropertyValue buffered() { return _buffered; }

    public boolean isComplete() { return _paramsNeeded <= 0; }
    
    /**
     * @return True if we have received all creator parameters
     */
    public boolean assignParameter(int index, Object value) {
        _creatorParameters[index] = value;
        return --_paramsNeeded <= 0;
    }
    
    public void bufferProperty(SettableBeanProperty prop, Object value) {
        _buffered = new PropertyValue.Regular(_buffered, value, prop);
    }
    
    public void bufferAnyProperty(SettableAnyProperty prop, String propName, Object value) {
        _buffered = new PropertyValue.Any(_buffered, value, prop, propName);
    }

    public void bufferMapProperty(Object key, Object value) {
        _buffered = new PropertyValue.Map(_buffered, value, key);
    }
}

