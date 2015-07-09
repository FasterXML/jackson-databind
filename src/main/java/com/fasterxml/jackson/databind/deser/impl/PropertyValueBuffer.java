package com.fasterxml.jackson.databind.deser.impl;

import java.io.IOException;
import java.util.BitSet;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.SettableAnyProperty;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;

/**
 * Simple container used for temporarily buffering a set of
 * <code>PropertyValue</code>s.
 * Using during construction of beans (and Maps) that use Creators, 
 * and hence need buffering before instance (that will have properties
 * to assign values to) is constructed.
 */
public class PropertyValueBuffer
{
    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    protected final JsonParser _parser;
    protected final DeserializationContext _context;

    protected final ObjectIdReader _objectIdReader;
    
    /*
    /**********************************************************
    /* Accumulated properties, other stuff
    /**********************************************************
     */

    /**
     * Buffer used for storing creator parameters for constructing
     * instance.
     */
    protected final Object[] _creatorParameters;
    
    /**
     * Number of creator parameters for which we have not yet received
     * values.
     */
    protected int _paramsNeeded;

    /**
     * Bitflag used to track parameters found from incoming data
     * when number of parameters is
     * less than 32 (fits in int).
     */
    protected int _paramsSeen;

    /**
     * Bitflag used to track parameters found from incoming data
     * when number of parameters is
     * 32 or higher.
     */
    protected final BitSet _paramsSeenBig;

    /**
     * If we get non-creator parameters before or between
     * creator parameters, those need to be buffered. Buffer
     * is just a simple linked list
     */
    protected PropertyValue _buffered;

    /**
     * In case there is an Object Id property to handle, this is the value
     * we have for it.
     */
    protected Object _idValue;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */
    
    public PropertyValueBuffer(JsonParser jp, DeserializationContext ctxt, int paramCount,
            ObjectIdReader oir)
    {
        _parser = jp;
        _context = ctxt;
        _paramsNeeded = paramCount;
        _objectIdReader = oir;
        _creatorParameters = new Object[paramCount];
        if (paramCount < 32) {
            _paramsSeenBig = null;
        } else {
            _paramsSeenBig = new BitSet();
        }
    }

    /**
     * Method called to do necessary post-processing such as injection of values
     * and verification of values for required properties,
     * after either {@link #assignParameter(SettableBeanProperty, Object)}
     * returns <code>true</code> (to indicate all creator properties are found), or when
     * then whole JSON Object has been processed,
     */
    protected Object[] getParameters(SettableBeanProperty[] props)
        throws JsonMappingException
    {
        // quick check to see if anything else is needed
        if (_paramsNeeded > 0) {
            if (_paramsSeenBig == null) {
                int mask = _paramsSeen;
                // not optimal, could use `Integer.trailingZeroes()`, but for now should not
                // really matter for common cases
                for (int ix = 0, len = _creatorParameters.length; ix < len; ++ix, mask >>= 1) {
                    if ((mask & 1) == 0) {
                        _creatorParameters[ix] = _findMissing(props[ix]);
                    }
                }
            } else {
                final int len = _creatorParameters.length;
                for (int ix = 0; (ix = _paramsSeenBig.nextClearBit(ix)) < len; ++ix) {
                    _creatorParameters[ix] = _findMissing(props[ix]);
                }
            }
        }
        return _creatorParameters;
    }

    protected Object _findMissing(SettableBeanProperty prop) throws JsonMappingException
    {
        // First: do we have injectable value?
        Object injectableValueId = prop.getInjectableValueId();
        if (injectableValueId != null) {
            return _context.findInjectableValue(prop.getInjectableValueId(),
                    prop, null);
        }
        // Second: required?
        if (prop.isRequired()) {
            throw _context.mappingException("Missing required creator property '%s' (index %d)",
                    prop.getName(), prop.getCreatorIndex());
        }
        if (_context.isEnabled(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)) {
            throw _context.mappingException("Missing creator property '%s' (index %d); DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES enabled",
                    prop.getName(), prop.getCreatorIndex());
        }
        // Third: default value
        JsonDeserializer<Object> deser = prop.getValueDeserializer();
        return deser.getNullValue(_context);
    }

    /*
    /**********************************************************
    /* Other methods
    /**********************************************************
     */


    /**
     * Helper method called to see if given non-creator property is the "id property";
     * and if so, handle appropriately.
     * 
     * @since 2.1
     */
    public boolean readIdProperty(String propName) throws IOException
    {
        if ((_objectIdReader != null) && propName.equals(_objectIdReader.propertyName.getSimpleName())) {
            _idValue = _objectIdReader.readObjectReference(_parser, _context);
            return true;
        }
        return false;
    }

    /**
     * Helper method called to handle Object Id value collected earlier, if any
     */
    public Object handleIdValue(final DeserializationContext ctxt, Object bean) throws IOException
    {
        if (_objectIdReader != null) {
            if (_idValue != null) {
                ReadableObjectId roid = ctxt.findObjectId(_idValue, _objectIdReader.generator, _objectIdReader.resolver);
                roid.bindItem(bean);
                // also: may need to set a property value as well
                SettableBeanProperty idProp = _objectIdReader.idProperty;
                if (idProp != null) {
                    return idProp.setAndReturn(bean, _idValue);
                }
            } else {
                // TODO: is this an error case?
                throw ctxt.mappingException("No _idValue when handleIdValue called, on instance of %s",
                        bean.getClass().getName());
            }
        }
        return bean;
    }
    
    protected PropertyValue buffered() { return _buffered; }

    public boolean isComplete() { return _paramsNeeded <= 0; }

    /**
     * @return True if we have received all creator parameters
     * 
     * @since 2.6
     */
    public boolean assignParameter(SettableBeanProperty prop, Object value)
    {
        final int ix = prop.getCreatorIndex();
        _creatorParameters[ix] = value;

        if (_paramsSeenBig == null) {
            int old = _paramsSeen;
            int newValue = (old | (1 << ix));
            if (old != newValue) {
                _paramsSeen = newValue;
                if (--_paramsNeeded <= 0) {
                    return true;
                }
            }
        } else {
            if (!_paramsSeenBig.get(ix)) {
                if (--_paramsNeeded <= 0) {
                    return true;
                }
                _paramsSeenBig.set(ix);
            }
        }
        return false;
    }
    
    /**
     * @deprecated Since 2.6
     */
    @Deprecated
    public boolean assignParameter(int index, Object value) {
        // !!! TODO: remove from 2.7
        _creatorParameters[index] = value;
        return false;
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

