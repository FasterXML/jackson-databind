package com.fasterxml.jackson.databind.deser.impl;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;

import com.fasterxml.jackson.core.JsonParser;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.util.ClassUtil;

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
    protected final HashMap<String, SettableBeanProperty> _properties;

    /**
     * Number of properties: usually same as size of {@link #_properties},
     * but not necessarily, when we have unnamed injectable properties.
     */
    protected final int _propertyCount;
    
    /**
     * If some property values must always have a non-null value (like
     * primitive types do), this array contains such default values.
     */
    protected final Object[]  _defaultValues;

    /**
     * Array that contains properties that expect value to inject, if any;
     * null if no injectable values are expected.
     */
    protected final SettableBeanProperty[] _propertiesWithInjectables;
    
    /*
    /**********************************************************
    /* Construction, initialization
    /**********************************************************
     */
    
    protected PropertyBasedCreator(ValueInstantiator valueInstantiator,
            SettableBeanProperty[] creatorProps, Object[] defaultValues)
    {
        _valueInstantiator = valueInstantiator;
        _properties = new HashMap<String, SettableBeanProperty>();
        SettableBeanProperty[] propertiesWithInjectables = null;
        final int len = creatorProps.length;
        _propertyCount = len;
        for (int i = 0; i < len; ++i) {
            SettableBeanProperty prop = creatorProps[i];
            _properties.put(prop.getName(), prop);
            Object injectableValueId = prop.getInjectableValueId();
            if (injectableValueId != null) {
                if (propertiesWithInjectables == null) {
                    propertiesWithInjectables = new SettableBeanProperty[len];
                }
                propertiesWithInjectables[i] = prop;
            }
        }
        _defaultValues = defaultValues;
        _propertiesWithInjectables = propertiesWithInjectables;
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
        Object[] defaultValues = null;
        for (int i = 0; i < len; ++i) {
            SettableBeanProperty prop = srcProps[i];
            if (!prop.hasValueDeserializer()) {
                prop = prop.withValueDeserializer(ctxt.findContextualValueDeserializer(prop.getType(), prop));
            }
            creatorProps[i] = prop;
            // [JACKSON-372]: primitive types need extra care
            // [JACKSON-774]: as do non-default nulls...
            JsonDeserializer<?> deser = prop.getValueDeserializer();
            Object nullValue = (deser == null) ? null : deser.getNullValue();
            if ((nullValue == null) && prop.getType().isPrimitive()) {
                nullValue = ClassUtil.defaultValue(prop.getType().getRawClass());
            }
            if (nullValue != null) {
                if (defaultValues == null) {
                    defaultValues = new Object[len];
                }
                defaultValues[i] = nullValue;
            }
        }
        return new PropertyBasedCreator(valueInstantiator, creatorProps, defaultValues);
    }
    
    public void assignDeserializer(SettableBeanProperty prop, JsonDeserializer<Object> deser) {
        prop = prop.withValueDeserializer(deser);
        _properties.put(prop.getName(), prop);
    }
    
    /*
    /**********************************************************
    /* Accessors
    /**********************************************************
     */
    
    public Collection<SettableBeanProperty> properties() {
        return _properties.values();
    }

    public SettableBeanProperty findCreatorProperty(String name) {
        return _properties.get(name);
    }

    public SettableBeanProperty findCreatorProperty(int propertyIndex) {
        for (SettableBeanProperty prop : _properties.values()) {
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
    public PropertyValueBuffer startBuilding(JsonParser jp, DeserializationContext ctxt,
            ObjectIdReader oir)
    {
        PropertyValueBuffer buffer = new PropertyValueBuffer(jp, ctxt, _propertyCount, oir);
        if (_propertiesWithInjectables != null) {
            buffer.inject(_propertiesWithInjectables);
        }
        return buffer;
    }

    public Object build(DeserializationContext ctxt, PropertyValueBuffer buffer) throws IOException
    {
        Object bean = _valueInstantiator.createFromObjectWith(ctxt, buffer.getParameters(_defaultValues));
        // Object Id to handle?
        bean = buffer.handleIdValue(ctxt, bean);
        
        // Anything buffered?
        for (PropertyValue pv = buffer.buffered(); pv != null; pv = pv.next) {
            pv.assign(bean);
        }
        return bean;
    }
}
