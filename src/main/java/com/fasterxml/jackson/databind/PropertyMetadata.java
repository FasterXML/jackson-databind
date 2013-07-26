package com.fasterxml.jackson.databind;

/**
 * Simple container class used for storing "additional" metadata about
 * properties. Carved out to reduce number of distinct properties that
 * actual property implementations and placeholders need to store;
 * since this instances are immutable, they can be freely shared.
 * 
 * @since 2.3
 */
public class PropertyMetadata
    implements java.io.Serializable
{
    private static final long serialVersionUID = -1;

    public final static PropertyMetadata STD_REQUIRED = new PropertyMetadata(Boolean.TRUE, null);

    public final static PropertyMetadata STD_OPTIONAL = new PropertyMetadata(Boolean.FALSE, null);

    public final static PropertyMetadata STD_REQUIRED_OR_OPTIONAL = new PropertyMetadata(null, null);
    
    /**
     * Three states: required, not required and unknown; unknown represented
     * as null.
     */
    protected final Boolean _required;

    /**
     * Optional human-readable description associated with the property.
     */
    protected final String _description;

    /*
    /**********************************************************
    /* Construction, configuration
    /**********************************************************
     */
    
    protected PropertyMetadata(Boolean req, String desc)
    {
        _required = req;
        _description = desc;
    }

    public static PropertyMetadata construct(boolean req, String desc) {
        PropertyMetadata md = req ? STD_REQUIRED : STD_OPTIONAL;
        if (desc != null) {
            return md.withDescription(desc);
        }
        return md;
    }
    
    /**
     * Minor optimization: let's canonicalize back to placeholders in cases
     * where there is no real data to consider
     */
    protected Object readResolve()
    {
        if (_description == null) {
            if (_required == null) {
                return STD_REQUIRED_OR_OPTIONAL;
            }
            return _required.booleanValue() ? STD_REQUIRED : STD_OPTIONAL;
        }
        return this;
    }

    public PropertyMetadata withDescription(String desc) {
        return new PropertyMetadata(_required, desc);
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
        return new PropertyMetadata(b, _description);
    }
    
    /*
    /**********************************************************
    /* Accessors
    /**********************************************************
     */

    public String getDescription() { return _description; }

    public boolean isRequired() { return (_required != null) && _required.booleanValue(); }
    
    public Boolean getRequired() { return _required; }
}
