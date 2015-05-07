package com.fasterxml.jackson.databind.deser.impl;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;

import com.fasterxml.jackson.core.JsonParser;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;

/**
 * Object that is used to collect arguments for non-default creator
 * (non-default-constructor, or argument-taking factory method)
 * before creator can be called.
 * Since ordering of JSON properties is not guaranteed, this may
 * require buffering of values other than ones being passed to
 * creator.
 */
public final class PropertyBasedCreator
{
    protected final ValueInstantiator _valueInstantiator;
    
    /**
     * Map that contains property objects for either constructor or factory
     * method (whichever one is null: one property for each
     * parameter for that one), keyed by logical property name
     */
    protected final HashMap<String, SettableBeanProperty> _propertyLookup;

    /**
     * Number of properties: usually same as size of {@link #_propertyLookup},
     * but not necessarily, when we have unnamed injectable properties.
     */
    protected final int _propertyCount;

    /**
     * Array that contains properties that expect value to inject, if any;
     * null if no injectable values are expected.
     */
    protected final SettableBeanProperty[] _allProperties;
    
    /*
    /**********************************************************
    /* Construction, initialization
    /**********************************************************
     */
    
    protected PropertyBasedCreator(ValueInstantiator valueInstantiator,
            SettableBeanProperty[] creatorProps)
    {
        _valueInstantiator = valueInstantiator;
        _propertyLookup = new HashMap<String, SettableBeanProperty>();
        final int len = creatorProps.length;
        _propertyCount = len;
        _allProperties = new SettableBeanProperty[len];
        for (int i = 0; i < len; ++i) {
            SettableBeanProperty prop = creatorProps[i];
            _allProperties[i] = prop;
            _propertyLookup.put(prop.getName(), prop);
        }
    }

    /**
     * Factory method used for building actual instances: resolves deserializers
     * and checks for "null values".
     */
    public static PropertyBasedCreator construct(DeserializationContext ctxt,
            ValueInstantiator valueInstantiator, SettableBeanProperty[] srcProps)
        throws JsonMappingException
    {
        final int len = srcProps.length;
        SettableBeanProperty[] creatorProps = new SettableBeanProperty[len];
        for (int i = 0; i < len; ++i) {
            SettableBeanProperty prop = srcProps[i];
            if (!prop.hasValueDeserializer()) {
                prop = prop.withValueDeserializer(ctxt.findContextualValueDeserializer(prop.getType(), prop));
            }
            creatorProps[i] = prop;
        }        
        return new PropertyBasedCreator(valueInstantiator, creatorProps);
    }

    // 05-May-2015, tatu: Does not seem to be used, commented out in 2.6
    /*
    public void assignDeserializer(SettableBeanProperty prop, JsonDeserializer<Object> deser) {
        prop = prop.withValueDeserializer(deser);
        _properties.put(prop.getName(), prop);
    }
    */
    
    /*
    /**********************************************************
    /* Accessors
    /**********************************************************
     */
    
    public Collection<SettableBeanProperty> properties() {
        return _propertyLookup.values();
    }

    public SettableBeanProperty findCreatorProperty(String name) {
        return _propertyLookup.get(name);
    }

    public SettableBeanProperty findCreatorProperty(int propertyIndex) {
        for (SettableBeanProperty prop : _propertyLookup.values()) {
            if (prop.getPropertyIndex() == propertyIndex) {
                return prop;
            }
        }
        return null;
    }
    
    /*
    /**********************************************************
    /* Building process
    /**********************************************************
     */

    /**
     * Method called when starting to build a bean instance.
     * 
     * @since 2.1 (added ObjectIdReader parameter -- existed in previous versions without)
     */
    public PropertyValueBuffer startBuilding(JsonParser p, DeserializationContext ctxt,
            ObjectIdReader oir) {
        return new PropertyValueBuffer(p, ctxt, _propertyCount, oir);
    }

    public Object build(DeserializationContext ctxt, PropertyValueBuffer buffer) throws IOException
    {
        Object bean = _valueInstantiator.createFromObjectWith(ctxt,
                buffer.getParameters(_allProperties));
        // returning null isn't quite legal, but let's let caller deal with that
        if (bean != null) {
            // Object Id to handle?
            bean = buffer.handleIdValue(ctxt, bean);
            
            // Anything buffered?
            for (PropertyValue pv = buffer.buffered(); pv != null; pv = pv.next) {
                pv.assign(bean);
            }
        }
        return bean;
    }
}
