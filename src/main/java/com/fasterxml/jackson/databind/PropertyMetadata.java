package com.fasterxml.jackson.databind;

/**
 * Simple container class used for storing "additional" metadata about
 * properties. Carved out to reduce number of distinct properties that
 * actual property implementations and placeholders need to store;
 * since instances are immutable, they can be freely shared.
 * 
 * @since 2.3
 */
public class PropertyMetadata
    implements java.io.Serializable
{
    private static final long serialVersionUID = -1;

    public final static PropertyMetadata STD_REQUIRED = new PropertyMetadata(Boolean.TRUE, null, null, null);

    public final static PropertyMetadata STD_OPTIONAL = new PropertyMetadata(Boolean.FALSE, null, null, null);

    public final static PropertyMetadata STD_REQUIRED_OR_OPTIONAL = new PropertyMetadata(null, null, null, null);
    
    /**
     * Three states: required, not required and unknown; unknown represented
     * as null.
     */
    protected final Boolean _required;

    /**
     * Optional human-readable description associated with the property.
     */
    protected final String _description;

    /**
     * Optional index of the property within containing Object.
     * 
     * @since 2.4
     */
    protected final Integer _index;

    /**
     * Optional default value, as String, for property; not used cor
     * any functionality by core databind, offered as metadata for
     * extensions.
     */
    protected final String _defaultValue;
    
    /*
    /**********************************************************
    /* Construction, configuration
    /**********************************************************
     */
    
    @Deprecated // since 2.4
    protected PropertyMetadata(Boolean req, String desc) { this(req, desc, null, null); }

    /**
     * @since 2.5
     */
    protected PropertyMetadata(Boolean req, String desc, Integer index, String def)
    {
        _required = req;
        _description = desc;
        _index = index;
        _defaultValue = (def == null || def.isEmpty()) ? null : def;
    }

    /**
     * @since 2.4 Use variant that takes more arguments.
     */
    @Deprecated
    public static PropertyMetadata construct(boolean req, String desc) {
        return construct(req, desc, null, null);
    }

    public static PropertyMetadata construct(boolean req, String desc, Integer index,
            String defaultValue) {
        if (desc != null || index != null || defaultValue != null) {
            return new PropertyMetadata(req, desc, index, defaultValue);
        }
        return req ? STD_REQUIRED : STD_OPTIONAL;
    }
    
    /**
     * Minor optimization: let's canonicalize back to placeholders in cases
     * where there is no real data to consider
     */
    protected Object readResolve()
    {
        if (_description == null && _index == null && _defaultValue == null) {
            if (_required == null) {
                return STD_REQUIRED_OR_OPTIONAL;
            }
            return _required.booleanValue() ? STD_REQUIRED : STD_OPTIONAL;
        }
        return this;
    }

    public PropertyMetadata withDescription(String desc) {
        return new PropertyMetadata(_required, desc, _index, _defaultValue);
    }

    public PropertyMetadata withDefaultValue(String def) {
        if ((def == null) || def.isEmpty()) {
            if (_defaultValue == null) {
                return this;
            }
            def = null;
        } else if (_defaultValue.equals(def)) {
            return this;
        }
        return new PropertyMetadata(_required, _description, _index, def);
    }
    
    public PropertyMetadata withIndex(Integer index) {
        return new PropertyMetadata(_required, _description, index, _defaultValue);
    }
    
    public PropertyMetadata withRequired(Boolean b) {
        if (b == null) {
            if (_required == null) {
                return this;
            }
        } else {
            if (_required != null && _required.booleanValue() == b.booleanValue()) {
                return this;
            }
        }
        return new PropertyMetadata(b, _description, _index, _defaultValue);
    }
    
    /*
    /**********************************************************
    /* Accessors
    /**********************************************************
     */

    public String getDescription() { return _description; }

    /**
     * @since 2.5
     */
    public String getDefaultValue() { return _defaultValue; }

    /**
     * @since 2.5
     */
    public boolean hasDefuaultValue() { return hasDefaultValue(); }

    // NOTE: officially only added in 2.6 (to replace 'hasDefuaultValue()'; actually added in 2.5.1
    //  for forwards-compatibility
    public boolean hasDefaultValue() { return (_defaultValue != null); }
    
    public boolean isRequired() { return (_required != null) && _required.booleanValue(); }
    
    public Boolean getRequired() { return _required; }

    /**
     * @since 2.4
     */
    public Integer getIndex() { return _index; }

    /**
     * @since 2.4
     */
    public boolean hasIndex() { return _index != null; }
}
