package com.fasterxml.jackson.databind.cfg;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * Configuration object that is accessed by databinding functionality
 * to find overrides to configuration of properties, based on declared
 * type of the property. Such overrides have precedence over annotations
 * attached to actual type ({@link java.lang.Class}), but can be further
 * overridden by annotations attached to the property itself.
 *
 * @since 2.8
 */
public abstract class ConfigOverride
{
    /**
     * Definitions of format overrides, if any.
     */
    protected JsonFormat.Value _format;

    /**
     * Definitions of inclusion overrides, if any.
     */
    protected JsonInclude.Value _include;

    /**
     * Definitions of property ignoral (whether to serialize, deserialize
     * given logical property) overrides, if any.
     */
    protected JsonIgnoreProperties.Value _ignorals;

    /**
     * Definitions of setter overrides regarding null handling
     *
     * @since 2.9
     */
    protected JsonSetter.Value _setterInfo;

    /**
     * Overrides for auto-detection visibility rules for this type.
     *
     * @since 2.9
     */
    protected JsonAutoDetect.Value _visibility;

    /**
     * Flag that indicates whether "is ignorable type" is specified for this type;
     * and if so, is it to be ignored (true) or not ignored (false); `null` is
     * used to indicate "not specified", in which case other configuration (class
     * annotation) is used.
     */
    protected Boolean _isIgnoredType;

    /**
     * Flag that indicates whether properties of this type default to being merged
     * or not.
     */
    protected Boolean _mergeable;
    
    protected ConfigOverride() { }
    protected ConfigOverride(ConfigOverride src) {
        _format = src._format;
        _include = src._include;
        _ignorals = src._ignorals;
        _isIgnoredType = src._isIgnoredType;
        _mergeable = src._mergeable;
    }

    /**
     * Accessor for immutable "empty" instance that has no configuration overrides defined.
     *
     * @since 2.9
     */
    public static ConfigOverride empty() {
        return Empty.INSTANCE;
    }

    public JsonFormat.Value getFormat() { return _format; }
    public JsonInclude.Value getInclude() { return _include; }

    public JsonIgnoreProperties.Value getIgnorals() { return _ignorals; }

    public Boolean getIsIgnoredType() {
        return _isIgnoredType;
    }
    
    /**
     * @since 2.9
     */
    public JsonSetter.Value getSetterInfo() { return _setterInfo; }

    /**
     * @since 2.9
     */
    public JsonAutoDetect.Value getVisibility() { return _visibility; }

    /**
     * @since 2.9
     */
    public Boolean getMergeable() { return _mergeable; }
    
    /**
     * Implementation used solely for "empty" instance; has no mutators
     * and is not changed by core functionality.
     *
     * @since 2.9
     */
    final static class Empty extends ConfigOverride {
        final static Empty INSTANCE = new Empty();

        private Empty() { }
    }
}
