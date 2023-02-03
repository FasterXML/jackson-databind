package com.fasterxml.jackson.databind.exc;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * Base class for {@link JsonMappingException}s that are specifically related
 * to problems related to binding an individual property.
 *
 * @since 2.3
 */
@SuppressWarnings("serial")
public abstract class PropertyBindingException
    extends MismatchedInputException // since 2.9
{
    /**
     * Class that does not contain mapping for the unrecognized property.
     */
    protected final Class<?> _referringClass;

    /**
     *<p>
     * Note: redundant information since it is also included in the
     * reference path.
     */
    protected final String _propertyName;

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

    /**
     * @since 2.7
     */
    protected PropertyBindingException(JsonParser p, String msg, JsonLocation loc,
            Class<?> referringClass, String propName,
            Collection<Object> propertyIds)
    {
        super(p, msg, loc);
        _referringClass = referringClass;
        _propertyName = propName;
        _propertyIds = propertyIds;
    }

    /**
     * @deprecated Since 2.7
     */
    @Deprecated // since 2.7
    protected PropertyBindingException(String msg, JsonLocation loc,
            Class<?> referringClass, String propName,
            Collection<Object> propertyIds)
    {
        this(null, msg, loc, referringClass, propName, propertyIds);
    }

    /*
    /**********************************************************
    /* Overrides
    /**********************************************************
     */

    /**
     * Somewhat arbitrary limit, but let's try not to create uselessly
     * huge error messages
     */
    private final static int MAX_DESC_LENGTH = 1000;

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
                    sb.append('"');
                    sb.append(String.valueOf(it.next()));
                    sb.append('"');
                    // one other thing: limit max length
                    if (sb.length() > MAX_DESC_LENGTH) {
                        sb.append(" [truncated]");
                        break;
                    }
                    if (it.hasNext()) {
                        sb.append(", ");
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
    public String getPropertyName() {
        return _propertyName;
    }

    public Collection<Object> getKnownPropertyIds()
    {
        if (_propertyIds == null) {
            return null;
        }
        return Collections.unmodifiableCollection(_propertyIds);
    }
}
