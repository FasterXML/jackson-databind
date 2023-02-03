package tools.jackson.databind.cfg;

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
 */
public abstract class ConfigOverride
{
    /**
     * Definitions of format overrides, if any.
     */
    protected JsonFormat.Value _format;

    /**
     * Definitions of inclusion defaults to use for properties included in this POJO type.
     * Overrides global defaults, may be overridden by per-property-type (see
     * {@link #_includeAsProperty}) and per-property overrides (annotations).
     */
    protected JsonInclude.Value _include;

    /**
     * Definitions of inclusion defaults for properties of this specified type (regardless
     * of POJO in which they are included).
     * Overrides global defaults, per-POJO inclusion defaults (see {#link {@link #_include}}),
     * may be overridden by per-property overrides.
     */
    protected JsonInclude.Value _includeAsProperty;

    /**
     * Definitions of property ignoral (whether to serialize, deserialize
     * given logical property) overrides, if any.
     */
    protected JsonIgnoreProperties.Value _ignorals;

    /**
     * Definitions of setter overrides regarding null handling
     */
    protected JsonSetter.Value _nullHandling;

    /**
     * Overrides for auto-detection visibility rules for this type.
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
        _includeAsProperty = src._includeAsProperty;
        _ignorals = src._ignorals;
        _nullHandling = src._nullHandling;
        _visibility = src._visibility;
        _isIgnoredType = src._isIgnoredType;
        _mergeable = src._mergeable;
    }

    /**
     * Accessor for immutable "empty" instance that has no configuration overrides defined.
     */
    public static ConfigOverride empty() {
        return Empty.INSTANCE;
    }

    public JsonFormat.Value getFormat() { return _format; }

    // @since 3.0
    public JsonFormat.Value getFormatOrEmpty() {
        return (_format == null) ? JsonFormat.Value.empty() : _format;
    }

    public JsonInclude.Value getInclude() { return _include; }

    public JsonInclude.Value getIncludeAsProperty() { return _includeAsProperty; }

    public JsonIgnoreProperties.Value getIgnorals() { return _ignorals; }

    public Boolean getIsIgnoredType() {
        return _isIgnoredType;
    }

    public JsonSetter.Value getNullHandling() { return _nullHandling; }

    public JsonAutoDetect.Value getVisibility() { return _visibility; }

    public Boolean getMergeable() { return _mergeable; }

    @Override
    public String toString() {
        return new StringBuilder("[ConfigOverrides ")
                .append("format=").append(_format)
                .append(", include=").append(_include).append("/").append(_includeAsProperty)
                .append(", ignorals=").append(_ignorals)
                .append(", ignoredType=").append(_isIgnoredType)
                .append(", nulls=").append(_ignorals)
                .append(", visibility=").append(_visibility)
                .append(", merge=").append(_mergeable)
                .toString();
    }

    /**
     * Implementation used solely for "empty" instance; has no mutators
     * and is not changed by core functionality.
     */
    final static class Empty extends ConfigOverride {
        final static Empty INSTANCE = new Empty();

        private Empty() { }
    }
}
