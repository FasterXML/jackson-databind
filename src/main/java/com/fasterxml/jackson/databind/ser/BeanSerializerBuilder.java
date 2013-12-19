package com.fasterxml.jackson.databind.ser;

import java.util.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.ser.impl.ObjectIdWriter;

/**
 * Builder class used for aggregating deserialization information about
 * a POJO, in order to build a {@link JsonSerializer} for serializing
 * intances.
 * Main reason for using separate builder class is that this makes it easier
 * to make actual serializer class fully immutable.
 */
public class BeanSerializerBuilder
{
    private final static BeanPropertyWriter[] NO_PROPERTIES = new BeanPropertyWriter[0];

    /*
    /**********************************************************
    /* Basic configuration we start with
    /**********************************************************
     */

    final protected BeanDescription _beanDesc;

    protected SerializationConfig _config;
    
    /*
    /**********************************************************
    /* Accumulated information about properties
    /**********************************************************
     */

    /**
     * Bean properties, in order of serialization
     */
    protected List<BeanPropertyWriter> _properties;

    /**
     * Optional array of filtered property writers; if null, no
     * view-based filtering is performed.
     */
    protected BeanPropertyWriter[] _filteredProperties;
    
    /**
     * Writer used for "any getter" properties, if any.
     */
    protected AnyGetterWriter _anyGetter;

    /**
     * Id of the property filter to use for POJO, if any.
     */
    protected Object _filterId;

    /**
     * Property that is used for type id (and not serialized as regular
     * property)
     */
    protected AnnotatedMember _typeId;

    /**
     * Object responsible for serializing Object Ids for the handled
     * type, if any.
     */
    protected ObjectIdWriter _objectIdWriter;
    
    /*
    /**********************************************************
    /* Construction and setter methods
    /**********************************************************
     */
    
    public BeanSerializerBuilder(BeanDescription beanDesc) {
        _beanDesc = beanDesc;
    }

    /**
     * Copy-constructor that may be used for sub-classing
     */
    protected BeanSerializerBuilder(BeanSerializerBuilder src) {
        _beanDesc = src._beanDesc;
        _properties = src._properties;
        _filteredProperties = src._filteredProperties;
        _anyGetter = src._anyGetter;
        _filterId = src._filterId;
    }

    /**
     * Initialization method called right after construction, to specify
     * configuration to use.
     *<p>
     * Note: ideally should be passed in constructor, but for backwards
     * compatibility, needed to add a setter instead
     * 
     * @since 2.1
     */
    protected void setConfig(SerializationConfig config) {
        _config = config;
    }
    
    public void setProperties(List<BeanPropertyWriter> properties) {
        _properties = properties;
    }

    public void setFilteredProperties(BeanPropertyWriter[] properties) {
        _filteredProperties = properties;
    }
    
    public void setAnyGetter(AnyGetterWriter anyGetter) {
        _anyGetter = anyGetter;
    }

    public void setFilterId(Object filterId) {
        _filterId = filterId;
    }
    
    public void setTypeId(AnnotatedMember idProp) {
        // Not legal to use multiple ones...
        if (_typeId != null) {
            throw new IllegalArgumentException("Multiple type ids specified with "+_typeId+" and "+idProp);
        }
        _typeId = idProp;
    }

    public void setObjectIdWriter(ObjectIdWriter w) {
        _objectIdWriter = w;
    }
    
    /*
    /**********************************************************
    /* Accessors for things BeanSerializer cares about:
    /* note -- likely to change between minor revisions
    /* by new methods getting added.
    /**********************************************************
     */

    public AnnotatedClass getClassInfo() { return _beanDesc.getClassInfo(); }
    
    public BeanDescription getBeanDescription() { return _beanDesc; }

    public List<BeanPropertyWriter> getProperties() { return _properties; }
    public boolean hasProperties() {
        return (_properties != null) && (_properties.size() > 0);
    }

    public BeanPropertyWriter[] getFilteredProperties() { return _filteredProperties; }
    
    public AnyGetterWriter getAnyGetter() { return _anyGetter; }
    
    public Object getFilterId() { return _filterId; }

    public AnnotatedMember getTypeId() { return _typeId; }

    public ObjectIdWriter getObjectIdWriter() { return _objectIdWriter; }
    
    /*
    /**********************************************************
    /* Build methods for actually creating serializer instance
    /**********************************************************
     */
    
    /**
     * Method called to create {@link BeanSerializer} instance with
     * all accumulated information. Will construct a serializer if we
     * have enough information, or return null if not.
     */
    public JsonSerializer<?> build()
    {
        BeanPropertyWriter[] properties;
        // No properties, any getter or object id writer?
        // No real serializer; caller gets to handle
        if (_properties == null || _properties.isEmpty()) {
            if (_anyGetter == null && _objectIdWriter == null) {
                return null;
            }
            properties = NO_PROPERTIES;
        } else {
            properties = _properties.toArray(new BeanPropertyWriter[_properties.size()]);
        }
        return new BeanSerializer(_beanDesc.getType(), this,
                properties, _filteredProperties);
    }
    
    /**
     * Factory method for constructing an "empty" serializer; one that
     * outputs no properties (but handles JSON objects properly, including
     * type information)
     */
    public BeanSerializer createDummy() {
        return BeanSerializer.createDummy(_beanDesc.getType());
    }
}

