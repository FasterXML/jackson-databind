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
    protected List<BeanPropertyWriter> _properties = Collections.emptyList();

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

    /**
     * @param properties Number and order of properties here MUST match that
     *    of "regular" properties set earlier using {@link #setProperties(List)}; if not,
     *    an {@link IllegalArgumentException} will be thrown
     */
    public void setFilteredProperties(BeanPropertyWriter[] properties) {
        if (properties != null) {
            if (properties.length != _properties.size()) { // as per [databind#1612]
                throw new IllegalArgumentException(String.format(
                        "Trying to set %d filtered properties; must match length of non-filtered `properties` (%d)",
                        properties.length, _properties.size()));
            }
        }
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
        // [databind#2789]: There can be a case wherein `_typeId` is used, but
        // nothing else. Rare but has happened; so force access.
        if (_typeId != null) {
            if (_config.isEnabled(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS)) {
                _typeId.fixAccess(_config.isEnabled(MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS));
            }
        }
        if (_anyGetter != null) {
            _anyGetter.fixAccess(_config);
        }

        BeanPropertyWriter[] properties;
        // No properties, any getter or object id writer?
        // No real serializer; caller gets to handle
        if (_properties == null || _properties.isEmpty()) {
            if (_anyGetter == null && _objectIdWriter == null) {
                // NOTE! Caller may still call `createDummy()` later on
                return null;
            }
            properties = NO_PROPERTIES;
        } else {
            properties = _properties.toArray(new BeanPropertyWriter[_properties.size()]);
            if (_config.isEnabled(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS)) {
                for (int i = 0, end = properties.length; i < end; ++i) {
                    properties[i].fixAccess(_config);
                }
            }
        }
        // 27-Apr-2017, tatu: Verify that filtered-properties settings are compatible
        if (_filteredProperties != null) {
            if (_filteredProperties.length != _properties.size()) { // lgtm [java/dereferenced-value-may-be-null]
                throw new IllegalStateException(String.format(
"Mismatch between `properties` size (%d), `filteredProperties` (%s): should have as many (or `null` for latter)",
_properties.size(), _filteredProperties.length));
            }
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
        // 20-Sep-2019, tatu: Can not skimp on passing builder  (see [databind#2077])
        return BeanSerializer.createDummy(_beanDesc.getType(), this);
    }
}

