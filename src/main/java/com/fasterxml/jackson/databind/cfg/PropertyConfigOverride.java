package com.fasterxml.jackson.databind.cfg;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Configuration object that is accessed by databinding functionality
 * to find overrides to configuration of properties, based on declared
 * type of the property. Such overrides have precedence over annotations
 * attached to actual type ({@link java.lang.Class}), but can be further
 * overridden by annotations attached to the property itself.
 *
 * @since 2.8
 */
public abstract class PropertyConfigOverride
{
    protected JsonFormat.Value _format;
    protected JsonInclude.Value _include;

    /**
     * Flag that indicates whether "is ignorable type" is specified for this type;
     * and if so, is it to be ignored (true) or not ignored (false); `null` is
     * used to indicate "not specified", in which case other configuration (class
     * annotation) is used.
     */
    protected Boolean _isIgnoredType;
    
    protected PropertyConfigOverride() { }
    protected PropertyConfigOverride(PropertyConfigOverride src) {
        _format = src._format;
        _include = src._include;
    }

    public JsonFormat.Value getFormat() { return _format; }
    public JsonInclude.Value getInclude() { return _include; }

    public Boolean getIsIgnoredType() {
        return _isIgnoredType;
    }
}
