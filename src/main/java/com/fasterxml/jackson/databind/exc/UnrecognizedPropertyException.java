package com.fasterxml.jackson.databind.exc;

import java.util.*;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * Specialized {@link JsonMappingException} sub-class specifically used
 * to indicate problems due to encountering a JSON property that could
 * not be mapped to an Object property (via getter, constructor argument
 * or field).
 */
public class UnrecognizedPropertyException
    extends JsonMappingException
{
    private static final long serialVersionUID = 1L;

    /**
     * Class that does not contain mapping for the unrecognized property.
     */
    protected final Class<?> _referringClass;
    
    /**
     *<p>
     * Note: redundant information since it is also included in the
     * reference path.
     */
    protected final String _unrecognizedPropertyName;
    
    /**
     * Set of ids of properties that are known for the type, if this
     * can be statically determined.
     */
    protected final Collection<Object> _propertyIds;

    /**
     * Lazily constructed description of known properties, used for
     * constructing actual message if and as needed.
     */
    protected transient String _propertiesAsString;
    
    public UnrecognizedPropertyException(String msg, JsonLocation loc,
            Class<?> referringClass, String propName,
            Collection<Object> propertyIds)
    {
        
        super(msg, loc);
        _referringClass = referringClass;
        _unrecognizedPropertyName = propName;
        _propertyIds = propertyIds;
    }

    /**
     * Factory method used for constructing instances of this exception type.
     * 
     * @param jp Underlying parser used for reading input being used for data-binding
     * @param fromObjectOrClass Reference to either instance of problematic type (
     *    if available), or if not, type itself
     * @param propertyName Name of unrecognized property
     * @param propertyIds (optional, null if not available) Set of properties that
     *    type would recognize, if completely known: null if set can not be determined.
     */
    public static UnrecognizedPropertyException from(JsonParser jp,
            Object fromObjectOrClass, String propertyName,
            Collection<Object> propertyIds)
    {
        if (fromObjectOrClass == null) {
            throw new IllegalArgumentException();
        }
        Class<?> ref;
        if (fromObjectOrClass instanceof Class<?>) {
            ref = (Class<?>) fromObjectOrClass;
        } else {
            ref = fromObjectOrClass.getClass();
        }
        String msg = "Unrecognized field \""+propertyName+"\" (class "+ref.getName()+"), not marked as ignorable";
        UnrecognizedPropertyException e = new UnrecognizedPropertyException(msg,
                jp.getCurrentLocation(), ref, propertyName, propertyIds);
        // but let's also ensure path includes this last (missing) segment
        e.prependPath(fromObjectOrClass, propertyName);
        return e;
    }

    /*
    /**********************************************************
    /* Overrides
    /**********************************************************
     */

    private final static int MAX_DESC_LENGTH = 200;
    
    @Override
    public String getMessageSuffix()
    {
        String suffix = _propertiesAsString;
        if (suffix == null && _propertyIds != null) {
            StringBuilder sb = new StringBuilder(100);
            int len = _propertyIds.size();
            if (len == 1) {
                sb.append(" (one known property: \"");
                sb.append(String.valueOf(_propertyIds.iterator().next()));
                sb.append('"');
            } else {
                sb.append(" (").append(len).append(" known properties: ");
                Iterator<Object> it = _propertyIds.iterator();
                while (it.hasNext()) {
                    sb.append(", \"");
                    sb.append(String.valueOf(it.next()));
                    sb.append('"');
                    // one other thing: limit max length
                    if (sb.length() > MAX_DESC_LENGTH) {
                        sb.append(" [truncated]");
                        break;
                    }
                }
            }
            sb.append("])");
            _propertiesAsString = suffix = sb.toString();
        }
        return suffix;
    }

    /*
    /**********************************************************
    /* Extended API
    /**********************************************************
     */
    
    /**
     * Method for accessing type (class) that is missing definition to allow
     * binding of the unrecognized property.
     */
    public Class<?> getReferringClass() {
        return _referringClass;
    }
    
    /**
     * Convenience method for accessing logical property name that could
     * not be mapped. Note that it is the last path reference in the
     * underlying path.
     */
    public String getUnrecognizedPropertyName() {
        return _unrecognizedPropertyName;
    }    
    
    public Collection<Object> getKnownPropertyIds()
    {
        if (_propertyIds == null) {
            return null;
        }
        return Collections.unmodifiableCollection(_propertyIds);
    }
}
